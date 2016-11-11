package controllers;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.server.play.AbstractCedarController;

public class AbstractPermissionServerController extends AbstractCedarController {

  protected static final CedarConfig cedarConfig;

  static {
    cedarConfig = CedarConfig.getInstance();
  }

}