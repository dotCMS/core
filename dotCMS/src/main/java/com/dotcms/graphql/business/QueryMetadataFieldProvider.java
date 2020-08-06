package com.dotcms.graphql.business;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;

import com.dotcms.graphql.datafetcher.QueryMetadataDataFetcher;
import com.dotmarketing.exception.DotDataException;
import graphql.schema.GraphQLFieldDefinition;
import java.util.Collection;
import java.util.Collections;

/**
 * This FieldsProvider provides a "QueryMetadata" field which includes the totalCount of results
 */

public enum QueryMetadataFieldProvider implements GraphQLFieldsProvider {

    INSTANCE;

    @Override
    public Collection<GraphQLFieldDefinition> getFields() throws DotDataException {
        return Collections.singleton(newFieldDefinition()
                .name("QueryMetadata")
                .type(list(QueryMetadataTypeProvider.INSTANCE.getTypes().iterator().next()))
                .dataFetcher(new QueryMetadataDataFetcher()).build());
    }
}
