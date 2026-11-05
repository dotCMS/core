package com.dotcms.saml.service.internal;

/**
 * Remembers the SAML assertions that have been accepted, so each one is accepted only once.
 *
 * @author dotCMS
 */
public interface AssertionReplayStore {

    /**
     * Records an assertion as used.
     *
     * @param key       identifies the assertion (IdP and assertion ID)
     * @param expiresAt epoch millis after which the assertion is no longer valid, so it can be forgotten
     * @return true the first time the key is recorded, false when it was already used (a replay)
     * @throws com.dotcms.saml.service.external.SamlException when the store can't tell; callers must reject
     */
    boolean markUsed(String key, long expiresAt);
}
