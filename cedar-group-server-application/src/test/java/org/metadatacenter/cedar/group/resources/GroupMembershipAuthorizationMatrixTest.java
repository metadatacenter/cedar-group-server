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
import org.metadatacenter.server.security.model.auth.CedarGroupUserRequest;
import org.metadatacenter.server.security.model.auth.CedarGroupUsersRequest;
import org.metadatacenter.server.security.model.permission.resource.ResourcePermissionUser;
import org.metadatacenter.server.security.model.user.CedarUser;
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
 * Who may change a group's membership — asked of a member, not only of a stranger.
 *
 * <p>{@link GroupsAuthorizationMatrixTest} covers the stranger: someone with no relationship to the
 * group is refused. The untested actor is the plain member, and it is the one that matters, because
 * group membership is the widest lever in the sharing model. A grant to a group applies to whoever is
 * in it at the time of asking, so whoever can edit the membership can hand out access to every
 * resource ever shared with that group — without touching those resources' ACLs and without their
 * owners being involved. Whether that power stops at the administrators is therefore a security
 * boundary, and it was unasserted.
 *
 * <p>The endpoint gates on {@code userAdministersGroup(gid) || <system permission>} and answers
 * Forbidden, so the expectation is that a member is refused exactly as a stranger is. The
 * administrator's own success is asserted alongside, because a table of refusals cannot show whether
 * the endpoint discriminates or is simply broken for everyone.
 *
 * <p>The request body is serialized from the real {@link CedarGroupUsersRequest} rather than written
 * by hand, and it restates the membership unchanged so that the administrator's row is idempotent and
 * cannot disturb the rows around it. Both properties matter: a body the endpoint rejects as malformed
 * would be answered 400 before authority was considered, and every row would then pass while
 * asserting nothing about who may change a membership.
 */
public class GroupMembershipAuthorizationMatrixTest {

  static {
    // Must run before the test support boots the server, which reads the Neo4j env vars. Ports are
    // assigned by the OS, so they cannot collide with the dev server or the other test in this module.
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
  private static String unchangedMembershipBody;

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

    CedarUser adminUser = TestAuthUtil.getAdminUser(cedarConfig);
    CedarUser user2 = TestAuthUtil.getTestUser2(cedarConfig);

    // Group creation needs a system permission, so the admin creates it and administers it.
    HttpResponse<String> created = send("POST", "/groups",
        "{\"schema:name\": \"Membership Matrix Group\", \"schema:description\": \"membership matrix fixture\"}",
        adminHeader);
    Assertions.assertEquals(201, created.statusCode(), "fixture group was not created: " + created.body());
    String groupId = created.body().replaceAll("(?s).*\"@id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    Assertions.assertTrue(groupId.startsWith("http"), "could not read the fixture group id from: " + created.body());
    String encoded = URLEncoder.encode(groupId, StandardCharsets.UTF_8);
    groupPath = "/groups/" + encoded;
    groupUsersPath = groupPath + "/users";

    // The membership under test: the admin administers, test user 2 is a plain member. Sent as the
    // body of every row below as well, so the administrator's row asks for no change.
    CedarGroupUsersRequest membership = new CedarGroupUsersRequest();
    membership.getUsers().add(new CedarGroupUserRequest(new ResourcePermissionUser(adminUser.getId()), true, true));
    membership.getUsers().add(new CedarGroupUserRequest(new ResourcePermissionUser(user2.getId()), false, true));
    unchangedMembershipBody = JsonMapper.MAPPER.writeValueAsString(membership);

    HttpResponse<String> seeded = send("PUT", groupUsersPath, unchangedMembershipBody, adminHeader, "*");
    Assertions.assertEquals(200, seeded.statusCode(),
        "the fixture membership was not established: " + seeded.body());
  }

  @AfterAll
  public static void oneTimeTearDown() {
    SERVER.after();
  }

  @Test
  public void onlyAnAdministratorMayChangeTheMembership() throws Exception {
    PermissionMatrix matrix = new PermissionMatrix("http://localhost:" + SERVER.getLocalPort(), actors);

    // The boundary. OTHER_USER is a member of this group, so this row is the one the existing matrix
    // could not ask: being in the group must not carry the power to decide who else is.
    matrix.when("PUT", groupUsersPath, unchangedMembershipBody)
        .header("If-Match", "*")
        .expect(ANONYMOUS, 401)
        .expect(OTHER_USER, 403)   // a member, and refused
        .expect(OWNER, 403)        // neither member nor administrator
        .expect(ADMIN, 200);       // administers the group, and the body changes nothing

    // Reads, and the answer is stricter than expected: membership confers no read access at all. A
    // member is refused both the group and its membership list, exactly as the non-member OWNER is —
    // GroupsAuthorizationMatrixTest pins the same 403 for a stranger. So a user can be in a group,
    // reach resources through it, and be unable to see the group or discover who else is in it.
    //
    // Pinned to the single code rather than accepting "200 or 403". An expectation that accepts either
    // asserts almost nothing, and that is not hypothetical: the re-share row in
    // FolderPermissionLevelMatrixTest looked like a passing denial until its single expected code
    // forced the question, at which point it turned out to be an escalation.
    matrix.when("GET", groupUsersPath)
        .expect(ANONYMOUS, 401)
        .expect(ADMIN, 200)
        .expect(OTHER_USER, 403);

    matrix.when("GET", groupPath)
        .expect(ANONYMOUS, 401)
        .expect(ADMIN, 200)
        .expect(OTHER_USER, 403);

    matrix.verify();

    // The refusals must have changed nothing: user 2 should still be a member, and no one else added.
    // Read as the administrator, who is definitely allowed to look.
    HttpResponse<String> after = send("GET", groupUsersPath, null, actors.get(ADMIN));
    Assertions.assertEquals(200, after.statusCode(), "the administrator should be able to read the membership");
    Assertions.assertEquals(2, countOccurrences(after.body(), "\"@id\""),
        "the membership should still hold exactly the administrator and the one member: " + after.body());
  }

  private static int countOccurrences(String haystack, String needle) {
    int count = 0;
    int at = haystack.indexOf(needle);
    while (at >= 0) {
      count++;
      at = haystack.indexOf(needle, at + needle.length());
    }
    return count;
  }

  private static HttpResponse<String> send(String method, String path, String body, String authHeader)
      throws Exception {
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

}
