package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

    /** The user cache must no longer fall back to the shared cache.default.size=1000. */
    @Test
    public void userCacheSize_isExplicitlySetTo4000() throws IOException {
        final Properties props = loadDotmarketingConfigProperties();
        assertEquals("4000", props.getProperty("cache.userdotcmscache.size"));
    }

    /**
     * cache.useremaildotcmscache.size (line 519) is a dead region: UserCacheImpl#add writes it
     * keyed by the raw email address, but UserCacheImpl#get reads it keyed by the
     * primary-group-prefixed id, so entries are never read back. Raising it alongside line 518
     * would reserve memory for a region nothing can use — it must stay unset.
     */
    @Test
    public void userEmailCacheSize_remainsUnsetDeadRegionNotRaised() throws IOException {
        final Properties props = loadDotmarketingConfigProperties();
        assertNull("cache.useremaildotcmscache.size must stay unset (dead region, key mismatch bug)",
                props.getProperty("cache.useremaildotcmscache.size"));
    }
}
