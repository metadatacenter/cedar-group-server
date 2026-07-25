package org.metadatacenter.cedar.group;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metadatacenter.util.test.TestUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Boots the real application through Dropwizard test support and exercises the wiring no
 * backend is needed for: the index resource must serve, and a protected endpoint must reject an
 * unauthenticated request through the CEDAR auth machinery. This catches configuration and
 * startup rot that a config-only test cannot see.
 */
public class GroupServerApplicationSmokeTest {

  static {
    // Must run before the test support boots the server, which reads the port env vars.
    // Alternate server ports, so the test instance never collides with a running dev server.
    Map<String, String> environment = new HashMap<>(System.getenv());
    environment.put("CEDAR_GROUP_HTTP_PORT", "19009");
    environment.put("CEDAR_GROUP_ADMIN_PORT", "19109");
    environment.put("CEDAR_GROUP_STOP_PORT", "19209");
    TestUtil.setEnv(environment);
  }

  public static final DropwizardTestSupport<GroupServerConfiguration> SERVER =
      new DropwizardTestSupport<>(GroupServerApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));

  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  @BeforeAll
  public static void oneTimeSetUp() {
    SERVER.before();
  }

  @AfterAll
  public static void oneTimeTearDown() {
    SERVER.after();
  }

  private HttpResponse<String> get(String path, String... headers) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + path))
        .GET();
    if (headers.length > 0) {
      request.headers(headers);
    }
    return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  public void indexIsServed() throws Exception {
    HttpResponse<String> response = get("/");
    Assertions.assertEquals(200, response.statusCode());
    Assertions.assertTrue(response.body().contains("name"));
  }

  @Test
  public void protectedEndpointRejectsMissingCredentials() throws Exception {
    HttpResponse<String> response = get("/groups");
    Assertions.assertEquals(401, response.statusCode());
  }

}
