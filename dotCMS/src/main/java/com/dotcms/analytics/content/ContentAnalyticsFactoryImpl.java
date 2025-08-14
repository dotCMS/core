package com.dotcms.analytics.content;

import com.dotcms.analytics.query.AnalyticsQuery;
import com.dotcms.analytics.query.AnalyticsQueryParser;
import com.dotcms.cube.CubeJSClient;
import com.dotcms.cube.CubeJSClientFactory;
import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.CubeJSResultSet;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This class is the implementation of the {@link ContentAnalyticsFactory} interface.
 *
 * @author Jose Castro
 * @since Sep 17th, 2024
 */
@ApplicationScoped
public class ContentAnalyticsFactoryImpl implements ContentAnalyticsFactory {

    private final AnalyticsQueryParser queryParser;
    private final CubeJSClientFactory cubeJSClientFactory;
    private final HostWebAPI hostWebAPI;

    @Inject
    public ContentAnalyticsFactoryImpl(final AnalyticsQueryParser queryParser,
                                       final CubeJSClientFactory cubeJSClientFactory,
                                       final HostWebAPI hostWebAPI) {
        this.queryParser = queryParser;
        this.cubeJSClientFactory = cubeJSClientFactory;
        this.hostWebAPI = hostWebAPI;
    }

    @Override
    public ReportResponse getReport(final AnalyticsQuery query, final User user) {

        Logger.debug(this, ()-> "Getting the report for the query: " + query);
        try {
            final CubeJSQuery cubeJSQuery = this.queryParser.parseQueryToCubeQuery(query);
            return getRawReport(cubeJSQuery, user);
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public ReportResponse getRawReport(final CubeJSQuery cubeJSQuery, final User user) {

        try {

            Logger.debug(this, ()-> "Getting the report for the raw query: " + cubeJSQuery);
            final CubeJSClient cubeClient = cubeJSClientFactory.create(user);
            return toReportResponse(cubeClient.send(cubeJSQuery));
        } catch (DotDataException| DotSecurityException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public ReportResponse getRawReport(final String cubeJsQueryJson, final User user) {

        try {

            Logger.debug(this, ()-> "Getting the report for the raw query: " + cubeJsQueryJson);

            final String siteId = CubeJSQuery.extractSiteId(cubeJsQueryJson)
                    .orElse( hostWebAPI.getCurrentHost().getIdentifier());

            final CubeJSClient cubeClient = cubeJSClientFactory.create(user, siteId);

            return toReportResponse(cubeClient.send(cubeJsQueryJson));
        } catch (DotDataException| DotSecurityException e) {

            Logger.error(this, e.getMessage(), e);
            throw new AnalyticsAppNotConfiguredException(e);
        }
    }

    private ReportResponse toReportResponse(final CubeJSResultSet cubeJSResultSet) {

        return new ReportResponse(StreamSupport.stream(cubeJSResultSet.spliterator(), false).collect(Collectors.toList()));
    }



}
