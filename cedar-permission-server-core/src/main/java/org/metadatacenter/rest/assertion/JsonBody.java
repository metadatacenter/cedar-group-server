package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.assertion.noun.ICedarRequestBody;
import org.metadatacenter.rest.assertion.noun.RequestNoun;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionResult;

public class JsonBody implements ICedarAssertion {

  JsonBody() {
  }

  @Override
  public CedarAssertionResult check(ICedarRequestContext requestContext, ICedarAssertionNoun target) {
    if (!(target instanceof RequestNoun)) {
      return new CedarAssertionResult("Only instances of CedarRequestNoun can be checked with this assertion");
    }
    RequestNoun cedarRequestNoun = (RequestNoun) target;
    if (cedarRequestNoun != null) {
      ICedarRequestBody jsonBody = cedarRequestNoun.jsonBody();
      if (jsonBody != null) {
        return null;
      }
    }
    return new CedarAssertionResult("You need to provide a valid JSON document as the body of the REST call")
        .badRequest();
  }

  @Override
  public CedarAssertionResult check(ICedarRequestContext requestContext, Object target) {
    return new CedarAssertionResult("Not implemented for Objects");
  }
}
