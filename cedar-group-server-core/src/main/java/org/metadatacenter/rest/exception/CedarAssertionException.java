package org.metadatacenter.rest.exception;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.rest.CedarOperationDescriptor;
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
  public static final String OPERATION = "operation";
  public static final String SOURCE_STACK_TRACE = "sourceStackTrace";

  private CedarAssertionResult result;
  private CedarOperationDescriptor operation;
  private String errorSource;
  private final String errorType;
  private Exception sourceException;
  private int code;

  private CedarAssertionException(String message) {
    super(message);
    this.code = CedarAssertionResult.HTTP_INTERNAL_SERVER_ERROR;
    this.errorType = "exception";
  }

  public CedarAssertionException(CedarAssertionResult result, CedarOperationDescriptor operation,
                                 Exception sourceException) {
    this(result != null ? result.getMessage() : sourceException != null ? sourceException.getMessage() : "");
    this.result = result;
    if (result != null) {
      this.code = result.getCode();
    }
    this.operation = operation;
    this.errorSource = "cedarAssertionFramework";
    this.sourceException = sourceException;
  }

  public CedarAssertionException(CedarAssertionResult result, CedarOperationDescriptor operation) {
    this(result, operation, null);
  }

  public CedarAssertionException(CedarAssertionResult result) {
    this(result, null, null);
  }

  public CedarAssertionException(Exception sourceException) {
    this(null, null, sourceException);
  }

  public CedarAssertionException(Exception sourceException, String errorSource) {
    this(null, null, sourceException);
    this.errorSource = errorSource;
  }

  public CedarAssertionException(String errorMessage, String errorSource) {
    this(errorMessage);
    this.errorSource = errorSource;
  }

  public int getCode() {
    return code;
  }

  public ObjectNode asJson() {
    ObjectNode objectNode = JsonMapper.MAPPER.createObjectNode();
    addExceptionData(objectNode);

    if (operation != null) {
      objectNode.set(OPERATION, operation.asJson());
    }

    if (result != null) {
      objectNode.put(ERROR_SUB_TYPE, result.getErrorSubType());
      objectNode.put(ERROR_CODE, result.getErrorCode());
      objectNode.put(SUGGESTED_ACTION, result.getSuggestedAction());

      ObjectNode errorParams = JsonMapper.MAPPER.createObjectNode();
      for (String key : result.getParameters().keySet()) {
        errorParams.set(key, JsonMapper.MAPPER.valueToTree(result.getParameters().get(key)));
      }
      objectNode.set(ERROR_PARAMS, errorParams);
    }

    return objectNode;
  }

  private void addExceptionData(ObjectNode objectNode) {
    objectNode.put(ERROR_SOURCE, errorSource);
    objectNode.put(ERROR_TYPE, errorType);
    objectNode.put(MESSAGE, this.getMessage());
    objectNode.put(LOCALIZED_MESSAGE, this.getLocalizedMessage());
    objectNode.put(STRING, this.toString());

    ArrayNode jsonST = objectNode.putArray(STACK_TRACE);
    for (StackTraceElement ste : this.getStackTrace()) {
      jsonST.add(ste.toString());
    }

    if (sourceException != null) {
      ArrayNode jsonSST = objectNode.putArray(SOURCE_STACK_TRACE);
      for (StackTraceElement ste : sourceException.getStackTrace()) {
        if (ste != null) {
          jsonSST.add(ste.toString());
        }
      }
    }
  }

}