package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.assertion.noun.ICedarParameter;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionResult;

public class IsNotNull implements ICedarAssertion {

  IsNotNull() {
  }

  @Override
  public CedarAssertionResult check(ICedarRequestContext requestContext, ICedarAssertionNoun target) {
    if (target == null) {
      return new CedarAssertionResult("The object should be not null");
    } else {
      if (target instanceof ICedarParameter) {
        ICedarParameter param = (ICedarParameter) target;
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
  public CedarAssertionResult check(ICedarRequestContext requestContext, Object target) {
    if (target != null) {
      return null;
    } else {
      return new CedarAssertionResult("The object should not be null");
    }
  }

}
