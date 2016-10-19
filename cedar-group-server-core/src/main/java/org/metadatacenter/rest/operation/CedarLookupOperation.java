package org.metadatacenter.rest.operation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.rest.CedarOperationDescriptor;
import org.metadatacenter.util.json.JsonMapper;

public class CedarLookupOperation implements CedarOperationDescriptor {

  private final Class clazz;
  private final String lookupAttributeName;
  private final Object lookupAttributeValue;

  public CedarLookupOperation(Class clazz, String lookupAttributeName, Object lookupAttributeValue) {
    this.clazz = clazz;
    this.lookupAttributeName = lookupAttributeName;
    this.lookupAttributeValue = lookupAttributeValue;
  }

  public Class getClazz() {
    return clazz;
  }

  public String getLookupAttributeName() {
    return lookupAttributeName;
  }

  public Object getLookupAttributeValue() {
    return lookupAttributeValue;
  }

  @Override
  public JsonNode asJson() {
    ObjectNode objectNode = JsonMapper.MAPPER.createObjectNode();
    objectNode.put("type", "lookup");
    objectNode.put("className", clazz.getName());
    objectNode.put("simpleClassName", clazz.getSimpleName());
    objectNode.put("lookupAttributeName", lookupAttributeName);
    objectNode.put("lookupAttributeValue", lookupAttributeValue == null ? null : lookupAttributeValue.toString());
    return objectNode;
  }

}
