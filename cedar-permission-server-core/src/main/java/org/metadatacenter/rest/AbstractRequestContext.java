package org.metadatacenter.rest;

import org.metadatacenter.rest.operation.CedarOperationBuilder;
import org.metadatacenter.server.security.model.user.CedarUser;

public abstract class AbstractRequestContext implements ICedarRequestContext {
  @Override
  public ICedarUser user() {
    return null;
  }

  @Override
  public ICedarAssertionTarget must(ICedarAssertionNoun target) {
    return null;
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
    return null;
  }
}
