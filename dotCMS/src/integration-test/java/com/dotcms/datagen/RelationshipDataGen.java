package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.structure.model.Relationship;

/**
 * @author Jonathan Gamba 2019-06-07
 */
public class RelationshipDataGen extends AbstractDataGen<Relationship> {

    private ContentType parentContentType;
    private ContentType childContentType;

    public RelationshipDataGen() {

    }

    public RelationshipDataGen(boolean autoGenerateContentTypes) {

        if (autoGenerateContentTypes) {
            parentContentType = new ContentTypeDataGen().nextPersisted();
            childContentType = new ContentTypeDataGen().nextPersisted();
        }
    }

    @SuppressWarnings("unused")
    public RelationshipDataGen parentContentType(ContentType parentContentType) {
        this.parentContentType = parentContentType;
        return this;
    }

    @SuppressWarnings("unused")
    public RelationshipDataGen childContentType(ContentType childContentType) {
        this.childContentType = childContentType;
        return this;
    }

    @Override
    public Relationship next() {

        Relationship relationship = new Relationship();
        if ((parentContentType == childContentType) || (parentContentType.id()
                .equals(childContentType.id()))) {
            relationship.setParentRelationName("Child " + parentContentType.name());
            relationship.setChildRelationName("Parent " + childContentType.name());
        } else {
            relationship.setParentRelationName(parentContentType.name());
            relationship.setChildRelationName(childContentType.name());
        }

        final String relationTypeValue = parentContentType.name() + "-" + childContentType.name();
        relationship.setRelationTypeValue(relationTypeValue);
        relationship.setParentStructureInode(parentContentType.inode());
        relationship.setChildStructureInode(childContentType.id());

        return relationship;
    }

    @WrapInTransaction
    @Override
    public Relationship persist(final Relationship relationship) {

        try {

            final Relationship foundRelationship = APILocator.getRelationshipAPI()
                    .byTypeValue(relationship.getRelationTypeValue());
            if (null != foundRelationship) {
                return foundRelationship;
            } else {

                try {
                    APILocator.getRelationshipAPI().create(relationship);
                } catch (Exception e) {
                    throw new DotRuntimeException(e);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist relationship.", e);
        }

        return relationship;
    }

    /**
     * Creates a new {@link Relationship} instance and persists it in DB
     *
     * @return A new Relationship instance persisted in DB
     */
    @Override
    public Relationship nextPersisted() {
        return persist(next());
    }

}