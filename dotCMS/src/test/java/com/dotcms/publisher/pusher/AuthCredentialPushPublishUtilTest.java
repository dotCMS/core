package com.dotcms.publisher.pusher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link AuthCredentialPushPublishUtil}.
 */
public class AuthCredentialPushPublishUtilTest {

    private static final String ENDPOINT_KEY = "c2VjcmV0LWtleS1kaWdlc3QtdmFsdWU=";

    /**
     * Given: the key sent in the request is the same as the endpoint key
     * Expected: the keys match
     */
    @Test
    public void test_keyMatches_sameKey_returnsTrue() {
        assertTrue(AuthCredentialPushPublishUtil.keyMatches(ENDPOINT_KEY, ENDPOINT_KEY));
    }

    /**
     * Given: a key with the same length as the endpoint key but different content
     * Expected: the keys don't match
     */
    @Test
    public void test_keyMatches_differentKeySameLength_returnsFalse() {
        final String otherKey = "X" + ENDPOINT_KEY.substring(1);
        assertFalse(AuthCredentialPushPublishUtil.keyMatches(otherKey, ENDPOINT_KEY));
    }

    /**
     * Given: a key that only shares a prefix with the endpoint key
     * Expected: the keys don't match
     */
    @Test
    public void test_keyMatches_prefixOfKey_returnsFalse() {
        final String prefix = ENDPOINT_KEY.substring(0, ENDPOINT_KEY.length() - 4);
        assertFalse(AuthCredentialPushPublishUtil.keyMatches(prefix, ENDPOINT_KEY));
    }

    /**
     * Given: a key that differs from the endpoint key only in letter case
     * Expected: the keys don't match
     */
    @Test
    public void test_keyMatches_differentCase_returnsFalse() {
        assertFalse(AuthCredentialPushPublishUtil.keyMatches(ENDPOINT_KEY.toLowerCase(), ENDPOINT_KEY));
    }

    /**
     * Given: the request has no key (no Authorization header)
     * Expected: the keys don't match
     */
    @Test
    public void test_keyMatches_emptyKey_returnsFalse() {
        assertFalse(AuthCredentialPushPublishUtil.keyMatches("", ENDPOINT_KEY));
    }

    /**
     * Given: a null key on either side
     * Expected: the keys don't match and no exception is thrown
     */
    @Test
    public void test_keyMatches_nullKey_returnsFalse() {
        assertFalse(AuthCredentialPushPublishUtil.keyMatches(null, ENDPOINT_KEY));
        assertFalse(AuthCredentialPushPublishUtil.keyMatches(ENDPOINT_KEY, null));
    }
}
