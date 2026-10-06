package org.metadatacenter.cedar.group.resources;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.group.GroupServerApplication;
import org.metadatacenter.cedar.group.GroupServerConfiguration;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.server.security.model.auth.CedarGroupUserRequest;
import org.metadatacenter.server.security.model.auth.CedarGroupUsersRequest;
import org.metadatacenter.server.security.model.permission.resource.ResourcePermissionUser;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.EmbeddedCedarNeo4j;
import org.metadatacenter.util.test.TestAuthUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Every conditional write the group server takes, with every kind of If-Match, on a group that
 * exists and on one that does not.
 *
 * <p>Each write handled If-Match in a block of its own, and the blocks disagreed about order. An
 * update or a patch asked for the header before it looked the group up, so a write to a group that
 * did not exist was told to send an If-Match rather than that there was nothing to write to. A delete
 * and a membership change looked first.
 *
 * <p>The rule the table holds every write to: without If-Match, an existing group answers 428 and a
 * missing one 404. With one, a current tag succeeds, as does {@code *}, a list holding the current tag
 * and the current tag with a representation suffix, and a stale, weak or malformed tag answers 412.
 * A missing group answers a conditional update or patch with 412, since the group the caller meant
 * to change has gone, and a delete or a membership change with 404, as the artifact server answers a
 * delete.
 */
public class GroupConditionalWriteMatrixTest {

  static {
    EmbeddedCedarNeo4j.startAndRedirectEnvironment(Map.of(
        "CEDAR_GROUP_HTTP_PORT", "0",
        "CEDAR_GROUP_ADMIN_PORT", "0",
        "CEDAR_GROUP_STOP_PORT", "0",
        "CEDAR_REDIS_PERSISTENT_PORT", "1"));
  }

  public static final DropwizardTestSupport<GroupServerConfiguration> SERVER =
      new DropwizardTestSupport<>(GroupServerApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));

  private static final HttpClient CLIENT = HttpClient.newHttpClient();
  private static String authHeaderAdmin;

  @BeforeAll
  public static void oneTimeSetUp() throws Exception {
    SERVER.before();
    Map<String, String> environment = CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_GROUP);
    CedarConfig cedarConfig = CedarConfig.getInstance(environment);
    TestAuthUtil.installInMemoryUserService(cedarConfig);
    authHeaderAdmin = TestAuthUtil.getAdminUserAuthHeader(cedarConfig);
    EmbeddedCedarNeo4j.seed(cedarConfig);
  }

  @AfterAll
  public static void oneTimeTearDown() {
    SERVER.after();
  }

  /** What a write sends, given the group's identifier while the group still exists. */
  private interface Body {
    String of(String id) throws Exception;
  }

  /** A conditional write: where it goes, what it sends, where its current tag is read, and its success. */
  private record Write(String name, String method, String contentType, Function<String, String> path, Body body,
                       Function<String, String> tagPath, int success) {}

  private static final List<Write> WRITES = List.of(
      new Write("rename", "PUT", "application/json", id -> "/groups/" + encode(id),
          id -> "{\"schema:name\": \"Renamed " + UUID.randomUUID() + "\", \"schema:description\": \"renamed\"}",
          id -> "/groups/" + encode(id), 200),
      new Write("patch", "PATCH", "application/merge-patch+json", id -> "/groups/" + encode(id),
          id -> "{\"schema:description\": \"patched\"}", id -> "/groups/" + encode(id), 200),
      new Write("delete", "DELETE", "application/json", id -> "/groups/" + encode(id), id -> null,
          id -> "/groups/" + encode(id), 204),
      new Write("replace members", "PUT", "application/json", id -> "/groups/" + encode(id) + "/users",
          GroupConditionalWriteMatrixTest::unchangedMembership, id -> "/groups/" + encode(id) + "/users", 200));

  /** An If-Match value, from the target's current tag, which is null for a group that does not exist. */
  private record Tag(String name, Function<String, String> value, boolean matches) {}

  private static final List<Tag> TAGS = List.of(
      new Tag("absent", current -> null, false),
      new Tag("blank", current -> " ", false),
      new Tag("current", current -> current, true),
      new Tag("any", current -> "*", true),
      new Tag("a list holding the current tag", current -> "\"999\", " + current, true),
      new Tag("the current tag with a representation suffix",
          current -> current.substring(0, current.length() - 1) + "-yaml\"", true),
      new Tag("stale", current -> "\"999\"", false),
      new Tag("weak", current -> "W/" + current, false),
      new Tag("malformed", current -> "not-a-tag", false),
      new Tag("a list without the current tag", current -> "\"998\", \"999\"", false));

  private static boolean absent(Tag tag) {
    return tag.name().equals("absent") || tag.name().equals("blank");
  }

  private static int expected(Write write, Tag tag, boolean exists) {
    if (exists)
      return absent(tag) ? 428 : tag.matches() ? write.success() : 412;
    if (absent(tag))
      return 404;
    return write.name().equals("rename") || write.name().equals("patch") ? 412 : 404;
  }

  @Test
  public void everyConditionalWriteAnswersEveryIfMatchByTheSameRule() throws Exception {
    List<String> differences = new ArrayList<>();
    for (Write write : WRITES) {
      for (Tag tag : TAGS) {
        for (boolean exists : List.of(true, false)) {
          String id = createGroup();
          String current = tagOf(write.tagPath().apply(id));
          String body = write.body().of(id);
          if (!exists) {
            HttpResponse<String> deleted = send("DELETE", "/groups/" + encode(id), null, "application/json",
                tagOf("/groups/" + encode(id)));
            Assertions.assertEquals(204, deleted.statusCode(), deleted.body());
          }
          HttpResponse<String> answer = send(write.method(), write.path().apply(id), body, write.contentType(),
              tag.value().apply(current));
          int expected = expected(write, tag, exists);
          if (answer.statusCode() != expected)
            differences.add(write.name() + " / " + tag.name() + " / " + (exists ? "exists" : "missing") + ": "
                + answer.statusCode() + " where " + expected);
        }
      }
    }
    Assertions.assertEquals(List.of(), differences);
  }

  /** The membership as it stands, restated in the shape a write takes. */
  private static String unchangedMembership(String id) throws Exception {
    HttpResponse<String> listing = send("GET", "/groups/" + encode(id) + "/users", null, "application/json", null);
    Assertions.assertEquals(200, listing.statusCode(), listing.body());
    CedarGroupUsersRequest request = new CedarGroupUsersRequest();
    for (JsonNode member : JsonMapper.STRICT_MAPPER.readTree(listing.body()).get("users")) {
      request.getUsers().add(new CedarGroupUserRequest(new ResourcePermissionUser(member.get("user").get("@id").asText()),
          member.get("administrator").asBoolean(), member.get("member").asBoolean()));
    }
    return JsonMapper.STRICT_MAPPER.writeValueAsString(request);
  }

  private static String createGroup() throws Exception {
    HttpResponse<String> created = send("POST", "/groups",
        "{\"schema:name\": \"Conditional " + UUID.randomUUID() + "\", \"schema:description\": \"matrix\"}",
        "application/json", null);
    Assertions.assertEquals(201, created.statusCode(), created.body());
    JsonNode group = JsonMapper.STRICT_MAPPER.readTree(created.body());
    return group.get("@id").asText();
  }

  private static String tagOf(String path) throws Exception {
    HttpResponse<String> read = send("GET", path, null, "application/json", null);
    Assertions.assertEquals(200, read.statusCode(), read.body());
    return read.headers().firstValue("ETag").orElseThrow();
  }

  private static HttpResponse<String> send(String method, String path, String body, String contentType, String ifMatch)
      throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + path))
        .header("Content-Type", contentType)
        .header("Authorization", authHeaderAdmin);
    if (ifMatch != null)
      builder.header("If-Match", ifMatch);
    builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
    return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static String encode(String id) {
    return URLEncoder.encode(id, StandardCharsets.UTF_8);
  }
}
