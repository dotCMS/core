package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;

/**
 * throw when a page is not found
 */
public class HTMLPageAssetNotFoundException extends RuntimeException {
    public HTMLPageAssetNotFoundException(String pageUri) {
        this(pageUri, null);
    }

    public HTMLPageAssetNotFoundException(String pageUri, Exception cause) {
        super(String.format("Page '%s' is not found", pageUri), cause);
    }
}
