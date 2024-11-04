package com.dotcms.analytics.content;

import com.dotcms.analytics.query.AnalyticsQuery;
import com.dotcms.cube.CubeJSQuery;
import com.liferay.portal.model.User;

/**
 * This API allows users to access Content Analytics in dotCMS. Content Analytics will enable
 * customers to track the health and engagement of their content at the level of individual content
 * items. This could also allow users to eventually include viewing analytics data in the UI of the
 * dotCMS back-end.
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

    /**
     * Runs a raw report based on a cubeJS json string query
     * @param cubeJsQueryJson
     * @param user
     * @return ReportResponse
     */
    ReportResponse runRawReport(String cubeJsQueryJson, User user);
}
