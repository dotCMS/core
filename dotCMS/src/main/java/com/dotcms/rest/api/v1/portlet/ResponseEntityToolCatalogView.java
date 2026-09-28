package com.dotcms.rest.api.v1.portlet;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;

/**
 * Response envelope for the tools catalog: the standard dotCMS {@link ResponseEntityView} around
 * the list of {@link ToolCatalogEntryView} rows, sorted by title.
 *
 * @author hassandotcms
 */
public class ResponseEntityToolCatalogView extends ResponseEntityView<List<ToolCatalogEntryView>> {

    /**
     * Wraps the catalog rows.
     *
     * @param entity the catalog rows, already filtered and sorted
     */
    public ResponseEntityToolCatalogView(final List<ToolCatalogEntryView> entity) {
        super(entity);
    }
}
