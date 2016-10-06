package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.assertion.ICedarAssertion;
import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.server.security.model.auth.CedarPermission;

public class CedarAssertionTarget implements ICedarAssertionTarget {

  private ICedarAssertionNoun target;
  private ICedarRequestContext requestContext;

  public CedarAssertionTarget(ICedarRequestContext requestContext, ICedarAssertionNoun target) {
    this.requestContext = requestContext;
    this.target = target;
  }

  @Override
  public void be(ICedarAssertion... assertions) throws CedarAssertionException {
    System.out.println("check assertions on: " + target);
    for (ICedarAssertion assertion : assertions) {
      System.out.println("Check this:" + assertion);
      assertion.check(requestContext, target);
    }
  }

  @Override
  public void have(CedarPermission... permissions) {
    // TODO: implemet this
  }
}
