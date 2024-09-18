package com.dotcms.rest.api.v1.analytics.content;

import com.dotcms.analytics.content.ContentAnalyticsAPI;
import com.dotcms.analytics.content.ReportResponse;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;

/**
 *
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public class ContentAnalyticsHelper {

    private final ContentAnalyticsAPI contentAnalyticsAPI;

    private static class SingletonHolder {
        private static final ContentAnalyticsHelper INSTANCE = new ContentAnalyticsHelper();
    }

    public static ContentAnalyticsHelper getInstance() {
        return ContentAnalyticsHelper.SingletonHolder.INSTANCE;
    }

    private ContentAnalyticsHelper() {
        this.contentAnalyticsAPI = APILocator.getContentAnalyticsAPI();
    }

    public ReportResponse query(final QueryForm queryForm, final User user) {

        return this.contentAnalyticsAPI.runReport(queryForm.getQuery(), user);
    }

}
