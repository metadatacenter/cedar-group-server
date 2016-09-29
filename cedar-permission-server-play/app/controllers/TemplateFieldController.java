package controllers;

import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import play.mvc.Result;

public class TemplateFieldController extends AbstractPermissionServerController {

  public static Result getTemplateFieldPermissions(String fieldId) {
    boolean canProceed = false;
    try {
      IAuthRequest frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.TEMPLATE_FIELD_READ);
      if (userHasReadAccessToResource(folderBase, fieldId)) {
        canProceed = true;
      }
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while reading the field permissions", e);
      return forbiddenWithError(e);
    }
    if (canProceed) {
      return getPermissions(fieldId);
    } else {
      return forbidden("You do not have read access for this field");
    }
  }

  public static Result updateTemplateFieldPermissions(String fieldId) {
    boolean canProceed = false;
    try {
      IAuthRequest frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.TEMPLATE_FIELD_UPDATE);
      if (userHasWriteAccessToResource(folderBase, fieldId)) {
        canProceed = true;
      }
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while updating the field permissions", e);
      return forbiddenWithError(e);
    }
    if (canProceed) {
      return updatePermissions(fieldId);
    } else {
      return forbidden("You do not have write access for this field");
    }
  }

}
