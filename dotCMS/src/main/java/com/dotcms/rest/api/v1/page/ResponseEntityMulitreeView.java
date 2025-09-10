package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for multitree structure responses.
 * Contains the relationships between pages, containers, and contentlets.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityMulitreeView extends ResponseEntityView<List<MulitreeView>> {
    public ResponseEntityMulitreeView(final List<MulitreeView> entity) {
        super(entity);
    }
}