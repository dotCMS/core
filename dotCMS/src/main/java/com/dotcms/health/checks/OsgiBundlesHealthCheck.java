package com.dotcms.health.checks;

import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.util.HealthCheckBase;
import com.dotmarketing.util.Config;
import org.apache.felix.framework.OSGIUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Readiness probe that fails if any installed OSGi plugin has not reached
 * {@link Bundle#ACTIVE} state after a configurable grace period. Blocks new
 * deployments from receiving traffic when a previously-working plugin has
 * regressed in the new image.
 *
 * <p>OSGi bundles install asynchronously, so this check honors a grace window
 * (default 5 minutes) that begins when {@code OSGIUtil.initializeFramework()}
 * completes. During the grace window the check reports {@code UP} so the
 * probe does not flap while bundles are still starting.</p>
 *
 * <p>Not registered as a liveness check: a failed plugin should block the
 * rollout, not restart-loop the pod.</p>
 *
 * Configuration (env-var form in parentheses):
 * <ul>
 *   <li>{@code health.check.osgi-bundles.mode} ({@code DOT_HEALTH_CHECK_OSGI_BUNDLES_MODE}) —
 *       PRODUCTION (default), MONITOR_MODE, DISABLED</li>
 *   <li>{@code health.check.osgi-bundles.grace.period.ms}
 *       ({@code DOT_HEALTH_CHECK_OSGI_BUNDLES_GRACE_PERIOD_MS}) — grace window in ms (default 300000)</li>
 *   <li>{@code health.check.osgi-bundles.required.bundles}
 *       ({@code DOT_HEALTH_CHECK_OSGI_BUNDLES_REQUIRED_BUNDLES}) — optional CSV of symbolic names that
 *       MUST be present and ACTIVE; if empty, every non-system, non-fragment bundle is required</li>
 * </ul>
 */
public class OsgiBundlesHealthCheck extends HealthCheckBase {

    private static final String CHECK_NAME = "osgi-bundles";
    private static final long DEFAULT_GRACE_PERIOD_MS = 300_000L; // 5 minutes

    @Override
    public String getName() {
        return CHECK_NAME;
    }

    @Override
    protected CheckResult performCheck() {
        final long initAt = osgiInitCompletedAt();
        if (initAt == 0L) {
            return new CheckResult(true, 0L,
                    "OSGi framework not yet initialized; deferring bundle check");
        }

        final long gracePeriodMs = gracePeriodMs();
        final long elapsed = System.currentTimeMillis() - initAt;
        final boolean withinGrace = elapsed < gracePeriodMs;

        final Bundle[] bundles = bundles();
        if (bundles == null) {
            return new CheckResult(true, 0L,
                    "OSGi bundle list not yet available; deferring bundle check");
        }

        final Set<String> required = requiredBundleNames();
        final List<Bundle> candidates = candidates(bundles, required);
        final List<Bundle> notActive = candidates.stream()
                .filter(b -> b.getState() != Bundle.ACTIVE)
                .collect(Collectors.toList());

        if (!required.isEmpty()) {
            final Set<String> present = candidates.stream()
                    .map(Bundle::getSymbolicName)
                    .collect(Collectors.toSet());
            final List<String> missing = required.stream()
                    .filter(name -> !present.contains(name))
                    .sorted()
                    .collect(Collectors.toList());
            if (!missing.isEmpty() && !withinGrace) {
                return new CheckResult(false, 0L,
                        "Required OSGi bundles missing after " + elapsed + "ms: "
                                + String.join(", ", missing));
            }
        }

        if (notActive.isEmpty()) {
            return new CheckResult(true, 0L,
                    "All " + candidates.size() + " OSGi bundle(s) ACTIVE");
        }

        if (withinGrace) {
            return new CheckResult(true, 0L,
                    notActive.size() + " of " + candidates.size()
                            + " OSGi bundle(s) still starting (" + elapsed + "ms of "
                            + gracePeriodMs + "ms grace)");
        }

        return new CheckResult(false, 0L,
                "OSGi bundle(s) did not reach ACTIVE within " + gracePeriodMs + "ms: "
                        + describe(notActive));
    }

    private static String describe(final List<Bundle> bundles) {
        return bundles.stream()
                .map(b -> b.getSymbolicName() + "[" + stateName(b.getState()) + "]")
                .collect(Collectors.joining(", "));
    }

    private static String stateName(final int state) {
        switch (state) {
            case Bundle.UNINSTALLED: return "UNINSTALLED";
            case Bundle.INSTALLED:   return "INSTALLED";
            case Bundle.RESOLVED:    return "RESOLVED";
            case Bundle.STARTING:    return "STARTING";
            case Bundle.STOPPING:    return "STOPPING";
            case Bundle.ACTIVE:      return "ACTIVE";
            default: return "UNKNOWN(" + state + ")";
        }
    }

    /**
     * Plugin candidates: every non-system, non-fragment bundle. When a required-bundles
     * list is configured we narrow to just that set (intersected with non-fragment).
     */
    private static List<Bundle> candidates(final Bundle[] bundles, final Set<String> required) {
        final List<Bundle> out = new ArrayList<>();
        for (final Bundle bundle : bundles) {
            if (bundle.getBundleId() == 0L) {
                continue; // system bundle
            }
            if (isFragment(bundle)) {
                continue; // fragments never reach ACTIVE
            }
            if (!required.isEmpty() && !required.contains(bundle.getSymbolicName())) {
                continue;
            }
            out.add(bundle);
        }
        return out;
    }

    private static boolean isFragment(final Bundle bundle) {
        return bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
    }

    // Package-private hooks for unit tests; production code reads the real OSGIUtil.

    long osgiInitCompletedAt() {
        return OSGIUtil.getInstance().getOsgiInitCompletedAt();
    }

    Bundle[] bundles() {
        try {
            return OSGIUtil.getInstance().getBundles();
        } catch (final Exception e) {
            return null;
        }
    }

    long gracePeriodMs() {
        return Config.getLongProperty(
                "health.check.osgi-bundles.grace.period.ms", DEFAULT_GRACE_PERIOD_MS);
    }

    Set<String> requiredBundleNames() {
        final String csv = Config.getStringProperty(
                "health.check.osgi-bundles.required.bundles", "");
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isLivenessCheck() {
        return false;
    }

    @Override
    public boolean isReadinessCheck() {
        return getMode() != HealthCheckMode.DISABLED;
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public String getDescription() {
        return String.format(
                "Fails readiness if any installed OSGi plugin is not ACTIVE after the grace period (Mode: %s, grace: %dms)",
                getMode().name(), gracePeriodMs());
    }

    @Override
    protected Map<String, Object> buildStructuredData(final CheckResult result,
                                                      final HealthStatus originalStatus,
                                                      final HealthStatus finalStatus,
                                                      final HealthCheckMode mode) {
        final Map<String, Object> data = new HashMap<>();
        final long initAt = osgiInitCompletedAt();
        data.put("osgiInitCompleted", initAt != 0L);
        if (initAt != 0L) {
            data.put("elapsedSinceInitMs", System.currentTimeMillis() - initAt);
        }
        data.put("gracePeriodMs", gracePeriodMs());

        final Bundle[] bundles = bundles();
        if (bundles != null) {
            final Set<String> required = requiredBundleNames();
            final List<Bundle> candidates = candidates(bundles, required);
            data.put("totalBundles", candidates.size());
            final Map<String, Long> byState = candidates.stream()
                    .collect(Collectors.groupingBy(b -> stateName(b.getState()),
                            Collectors.counting()));
            data.put("bundlesByState", byState);

            final List<Map<String, Object>> failing = candidates.stream()
                    .filter(b -> b.getState() != Bundle.ACTIVE)
                    .map(b -> {
                        final Map<String, Object> m = new HashMap<>();
                        m.put("symbolicName", b.getSymbolicName());
                        m.put("state", stateName(b.getState()));
                        m.put("location", b.getLocation());
                        return m;
                    })
                    .collect(Collectors.toList());
            if (!failing.isEmpty()) {
                data.put("notActiveBundles", failing);
            }
            if (!required.isEmpty()) {
                data.put("requiredBundles", required);
            }
        }
        return data;
    }
}
