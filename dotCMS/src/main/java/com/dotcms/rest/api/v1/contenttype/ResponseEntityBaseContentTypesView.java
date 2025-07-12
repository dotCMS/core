package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for base content types collection responses.
 * Contains lists of base content type views for type selection operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityBaseContentTypesView extends ResponseEntityView<List<BaseContentTypesView>> {
    public ResponseEntityBaseContentTypesView(final List<BaseContentTypesView> entity) {
        super(entity);
    }
}