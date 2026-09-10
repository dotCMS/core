package com.dotmarketing.business;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

/**
 * Unit tests for the shipped {@code cache.userdotcmscache.size} default (issue #37186, FR-002).
 *
 * <p>These read {@code dotmarketing-config.properties} directly as a resource, rather than going
 * through {@link com.dotmarketing.util.Config}, because {@code Config} requires the full
 * dotCMS startup context to resolve properties — asserting on the raw shipped file is enough to
 * pin the default without that dependency, and is exactly what the property lookup falls back to
 * at runtime.</p>
 */
public class UserCacheConfigTest {

    private static Properties loadDotmarketingConfigProperties() throws IOException {
        final Properties props = new Properties();
        try (InputStream in = UserCacheConfigTest.class.getClassLoader()
                .getResourceAsStream("dotmarketing-config.properties")) {
            assertNotNull("dotmarketing-config.properties must be on the test classpath", in);
            props.load(in);
        }
        return props;
    }

    /**
     * The user cache must no longer fall back to the shared cache.default.size=1000.
     *
     * <p>Per review feedback, this intentionally does not pin the literal "4000": that would
     * break the moment someone legitimately re-tunes the value. What actually matters is that
     * the key is present and set well above the shared default, so it asserts a lower bound
     * instead.</p>
     */
    @Test
    public void userCacheSize_isExplicitlySetAboveSharedDefault() throws IOException {
        final Properties props = loadDotmarketingConfigProperties();
        final String rawValue = props.getProperty("cache.userdotcmscache.size");
        assertNotNull("cache.userdotcmscache.size must be explicitly set", rawValue);

        final int value = Integer.parseInt(rawValue);
        assertTrue("cache.userdotcmscache.size must be greater than the shared cache.default.size=1000, was " + value,
                value > 1000);
    }

    /**
     * cache.useremaildotcmscache.size (line 519) is a dead region: UserCacheImpl#add writes it
     * keyed by the raw email address, but UserCacheImpl#get reads it keyed by the
     * primary-group-prefixed id, so entries are never read back. Raising it alongside line 518
     * would reserve memory for a region nothing can use — it must stay unset.
     *
     * <p>Note: if the key-mismatch bug described above is ever fixed so that the email region
     * becomes usable, this assertion will need to be revisited — an unset value would then be a
     * real regression rather than the expected state.</p>
     */
    @Test
    public void userEmailCacheSize_remainsUnsetDeadRegionNotRaised() throws IOException {
        final Properties props = loadDotmarketingConfigProperties();
        assertNull("cache.useremaildotcmscache.size must stay unset (dead region, key mismatch bug)",
                props.getProperty("cache.useremaildotcmscache.size"));
    }
}
