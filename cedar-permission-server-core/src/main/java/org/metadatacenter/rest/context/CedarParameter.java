package org.metadatacenter.rest.context;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.rest.assertion.noun.ParameterNoun;

public class CedarParameter extends ParameterNoun {

  private String name;
  private CedarParameterSource source;
  private JsonNode jsonNode;

  public CedarParameter(String name, CedarParameterSource source) {
    this.name = name;
    this.source = source;
  }

  public void setJsonNode(JsonNode jsonNode) {
    this.jsonNode = jsonNode;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public CedarParameterSource getSource() {
    return source;
  }

  @Override
  public String stringValue() {
    if (jsonNode != null && !jsonNode.isMissingNode()) {
      return jsonNode.asText();
    } else {
      return null;
    }
  }

  public boolean isNull() {
    return isMissing() || jsonNode.isNull();
  }

  public boolean isMissing() {
    return jsonNode == null || jsonNode.isMissingNode();
  }

}
