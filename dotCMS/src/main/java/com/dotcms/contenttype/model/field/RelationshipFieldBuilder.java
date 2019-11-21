package com.dotcms.contenttype.model.field;

import com.dotmarketing.business.DotStateException;

public interface RelationshipFieldBuilder extends FieldBuilder {

    FieldBuilder skipRelationshipCreation(boolean skipRelationshipCreation);

    static RelationshipFieldBuilder builder(Field field) throws DotStateException {
        return (RelationshipFieldBuilder)FieldBuilder.builder(field);
    }

}
