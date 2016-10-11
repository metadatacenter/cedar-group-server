package org.metadatacenter.rest.context;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.assertion.assertiontarget.*;
import org.metadatacenter.rest.assertion.noun.ICedarRequest;
import org.metadatacenter.rest.assertion.noun.ICedarUser;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.user.CedarUser;

public abstract class AbstractRequestContext implements ICedarRequestContext {

  protected CedarUser currentUser;
  protected ICedarUser user;
  protected ICedarRequest wrappedRequest;
  protected IAuthRequest authRequest;

  protected void initialize() {
    initializeLocal();
  }

  abstract void initializeLocal();

  @Override
  public ICedarUser user() {
    return user;
  }

  @Override
  public IAssertionNounTargetFuture should(ICedarAssertionNoun... targets) {
    return new AssertionNounTargetFuture(this, targets);
  }

  @Override
  public IAssertionPOJOTargetFuture should(Object... targets) {
    return new AssertionPOJOTargetFuture(this, targets);
  }

  @Override
  public IAssertionNounTargetPresent must(ICedarAssertionNoun... targets) {
    return new AssertionNounTargetPresent(this, targets);
  }

  @Override
  public IAssertionPOJOTargetPresent must(Object... targets) {
    return new AssertionPOJOTargetPresent(this, targets);
  }

  @Override
  public CedarUser getCedarUser() {
    return currentUser;
  }

  @Override
  public ICedarRequest request() {
    return wrappedRequest;
  }

  @Override
  public IAuthRequest getAuthRequest() {
    return authRequest;
  }

}
