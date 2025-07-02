package com.dotcms.rest.api.v1.index;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for index list responses.
 * Contains list of available Elasticsearch indices.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityIndexListView extends ResponseEntityView<List<String>> {
    public ResponseEntityIndexListView(final List<String> entity) {
        super(entity);
    }
}
