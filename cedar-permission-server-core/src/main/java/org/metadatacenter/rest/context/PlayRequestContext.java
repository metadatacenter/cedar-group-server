package org.metadatacenter.rest.context;

import org.metadatacenter.rest.ICedarRequest;
import org.metadatacenter.rest.assertion.noun.CedarUserNoun;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.IAuthRequest;
import play.mvc.Http;

public class PlayRequestContext extends AbstractRequestContext {

  private Http.Request request;
  private IAuthRequest authRequest;

  PlayRequestContext(Http.Request request) {
    this.request = request;
    initialize();
  }

  @Override
  public ICedarRequest request() {
    return null;
  }

  @Override
  public IAuthRequest getAuthRequest() {
    return authRequest;
  }

  @Override
  void initializeLocal() {
    try {
      authRequest = CedarAuthFromRequestFactory.fromRequest(request);
      currentUser = Authorization.getUser(authRequest);
    } catch (CedarAccessException e) {
      // do not do anything, currentUser will be null, menaing we were not able to match any users
    }
    user = new CedarUserNoun(currentUser);
  }

}
