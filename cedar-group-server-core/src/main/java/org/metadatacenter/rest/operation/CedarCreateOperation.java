package org.metadatacenter.rest.operation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.rest.CedarOperationDescriptor;
import org.metadatacenter.util.json.JsonMapper;

public class CedarCreateOperation implements CedarOperationDescriptor {

  private final Class clazz;
  private final String primaryIdAttributeName;
  private final Object primaryIdAttributeValue;

  public CedarCreateOperation(Class clazz, String primaryIdAttributeName, Object primaryIdAttributeValue) {
    this.clazz = clazz;
    this.primaryIdAttributeName = primaryIdAttributeName;
    this.primaryIdAttributeValue = primaryIdAttributeValue;
  }

  public Class getClazz() {
    return clazz;
  }

  public String getPrimaryIdAttributeName() {
    return primaryIdAttributeName;
  }

  public Object getPrimaryIdAttributeValue() {
    return primaryIdAttributeValue;
  }

  @Override
  public JsonNode asJson() {
    ObjectNode objectNode = JsonMapper.MAPPER.createObjectNode();
    objectNode.put("type", "create");
    objectNode.put("className", clazz.getName());
    objectNode.put("simpleClassName", clazz.getSimpleName());
    objectNode.put("primaryIdAttributeName", primaryIdAttributeName);
    objectNode.put("primaryIdAttributeValue", primaryIdAttributeValue == null ? null :
        primaryIdAttributeValue.toString());
    return objectNode;
  }
}
