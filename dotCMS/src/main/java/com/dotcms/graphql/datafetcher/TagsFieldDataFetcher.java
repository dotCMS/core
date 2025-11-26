package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class TagsFieldDataFetcher implements DataFetcher<List<String>> {
    
    private final static List<String> EMPTY_LIST=ImmutableList.of();
    
    @Override
    public List<String> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final String var = environment.getField().getName();

            Logger.debug(this, ()-> "Fetching tags field for contentlet: " + contentlet.getIdentifier() + " field: " + var);
            String values = (String) contentlet.get(var);

            if (!UtilMethods.isSet(values)) {
                contentlet.setTags();
                values = contentlet.getStringProperty(var);
            }
            return UtilMethods.isSet(values) ? Arrays.asList(values.split("\\s*,\\s*")) : EMPTY_LIST;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
