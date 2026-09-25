package com.dotcms.rest.api.v1.layout;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;

/**
 * Response envelope for the navigation section list returned by {@code GET /v1/layouts} and by
 * the writes that return the full list (delete, reorder, set tools).
 *
 * @author hassandotcms
 */
public class ResponseEntitySectionListView extends ResponseEntityView<List<SectionView>> {

    public ResponseEntitySectionListView(final List<SectionView> entity) {
        super(entity);
    }
}
