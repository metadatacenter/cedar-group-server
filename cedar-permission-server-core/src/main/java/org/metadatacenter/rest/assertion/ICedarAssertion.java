package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionException;

public interface ICedarAssertion {

  void check(ICedarRequestContext requestContext, ICedarAssertionNoun target) throws CedarAssertionException;
}
