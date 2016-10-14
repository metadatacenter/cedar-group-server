package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.assertion.ICedarAssertion;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.server.security.model.auth.CedarPermission;

public interface IAssertionNounTargetPresent {

  void be(ICedarAssertion... assertions) throws CedarAssertionException;

  void have(CedarPermission... permissions) throws CedarAssertionException;

}
