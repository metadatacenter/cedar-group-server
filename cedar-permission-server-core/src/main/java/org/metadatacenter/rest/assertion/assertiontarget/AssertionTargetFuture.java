package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.ICedarOperationDescriptor;
import org.metadatacenter.rest.assertion.ICedarAssertion;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.rest.exception.CedarAssertionResult;

import java.util.Collection;

public abstract class AssertionTargetFuture<T> implements IAssertionTargetFuture {

  protected Collection<T> targets;
  protected ICedarRequestContext requestContext;
  protected Collection<ICedarAssertion> assertions;

  @Override
  public void otherwiseBadRequest() throws CedarAssertionException {
    buildAndThrowAssertionExceptionIfNeeded(getFirstAssertionError(), null, null,
        CedarAssertionResult.HTTP_BAD_REQUEST);
  }

  @Override
  public void otherwiseBadRequest(ICedarOperationDescriptor operation, String message) throws CedarAssertionException {
    buildAndThrowAssertionExceptionIfNeeded(getFirstAssertionError(), operation, message,
        CedarAssertionResult.HTTP_BAD_REQUEST);
  }

  @Override
  public void otherwiseInternalServerError(ICedarOperationDescriptor operation, String message) throws
      CedarAssertionException {
    buildAndThrowAssertionExceptionIfNeeded(getFirstAssertionError(), operation, message,
        CedarAssertionResult.HTTP_INTERNAL_SERVER_ERROR);
  }

  @Override
  public void otherwiseNotFound(ICedarOperationDescriptor operation, String message) throws CedarAssertionException {
    buildAndThrowAssertionExceptionIfNeeded(getFirstAssertionError(), operation, message,
        CedarAssertionResult.HTTP_NOT_FOUND);
  }


  protected CedarAssertionResult getFirstAssertionError() {
    CedarAssertionResult assertionError = null;
    for (T target : targets) {
      for (ICedarAssertion assertion : assertions) {
        if (target instanceof ICedarAssertionNoun) {
          assertionError = assertion.check(requestContext, (ICedarAssertionNoun) target);
        } else {
          assertionError = assertion.check(requestContext, target);
        }
        if (assertionError != null) {
          return assertionError;
        }
      }
    }
    return null;
  }

  private void buildAndThrowAssertionExceptionIfNeeded(CedarAssertionResult assertionResult,
                                                       ICedarOperationDescriptor operation, String message,
                                                       int errorCode) throws CedarAssertionException {
    if (assertionResult != null) {
      assertionResult.setCode(errorCode);
      if (message != null) {
        assertionResult.setMessage(message);
      }
      CedarAssertionException ex = new CedarAssertionException(assertionResult, operation);
      throw ex;
    }
  }


}
