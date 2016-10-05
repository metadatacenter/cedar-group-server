package org.metadatacenter.rest.bridge;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.rest.ICedarRequestContext;
import org.metadatacenter.server.neo4j.Neo4JProxy;
import org.metadatacenter.server.neo4j.Neo4JUserSession;
import org.metadatacenter.server.neo4j.Neo4jConfig;
import org.metadatacenter.server.service.UserService;
import org.metadatacenter.server.service.mongodb.UserServiceMongoDB;

public final class CedarDataServices {

  private static CedarConfig cedarConfig;
  private static UserService userService;
  private static Neo4JProxy neo4JProxy;

  private CedarDataServices() {
    cedarConfig = CedarConfig.getInstance();

    userService = new UserServiceMongoDB(cedarConfig.getMongoConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.USER));

    neo4JProxy = new Neo4JProxy(Neo4jConfig.fromCedarConfig(cedarConfig),
        cedarConfig.getLinkedDataConfig().getBase(),
        cedarConfig.getLinkedDataConfig().getUsersBase());
  }

  public static Neo4JUserSession getNeo4jSession(ICedarRequestContext context) {
    return Neo4JUserSession.get(neo4JProxy, userService, context.getCedarUser(), true);
  }
}
