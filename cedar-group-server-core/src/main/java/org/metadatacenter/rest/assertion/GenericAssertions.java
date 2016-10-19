package org.metadatacenter.rest.assertion;

public class GenericAssertions {
  public static LoggedIn loggedIn = new LoggedIn();
  public static JsonBody jsonBody = new JsonBody();
  public static NonEmpty nonEmpty = new NonEmpty();
  public static IsNull isNull = new IsNull();
  public static IsNotNull isNotNull = new IsNotNull();
  public static CedarAssertion jsonMergePatch = new JsonMergePatch();
  public static CedarAssertion isTrue = new IsTrue();
}
