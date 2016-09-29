package controllers;

import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import play.mvc.Result;

public class TemplateController extends AbstractPermissionServerController {

  public static Result getTemplatePermissions(String templateId) {
    boolean canProceed = false;
    try {
      IAuthRequest frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.TEMPLATE_READ);
      if (userHasReadAccessToResource(folderBase, templateId)) {
        canProceed = true;
      }
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while reading the template permissions", e);
      return forbiddenWithError(e);
    }
    if (canProceed) {
      return getPermissions(templateId);
    } else {
      return forbidden("You do not have read access for this template");
    }
  }

  public static Result updateTemplatePermissions(String templateId) {
    boolean canProceed = false;
    try {
      IAuthRequest frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.TEMPLATE_UPDATE);
      if (userHasWriteAccessToResource(folderBase, templateId)) {
        canProceed = true;
      }
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while updating the template permissions", e);
      return forbiddenWithError(e);
    }
    if (canProceed) {
      return updatePermissions(templateId);
    } else {
      return forbidden("You do not have write access for this template");
    }
  }

}
