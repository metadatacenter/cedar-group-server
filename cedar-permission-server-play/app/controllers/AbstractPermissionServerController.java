package controllers;

import org.metadatacenter.cedar.resource.util.FolderServerProxy;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.model.folderserver.CedarFSFolder;
import org.metadatacenter.model.folderserver.CedarFSResource;
import org.metadatacenter.server.play.AbstractCedarController;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.auth.NodePermission;

public class AbstractPermissionServerController extends AbstractCedarController {

  protected static final String PREFIX_RESOURCES = "resources";

  protected static CedarConfig cedarConfig;
  protected final static String folderBase;

  static {
    cedarConfig = CedarConfig.getInstance();
    folderBase = cedarConfig.getServers().getFolder().getBase();
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