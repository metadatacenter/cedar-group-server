package org.metadatacenter.cedar.group;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.cedar.group.health.GroupServerHealthCheck;
import org.metadatacenter.cedar.group.resources.GroupsResource;
import org.metadatacenter.cedar.group.resources.IndexResource;
import org.metadatacenter.cedar.util.dw.CedarDropwizardApplicationUtil;

public class GroupServerApplication extends Application<GroupServerConfiguration> {
  public static void main(String[] args) throws Exception {
    new GroupServerApplication().run(args);
  }

  @Override
  public String getName() {
    return "group-server";
  }

  @Override
  public void initialize(Bootstrap<GroupServerConfiguration> bootstrap) {
    CedarDropwizardApplicationUtil.setupKeycloak();
  }

  @Override
  public void run(GroupServerConfiguration configuration, Environment environment) {
    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    final GroupsResource groups = new GroupsResource();
    environment.jersey().register(groups);

    final GroupServerHealthCheck healthCheck = new GroupServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);

    CedarDropwizardApplicationUtil.setupEnvironment(environment);

  }
}
