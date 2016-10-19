package org.metadatacenter.rest.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.rest.assertion.noun.CedarParameter;
import org.metadatacenter.rest.assertion.noun.CedarParameterImpl;
import org.metadatacenter.rest.assertion.noun.CedarRequestBody;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.util.json.JsonMapper;

public class PlayRequestJsonBody implements CedarRequestBody {

  private final JsonNode bodyNode;

  public PlayRequestJsonBody(JsonNode bodyNode) {
    this.bodyNode = bodyNode;
  }

  @Override
  public CedarParameter get(String name) {
    CedarParameterImpl p = new CedarParameterImpl(name, CedarParameterSource.JsonBody);
    if (bodyNode != null) {
      JsonNode jsonNode = bodyNode.get(name);
      if (jsonNode != null && !jsonNode.isMissingNode()) {
        p.setJsonNode(jsonNode);
      }
    }
    return p;
  }

  @Override
  public <T> T as(Class<T> type) throws CedarAssertionException {
    T object;
    try {
      object = JsonMapper.MAPPER.treeToValue(bodyNode, type);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new CedarAssertionException(e);
    }
    return object;
  }
}
