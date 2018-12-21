package com.dotcms.graphql.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;

import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

public interface GraphqlAPI {
    GraphQLSchema getSchema();
    GraphQLType createSchemaType(final ContentType contentType);
    void updateSchemaType(final ContentType contentType);
    void deleteSchemaType(final String contentTypeVar);
    void createSchemaTypeField(final ContentType contentType, final Field field);
    void updateSchemaTypeField(final ContentType contentType, final Field field);
    void deleteSchemaTypeField(final ContentType contentType, final String fieldVar);
}
