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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Endpoint tests for the group resource against an in-process Neo4j. Authentication is served by
 * the in-memory user service; the graph is seeded with the global objects and test users, so the
 * queries below run against the real Cypher layer.
 */
public class GroupsResourceTest {

  static {
    // Must run before the test support boots the server, which reads the Neo4j env vars.
    // OS-assigned server ports, so the test instance never collides with a running dev server.
    // Redis is redirected to a dead port: queue writes are best-effort, and this enforces that
    // no endpoint under test ever depends on a live Redis.
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
    return request(method, path, body, authHeader, "application/json");
  }

  private HttpResponse<String> request(String method, String path, String body, String authHeader,
                                       String contentType) throws Exception {
    return request(method, path, body, authHeader, contentType, null);
  }

  private HttpResponse<String> request(String method, String path, String body, String authHeader,
                                       String contentType, String ifMatch) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + path))
        .header("Content-Type", contentType);
    if (authHeader != null) {
      builder.header("Authorization", authHeader);
    }
    if (ifMatch != null) {
      builder.header("If-Match", ifMatch);
    }
    if (body == null) {
      builder.method(method, HttpRequest.BodyPublishers.noBody());
    } else {
      builder.method(method, HttpRequest.BodyPublishers.ofString(body));
    }
    return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private String createGroup(String name, String description) throws Exception {
    HttpResponse<String> created = request("POST", "/groups",
        "{\"schema:name\": \"" + name + "\", \"schema:description\": \"" + description + "\"}", authHeaderAdmin);
    Assertions.assertEquals(201, created.statusCode(), "fixture group was not created: " + created.body());
    return JsonMapper.MAPPER.readTree(created.body()).get("@id").asText();
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
    Assertions.assertEquals("\"1\"", created.headers().firstValue("ETag").orElse(null));
    JsonNode group = JsonMapper.MAPPER.readTree(created.body());
    String groupId = group.get("@id").asText();

    // Read back
    HttpResponse<String> found = request("GET", "/groups/" + encode(groupId), null, authHeaderAdmin);
    Assertions.assertEquals(200, found.statusCode());
    Assertions.assertEquals("\"1\"", found.headers().firstValue("ETag").orElse(null));
    Assertions.assertEquals("Test Group", JsonMapper.MAPPER.readTree(found.body()).get("schema:name").asText());

    // Update
    HttpResponse<String> missingPrecondition = request("PUT", "/groups/" + encode(groupId),
        "{\"schema:name\": \"Test Group Renamed\", \"schema:description\": \"Updated description\"}",
        authHeaderAdmin);
    Assertions.assertEquals(428, missingPrecondition.statusCode(), missingPrecondition.body());

    HttpResponse<String> updated = request("PUT", "/groups/" + encode(groupId),
        "{\"schema:name\": \"Test Group Renamed\", \"schema:description\": \"Updated description\"}",
        authHeaderAdmin, "application/json", "\"1\"");
    Assertions.assertEquals(200, updated.statusCode());
    Assertions.assertEquals("\"2\"", updated.headers().firstValue("ETag").orElse(null));
    Assertions.assertEquals("Test Group Renamed", JsonMapper.MAPPER.readTree(updated.body()).get("schema:name").asText());

    HttpResponse<String> staleUpdate = request("PUT", "/groups/" + encode(groupId),
        "{\"schema:name\": \"Stale Group Name\", \"schema:description\": \"stale\"}",
        authHeaderAdmin, "application/json", "\"1\"");
    Assertions.assertEquals(412, staleUpdate.statusCode(), staleUpdate.body());

    // Delete
    HttpResponse<String> staleDelete = request("DELETE", "/groups/" + encode(groupId), null,
        authHeaderAdmin, "application/json", "\"1\"");
    Assertions.assertEquals(412, staleDelete.statusCode(), staleDelete.body());

    HttpResponse<String> deleted = request("DELETE", "/groups/" + encode(groupId), null,
        authHeaderAdmin, "application/json", "\"2\"");
    Assertions.assertEquals(204, deleted.statusCode());

    HttpResponse<String> gone = request("GET", "/groups/" + encode(groupId), null, authHeaderAdmin);
    Assertions.assertEquals(404, gone.statusCode());

    String staleBody = "{\"schema:name\": \"Deleted Group\", \"schema:description\": \"must stay deleted\"}";
    for (String ifMatch : List.of("\"2\"", "*")) {
      HttpResponse<String> stalePut = request("PUT", "/groups/" + encode(groupId), staleBody,
          authHeaderAdmin, "application/json", ifMatch);
      Assertions.assertEquals(412, stalePut.statusCode(), stalePut.body());

      HttpResponse<String> stalePatch = request("PATCH", "/groups/" + encode(groupId),
          "{\"schema:description\": \"must stay deleted\"}", authHeaderAdmin,
          "application/merge-patch+json", ifMatch);
      Assertions.assertEquals(412, stalePatch.statusCode(), stalePatch.body());
    }
  }

  @Test
  public void concurrentGroupDeletesConvergeWithoutServerOrPermissionErrors() throws Exception {
    String groupId = createGroup("Concurrent Delete Group " + UUID.randomUUID(),
        "A sacrificial group for the repeated DELETE regression test");
    String path = "/groups/" + encode(groupId);
    HttpResponse<String> current = request("GET", path, null, authHeaderAdmin);
    Assertions.assertEquals(200, current.statusCode(), current.body());
    String etag = current.headers().firstValue("ETag").orElseThrow();

    List<Integer> statuses = concurrentDeleteStatuses(20,
        () -> request("DELETE", path, null, authHeaderAdmin, "application/json", etag).statusCode());

    Assertions.assertEquals(1, statuses.stream().filter(status -> status == 204).count(), statuses::toString);
    Assertions.assertTrue(statuses.stream().allMatch(status -> status == 204 || status == 404 || status == 412),
        () -> "concurrent DELETE returned a non-convergent status: " + statuses);
  }

  private static List<Integer> concurrentDeleteStatuses(int count,
                                                         java.util.concurrent.Callable<Integer> deletion)
      throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(count);
    CountDownLatch ready = new CountDownLatch(count);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> futures = new ArrayList<>(count);
    try {
      for (int i = 0; i < count; i++) {
        futures.add(executor.submit(() -> {
          ready.countDown();
          start.await();
          return deletion.call();
        }));
      }
      Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      List<Integer> statuses = new ArrayList<>(count);
      for (Future<Integer> future : futures) {
        statuses.add(future.get());
      }
      return statuses;
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void duplicateGroupNameIsRejected() throws Exception {
    String body = "{\"schema:name\": \"Duplicate Group\", \"schema:description\": \"first\"}";
    HttpResponse<String> first = request("POST", "/groups", body, authHeaderAdmin);
    Assertions.assertEquals(201, first.statusCode());
    HttpResponse<String> second = request("POST", "/groups", body, authHeaderAdmin);
    // 409 Conflict, not 400: the request is well formed and permitted, it collides with existing
    // state. A client can act on that distinction; it could not when both were 400.
    Assertions.assertEquals(409, second.statusCode());
    Assertions.assertTrue(second.body().contains("groupAlreadyPresent"));
  }

  /**
   * Merge-patch reads an explicit null as "remove this property". A group must always have a name, so
   * the request is refused — and refused as a bad request. It used to reach the update, where the null
   * name was lowercased for the NAME_LOWER property and threw, answering 500.
   */
  @Test
  public void patchCanNotRemoveTheGroupName() throws Exception {
    String groupId = createGroup("Patch Null Name Group", "a group whose name will be patched away");

    HttpResponse<String> patched = request("PATCH", "/groups/" + encode(groupId),
        "{\"schema:name\": null}", authHeaderAdmin, "application/merge-patch+json", "\"1\"");
    Assertions.assertEquals(400, patched.statusCode(), "removing the name should be refused: " + patched.body());

    HttpResponse<String> after = request("GET", "/groups/" + encode(groupId), null, authHeaderAdmin);
    Assertions.assertEquals("Patch Null Name Group",
        JsonMapper.MAPPER.readTree(after.body()).get("schema:name").asText(),
        "the refused patch must have left the name alone");
  }

  /**
   * A rename onto a sibling's name is the same collision {@link #duplicateGroupNameIsRejected} pins on
   * create, and it gets the same answer: 409 with the key, not the 400 the rename path used to give.
   * Asserted on both writing endpoints, which share the check.
   */
  @Test
  public void renamingOntoAnExistingNameIsRejectedAsConflict() throws Exception {
    createGroup("Rename Target Group", "the name that is already taken");
    String otherId = createGroup("Rename Source Group", "the group that will try to take it");

    HttpResponse<String> put = request("PUT", "/groups/" + encode(otherId),
        "{\"schema:name\": \"Rename Target Group\", \"schema:description\": \"still trying\"}",
        authHeaderAdmin, "application/json", "\"1\"");
    Assertions.assertEquals(409, put.statusCode(), "PUT rename collision: " + put.body());
    Assertions.assertTrue(put.body().contains("groupAlreadyPresent"), put.body());

    HttpResponse<String> patch = request("PATCH", "/groups/" + encode(otherId),
        "{\"schema:name\": \"Rename Target Group\"}", authHeaderAdmin,
        "application/merge-patch+json", "\"1\"");
    Assertions.assertEquals(409, patch.statusCode(), "PATCH rename collision: " + patch.body());
    Assertions.assertTrue(patch.body().contains("groupAlreadyPresent"), patch.body());
  }

  /**
   * Group names are unique without regard to case. The uniqueness lookup used to match the cased NAME
   * property, so a name differing only in case passed the check and both groups existed — indexed and
   * listed next to each other by the lowercase name they shared.
   */
  @Test
  public void groupNamesCollideRegardlessOfCase() throws Exception {
    createGroup("Case Sensitivity Group", "the original");

    HttpResponse<String> lowered = request("POST", "/groups",
        "{\"schema:name\": \"case sensitivity group\", \"schema:description\": \"the same name, lowercased\"}",
        authHeaderAdmin);
    Assertions.assertEquals(409, lowered.statusCode(), "a lowercased duplicate should collide: " + lowered.body());
    Assertions.assertTrue(lowered.body().contains("groupAlreadyPresent"), lowered.body());
  }

  /**
   * A membership request naming a user the graph does not hold is refused whole. The relation queries
   * match on id and affect nothing when the node is absent, so the unknown user used to be dropped in
   * silence while the rest of the request was applied and the caller was answered 200.
   */
  @Test
  public void memberUpdateNamingAnUnknownUserChangesNothing() throws Exception {
    String groupId = createGroup("Unknown Member Group", "a group to aim a bad membership at");
    String usersPath = "/groups/" + encode(groupId) + "/users";

    HttpResponse<String> before = request("GET", usersPath, null, authHeaderAdmin);
    Assertions.assertEquals(200, before.statusCode());
    String membershipBefore = before.body();
    String membershipEtag = before.headers().firstValue("ETag").orElseThrow();

    String adminId = JsonMapper.MAPPER.readTree(membershipBefore).get("users").get(0).get("user").get("@id").asText();
    String unknownId = "https://metadatacenter.orgx/users/00000000-0000-0000-0000-000000000000";
    HttpResponse<String> updated = request("PUT", usersPath,
        "{\"users\": ["
            + "{\"user\": {\"@id\": \"" + adminId + "\"}, \"administrator\": true, \"member\": true},"
            + "{\"user\": {\"@id\": \"" + unknownId + "\"}, \"administrator\": false, \"member\": true}"
            + "]}",
        authHeaderAdmin, "application/json", membershipEtag);
    Assertions.assertEquals(404, updated.statusCode(), "an unknown user should fail the request: " + updated.body());
    Assertions.assertTrue(updated.body().contains("userNotFound"), updated.body());

    HttpResponse<String> after = request("GET", usersPath, null, authHeaderAdmin);
    Assertions.assertEquals(membershipBefore, after.body(),
        "the refused membership change must have left the group untouched");
  }

  @Test
  public void membershipReplacementRequiresAndAdvancesItsOwnEtag() throws Exception {
    String groupId = createGroup("Membership ETag Group", "a group with versioned membership");
    String usersPath = "/groups/" + encode(groupId) + "/users";

    HttpResponse<String> initial = request("GET", usersPath, null, authHeaderAdmin);
    Assertions.assertEquals(200, initial.statusCode());
    String initialEtag = initial.headers().firstValue("ETag").orElseThrow();
    Assertions.assertEquals("\"1\"", initialEtag);

    String unchanged = initial.body();
    HttpResponse<String> missing = request("PUT", usersPath, unchanged, authHeaderAdmin);
    Assertions.assertEquals(428, missing.statusCode(), missing.body());

    HttpResponse<String> updated = request("PUT", usersPath, unchanged, authHeaderAdmin,
        "application/json", initialEtag);
    Assertions.assertEquals(200, updated.statusCode(), updated.body());
    Assertions.assertEquals("\"2\"", updated.headers().firstValue("ETag").orElseThrow());

    HttpResponse<String> stale = request("PUT", usersPath, unchanged, authHeaderAdmin,
        "application/json", initialEtag);
    Assertions.assertEquals(412, stale.statusCode(), stale.body());
    Assertions.assertEquals("\"2\"",
        JsonMapper.MAPPER.readTree(stale.body()).get("parameters").get("currentETag").asText());

    HttpResponse<String> wildcard = request("PUT", usersPath, unchanged, authHeaderAdmin,
        "application/json", "*");
    Assertions.assertEquals(200, wildcard.statusCode(), wildcard.body());
    Assertions.assertEquals("\"3\"", wildcard.headers().firstValue("ETag").orElseThrow());
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

  @Test
  public void everybodyGroupMembershipCanNotBeReplaced() throws Exception {
    HttpResponse<String> groups = request("GET", "/groups", null, authHeaderAdmin);
    JsonNode groupList = JsonMapper.MAPPER.readTree(groups.body()).get("groups");
    String everybodyId = null;
    for (JsonNode group : groupList) {
      if ("Everybody".equals(group.get("schema:name").asText())) {
        everybodyId = group.get("@id").asText();
      }
    }
    Assertions.assertNotNull(everybodyId, "The Everybody group must exist");

    String usersPath = "/groups/" + encode(everybodyId) + "/users";
    HttpResponse<String> before = request("GET", usersPath, null, authHeaderAdmin);
    Assertions.assertEquals(200, before.statusCode(), before.body());

    HttpResponse<String> replaced = request("PUT", usersPath, "{\"users\": []}",
        authHeaderAdmin, "application/json", "*");
    Assertions.assertEquals(400, replaced.statusCode(), replaced.body());

    HttpResponse<String> after = request("GET", usersPath, null, authHeaderAdmin);
    Assertions.assertEquals(200, after.statusCode(), after.body());
    Assertions.assertEquals(before.body(), after.body(),
        "the refused replacement must leave Everybody's membership unchanged");
    Assertions.assertEquals(before.headers().firstValue("ETag"), after.headers().firstValue("ETag"),
        "the refused replacement must not advance Everybody's membership revision");
  }

}
