package org.metadatacenter.rest.exception;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.util.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

public class CedarAssertionException extends Exception {

  private static int HTTP_UNAUTHORIZED = 401;
  private static int HTTP_FORBIDDEN = 403;
  private static int HTTP_INTERNAL_SERVER_ERROR = 500;

  private int code;
  private String errorSubType = "other";
  private String suggestedAction = "none";
  private String errorCode = null;
  private Map<String, Object> params;

  public CedarAssertionException(String message) {
    super(message);
    code = HTTP_INTERNAL_SERVER_ERROR;
    params = new HashMap<>();
  }

  public CedarAssertionException internalServerError() {
    code = HTTP_INTERNAL_SERVER_ERROR;
    return this;
  }

  public CedarAssertionException forbidden() {
    code = HTTP_FORBIDDEN;
    return this;
  }

  public CedarAssertionException unauthorized() {
    code = HTTP_UNAUTHORIZED;
    return this;
  }

  public int getCode() {
    return code;
  }

  public ObjectNode asJson() {
    ObjectNode objectNode = JsonMapper.MAPPER.createObjectNode();
    addExceptionData(objectNode, this, "cedarAssertionFramework");

    objectNode.put("errorSubType", errorSubType);
    objectNode.put("errorCode", errorCode);
    objectNode.put("suggestedAction", suggestedAction);

    ObjectNode errorParams = JsonMapper.MAPPER.createObjectNode();
    for (String key : params.keySet()) {
      errorParams.set(key, JsonMapper.MAPPER.valueToTree(params.get(key)));
    }
    objectNode.set("errorParams", errorParams);

    return objectNode;
  }


  public static ObjectNode asJson(Exception ex) {
    ObjectNode objectNode = JsonMapper.MAPPER.createObjectNode();
    addExceptionData(objectNode, ex, "cedarServer");
    return objectNode;
  }

  private static void addExceptionData(ObjectNode objectNode, Exception ex, String errorSource) {
    objectNode.put("errorSource", errorSource);
    objectNode.put("errorType", "exception");
    objectNode.put("message", ex.getMessage());
    objectNode.put("localizedMessage", ex.getLocalizedMessage());
    objectNode.put("string", ex.toString());

    ArrayNode jsonST = objectNode.putArray("stackTrace");
    for (StackTraceElement ste : ex.getStackTrace()) {
      jsonST.add(ste.toString());
    }
  }


}