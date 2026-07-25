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
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.EmbeddedCedarNeo4j;
import org.metadatacenter.util.test.TestAuthUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Endpoint tests for the group resource against an in-process Neo4j. Authentication is served by
 * the in-memory user service; the graph is seeded with the global objects and test users, so the
 * queries below run against the real Cypher layer.
 */
public class GroupsResourceTest {

  static {
    // Must run before the test support boots the server, which reads the Neo4j env vars.
    // Alternate server ports, so the test instance never collides with a running dev server.
    // Redis is redirected to a dead port: queue writes are best-effort, and this enforces that
    // no endpoint under test ever depends on a live Redis.
    EmbeddedCedarNeo4j.startAndRedirectEnvironment(Map.of(
        "CEDAR_GROUP_HTTP_PORT", "19009",
        "CEDAR_GROUP_ADMIN_PORT", "19109",
        "CEDAR_GROUP_STOP_PORT", "19209",
        "CEDAR_REDIS_PERSISTENT_PORT", "1"));
  }

  public static final DropwizardTestSupport<GroupServerConfiguration> SERVER =
      new DropwizardTestSupport<>(GroupServerApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));

  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  private static String authHeaderAdmin;
  private static String authHeaderUser1;

  @BeforeAll
  public static void oneTimeSetUp() throws Exception {
    SERVER.before();
    Map<String, String> environment = CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_GROUP);
    CedarConfig cedarConfig = CedarConfig.getInstance(environment);

    TestAuthUtil.installInMemoryUserService(cedarConfig);
    authHeaderAdmin = TestAuthUtil.getAdminUserAuthHeader(cedarConfig);
    authHeaderUser1 = TestAuthUtil.getTestUser1AuthHeader(cedarConfig);

    EmbeddedCedarNeo4j.seed(cedarConfig);
  }

  @AfterAll
  public static void oneTimeTearDown() {
    SERVER.after();
  }

  private HttpResponse<String> request(String method, String path, String body, String authHeader) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + path))
        .header("Content-Type", "application/json");
    if (authHeader != null) {
      builder.header("Authorization", authHeader);
    }
    if (body == null) {
      builder.method(method, HttpRequest.BodyPublishers.noBody());
    } else {
      builder.method(method, HttpRequest.BodyPublishers.ofString(body));
    }
    return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static String encode(String id) {
    return URLEncoder.encode(id, StandardCharsets.UTF_8);
  }

  @Test
  public void everybodyGroupIsPresent() throws Exception {
    HttpResponse<String> response = request("GET", "/groups", null, authHeaderAdmin);
    Assertions.assertEquals(200, response.statusCode());
    Assertions.assertTrue(response.body().contains("Everybody"));
  }

  @Test
  public void groupListingRequiresGroupReadPermission() throws Exception {
    HttpResponse<String> response = request("GET", "/groups", null, authHeaderUser1);
    Assertions.assertEquals(403, response.statusCode());
  }

  @Test
  public void groupLifecycleCreateReadUpdateDelete() throws Exception {
    // Create
    HttpResponse<String> created = request("POST", "/groups",
        "{\"schema:name\": \"Test Group\", \"schema:description\": \"A group created by the integration test\"}",
        authHeaderAdmin);
    Assertions.assertEquals(201, created.statusCode());
    JsonNode group = JsonMapper.MAPPER.readTree(created.body());
    String groupId = group.get("@id").asText();

    // Read back
    HttpResponse<String> found = request("GET", "/groups/" + encode(groupId), null, authHeaderAdmin);
    Assertions.assertEquals(200, found.statusCode());
    Assertions.assertEquals("Test Group", JsonMapper.MAPPER.readTree(found.body()).get("schema:name").asText());

    // Update
    HttpResponse<String> updated = request("PUT", "/groups/" + encode(groupId),
        "{\"schema:name\": \"Test Group Renamed\", \"schema:description\": \"Updated description\"}",
        authHeaderAdmin);
    Assertions.assertEquals(200, updated.statusCode());
    Assertions.assertEquals("Test Group Renamed", JsonMapper.MAPPER.readTree(updated.body()).get("schema:name").asText());

    // Delete
    HttpResponse<String> deleted = request("DELETE", "/groups/" + encode(groupId), null, authHeaderAdmin);
    Assertions.assertEquals(204, deleted.statusCode());

    HttpResponse<String> gone = request("GET", "/groups/" + encode(groupId), null, authHeaderAdmin);
    Assertions.assertEquals(404, gone.statusCode());
  }

  @Test
  public void duplicateGroupNameIsRejected() throws Exception {
    String body = "{\"schema:name\": \"Duplicate Group\", \"schema:description\": \"first\"}";
    HttpResponse<String> first = request("POST", "/groups", body, authHeaderAdmin);
    Assertions.assertEquals(201, first.statusCode());
    HttpResponse<String> second = request("POST", "/groups", body, authHeaderAdmin);
    Assertions.assertEquals(400, second.statusCode());
    Assertions.assertTrue(second.body().contains("groupAlreadyPresent"));
  }

  @Test
  public void everybodyGroupCanNotBeDeleted() throws Exception {
    HttpResponse<String> groups = request("GET", "/groups", null, authHeaderAdmin);
    JsonNode groupList = JsonMapper.MAPPER.readTree(groups.body()).get("groups");
    String everybodyId = null;
    for (JsonNode g : groupList) {
      if ("Everybody".equals(g.get("schema:name").asText())) {
        everybodyId = g.get("@id").asText();
      }
    }
    Assertions.assertNotNull(everybodyId, "The Everybody group must exist");

    HttpResponse<String> deleted = request("DELETE", "/groups/" + encode(everybodyId), null, authHeaderAdmin);
    Assertions.assertEquals(400, deleted.statusCode());
  }

}
