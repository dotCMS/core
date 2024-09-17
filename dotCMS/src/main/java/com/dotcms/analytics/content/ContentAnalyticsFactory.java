package com.dotcms.analytics.content;

import com.dotcms.analytics.query.AnalyticsQuery;
import com.liferay.portal.model.User;

/**
 *
 *
 * @author Jose Castro
 * @since Sep 17th, 2024
 */
public interface ContentAnalyticsFactory {

    ReportResponse runReport(final AnalyticsQuery query, final User user);

}
