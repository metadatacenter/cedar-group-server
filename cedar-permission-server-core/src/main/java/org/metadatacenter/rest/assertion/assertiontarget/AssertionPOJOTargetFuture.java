package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.assertion.ICedarAssertion;
import org.metadatacenter.rest.context.ICedarRequestContext;

import java.util.LinkedHashSet;

public class AssertionPOJOTargetFuture extends AssertionTargetFuture<Object> implements IAssertionPOJOTargetFuture {

  public AssertionPOJOTargetFuture(ICedarRequestContext requestContext, Object... targets) {
    this.requestContext = requestContext;
    this.targets = new LinkedHashSet<>();
    for (Object target : targets) {
      this.targets.add(target);
    }
    this.assertions = new LinkedHashSet<>();
  }

  @Override
  public AssertionPOJOTargetFuture be(ICedarAssertion... assertions) {
    for (ICedarAssertion assertion : assertions) {
      this.assertions.add(assertion);
    }
    return this;
  }
}
