package org.metadatacenter.rest.context;

import org.metadatacenter.rest.ICedarAssertionNoun;
import org.metadatacenter.rest.assertion.assertiontarget.IAssertionNounTargetFuture;
import org.metadatacenter.rest.assertion.assertiontarget.IAssertionNounTargetPresent;
import org.metadatacenter.rest.assertion.assertiontarget.IAssertionPOJOTargetFuture;
import org.metadatacenter.rest.assertion.assertiontarget.IAssertionPOJOTargetPresent;
import org.metadatacenter.rest.assertion.noun.ICedarRequest;
import org.metadatacenter.rest.assertion.noun.ICedarUser;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.user.CedarUser;

public interface ICedarRequestContext {

  ICedarRequest request();

  IAuthRequest getAuthRequest();

  ICedarUser user();

  IAssertionNounTargetFuture should(ICedarAssertionNoun... nouns);

  IAssertionPOJOTargetFuture should(Object... objects);

  IAssertionNounTargetPresent must(ICedarAssertionNoun... nouns);

  IAssertionPOJOTargetPresent must(Object... objects);

  CedarUser getCedarUser();
}
