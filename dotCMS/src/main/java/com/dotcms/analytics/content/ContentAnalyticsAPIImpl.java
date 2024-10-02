package com.dotcms.analytics.content;

import com.dotcms.analytics.query.AnalyticsQuery;
import com.dotcms.cube.CubeJSQuery;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Implementation class for the {@link ContentAnalyticsAPIImpl} interface.
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
@ApplicationScoped
public class ContentAnalyticsAPIImpl implements ContentAnalyticsAPI {

    private final ContentAnalyticsFactory contentAnalyticsFactory;

    @Inject
    public ContentAnalyticsAPIImpl(final ContentAnalyticsFactory contentAnalyticsFactory) {
        this.contentAnalyticsFactory = contentAnalyticsFactory;
    }

    @Override
    public ReportResponse runReport(final AnalyticsQuery query, final User user) {
        Logger.debug(this, ()-> "Running the report for the query: " + query);
        // TODO: We should check for specific user permissions
        return this.contentAnalyticsFactory.getReport(query, user);
    }

    @Override
    public ReportResponse runRawReport(final CubeJSQuery cubeJSQuery, final User user) {
        Logger.debug(this, ()-> "Running the report for the raw query: " + cubeJSQuery);
        // TODO: We should check for specific user permissions
        return this.contentAnalyticsFactory.getRawReport(cubeJSQuery, user);
    }

    @Override
    public ReportResponse runRawReport(final String cubeJsQueryJson, final User user) {

        Logger.debug(this, ()-> "Running the report for the raw json query: " + cubeJsQueryJson);
        // note: should check any permissions for an user.
        return this.contentAnalyticsFactory.getRawReport(cubeJsQueryJson, user);
    }
}
