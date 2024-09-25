package com.dotcms.analytics.query;

import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.filters.Filter;
import com.dotcms.cube.filters.LogicalFilter;
import com.dotcms.cube.filters.SimpleFilter;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vavr.Tuple2;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parser for the analytics query, it can parse a json string to a {@link AnalyticsQuery} or a {@link CubeJSQuery}
 * @author jsanca
 */
@ApplicationScoped
public class AnalyticsQueryParser {

    /**
     * Parse a json string to a {@link AnalyticsQuery}
     * Example:
     * {
     * 	"dimensions": ["Events.referer", "Events.experiment", "Events.variant", "Events.utcTime", "Events.url", "Events.lookBackWindow", "Events.eventType"],
     * 	"measures": ["Events.count", "Events.uniqueCount"],
     * 	"filters": "Events.variant = ['B'] or Events.experiments = ['B']",
     * 	"limit":100,
     * 	"offset":1,
     * 	"timeDimensions":"Events.day day",
     * 	"orders":"Events.day ASC"
     * }
     * @param json
     * @return AnalyticsQuery
     */
    public AnalyticsQuery parseJsonToQuery(final String json) {

        if (Objects.isNull(json)) {
            throw new IllegalArgumentException("Json can not be null");
        }
        try {

            Logger.debug(this, ()-> "Parsing json to query: " + json);
            return DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                    .readValue(json, AnalyticsQuery.class);
        } catch (JsonProcessingException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Parse a json string to a {@link CubeJSQuery}
     * Example:
     * {
     * 	"dimensions": ["Events.referer", "Events.experiment", "Events.variant", "Events.utcTime", "Events.url", "Events.lookBackWindow", "Events.eventType"],
     * 	"measures": ["Events.count", "Events.uniqueCount"],
     * 	"filters": "Events.variant = ['B'] or Events.experiments = ['B']",
     * 	"limit":100,
     * 	"offset":1,
     * 	"timeDimensions":"Events.day day",
     * 	"orders":"Events.day ASC"
     * }
     * @param json
     * @return CubeJSQuery
     */
    public CubeJSQuery parseJsonToCubeQuery(final String json) {

        Logger.debug(this, ()-> "Parsing json to cube query: " + json);
        final AnalyticsQuery query = parseJsonToQuery(json);
        return parseQueryToCubeQuery(query);
    }

    /**
     * Parse an {@link AnalyticsQuery} to a {@link CubeJSQuery}
     * @param query
     * @return CubeJSQuery
     */
    public CubeJSQuery parseQueryToCubeQuery(final AnalyticsQuery query) {

        if (Objects.isNull(query)) {
            throw new IllegalArgumentException("Query can not be null");
        }

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

        builder.limit(query.getLimit()).offset(query.getOffset());

        if (UtilMethods.isSet(query.getOrders())) {
            builder.orders(parseOrders(query.getOrders()));
        }

        if (UtilMethods.isSet(query.getTimeDimensions())) {
            builder.timeDimensions(parseTimeDimensions(query.getTimeDimensions()));
        }

        return builder.build();
    }

    private Collection<CubeJSQuery.TimeDimension> parseTimeDimensions(final String timeDimensions) {
        final TimeDimensionParser.TimeDimension parsedTimeDimension = TimeDimensionParser.parseTimeDimension(timeDimensions);
        return Stream.of(
                new CubeJSQuery.TimeDimension(parsedTimeDimension.getTerm(),
                        parsedTimeDimension.getField())
        ).collect(Collectors.toList());
    }

    private Collection<CubeJSQuery.OrderItem> parseOrders(final String orders) {

        final OrderParser.ParsedOrder parsedOrder = OrderParser.parseOrder(orders);
        return Stream.of(
                new CubeJSQuery.OrderItem(parsedOrder.getTerm(),
                    "ASC".equalsIgnoreCase(parsedOrder.getOrder())?
                    Filter.Order.ASC:Filter.Order.DESC)
                ).collect(Collectors.toList());
    }

    private Collection<Filter> parseFilters(final String filters) {
        final  Tuple2<List<FilterParser.Token>,List<FilterParser.LogicalOperator>> result =
                FilterParser.parseFilterExpression(filters);

        final List<Filter> filterList = new ArrayList<>();
        final List<SimpleFilter> simpleFilters = new ArrayList<>();

        for (final FilterParser.Token token : result._1) {

            simpleFilters.add(
                    new SimpleFilter(token.member,
                            parseOperator(token.operator),
                            new Object[]{token.values}));
        }

        // if has operators
        if (UtilMethods.isSet(result._2())) {

            FilterParser.LogicalOperator logicalOperator = result._2().get(0); // first one
            LogicalFilter.Builder logicalFilterBuilder = logicalOperator == FilterParser.LogicalOperator.AND?
                    LogicalFilter.Builder.and():LogicalFilter.Builder.or();

            LogicalFilter logicalFilterFirst = logicalFilterBuilder.add(simpleFilters.get(0)).add(simpleFilters.get(1)).build();
            for (int i = 1; i < result._2().size(); i++) { // nest the next ones

                logicalOperator = result._2().get(i);
                logicalFilterBuilder = logicalOperator == FilterParser.LogicalOperator.AND?
                        LogicalFilter.Builder.and():LogicalFilter.Builder.or();

                logicalFilterFirst = logicalFilterBuilder.add(logicalFilterFirst)
                        .add(simpleFilters.get(i + 1)).build();
            }

            filterList.add(logicalFilterFirst);
        } else {
            filterList.addAll(simpleFilters);
        }

        return filterList;
    }

    private SimpleFilter.Operator parseOperator(final String operator) {
        switch (operator) {
            case "=":
                return SimpleFilter.Operator.EQUALS;
            case "!=":
                return SimpleFilter.Operator.NOT_EQUALS;
            case "in":
                return SimpleFilter.Operator.CONTAINS;
            case "!in":
                return SimpleFilter.Operator.NOT_CONTAINS;
            default:
                throw new DotRuntimeException("Operator not supported: " + operator);
        }
    }
}
