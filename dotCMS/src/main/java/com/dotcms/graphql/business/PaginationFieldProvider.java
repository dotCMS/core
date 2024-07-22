package com.dotcms.graphql.business;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;

import com.dotcms.graphql.datafetcher.PaginationsDataFetcher;
import com.dotmarketing.exception.DotDataException;
import graphql.schema.GraphQLFieldDefinition;
import java.util.Collection;
import java.util.Set;

/**
 * This FieldsProvider provides a "Pagination" field which includes the pagination for each
 * requested field of the GraphQL Content Delivery API
 */
public enum PaginationFieldProvider implements GraphQLFieldsProvider {

    INSTANCE;

    @Override
    public Collection<GraphQLFieldDefinition> getFields() throws DotDataException {
        return Set.of(newFieldDefinition()
                .name("Pagination")
                .type(list(PaginationTypeProvider.INSTANCE.getTypes().iterator().next()))
                .dataFetcher(new PaginationsDataFetcher()).build());
    }
}
