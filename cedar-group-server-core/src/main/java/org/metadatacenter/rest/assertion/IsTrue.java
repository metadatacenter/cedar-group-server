package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionResult;

public class IsTrue implements ICedarAssertion {

  IsTrue() {
  }

  @Override
  public CedarAssertionResult check(ICedarRequestContext requestContext, ICedarAssertionNoun target) {
    return new CedarAssertionResult("Not implemented for ICedarAssertionNoun");
  }

  @Override
  public CedarAssertionResult check(ICedarRequestContext requestContext, Object target) {
    if (target != null) {
      if (target instanceof Boolean) {
        Boolean b = (Boolean) target;
        if (b != null && b.equals(Boolean.TRUE)) {
          return null;
        }
      }
    }
    return new CedarAssertionResult("The object should be true");
  }
}
