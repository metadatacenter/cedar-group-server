package org.metadatacenter.cedar.group;

import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.group.health.GroupServerHealthCheck;
import org.metadatacenter.cedar.group.resources.GroupsResource;
import org.metadatacenter.cedar.group.resources.IndexResource;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.server.cache.util.CacheService;
import org.metadatacenter.server.search.permission.SearchPermissionEnqueueService;

public class GroupServerApplication extends CedarMicroserviceApplication<GroupServerConfiguration> {

  private static SearchPermissionEnqueueService searchPermissionEnqueueService;

  public static void main(String[] args) throws Exception {
    new GroupServerApplication().run(args);
  }

  @Override
  public String getName() {
    return "cedar-group-server";
  }

  @Override
  public void initializeApp(Bootstrap<GroupServerConfiguration> bootstrap) {
    CedarDataServices.initializeFolderServices(cedarConfig);
    
    searchPermissionEnqueueService = new SearchPermissionEnqueueService(
        new CacheService(cedarConfig.getCacheConfig().getPersistent()));

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
