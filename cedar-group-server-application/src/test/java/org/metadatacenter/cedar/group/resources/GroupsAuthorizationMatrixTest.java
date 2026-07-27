package org.metadatacenter.cedar.group.resources;

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
import org.metadatacenter.util.test.EmbeddedCedarNeo4j;
import org.metadatacenter.util.test.PermissionMatrix;
import org.metadatacenter.util.test.TestAuthUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.metadatacenter.util.test.PermissionMatrix.Actor.ADMIN;
import static org.metadatacenter.util.test.PermissionMatrix.Actor.ANONYMOUS;
import static org.metadatacenter.util.test.PermissionMatrix.Actor.OTHER_USER;
import static org.metadatacenter.util.test.PermissionMatrix.Actor.OWNER;

/**
 * The group server's authorization grid, as a table.
 *
 * <p>Group management is administrative: creating, updating and deleting a group each require a
 * GROUP_* permission that only the group-administrator role carries. An ordinary user — the test
 * users hold default-user, template-creator and metadata-creator — must be refused. Nothing
 * previously asserted that, so the permission gate could have been dropped from any of these
 * endpoints without a test noticing.
 *
 * <p>The table deliberately asserts the <em>denial</em> cells: unauthenticated must be 401, and an
 * ordinary user must be 403. Those probes are side-effect free, which is what makes it safe to sweep
 * every endpoint in one test. The permitted paths mutate state (creating and deleting groups), so
 * they stay in {@link GroupsResourceTest}'s lifecycle tests rather than being repeated here; the
 * read-only listing is the one place this test also checks an allowed actor.
 */
public class GroupsAuthorizationMatrixTest {

  static {
    // Must run before the test support boots the server, which reads the Neo4j env vars. Ports are
    // distinct from the dev server and from every other booting test class. Redis goes to a dead
    // port: no endpoint under test depends on a live Redis.
    EmbeddedCedarNeo4j.startAndRedirectEnvironment(Map.of(
        "CEDAR_GROUP_HTTP_PORT", "19026",
        "CEDAR_GROUP_ADMIN_PORT", "19126",
        "CEDAR_GROUP_STOP_PORT", "19226",
        "CEDAR_REDIS_PERSISTENT_PORT", "1"));
  }

  public static final DropwizardTestSupport<GroupServerConfiguration> SERVER =
      new DropwizardTestSupport<>(GroupServerApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));

  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  private static Map<PermissionMatrix.Actor, String> actors;
  private static String groupPath;
  private static String groupUsersPath;

  @BeforeAll
  public static void oneTimeSetUp() throws Exception {
    SERVER.before();
    Map<String, String> environment = CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_GROUP);
    CedarConfig cedarConfig = CedarConfig.getInstance(environment);
    TestAuthUtil.installInMemoryUserService(cedarConfig);
    EmbeddedCedarNeo4j.seed(cedarConfig);

    String adminHeader = TestAuthUtil.getAdminUserAuthHeader(cedarConfig);
    actors = Map.of(
        OWNER, TestAuthUtil.getTestUser1AuthHeader(cedarConfig),
        OTHER_USER, TestAuthUtil.getTestUser2AuthHeader(cedarConfig),
        ADMIN, adminHeader);

    // One real group to aim the by-id rows at, created by the only actor allowed to create one.
    HttpResponse<String> created = send("POST", "/groups",
        "{\"schema:name\": \"Matrix Fixture Group\", \"schema:description\": \"authorization matrix fixture\"}",
        adminHeader);
    Assertions.assertEquals(201, created.statusCode(), "fixture group was not created: " + created.body());
    String groupId = created.body().replaceAll("(?s).*\"@id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    Assertions.assertTrue(groupId.startsWith("http"), "could not read the fixture group id from: " + created.body());
    String encoded = URLEncoder.encode(groupId, StandardCharsets.UTF_8);
    groupPath = "/groups/" + encoded;
    groupUsersPath = "/groups/" + encoded + "/users";
  }

  @AfterAll
  public static void oneTimeTearDown() {
    SERVER.after();
  }

  private static HttpResponse<String> send(String method, String path, String body, String authHeader) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + path))
        .header("Content-Type", "application/json");
    if (authHeader != null) {
      builder.header("Authorization", authHeader);
    }
    builder.method(method, body == null
        ? HttpRequest.BodyPublishers.noBody()
        : HttpRequest.BodyPublishers.ofString(body));
    return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  public void groupEndpointsEnforceTheirPermissions() throws Exception {
    String groupBody = "{\"schema:name\": \"Should Never Exist\", \"schema:description\": \"denied\"}";
    PermissionMatrix matrix = new PermissionMatrix("http://localhost:" + SERVER.getLocalPort(), actors);

    // Group management is administrative in its entirety: every endpoint requires a GROUP_*
    // permission, reads included (GROUP_READ), and only the group-administrator role carries them.
    // So an ordinary user is refused even for listing — the reads are the rows where ADMIN is also
    // asserted, since reading changes nothing.
    matrix.when("GET", "/groups")
        .expect(ANONYMOUS, 401)
        .expect(OWNER, 403)
        .expect(OTHER_USER, 403)
        .expect(ADMIN, 200);

    matrix.when("GET", groupPath)
        .expect(ANONYMOUS, 401)
        .expect(OWNER, 403)
        .expect(OTHER_USER, 403)
        .expect(ADMIN, 200);

    matrix.when("GET", groupUsersPath)
        .expect(ANONYMOUS, 401)
        .expect(OWNER, 403)
        .expect(OTHER_USER, 403)
        .expect(ADMIN, 200);

    // The mutating rows omit ADMIN on purpose: those would change state, and the lifecycle tests in
    // GroupsResourceTest cover the permitted paths.
    matrix.when("POST", "/groups", groupBody)
        .expect(ANONYMOUS, 401)
        .expect(OWNER, 403)
        .expect(OTHER_USER, 403);

    matrix.when("PUT", groupPath, groupBody)
        .expect(ANONYMOUS, 401)
        .expect(OWNER, 403)
        .expect(OTHER_USER, 403);

    matrix.when("DELETE", groupPath)
        .expect(ANONYMOUS, 401)
        .expect(OWNER, 403)
        .expect(OTHER_USER, 403);

    matrix.when("PUT", groupUsersPath, "{\"users\": []}")
        .expect(ANONYMOUS, 401)
        .expect(OWNER, 403)
        .expect(OTHER_USER, 403);

    matrix.verify();

    // Statuses alone would not prove the denials had no effect, so check the fixture survived. This
    // runs in the same method rather than a second test because it is only meaningful after the
    // probes above, and JUnit does not guarantee method order.
    HttpResponse<String> survivor = send("GET", groupPath, null, actors.get(ADMIN));
    Assertions.assertEquals(200, survivor.statusCode(),
        "the fixture group should have survived the denied requests");
    Assertions.assertTrue(survivor.body().contains("Matrix Fixture Group"),
        "a request that should have been denied altered the fixture group: " + survivor.body());
  }

}
