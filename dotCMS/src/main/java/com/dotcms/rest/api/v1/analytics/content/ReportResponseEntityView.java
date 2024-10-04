package com.dotcms.rest.api.v1.analytics.content;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;
import java.util.Map;

/**
 * View for the analytics report response entity.
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public class ReportResponseEntityView extends ResponseEntityView<List<Map<String, Object>>> {

    public ReportResponseEntityView(final List<Map<String, Object>> entity) {
        super(entity);
    }

}
