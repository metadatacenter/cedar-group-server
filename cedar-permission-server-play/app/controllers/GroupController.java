package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.model.folderserver.CedarFSGroup;
import org.metadatacenter.model.response.FSGroupListResponse;
import org.metadatacenter.rest.*;
import org.metadatacenter.rest.bridge.CedarDataServices;
import org.metadatacenter.server.neo4j.Neo4JUserSession;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.security.model.user.CedarUser;
import org.metadatacenter.util.json.JsonMapper;
import play.mvc.Result;
import utils.DataServices;

import java.util.List;

public class GroupController extends AbstractPermissionServerController {


  public static Result findGroups() {
    IAuthRequest frontendRequest = null;
    CedarUser currentUser = null;
    try {
      frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      currentUser = Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.LOGGED_IN);
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while reading the groups", e);
      return forbiddenWithError(e);
    }

    try {
      Neo4JUserSession neoSession = DataServices.getInstance().getNeo4JSession(currentUser);

      List<CedarFSGroup> groups = neoSession.findGroups();

      FSGroupListResponse r = new FSGroupListResponse();

      r.setGroups(groups);

      JsonNode resp = JsonMapper.MAPPER.valueToTree(r);
      return ok(resp);

    } catch (Exception e) {
      play.Logger.error("Error while listing groups", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result createGroup() {
    ICedarRequestContext c = CedarRequestContextFactory.fromRequest(request());

    c.must(c.user()).be(GenericAssertions.loggedIn);
    c.must(c.user()).have(CedarPermission.GROUP_CREATE);

    c.must(c.request()).be(GenericAssertions.jsonBody, GenericAssertions.nonEmpty);
    ICedarRequestBody requestBody = c.request().jsonBody();

    ICedarParameter groupName = requestBody.get("name");
    ICedarParameter groupDescription = requestBody.get("description");
    c.must(groupName, groupDescription).allPresent();

    Neo4JUserSession neoSession = CedarDataServices.getNeo4jSession(c);

    CedarFSGroup oldGroup = neoSession.findGroupByName(groupName.stringValue());
    c.must(oldGroup).beNull(c.operation().lookup(CedarFSGroup.class, "name", groupName));

    CedarFSGroup newGroup = neoSession.createGroup(groupName.stringValue(), groupName.stringValue(),
        groupDescription.stringValue(), c.getCedarUser().getId());
    c.must(newGroup).beNotNull(c.operation().create(CedarFSGroup.class, "name", groupName));

    JsonNode createdGroup = JsonMapper.MAPPER.valueToTree(newGroup);
    String absoluteUrl = routes.GroupController.findGroup(newGroup.getId()).absoluteURL(request());
    response().setHeader(HttpConstants.HTTP_HEADER_LOCATION, absoluteUrl);
    return created(createdGroup);
  }

  public static Result findGroup(String id) {
    ICedarRequestContext c = CedarRequestContextFactory.fromRequest(request());

    c.must(c.user()).be(GenericAssertions.loggedIn);
    c.must(c.user()).have(CedarPermission.GROUP_READ);

    Neo4JUserSession neoSession = CedarDataServices.getNeo4jSession(c);

    CedarFSGroup group = neoSession.findGroupById(id);
    //TODO : how do we differentiate between this benotnull + lookup, which should result in 404
    // and the beNull + lookup  above, in create, which should result in illegal argument
    // or the other beNotNull + create, whichshould result in internal server error.
    c.must(group).beNotNull(c.operation().lookup(CedarFSGroup.class, "id", id));

    JsonNode foundGroup = JsonMapper.MAPPER.valueToTree(group);
    return ok(foundGroup);
  }

  public static Result patchGroup(String id) {
    return play.mvc.Results.TODO;
  }

  public static Result updateGroup(String id) {
    return play.mvc.Results.TODO;
  }

  public static Result deleteGroup(String id) {
    return play.mvc.Results.TODO;
  }

  public static Result getGroupMembers(String id) {
    return play.mvc.Results.TODO;
  }

  public static Result updateGroupMembers(String id) {
    return play.mvc.Results.TODO;
  }
}