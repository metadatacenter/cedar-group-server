package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.assertion.ICedarAssertion;
import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.server.security.model.auth.CedarPermission;

import java.util.Collection;
import java.util.LinkedHashSet;

public class AssertionNountTargetPresent implements IAssertionNounTargetPresent {

  private Collection<ICedarAssertionNoun> targets;
  private ICedarRequestContext requestContext;

  public AssertionNountTargetPresent(ICedarRequestContext requestContext, ICedarAssertionNoun... targets) {
    this.requestContext = requestContext;
    this.targets = new LinkedHashSet<>();
    for (ICedarAssertionNoun target : targets) {
      this.targets.add(target);
    }
  }

  @Override
  public void be(ICedarAssertion... assertions) throws CedarAssertionException {
    for (ICedarAssertionNoun target : targets) {
      for (ICedarAssertion assertion : assertions) {
        assertion.check(requestContext, target);
      }
    }
  }

  @Override
  public void have(CedarPermission... permissions) {
    // TODO: implemet this
  }
}
