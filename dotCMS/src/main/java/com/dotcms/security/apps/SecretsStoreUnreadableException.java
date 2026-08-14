package com.dotcms.security.apps;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Raised when the App secrets store exists but this node cannot load it -- a wrong password
 * (usually a changed cluster salt or a per-node {@code SECRETS_KEYSTORE_PASSWORD_KEY}), or content
 * that will not parse.
 *
 * Exists so callers can recognise <em>this</em> condition specifically rather than catching
 * {@link DotRuntimeException}, which is dotCMS's general-purpose unchecked wrapper: database blips,
 * cache failures and plenty else surface as one. Two callers on request paths must degrade rather
 * than propagate, because raising there returns a 500 for every {@code /api/*} request
 * ({@code AppsAPIImpl.filterSitesForAppKey}, reached from {@code EMAWebInterceptor}) or escapes onto
 * the analytics collection path ({@code SiteAuthValidator}). Catching the broad type there would
 * have silently degraded unrelated infrastructure failures into "no secrets configured" too; this
 * type keeps that narrow.
 *
 * Extends {@link DotRuntimeException} so existing handlers, and the write paths that must never
 * overwrite a store they could not read, keep working unchanged. See issue #36724.
 */
public class SecretsStoreUnreadableException extends DotRuntimeException {

    public SecretsStoreUnreadableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
