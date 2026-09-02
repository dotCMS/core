package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Response wrapper for contentlets with its container, page and styles information.
 */
public class ResponseEntityContentView extends ResponseEntityView<List<ContentView>> {

    /**
     * Constructor for contentlets with its container, page and styles information.
     *
     * @param contentletStylingList The list of ContentView objects with to be included in the response.
     */
    public ResponseEntityContentView(List<ContentView> contentletStylingList) {
        super(contentletStylingList);
    }
}