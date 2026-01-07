package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Response wrapper for contentlets with its container, page and styles information.
 */
public class ResponseEntityContentletStylingView extends ResponseEntityView<List<ContentletStylingView>> {

    /**
     * Constructor for contentlets with its container, page and styles information.
     *
     * @param contentletStylingList The list of ContentletStylingView objects to be included in the response.
     */
    public ResponseEntityContentletStylingView(List<ContentletStylingView> contentletStylingList) {
        super(contentletStylingList);
    }
}