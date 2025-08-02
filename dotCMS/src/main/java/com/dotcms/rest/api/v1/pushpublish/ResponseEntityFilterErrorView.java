package com.dotcms.rest.api.v1.pushpublish;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for push publishing filter error responses.
 * Contains error information when filter operations fail.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFilterErrorView extends ResponseEntityView<String> {
    public ResponseEntityFilterErrorView(final List<ErrorEntity> errors) {
        super(errors);
    }
}