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
 * <p>Two different things gate a group endpoint, and only the second one narrows anything. The first
 * is a GROUP_* permission, which comes from the groupAdministrator role — a name that suggests a
 * restricted role and is not one: the blueprint user profile grants it to every account the estate
 * creates, so all four GROUP_* permissions are universal and every gate spelled in terms of them
 * admits every logged-in caller. The second is the per-group check that the caller administers this
 * particular group, and it is what actually separates the actors below.
 *
 * <p>So a group's own record is readable by any authenticated account, while its roster and every
 * write are refused to everyone but the group's own administrators. The table asserted a blanket
 * refusal until the fixture users were built from the blueprint rather than from a hand-written list
 * of three roles: they lacked groupAdministrator, were refused everywhere, and the refusal was
 * recorded here as the intended design. Every cell an ordinary account occupies below is therefore
 * load-bearing, and each 403 now comes from the administrator check rather than from a permission
 * the actor never held, which is the boundary worth guarding.
 *
 * <p>The rows that mutate carry {@code If-Match} where the handler demands a precondition before it
 * considers authority, so a refusal is a refusal and not a 428 wearing its clothes. Creating a group
 * has left the table altogether: an ordinary account may do it, so the cell has an effect and cannot
 * sit in a sweep whose safety rests on the probes changing nothing. It is asserted on its own below,
 * with the group it creates deleted afterwards.
 */
public class GroupsAuthorizationMatrixTest {

  static {
    // Must run before the test support boots the server, which reads the Neo4j env vars. Ports are
    // assigned by the OS, so they cannot collide with the dev server or another test. Redis goes to a dead
    // port: no endpoint under test depends on a live Redis.
    EmbeddedCedarNeo4j.startAndRedirectEnvironment(Map.of(
        "CEDAR_GROUP_HTTP_PORT", "0",
        "CEDAR_GROUP_ADMIN_PORT", "0",
        "CEDAR_GROUP_STOP_PORT", "0",
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
    return send(method, path, body, authHeader, null);
  }

  private static HttpResponse<String> send(String method, String path, String body, String authHeader,
                                           String ifMatch) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + path))
        .header("Content-Type", "application/json");
    if (authHeader != null) {
      builder.header("Authorization", authHeader);
    }
    if (ifMatch != null) {
      builder.header("If-Match", ifMatch);
    }
    builder.method(method, body == null
        ? HttpRequest.BodyPublishers.noBody()
        : HttpRequest.BodyPublishers.ofString(body));
    return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  public void groupReadsAreOpenAndWritesAreRestrictedToAdministrators() throws Exception {
    String groupBody = "{\"schema:name\": \"Should Never Exist\", \"schema:description\": \"denied\"}";
    PermissionMatrix matrix = new PermissionMatrix("http://localhost:" + SERVER.getLocalPort(), actors);

    // Reads split in two. GROUP_READ is universal, so it decides nothing; what a caller may read is
    // decided by whether the answer names people. A group's own record does not, and stays open so
    // it can be chosen as the target of a share. Its roster does, and is restricted to the group's
    // administrators. Both ordinary actors are strangers to the fixture: OWNER created nothing here
    // and OTHER_USER is not a member.
    matrix.when("GET", "/groups")
        .expect(ANONYMOUS, 401)
        .expect(OWNER, 200)
        .expect(OTHER_USER, 200)
        .expect(ADMIN, 200);

    matrix.when("GET", groupPath)
        .expect(ANONYMOUS, 401)
        .expect(OWNER, 200)
        .expect(OTHER_USER, 200)
        .expect(ADMIN, 200);

    matrix.when("GET", groupUsersPath)
        .expect(ANONYMOUS, 401)
        .expect(OWNER, 403)
        .expect(OTHER_USER, 403)
        .expect(ADMIN, 200);

    // Creating carries only the anonymous cell here. An authenticated account is allowed to create,
    // which would leave a group behind and break the sweep's one safety property.
    matrix.when("POST", "/groups", groupBody)
        .expect(ANONYMOUS, 401);

    // Writes against a group the actor does not administer. Each of these actors holds GROUP_UPDATE
    // and GROUP_DELETE, so the permission gate passes and the 403 comes from the per-group
    // administrator check — the escalation that matters, since a group's membership decides who
    // reaches everything ever shared with it.
    //
    // PUT carries If-Match because that handler returns 428 for a missing precondition before it
    // looks at authority. Without the header the row would pass on a 4xx that says nothing about who
    // may write. DELETE and the membership replacement check authority first, so they are probed as
    // a client with no ETag in hand would send them.
    matrix.when("PUT", groupPath, groupBody)
        .header("If-Match", "*")
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

  /**
   * Creating a group is open to every account, and the creator administers what it created. This is
   * the cell the matrix cannot hold, because it leaves a group behind; the group is deleted again
   * here so the rest of the class still runs against the fixtures it set up.
   *
   * <p>It is also what makes the write refusals above mean something. Each of those actors can
   * create a group and administer it, so their 403 against the fixture is a statement about that
   * group rather than about their standing on the server.
   */
  @Test
  public void anOrdinaryAccountMayCreateAGroupAndAdministersWhatItCreates() throws Exception {
    String name = "Matrix Group Made By An Ordinary Account";
    String owner = actors.get(OWNER);

    HttpResponse<String> created = send("POST", "/groups",
        "{\"schema:name\": \"" + name + "\", \"schema:description\": \"created by an ordinary account\"}",
        owner);
    Assertions.assertEquals(201, created.statusCode(),
        "an ordinary account holds GROUP_CREATE and must be able to create a group: " + created.body());

    String id = JsonMapper.MAPPER.readTree(created.body()).get("@id").asText();
    String path = "/groups/" + URLEncoder.encode(id, StandardCharsets.UTF_8);
    String etag = created.headers().firstValue("ETag").orElse(null);
    try {
      HttpResponse<String> members = send("GET", path + "/users", null, owner);
      Assertions.assertEquals(200, members.statusCode(),
          "the creator administers this group, so the roster restriction must not shut it out: "
              + members.body());
      JsonNode record = JsonMapper.MAPPER.readTree(members.body()).get("users").get(0);
      Assertions.assertTrue(record.get("administrator").asBoolean(),
          "the creator must administer what it created, or no one could ever manage the group: "
              + members.body());

      HttpResponse<String> byOther = send("PUT", path,
          "{\"schema:name\": \"" + name + " renamed\", \"schema:description\": \"by a stranger\"}",
          actors.get(OTHER_USER), "*");
      Assertions.assertEquals(403, byOther.statusCode(),
          "a second ordinary account must not write a group it does not administer: " + byOther.body());
    } finally {
      HttpResponse<String> deleted = send("DELETE", path, null, owner, etag);
      Assertions.assertEquals(204, deleted.statusCode(),
          "the group this test created was left behind: " + deleted.body());
    }
  }

  /**
   * Who may read a group's membership, and the two decisions that answer rests on.
   *
   * <p>The listing at {@code GET /groups} stays open. A group has to be visible to be chosen as the
   * target of a share, so restricting the listing would break sharing with a group one does not
   * belong to. Names are the lesser disclosure and the one the product needs.
   *
   * <p>Reading a roster requires administering that group, not belonging to it. Membership is not a
   * sufficient test, because the everybody group has every account in it: a rule phrased as "a
   * member or an administrator may read" would leave the everybody roster — the whole user
   * directory, which is the disclosure that matters — readable by everyone, and would close
   * nothing. The everybody group is asserted separately for that reason, and it is the case to
   * preserve if this rule is ever revisited.
   */
  @Test
  public void aGroupRosterIsReadableOnlyByAnAdministratorOfThatGroup() throws Exception {
    String stranger = actors.get(OTHER_USER);

    Assertions.assertEquals(200, send("GET", "/groups", null, stranger).statusCode(),
        "the listing stays open: a group must be visible to be shared with");

    Assertions.assertEquals(403, send("GET", groupUsersPath, null, stranger).statusCode(),
        "an account that neither administers nor belongs to this group read its membership");

    Assertions.assertEquals(403, send("GET", everybodyUsersPath(), null, stranger).statusCode(),
        "the everybody roster is the deployment's user directory, so belonging to it cannot be what "
            + "authorizes reading it");

    Assertions.assertEquals(200, send("GET", groupUsersPath, null, actors.get(ADMIN)).statusCode(),
        "an administrator must still read the roster, or the refusals above would pass on an "
            + "endpoint that is simply broken");
  }

  /** The everybody group's membership path, found by its marker rather than by its configurable name. */
  private static String everybodyUsersPath() throws Exception {
    HttpResponse<String> listed = send("GET", "/groups", null, actors.get(ADMIN));
    Assertions.assertEquals(200, listed.statusCode(), listed.body());
    for (JsonNode group : JsonMapper.MAPPER.readTree(listed.body()).get("groups")) {
      JsonNode marker = group.get("specialGroup");
      if (marker != null && "EVERYBODY".equals(marker.asText())) {
        return "/groups/" + URLEncoder.encode(group.get("@id").asText(), StandardCharsets.UTF_8) + "/users";
      }
    }
    throw new AssertionError("no group carries the EVERYBODY marker: " + listed.body());
  }

}
