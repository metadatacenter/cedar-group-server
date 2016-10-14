package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.assertion.ICedarAssertion;
import org.metadatacenter.server.security.model.auth.CedarPermission;

public interface IAssertionNounTargetFuture extends IAssertionTargetFuture {

  IAssertionNounTargetFuture be(ICedarAssertion... assertions);

  IAssertionNounTargetFuture have(CedarPermission... permissions);
}
