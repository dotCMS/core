package com.dotcms.analytics.content;

import com.dotcms.analytics.query.AnalyticsQuery;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

/**
 * Implementation class for the {@link ContentAnalyticsAPI} interface.
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public class ContentAnalyticsAPIImpl implements ContentAnalyticsAPI {

    private final ContentAnalyticsFactory contentAnalyticsFactory;

    public ContentAnalyticsAPIImpl() {
        this(new ContentAnalyticsFactoryImpl());
    }

    @VisibleForTesting
    public ContentAnalyticsAPIImpl(final ContentAnalyticsFactory contentAnalyticsFactory) {
        this.contentAnalyticsFactory = contentAnalyticsFactory;
    }

    @Override
    public ReportResponse runReport(final AnalyticsQuery query, final User user) {
        return this.contentAnalyticsFactory.runReport(query, user);
    }

}
