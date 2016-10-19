package org.metadatacenter.rest.assertion.assertiontarget;

import org.metadatacenter.rest.CedarAssertionNoun;
import org.metadatacenter.rest.CedarOperationDescriptor;
import org.metadatacenter.rest.assertion.CedarAssertion;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.rest.exception.CedarAssertionResult;

import java.util.Collection;

public abstract class AssertionTargetFutureImpl<T> implements AssertionTargetFuture {

  protected Collection<T> targets;
  protected CedarRequestContext requestContext;
  protected Collection<CedarAssertion> assertions;

  @Override
  public void otherwiseBadRequest() throws CedarAssertionException {
    buildAndThrowAssertionExceptionIfNeeded(getFirstAssertionError(), null, null,
        CedarAssertionResult.HTTP_BAD_REQUEST);
  }

  @Override
  public void otherwiseBadRequest(CedarOperationDescriptor operation, String message) throws CedarAssertionException {
    buildAndThrowAssertionExceptionIfNeeded(getFirstAssertionError(), operation, message,
        CedarAssertionResult.HTTP_BAD_REQUEST);
  }

  @Override
  public void otherwiseInternalServerError(CedarOperationDescriptor operation, String message) throws
      CedarAssertionException {
    buildAndThrowAssertionExceptionIfNeeded(getFirstAssertionError(), operation, message,
        CedarAssertionResult.HTTP_INTERNAL_SERVER_ERROR);
  }

  @Override
  public void otherwiseNotFound(CedarOperationDescriptor operation, String message) throws CedarAssertionException {
    buildAndThrowAssertionExceptionIfNeeded(getFirstAssertionError(), operation, message,
        CedarAssertionResult.HTTP_NOT_FOUND);
  }

  @Override
  public void otherwiseForbidden(CedarOperationDescriptor operation, String message) throws CedarAssertionException {
    buildAndThrowAssertionExceptionIfNeeded(getFirstAssertionError(), operation, message,
        CedarAssertionResult.HTTP_FORBIDDEN);
  }

  @SuppressWarnings("UnusedAssignment")
  protected CedarAssertionResult getFirstAssertionError() {
    CedarAssertionResult assertionError;
    for (T target : targets) {
      for (CedarAssertion assertion : assertions) {
        if (target instanceof CedarAssertionNoun) {
          assertionError = assertion.check(requestContext, (CedarAssertionNoun) target);
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
                                                       CedarOperationDescriptor operation, String message,
                                                       int errorCode) throws CedarAssertionException {
    if (assertionResult != null) {
      assertionResult.setCode(errorCode);
      if (message != null) {
        assertionResult.setMessage(message);
      }
      throw new CedarAssertionException(assertionResult, operation);
    }
  }


}
