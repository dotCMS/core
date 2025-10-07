package com.dotcms.rest.api.v1.event;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for event error responses.
 * Contains error information when event operations fail or timeout.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityEventErrorView extends ResponseEntityView<String> {
    public ResponseEntityEventErrorView(final List<ErrorEntity> errors) {
        super(errors);
    }
}