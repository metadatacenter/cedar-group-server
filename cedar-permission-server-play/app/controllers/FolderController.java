package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.model.folderserver.CedarFSFolder;
import org.metadatacenter.server.neo4j.Neo4JUserSession;
import org.metadatacenter.server.result.BackendCallResult;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.auth.CedarNodePermissions;
import org.metadatacenter.server.security.model.auth.CedarNodePermissionsRequest;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.security.model.user.CedarUser;
import org.metadatacenter.util.json.JsonMapper;
import play.mvc.Result;
import utils.DataServices;

public class FolderController extends AbstractPermissionServerController {

  public static Result getPermissions(String folderId) {
    IAuthRequest frontendRequest = null;
    CedarUser currentUser = null;
    try {
      frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      currentUser = Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.LOGGED_IN);
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while reading the permissions of folder", e);
      return forbiddenWithError(e);
    }

    try {
      Neo4JUserSession neoSession = DataServices.getInstance().getNeo4JSession(currentUser);

      CedarFSFolder folder = neoSession.findFolderById(folderId);
      if (folder == null) {
        ObjectNode errorParams = JsonNodeFactory.instance.objectNode();
        errorParams.put("id", folderId);
        return notFound(generateErrorDescription("folderNotFound",
            "The folder can not be found by id:" + folderId, errorParams));
      } else {
        CedarNodePermissions permissions = neoSession.getNodePermissions(folderId, true);
        JsonNode permissionsNode = JsonMapper.MAPPER.valueToTree(permissions);
        return ok(permissionsNode);
      }
    } catch (Exception e) {
      play.Logger.error("Error while getting the folder", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result updatePermissions(String folderId) {
    IAuthRequest frontendRequest = null;
    CedarUser currentUser = null;
    try {
      frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      currentUser = Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.LOGGED_IN);
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while updating the folder permissions", e);
      return forbiddenWithError(e);
    }

    try {
      JsonNode permissionUpdateRequest = request().body().asJson();
      if (permissionUpdateRequest == null) {
        return badRequest(generateErrorDescription("missingRequestBody",
            "You must supply the request body as a json object!"));
      }

      Neo4JUserSession neoSession = DataServices.getInstance().getNeo4JSession(currentUser);

      CedarNodePermissionsRequest permissionsRequest = JsonMapper.MAPPER.treeToValue(permissionUpdateRequest,
          CedarNodePermissionsRequest.class);

      CedarFSFolder folder = neoSession.findFolderById(folderId);
      if (folder == null) {
        ObjectNode errorParams = JsonNodeFactory.instance.objectNode();
        errorParams.put("id", folderId);
        return notFound(generateErrorDescription("folderNotFound",
            "The folder can not be found by id:" + folderId, errorParams));
      } else {
        BackendCallResult backendCallResult = neoSession.updateNodePermissions(folderId, permissionsRequest, true);
        if (backendCallResult.isError()) {
          return backendCallError(backendCallResult);
        }
        CedarNodePermissions permissions = neoSession.getNodePermissions(folderId, true);
        JsonNode permissionsNode = JsonMapper.MAPPER.valueToTree(permissions);
        return ok(permissionsNode);
      }
    } catch (Exception e) {
      play.Logger.error("Error while updating the folder permissions", e);
      return internalServerErrorWithError(e);
    }
  }

}
