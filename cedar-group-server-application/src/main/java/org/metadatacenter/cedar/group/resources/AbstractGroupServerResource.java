package org.metadatacenter.cedar.group.resources;

import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;

public class AbstractGroupServerResource extends CedarMicroserviceResource {

  protected final org.metadatacenter.bridge.CedarDataServices dataServices;

  protected AbstractGroupServerResource(CedarConfig cedarConfig) {
    this(cedarConfig, org.metadatacenter.bridge.CedarDataServices.getInstance());
  }

  protected AbstractGroupServerResource(CedarConfig cedarConfig, org.metadatacenter.bridge.CedarDataServices dataServices) {
    super(cedarConfig);
    this.dataServices = dataServices;
  }

}
