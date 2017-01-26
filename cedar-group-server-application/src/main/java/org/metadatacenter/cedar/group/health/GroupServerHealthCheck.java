package org.metadatacenter.cedar.group.health;

import com.codahale.metrics.health.HealthCheck;

public class GroupServerHealthCheck extends HealthCheck {

  public GroupServerHealthCheck() {
  }

  @Override
  protected Result check() throws Exception {
    if (2 * 2 == 5) {
      return Result.unhealthy("Unhealthy, because 2 * 2 == 5");
    }
    return Result.healthy();
  }
}