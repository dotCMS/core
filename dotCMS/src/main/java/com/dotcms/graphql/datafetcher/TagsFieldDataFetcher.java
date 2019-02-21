package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.Arrays;
import java.util.List;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class TagsFieldDataFetcher implements DataFetcher<List<String>> {
    @Override
    public List<String> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final String var = environment.getField().getName();
            String values = (String) contentlet.get(var);

            if (!UtilMethods.isSet(values)) {
                contentlet.setTags();
                values = contentlet.getStringProperty(var);
            }
            return Arrays.asList(values.split("\\s*,\\s*"));
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
