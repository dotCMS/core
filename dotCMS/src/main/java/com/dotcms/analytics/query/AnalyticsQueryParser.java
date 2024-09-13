package com.dotcms.analytics.query;

import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.filters.Filter;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Collection;

public class AnalyticsQueryParser {

    public AnalyticsQuery parseJsonToQuery(final String json) {

        try {

            Logger.debug(this, ()-> "Parsing json to query: " + json);
            final AnalyticsQuery query = DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                    .readValue(json, AnalyticsQuery.class);

            return query;
        } catch (JsonProcessingException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    public CubeJSQuery parseJsonToCubeQuery(final String json) {

        Logger.debug(this, ()-> "Parsing json to cube query: " + json);
        final AnalyticsQuery query = parseJsonToQuery(json);
        return parseQueryToCubeQuery(query);
    }



    public CubeJSQuery parseQueryToCubeQuery(final AnalyticsQuery query) {

        final CubeJSQuery.Builder builder = new CubeJSQuery.Builder();
        Logger.debug(this, ()-> "Parsing query to cube query: " + query);

        if (UtilMethods.isSet(query.getDimensions())) {
            builder.dimensions(query.getDimensions());
        }

        if (UtilMethods.isSet(query.getMeasures())) {
            builder.measures(query.getMeasures());
        }

        if (UtilMethods.isSet(query.getFilters())) {
            builder.filters(parseFilters(query.getFilters()));
        }


        return builder.build();
    }

    private Collection<Filter> parseFilters(final String filters) {
        return null;
    }
}
