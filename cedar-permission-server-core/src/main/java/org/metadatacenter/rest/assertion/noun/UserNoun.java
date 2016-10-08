package org.metadatacenter.rest.assertion.noun;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.server.security.model.user.CedarUser;

public class UserNoun implements ICedarUser, ICedarAssertionNoun {

  private CedarUser user;

  public UserNoun(CedarUser user) {
    this.user = user;
  }

  public CedarUser getUser() {
    return user;
  }
}
