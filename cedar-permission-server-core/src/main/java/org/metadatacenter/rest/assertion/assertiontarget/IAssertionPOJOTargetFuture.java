package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.assertion.ICedarAssertion;

public interface IAssertionPOJOTargetFuture extends IAssertionTargetFuture {

  IAssertionPOJOTargetFuture be(ICedarAssertion... assertions);

}
