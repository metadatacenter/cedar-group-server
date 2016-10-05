package org.metadatacenter.rest;

import org.metadatacenter.rest.operation.CedarOperationBuilder;
import org.metadatacenter.server.security.model.user.CedarUser;

public interface ICedarRequestContext {
  ICedarRequest request();

  ICedarUser user();

  ICedarAssertionTarget must(ICedarAssertionNoun target);

  ICedarAssertionParameterTarget must(ICedarParameter... params);

  ICedarAssertionObjectTarget must(Object object);

  CedarOperationBuilder operation();

  CedarUser getCedarUser();
}
