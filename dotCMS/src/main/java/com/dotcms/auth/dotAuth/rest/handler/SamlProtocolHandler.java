package com.dotcms.auth.dotAuth.rest.handler;

import com.dotcms.auth.dotAuth.rest.DotAuthProtocol;
import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SamlProtocolHandler implements ProtocolHandler {

    public static final String HIDDEN_SECRET_MASK = "****";

    /** Ordered to match the schema in dotsaml-config.yml. */
    private static final List<String> SAML_SECRET_KEYS = List.of(
            "enable",
            "idpName",
            "sPIssuerURL",
            "sPEndpointHostname",
            "signatureValidationType",
            "idPMetadataFile",
            "publicCert",
            "privateKey",
            "buttonParam");

    private static final Set<String> HIDDEN_KEYS = Set.of("privateKey");

    private static final Set<String> BOOLEAN_KEYS = Set.of("enable");

    @Override public DotAuthProtocol protocol() { return DotAuthProtocol.SAML; }
    @Override public String appKey()            { return DotSamlProxyFactory.SAML_APP_CONFIG_KEY; }
    @Override public List<String> secretKeys()  { return SAML_SECRET_KEYS; }
    @Override public Set<String> hiddenKeys()   { return HIDDEN_KEYS; }
    @Override public Set<String> booleanKeys()  { return BOOLEAN_KEYS; }

    @Override
    public Map<String, Object> maskedValues(final AppSecrets secrets) {
        final Map<String, Secret> raw = secrets.getSecrets();
        final Map<String, Object> out = new HashMap<>();
        for (final String key : SAML_SECRET_KEYS) {
            final Secret secret = raw.get(key);
            if (secret == null) continue;
            if (HIDDEN_KEYS.contains(key))  { out.put(key, HIDDEN_SECRET_MASK); continue; }
            if (BOOLEAN_KEYS.contains(key)) { out.put(key, Try.of(secret::getBoolean).getOrElse(false)); continue; }
            out.put(key, Try.of(secret::getString).getOrElse(""));
        }
        return out;
    }

    @Override
    public AppSecrets buildSecrets(final Map<String, Object> incoming,
                                   final Optional<AppSecrets> existing) {
        final AppSecrets.Builder builder = AppSecrets.builder().withKey(appKey());
        for (final String key : SAML_SECRET_KEYS) {
            final Object raw = incoming.get(key);
            if (HIDDEN_KEYS.contains(key)) {
                final String str = raw == null ? null : String.valueOf(raw);
                if (HIDDEN_SECRET_MASK.equals(str)) {
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
