package org.metadatacenter.cedar.group;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.TestAuthUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
    // OS-assigned server ports, so the test instance never collides with a running dev server.
    Map<String, String> environment = new HashMap<>(CedarEnvironmentSource.getAll());
    environment.put("CEDAR_GROUP_HTTP_PORT", "0");
    environment.put("CEDAR_GROUP_ADMIN_PORT", "0");
    environment.put("CEDAR_GROUP_STOP_PORT", "0");
    environment.put("CEDAR_NEO4J_HOST", "127.0.0.1");
    environment.put("CEDAR_NEO4J_BOLT_PORT", "1");
    CedarEnvironmentSource.setOverride(environment);
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
  }

  @AfterAll
  public static void oneTimeTearDown() {
    SERVER.after();
  }

  private HttpResponse<String> get(String path, String... headers) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + path))
        .timeout(Duration.ofSeconds(5))
        .GET();
    if (headers.length > 0) {
      request.headers(headers);
    }
    return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  /**
   * The group server ships an API spec, so it advertises the documentation links and serves the
   * document.
   *
   * <p>It held the quiet side of the documentation gate until its resource classes were annotated.
   * That side is now held by {@code SchemaServerApplicationSmokeTest}: the schema server declares no
   * resource classes at all, so it has nothing to document and will stay on that side.
   */
  @Test
  public void apiDocumentationIsAdvertisedAndServed() throws Exception {
    Assertions.assertTrue(get("/").body().contains("apiDocs"),
        "A service with a spec should advertise its documentation");

    HttpResponse<String> spec = get("/swagger-api/swagger.json");
    Assertions.assertEquals(200, spec.statusCode(), "The advertised spec path should serve the document");
    Assertions.assertTrue(spec.body().contains("openapi"), "The document served should be an OpenAPI spec");
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

  @Test
  public void graphOutageReturnsSanitizedServiceUnavailable() throws Exception {
    HttpResponse<String> response = get("/groups", "Authorization", authHeaderAdmin);

    Assertions.assertEquals(503, response.statusCode(), response.body());
    JsonNode error = JsonMapper.MAPPER.readTree(response.body());
    Assertions.assertEquals("SERVICE_UNAVAILABLE", error.path("status").asText(), response.body());
    Assertions.assertEquals("Neo4j is unavailable", error.path("message").asText(), response.body());
    Assertions.assertTrue(error.path("originalException").isMissingNode()
        || error.path("originalException").isNull(), response.body());
    Assertions.assertTrue(error.path("sourceException").isMissingNode()
        || error.path("sourceException").isNull(), response.body());
    Assertions.assertFalse(response.body().contains("127.0.0.1"), response.body());
  }

}
