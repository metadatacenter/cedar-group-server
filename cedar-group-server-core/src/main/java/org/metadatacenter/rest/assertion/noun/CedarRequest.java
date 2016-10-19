package org.metadatacenter.rest.assertion.noun;

import org.metadatacenter.rest.CedarAssertionNoun;

public interface CedarRequest extends CedarAssertionNoun {

  CedarRequestBody getJsonBody();

  String getContentType();

}
