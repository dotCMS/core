package com.dotcms.rest.api.v1.usage;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

/**
 * {@link ResponseEntityView} for usage dashboard summary
 */
public class ResponseEntityUsageSummaryView extends ResponseEntityView<UsageSummary> {

    public ResponseEntityUsageSummaryView(final UsageSummary entity) {
        super(entity);
    }

    public ResponseEntityUsageSummaryView(final UsageSummary entity, final Map<String, String> i18nMessagesMap) {
        super(entity, i18nMessagesMap);
    }
}