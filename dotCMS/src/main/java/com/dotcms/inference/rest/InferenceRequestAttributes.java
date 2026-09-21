package com.dotcms.inference.rest;

/**
 * Request-scoped attribute keys shared between the resources and the family's filters.
 *
 * <p>The resolved site is published here rather than returned, so the response filter can report
 * it even on a response the resource never produced — an exception mapped to a 4xx, or a stream
 * that failed after it began. The serving site is reported on <em>every</em> response, and a
 * value only the happy path can set would not deliver that.</p>
 */
public final class InferenceRequestAttributes {

    /** Identifier of the site whose configuration served the request. */
    public static final String RESOLVED_SITE_ID = "com.dotcms.inference.resolvedSiteId";

    private InferenceRequestAttributes() {
    }
}
