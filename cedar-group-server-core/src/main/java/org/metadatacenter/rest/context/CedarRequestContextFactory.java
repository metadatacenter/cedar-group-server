package org.metadatacenter.rest.context;

import play.mvc.Http;

public class CedarRequestContextFactory {
  public static CedarRequestContext fromRequest(Http.Request request) {
    return new PlayRequestContext(request);
  }
}
