package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.CedarAssertionNoun;
import org.metadatacenter.rest.assertion.noun.CedarUserNoun;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionResult;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.security.model.user.CedarUser;

public class LoggedInAssertion implements CedarAssertion {

  LoggedInAssertion() {
  }

  @Override
  public CedarAssertionResult check(CedarRequestContext requestContext, CedarAssertionNoun target) {
    if (!(target instanceof CedarUserNoun)) {
      return new CedarAssertionResult("Only instances of CedarUserNoun can be checked with this assertion");
    }
    CedarUserNoun cedarUserNoun = (CedarUserNoun) target;
    //noinspection ConstantConditions
    CedarUser user = cedarUserNoun.getUser();
    if (user != null) {
      String cn = CedarPermission.LOGGED_IN.getPermissionName();
      if (user.getPermissions() == null || !user.getPermissions().contains(cn)) {
        return new CedarAssertionResult("The user must be logged in").forbidden();
      } else {
        return null;
      }
    }
    return new CedarAssertionResult("You need to provide valid authorization data to execute REST calls")
        .unauthorized();
  }

  @Override
  public CedarAssertionResult check(CedarRequestContext requestContext, Object target) {
    return new CedarAssertionResult("Not implemented for Objects");
  }

}
