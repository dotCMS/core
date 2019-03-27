package com.dotmarketing.portlets.structure.model;

import static com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY;

import com.dotcms.contenttype.model.type.ContentType;


/**
 * Strategy to create an instance of a Many to One relationship
 * @author nollymar
 */
public class ManyToOneRelationshipConstructionStrategy implements RelationshipConstructionStrategy{

    @Override
    public void apply(final Relationship relationship) {
        final String childContentTypeId = relationship.getParentStructureInode();
        final String childRelationName = relationship.getParentRelationName();
        final boolean childRequired = relationship.isParentRequired();

        relationship.setParentStructureInode(relationship.getChildStructureInode());
        relationship.setChildStructureInode(childContentTypeId);
        relationship.setParentRelationName(relationship.getChildRelationName());
        relationship.setChildRelationName(childRelationName);
        relationship.setCardinality(ONE_TO_MANY.ordinal());
        relationship.setParentRequired(relationship.isChildRequired());
        relationship.setChildRequired(childRequired);
    }
}
