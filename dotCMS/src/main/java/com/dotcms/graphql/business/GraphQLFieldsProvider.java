package com.dotcms.graphql.business;

import com.dotmarketing.exception.DotDataException;
import graphql.schema.GraphQLFieldDefinition;
import java.util.Collection;

public interface GraphQLFieldsProvider {
    Collection<GraphQLFieldDefinition> getFields() throws DotDataException;
}
