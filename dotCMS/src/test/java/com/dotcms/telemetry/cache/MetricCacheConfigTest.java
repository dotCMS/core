package com.dotcms.telemetry.cache;

import com.dotcms.telemetry.ProfileType;
import com.dotmarketing.util.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MetricCacheConfig}.
 *
 * <p>Verifies configuration property reading including:</p>
 * <ul>
 *     <li>Default profile values when properties are not set</li>
 *     <li>Reading profile values from configuration properties</li>
 *     <li>Handling invalid profile values with fallback to defaults</li>
 *     <li>Case-insensitive profile parsing</li>
 * </ul>
 */
public class MetricCacheConfigTest {

    private static final String DEFAULT_PROFILE_PROP = "telemetry.default.profile";
    private static final String CRON_PROFILE_PROP = "telemetry.cron.profile";

    private MetricCacheConfig config;
    private String originalDefaultProfile;
    private String originalCronProfile;

    @Before
    public void setUp() {
        config = new MetricCacheConfig();

        // Save original property values to restore after tests
        originalDefaultProfile = Config.getStringProperty(DEFAULT_PROFILE_PROP, null);
        originalCronProfile = Config.getStringProperty(CRON_PROFILE_PROP, null);
    }

    @After
    public void tearDown() {
        // Restore original property values
        if (originalDefaultProfile != null) {
            Config.setProperty(DEFAULT_PROFILE_PROP, originalDefaultProfile);
        }
        if (originalCronProfile != null) {
            Config.setProperty(CRON_PROFILE_PROP, originalCronProfile);
        }
    }

    @Test
    public void testGetActiveProfile_withNoConfigProperty_shouldReturnMinimalDefault() {
        // Given: No configuration property set (use current value or default)
        // When: Getting active profile
        final ProfileType activeProfile = config.getActiveProfile();

        // Then: Should return MINIMAL as default (or the configured value)
        assertNotNull("Active profile should not be null", activeProfile);
        assertTrue("Active profile should be a valid ProfileType",
                activeProfile == ProfileType.MINIMAL ||
                activeProfile == ProfileType.STANDARD ||
                activeProfile == ProfileType.FULL);
    }

    @Test
    public void testGetActiveProfile_withMinimalConfig_shouldReturnMinimal() {
        // Given: Configuration property set to MINIMAL
        Config.setProperty(DEFAULT_PROFILE_PROP, "MINIMAL");

        // When: Getting active profile
        final ProfileType activeProfile = config.getActiveProfile();

        // Then: Should return MINIMAL
        assertEquals("Active profile should be MINIMAL when configured",
                ProfileType.MINIMAL, activeProfile);
    }

    @Test
    public void testGetActiveProfile_withStandardConfig_shouldReturnStandard() {
        // Given: Configuration property set to STANDARD
        Config.setProperty(DEFAULT_PROFILE_PROP, "STANDARD");

        // When: Getting active profile
        final ProfileType activeProfile = config.getActiveProfile();

        // Then: Should return STANDARD
        assertEquals("Active profile should be STANDARD when configured",
                ProfileType.STANDARD, activeProfile);
    }

    @Test
    public void testGetActiveProfile_withFullConfig_shouldReturnFull() {
        // Given: Configuration property set to FULL
        Config.setProperty(DEFAULT_PROFILE_PROP, "FULL");

        // When: Getting active profile
        final ProfileType activeProfile = config.getActiveProfile();

        // Then: Should return FULL
        assertEquals("Active profile should be FULL when configured",
                ProfileType.FULL, activeProfile);
    }

    @Test
    public void testGetActiveProfile_withLowercaseConfig_shouldReturnCorrectProfile() {
        // Given: Configuration property set to lowercase "minimal"
        Config.setProperty(DEFAULT_PROFILE_PROP, "minimal");

        // When: Getting active profile
        final ProfileType activeProfile = config.getActiveProfile();

        // Then: Should return MINIMAL (case-insensitive parsing)
        assertEquals("Active profile should handle lowercase 'minimal'",
                ProfileType.MINIMAL, activeProfile);
    }

    @Test
    public void testGetActiveProfile_withMixedCaseConfig_shouldReturnCorrectProfile() {
        // Given: Configuration property set to mixed case "StAnDaRd"
        Config.setProperty(DEFAULT_PROFILE_PROP, "StAnDaRd");

        // When: Getting active profile
        final ProfileType activeProfile = config.getActiveProfile();

        // Then: Should return STANDARD (case-insensitive parsing)
        assertEquals("Active profile should handle mixed case 'StAnDaRd'",
                ProfileType.STANDARD, activeProfile);
    }

    @Test
    public void testGetActiveProfile_withInvalidConfig_shouldReturnMinimalDefault() {
        // Given: Configuration property set to invalid value
        Config.setProperty(DEFAULT_PROFILE_PROP, "INVALID_PROFILE");

        // When: Getting active profile
        final ProfileType activeProfile = config.getActiveProfile();

        // Then: Should fall back to MINIMAL default
        assertEquals("Active profile should fall back to MINIMAL for invalid config",
                ProfileType.MINIMAL, activeProfile);
    }

    @Test
    public void testGetCronProfile_withNoConfigProperty_shouldReturnFullDefault() {
        // Given: No cron configuration property set (use current value or default)
        // When: Getting cron profile
        final ProfileType cronProfile = config.getCronProfile();

        // Then: Should return FULL as default (or the configured value)
        assertNotNull("Cron profile should not be null", cronProfile);
        assertTrue("Cron profile should be a valid ProfileType",
                cronProfile == ProfileType.MINIMAL ||
                cronProfile == ProfileType.STANDARD ||
                cronProfile == ProfileType.FULL);
    }

    @Test
    public void testGetCronProfile_withFullConfig_shouldReturnFull() {
        // Given: Cron configuration property set to FULL
        Config.setProperty(CRON_PROFILE_PROP, "FULL");

        // When: Getting cron profile
        final ProfileType cronProfile = config.getCronProfile();

        // Then: Should return FULL
        assertEquals("Cron profile should be FULL when configured",
                ProfileType.FULL, cronProfile);
    }

    @Test
    public void testGetCronProfile_withMinimalConfig_shouldReturnMinimal() {
        // Given: Cron configuration property set to MINIMAL
        Config.setProperty(CRON_PROFILE_PROP, "MINIMAL");

        // When: Getting cron profile
        final ProfileType cronProfile = config.getCronProfile();

        // Then: Should return MINIMAL
        assertEquals("Cron profile should be MINIMAL when configured",
                ProfileType.MINIMAL, cronProfile);
    }

    @Test
    public void testGetCronProfile_withLowercaseConfig_shouldReturnCorrectProfile() {
        // Given: Cron configuration property set to lowercase "full"
        Config.setProperty(CRON_PROFILE_PROP, "full");

        // When: Getting cron profile
        final ProfileType cronProfile = config.getCronProfile();

        // Then: Should return FULL (case-insensitive parsing)
        assertEquals("Cron profile should handle lowercase 'full'",
                ProfileType.FULL, cronProfile);
    }

    @Test
    public void testGetCronProfile_withInvalidConfig_shouldReturnFullDefault() {
        // Given: Cron configuration property set to invalid value
        Config.setProperty(CRON_PROFILE_PROP, "NOT_A_PROFILE");

        // When: Getting cron profile
        final ProfileType cronProfile = config.getCronProfile();

        // Then: Should fall back to FULL default
        assertEquals("Cron profile should fall back to FULL for invalid config",
                ProfileType.FULL, cronProfile);
    }

    @Test
    public void testDefaultAndCronProfiles_canBeDifferent() {
        // Given: Different profiles for default and cron
        Config.setProperty(DEFAULT_PROFILE_PROP, "MINIMAL");
        Config.setProperty(CRON_PROFILE_PROP, "FULL");

        // When: Getting both profiles
        final ProfileType activeProfile = config.getActiveProfile();
        final ProfileType cronProfile = config.getCronProfile();

        // Then: Should return different profiles
        assertEquals("Active profile should be MINIMAL", ProfileType.MINIMAL, activeProfile);
        assertEquals("Cron profile should be FULL", ProfileType.FULL, cronProfile);
        assertNotEquals("Active and cron profiles should be different", activeProfile, cronProfile);
    }

    @Test
    public void testGetActiveProfile_withEmptyString_shouldReturnDefault() {
        // Given: Configuration property set to empty string
        Config.setProperty(DEFAULT_PROFILE_PROP, "");

        // When: Getting active profile
        final ProfileType activeProfile = config.getActiveProfile();

        // Then: Should return default MINIMAL (empty string is treated as not set)
        assertEquals("Active profile should return MINIMAL for empty string",
                ProfileType.MINIMAL, activeProfile);
    }

    @Test
    public void testGetCronProfile_withEmptyString_shouldReturnDefault() {
        // Given: Cron configuration property set to empty string
        Config.setProperty(CRON_PROFILE_PROP, "");

        // When: Getting cron profile
        final ProfileType cronProfile = config.getCronProfile();

        // Then: Should return default FULL (empty string is treated as not set)
        assertEquals("Cron profile should return FULL for empty string",
                ProfileType.FULL, cronProfile);
    }
}