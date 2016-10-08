package org.metadatacenter.rest.assertion.noun;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.context.CedarParameterSource;

public interface ICedarParameter extends ICedarAssertionNoun {

  String stringValue();

  String getName();

  CedarParameterSource getSource();

  boolean isNull();

  boolean isMissing();
}
