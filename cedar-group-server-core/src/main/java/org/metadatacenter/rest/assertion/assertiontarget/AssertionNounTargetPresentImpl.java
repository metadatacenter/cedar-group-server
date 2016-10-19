package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.assertion.CedarAssertion;
import org.metadatacenter.rest.CedarAssertionNoun;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.rest.exception.CedarAssertionResult;
import org.metadatacenter.server.security.model.auth.CedarPermission;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

public class AssertionNounTargetPresentImpl implements AssertionNounTargetPresent {

  private final Collection<CedarAssertionNoun> targets;
  private final CedarRequestContext requestContext;

  public AssertionNounTargetPresentImpl(CedarRequestContext requestContext, CedarAssertionNoun... targets) {
    this.requestContext = requestContext;
    this.targets = new LinkedHashSet<>();
    Collections.addAll(this.targets, targets);
  }

  @Override
  public void be(CedarAssertion... assertions) throws CedarAssertionException {
    for (CedarAssertionNoun target : targets) {
      for (CedarAssertion assertion : assertions) {
        CedarAssertionResult result = assertion.check(requestContext, target);
        if (result != null) {
          throw new CedarAssertionException(result);
        }
      }
    }
  }

  @Override
  public void have(CedarPermission... permissions) {
    // TODO: implement this
  }
}
