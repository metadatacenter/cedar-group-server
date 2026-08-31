package org.metadatacenter.cedar.group.resources;

import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;

public abstract class AbstractGroupServerResource extends CedarMicroserviceResource {

  public AbstractGroupServerResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  public AbstractGroupServerResource(CedarConfig cedarConfig, CedarDataServices dataServices) {
    super(cedarConfig, dataServices);
  }

}
