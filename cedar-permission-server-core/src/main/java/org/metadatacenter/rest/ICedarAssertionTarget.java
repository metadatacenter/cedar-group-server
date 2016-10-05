package org.metadatacenter.rest;

import org.metadatacenter.server.security.model.auth.CedarPermission;

public interface ICedarAssertionTarget {
  void be(ICedarAssertion... assertions);

  void have(CedarPermission permission);

}
