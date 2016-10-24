package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.CedarAssertionNoun;
import org.metadatacenter.rest.assertion.noun.CedarRequestNoun;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionResult;

public class JsonMergePatchAssertion implements CedarAssertion {

  public static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";

  JsonMergePatchAssertion() {
  }

  @Override
  public CedarAssertionResult check(CedarRequestContext requestContext, CedarAssertionNoun target) {
    if (!(target instanceof CedarRequestNoun)) {
      return new CedarAssertionResult("Only instances of CedarRequestNoun can be checked with this assertion");
    }
    String contentType;
    CedarRequestNoun cedarRequestNoun = (CedarRequestNoun) target;
    contentType = cedarRequestNoun.getContentType();
    if (APPLICATION_MERGE_PATCH_JSON.equals(contentType)) {
      return null;
    }
    return new CedarAssertionResult("You need to provide a request with '" + APPLICATION_MERGE_PATCH_JSON +
        "' convert content type!").badRequest();
  }

  @Override
  public CedarAssertionResult check(CedarRequestContext requestContext, Object target) {
    return new CedarAssertionResult("Not implemented for Objects");
  }
}
