package org.metadatacenter.rest.context;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.rest.assertion.noun.ICedarParameter;
import org.metadatacenter.rest.assertion.noun.ICedarRequestBody;

public class PlayRequestJsonBody implements ICedarRequestBody {

  private JsonNode bodyNode;

  public PlayRequestJsonBody(JsonNode bodyNode) {
    this.bodyNode = bodyNode;
  }

  @Override
  public ICedarParameter get(String name) {
    CedarParameter p = new CedarParameter(name, CedarParameterSource.JsonBody);
    if (bodyNode != null) {
      JsonNode jsonNode = bodyNode.get(name);
      if (jsonNode != null && !jsonNode.isMissingNode()) {
        p.setJsonNode(jsonNode);
      }
    }
    return p;
  }
}
