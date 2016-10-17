package org.metadatacenter.rest.operation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.rest.ICedarOperationDescriptor;
import org.metadatacenter.util.json.JsonMapper;

public class CedarListOperation implements ICedarOperationDescriptor {

  private Class clazz;
  private String lookupAttributeName;
  private Object lookupAttributeValue;

  public CedarListOperation(Class clazz, String lookupAttributeName, Object lookupAttributeValue) {
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
    objectNode.put("type", "list");
    objectNode.put("className", clazz.getName());
    objectNode.put("simpleClassName", clazz.getSimpleName());
    objectNode.put("lookupAttributeName", lookupAttributeName);
    objectNode.put("lookupAttributeValue", lookupAttributeValue == null ? null : lookupAttributeValue.toString());
    return objectNode;
  }

}
