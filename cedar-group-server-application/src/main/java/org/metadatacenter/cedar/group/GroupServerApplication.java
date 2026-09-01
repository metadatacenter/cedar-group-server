package org.metadatacenter.cedar.group;

import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import org.metadatacenter.cedar.group.resources.GroupsResource;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceIndexResource;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.ServerName;
import org.metadatacenter.server.search.permission.SearchPermissionEnqueueService;

public class GroupServerApplication extends CedarMicroserviceApplication<GroupServerConfiguration> {

  private SearchPermissionEnqueueService searchPermissionEnqueueService;

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
    searchPermissionEnqueueService = new SearchPermissionEnqueueService(cedarConfig);

    GroupsResource.injectSearchPermissionService(searchPermissionEnqueueService);
  }

  @Override
  public void runApp(GroupServerConfiguration configuration, Environment environment) {
    environment.lifecycle().manage(new Managed() {
      @Override
      public void start() {
        searchPermissionEnqueueService.start();
      }

      @Override
      public void stop() {
        searchPermissionEnqueueService.close();
      }
    });

    final CedarMicroserviceIndexResource index =
        new CedarMicroserviceIndexResource(cedarConfig, getServerName());
    environment.jersey().register(index);

    final GroupsResource groups = new GroupsResource(cedarConfig);
    environment.jersey().register(groups);

  }
}
