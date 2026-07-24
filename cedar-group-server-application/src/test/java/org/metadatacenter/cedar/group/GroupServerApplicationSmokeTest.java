package org.metadatacenter.cedar.group;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Boots the real application through the Dropwizard test rule and exercises the wiring no
 * backend is needed for: the index resource must serve, and a protected endpoint must reject an
 * unauthenticated request through the CEDAR auth machinery. This catches configuration and
 * startup rot that a config-only test cannot see.
 */
public class GroupServerApplicationSmokeTest {

  @ClassRule
  public static final DropwizardAppRule<GroupServerConfiguration> SERVER =
      new DropwizardAppRule<>(GroupServerApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));

  private static final HttpClient CLIENT = HttpClient.newHttpClient();

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
    Assert.assertEquals(200, response.statusCode());
    Assert.assertTrue(response.body().contains("name"));
  }

  @Test
  public void protectedEndpointRejectsMissingCredentials() throws Exception {
    HttpResponse<String> response = get("/groups");
    Assert.assertEquals(401, response.statusCode());
  }

}
