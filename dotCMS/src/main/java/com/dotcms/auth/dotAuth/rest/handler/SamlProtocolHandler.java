package com.dotcms.auth.dotAuth.rest.handler;

import com.dotcms.auth.dotAuth.DotAuthConstants;
import com.dotcms.auth.dotAuth.rest.DotAuthProtocol;
import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.security.apps.AppSecrets;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.security.apps.Secret;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import javax.ws.rs.BadRequestException;

public final class SamlProtocolHandler implements ProtocolHandler {

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

    /**
     * Extra-key names must be identifier-shaped. Bars whitespace tricks like
     * {@code "privateKey "} (trailing space) that would otherwise slip past the
     * {@link #SAML_SECRET_KEYS} contains() check and end up stored unmasked.
     */
    private static final Pattern EXTRA_KEY_NAME = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    /**
     * Cap per-value length to keep arbitrary extra secrets from turning into a
     * cheap storage amplification channel. 8 KiB is well above anything a real
     * SAML attribute mapping would need.
     */
    private static final int MAX_EXTRA_VALUE_LENGTH = 8 * 1024;

    @Override public DotAuthProtocol protocol() { return DotAuthProtocol.SAML; }
    @Override public String appKey()            { return DotSamlProxyFactory.SAML_APP_CONFIG_KEY; }
    @Override public List<String> secretKeys()  { return SAML_SECRET_KEYS; }
    @Override public Set<String> hiddenKeys()   { return HIDDEN_KEYS; }
    @Override public Set<String> booleanKeys()  { return BOOLEAN_KEYS; }

    @Override
    public Map<String, Object> maskedValues(final AppSecrets secrets) {
        final Map<String, Secret> raw = secrets.getSecrets();
        final Map<String, Object> out = new HashMap<>();
        // Declared keys first, in YAML order, with hidden/boolean semantics.
        for (final String key : SAML_SECRET_KEYS) {
            final Secret secret = raw.get(key);
            if (secret == null) continue;
            if (HIDDEN_KEYS.contains(key))  { out.put(key, DotAuthConstants.HIDDEN_SECRET_MASK); continue; }
            if (BOOLEAN_KEYS.contains(key)) { out.put(key, Try.of(secret::getBoolean).getOrElse(false)); continue; }
            out.put(key, Try.of(secret::getString).getOrElse(""));
        }
        // Custom attributes: any stored keys not in the declared schema. Emitted
        // as strings with no mask — the Apps descriptor has allowExtraParameters
        // so admins can store arbitrary SAML-handler settings (e.g. emailAttribute,
        // rolesAttribute, autoCreateUsers) alongside the declared params.
        for (final Map.Entry<String, Secret> entry : raw.entrySet()) {
            final String key = entry.getKey();
            if (SAML_SECRET_KEYS.contains(key)) continue;
            out.put(key, Try.of(entry.getValue()::getString).getOrElse(""));
        }
        return out;
    }

    @Override
    public AppSecrets buildSecrets(final Map<String, Object> incoming,
                                   final Optional<AppSecrets> existing) {
        final AppSecrets.Builder builder = AppSecrets.builder().withKey(appKey());

        boolean hasPrivateKey = false;
        boolean hasPublicCert = false;

        for (final String key : SAML_SECRET_KEYS) {
            final Object raw = incoming.get(key);
            if (HIDDEN_KEYS.contains(key)) {
                final String str = raw == null ? null : String.valueOf(raw);
                if (DotAuthConstants.HIDDEN_SECRET_MASK.equals(str)) {
                    existing.map(AppSecrets::getSecrets)
                            .map(m -> m.get(key))
                            .ifPresent(secret -> {
                                builder.withSecret(key, secret);
                            });
                    if ("privateKey".equals(key)) {
                        hasPrivateKey = existing.map(AppSecrets::getSecrets)
                                .map(m -> m.get(key)).isPresent();
                    }
                    continue;
                }
                if (str == null || str.isEmpty()) continue;
                builder.withHiddenSecret(key, str);
                if ("privateKey".equals(key)) hasPrivateKey = true;
                continue;
            }
            if (raw == null) continue;
            if (BOOLEAN_KEYS.contains(key)) {
                builder.withSecret(key, Boolean.parseBoolean(String.valueOf(raw)));
            } else {
                builder.withSecret(key, String.valueOf(raw));
                if ("publicCert".equals(key) && UtilMethods.isSet(String.valueOf(raw))) {
                    hasPublicCert = true;
                }
            }
        }

        if (!hasPrivateKey && !hasPublicCert) {
            final String hostname = incoming.get("sPEndpointHostname") != null
                    ? String.valueOf(incoming.get("sPEndpointHostname"))
                    : null;
            final SamlKeyPairGenerator.GeneratedKeyPair kp =
                    SamlKeyPairGenerator.generate(hostname);
            builder.withHiddenSecret("privateKey", kp.privateKeyPem);
            builder.withSecret("publicCert", kp.publicCertPem);
        }

        // Pass through any extra keys the client submitted as plain strings.
        // The client is authoritative — if it doesn't send a previously-stored
        // extra, it's dropped (admins remove rows through the UI).
        for (final Map.Entry<String, Object> entry : incoming.entrySet()) {
            final String key = entry.getKey();
            if (SAML_SECRET_KEYS.contains(key) || entry.getValue() == null) continue;
            if (!EXTRA_KEY_NAME.matcher(key).matches()) {
                throw new BadRequestException("Invalid SAML extra-attribute key '" + key
                        + "' — must match " + EXTRA_KEY_NAME.pattern());
            }
            final String str = String.valueOf(entry.getValue());
            if (str.isEmpty()) continue;
            if (str.length() > MAX_EXTRA_VALUE_LENGTH) {
                throw new BadRequestException("SAML extra-attribute '" + key
                        + "' value exceeds " + MAX_EXTRA_VALUE_LENGTH + " chars");
            }
            builder.withSecret(key, str);
        }
        return builder.build();
    }
}
