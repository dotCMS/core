package com.dotcms.auth.dotAuth;

/**
 * Constants for the {@code dotAuth} App/portlet. Single source of truth for the
 * OAuth-flavor AppSecrets key (SAML uses its own {@code dotsaml-config} key from
 * {@link com.dotcms.saml.DotSamlProxyFactory#SAML_APP_CONFIG_KEY}).
 */
public final class DotAuthConstants {

    /** AppSecrets key under which the OAuth runtime stores its config. */
    public static final String APP_KEY = "dotAuth";

    /**
     * Value returned for hidden secrets in the dotAuth REST surface. When a client
     * posts this value back on a hidden key, the stored secret is preserved.
     */
    public static final String HIDDEN_SECRET_MASK = "****";

    private DotAuthConstants() {}
}
