package org.metadatacenter.rest.assertion.noun;

import org.metadatacenter.rest.ICedarAssertionNoun;

public interface ICedarRequest extends ICedarAssertionNoun {

  ICedarRequestBody getJsonBody();

  String getContentType();

}
