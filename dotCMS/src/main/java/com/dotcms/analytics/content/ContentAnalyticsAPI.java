package com.dotcms.analytics.content;

import com.dotcms.analytics.query.AnalyticsQuery;
import com.liferay.portal.model.User;

/**
 *
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public interface ContentAnalyticsAPI {

    ReportResponse runReport(final AnalyticsQuery query, final User user);

}
