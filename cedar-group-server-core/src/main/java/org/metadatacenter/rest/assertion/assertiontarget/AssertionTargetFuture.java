package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.CedarOperationDescriptor;
import org.metadatacenter.rest.exception.CedarAssertionException;

public interface AssertionTargetFuture {

  void otherwiseBadRequest() throws CedarAssertionException;

  void otherwiseBadRequest(CedarOperationDescriptor operation, String message) throws CedarAssertionException;

  void otherwiseInternalServerError(CedarOperationDescriptor operation, String message) throws CedarAssertionException;

  void otherwiseNotFound(CedarOperationDescriptor operation, String message) throws CedarAssertionException;

  void otherwiseForbidden(CedarOperationDescriptor operation, String message) throws CedarAssertionException;
}
