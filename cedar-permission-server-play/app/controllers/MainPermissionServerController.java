package controllers;

import org.metadatacenter.server.play.AbstractCedarController;
import play.mvc.Result;

public class MainPermissionServerController extends AbstractPermissionServerController {

  public static Result index() {
    return ok("CEDAR Permission Server.");
  }

  /* For CORS */
  public static Result preflight(String all) {
    return AbstractCedarController.preflight(all);
  }

}
