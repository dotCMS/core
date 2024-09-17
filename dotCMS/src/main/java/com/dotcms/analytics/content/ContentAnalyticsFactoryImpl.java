package com.dotcms.analytics.content;

import com.dotcms.analytics.query.AnalyticsQuery;
import com.dotcms.analytics.query.AnalyticsQueryParser;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

/**
 * This class is the implementation of the {@link ContentAnalyticsFactory} interface.
 *
 * @author Jose Castro
 * @since Sep 17th, 2024
 */
public class ContentAnalyticsFactoryImpl implements ContentAnalyticsFactory {

    private final AnalyticsQueryParser queryParser;

    public ContentAnalyticsFactoryImpl() {
        this(new AnalyticsQueryParser());
    }

    @VisibleForTesting
    public ContentAnalyticsFactoryImpl(final AnalyticsQueryParser queryParser) {
        this.queryParser = queryParser;
    }

    @Override
    public ReportResponse runReport(final AnalyticsQuery query, final User user) {

        return null;
    }

}
