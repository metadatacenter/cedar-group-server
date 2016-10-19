package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.CedarAssertionNoun;
import org.metadatacenter.rest.assertion.noun.RequestNoun;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionResult;

public class JsonMergePatch implements CedarAssertion {

  public static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";

  JsonMergePatch() {
  }

  @Override
  public CedarAssertionResult check(CedarRequestContext requestContext, CedarAssertionNoun target) {
    if (!(target instanceof RequestNoun)) {
      return new CedarAssertionResult("Only instances of CedarRequestNoun can be checked with this assertion");
    }
    String contentType;
    RequestNoun cedarRequestNoun = (RequestNoun) target;
    contentType = cedarRequestNoun.getContentType();
    if (APPLICATION_MERGE_PATCH_JSON.equals(contentType)) {
      return null;
    }
    return new CedarAssertionResult("You need to provide a request with '" + APPLICATION_MERGE_PATCH_JSON +
        "' as content type!").badRequest();
  }

  @Override
  public CedarAssertionResult check(CedarRequestContext requestContext, Object target) {
    return new CedarAssertionResult("Not implemented for Objects");
  }
}
