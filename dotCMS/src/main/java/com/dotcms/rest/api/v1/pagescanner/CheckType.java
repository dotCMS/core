package com.dotcms.rest.api.v1.pagescanner;

/**
 * Identifies which type of scan to run against the Page Scanner upstream service.
 */
public enum CheckType {

    a11y, geo;

    /** Returns the URL path segment used by the upstream service (e.g. "a11y", "geo"). */
    public String pathSegment() {
        return this.name();
    }
}
