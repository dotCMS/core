package com.dotcms.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the keys {@link SystemTableConfigSource} serves as configuration.
 */
class SystemTableConfigSourceTest {

    /** The expected provisioning keys, written out so a change to the set is a deliberate change here too. */
    static final List<String> PROVISIONING_KEYS = List.of(
            "REMOTE_CALL_SUBNET_BLACKLIST", "REMOTE_CALL_SUBNET_ALLOWLIST", "ALLOW_ACCESS_TO_PRIVATE_SUBNETS",
            "REMOTE_CALL_ALLOW_REDIRECTS", "REMOTE_CALL_ALLOW_IPV6", "TEMP_RESOURCE_BY_URL_ADMIN_ONLY",
            "TEMP_RESOURCE_BY_URL_ALLOWED_EXTENSIONS");

    /**
     * The provisioning key set holds exactly the expected keys.
     */
    @Test
    void testProvisioningKeys_ShouldHoldTheExpectedKeys() {
        assertEquals(Set.copyOf(PROVISIONING_KEYS), SystemTableConfigSource.PROVISIONING_KEYS);
    }

    /**
     * A provisioning key is recognised however it is spelled: letter case, surrounding spaces and
     * the {@code DOT_} prefix make no difference.
     */
    @Test
    void testIsProvisioningKey_WhenKeyIsAProvisioningKey_ShouldReturnTrue() {
        for (final String key : PROVISIONING_KEYS) {
            for (final String spelling : new String[]{key, key.toLowerCase(), "DOT_" + key, "dot_" + key.toLowerCase(), " " + key + " "}) {
                assertTrue(SystemTableConfigSource.isProvisioningKey(spelling), spelling);
            }
        }
        assertTrue(SystemTableConfigSource.isProvisioningKey("remote_call_allow_ipv6"));
    }

    /**
     * The check matches whole keys: unrelated keys, keys that merely share a prefix, and missing
     * keys are not provisioning keys.
     */
    @Test
    void testIsProvisioningKey_WhenKeyIsNotAProvisioningKey_ShouldReturnFalse() {
        for (final String key : new String[]{"SOME_OTHER_KEY", "REMOTE_CALL_TIMEOUT", "REMOTE_CALL_SUBNET_BLACKLIST_NOTES",
                "TEMP_RESOURCE_MAX_FILE_SIZE", "DOT_SOME_OTHER_KEY", "concurrent.pool", null, ""}) {
            assertFalse(SystemTableConfigSource.isProvisioningKey(key), String.valueOf(key));
        }
    }

    /**
     * Provisioning keys are never served from the table, however they are spelled.
     */
    @Test
    void testIsServedFromSystemTable_WhenKeyIsAProvisioningKey_ShouldReturnFalse() {
        for (final String key : PROVISIONING_KEYS) {
            for (final String spelling : new String[]{key, key.toLowerCase(), "DOT_" + key, "dot_" + key.toLowerCase(), " " + key + " "}) {
                assertFalse(SystemTableConfigSource.isServedFromSystemTable(spelling), spelling);
            }
        }
    }

    /**
     * The exclusion matches whole keys: unrelated keys, and keys that merely share a prefix, keep
     * being served.
     */
    @Test
    void testIsServedFromSystemTable_WhenKeyIsNotAProvisioningKey_ShouldReturnTrue() {
        for (final String key : new String[]{"SOME_OTHER_KEY", "REMOTE_CALL_TIMEOUT", "REMOTE_CALL_SUBNET_BLACKLIST_NOTES",
                "TEMP_RESOURCE_MAX_FILE_SIZE", "DOT_SOME_OTHER_KEY"}) {
            assertTrue(SystemTableConfigSource.isServedFromSystemTable(key), key);
        }
    }

    /**
     * The existing exclusions keep working, and a missing key is not served.
     */
    @Test
    void testIsServedFromSystemTable_WhenKeyMatchesExistingSkipList_ShouldReturnFalse() {
        for (final String key : new String[]{"concurrent.pool", "scheduler.x", "DOT_CONCURRENT_POOL", null, ""}) {
            assertFalse(SystemTableConfigSource.isServedFromSystemTable(key), String.valueOf(key));
        }
    }

    /**
     * Reading a provisioning key through the source answers nothing, without touching the table,
     * so the environment or the properties file is the only place its value comes from. One
     * spelling is enough here: the others are covered on {@code isProvisioningKey}, and a key
     * that slipped through would reach the real table, which no unit test can open.
     */
    @Test
    void testGetValue_WhenKeyIsAProvisioningKey_ShouldReturnNull() {
        assertNull(new SystemTableConfigSource().getValue("REMOTE_CALL_ALLOW_IPV6"));
    }
}
