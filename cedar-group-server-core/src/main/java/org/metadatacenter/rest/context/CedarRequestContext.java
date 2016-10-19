package org.metadatacenter.rest.context;

import org.metadatacenter.rest.CedarAssertionNoun;
import org.metadatacenter.rest.assertion.assertiontarget.AssertionNounTargetFuture;
import org.metadatacenter.rest.assertion.assertiontarget.AssertionNounTargetPresent;
import org.metadatacenter.rest.assertion.assertiontarget.AssertionPOJOTargetFuture;
import org.metadatacenter.rest.assertion.assertiontarget.AssertionPOJOTargetPresent;
import org.metadatacenter.rest.assertion.noun.CedarRequest;
import org.metadatacenter.rest.assertion.noun.CedarAssertionUser;
import org.metadatacenter.server.security.model.AuthRequest;

public interface CedarRequestContext {

  CedarRequest request();

  AuthRequest getAuthRequest();

  CedarAssertionUser user();

  AssertionNounTargetFuture should(CedarAssertionNoun... nouns);

  AssertionPOJOTargetFuture should(Object... objects);

  AssertionNounTargetPresent must(CedarAssertionNoun... nouns);

  AssertionPOJOTargetPresent must(Object... objects);

  org.metadatacenter.server.security.model.user.CedarUser getCedarUser();
}
