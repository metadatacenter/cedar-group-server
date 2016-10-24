package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.CedarAssertionNoun;
import org.metadatacenter.rest.assertion.noun.CedarParameter;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionResult;

public class NonEmptyAssertion implements CedarAssertion {

  NonEmptyAssertion() {
  }

  @Override
  public CedarAssertionResult check(CedarRequestContext requestContext, CedarAssertionNoun target) {
    if (!(target instanceof CedarParameter)) {
      return new CedarAssertionResult("Only instances of ICedarParameter can be checked with this assertion");
    }
    CedarParameter cedarParameter = (CedarParameter) target;
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
  }

  @Override
  public CedarAssertionResult check(CedarRequestContext requestContext, Object target) {
    return new CedarAssertionResult("Not implemented for Objects");
  }

}
