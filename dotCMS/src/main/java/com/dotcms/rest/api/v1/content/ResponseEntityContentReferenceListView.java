package com.dotcms.rest.api.v1.content;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for ResponseEntityView<List<ContentReferenceView>>
 * 
 * @author Steve Bolton
 */
public class ResponseEntityContentReferenceListView extends ResponseEntityView<List<ContentReferenceView>> {

    public ResponseEntityContentReferenceListView(List<ContentReferenceView> entity) {
        super(entity);
    }
}