package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.CedarAssertionNoun;
import org.metadatacenter.rest.assertion.noun.CedarParameter;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionResult;

public class IsNotNull implements CedarAssertion {

  IsNotNull() {
  }

  @Override
  public CedarAssertionResult check(CedarRequestContext requestContext, CedarAssertionNoun target) {
    if (target == null) {
      return new CedarAssertionResult("The object should be not null");
    } else {
      if (target instanceof CedarParameter) {
        CedarParameter param = (CedarParameter) target;
        if (param.isMissing()) {
          return new CedarAssertionResult(new StringBuilder().append("The parameter named '").append(param.getName())
              .append("' from ").append(param.getSource()).append(" should be present").toString())
              .setParameter("name", param.getName())
              .setParameter("source", param.getSource());
        } else if (param.isNull()) {
          return new CedarAssertionResult(new StringBuilder().append("The parameter named '").append(param.getName())
              .append("' from ").append(param.getSource()).append(" should be not null").toString())
              .setParameter("name", param.getName())
              .setParameter("source", param.getSource());
        } else {
          return null;
        }
      }
    }
    return null;
  }

  @Override
  public CedarAssertionResult check(CedarRequestContext requestContext, Object target) {
    if (target != null) {
      return null;
    } else {
      return new CedarAssertionResult("The object should not be null");
    }
  }

}
