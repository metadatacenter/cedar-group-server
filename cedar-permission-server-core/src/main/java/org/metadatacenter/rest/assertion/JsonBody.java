package org.metadatacenter.rest.assertion;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.context.ICedarRequestContext;

public class JsonBody implements ICedarAssertion {

  JsonBody() {
  }

  @Override
  public void check(ICedarRequestContext requestContext, ICedarAssertionNoun target) {
    System.out.println("Check if it is a json body:" + target);
  }
}
