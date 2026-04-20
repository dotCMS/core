package com.dotcms.auth.dotAuth.rest;

/**
 * Per-site status for the dotAuth (OAuth) configuration.
 */
public enum DotAuthSiteStatus {

    /** The site has its own {@code dotAuth} secrets row. */
    SITE_OVERRIDE,

    /** The site has no row of its own but SYSTEM_HOST does. */
    INHERITED,

    /** Neither the site nor SYSTEM_HOST has a row. */
    NOT_CONFIGURED
}
