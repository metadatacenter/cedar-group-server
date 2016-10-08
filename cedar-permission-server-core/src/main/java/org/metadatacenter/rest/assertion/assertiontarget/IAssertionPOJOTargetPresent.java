package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.assertion.ICedarAssertion;
import org.metadatacenter.rest.exception.CedarAssertionException;

public interface IAssertionPOJOTargetPresent {

  void be(ICedarAssertion... assertions) throws CedarAssertionException;

}
