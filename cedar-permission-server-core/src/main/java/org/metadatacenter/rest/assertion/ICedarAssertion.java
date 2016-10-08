package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionResult;

public interface ICedarAssertion {

  CedarAssertionResult check(ICedarRequestContext requestContext, ICedarAssertionNoun target);

  CedarAssertionResult check(ICedarRequestContext requestContext, Object target);
}
