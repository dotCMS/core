package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class KeyValueFieldDataFetcher implements DataFetcher<List<Map<String, String>> > {
    @Override
    public List<Map<String, String>> get(final DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();
        final String var = environment.getField().getName();
        final List<Map<String, String>> keyValueMaps = new ArrayList<>();

        contentlet.getKeyValueProperty(var).forEach((key, value) -> {
            final Map<String, String> keyValueMap = new HashMap<>();
            keyValueMap.put("key", key);
            keyValueMap.put("value", (String) value);
            keyValueMaps.add(keyValueMap);
        });

        return keyValueMaps;
    }
}
