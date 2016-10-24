package org.metadatacenter.rest.context;

import org.metadatacenter.rest.CedarAssertionNoun;
import org.metadatacenter.rest.assertion.assertiontarget.*;
import org.metadatacenter.rest.assertion.noun.CedarRequest;
import org.metadatacenter.rest.assertion.noun.CedarAssertionUser;
import org.metadatacenter.server.security.model.AuthRequest;

public abstract class AbstractRequestContext implements CedarRequestContext {

  protected org.metadatacenter.server.security.model.user.CedarUser currentUser;
  protected CedarAssertionUser user;
  protected CedarRequest wrappedRequest;
  protected AuthRequest authRequest;

  @Override
  public CedarAssertionUser user() {
    return user;
  }

  @Override
  public AssertionNounTargetFuture should(CedarAssertionNoun... targets) {
    return new AssertionNounTargetFutureImpl(this, targets);
  }

  @Override
  public AssertionPOJOTargetFuture should(Object... targets) {
    return new AssertionPOJOTargetFutureImpl(this, targets);
  }

  @Override
  public AssertionNounTargetPresent must(CedarAssertionNoun... targets) {
    return new AssertionNounTargetPresentImpl(this, targets);
  }

  @Override
  public AssertionPOJOTargetPresent must(Object... targets) {
    return new AssertionPOJOTargetPresentImpl(this, targets);
  }

  @Override
  public org.metadatacenter.server.security.model.user.CedarUser getCedarUser() {
    return currentUser;
  }

  @Override
  public CedarRequest request() {
    return wrappedRequest;
  }

  @Override
  public AuthRequest getAuthRequest() {
    return authRequest;
  }

}
