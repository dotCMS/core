package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.PaginatedArrayList;

import java.util.Map;

/**
 * Entity View for multitree structure responses.
 * Contains the relationships between pages, containers, and contentlets.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityPaginatedArrayListHostView extends ResponseEntityView<PaginatedArrayList<Host>> {

    public ResponseEntityPaginatedArrayListHostView(PaginatedArrayList<Host> entity) {
        super(entity);
    }
}