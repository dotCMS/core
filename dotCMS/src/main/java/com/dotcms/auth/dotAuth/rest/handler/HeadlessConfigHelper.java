package com.dotcms.auth.dotAuth.rest.handler;

import com.dotcms.auth.dotAuth.DotAuthConstants;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Read/write/mask logic for the {@code dotauth-headless} AppSecrets key.
 * <p>
 * This is NOT a {@link ProtocolHandler} — headless token exchange is not an SSO
 * protocol and does not participate in the SAML/OAuth mutual exclusion.
 */
public final class HeadlessConfigHelper {

    public static final String KEY_ENABLED               = "enabled";
    public static final String KEY_PROVIDER_TYPE          = "providerType";
    public static final String KEY_ISSUER_URL             = "issuerUrl";
    public static final String KEY_CLIENT_ID              = "clientId";
    public static final String KEY_SCOPES                 = "scopes";
    public static final String KEY_AUTHORIZATION_URL      = "authorizationUrl";
    public static final String KEY_TOKEN_URL              = "tokenUrl";
    public static final String KEY_USERINFO_URL           = "userinfoUrl";
    public static final String KEY_REVOCATION_URL         = "revocationUrl";
    public static final String KEY_LOGOUT_URL             = "logoutUrl";
    public static final String KEY_GROUPS_CLAIM           = "groupsClaim";
    public static final String KEY_GROUPS_URL             = "groupsUrl";
    public static final String KEY_EXTRA_ROLES            = "extraRoles";
    public static final String KEY_BUILD_ROLES_STRATEGY   = "buildRolesStrategy";
    public static final String KEY_CALLBACK_URL           = "callbackUrl";
    public static final String KEY_HASH_USERID            = "hashUserId";
    public static final String KEY_SESSION_REF_TTL_MINUTES = "sessionRefTtlMinutes";
    public static final String KEY_REFRESH_TTL_HOURS      = "refreshTtlHours";
    public static final String KEY_ROTATE_ON_USE          = "rotateOnUse";
    public static final String KEY_CLAMP_TO_IDP_EXP       = "clampToIdpExp";
    public static final String KEY_ALLOWED_ORIGINS        = "allowedOrigins";
    public static final String KEY_TRUSTED_IDPS           = "trustedIdps";

    static final List<String> SECRET_KEYS = List.of(
            KEY_ENABLED,
            KEY_PROVIDER_TYPE,
            KEY_ISSUER_URL,
            KEY_CLIENT_ID,
            KEY_SCOPES,
            KEY_AUTHORIZATION_URL,
            KEY_TOKEN_URL,
            KEY_USERINFO_URL,
            KEY_REVOCATION_URL,
            KEY_LOGOUT_URL,
            KEY_GROUPS_CLAIM,
            KEY_GROUPS_URL,
            KEY_EXTRA_ROLES,
            KEY_BUILD_ROLES_STRATEGY,
            KEY_CALLBACK_URL,
            KEY_HASH_USERID,
            KEY_SESSION_REF_TTL_MINUTES,
            KEY_REFRESH_TTL_HOURS,
            KEY_ROTATE_ON_USE,
            KEY_CLAMP_TO_IDP_EXP,
            KEY_ALLOWED_ORIGINS,
            KEY_TRUSTED_IDPS);

    private static final Set<String> BOOLEAN_KEYS = Set.of(
            KEY_ENABLED,
            KEY_HASH_USERID,
            KEY_ROTATE_ON_USE,
            KEY_CLAMP_TO_IDP_EXP);

    public String appKey() {
        return DotAuthConstants.HEADLESS_APP_KEY;
    }

    public List<String> secretKeys() {
        return SECRET_KEYS;
    }

    public Map<String, Object> values(final AppSecrets secrets) {
        final Map<String, Secret> raw = secrets.getSecrets();
        final Map<String, Object> out = new HashMap<>();
        for (final String key : SECRET_KEYS) {
            final Secret secret = raw.get(key);
            if (secret == null) continue;
            if (BOOLEAN_KEYS.contains(key)) { out.put(key, Try.of(secret::getBoolean).getOrElse(false)); continue; }
            out.put(key, Try.of(secret::getString).getOrElse(""));
        }
        return out;
    }

    public AppSecrets buildSecrets(final Map<String, Object> incoming) {
        final AppSecrets.Builder builder = AppSecrets.builder()
                .withKey(DotAuthConstants.HEADLESS_APP_KEY);
        for (final String key : SECRET_KEYS) {
            final Object raw = incoming.get(key);
            if (raw == null) continue;
            if (BOOLEAN_KEYS.contains(key)) {
                builder.withSecret(key, Boolean.parseBoolean(String.valueOf(raw)));
            } else {
                builder.withSecret(key, String.valueOf(raw));
            }
        }
        return builder.build();
    }
}
