package com.dotcms.analytics.content;

import com.dotcms.rest.api.v1.anaytics.content.QueryForm;
import com.dotmarketing.business.APILocator;

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

    public ReportResponse query(final QueryForm queryForm) {
        final ReportRequest reportRequest = new ReportRequest.Builder().build();
        return this.contentAnalyticsAPI.runReport(reportRequest);

    }

}
