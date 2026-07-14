package com.dotcms.auth.dotAuth.rest.handler;

import com.dotcms.auth.dotAuth.rest.DotAuthProtocol;
import com.dotcms.security.apps.AppSecrets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Per-protocol strategy for reading and writing secrets through {@link
 * com.dotcms.security.apps.AppsAPI}. Implementations own their secret-key
 * set, their hidden-key set (values masked on GET, preserved on PUT when the
 * client posts back the mask), and their boolean-key set (stored as
 * primitives rather than strings).
 */
public interface ProtocolHandler {

    /** The protocol this handler serves. */
    DotAuthProtocol protocol();

    /** AppSecrets key under which this protocol's secrets are stored. */
    String appKey();

    /** Ordered list of secret keys the portlet is allowed to read/write. */
    List<String> secretKeys();

    /** Subset of {@link #secretKeys()} whose values are masked on GET. */
    Set<String> hiddenKeys();

    /** Subset of {@link #secretKeys()} whose values are stored as booleans. */
    Set<String> booleanKeys();

    /**
     * Render an AppSecrets object for the client. Hidden-key values are
     * replaced with the mask sentinel; boolean keys are unboxed.
     */
    Map<String, Object> maskedValues(AppSecrets secrets);

    /**
     * Build an AppSecrets object from a form submission. If {@code existing}
     * is present and the incoming value for a hidden key is the mask
     * sentinel, the stored value is preserved.
     */
    AppSecrets buildSecrets(Map<String, Object> incoming, Optional<AppSecrets> existing);
}
