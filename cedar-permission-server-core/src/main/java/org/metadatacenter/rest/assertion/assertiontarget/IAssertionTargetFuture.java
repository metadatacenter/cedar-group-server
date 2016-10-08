package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.ICedarOperationDescriptor;
import org.metadatacenter.rest.exception.CedarAssertionException;

public interface IAssertionTargetFuture {

  void otherwiseBadRequest() throws CedarAssertionException;

  void otherwiseBadRequest(ICedarOperationDescriptor operation) throws CedarAssertionException;

  void otherwiseInternalServerError(ICedarOperationDescriptor operation) throws CedarAssertionException;

  void otherwiseNotFound(ICedarOperationDescriptor operation) throws CedarAssertionException;
}
