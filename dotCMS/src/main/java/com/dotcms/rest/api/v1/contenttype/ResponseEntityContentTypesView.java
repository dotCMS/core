package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.structure.model.Structure;
import java.util.List;

/**
 * Entity View for content types collection responses.
 * Contains lists of content type structures for retrieval operations.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityContentTypesView extends ResponseEntityView<List<Structure>> {
    public ResponseEntityContentTypesView(final List<Structure> entity) {
        super(entity);
    }
}