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
    buildAndThrowAssertionExceptionIfNeeded(getFirstAssertionError(), null,
        CedarAssertionResult.HTTP_BAD_REQUEST);
  }

  @Override
  public void otherwiseBadRequest(ICedarOperationDescriptor operation) throws CedarAssertionException {
    buildAndThrowAssertionExceptionIfNeeded(getFirstAssertionError(), operation,
        CedarAssertionResult.HTTP_BAD_REQUEST);
  }

  @Override
  public void otherwiseInternalServerError(ICedarOperationDescriptor operation) throws CedarAssertionException {
    buildAndThrowAssertionExceptionIfNeeded(getFirstAssertionError(), operation,
        CedarAssertionResult.HTTP_INTERNAL_SERVER_ERROR);
  }

  @Override
  public void otherwiseNotFound(ICedarOperationDescriptor operation) throws CedarAssertionException {
    buildAndThrowAssertionExceptionIfNeeded(getFirstAssertionError(), operation,
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
                                                       ICedarOperationDescriptor operation, int errorCode) throws
      CedarAssertionException {
    System.out.println("Build ex and throw");
    if (assertionResult != null) {
      System.out.println("assertion result:" + assertionResult);
      assertionResult.setCode(errorCode);
      CedarAssertionException ex = new CedarAssertionException(assertionResult);
      if (operation != null) {
        System.out.println("op:" + operation);
        System.out.println(" **** * HERE add Op Description to EX" + operation);
      }
      throw ex;
    }
  }


}
