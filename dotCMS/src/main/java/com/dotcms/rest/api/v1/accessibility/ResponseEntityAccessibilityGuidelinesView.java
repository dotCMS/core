package com.dotcms.rest.api.v1.accessibility;

import com.dotcms.enterprise.achecker.model.GuideLineBean;
import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for accessibility guidelines responses.
 * Contains list of accessibility guidelines available in the system.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityAccessibilityGuidelinesView extends ResponseEntityView<List<GuideLineBean>> {
    public ResponseEntityAccessibilityGuidelinesView(final List<GuideLineBean> entity) {
        super(entity);
    }
}