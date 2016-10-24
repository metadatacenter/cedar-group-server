package org.metadatacenter.rest.assertion.noun;

import org.metadatacenter.rest.CedarAssertionNoun;
import org.metadatacenter.rest.exception.CedarAssertionException;

public interface CedarRequestBody extends CedarAssertionNoun {

  CedarParameter get(String name);

  <T> T convert(Class<T> type) throws CedarAssertionException;
}
