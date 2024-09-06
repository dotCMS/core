package com.dotcms.graphql.datafetcher;

import com.dotcms.util.JsonUtil;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.vavr.control.Try;
import java.util.Collections;
import java.util.Map;

/**
 * DataFetcher to resolve fields of type {@link com.dotcms.contenttype.model.field.JSONField}
 * when requested via GraphQL
 */

public class JSONFieldDataFetcher implements DataFetcher<Map<String, Object>> {
    
    @Override
    public Map<String, Object> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final String var = environment.getField().getName();

            Logger.debug(this, ()-> "Fetching JSON field for contentlet: " + contentlet.getIdentifier() + " field: " + var);
            String jsonAsString = (String) contentlet.get(var);
            return Try.of(()-> JsonUtil.getJsonFromString(jsonAsString))
                    .getOrElse(Collections.emptyMap());
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
