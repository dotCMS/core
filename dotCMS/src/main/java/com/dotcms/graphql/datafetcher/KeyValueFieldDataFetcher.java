package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class KeyValueFieldDataFetcher implements DataFetcher<List<Map<String, Object>> > {
    @Override
    public List<Map<String, Object>> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final String var = environment.getField().getName();

            Logger.debug(this, ()-> "Fetching key-value field for contentlet: " + contentlet.getIdentifier() + " field: " + var);
            final List<Map<String, Object>> keyValueMaps = new ArrayList<>();

            contentlet.getKeyValueProperty(var).forEach((key, value) -> {
                final Map<String, Object> keyValueMap = new HashMap<>();
                keyValueMap.put("key", key);
                keyValueMap.put("value", value);
                keyValueMaps.add(keyValueMap);
            });

            return keyValueMaps;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
