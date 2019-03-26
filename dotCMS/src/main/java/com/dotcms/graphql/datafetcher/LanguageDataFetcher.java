package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.LanguageToMapTransformer;
import com.dotmarketing.util.Logger;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;

public class LanguageDataFetcher implements DataFetcher<Map<String, Object>> {
  @Override
  public Map<String, Object> get(final DataFetchingEnvironment environment) throws Exception {
    try {
      final Contentlet contentlet = environment.getSource();

      final LanguageToMapTransformer transformer = new LanguageToMapTransformer(contentlet);
      return (Map<String, Object>) transformer.asMap().get("languageMap");
    } catch (Exception e) {
      Logger.error(this, e.getMessage(), e);
      throw e;
    }
  }
}
