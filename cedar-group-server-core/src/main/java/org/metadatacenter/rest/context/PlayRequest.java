package org.metadatacenter.rest.context;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.rest.assertion.noun.CedarRequestBody;
import org.metadatacenter.rest.assertion.noun.RequestNoun;
import play.mvc.Http;

@SuppressWarnings("ALL")
public class PlayRequest extends RequestNoun {

  private final Http.Request nativeRequest;

  PlayRequest(Http.Request request) {
    this.nativeRequest = request;
  }

  @Override
  public CedarRequestBody getJsonBody() {
    JsonNode jsonBodyNode;
    if (nativeRequest != null && nativeRequest.body() != null) {
      jsonBodyNode = nativeRequest.body().asJson();
      if (jsonBodyNode != null) {
        return new PlayRequestJsonBody(jsonBodyNode);
      }
    }
    return null;
  }

  @Override
  public String getContentType() {
    if (nativeRequest != null ) {
      return nativeRequest.getHeader("Content-Type");
    }
    return null;
  }
}
