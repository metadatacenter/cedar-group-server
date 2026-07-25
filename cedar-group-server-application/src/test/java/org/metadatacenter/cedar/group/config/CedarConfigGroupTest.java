package org.metadatacenter.cedar.group.config;

import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.test.AbstractCedarConfigTest;

public class CedarConfigGroupTest extends AbstractCedarConfigTest {

  @Override
  protected SystemComponent getSystemComponent() {
    return SystemComponent.SERVER_GROUP;
  }

}
