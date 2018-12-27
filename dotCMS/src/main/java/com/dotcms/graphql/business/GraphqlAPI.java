package com.dotcms.graphql.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;

import graphql.schema.GraphQLSchema;

public interface GraphqlAPI {
    GraphQLSchema getSchema() throws DotDataException;
    void updateSchemaType(final ContentType contentType);
    void deleteSchemaType(final String contentTypeVar);
    void createSchemaTypeField(final ContentType contentType, final Field field);
    void updateSchemaTypeField(final ContentType contentType, final Field field);
    void deleteSchemaTypeField(final ContentType contentType, final String fieldVar);
}
