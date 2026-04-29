package com.dotcms.auth.dotAuth.rest.handler;

import com.dotcms.auth.dotAuth.DotAuthConstants;
import com.dotcms.auth.dotAuth.rest.DotAuthProtocol;
import com.dotcms.auth.providers.oauth.OAuthAppConfig;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class OAuthProtocolHandler implements ProtocolHandler {

    private static final List<String> SECRET_KEYS = List.of(
            OAuthAppConfig.KEY_ENABLED,
            OAuthAppConfig.KEY_ENABLE_BACKEND,
            OAuthAppConfig.KEY_ENABLE_FRONTEND,
            OAuthAppConfig.KEY_PROVIDER_TYPE,
            OAuthAppConfig.KEY_ISSUER_URL,
            OAuthAppConfig.KEY_CLIENT_ID,
            OAuthAppConfig.KEY_CLIENT_SECRET,
            OAuthAppConfig.KEY_SCOPES,
            OAuthAppConfig.KEY_AUTHORIZATION_URL,
            OAuthAppConfig.KEY_TOKEN_URL,
            OAuthAppConfig.KEY_USERINFO_URL,
            OAuthAppConfig.KEY_REVOCATION_URL,
            OAuthAppConfig.KEY_LOGOUT_URL,
            OAuthAppConfig.KEY_GROUPS_CLAIM,
            OAuthAppConfig.KEY_GROUPS_URL,
            OAuthAppConfig.KEY_EXTRA_ROLES,
            OAuthAppConfig.KEY_BUILD_ROLES_STRATEGY,
            OAuthAppConfig.KEY_CALLBACK_URL,
            OAuthAppConfig.KEY_HASH_USERID,
            OAuthAppConfig.KEY_EXCHANGE_ENABLED,
            OAuthAppConfig.KEY_EXCHANGE_PROVIDER_TYPE,
            OAuthAppConfig.KEY_EXCHANGE_ISSUER_URL,
            OAuthAppConfig.KEY_EXCHANGE_CLIENT_ID,
            OAuthAppConfig.KEY_EXCHANGE_CLIENT_SECRET,
            OAuthAppConfig.KEY_EXCHANGE_SCOPES,
            OAuthAppConfig.KEY_EXCHANGE_AUTHORIZATION_URL,
            OAuthAppConfig.KEY_EXCHANGE_TOKEN_URL,
            OAuthAppConfig.KEY_EXCHANGE_USERINFO_URL,
            OAuthAppConfig.KEY_EXCHANGE_REVOCATION_URL,
            OAuthAppConfig.KEY_EXCHANGE_LOGOUT_URL,
            OAuthAppConfig.KEY_EXCHANGE_GROUPS_CLAIM,
            OAuthAppConfig.KEY_EXCHANGE_GROUPS_URL,
            OAuthAppConfig.KEY_EXCHANGE_EXTRA_ROLES,
            OAuthAppConfig.KEY_EXCHANGE_BUILD_ROLES_STRATEGY,
            OAuthAppConfig.KEY_EXCHANGE_CALLBACK_URL,
            OAuthAppConfig.KEY_EXCHANGE_HASH_USERID,
            "headlessSessionRefTtlMinutes",
            "headlessRefreshTtlHours",
            "headlessRotateOnUse",
            "headlessClampToIdpExp",
            "headlessAllowedOrigins",
            "headlessTrustedIdps");

    private static final Set<String> HIDDEN_KEYS = Set.of(
            OAuthAppConfig.KEY_CLIENT_SECRET,
            OAuthAppConfig.KEY_EXCHANGE_CLIENT_SECRET);

    private static final Set<String> BOOLEAN_KEYS = Set.of(
            OAuthAppConfig.KEY_ENABLED,
            OAuthAppConfig.KEY_ENABLE_BACKEND,
            OAuthAppConfig.KEY_ENABLE_FRONTEND,
            OAuthAppConfig.KEY_HASH_USERID,
            OAuthAppConfig.KEY_EXCHANGE_ENABLED,
            OAuthAppConfig.KEY_EXCHANGE_HASH_USERID,
            "headlessRotateOnUse",
            "headlessClampToIdpExp");

    @Override public DotAuthProtocol protocol() { return DotAuthProtocol.OAUTH; }
    @Override public String appKey()            { return DotAuthConstants.APP_KEY; }
    @Override public List<String> secretKeys()  { return SECRET_KEYS; }
    @Override public Set<String> hiddenKeys()   { return HIDDEN_KEYS; }
    @Override public Set<String> booleanKeys()  { return BOOLEAN_KEYS; }

    @Override
    public Map<String, Object> maskedValues(final AppSecrets secrets) {
        final Map<String, Secret> raw = secrets.getSecrets();
        final Map<String, Object> out = new HashMap<>();
        for (final String key : SECRET_KEYS) {
            final Secret secret = raw.get(key);
            if (secret == null) continue;
            if (HIDDEN_KEYS.contains(key))  { out.put(key, DotAuthConstants.HIDDEN_SECRET_MASK); continue; }
            if (BOOLEAN_KEYS.contains(key)) { out.put(key, Try.of(secret::getBoolean).getOrElse(false)); continue; }
            out.put(key, Try.of(secret::getString).getOrElse(""));
        }
        return out;
    }

    @Override
    public AppSecrets buildSecrets(final Map<String, Object> incoming,
                                   final Optional<AppSecrets> existing) {
        final AppSecrets.Builder builder = AppSecrets.builder().withKey(DotAuthConstants.APP_KEY);
        for (final String key : SECRET_KEYS) {
            final Object raw = incoming.get(key);
            if (HIDDEN_KEYS.contains(key)) {
                final String str = raw == null ? null : String.valueOf(raw);
                if (DotAuthConstants.HIDDEN_SECRET_MASK.equals(str)) {
                    existing.map(AppSecrets::getSecrets)
                            .map(m -> m.get(key))
                            .ifPresent(secret -> builder.withSecret(key, secret));
                    continue;
                }
                if (str == null || str.isEmpty()) continue;
                builder.withHiddenSecret(key, str);
                continue;
            }
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
