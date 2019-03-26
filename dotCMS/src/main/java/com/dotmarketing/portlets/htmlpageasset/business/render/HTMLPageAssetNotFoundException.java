package com.dotmarketing.portlets.htmlpageasset.business.render;

/** throw when a page is not found */
public class HTMLPageAssetNotFoundException extends RuntimeException {
  public HTMLPageAssetNotFoundException(final String pageUri) {
    this(pageUri, null);
  }

  public HTMLPageAssetNotFoundException(final String pageUri, final Exception cause) {
    super(String.format("Page '%s' not found", pageUri), cause);
  }
}
