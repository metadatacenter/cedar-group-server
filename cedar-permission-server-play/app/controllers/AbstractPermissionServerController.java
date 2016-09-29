package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.cedar.resource.util.FolderServerProxy;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.model.folderserver.CedarFSFolder;
import org.metadatacenter.model.folderserver.CedarFSResource;
import org.metadatacenter.server.neo4j.Neo4JUserSession;
import org.metadatacenter.server.play.AbstractCedarController;
import org.metadatacenter.server.result.BackendCallResult;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.auth.CedarNodePermissions;
import org.metadatacenter.server.security.model.auth.CedarNodePermissionsRequest;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.security.model.auth.NodePermission;
import org.metadatacenter.server.security.model.user.CedarUser;
import org.metadatacenter.util.json.JsonMapper;
import play.mvc.Result;
import utils.DataServices;

public class AbstractPermissionServerController extends AbstractCedarController {

  protected static final String PREFIX_RESOURCES = "resources";

  protected static CedarConfig cedarConfig;
  protected final static String folderBase;

  static {
    cedarConfig = CedarConfig.getInstance();
    folderBase = cedarConfig.getServers().getFolder().getBase();
  }

  public static Result getPermissions(String resourceId) {
    IAuthRequest frontendRequest = null;
    CedarUser currentUser = null;
    try {
      frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      currentUser = Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.LOGGED_IN);
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while reading the permissions of resource", e);
      return forbiddenWithError(e);
    }

    try {
      Neo4JUserSession neoSession = DataServices.getInstance().getNeo4JSession(currentUser);

      CedarFSResource resource = neoSession.findResourceById(resourceId);
      if (resource == null) {
        ObjectNode errorParams = JsonNodeFactory.instance.objectNode();
        errorParams.put("id", resourceId);
        return notFound(generateErrorDescription("resourceNotFound",
            "The resource can not be found by id:" + resourceId, errorParams));
      } else {
        CedarNodePermissions permissions = neoSession.getNodePermissions(resourceId, false);
        JsonNode permissionsNode = JsonMapper.MAPPER.valueToTree(permissions);
        return ok(permissionsNode);
      }
    } catch (Exception e) {
      play.Logger.error("Error while getting the resource", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result updatePermissions(String resourceId) {
    IAuthRequest frontendRequest = null;
    CedarUser currentUser = null;
    try {
      frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      currentUser = Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.LOGGED_IN);
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while updating the resource permissions", e);
      return forbiddenWithError(e);
    }

    try {
      JsonNode permissionUpdateRequest = request().body().asJson();
      if (permissionUpdateRequest == null) {
        throw new IllegalArgumentException("You must supply the request body as a json object!");
      }

      Neo4JUserSession neoSession = DataServices.getInstance().getNeo4JSession(currentUser);

      CedarNodePermissionsRequest permissionsRequest = JsonMapper.MAPPER.treeToValue(permissionUpdateRequest,
          CedarNodePermissionsRequest.class);

      CedarFSResource resource = neoSession.findResourceById(resourceId);
      if (resource == null) {
        ObjectNode errorParams = JsonNodeFactory.instance.objectNode();
        errorParams.put("id", resourceId);
        return notFound(generateErrorDescription("resourceNotFound",
            "The resource can not be found by id:" + resourceId, errorParams));
      } else {
        BackendCallResult backendCallResult = neoSession.updateNodePermissions(resourceId, permissionsRequest, false);
        if (backendCallResult.isError()) {
          return backendCallError(backendCallResult);
        }
        CedarNodePermissions permissions = neoSession.getNodePermissions(resourceId, false);
        JsonNode permissionsNode = JsonMapper.MAPPER.valueToTree(permissions);
        return ok(permissionsNode);
      }
    } catch (Exception e) {
      play.Logger.error("Error while updating the resource permissions", e);
      return internalServerErrorWithError(e);
    }
  }

  protected static boolean userHasReadAccessToFolder(String folderBase, String
      folderId) throws CedarAccessException {
    String url = folderBase + CedarNodeType.Prefix.FOLDERS;
    CedarFSFolder fsFolder = FolderServerProxy.getFolder(url, folderId, request());
    if (fsFolder == null) {
      throw new IllegalArgumentException("Folder not found for id:" + folderId);
    }
    return fsFolder.currentUserCan(NodePermission.READ);
  }

  protected static boolean userHasWriteAccessToFolder(String folderBase, String
      folderId) throws CedarAccessException {
    String url = folderBase + CedarNodeType.Prefix.FOLDERS;
    CedarFSFolder fsFolder = FolderServerProxy.getFolder(url, folderId, request());
    if (fsFolder == null) {
      throw new IllegalArgumentException("Folder not found for id:" + folderId);
    }
    return fsFolder.currentUserCan(NodePermission.WRITE);
  }

  protected static boolean userHasReadAccessToResource(String folderBase, String
      nodeId) throws CedarAccessException {
    String url = folderBase + PREFIX_RESOURCES;
    CedarFSResource fsResource = FolderServerProxy.getResource(url, nodeId, request());
    if (fsResource == null) {
      throw new IllegalArgumentException("Resource not found:" + nodeId);
    }
    return fsResource.currentUserCan(NodePermission.READ);
  }

  protected static boolean userHasWriteAccessToResource(String folderBase, String
      nodeId) throws CedarAccessException {
    String url = folderBase + PREFIX_RESOURCES;
    CedarFSResource fsResource = FolderServerProxy.getResource(url, nodeId, request());
    if (fsResource == null) {
      throw new IllegalArgumentException("Resource not found:" + nodeId);
    }
    return fsResource.currentUserCan(NodePermission.WRITE);
  }

}