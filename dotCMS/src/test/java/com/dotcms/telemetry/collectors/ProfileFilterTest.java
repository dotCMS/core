package com.dotcms.telemetry.collectors;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ProfileFilter}.
 *
 * <p>Verifies profile matching logic including:</p>
 * <ul>
 *     <li>Metrics with matching profiles are included</li>
 *     <li>Metrics without matching profiles are excluded</li>
 *     <li>Metrics without {@code @MetricsProfile} annotation are excluded</li>
 *     <li>CDI proxy classes are correctly unwrapped</li>
 * </ul>
 */
@RunWith(MockitoJUnitRunner.class)
public class ProfileFilterTest {

    /**
     * Test metric with MINIMAL profile annotation.
     */
    @MetricsProfile({ProfileType.MINIMAL})
    static class MinimalOnlyMetric implements MetricType {
        @Override
        public String getName() {
            return "TEST_MINIMAL";
        }

        @Override
        public String getDescription() {
            return "Test metric with MINIMAL profile";
        }

        @Override
        public MetricCategory getCategory() {
            return MetricCategory.DIFFERENTIATING_FEATURES;
        }

        @Override
        public MetricFeature getFeature() {
            return MetricFeature.CONTENTLETS;
        }

        @Override
        public Optional<Object> getValue() {
            return Optional.of(100);
        }
    }

    /**
     * Test metric with multiple profile annotations.
     */
    @MetricsProfile({ProfileType.MINIMAL, ProfileType.STANDARD, ProfileType.FULL})
    static class AllProfilesMetric implements MetricType {
        @Override
        public String getName() {
            return "TEST_ALL_PROFILES";
        }

        @Override
        public String getDescription() {
            return "Test metric with all profiles";
        }

        @Override
        public MetricCategory getCategory() {
            return MetricCategory.DIFFERENTIATING_FEATURES;
        }

        @Override
        public MetricFeature getFeature() {
            return MetricFeature.CONTENTLETS;
        }

        @Override
        public Optional<Object> getValue() {
            return Optional.of(200);
        }
    }

    /**
     * Test metric with FULL profile only.
     */
    @MetricsProfile({ProfileType.FULL})
    static class FullOnlyMetric implements MetricType {
        @Override
        public String getName() {
            return "TEST_FULL_ONLY";
        }

        @Override
        public String getDescription() {
            return "Test metric with FULL profile only";
        }

        @Override
        public MetricCategory getCategory() {
            return MetricCategory.DIFFERENTIATING_FEATURES;
        }

        @Override
        public MetricFeature getFeature() {
            return MetricFeature.CONTENTLETS;
        }

        @Override
        public Optional<Object> getValue() {
            return Optional.of(300);
        }
    }

    /**
     * Test metric WITHOUT @MetricsProfile annotation (should be excluded).
     */
    static class NoAnnotationMetric implements MetricType {
        @Override
        public String getName() {
            return "TEST_NO_ANNOTATION";
        }

        @Override
        public String getDescription() {
            return "Test metric without profile annotation";
        }

        @Override
        public MetricCategory getCategory() {
            return MetricCategory.DIFFERENTIATING_FEATURES;
        }

        @Override
        public MetricFeature getFeature() {
            return MetricFeature.CONTENTLETS;
        }

        @Override
        public Optional<Object> getValue() {
            return Optional.of(400);
        }
    }

    /**
     * Simulated CDI proxy class extending a real metric.
     */
    static class MinimalOnlyMetric$Proxy extends MinimalOnlyMetric {
        // CDI proxies extend the real class
    }

    @Test
    public void testMatchesWithMinimalProfile_shouldReturnTrueForMinimalMetric() {
        // Given: A metric annotated with MINIMAL profile
        final MetricType metric = new MinimalOnlyMetric();

        // When: Checking if it matches MINIMAL profile
        final boolean matches = ProfileFilter.matches(metric, ProfileType.MINIMAL);

        // Then: Should match
        assertTrue("Metric with MINIMAL profile should match MINIMAL profile", matches);
    }

    @Test
    public void testMatchesWithFullProfile_shouldReturnFalseForMinimalOnlyMetric() {
        // Given: A metric annotated with MINIMAL profile only
        final MetricType metric = new MinimalOnlyMetric();

        // When: Checking if it matches FULL profile
        final boolean matches = ProfileFilter.matches(metric, ProfileType.FULL);

        // Then: Should not match
        assertFalse("Metric with only MINIMAL profile should not match FULL profile", matches);
    }

    @Test
    public void testMatchesWithAllProfiles_shouldReturnTrueForAnyProfile() {
        // Given: A metric annotated with all profiles
        final MetricType metric = new AllProfilesMetric();

        // When/Then: Should match all profile types
        assertTrue("Metric with all profiles should match MINIMAL",
                ProfileFilter.matches(metric, ProfileType.MINIMAL));
        assertTrue("Metric with all profiles should match STANDARD",
                ProfileFilter.matches(metric, ProfileType.STANDARD));
        assertTrue("Metric with all profiles should match FULL",
                ProfileFilter.matches(metric, ProfileType.FULL));
    }

    @Test
    public void testMatchesWithFullOnlyMetric_shouldOnlyMatchFullProfile() {
        // Given: A metric annotated with FULL profile only
        final MetricType metric = new FullOnlyMetric();

        // When/Then: Should only match FULL profile
        assertFalse("Metric with only FULL profile should not match MINIMAL",
                ProfileFilter.matches(metric, ProfileType.MINIMAL));
        assertFalse("Metric with only FULL profile should not match STANDARD",
                ProfileFilter.matches(metric, ProfileType.STANDARD));
        assertTrue("Metric with only FULL profile should match FULL",
                ProfileFilter.matches(metric, ProfileType.FULL));
    }

    @Test
    public void testMatchesWithNoAnnotation_shouldReturnFalse() {
        // Given: A metric without @MetricsProfile annotation
        final MetricType metric = new NoAnnotationMetric();

        // When: Checking if it matches any profile
        final boolean matchesMinimal = ProfileFilter.matches(metric, ProfileType.MINIMAL);
        final boolean matchesFull = ProfileFilter.matches(metric, ProfileType.FULL);

        // Then: Should not match any profile
        assertFalse("Metric without @MetricsProfile should not match MINIMAL", matchesMinimal);
        assertFalse("Metric without @MetricsProfile should not match FULL", matchesFull);
    }

    @Test
    public void testMatchesWithCDIProxy_shouldUnwrapAndCheckAnnotation() {
        // Given: A CDI proxy instance extending a metric with MINIMAL profile
        final MetricType proxyMetric = new MinimalOnlyMetric$Proxy();

        // When: Checking if the proxy matches MINIMAL profile
        final boolean matches = ProfileFilter.matches(proxyMetric, ProfileType.MINIMAL);

        // Then: Should correctly unwrap proxy and find annotation on superclass
        assertTrue("CDI proxy should be unwrapped to find @MetricsProfile on superclass", matches);
    }

    @Test
    public void testMatchesWithCDIProxy_shouldNotMatchWrongProfile() {
        // Given: A CDI proxy instance extending a metric with MINIMAL profile
        final MetricType proxyMetric = new MinimalOnlyMetric$Proxy();

        // When: Checking if the proxy matches FULL profile
        final boolean matches = ProfileFilter.matches(proxyMetric, ProfileType.FULL);

        // Then: Should correctly unwrap proxy and verify it doesn't match FULL
        assertFalse("CDI proxy with MINIMAL profile should not match FULL profile", matches);
    }

    @Test
    public void testMatchesWithStandardProfile_shouldWorkCorrectly() {
        // Given: A metric annotated with all profiles
        final MetricType metric = new AllProfilesMetric();

        // When: Checking if it matches STANDARD profile
        final boolean matches = ProfileFilter.matches(metric, ProfileType.STANDARD);

        // Then: Should match
        assertTrue("Metric with STANDARD profile should match STANDARD profile", matches);
    }
}