package org.metadatacenter.cedar.group.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.dropwizard.jersey.PATCH;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.error.CedarAssertionResult;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.error.CedarErrorPack;
import org.metadatacenter.exception.CedarBackendException;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.id.CedarGroupId;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.folderserver.basic.FolderServerGroup;
import org.metadatacenter.model.response.FolderServerGroupListResponse;
import org.metadatacenter.operation.CedarOperations;
import org.metadatacenter.rest.assertion.noun.CedarParameter;
import org.metadatacenter.rest.assertion.noun.CedarRequestBody;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.GroupServiceSession;
import org.metadatacenter.server.RevisionConflictException;
import org.metadatacenter.server.RevisionPrecondition;
import org.metadatacenter.server.VersionedGroupUsers;
import org.metadatacenter.server.VersionedResource;
import org.metadatacenter.server.neo4j.cypher.NodeProperty;
import org.metadatacenter.server.result.BackendCallResult;
import org.metadatacenter.server.search.permission.SearchPermissionEnqueueService;
import org.metadatacenter.server.security.model.auth.CedarGroupUsersRequest;
import org.metadatacenter.util.http.CedarUrlUtil;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.http.RevisionPreconditionParser;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.metadatacenter.constant.CedarPathParameters.PP_ID;
import static org.metadatacenter.constant.HttpConstants.CONTENT_TYPE_APPLICATION_MERGE_PATCH_JSON;
import static org.metadatacenter.error.CedarErrorKey.*;
import static org.metadatacenter.rest.assertion.GenericAssertions.*;
import static org.metadatacenter.server.security.model.auth.CedarPermission.*;

@Path("/groups")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Groups")
@SecurityRequirement(name = "api_key")
public class GroupsResource extends AbstractGroupServerResource {


  private static SearchPermissionEnqueueService searchPermissionEnqueueService;

  public GroupsResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  public static void injectSearchPermissionService(SearchPermissionEnqueueService searchPermissionEnqueueService) {
    GroupsResource.searchPermissionEnqueueService = searchPermissionEnqueueService;
  }

  @GET
  @Timed
  @Operation(summary = "List all groups",
      description = "Return every group in the system. Groups are visible to anyone who may read "
          + "them; membership is not what decides whether a group is listed.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Every group"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "The caller lacks the group read permission"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response findGroups() throws CedarException {
    CedarRequestContext c = buildRequestContext();

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_READ);

    GroupServiceSession groupSession = dataServices.getGroupServiceSession(c);
    List<FolderServerGroup> groups = groupSession.findGroups();

    FolderServerGroupListResponse r = new FolderServerGroupListResponse();
    r.setGroups(groups);

    return Response.ok().entity(r).build();
  }

  @POST
  @Timed
  @Operation(summary = "Create a group",
      description = "Create a group from a name and a description. Names are unique, so a name "
          + "already in use is refused rather than resolved.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "The group as created",
          headers = {
              @Header(name = "Location", description = "URL of the new group.", schema = @Schema(type = "string")),
              @Header(name = "ETag", description = "Strong validator for the group's current revision.", schema = @Schema(type = "string"))
          }),
      @ApiResponse(responseCode = "400", description = "The name or the description is missing"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "The caller lacks the group create permission"),
      @ApiResponse(responseCode = "409", description = "A group of that name already exists"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response createGroup() throws CedarException {
    CedarRequestContext c = buildRequestContext();

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_CREATE);

    CedarRequestBody requestBody = c.request().getRequestBody();

    CedarParameter groupName = requestBody.get("schema:name");
    CedarParameter groupDescription = requestBody.get("schema:description");
    c.should(groupName, groupDescription).be(NonNull).otherwiseBadRequest();

    GroupServiceSession groupSession = dataServices.getGroupServiceSession(c);

    FolderServerGroup oldGroup = groupSession.findGroupByName(groupName.stringValue());
    c.should(oldGroup).be(Null).otherwiseConflict(
        new CedarErrorPack()
            .message("There is a group with the same name present in the system. Group names must be unique!")
            .operation(CedarOperations.lookup(FolderServerGroup.class, "schema:name", groupName))
            .errorKey(CedarErrorKey.GROUP_ALREADY_PRESENT)
    );

    FolderServerGroup newGroup = groupSession.createGroup(groupName.stringValue(), groupDescription.stringValue());
    c.should(newGroup).be(NonNull).otherwiseInternalServerError(
        new CedarErrorPack()
            .message("There was an error while creating the group!")
            .operation(CedarOperations.create(FolderServerGroup.class, "schema:name", groupName))
    );

    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    URI uri = builder.path(CedarUrlUtil.urlEncode(newGroup.getId())).build();
    return Response.created(uri).header(HttpHeaders.ETAG, RevisionPreconditionParser.format(1L))
        .entity(newGroup).build();
  }

  @GET
  @Timed
  @Path("/{id}")
  @Operation(summary = "Get a group",
      description = "Return one group. The ETag it carries is what a later update or delete must "
          + "supply in If-Match.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The group",
          headers = @Header(name = "ETag", description = "Strong validator for the group's current revision.", schema = @Schema(type = "string"))),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "The caller lacks the group read permission"),
      @ApiResponse(responseCode = "404", description = "No such group"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response findGroup(
      @Parameter(description = "Group identifier.", required = true)
      @PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = buildRequestContext();

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_READ);

    GroupServiceSession groupSession = dataServices.getGroupServiceSession(c);

    CedarGroupId gid = CedarGroupId.build(id);
    VersionedResource<FolderServerGroup> snapshot = groupSession.findVersionedGroupById(gid);
    c.should(snapshot).be(NonNull).otherwiseNotFound(
        new CedarErrorPack()
            .message("The group can not be found by id!")
            .operation(CedarOperations.lookup(FolderServerGroup.class, "id", id))
    );

    // BackendCallResult<FolderServerGroup> bcr = groupSession.findGroupById(id);
    // c.must(backendCallResult).be(Successful);
    // c.must(backendCallResult).be(Found);
    // FolderServerGroup group = bcr.get();

    return Response.ok().header(HttpHeaders.ETAG, RevisionPreconditionParser.format(snapshot.revision()))
        .entity(snapshot.resource()).build();
  }

  @PUT
  @Timed
  @Path("/{id}")
  @Operation(summary = "Rename a group",
      description = "Change a group's name or description. Only an administrator of the group may change it. The group-update permission every user holds does not gate this on its own. The built-in special groups "
          + "cannot be changed at all.",
      parameters = @Parameter(in = ParameterIn.HEADER, name = "If-Match", required = true,
          description = "The group's current ETag, as returned by GET. A write without it is refused with "
              + "428, and a stale one with 412, so a change can not silently overwrite one that "
              + "landed since the group was read.", schema = @Schema(type = "string")))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The group as updated",
          headers = @Header(name = "ETag", description = "Strong validator for the group's current revision.", schema = @Schema(type = "string"))),
      @ApiResponse(responseCode = "400", description = "Nothing to change, or the group is a special group"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "The caller does not administer this group"),
      @ApiResponse(responseCode = "404", description = "No such group"),
      @ApiResponse(responseCode = "412", description = "The If-Match value is stale, or the group has since been deleted"),
      @ApiResponse(responseCode = "428", description = "Updating a group requires its current ETag in If-Match"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response updateGroup(
      @Parameter(description = "Group identifier.", required = true)
      @PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = buildRequestContext();

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_UPDATE);

    CedarRequestBody requestBody = c.request().getRequestBody();

    GroupServiceSession groupSession = dataServices.getGroupServiceSession(c);
    CedarGroupId gid = CedarGroupId.build(id);

    String ifMatch = c.getIfMatchHeader();
    if (ifMatch == null || ifMatch.isBlank()) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_REQUIRED)
          .errorMessage("Updating a group requires the ETag returned by GET in If-Match")
          .build();
    }
    RevisionPrecondition precondition = RevisionPreconditionParser.parse(ifMatch);

    FolderServerGroup existingGroup = groupSession.findGroupById(gid);
    if (existingGroup == null) {
      return groupUpdateTargetDeleted();
    }
    requireNonSpecialGroup(existingGroup, gid);

    // Only an administrator of this group may rename it. GROUP_UPDATE is held by every user, so it
    // gates nothing on its own; without this check any user could rename any group. Mirrors the check
    // in updateGroupMembers and deleteGroup.
    boolean isAdministrator = groupSession.userAdministersGroup(gid) || c.getCedarUser().has(UPDATE_NOT_ADMINISTERED_GROUP);
    if (!isAdministrator && groupSession.findGroupById(gid) == null) {
      return groupUpdateTargetDeleted();
    }
    c.should(isAdministrator).be(True).otherwiseForbidden(
        new CedarErrorPack()
            .errorKey(GROUP_CAN_BY_MODIFIED_ONLY_BY_GROUP_ADMIN)
            .message("Only the administrators can update the group!")
            .operation(CedarOperations.update(FolderServerGroup.class, "id", id))
    );

    CedarParameter groupName = requestBody.get("schema:name");
    CedarParameter groupDescription = requestBody.get("schema:description");
    c.should(groupName, groupDescription).be(NonNull).otherwiseBadRequest();

    // check if the new name is unique
    FolderServerGroup otherGroup = groupSession.findGroupByName(groupName.stringValue());
    checkUniqueness(otherGroup, existingGroup);

    Map<NodeProperty, String> updateFields = new HashMap<>();
    updateFields.put(NodeProperty.NAME, groupName.stringValue());
    updateFields.put(NodeProperty.NAME_LOWER, groupName.stringValue().toLowerCase());
    updateFields.put(NodeProperty.DESCRIPTION, groupDescription.stringValue());
    VersionedResource<FolderServerGroup> updatedGroup;
    try {
      updatedGroup = groupSession.updateGroupById(gid, updateFields, precondition);
    } catch (RevisionConflictException e) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
          .parameter("currentETag", RevisionPreconditionParser.format(e.getCurrentRevision()))
          .errorMessage("The group has been updated since it was read")
          .build();
    }

    if (updatedGroup == null) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
          .errorMessage("The group was deleted before the update could be applied")
          .build();
    }

    // BackendCallResult<FolderServerGroup> bcr = groupSession.updateGroup(c, groupSession, id, updateFields);
    // c.must(backendCallResult).be(Successful); // InternalServerError, 404 NotFound, 403 Forbidden if special
    // FolderServerGroup existingGroup = bcr.get();

    return Response.ok().header(HttpHeaders.ETAG, RevisionPreconditionParser.format(updatedGroup.revision()))
        .entity(updatedGroup.resource()).build();
  }

  private static void checkUniqueness(FolderServerGroup otherGroup, FolderServerGroup existingGroup) throws CedarException {
    if (otherGroup != null && !otherGroup.getId().equals(existingGroup.getId())) {
      // 409 and the same error key as createGroup, which rejects the identical condition: the request
      // is well formed and permitted, it collides with existing state. A 400 tells the client to fix a
      // request that has nothing wrong with it, and without the key the collision can only be
      // recognized by reading the message.
      CedarAssertionResult ar = new CedarAssertionResult(
          "There is a group with the new name present in the system. Group names must be unique!")
          .parameter("schema:name", otherGroup.getName())
          .parameter("id", otherGroup.getId())
          .errorKey(CedarErrorKey.GROUP_ALREADY_PRESENT)
          .conflict();
      throw new CedarBackendException(ar);
    }
  }

  private static void requireNonSpecialGroup(FolderServerGroup existingGroup, CedarGroupId id)
      throws CedarException {
    if (existingGroup.getSpecialGroup() != null) {
      CedarAssertionResult ar = new CedarAssertionResult("Special groups can not be modified!")
          .parameter("id", id)
          .parameter("specialGroup", existingGroup.getSpecialGroup())
          .badRequest();
      throw new CedarBackendException(ar);
    }
  }

  private static Response groupUpdateTargetDeleted() {
    return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
        .errorMessage("The group no longer exists, so the conditional update can not be applied")
        .build();
  }

  @DELETE
  @Timed
  @Path("/{id}")
  @Operation(summary = "Delete a group",
      description = "Remove a group. Only an administrator of the group may change it. The group-update permission every user holds does not gate this on its own. The built-in special groups cannot be deleted.",
      parameters = @Parameter(in = ParameterIn.HEADER, name = "If-Match", required = true,
          description = "The group's current ETag, as returned by GET. A write without it is refused with "
              + "428, and a stale one with 412, so a change can not silently overwrite one that "
              + "landed since the group was read.", schema = @Schema(type = "string")))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Deleted"),
      @ApiResponse(responseCode = "400", description = "The group is a special group"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "The caller does not administer this group"),
      @ApiResponse(responseCode = "404", description = "No such group"),
      @ApiResponse(responseCode = "412", description = "The If-Match value is stale, or the group has since been deleted"),
      @ApiResponse(responseCode = "428", description = "Deleting a group requires its current ETag in If-Match"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response deleteGroup(
      @Parameter(description = "Group identifier.", required = true)
      @PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = buildRequestContext();

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_DELETE);

    GroupServiceSession groupSession = dataServices.getGroupServiceSession(c);
    CedarGroupId gid = CedarGroupId.build(id);
    FolderServerGroup existingGroup = groupSession.findGroupById(gid);

    c.should(existingGroup).be(NonNull).otherwiseNotFound(
        new CedarErrorPack()
            .message("The group can not be found by id!")
            .operation(CedarOperations.lookup(FolderServerGroup.class, "id", id))
    );

    String specialGroup = existingGroup.getSpecialGroup();
    c.should(specialGroup).be(Null).otherwiseBadRequest(
        new CedarErrorPack()
            .errorKey(SPECIAL_GROUP_CAN_NOT_BE_DELETED)
            .parameter("schema:name", existingGroup.getName())
            .message("The special group '" + specialGroup + "'can not be deleted!")
            .operation(CedarOperations.delete(FolderServerGroup.class, "id", id))
    );

    boolean isAdministrator = groupSession.userAdministersGroup(gid) || c.getCedarUser().has
        (UPDATE_NOT_ADMINISTERED_GROUP);
    if (!isAdministrator && groupSession.findGroupById(gid) == null) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
          .errorMessage("The group was deleted before this deletion could be applied")
          .build();
    }
    c.should(isAdministrator).be(True).otherwiseForbidden(
        new CedarErrorPack()
            .errorKey(GROUP_CAN_BY_DELETED_ONLY_BY_GROUP_ADMIN)
            .message("Only the administrators can delete the group!")
            .operation(CedarOperations.delete(FolderServerGroup.class, "id", id))
    );


    String ifMatch = c.getIfMatchHeader();
    if (ifMatch == null || ifMatch.isBlank()) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_REQUIRED)
          .errorMessage("Deleting a group requires the ETag returned by GET in If-Match")
          .build();
    }
    boolean deleted;
    try {
      deleted = groupSession.deleteGroupById(gid, RevisionPreconditionParser.parse(ifMatch));
    } catch (RevisionConflictException e) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
          .parameter("currentETag", RevisionPreconditionParser.format(e.getCurrentRevision()))
          .errorMessage("The group has been updated since it was read")
          .build();
    }
    if (!deleted) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
          .errorMessage("The group was deleted before this deletion could be applied")
          .build();
    }

    searchPermissionEnqueueService.groupDeleted(id);

    return Response.noContent().build();
  }

  @GET
  @Timed
  @Path("/{id}/users")
  @Operation(summary = "List a group's members",
      description = "Return the group's members and its administrators. The ETag is over the "
          + "membership, so it is what a membership update must supply, not the group's own.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The group's members and administrators",
          headers = @Header(name = "ETag", description = "\"Strong validator for the membership's current revision.\"",
              schema = @Schema(type = "string"))),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "The caller lacks the group read permission"),
      @ApiResponse(responseCode = "404", description = "No such group"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response getGroupMembers(
      @Parameter(description = "Group identifier.", required = true)
      @PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = buildRequestContext();

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_READ);

    GroupServiceSession groupSession = dataServices.getGroupServiceSession(c);
    CedarGroupId gid = CedarGroupId.build(id);

    FolderServerGroup group = groupSession.findGroupById(gid);
    c.should(group).be(NonNull).otherwiseNotFound(
        new CedarErrorPack()
            .message("The group can not be found by id!")
            .operation(CedarOperations.lookup(FolderServerGroup.class, "id", id))
    );

    VersionedGroupUsers groupUsers = groupSession.findVersionedGroupUsers(gid);
    c.should(groupUsers).be(NonNull).otherwiseInternalServerError(
        new CedarErrorPack()
            .message("There was an error while listing the group users!")
            .operation(CedarOperations.list(FolderServerGroup.class, "id", id))
    );

    return Response.ok().header(HttpHeaders.ETAG, RevisionPreconditionParser.format(groupUsers.revision()))
        .entity(groupUsers.content()).build();
  }

  @PUT
  @Timed
  @Path("/{id}/users")
  @Operation(summary = "Replace a group's members",
      description = "Set who belongs to the group and who administers it. This replaces the "
          + "membership rather than adding to it. Only an administrator of the group may change it. "
          + "The group-update permission every user holds does not gate this on its own. The built-in "
          + "special groups cannot be changed.",
      parameters = @Parameter(in = ParameterIn.HEADER, name = "If-Match", required = true,
          description = "The membership's current ETag, as returned by the members listing. A write "
              + "without it is refused with 428, and a stale one with 412.",
          schema = @Schema(type = "string")))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The membership as updated"),
      @ApiResponse(responseCode = "400", description = "The membership in the body is not well formed, or the group is a special group"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "The caller does not administer this group"),
      @ApiResponse(responseCode = "404", description = "No such group"),
      @ApiResponse(responseCode = "412", description = "The If-Match value is stale, or the group has since been deleted"),
      @ApiResponse(responseCode = "428", description = "Changing a membership requires its current ETag in If-Match"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response updateGroupMembers(
      @Parameter(description = "Group identifier.", required = true)
      @PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = buildRequestContext();

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_UPDATE);

    GroupServiceSession groupSession = dataServices.getGroupServiceSession(c);
    CedarGroupId gid = CedarGroupId.build(id);

    FolderServerGroup group = groupSession.findGroupById(gid);
    c.should(group).be(NonNull).otherwiseNotFound(
        new CedarErrorPack()
            .message("The group can not be found by id!")
            .operation(CedarOperations.lookup(FolderServerGroup.class, "id", id))
    );
    requireNonSpecialGroup(group, gid);

    boolean isAdministrator = groupSession.userAdministersGroup(gid) || c.getCedarUser().has
        (UPDATE_NOT_ADMINISTERED_GROUP);
    c.should(isAdministrator).be(True).otherwiseForbidden(
        new CedarErrorPack()
            .errorKey(GROUP_CAN_BY_MODIFIED_ONLY_BY_GROUP_ADMIN)
            .message("Only the administrators can update the group!")
            .operation(CedarOperations.update(FolderServerGroup.class, "id", id))
    );

    String ifMatch = c.getIfMatchHeader();
    if (ifMatch == null || ifMatch.isBlank()) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_REQUIRED)
          .id(id)
          .errorKey(GROUP_USERS_NOT_UPDATED)
          .errorMessage("Replacing group membership requires the ETag returned by GET in If-Match")
          .build();
    }
    RevisionPrecondition precondition = RevisionPreconditionParser.parse(ifMatch);

    CedarRequestBody requestBody = c.request().getRequestBody();
    CedarGroupUsersRequest usersRequest = requestBody.convert(CedarGroupUsersRequest.class);

    BackendCallResult<VersionedGroupUsers> backendCallResult;
    try {
      backendCallResult = groupSession.updateGroupUsers(gid, usersRequest, precondition);
    } catch (RevisionConflictException e) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
          .id(id)
          .errorKey(GROUP_USERS_NOT_UPDATED)
          .errorMessage("The group membership has been updated since it was read")
          .parameter("currentETag", RevisionPreconditionParser.format(e.getCurrentRevision()))
          .build();
    }
    c.must(backendCallResult).be(Successful);

    //TODO: check if this was a real update in members
    searchPermissionEnqueueService.groupMembersUpdated(id);

    VersionedGroupUsers updatedGroupUsers = backendCallResult.getPayload();
    return Response.ok().header(HttpHeaders.ETAG, RevisionPreconditionParser.format(updatedGroupUsers.revision()))
        .entity(updatedGroupUsers.content()).build();
  }

  @PATCH
  @Timed
  @Path("/{id}")
  @Consumes(CONTENT_TYPE_APPLICATION_MERGE_PATCH_JSON)
  @Operation(summary = "Change part of a group",
      description = "Change a group's name or description with a JSON merge patch, leaving anything "
          + "the patch does not mention alone. Only an administrator of the group may change it. The group-update permission every user holds does not gate this on its own. The built-in special groups cannot be "
          + "changed.",
      parameters = @Parameter(in = ParameterIn.HEADER, name = "If-Match", required = true,
          description = "The group's current ETag, as returned by GET. A write without it is refused with "
              + "428, and a stale one with 412, so a change can not silently overwrite one that "
              + "landed since the group was read.", schema = @Schema(type = "string")))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The group as patched",
          headers = @Header(name = "ETag", description = "Strong validator for the group's current revision.", schema = @Schema(type = "string"))),
      @ApiResponse(responseCode = "400", description = "The patch is not well formed, or the group is a special group"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "The caller does not administer this group"),
      @ApiResponse(responseCode = "404", description = "No such group"),
      @ApiResponse(responseCode = "412", description = "The If-Match value is stale, or the group has since been deleted"),
      @ApiResponse(responseCode = "428", description = "Patching a group requires its current ETag in If-Match"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response patchGroup(
      @Parameter(description = "Group identifier.", required = true)
      @PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = buildRequestContext();

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_UPDATE);

    //c.must(c.request()).be(GenericAssertions.jsonBody);
    c.must(c.request()).be(JsonMergePatch);
    CedarRequestBody requestBody = c.request().getRequestBody();

    GroupServiceSession groupSession = dataServices.getGroupServiceSession(c);
    CedarGroupId gid = CedarGroupId.build(id);

    String ifMatch = c.getIfMatchHeader();
    if (ifMatch == null || ifMatch.isBlank()) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_REQUIRED)
          .errorMessage("Updating a group requires the ETag returned by GET in If-Match")
          .build();
    }
    RevisionPrecondition precondition = RevisionPreconditionParser.parse(ifMatch);

    FolderServerGroup existingGroup = groupSession.findGroupById(gid);
    if (existingGroup == null) {
      return groupUpdateTargetDeleted();
    }
    requireNonSpecialGroup(existingGroup, gid);

    // Only an administrator of this group may change it. As in updateGroup, GROUP_UPDATE alone gates
    // nothing since every user holds it.
    boolean isAdministrator = groupSession.userAdministersGroup(gid) || c.getCedarUser().has(UPDATE_NOT_ADMINISTERED_GROUP);
    if (!isAdministrator && groupSession.findGroupById(gid) == null) {
      return groupUpdateTargetDeleted();
    }
    c.should(isAdministrator).be(True).otherwiseForbidden(
        new CedarErrorPack()
            .errorKey(GROUP_CAN_BY_MODIFIED_ONLY_BY_GROUP_ADMIN)
            .message("Only the administrators can update the group!")
            .operation(CedarOperations.update(FolderServerGroup.class, "id", id))
    );

    CedarParameter groupName = requestBody.get("schema:name");
    CedarParameter groupDescription = requestBody.get("schema:description");

    // Merge-patch reads an explicit null as "remove this property", but a group must always have a
    // name. Rejecting it here is what the name branch below assumes: it reads a present null as a
    // rename and then lowercases the value, which threw for a null name.
    if (groupName.isPresentAndNull()) {
      CedarAssertionResult ar = new CedarAssertionResult("The group name can not be removed!")
          .parameter("schema:name", "null")
          .parameter("id", id)
          .badRequest();
      throw new CedarBackendException(ar);
    }

    // A present null is already refused for the name, so only a supplied value renames. The
    // description may be nulled: clearing it is a legitimate merge-patch.
    boolean updateName = groupName.stringValue() != null
        && theyDiffer(existingGroup.getName(), groupName.stringValue());
    boolean updateDescription = (groupDescription.stringValue() != null || groupDescription.isPresentAndNull())
        && theyDiffer(existingGroup.getDescription(), groupDescription.stringValue());

    if (!updateName && !updateDescription) {
      VersionedResource<FolderServerGroup> snapshot = groupSession.findVersionedGroupById(gid);
      if (snapshot == null) {
        return groupUpdateTargetDeleted();
      }
      if (!precondition.matches(snapshot.revision())) {
        return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
            .parameter("currentETag", RevisionPreconditionParser.format(snapshot.revision()))
            .errorMessage("The group has been updated since it was read")
            .build();
      }
      return Response.ok().header(HttpHeaders.ETAG, RevisionPreconditionParser.format(snapshot.revision()))
          .entity(snapshot.resource()).build();
    }

    // check if the new name is unique
    if (updateName) {
      FolderServerGroup otherGroup = groupSession.findGroupByName(groupName.stringValue());
      checkUniqueness(otherGroup, existingGroup);
    }

    Map<NodeProperty, String> updateFields = new HashMap<>();
    if (updateName) {
      updateFields.put(NodeProperty.NAME, groupName.stringValue());
      updateFields.put(NodeProperty.NAME_LOWER, groupName.stringValue().toLowerCase());
    }
    if (updateDescription) {
      updateFields.put(NodeProperty.DESCRIPTION, groupDescription.stringValue());
    }
    VersionedResource<FolderServerGroup> updatedGroup;
    try {
      updatedGroup = groupSession.updateGroupById(gid, updateFields, precondition);
    } catch (RevisionConflictException e) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
          .parameter("currentETag", RevisionPreconditionParser.format(e.getCurrentRevision()))
          .errorMessage("The group has been updated since it was read")
          .build();
    }

    if (updatedGroup == null) {
      return groupUpdateTargetDeleted();
    }

    return Response.ok().header(HttpHeaders.ETAG, RevisionPreconditionParser.format(updatedGroup.revision()))
        .entity(updatedGroup.resource()).build();
  }

  private static boolean theyDiffer(String v1, String v2) {
    return !Objects.equals(v1, v2);
  }

}
