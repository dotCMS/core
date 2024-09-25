package com.dotcms.rest.api.v1.analytics.content;

import com.dotcms.analytics.content.ReportResponse;
import com.dotcms.rest.ResponseEntityView;

/**
 *
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public class ReportResponseEntityView extends ResponseEntityView<ReportResponse> {

    public ReportResponseEntityView(final ReportResponse entity) {
        super(entity);
    }

}
