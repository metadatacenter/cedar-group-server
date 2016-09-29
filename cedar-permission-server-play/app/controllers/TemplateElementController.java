package controllers;

import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import play.mvc.Result;

public class TemplateElementController extends AbstractPermissionServerController {

  public static Result getTemplateElementPermissions(String elementId) {
    boolean canProceed = false;
    try {
      IAuthRequest frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.TEMPLATE_ELEMENT_READ);
      if (userHasReadAccessToResource(folderBase, elementId)) {
        canProceed = true;
      }
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while reading the element permissions", e);
      return forbiddenWithError(e);
    }
    if (canProceed) {
      return getPermissions(elementId);
    } else {
      return forbidden("You do not have read access for this element");
    }
  }

  public static Result updateTemplateElementPermissions(String elementId) {
    boolean canProceed = false;
    try {
      IAuthRequest frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.TEMPLATE_ELEMENT_UPDATE);
      if (userHasWriteAccessToResource(folderBase, elementId)) {
        canProceed = true;
      }
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while updating the element permissions", e);
      return forbiddenWithError(e);
    }
    if (canProceed) {
      return updatePermissions(elementId);
    } else {
      return forbidden("You do not have write access for this element");
    }
  }

}
