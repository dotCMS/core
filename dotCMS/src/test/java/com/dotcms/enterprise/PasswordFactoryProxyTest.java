package com.dotcms.enterprise;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import com.dotcms.UnitTestBase;
import org.junit.Test;

/**
 * Unit tests for {@link PasswordFactoryProxy}.
 */
public class PasswordFactoryProxyTest extends UnitTestBase {

    /**
     * Method to test: {@link PasswordFactoryProxy#generateRandomHash()}
     * Given: two calls in a row
     * Then:  each returns a secure hash, and the two hashes are different
     */
    @Test
    public void test_generateRandomHash_returnsDistinctSecureHashes() throws Exception {
        final String first = PasswordFactoryProxy.generateRandomHash();
        final String second = PasswordFactoryProxy.generateRandomHash();

        assertFalse(PasswordFactoryProxy.isUnsecurePasswordHash(first));
        assertFalse(PasswordFactoryProxy.isUnsecurePasswordHash(second));
        assertNotEquals(first, second);
    }
}
