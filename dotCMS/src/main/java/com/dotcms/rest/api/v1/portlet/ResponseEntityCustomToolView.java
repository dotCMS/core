package com.dotcms.rest.api.v1.portlet;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response envelope for the single custom-tool read: the standard dotCMS
 * {@link ResponseEntityView} around one {@link CustomToolView}.
 *
 * @author hassandotcms
 */
public class ResponseEntityCustomToolView extends ResponseEntityView<CustomToolView> {

    /**
     * Wraps one custom tool's configuration.
     *
     * @param entity the custom tool
     */
    public ResponseEntityCustomToolView(final CustomToolView entity) {
        super(entity);
    }
}
