package org.metadatacenter.cedar.group;

import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.cedar.group.health.GroupServerHealthCheck;
import org.metadatacenter.cedar.group.resources.GroupsResource;
import org.metadatacenter.cedar.group.resources.IndexResource;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.ServerName;
import org.metadatacenter.server.search.permission.SearchPermissionEnqueueService;

public class GroupServerApplication extends CedarMicroserviceApplication<GroupServerConfiguration> {

  public static void main(String[] args) throws Exception {
    new GroupServerApplication().run(args);
  }

  @Override
  protected ServerName getServerName() {
    return ServerName.GROUP;
  }

  @Override
  protected void initializeWithBootstrap(Bootstrap<GroupServerConfiguration> bootstrap, CedarConfig cedarConfig) {
  }

  @Override
  public void initializeApp() {
    SearchPermissionEnqueueService searchPermissionEnqueueService = new SearchPermissionEnqueueService(cedarConfig);

    GroupsResource.injectSearchPermissionService(searchPermissionEnqueueService);
  }

  @Override
  public void runApp(GroupServerConfiguration configuration, Environment environment) {
    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    final GroupsResource groups = new GroupsResource(cedarConfig);
    environment.jersey().register(groups);

    final GroupServerHealthCheck healthCheck = new GroupServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);
  }
}
