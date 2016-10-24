package org.metadatacenter.rest.context;

import org.metadatacenter.rest.assertion.noun.CedarUserNoun;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.CedarAccessException;
import play.mvc.Http;

public class PlayRequestContext extends AbstractRequestContext {

  private final Http.Request playRequest;

  PlayRequestContext(Http.Request request) {
    if (request == null) {
      throw new IllegalArgumentException("The PlayRequest should never be null at this point");
    }
    playRequest = request;
    wrappedRequest = new PlayRequest(request);
    try {
      authRequest = CedarAuthFromRequestFactory.fromRequest(request);
      currentUser = Authorization.getUser(authRequest);
    } catch (CedarAccessException e) {
      // do not do anything, currentUser will be null, meaning we were not able to match any users
    }
    user = new CedarUserNoun(currentUser);
  }
}
