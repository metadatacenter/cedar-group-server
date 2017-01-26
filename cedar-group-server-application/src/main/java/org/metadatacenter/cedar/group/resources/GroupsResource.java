package org.metadatacenter.cedar.group.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.jersey.PATCH;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.error.CedarAssertionResult;
import org.metadatacenter.exception.CedarBackendException;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.folderserver.FolderServerGroup;
import org.metadatacenter.model.response.FolderServerGroupListResponse;
import org.metadatacenter.operation.CedarOperations;
import org.metadatacenter.rest.assertion.noun.CedarParameter;
import org.metadatacenter.rest.assertion.noun.CedarRequestBody;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.server.GroupServiceSession;
import org.metadatacenter.server.neo4j.Neo4JFields;
import org.metadatacenter.server.result.BackendCallResult;
import org.metadatacenter.server.security.model.auth.CedarGroupUsers;
import org.metadatacenter.server.security.model.auth.CedarGroupUsersRequest;
import org.metadatacenter.util.http.CedarUrlUtil;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.metadatacenter.constant.CedarPathParameters.PP_ID;
import static org.metadatacenter.rest.assertion.GenericAssertions.*;
import static org.metadatacenter.server.security.model.auth.CedarPermission.*;

@Path("/groups")
@Produces(MediaType.APPLICATION_JSON)
public class GroupsResource {

  private
  @Context
  UriInfo uriInfo;

  private
  @Context
  HttpServletRequest request;

  public GroupsResource() {
  }

  @GET
  @Timed
  public Response findGroups() throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_READ);

    GroupServiceSession groupSession = CedarDataServices.getGroupServiceSession(c);
    List<FolderServerGroup> groups = groupSession.findGroups();

    FolderServerGroupListResponse r = new FolderServerGroupListResponse();
    r.setGroups(groups);

    return Response.ok().entity(r).build();
  }

  @POST
  @Timed
  public Response createGroup() throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_CREATE);

    CedarRequestBody requestBody = c.request().getRequestBody();

    CedarParameter groupName = requestBody.get("name");
    CedarParameter groupDescription = requestBody.get("description");
    c.should(groupName, groupDescription).be(NonNull).otherwiseBadRequest();

    GroupServiceSession groupSession = CedarDataServices.getGroupServiceSession(c);

    FolderServerGroup oldGroup = groupSession.findGroupByName(groupName.stringValue());
    c.should(oldGroup).be(Null).otherwiseBadRequest(
        CedarOperations.lookup(FolderServerGroup.class, "name", groupName),
        "There is a group with the same name present in the system. Group names must be unique!");

    FolderServerGroup newGroup = groupSession.createGroup(groupName.stringValue(), groupName.stringValue(),
        groupDescription.stringValue());
    c.should(newGroup).be(NonNull).otherwiseInternalServerError(
        CedarOperations.create(FolderServerGroup.class, "name", groupName),
        "There was an error while creating the group!"
    );

    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    URI uri = builder.path(CedarUrlUtil.urlEncode(newGroup.getId())).build();
    return Response.created(uri).entity(newGroup).build();
  }

  @GET
  @Timed
  @Path("/{id}")
  public Response findGroup(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_READ);

    GroupServiceSession groupSession = CedarDataServices.getGroupServiceSession(c);

    FolderServerGroup group = groupSession.findGroupById(id);
    c.should(group).be(NonNull).otherwiseNotFound(
        CedarOperations.lookup(FolderServerGroup.class, "id", id),
        "The group can not be found by id!");

    // BackendCallResult<FolderServerGroup> bcr = groupSession.findGroupById(id);
    // c.must(backendCallResult).be(Successful);
    // c.must(backendCallResult).be(Found);
    // FolderServerGroup group = bcr.get();

    return Response.ok().entity(group).build();
  }

  @PUT
  @Timed
  @Path("/{id}")
  public Response updateGroup(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_UPDATE);

    CedarRequestBody requestBody = c.request().getRequestBody();

    GroupServiceSession groupSession = CedarDataServices.getGroupServiceSession(c);
    FolderServerGroup existingGroup = findNonSpecialGroupById(c, groupSession, id);

    CedarParameter groupName = requestBody.get("name");
    CedarParameter groupDescription = requestBody.get("description");
    c.should(groupName, groupDescription).be(NonNull).otherwiseBadRequest();

    // check if the new name is unique
    FolderServerGroup otherGroup = groupSession.findGroupByName(groupName.stringValue());
    checkUniqueness(otherGroup, existingGroup);

    Map<String, String> updateFields = new HashMap<>();
    updateFields.put(Neo4JFields.NAME, groupName.stringValue());
    updateFields.put(Neo4JFields.DISPLAY_NAME, groupName.stringValue());
    updateFields.put(Neo4JFields.DESCRIPTION, groupDescription.stringValue());
    FolderServerGroup updatedGroup = groupSession.updateGroupById(id, updateFields);

    c.should(updatedGroup).be(NonNull).otherwiseInternalServerError(
        CedarOperations.update(FolderServerGroup.class, "id", id),
        "There was an error while updating the group!"
    );

    // BackendCallResult<FolderServerGroup> bcr = groupSession.updateGroup(c, groupSession, id, updateFields);
    // c.must(backendCallResult).be(Successful); // InternalServerError, 404 NotFound, 403 Forbidden if special
    // FolderServerGroup existingGroup = bcr.get();

    return Response.ok().entity(updatedGroup).build();
  }

  private static void checkUniqueness(FolderServerGroup otherGroup, FolderServerGroup existingGroup) throws
      CedarException {
    if (otherGroup != null && !otherGroup.getId().equals(existingGroup.getId())) {
      CedarAssertionResult ar = new CedarAssertionResult(
          "There is a group with the new name present in the system. Group names must be unique!")
          .parameter("name", otherGroup.getName())
          .parameter("id", otherGroup.getId())
          .badRequest();
      throw new CedarBackendException(ar);
    }
  }

  private static FolderServerGroup findNonSpecialGroupById(CedarRequestContext c, GroupServiceSession groupSession,
                                                           String id) throws CedarException {
    FolderServerGroup existingGroup = groupSession.findGroupById(id);
    c.should(existingGroup).be(NonNull).otherwiseNotFound(
        CedarOperations.lookup(FolderServerGroup.class, "id", id),
        "The group can not be found by id!"
    );

    if (existingGroup.getSpecialGroup() != null) {
      CedarAssertionResult ar = new CedarAssertionResult("Special groups can not be modified!")
          .parameter("id", id)
          .parameter("specialGroup", existingGroup.getSpecialGroup())
          .badRequest();
      throw new CedarBackendException(ar);
    }
    return existingGroup;
  }

  @DELETE
  @Timed
  @Path("/{id}")
  public Response deleteGroup(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_DELETE);

    GroupServiceSession groupSession = CedarDataServices.getGroupServiceSession(c);
    FolderServerGroup existingGroup = groupSession.findGroupById(id);

    c.should(existingGroup).be(NonNull).otherwiseNotFound(
        CedarOperations.lookup(FolderServerGroup.class, "id", id),
        "The group can not be found by id!");

    String specialGroup = existingGroup.getSpecialGroup();
    c.should(specialGroup).be(Null).otherwiseBadRequest(
        CedarOperations.delete(FolderServerGroup.class, "id", id),
        "The special group '" + specialGroup + "'can not be deleted!");

    boolean deleted = groupSession.deleteGroupById(id);
    c.should(deleted).be(True).otherwiseInternalServerError(
        CedarOperations.delete(FolderServerGroup.class, "id", id),
        "There was an error while deleting the group!"
    );

    return Response.noContent().build();
  }

  @GET
  @Timed
  @Path("/{id}/users")
  public Response getGroupMembers(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_READ);

    GroupServiceSession groupSession = CedarDataServices.getGroupServiceSession(c);

    FolderServerGroup group = groupSession.findGroupById(id);
    c.should(group).be(NonNull).otherwiseNotFound(
        CedarOperations.lookup(FolderServerGroup.class, "id", id),
        "The group can not be found by id!");

    CedarGroupUsers groupUsers = groupSession.findGroupUsers(id);
    c.should(groupUsers).be(NonNull).otherwiseInternalServerError(
        CedarOperations.list(FolderServerGroup.class, "id", id),
        "There was an error while listing the group users!"
    );

    return Response.ok().entity(groupUsers).build();
  }

  @PUT
  @Timed
  @Path("/{id}/users")
  public Response updateGroupMembers(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_UPDATE);

    GroupServiceSession groupSession = CedarDataServices.getGroupServiceSession(c);

    FolderServerGroup group = groupSession.findGroupById(id);
    c.should(group).be(NonNull).otherwiseNotFound(
        CedarOperations.lookup(FolderServerGroup.class, "id", id),
        "The group can not be found by id!");


    boolean isAdministrator = groupSession.userAdministersGroup(id);
    c.should(isAdministrator).be(True).otherwiseForbidden(
        CedarOperations.update(FolderServerGroup.class, "id", id),
        "Only the administrators can update the group!");

    CedarRequestBody requestBody = c.request().getRequestBody();
    CedarGroupUsersRequest usersRequest = requestBody.convert(CedarGroupUsersRequest.class);

    BackendCallResult backendCallResult = groupSession.updateGroupUsers(id, usersRequest);
    c.must(backendCallResult).be(Successful);

    ADD permission update here
    // if the group members really were changed
    // we need to update the permissions on all the nodes that the group has access to

    CedarGroupUsers updatedGroupUsers = groupSession.findGroupUsers(id);

    return Response.ok().entity(updatedGroupUsers).build();
  }

  @PATCH
  @Timed
  @Path("/{id}")
  @Consumes("application/merge-patch+json")
  public Response patchGroup(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_UPDATE);

    //c.must(c.request()).be(GenericAssertions.jsonBody);
    c.must(c.request()).be(JsonMergePatch);
    CedarRequestBody requestBody = c.request().getRequestBody();

    GroupServiceSession groupSession = CedarDataServices.getGroupServiceSession(c);
    FolderServerGroup existingGroup = findNonSpecialGroupById(c, groupSession, id);

    CedarParameter groupName = requestBody.get("name");
    CedarParameter groupDescription = requestBody.get("description");

    boolean updateName = (groupName.stringValue() != null || groupName.isPresentAndNull())
        && theyDiffer(existingGroup.getName(), groupName.stringValue());
    boolean updateDescription = (groupDescription.stringValue() != null || groupDescription.isPresentAndNull())
        && theyDiffer(existingGroup.getDescription(), groupDescription.stringValue());

    if (!updateName && !updateDescription) {
      return Response.ok().entity(existingGroup).build();
    }

    // check if the new name is unique
    if (updateName) {
      FolderServerGroup otherGroup = groupSession.findGroupByName(groupName.stringValue());
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
    System.out.println(updateFields);
    FolderServerGroup updatedGroup = groupSession.updateGroupById(id, updateFields);

    c.should(updatedGroup).be(NonNull).otherwiseInternalServerError(
        CedarOperations.update(FolderServerGroup.class, "id", id),
        "There was an error while updating the group!"
    );

    return Response.ok().entity(updatedGroup).build();
  }

  private static boolean theyDiffer(String v1, String v2) {
    if (v1 == null) {
      return v2 != null;
    }
    if (v2 == null) {
      return v1 != null;
    }
    return !v1.equals(v2);
  }

}