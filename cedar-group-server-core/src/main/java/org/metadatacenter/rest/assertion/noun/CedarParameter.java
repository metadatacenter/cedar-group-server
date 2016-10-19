package org.metadatacenter.rest.assertion.noun;

import org.metadatacenter.rest.CedarAssertionNoun;
import org.metadatacenter.rest.context.CedarParameterSource;

public interface CedarParameter extends CedarAssertionNoun {

  String stringValue();

  String getName();

  CedarParameterSource getSource();

  boolean isNull();

  boolean isMissing();
}
