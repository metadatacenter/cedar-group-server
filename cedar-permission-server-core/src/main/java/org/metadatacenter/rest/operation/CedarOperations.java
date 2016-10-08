package org.metadatacenter.rest.operation;

import org.metadatacenter.rest.assertion.noun.ICedarParameter;

public final class CedarOperations {

  private CedarOperations() {
  }

  public static CedarLookupOperation lookup(Class clazz, String lookupAttributeName, ICedarParameter
      lookupAttributeValue) {
    return new CedarLookupOperation();
  }

  public static CedarLookupOperation lookup(Class clazz, String lookupAttributeName, String lookupAttributeValue) {
    return new CedarLookupOperation();
  }

  public static CedarCreateOperation create(Class clazz, String primaryIdAttributeName, ICedarParameter
      primaryIdAttributeValue) {
    return new CedarCreateOperation();
  }
}
