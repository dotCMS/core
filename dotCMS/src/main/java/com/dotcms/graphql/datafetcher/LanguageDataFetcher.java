package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.LanguageToMapTransformer;
import com.dotmarketing.portlets.contentlet.transform.strategy.LanguageViewStrategy;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.ViewAsPageStatus;
import com.dotmarketing.util.Logger;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class LanguageDataFetcher implements DataFetcher<Map<String, Object>> {
    @Override
    public Map<String, Object> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            Contentlet contentlet;

            if(environment.getSource() instanceof Contentlet) {
                contentlet = environment.getSource();
                final LanguageToMapTransformer transformer = new LanguageToMapTransformer(contentlet);
                return (Map<String, Object>) transformer.asMap().get("languageMap");
            } else  {
                ViewAsPageStatus viewAsPageStatus = environment.getSource();
                return (Map<String, Object>) LanguageViewStrategy.mapLanguage(viewAsPageStatus.getLanguage(),
                        true).get("languageMap");
            }
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }

    }
}