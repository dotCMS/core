package com.dotcms.datagen;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

import static com.dotcms.util.CollectionsUtils.list;

public class FieldRelationshipDataGen extends AbstractDataGen<Relationship>  {

    private ContentType parent;
    private ContentType child;
    private WebKeys.Relationship.RELATIONSHIP_CARDINALITY cardinality = WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY;
    private boolean parentRequired = false;

    public FieldRelationshipDataGen parent(ContentType parent) {
        this.parent = parent;
        return this;
    }

    public FieldRelationshipDataGen child(ContentType child) {
        this.child = child;
        return this;
    }

    public FieldRelationshipDataGen cardinality(WebKeys.Relationship.RELATIONSHIP_CARDINALITY cardinality) {
        this.cardinality = cardinality;
        return this;
    }

    public FieldRelationshipDataGen parentRequired(boolean parentRequired) {
        this.parentRequired = parentRequired;
        return this;
    }

    @Override
    public Relationship next() {
        final Relationship relationship = new Relationship();
        relationship.setCardinality(cardinality.ordinal());
        relationship.setChildRelationName(child.variable());
        relationship.setParentRelationName(parent.variable());
        relationship.setParentRequired(parentRequired);

        return relationship;
    }

    @Override
    public Relationship persist(Relationship object) {
        final Field field = FieldBuilder.builder(RelationshipField.class)
                .name(parent.variable())
                .contentTypeId(parent.id())
                .values(String.valueOf(cardinality.ordinal()))
                .relationType(child.variable()).required(false).build();

        final User systemUser = APILocator.systemUser();
        try {
            final Field fieldSaved = APILocator.getContentTypeFieldAPI().save(field, systemUser);
            return APILocator.getRelationshipAPI().getRelationshipFromField(fieldSaved, systemUser);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
