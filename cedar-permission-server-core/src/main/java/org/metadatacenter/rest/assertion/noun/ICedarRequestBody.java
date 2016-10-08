package org.metadatacenter.rest.assertion.noun;

import org.metadatacenter.rest.ICedarAssertionNoun;

public interface ICedarRequestBody extends ICedarAssertionNoun {

  ICedarParameter get(String name);

}
