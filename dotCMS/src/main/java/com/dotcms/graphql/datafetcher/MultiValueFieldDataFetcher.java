package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MultiValueFieldDataFetcher implements DataFetcher<List<String>> {
    @Override
    public List<String> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final String var = environment.getField().getName();
            final String values = (String) contentlet.get(var);
            Logger.debug(this, ()-> "Fetching multi-value field for contentlet: " + contentlet.getIdentifier() + " field: " + var);
            return UtilMethods.isSet(values) ? Arrays.asList(values.split("\\s*,\\s*")) : Collections.emptyList();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
