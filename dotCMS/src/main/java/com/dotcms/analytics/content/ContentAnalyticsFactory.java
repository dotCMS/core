package com.dotcms.analytics.content;

import com.dotcms.analytics.query.AnalyticsQuery;
import com.dotcms.cube.CubeJSQuery;
import com.liferay.portal.model.User;

/**
 * Handles the persistence retrieval of the content analytics data.
 *
 * @author Jose Castro
 * @since Sep 17th, 2024
 */
public interface ContentAnalyticsFactory {

    /**
     * Runs the report based on the query and user.
     *
     * @param query the query to run the report.
     * @param user the user to run the report.
     * @return the report response.
     */
    ReportResponse getReport(final AnalyticsQuery query, final User user);

    /**
     * Runs the raw report based on the cube js query and user.
     *
     * @param query the query to run the report.
     * @param user the user to run the report.
     * @return the report response.
     */
    ReportResponse getRawReport(final CubeJSQuery query, final User user);

    /**
     * Runs the raw report based on the cube js json string query and user.
     *
     * @param cubeJsQueryJson the query to run the report.
     * @param user the user to run the report.
     * @return the report response.
     */
    ReportResponse getRawReport(String cubeJsQueryJson, User user);
}
