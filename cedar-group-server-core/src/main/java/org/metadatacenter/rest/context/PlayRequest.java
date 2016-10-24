package org.metadatacenter.rest.context;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.rest.assertion.noun.CedarRequestBody;
import org.metadatacenter.rest.assertion.noun.CedarRequestNoun;
import org.metadatacenter.rest.exception.CedarAssertionException;
import play.mvc.Http;

public class PlayRequest extends CedarRequestNoun {

  private final Http.Request nativeRequest;

  PlayRequest(Http.Request request) {
    if (request == null) {
      throw new IllegalArgumentException("The PlayRequest should never be null at this point");
    }
    this.nativeRequest = request;
  }

  @Override
  public CedarRequestBody getRequestBody() throws CedarAssertionException {
    if (nativeRequest.body() != null) {
      JsonNode jsonBodyNode = nativeRequest.body().asJson();
      if (jsonBodyNode != null) {
        return new PlayRequestJsonBody(jsonBodyNode);
      }
      // TODO: add an enum to the source here
      throw new CedarAssertionException("Currently only JSON content type is supported!", "cedarAssertionFramework");
    } else {
      return new PlayRequestEmptyBody();
    }
  }

  @Override
  public String getContentType() {
    if (nativeRequest != null) {
      return nativeRequest.getHeader("Content-Type");
    }
    return null;
  }
}
