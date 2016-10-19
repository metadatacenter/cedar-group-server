package controllers;

import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.model.folderserver.CedarFSGroup;
import org.metadatacenter.model.response.FSGroupListResponse;
import org.metadatacenter.rest.assertion.GenericAssertions;
import org.metadatacenter.rest.assertion.noun.ICedarParameter;
import org.metadatacenter.rest.assertion.noun.ICedarRequestBody;
import org.metadatacenter.rest.bridge.CedarDataServices;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.rest.exception.CedarAssertionResult;
import org.metadatacenter.rest.operation.CedarOperations;
import org.metadatacenter.server.neo4j.Neo4JFields;
import org.metadatacenter.server.neo4j.Neo4JUserSession;
import org.metadatacenter.server.result.BackendCallResult;
import org.metadatacenter.server.security.model.auth.CedarGroupUser;
import org.metadatacenter.server.security.model.auth.CedarGroupUsers;
import org.metadatacenter.server.security.model.auth.CedarGroupUsersRequest;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import play.mvc.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupController extends AbstractPermissionServerController {


  public static Result findGroups() throws CedarAssertionException {
    ICedarRequestContext c = CedarRequestContextFactory.fromRequest(request());
    c.must(c.user()).be(GenericAssertions.loggedIn);

    Neo4JUserSession neoSession = CedarDataServices.getNeo4jSession(c);
    List<CedarFSGroup> groups = neoSession.findGroups();

    FSGroupListResponse r = new FSGroupListResponse();
    r.setGroups(groups);
    return ok(asJson(r));
  }

  public static Result createGroup() throws CedarAssertionException {
    ICedarRequestContext c = CedarRequestContextFactory.fromRequest(request());

    c.must(c.user()).be(GenericAssertions.loggedIn);
    c.must(c.user()).have(CedarPermission.GROUP_CREATE);

    c.must(c.request()).be(GenericAssertions.jsonBody);
    ICedarRequestBody requestBody = c.request().getJsonBody();

    ICedarParameter groupName = requestBody.get("name");
    ICedarParameter groupDescription = requestBody.get("description");
    c.should(groupName, groupDescription).be(GenericAssertions.isNotNull).otherwiseBadRequest();

    Neo4JUserSession neoSession = CedarDataServices.getNeo4jSession(c);

    CedarFSGroup oldGroup = neoSession.findGroupByName(groupName.stringValue());
    c.should(oldGroup).be(GenericAssertions.isNull).otherwiseBadRequest(
        CedarOperations.lookup(CedarFSGroup.class, "name", groupName),
        "There is a group with the same name present in the system. Group names must be unique!");

    CedarFSGroup newGroup = neoSession.createGroup(groupName.stringValue(), groupName.stringValue(),
        groupDescription.stringValue());
    c.should(newGroup).be(GenericAssertions.isNotNull).otherwiseInternalServerError(
        CedarOperations.create(CedarFSGroup.class, "name", groupName),
        "There was an error while creating the group!"
    );

    String absoluteUrl = routes.GroupController.findGroup(newGroup.getId()).absoluteURL(request());
    response().setHeader(HttpConstants.HTTP_HEADER_LOCATION, absoluteUrl);
    return created(asJson(newGroup));
  }

  public static Result findGroup(String id) throws CedarAssertionException {
    ICedarRequestContext c = CedarRequestContextFactory.fromRequest(request());

    c.must(c.user()).be(GenericAssertions.loggedIn);
    c.must(c.user()).have(CedarPermission.GROUP_READ);

    Neo4JUserSession neoSession = CedarDataServices.getNeo4jSession(c);

    CedarFSGroup group = neoSession.findGroupById(id);
    c.should(group).be(GenericAssertions.isNotNull).otherwiseNotFound(
        CedarOperations.lookup(CedarFSGroup.class, "id", id),
        "The group can not be found by id!");

    return ok(asJson(group));
  }

  public static Result updateGroup(String id) throws CedarAssertionException {
    ICedarRequestContext c = CedarRequestContextFactory.fromRequest(request());

    c.must(c.user()).be(GenericAssertions.loggedIn);
    c.must(c.user()).have(CedarPermission.GROUP_UPDATE);

    c.must(c.request()).be(GenericAssertions.jsonBody);
    ICedarRequestBody requestBody = c.request().getJsonBody();

    Neo4JUserSession neoSession = CedarDataServices.getNeo4jSession(c);
    CedarFSGroup existingGroup = findNonSpecialGroupById(c, neoSession, id);

    ICedarParameter groupName = requestBody.get("name");
    ICedarParameter groupDescription = requestBody.get("description");
    c.should(groupName, groupDescription).be(GenericAssertions.isNotNull).otherwiseBadRequest();

    // check if the new name is unique
    CedarFSGroup otherGroup = neoSession.findGroupByName(groupName.stringValue());
    checkUniqueness(otherGroup, existingGroup);

    Map<String, String> updateFields = new HashMap<>();
    updateFields.put(Neo4JFields.NAME, groupName.stringValue());
    updateFields.put(Neo4JFields.DISPLAY_NAME, groupName.stringValue());
    updateFields.put(Neo4JFields.DESCRIPTION, groupDescription.stringValue());
    CedarFSGroup updatedGroup = neoSession.updateGroupById(id, updateFields);

    c.should(updatedGroup).be(GenericAssertions.isNotNull).otherwiseInternalServerError(
        CedarOperations.update(CedarFSGroup.class, "id", id),
        "There was an error while updating the group!"
    );

    return ok(asJson(updatedGroup));
  }

  public static Result patchGroup(String id) throws CedarAssertionException {
    ICedarRequestContext c = CedarRequestContextFactory.fromRequest(request());

    c.must(c.user()).be(GenericAssertions.loggedIn);
    c.must(c.user()).have(CedarPermission.GROUP_UPDATE);

    //c.must(c.request()).be(GenericAssertions.jsonBody);
    c.must(c.request()).be(GenericAssertions.jsonMergePatch);
    ICedarRequestBody requestBody = c.request().getJsonBody();

    Neo4JUserSession neoSession = CedarDataServices.getNeo4jSession(c);
    CedarFSGroup existingGroup = findNonSpecialGroupById(c, neoSession, id);

    ICedarParameter groupName = requestBody.get("name");
    ICedarParameter groupDescription = requestBody.get("description");

    boolean updateName = existingGroup.getName().equals(groupName.stringValue());
    boolean updateDescription = existingGroup.getDescription().equals(groupDescription.stringValue());

    if (!updateName && !updateDescription) {
      return ok(asJson(existingGroup));
    }

    // check if the new name is unique
    if (updateName) {
      CedarFSGroup otherGroup = neoSession.findGroupByName(groupName.stringValue());
      checkUniqueness(otherGroup, existingGroup);
    }

    Map<String, String> updateFields = new HashMap<>();
    if (updateName) {
      updateFields.put(Neo4JFields.NAME, groupName.stringValue());
      updateFields.put(Neo4JFields.DISPLAY_NAME, groupName.stringValue());
    }
    if (updateDescription) {
      updateFields.put(Neo4JFields.DESCRIPTION, groupDescription.stringValue());
    }
    CedarFSGroup updatedGroup = neoSession.updateGroupById(id, updateFields);

    c.should(updatedGroup).be(GenericAssertions.isNotNull).otherwiseInternalServerError(
        CedarOperations.update(CedarFSGroup.class, "id", id),
        "There was an error while updating the group!"
    );

    return ok(asJson(updatedGroup));
  }

  private static CedarFSGroup findNonSpecialGroupById(ICedarRequestContext c, Neo4JUserSession neoSession, String id)
      throws CedarAssertionException {
    CedarFSGroup existingGroup = neoSession.findGroupById(id);
    c.should(existingGroup).be(GenericAssertions.isNotNull).otherwiseNotFound(
        CedarOperations.lookup(CedarFSGroup.class, "id", id),
        "The group can not be found by id!"
    );

    if (existingGroup.getSpecialGroup() != null) {
      CedarAssertionResult ar = new CedarAssertionResult("Special groups can not be modified!")
          .setParameter("id", id)
          .setParameter("specialGroup", existingGroup.getSpecialGroup())
          .badRequest();
      throw new CedarAssertionException(ar);
    }
    return existingGroup;
  }

  private static void checkUniqueness(CedarFSGroup otherGroup, CedarFSGroup existingGroup) throws
      CedarAssertionException {
    if (otherGroup != null && !otherGroup.getId().equals(existingGroup.getId())) {
      CedarAssertionResult ar = new CedarAssertionResult(
          "There is a group with the new name present in the system. Group names must be unique!")
          .setParameter("name", otherGroup.getName())
          .setParameter("id", otherGroup.getId())
          .badRequest();
      throw new CedarAssertionException(ar);
    }
  }

  public static Result deleteGroup(String id) throws CedarAssertionException {
    ICedarRequestContext c = CedarRequestContextFactory.fromRequest(request());

    c.must(c.user()).be(GenericAssertions.loggedIn);
    c.must(c.user()).have(CedarPermission.GROUP_DELETE);

    Neo4JUserSession neoSession = CedarDataServices.getNeo4jSession(c);
    CedarFSGroup existingGroup = neoSession.findGroupById(id);

    c.should(existingGroup).be(GenericAssertions.isNotNull).otherwiseNotFound(
        CedarOperations.lookup(CedarFSGroup.class, "id", id),
        "The group can not be found by id!");

    String specialGroup = existingGroup.getSpecialGroup();
    c.should(specialGroup).be(GenericAssertions.isNull).otherwiseBadRequest(
        CedarOperations.delete(CedarFSGroup.class, "id", id),
        "The special group '" + specialGroup + "'can not be deleted!");

    boolean deleted = neoSession.deleteGroupById(id);
    c.should(deleted).be(GenericAssertions.isTrue).otherwiseInternalServerError(
        CedarOperations.delete(CedarFSGroup.class, "id", id),
        "There was an error while deleting the group!"
    );

    return noContent();
  }

  public static Result getGroupMembers(String id) throws CedarAssertionException {
    ICedarRequestContext c = CedarRequestContextFactory.fromRequest(request());

    c.must(c.user()).be(GenericAssertions.loggedIn);
    c.must(c.user()).have(CedarPermission.GROUP_READ);

    Neo4JUserSession neoSession = CedarDataServices.getNeo4jSession(c);

    CedarFSGroup group = neoSession.findGroupById(id);
    c.should(group).be(GenericAssertions.isNotNull).otherwiseNotFound(
        CedarOperations.lookup(CedarFSGroup.class, "id", id),
        "The group can not be found by id!");

    CedarGroupUsers groupUsers = neoSession.findGroupUsers(id);
    c.should(groupUsers).be(GenericAssertions.isNotNull).otherwiseInternalServerError(
        CedarOperations.list(CedarFSGroup.class, "id", id),
        "There was an error while listing the group users!"
    );
    return ok(asJson(groupUsers));
  }

  public static Result updateGroupMembers(String id) throws CedarAssertionException {
    ICedarRequestContext c = CedarRequestContextFactory.fromRequest(request());

    c.must(c.user()).be(GenericAssertions.loggedIn);
    c.must(c.user()).have(CedarPermission.GROUP_UPDATE);

    Neo4JUserSession neoSession = CedarDataServices.getNeo4jSession(c);

    CedarFSGroup group = neoSession.findGroupById(id);
    c.should(group).be(GenericAssertions.isNotNull).otherwiseNotFound(
        CedarOperations.lookup(CedarFSGroup.class, "id", id),
        "The group can not be found by id!");

    CedarGroupUsers groupUsers = neoSession.findGroupUsers(id);
    c.should(groupUsers).be(GenericAssertions.isNotNull).otherwiseInternalServerError(
        CedarOperations.list(CedarFSGroup.class, "id", id),
        "There was an error while listing the group users!"
    );

    String currentUserId = neoSession.getUserId();
    boolean currentUserIsGroupAdmin = false;
    for(CedarGroupUser user : groupUsers.getUsers()) {
      if (currentUserId.equals(user.getUser().getId()) && user.isAdministrator()) {
        currentUserIsGroupAdmin = true;
        break;
      }
    }

    c.should(currentUserIsGroupAdmin).be(GenericAssertions.isTrue).otherwiseForbidden(
        CedarOperations.update(CedarFSGroup.class, "id", id),
        "Only the administrators can update the group!");

    ICedarRequestBody requestBody = c.request().getJsonBody();
    CedarGroupUsersRequest usersRequest = requestBody.as(CedarGroupUsersRequest.class);

    BackendCallResult backendCallResult = neoSession.updateGroupUsers(id, usersRequest);
    //TODO : implement this instead of the if
    // c.backend(backendCallResult);
    // or
    // c.backend(neoSession.updateGroupUsers(id, usersRequest));
    if (backendCallResult.isError()) {
      return backendCallError(backendCallResult);
    }

    CedarGroupUsers updatedGroupUsers = neoSession.findGroupUsers(id);
    return ok(asJson(updatedGroupUsers));
  }
}