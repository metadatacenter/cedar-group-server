package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.assertion.ICedarAssertion;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionException;

import java.util.Collection;
import java.util.LinkedHashSet;

public class AssertionPOJOTargetPresent implements IAssertionPOJOTargetPresent {

  private Collection<Object> targets;
  private ICedarRequestContext requestContext;

  public AssertionPOJOTargetPresent(ICedarRequestContext requestContext, Object... targets) {
    this.requestContext = requestContext;
    this.targets = new LinkedHashSet<>();
    for (Object target : targets) {
      this.targets.add(target);
    }
  }

  @Override
  public void be(ICedarAssertion... assertions) throws CedarAssertionException {
    for (Object target : targets) {
      for (ICedarAssertion assertion : assertions) {
        assertion.check(requestContext, target);
      }
    }
  }
}
