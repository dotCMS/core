package com.dotcms.graphql.datafetcher;

import com.dotmarketing.util.Logger;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;

public class MapFieldPropertiesDataFetcher implements DataFetcher<Object> {
  @Override
  public Object get(final DataFetchingEnvironment environment) throws Exception {
    try {
      final Map<String, Object> map = environment.getSource();
      final String var = environment.getField().getName();
      return map.get(var);
    } catch (Exception e) {
      Logger.error(this, e.getMessage(), e);
      throw e;
    }
  }
}
