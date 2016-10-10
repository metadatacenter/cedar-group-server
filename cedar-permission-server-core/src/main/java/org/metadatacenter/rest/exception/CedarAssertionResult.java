package org.metadatacenter.rest.exception;

import java.util.HashMap;
import java.util.Map;

public class CedarAssertionResult {

  public static int HTTP_BAD_REQUEST = 400;
  public static int HTTP_UNAUTHORIZED = 401;
  public static int HTTP_FORBIDDEN = 403;
  public static int HTTP_NOT_FOUND = 404;
  public static int HTTP_INTERNAL_SERVER_ERROR = 500;

  private int code;
  private String message;
  private String errorSubType = "other";
  private String suggestedAction = "none";
  private String errorCode = null;
  private Map<String, Object> parameters;

  public CedarAssertionResult(String message) {
    this.message = message;
    code = HTTP_INTERNAL_SERVER_ERROR;
    parameters = new HashMap<>();
  }

  public CedarAssertionResult internalServerError() {
    code = HTTP_INTERNAL_SERVER_ERROR;
    return this;
  }

  public CedarAssertionResult forbidden() {
    code = HTTP_FORBIDDEN;
    return this;
  }

  public CedarAssertionResult unauthorized() {
    code = HTTP_UNAUTHORIZED;
    return this;
  }

  public CedarAssertionResult notFound() {
    code = HTTP_NOT_FOUND;
    return this;
  }

  public CedarAssertionResult badRequest() {
    code = HTTP_BAD_REQUEST;
    return this;
  }

  public CedarAssertionResult setParameter(String name, Object value) {
    this.parameters.put(name, value);
    return this;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public int getCode() {
    return code;
  }

  public String getErrorSubType() {
    return errorSubType;
  }

  public String getSuggestedAction() {
    return suggestedAction;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }
}