package org.metadatacenter.rest.assertion.noun;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.exception.CedarAssertionException;

public interface ICedarRequestBody extends ICedarAssertionNoun {

  ICedarParameter get(String name);

  <T> T as(Class<T> type) throws CedarAssertionException;
}
