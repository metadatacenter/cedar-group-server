package org.metadatacenter.rest.context;

public enum CedarParameterSource {

  JsonBody("JsonBody"),
  QueryString("QueryString");

  private String value;

  CedarParameterSource(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
