package com.dotcms.rest.api.v1.pushpublish;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for push publishing filter names list responses.
 * Contains a list of filter keys/names after operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityFilterNamesView extends ResponseEntityView<List<String>> {
    public ResponseEntityFilterNamesView(final List<String> entity) {
        super(entity);
    }
}