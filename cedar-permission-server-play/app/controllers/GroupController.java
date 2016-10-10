package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.model.folderserver.CedarFSGroup;
import org.metadatacenter.model.response.FSGroupListResponse;
import org.metadatacenter.rest.assertion.noun.ICedarParameter;
import org.metadatacenter.rest.assertion.noun.ICedarRequestBody;
import org.metadatacenter.rest.assertion.GenericAssertions;
import org.metadatacenter.rest.bridge.CedarDataServices;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.rest.context.ICedarRequestContext;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.rest.operation.CedarOperations;
import org.metadatacenter.server.neo4j.Neo4JUserSession;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.util.json.JsonMapper;
import play.mvc.Result;

import java.util.List;

public class GroupController extends AbstractPermissionServerController {


  public static Result findGroups() throws CedarAssertionException {
    ICedarRequestContext c = CedarRequestContextFactory.fromRequest(request());
    c.must(c.user()).be(GenericAssertions.loggedIn);

    Neo4JUserSession neoSession = CedarDataServices.getNeo4jSession(c);
    List<CedarFSGroup> groups = neoSession.findGroups();

    FSGroupListResponse r = new FSGroupListResponse();
    r.setGroups(groups);
    JsonNode resp = JsonMapper.MAPPER.valueToTree(r);
    return ok(resp);
  }

  public static Result createGroup() throws CedarAssertionException {
    ICedarRequestContext c = CedarRequestContextFactory.fromRequest(request());

    c.must(c.user()).be(GenericAssertions.loggedIn);
    c.must(c.user()).have(CedarPermission.GROUP_CREATE);

    c.must(c.request()).be(GenericAssertions.jsonBody);
    ICedarRequestBody requestBody = c.request().jsonBody();

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

    JsonNode createdGroup = JsonMapper.MAPPER.valueToTree(newGroup);
    String absoluteUrl = routes.GroupController.findGroup(newGroup.getId()).absoluteURL(request());
    response().setHeader(HttpConstants.HTTP_HEADER_LOCATION, absoluteUrl);
    return created(createdGroup);
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