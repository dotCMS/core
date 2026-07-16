package com.dotcms.rest.api.v1.personalization;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for personalization operation responses.
 * Contains personalization data or operation results.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityPersonalizationView extends ResponseEntityView<List<Object>> {
    public ResponseEntityPersonalizationView(final List<Object> entity) {
        super(entity);
    }
}