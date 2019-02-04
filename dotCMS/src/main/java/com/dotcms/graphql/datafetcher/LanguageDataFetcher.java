package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.LanguageToMapTransformer;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class LanguageDataFetcher implements DataFetcher<Map<String, Object>> {
    @Override
    public Map<String, Object> get(final DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();

        final LanguageToMapTransformer transformer = new LanguageToMapTransformer(contentlet);
        return (Map<String, Object>) transformer.asMap().get("languageMap");

    }
}