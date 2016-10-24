package org.metadatacenter.rest.assertion.noun;

import org.metadatacenter.rest.CedarAssertionNoun;
import org.metadatacenter.rest.exception.CedarAssertionException;

public interface CedarRequest extends CedarAssertionNoun {

  CedarRequestBody getRequestBody() throws CedarAssertionException;

  String getContentType();

}
