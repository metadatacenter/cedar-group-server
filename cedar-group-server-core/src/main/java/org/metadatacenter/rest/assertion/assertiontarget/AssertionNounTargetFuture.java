package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.ICedarOperationDescriptor;
import org.metadatacenter.rest.assertion.ICedarAssertion;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.server.security.model.auth.CedarPermission;

import java.util.Collection;
import java.util.LinkedHashSet;

public class AssertionNounTargetFuture extends AssertionTargetFuture<ICedarAssertionNoun> implements
    IAssertionNounTargetFuture {

  private Collection<CedarPermission> permissions;

  public AssertionNounTargetFuture(ICedarRequestContext requestContext, ICedarAssertionNoun... targets) {
    this.requestContext = requestContext;
    this.targets = new LinkedHashSet<>();
    for (ICedarAssertionNoun target : targets) {
      this.targets.add(target);
    }
    this.assertions = new LinkedHashSet<>();
    this.permissions = new LinkedHashSet<>();
  }

  @Override
  public AssertionNounTargetFuture be(ICedarAssertion... assertions) {
    for (ICedarAssertion assertion : assertions) {
      this.assertions.add(assertion);
    }
    return this;
  }

  @Override
  public AssertionNounTargetFuture have(CedarPermission... permissions) {
    for (CedarPermission permission : permissions) {
      this.permissions.add(permission);
    }
    return this;
  }

}
