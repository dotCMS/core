package com.dotcms.analytics.content;

import com.dotcms.analytics.query.AnalyticsQuery;
import com.dotcms.cube.CubeJSQuery;
import com.liferay.portal.model.User;

/**
 * This interface provides the methods to run reports on content analytics.
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public interface ContentAnalyticsAPI {

    /**
     * Run a report based on an analytics query
     * @param query
     * @param user
     * @return ReportResponse
     */
    ReportResponse runReport(final AnalyticsQuery query, final User user);

    /**
     * Runs a raw report based on a cubeJS query
     * @param cubeJSQuery
     * @param user
     * @return ReportResponse
     */
    ReportResponse runRawReport(CubeJSQuery cubeJSQuery, User user);
}
