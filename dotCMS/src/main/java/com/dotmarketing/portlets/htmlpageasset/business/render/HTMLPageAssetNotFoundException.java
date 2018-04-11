package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;

/**
 * throw when a page is not found
 */
public class HTMLPageAssetNotFoundException extends RuntimeException {
    HTMLPageAssetNotFoundException(String pageUri) {
        super(String.format("Page '%s' is not found", pageUri));
    }
}
