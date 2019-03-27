package com.dotmarketing.portlets.structure.model;

import static com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY;

import com.dotcms.contenttype.model.type.ContentType;


/**
 * Strategy to create any relationship instance (except for Many to One relationships)
 * @author nollymar
 */
public class DefaultRelationshipConstructionStrategy implements RelationshipConstructionStrategy{

    @Override
    public void apply(final Relationship relationship) {
        //do nothing
    }
}
