package com.dotcms.analytics.viewtool;

import com.dotcms.analytics.content.ContentAnalyticsAPI;
import com.dotcms.analytics.content.ContentAnalyticsQuery;
import com.dotcms.analytics.content.ReportResponse;
import com.dotcms.analytics.query.AnalyticsQuery;
import com.dotcms.analytics.query.AnalyticsQueryParser;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.filters.Filter;
import com.dotcms.cube.filters.SimpleFilter;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.dotcms.analytics.content.ContentAnalyticsQuery.DIMENSIONS_ATTR;
import static com.dotcms.analytics.content.ContentAnalyticsQuery.MEASURES_ATTR;
import static com.dotcms.analytics.content.ContentAnalyticsQuery.TIME_DIMENSIONS_ATTR;
import static com.dotcms.util.DotPreconditions.checkArgument;

/**
 * This class is a ViewTool that can be used to access the analytics data.
 * @author jsanca
 */
public class AnalyticsTool  implements ViewTool {

    private final ContentAnalyticsAPI contentAnalyticsAPI;
    private final AnalyticsQueryParser analyticsQueryParser;
    private final UserWebAPI userWebAPI;

    private User user = null;

    public AnalyticsTool() {
        this(getContentAnalyticsAPI(),
                getAnalyticsQueryParser(),
                WebAPILocator.getUserWebAPI());
    }

    private static ContentAnalyticsAPI getContentAnalyticsAPI() {
        return CDIUtils.getBeanThrows(ContentAnalyticsAPI.class);
    }

    private static AnalyticsQueryParser getAnalyticsQueryParser() {
        return CDIUtils.getBeanThrows(AnalyticsQueryParser.class);
    }

    public AnalyticsTool(final ContentAnalyticsAPI contentAnalyticsAPI,
                         final AnalyticsQueryParser analyticsQueryParser,
                         final UserWebAPI userWebAPI) {

        this.contentAnalyticsAPI  = contentAnalyticsAPI;
        this.analyticsQueryParser = analyticsQueryParser;
        this.userWebAPI = userWebAPI;
    }

    @Override
    public void init(final Object initData) {

        if (initData instanceof ViewContext) {

            final HttpServletRequest request = ((ViewContext) initData).getRequest();
            final HttpSession session = request.getSession(false);

            if (session != null) {
                try {
                    user = userWebAPI.getLoggedInUser(request);
                } catch (DotRuntimeException e) {
                    Logger.error(this.getClass(), e.getMessage());
                }
            }
        }
    }

    /**
     * Runs an analytics report based on the string json query.
     * example:
     * <code>
     * #set($query = "{
     * 	"dimensions": ["Events.referer", "Events.experiment", "Events.variant", "Events.utcTime", "Events.url", "Events.lookBackWindow", "Events.eventType"],
     * 	"measures": ["Events.count", "Events.uniqueCount"],
     * 	"filters": "Events.variant = ['B'] or Events.experiments = ['B']",
     * 	"limit":100,
     * 	"offset":1,
     * 	"timeDimensions":"Events.day day",
     * 	"order":"Events.day ASC"
     * }")
     *
     * $analytics.runReportFromJson($query)
     * </code>
     * @param query
     * @return
     */
    public ReportResponse runReportFromJson(final String query) {

        Logger.debug(this, () -> "Running report from json: " + query);
        return contentAnalyticsAPI.runReport(this.analyticsQueryParser.parseJsonToQuery(query), user);
    }

    /**
     * Runs an analytics report based on a set of parameters
     * example:
     * <code>
     * $analytics.runReport('Events.count Events.uniqueCount',
     * 'Events.referer Events.experiment', 'Events.day day',
     * 'Events.variant = [B] : Events.experiments = [B]', 'Events.day ASC', 100, 0)
     * </code>
     * @param measures String
     *                 example: 'Events.count Events.uniqueCount'
     * @param dimensions String
     *                   example: 'Events.referer Events.experiment'
     * @param timeDimensions String
     *                       example: 'Events.day day'
     * @param filters String
     *                example: 'Events.variant = [B] : Events.experiments = [B]'
     * @param order String
     *              example: 'Events.day ASC'
     * @param limit int
     *              example: 100
     * @param offset int
     *               example: 0
     * @return ReportResponse
     */
    public ReportResponse runReport(final String measures, final String dimensions,
                                    final String timeDimensions, final String filters, final String order,
                                    final int limit, final int offset) {

        checkArgument(!(UtilMethods.isNotSet(measures)
                        && UtilMethods.isNotSet(dimensions)
                        && UtilMethods.isNotSet(timeDimensions)),
                IllegalArgumentException.class,
                "Query should contain either measures, dimensions or timeDimensions with granularities in order to be valid");

        final ContentAnalyticsQuery.Builder builder = new ContentAnalyticsQuery.Builder();

        if (Objects.nonNull(dimensions)) {
            builder.dimensions(dimensions);
        }

        if (Objects.nonNull(measures)) {
            builder.measures(measures);
        }

        if (Objects.nonNull(filters)) {
            builder.filters(filters);
        }

        if (Objects.nonNull(order)) {
            builder.order(order);
        }

        if (Objects.nonNull(timeDimensions)) {
            builder.timeDimensions(timeDimensions);
        }

        if (limit > 0) {
            builder.limit(limit);
        }
        if (offset >= 0) {
            builder.offset(offset);
        }

        final ContentAnalyticsQuery contentAnalyticsQuery = builder.build();

        Logger.debug(this, () -> "Running report from query: " + contentAnalyticsQuery.toString());

        final Map<String, Object> queryMap = new HashMap<>();
        if (UtilMethods.isSet(contentAnalyticsQuery.measures())) {
            queryMap.put("measures", contentAnalyticsQuery.measures());
        }
        if (UtilMethods.isSet(contentAnalyticsQuery.dimensions())) {
            queryMap.put("dimensions", contentAnalyticsQuery.dimensions());
        }
        if (UtilMethods.isSet(contentAnalyticsQuery.timeDimensions())) {
            queryMap.put("timeDimensions", contentAnalyticsQuery.timeDimensions());
        }
        if (UtilMethods.isSet(contentAnalyticsQuery.filters())) {
            queryMap.put("filters", contentAnalyticsQuery.filters());
        }
        if (UtilMethods.isSet(contentAnalyticsQuery.order())) {
            queryMap.put("order", contentAnalyticsQuery.order());
        }
        queryMap.put("limit", contentAnalyticsQuery.limit());
        queryMap.put("offset", contentAnalyticsQuery.offset());

        final String cubeJsQuery = JsonUtil.getJsonStringFromObject(queryMap);

        final ReportResponse reportResponse = this.contentAnalyticsAPI.runRawReport(cubeJsQuery,
                user);

        return reportResponse;
    }

    /**
     * Runs an analytics report based on Map query.
     * example:
     * <code>
     * #set ($myQuery = {})
     * $myMap.put('dimensions', ["Events.referer", "Events.experiment", "Events.variant", "Events.utcTime", "Events.url", "Events.lookBackWindow", "Events.eventType"])
     * $myMap.put('measures', ["Events.count", "Events.uniqueCount"])
     * $myMap.put('filters', "Events.variant = ['B'] or Events.experiments = ['B']")
     * $myMap.put('limit', 100)
     * $myMap.put('offset', 1)
     * $myMap.put('timeDimensions', "Events.day day")
     * $myMap.put('order', "Events.day ASC")
     *
     * $analytics.runReportFromMap($myQuery)
     * </code>
     * @param query
     * @return
     */
    public ReportResponse runReportFromMap(final Map<String, Object> query) {

        if (Objects.isNull(query)) {
            throw new IllegalArgumentException("Query can not be null");
        }
        
        Logger.debug(this, () -> "Running report from map: " + query);
        final AnalyticsQuery analyticsQuery = DotObjectMapperProvider.getInstance()
                .getDefaultObjectMapper().convertValue(query, AnalyticsQuery.class);
        return contentAnalyticsAPI.runReport(analyticsQuery, user);
    }

    /**
     * Creates a CubeJSQuery.Builder instance
     * @return
     */
    public CubeJSQuery.Builder createCubeJSQueryBuilder() {
        return new CubeJSQuery.Builder();
    }

    /**
     * Create a {@link SimpleFilter.Operator} from its name
     *
     * @param operatorName
     * @return
     */
    public SimpleFilter.Operator operator(String operatorName) {
        return SimpleFilter.Operator.valueOf(operatorName);
    }

    /**
     * Create a {@link Filter.Order} from its name
     * @param orderName
     * @return
     */
    public Filter.Order order(String orderName) {
        return Filter.Order.valueOf(orderName);
    }

    /**
     * Runs an analytics report based cube js raw json string query
     *
     * example:
     * <code>
     * #set($query = $analytics.createCubeJSQueryBuilder())
     * $query.dimensions("Events.experiment", "Events.variant")
     * $analytics.runRawReport($query.build())
     * </code>
     * @param query
     * @return
     */
    public ReportResponse runRawReport(final CubeJSQuery query) {

        if (Objects.isNull(query)) {
            throw new IllegalArgumentException("Query can not be null");
        }

        Logger.debug(this, () -> "Running report from raw query: " + query);
        return contentAnalyticsAPI.runRawReport(query, user);
    }
}
