package com.dotcms.graphql.business;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;

import com.dotcms.graphql.datafetcher.CountDataFetcher;
import com.dotcms.graphql.datafetcher.page.PageDataFetcher;
import com.dotmarketing.exception.DotDataException;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;
import java.util.Collection;
import java.util.Collections;

public enum CountFieldProvider implements GraphQLFieldsProvider {

    INSTANCE;

    @Override
    public Collection<GraphQLFieldDefinition> getFields() throws DotDataException {
        return Collections.singleton(newFieldDefinition()
                .name("count")
                .type(list(CountTypeProvider.INSTANCE.getTypes().iterator().next()))
                .dataFetcher(new CountDataFetcher()).build());
    }
}
