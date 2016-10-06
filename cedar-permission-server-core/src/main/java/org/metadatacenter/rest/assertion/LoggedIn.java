package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.assertion.noun.CedarUserNoun;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.security.model.user.CedarUser;

public class LoggedIn implements ICedarAssertion {

  LoggedIn() {
  }

  @Override
  public void check(ICedarRequestContext requestContext, ICedarAssertionNoun target) throws CedarAssertionException {
    if (!(target instanceof CedarUserNoun)) {
      throw new CedarAssertionException("Only instances of CedarUserNoun can be checked with this assertion")
          .internalServerError();
    }
    CedarUserNoun cedarUserNoun = (CedarUserNoun) target;
    if (cedarUserNoun != null) {
      CedarUser user = cedarUserNoun.getUser();
      if (user != null) {
        String cn = CedarPermission.LOGGED_IN.getPermissionName();
        if (user.getPermissions() == null || !user.getPermissions().contains(cn)) {
          throw new CedarAssertionException("The user must be logged in")
              .forbidden();
        } else {
          return;
        }
      }
    }
    throw new CedarAssertionException("You need to provide valid authorization data to execute REST calls")
        .unauthorized();
  }
}
