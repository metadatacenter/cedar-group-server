package controllers;

import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import play.mvc.Result;

public class TemplateInstanceController extends AbstractPermissionServerController {

  public static Result getTemplateInstancePermissions(String instanceId) {
    boolean canProceed = false;
    try {
      IAuthRequest frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.TEMPLATE_INSTANCE_READ);
      if (userHasReadAccessToResource(folderBase, instanceId)) {
        canProceed = true;
      }
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while reading the instance permissions", e);
      return forbiddenWithError(e);
    }
    if (canProceed) {
      return getPermissions(instanceId);
    } else {
      return forbidden("You do not have read access for this instance");
    }
  }

  public static Result updateTemplateInstancePermissions(String instanceId) {
    boolean canProceed = false;
    try {
      IAuthRequest frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.TEMPLATE_INSTANCE_UPDATE);
      if (userHasWriteAccessToResource(folderBase, instanceId)) {
        canProceed = true;
      }
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while updating the instance permissions", e);
      return forbiddenWithError(e);
    }
    if (canProceed) {
      return updatePermissions(instanceId);
    } else {
      return forbidden("You do not have write access for this instance");
    }
  }

}
