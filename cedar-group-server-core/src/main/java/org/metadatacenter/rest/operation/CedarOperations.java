package org.metadatacenter.rest.operation;

import org.metadatacenter.rest.assertion.noun.ICedarParameter;

public final class CedarOperations {

  private CedarOperations() {
  }

  public static CedarLookupOperation lookup(Class clazz, String lookupAttributeName, ICedarParameter
      lookupAttributeValue) {
    return new CedarLookupOperation(clazz, lookupAttributeName, lookupAttributeValue.stringValue());
  }

  public static CedarLookupOperation lookup(Class clazz, String lookupAttributeName, String lookupAttributeValue) {
    return new CedarLookupOperation(clazz, lookupAttributeName, lookupAttributeValue);
  }

  public static CedarCreateOperation create(Class clazz, String primaryIdAttributeName, ICedarParameter
      primaryIdAttributeValue) {
    return new CedarCreateOperation(clazz, primaryIdAttributeName, primaryIdAttributeValue);
  }

  public static CedarUpdateOperation update(Class clazz, String lookupAttributeName, String lookupAttributeValue) {
    return new CedarUpdateOperation(clazz, lookupAttributeName, lookupAttributeValue);
  }

  public static CedarDeleteOperation delete(Class clazz, String lookupAttributeName, String lookupAttributeValue) {
    return new CedarDeleteOperation(clazz, lookupAttributeName, lookupAttributeValue);
  }

}

