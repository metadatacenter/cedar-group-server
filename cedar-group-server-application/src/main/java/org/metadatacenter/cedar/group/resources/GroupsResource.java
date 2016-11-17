package org.metadatacenter.cedar.group.resources;

import com.codahale.metrics.annotation.Timed;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.model.folderserver.FolderServerGroup;
import org.metadatacenter.model.response.FolderServerGroupListResponse;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.server.GroupServiceSession;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;
import static org.metadatacenter.server.security.model.auth.CedarPermission.GROUP_READ;

@Path("/groups")
@Produces(MediaType.APPLICATION_JSON)
public class GroupsResource {

  public GroupsResource() {
  }

  @GET
  @Timed
  public Object getGroups(@Context HttpServletRequest request) throws CedarAssertionException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(GROUP_READ);

    GroupServiceSession groupSession = CedarDataServices.getGroupServiceSession(c);
    List<FolderServerGroup> groups = groupSession.findGroups();

    FolderServerGroupListResponse r = new FolderServerGroupListResponse();
    r.setGroups(groups);
    return r;
  }
}