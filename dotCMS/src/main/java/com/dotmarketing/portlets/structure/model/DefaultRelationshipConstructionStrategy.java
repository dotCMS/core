package com.dotmarketing.portlets.structure.model;

/**
 * Strategy to create any relationship instance (except for Many to One relationships)
 *
 * @author nollymar
 */
public class DefaultRelationshipConstructionStrategy implements RelationshipConstructionStrategy {

  @Override
  public void apply(final Relationship relationship) {
    // do nothing
  }
}
