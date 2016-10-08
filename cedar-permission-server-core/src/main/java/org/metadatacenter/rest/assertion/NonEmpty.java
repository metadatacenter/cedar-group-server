package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.assertion.noun.ICedarParameter;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionResult;

public class NonEmpty implements ICedarAssertion {

  NonEmpty() {
  }

  @Override
  public CedarAssertionResult check(ICedarRequestContext requestContext, ICedarAssertionNoun target) {
    if (!(target instanceof ICedarParameter)) {
      return new CedarAssertionResult("Only instances of ICedarParameter can be checked with this assertion");
    }
    ICedarParameter cedarParameter = (ICedarParameter) target;
    if (cedarParameter != null) {
      String s = cedarParameter.stringValue();
      if (s != null && !s.trim().isEmpty()) {
        return null;
      } else {
        return new CedarAssertionResult("You need to provide a non-null value for the parameter:"
            + cedarParameter.getName() + " from " + cedarParameter.getSource())
            .setParameter("name", cedarParameter.getName())
            .setParameter("source", cedarParameter.getSource())
            .badRequest();
      }
    } else {
      return new CedarAssertionResult("The parameter should not be null");
    }
  }

  @Override
  public CedarAssertionResult check(ICedarRequestContext requestContext, Object target) {
    return new CedarAssertionResult("Not implemented for Objects");
  }

}
