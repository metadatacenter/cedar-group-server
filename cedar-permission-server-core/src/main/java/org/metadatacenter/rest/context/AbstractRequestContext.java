package org.metadatacenter.rest.context;

import org.metadatacenter.rest.*;
import org.metadatacenter.rest.assertion.noun.ICedarUser;
import org.metadatacenter.rest.assertion.assertiontarget.CedarAssertionTarget;
import org.metadatacenter.rest.assertion.assertiontarget.ICedarAssertionTarget;
import org.metadatacenter.rest.operation.CedarOperationBuilder;
import org.metadatacenter.server.security.model.user.CedarUser;

public abstract class AbstractRequestContext implements ICedarRequestContext {

  protected CedarUser currentUser;
  protected ICedarUser user;

  protected void initialize() {
    initializeLocal();
  }

  abstract void initializeLocal();

  @Override
  public ICedarUser user() {
    return user;
  }

  @Override
  public ICedarAssertionTarget must(ICedarAssertionNoun target) {
    return new CedarAssertionTarget(this, target);
  }

  @Override
  public ICedarAssertionParameterTarget must(ICedarParameter... params) {
    return null;
  }

  @Override
  public ICedarAssertionObjectTarget must(Object object) {
    return null;
  }

  @Override
  public CedarOperationBuilder operation() {
    return null;
  }

  @Override
  public CedarUser getCedarUser() {
    return currentUser;
  }
}
