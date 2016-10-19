package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.CedarAssertionNoun;
import org.metadatacenter.rest.assertion.noun.CedarRequestBody;
import org.metadatacenter.rest.assertion.noun.RequestNoun;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionResult;

public class JsonBody implements CedarAssertion {

  JsonBody() {
  }

  @Override
  public CedarAssertionResult check(CedarRequestContext requestContext, CedarAssertionNoun target) {
    if (!(target instanceof RequestNoun)) {
      return new CedarAssertionResult("Only instances of CedarRequestNoun can be checked with this assertion");
    }
    RequestNoun cedarRequestNoun = (RequestNoun) target;
    CedarRequestBody jsonBody = cedarRequestNoun.getJsonBody();
    if (jsonBody != null) {
      return null;
    }
    return new CedarAssertionResult("You need to provide a valid JSON document as the body of the REST call")
        .badRequest();
  }

  @Override
  public CedarAssertionResult check(CedarRequestContext requestContext, Object target) {
    return new CedarAssertionResult("Not implemented for Objects");
  }
}
