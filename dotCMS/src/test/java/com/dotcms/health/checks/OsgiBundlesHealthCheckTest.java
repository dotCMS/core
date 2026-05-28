package com.dotcms.health.checks;

import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OsgiBundlesHealthCheckTest {

    private static final long GRACE_MS = 60_000L;

    @Test
    public void preInit_reportsUp_withDeferredMessage() {
        final TestableCheck check = new TestableCheck()
                .withInitAt(0L)
                .withBundles(new Bundle[] { bundle("plugin-a", Bundle.ACTIVE, false) });

        final HealthCheckResult result = check.check();

        assertEquals(HealthStatus.UP, result.status());
        assertTrue(result.message().orElse(""),result.message().orElse("").contains("not yet initialized"));
    }

    @Test
    public void withinGrace_bundleNotActive_reportsUp() {
        final long now = System.currentTimeMillis();
        final TestableCheck check = new TestableCheck()
                .withInitAt(now - 1_000L) // 1s elapsed; well inside 60s grace
                .withBundles(new Bundle[] {
                        systemBundle(),
                        bundle("plugin-a", Bundle.STARTING, false)
                });

        final HealthCheckResult result = check.check();

        assertEquals(HealthStatus.UP, result.status());
        assertTrue(result.message().orElse(""),result.message().orElse("").contains("still starting"));
    }

    @Test
    public void afterGrace_bundleNotActive_reportsDown() {
        final long now = System.currentTimeMillis();
        final TestableCheck check = new TestableCheck()
                .withInitAt(now - (GRACE_MS + 5_000L)) // past grace
                .withBundles(new Bundle[] {
                        systemBundle(),
                        bundle("plugin-a", Bundle.ACTIVE, false),
                        bundle("plugin-b", Bundle.RESOLVED, false)
                });

        final HealthCheckResult result = check.check();

        assertEquals(HealthStatus.DOWN, result.status());
        assertTrue(result.message().orElse(""),result.message().orElse("").contains("plugin-b"));
        assertTrue(result.message().orElse(""),result.message().orElse("").contains("RESOLVED"));
    }

    @Test
    public void afterGrace_allActive_reportsUp() {
        final long now = System.currentTimeMillis();
        final TestableCheck check = new TestableCheck()
                .withInitAt(now - (GRACE_MS + 5_000L))
                .withBundles(new Bundle[] {
                        systemBundle(),
                        bundle("plugin-a", Bundle.ACTIVE, false),
                        bundle("plugin-b", Bundle.ACTIVE, false)
                });

        final HealthCheckResult result = check.check();

        assertEquals(HealthStatus.UP, result.status());
        assertTrue(result.message().orElse(""),result.message().orElse("").contains("ACTIVE"));
    }

    @Test
    public void fragmentBundle_isIgnored_evenWhenResolved() {
        final long now = System.currentTimeMillis();
        final TestableCheck check = new TestableCheck()
                .withInitAt(now - (GRACE_MS + 5_000L))
                .withBundles(new Bundle[] {
                        systemBundle(),
                        bundle("plugin-a", Bundle.ACTIVE, false),
                        bundle("plugin-fragment", Bundle.RESOLVED, true) // fragments never go ACTIVE
                });

        final HealthCheckResult result = check.check();

        assertEquals(HealthStatus.UP, result.status());
    }

    @Test
    public void requiredBundles_missing_afterGrace_reportsDown() {
        final long now = System.currentTimeMillis();
        final TestableCheck check = new TestableCheck()
                .withInitAt(now - (GRACE_MS + 5_000L))
                .withRequired(Collections.singleton("plugin-required"))
                .withBundles(new Bundle[] {
                        systemBundle(),
                        bundle("plugin-a", Bundle.ACTIVE, false)
                });

        final HealthCheckResult result = check.check();

        assertEquals(HealthStatus.DOWN, result.status());
        assertTrue(result.message().orElse(""),result.message().orElse("").contains("plugin-required"));
        assertTrue(result.message().orElse(""),result.message().orElse("").contains("missing"));
    }

    @Test
    public void name_isStable() {
        assertEquals("osgi-bundles", new OsgiBundlesHealthCheck().getName());
    }

    @Test
    public void readiness_yes_liveness_no() {
        final OsgiBundlesHealthCheck check = new OsgiBundlesHealthCheck();
        assertTrue(check.isReadinessCheck());
        assertEquals(false, check.isLivenessCheck());
    }

    @Test
    public void description_includesGracePeriod() {
        final String description = new TestableCheck().getDescription();
        assertNotNull(description);
        assertTrue(description, description.contains("grace"));
    }

    private static Bundle systemBundle() {
        final Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(0L);
        when(b.getSymbolicName()).thenReturn("org.apache.felix.framework");
        when(b.getState()).thenReturn(Bundle.ACTIVE);
        when(b.getHeaders()).thenReturn(new Hashtable<>());
        return b;
    }

    private static Bundle bundle(final String symbolicName, final int state, final boolean fragment) {
        final Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(1L);
        when(b.getSymbolicName()).thenReturn(symbolicName);
        when(b.getState()).thenReturn(state);
        when(b.getLocation()).thenReturn("file:" + symbolicName + ".jar");
        final Hashtable<String, String> headers = new Hashtable<>();
        if (fragment) {
            headers.put(Constants.FRAGMENT_HOST, "host.bundle");
        }
        when(b.getHeaders()).thenReturn(headers);
        return b;
    }

    private static final class TestableCheck extends OsgiBundlesHealthCheck {
        private long initAt;
        private Bundle[] bundles = new Bundle[0];
        private Set<String> required = Collections.emptySet();

        TestableCheck withInitAt(final long ts) {
            this.initAt = ts;
            return this;
        }

        TestableCheck withBundles(final Bundle[] bundles) {
            this.bundles = bundles;
            return this;
        }

        TestableCheck withRequired(final Set<String> required) {
            this.required = required;
            return this;
        }

        @Override
        long osgiInitCompletedAt() {
            return initAt;
        }

        @Override
        Bundle[] bundles() {
            return bundles;
        }

        @Override
        long gracePeriodMs() {
            return GRACE_MS;
        }

        @Override
        Set<String> requiredBundleNames() {
            return required;
        }
    }
}
