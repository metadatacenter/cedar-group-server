package org.metadatacenter.rest.assertion.noun;

import org.metadatacenter.server.security.model.user.CedarUser;

public class CedarUserNoun implements ICedarUser {

  private CedarUser user;

  public CedarUserNoun(CedarUser user) {
    this.user = user;
  }

  public CedarUser getUser() {
    return user;
  }
}
