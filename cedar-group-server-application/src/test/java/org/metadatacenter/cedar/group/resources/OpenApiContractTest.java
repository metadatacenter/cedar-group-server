package org.metadatacenter.cedar.group.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiContractTest {

  @Test
  void groupBodiesAndResponsesAreTyped() throws IOException {
    JsonNode spec = readSpec();
    assertBody(spec, "/groups", "post", "application/json", "GroupWriteRequest");
    assertBody(spec, "/groups/{id}", "put", "application/json", "GroupWriteRequest");
    assertBody(spec, "/groups/{id}", "patch", "application/merge-patch+json", "GroupPatchRequest");
    assertBody(spec, "/groups/{id}/users", "put", "application/json", "GroupMembership");

    assertResponse(spec, "/groups", "get", "200", "GroupList");
    assertResponse(spec, "/groups", "post", "201", "Group");
    assertResponse(spec, "/groups/{id}", "get", "200", "Group");
    assertResponse(spec, "/groups/{id}", "put", "200", "Group");
    assertResponse(spec, "/groups/{id}", "patch", "200", "Group");
    assertResponse(spec, "/groups/{id}/users", "get", "200", "GroupMembership");
    assertResponse(spec, "/groups/{id}/users", "put", "200", "GroupMembership");

    assertTrue(spec.at("/components/schemas/GroupWriteRequest/required").toString().contains("schema:name"));
    assertTrue(spec.at("/components/schemas/GroupMembership/properties/users/items/$ref").asText()
        .endsWith("/GroupMember"));
    JsonNode groupProperties = spec.at("/components/schemas/Group/properties");
    assertTrue(groupProperties.has("pav:createdOn"));
    assertTrue(groupProperties.has("pav:lastUpdatedOn"));
    assertTrue(groupProperties.has("pav:createdBy"));
    assertTrue(groupProperties.has("oslc:modifiedBy"));
    assertFalse(groupProperties.has("createdOn"));
    assertFalse(groupProperties.has("lastUpdatedOn"));
  }

  private static void assertBody(JsonNode spec, String path, String method, String mediaType, String schema) {
    JsonNode body = spec.path("paths").path(path).path(method).path("requestBody");
    assertTrue(body.path("required").asBoolean(), path + " " + method);
    assertEquals("#/components/schemas/" + schema,
        body.path("content").path(mediaType).path("schema").path("$ref").asText());
  }

  private static void assertResponse(JsonNode spec, String path, String method, String status, String schema) {
    assertEquals("#/components/schemas/" + schema,
        spec.path("paths").path(path).path(method).path("responses").path(status)
            .path("content").path("application/json").path("schema").path("$ref").asText());
  }

  private static JsonNode readSpec() throws IOException {
    try (InputStream input = OpenApiContractTest.class.getResourceAsStream("/assets/swagger-api/swagger.json")) {
      assertNotNull(input, "generated OpenAPI document");
      return JsonMapper.MAPPER.readTree(input);
    }
  }
}
