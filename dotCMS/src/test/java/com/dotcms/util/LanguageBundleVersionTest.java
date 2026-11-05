package com.dotcms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import com.liferay.portal.util.ReleaseInfo;
import java.util.Date;
import org.junit.Test;
import org.mockito.MockedStatic;

/** Unit tests for the build identity used to version language bundles. */
public class LanguageBundleVersionTest {

    /** Identical build inputs always produce the same SHA-256 token. */
    @Test
    public void sameBuildInputsProduceStableVersion() {
        final String version = LanguageBundleVersion.compute("26.01", "abc123", 1700000000000L);

        assertEquals(version, LanguageBundleVersion.compute("26.01", "abc123", 1700000000000L));
        assertTrue(version.matches("[0-9a-f]{64}"));
    }

    /** Every component of the build identity participates in invalidation. */
    @Test
    public void changingAnyBuildInputChangesVersion() {
        final String version = LanguageBundleVersion.compute("26.01", "abc123", 1700000000000L);

        assertNotEquals(version, LanguageBundleVersion.compute("26.02", "abc123", 1700000000000L));
        assertNotEquals(version, LanguageBundleVersion.compute("26.01", "def456", 1700000000000L));
        assertNotEquals(version, LanguageBundleVersion.compute("26.01", "abc123", 1700000000001L));
    }

    /** Component boundaries cannot alias even when inputs contain separators. */
    @Test
    public void buildInputBoundariesAreUnambiguous() {
        assertNotEquals(LanguageBundleVersion.compute("a:b", "c", 1L),
                LanguageBundleVersion.compute("a", "b:c", 1L));
    }

    /** The release timestamp is used directly, never its timezone-dependent display string. */
    @Test
    public void currentVersionUsesReleaseBuildEpoch() {
        try (MockedStatic<ReleaseInfo> release = mockStatic(ReleaseInfo.class)) {
            release.when(ReleaseInfo::getVersion).thenReturn("26.01");
            release.when(ReleaseInfo::getBuildNumber).thenReturn("abc123");
            release.when(ReleaseInfo::getBuildDate).thenReturn(new Date(1700000000000L));

            final String expected = LanguageBundleVersion.compute("26.01", "abc123", 1700000000000L);
            assertEquals(expected, LanguageBundleVersion.get());
            assertEquals(expected, LanguageBundleVersion.get());
            release.verify(ReleaseInfo::getBuildDateString, never());
        }
    }
}
