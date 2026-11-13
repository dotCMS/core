package com.dotcms.rest.api.v1.system;

import com.dotcms.config.SystemTableConfigSource;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the keys {@link SystemTableResource} refuses to read or delete.
 */
class SystemTableResourceTest {

    /**
     * The default list holds the cluster keys and nothing else: a provisioning key is not hidden
     * from reads and deletes, so a row stored before the write rule can still be seen and removed,
     * and other keys stay accepted.
     */
    @Test
    void testRefusedKeys_WhenKeyIsOnTheDefaultList_ShouldRefuseOnlyTheClusterKeys() {
        final Predicate<String> refused = SystemTableResource.refusedKeys();

        assertTrue(refused.test("DOTCMS_CLUSTER_SALT"));
        assertTrue(refused.test("DOTCMS_CLUSTER_ID"));
        for (final String key : SystemTableConfigSource.PROVISIONING_KEYS) {
            assertFalse(refused.test(key), key);
        }
        for (final String key : new String[]{"MY_KEY", "REMOTE_CALL_TIMEOUT", "REMOTE_CALL_SUBNET_BLACKLIST_NOTES"}) {
            assertFalse(refused.test(key), key);
        }
    }
}
