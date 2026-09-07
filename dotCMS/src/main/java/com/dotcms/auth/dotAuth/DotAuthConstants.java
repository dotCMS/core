package com.dotcms.auth.dotAuth;

import com.dotmarketing.util.Config;

/**
 * Constants for the {@code dotAuth} App/portlet. Single source of truth for the
 * OAuth-flavor AppSecrets key (SAML uses its own {@code dotsaml-config} key from
 * {@link com.dotcms.saml.DotSamlProxyFactory#SAML_APP_CONFIG_KEY}).
 */
public final class DotAuthConstants {

    /** AppSecrets key under which the OAuth SSO runtime stores its config. */
    public static final String APP_KEY = "dotAuth";

    /** AppSecrets key for headless token-exchange config (separate from SSO). */
    public static final String HEADLESS_APP_KEY = "dotauth-headless";

    /** Internal metadata key used to resolve temporary SSO protocol overlap after save. */
    public static final String LAST_SAVED_PROTOCOL_AT_KEY = "__dotauthLastSavedProtocolAt";

    /**
     * Value returned for hidden secrets in the dotAuth REST surface. When a client
     * posts this value back on a hidden key, the stored secret is preserved.
     */
    public static final String HIDDEN_SECRET_MASK = "****";

    /** Query param that lets an operator skip SSO and reach the native login form. */
    public static final String BYPASS_PARAM = "native";

    /**
     * Value {@link #BYPASS_PARAM} must carry to skip SSO. Shared by every dotAuth protocol
     * so hardening the token in one place covers OAuth/OIDC and SAML alike. {@code
     * SAML_BYPASS_VALUE} is honored first for backwards compatibility with existing SAML
     * deployments; {@code SSO_BYPASS_TOKEN} is the protocol-neutral name.
     */
    public static String getBypassValue() {
        return Config.getStringProperty("SAML_BYPASS_VALUE",
                Config.getStringProperty("SSO_BYPASS_TOKEN", "true"));
    }

    private DotAuthConstants() {}
}
