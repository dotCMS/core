package com.dotcms.rest.api.v1.usage;

import com.dotcms.rest.ResponseEntityView;

/**
 * {@link ResponseEntityView} for usage dashboard summary
 */
public class ResponseEntityUsageSummaryView extends ResponseEntityView<UsageSummary> {

    public ResponseEntityUsageSummaryView(final UsageSummary entity) {
        super(entity);
    }
}