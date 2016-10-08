package org.metadatacenter.rest.exception;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.util.json.JsonMapper;

public class CedarAssertionException extends Exception {

  public static final String ERROR_SUB_TYPE = "errorSubType";
  public static final String ERROR_CODE = "errorCode";
  public static final String SUGGESTED_ACTION = "suggestedAction";
  public static final String ERROR_PARAMS = "errorParams";
  public static final String ERROR_SOURCE = "errorSource";
  public static final String ERROR_TYPE = "errorType";
  public static final String MESSAGE = "message";
  public static final String LOCALIZED_MESSAGE = "localizedMessage";
  public static final String STRING = "string";
  public static final String STACK_TRACE = "stackTrace";

  private CedarAssertionResult result;

  public CedarAssertionException(CedarAssertionResult result) {
    super(result.getMessage());
    this.result = result;
  }

  public int getCode() {
    return result.getCode();
  }

  public ObjectNode asJson() {
    ObjectNode objectNode = JsonMapper.MAPPER.createObjectNode();
    addExceptionData(objectNode, this, "cedarAssertionFramework");

    objectNode.put(ERROR_SUB_TYPE, result.getErrorSubType());
    objectNode.put(ERROR_CODE, result.getErrorCode());
    objectNode.put(SUGGESTED_ACTION, result.getSuggestedAction());

    ObjectNode errorParams = JsonMapper.MAPPER.createObjectNode();
    for (String key : result.getParameters().keySet()) {
      errorParams.set(key, JsonMapper.MAPPER.valueToTree(result.getParameters().get(key)));
    }
    objectNode.set(ERROR_PARAMS, errorParams);

    return objectNode;
  }

  public static ObjectNode asJson(String errorSource, String errorMessage) {
    ObjectNode objectNode = JsonMapper.MAPPER.createObjectNode();
    addStringData(objectNode, errorSource, errorMessage);
    return objectNode;
  }

  public static ObjectNode asJson(Exception ex) {
    ObjectNode objectNode = JsonMapper.MAPPER.createObjectNode();
    addExceptionData(objectNode, ex, "cedarServer");
    return objectNode;
  }

  private static void addExceptionData(ObjectNode objectNode, Exception ex, String errorSource) {
    objectNode.put(ERROR_SOURCE, errorSource);
    objectNode.put(ERROR_TYPE, "exception");
    objectNode.put(MESSAGE, ex.getMessage());
    objectNode.put(LOCALIZED_MESSAGE, ex.getLocalizedMessage());
    objectNode.put(STRING, ex.toString());

    ArrayNode jsonST = objectNode.putArray(STACK_TRACE);
    for (StackTraceElement ste : ex.getStackTrace()) {
      jsonST.add(ste.toString());
    }
  }

  private static void addStringData(ObjectNode objectNode, String errorSource, String errorMessage) {
    objectNode.put(ERROR_SOURCE, errorSource);
    objectNode.put(ERROR_TYPE, "error");
    objectNode.put(MESSAGE, errorMessage);
    objectNode.put(LOCALIZED_MESSAGE, errorMessage);
    objectNode.put(STRING, errorMessage);
  }

}