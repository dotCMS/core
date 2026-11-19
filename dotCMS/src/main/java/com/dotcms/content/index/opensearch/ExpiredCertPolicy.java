package com.dotcms.content.index.opensearch;

import com.dotmarketing.exception.DotRuntimeException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * What the OpenSearch client does when the server presents a certificate that is expired, or not
 * valid yet, configured with {@code OS_TLS_ON_EXPIRED_CERT}.
 *
 * <p>The default is {@link #WARN} because it preserves the behaviour of every existing
 * installation: a trust anchor's own validity dates are not checked by PKIX, so a pinned
 * self-signed certificate that expired used to be accepted in silence. {@link #REJECT} turns that
 * into a refused connection, and {@link #ALLOW} restores the silence.</p>
 */
public enum ExpiredCertPolicy {

    /** Accept the certificate without saying anything. */
    ALLOW,

    /** Accept the certificate and log a warning naming the certificate and its expiry date. */
    WARN,

    /** Refuse the connection. */
    REJECT;

    /**
     * Resolves the value configured in {@code OS_TLS_ON_EXPIRED_CERT}, ignoring case.
     *
     * @param value the configured value, which may be {@code null} or blank for the default
     * @return the matching policy, or {@link #WARN} when nothing is configured
     * @throws DotRuntimeException when the value does not name a policy, so that a typo fails
     *         loudly at startup instead of silently selecting a weaker behaviour
     */
    public static ExpiredCertPolicy from(final String value) {
        if (value == null || value.trim().isEmpty()) {
            return WARN;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DotRuntimeException("Invalid OS_TLS_ON_EXPIRED_CERT value '" + value
                    + "'. Valid values are: "
                    + Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", ")), e);
        }
    }
}
