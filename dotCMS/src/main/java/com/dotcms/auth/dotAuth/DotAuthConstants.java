package com.dotcms.auth.dotAuth;

/**
 * Constants for the {@code dotAuth} App/portlet. Single source of truth for the
 * AppSecrets key used by the OAuth runtime and by the dotAuth portlet's REST layer.
 */
public final class DotAuthConstants {

    /** AppSecrets key under which the dotAuth portlet stores OAuth config. */
    public static final String APP_KEY = "dotAuth";

    private DotAuthConstants() {}
}
