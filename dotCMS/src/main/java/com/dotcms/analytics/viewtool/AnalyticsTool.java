package com.dotcms.analytics.viewtool;

import com.dotcms.analytics.content.ContentAnalyticsAPI;
import com.dotcms.analytics.content.ReportResponse;
import com.dotcms.analytics.query.AnalyticsQuery;
import com.dotcms.analytics.query.AnalyticsQueryParser;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.cube.CubeJSQuery;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
        final Optional<ContentAnalyticsAPI> contentAnalyticsAPI = CDIUtils.getBean(ContentAnalyticsAPI.class);
        if (!contentAnalyticsAPI.isPresent()) {
            throw new DotRuntimeException("Could not instance ContentAnalyticsAPI");
        }
        return contentAnalyticsAPI.get();
    }

    private static AnalyticsQueryParser getAnalyticsQueryParser() {
        final Optional<AnalyticsQueryParser> queryParserOptional = CDIUtils.getBean(AnalyticsQueryParser.class);
        if (!queryParserOptional.isPresent()) {
            throw new DotRuntimeException("Could not instance AnalyticsQueryParser");
        }
        return queryParserOptional.get();
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
     * 	"orders":"Events.day ASC"
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
     * $myMap.put('orders', "Events.day ASC")
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
