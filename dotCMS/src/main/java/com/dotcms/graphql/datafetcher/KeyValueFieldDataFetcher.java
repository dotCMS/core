package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyValueFieldDataFetcher implements DataFetcher<List<Map<String, String>>> {
  @Override
  public List<Map<String, String>> get(final DataFetchingEnvironment environment) throws Exception {
    try {
      final Contentlet contentlet = environment.getSource();
      final String var = environment.getField().getName();
      final List<Map<String, String>> keyValueMaps = new ArrayList<>();

      contentlet
          .getKeyValueProperty(var)
          .forEach(
              (key, value) -> {
                final Map<String, String> keyValueMap = new HashMap<>();
                keyValueMap.put("key", key);
                keyValueMap.put("value", (String) value);
                keyValueMaps.add(keyValueMap);
              });

      return keyValueMaps;
    } catch (Exception e) {
      Logger.error(this, e.getMessage(), e);
      throw e;
    }
  }
}
