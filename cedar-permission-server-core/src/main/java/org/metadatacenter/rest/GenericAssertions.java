package org.metadatacenter.rest;

import org.metadatacenter.rest.assertion.JsonBody;
import org.metadatacenter.rest.assertion.LoggedIn;
import org.metadatacenter.rest.assertion.NonEmpty;

public class GenericAssertions {
  public static LoggedIn loggedIn;
  public static JsonBody jsonBody;
  public static NonEmpty nonEmpty;
}
