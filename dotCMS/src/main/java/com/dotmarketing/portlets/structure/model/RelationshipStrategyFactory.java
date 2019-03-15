package com.dotmarketing.portlets.structure.model;

/**
 * @author nollymar
 */
public class RelationshipStrategyFactory {

    private RelationshipConstructionStrategy defaultRelationshipConstructionStrategy = new DefaultRelationshipConstructionStrategy();
    private RelationshipConstructionStrategy manyToOneRelationshipConstructionStrategy = new ManyToOneRelationshipConstructionStrategy();

    public RelationshipConstructionStrategy getRelationshipConstructionStrategy(
            final boolean isManyToOne) {
        return isManyToOne ? manyToOneRelationshipConstructionStrategy
                : defaultRelationshipConstructionStrategy;
    }
}
