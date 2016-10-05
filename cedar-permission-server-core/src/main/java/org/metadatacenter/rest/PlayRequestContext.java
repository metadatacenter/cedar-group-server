package org.metadatacenter.rest;

import play.mvc.Http;

public class PlayRequestContext extends AbstractRequestContext {

  private Http.Request request;

  PlayRequestContext(Http.Request request) {
    this.request = request;
  }

  @Override
  public ICedarRequest request() {
    return null;
  }

}
