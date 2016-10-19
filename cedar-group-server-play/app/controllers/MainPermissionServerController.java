package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.server.play.AbstractCedarController;
import play.mvc.Result;

import java.util.Map;
import java.util.HashMap;

public class MainPermissionServerController extends AbstractPermissionServerController {

  private final static Map<String, Object> indexResponse;
  private final static JsonNode indexResponseNode;

  static {
    indexResponse = new HashMap<>();
    indexResponse.put("serverName", "cedar-permission-server");
    indexResponse.put("serverDescription", "CEDAR Permission Server.");
    indexResponseNode = asJson(indexResponse);
  }

  public static Result index() {
    return ok(indexResponseNode);
  }

  /* For CORS */
  public static Result preflight(String all) {
    return AbstractCedarController.preflight(all);
  }

}
