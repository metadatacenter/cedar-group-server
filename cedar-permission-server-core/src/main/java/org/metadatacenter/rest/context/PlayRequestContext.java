package org.metadatacenter.rest.context;

import org.metadatacenter.rest.assertion.noun.UserNoun;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.CedarAccessException;
import play.mvc.Http;

public class PlayRequestContext extends AbstractRequestContext {

  private Http.Request playRequest;

  PlayRequestContext(Http.Request request) {
    playRequest = request;
    initialize();
  }

  @Override
  void initializeLocal() {
    wrappedRequest = new PlayRequest(playRequest);
    try {
      authRequest = CedarAuthFromRequestFactory.fromRequest(playRequest);
      currentUser = Authorization.getUser(authRequest);
    } catch (CedarAccessException e) {
      // do not do anything, currentUser will be null, menaing we were not able to match any users
    }
    user = new UserNoun(currentUser);
  }

}
