package org.metadatacenter.rest.context;

import org.metadatacenter.rest.*;
import org.metadatacenter.rest.assertion.noun.ICedarUser;
import org.metadatacenter.rest.assertion.assertiontarget.ICedarAssertionTarget;
import org.metadatacenter.rest.operation.CedarOperationBuilder;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.user.CedarUser;

public interface ICedarRequestContext {

  ICedarRequest request();

  IAuthRequest getAuthRequest();

  ICedarUser user();

  ICedarAssertionTarget must(ICedarAssertionNoun target);

  ICedarAssertionParameterTarget must(ICedarParameter... params);

  ICedarAssertionObjectTarget must(Object object);

  CedarOperationBuilder operation();

  CedarUser getCedarUser();
}
