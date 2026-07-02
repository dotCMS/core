package com.dotcms.health.checks.cdi;

import com.dotcms.health.util.HealthCheckBase;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.util.Logger;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import javax.enterprise.context.ApplicationScoped;
import java.io.StringWriter;

/**
 * CDI-based health check that verifies the Velocity engine's global macro
 * library is loaded and resolvable.
 *
 * <p>Background: {@code VelocimacroFactory} has historically been able to silently
 * swallow a {@code ResourceNotFoundException} at engine init, leaving
 * {@code #renderMarks} and {@code #editContentlet} unregistered while
 * {@code engine.init()} still returns successfully. In that state every public
 * page renders the macro source as literal text. See spike #35329 and the
 * fail-loud companion fix (#35601).
 *
 * <p>The probe evaluates {@value #PROBE_TEMPLATE} through
 * {@link VelocityUtil#getEngine()}. When the macro is registered the rendered
 * output is whatever the macro body produces; when it is not, the engine
 * renders the directive source verbatim, which we detect via the literal
 * {@value #LITERAL_MARKER} marker.
 *
 * <p>Excluded from liveness probes: a missing macro library is a one-time
 * startup concern that should remove the pod from the load balancer, not
 * trigger a restart loop.
 *
 * <p>Configuration:
 * <ul>
 *   <li>{@code health.check.velocity.mode} — PRODUCTION (default), MONITOR_MODE, DISABLED</li>
 * </ul>
 */
@ApplicationScoped
public class VelocityHealthCheck extends HealthCheckBase {

    private static final String PROBE_TEMPLATE = "#renderMarks($null)";
    private static final String LITERAL_MARKER = "#renderMarks(";
    private static final String PROBE_LOG_TAG = "VelocityHealthCheck:probe";

    @Override
    public String getName() {
        return "velocity";
    }

    @Override
    public int getOrder() {
        // Runs after database (default 100), cache (30), and elasticsearch (40).
        return 110;
    }

    @Override
    public boolean isLivenessCheck() {
        return false;
    }

    @Override
    public boolean isReadinessCheck() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Verifies the Velocity global macro library is registered "
                + "(probes #renderMarks resolution)";
    }

    @Override
    protected CheckResult performCheck() throws Exception {
        if (isShutdownInProgress()) {
            Logger.debug(this, "Skipping Velocity probe during shutdown");
            return new CheckResult(false, 0L,
                    "Velocity health check skipped during shutdown");
        }

        // VelocityUtil.getEngine() triggers a lazy init that touches the DB/company context.
        // On a fresh install this fires before Task00001LoadSchema runs, throwing "No Company!".
        // Defer until InitServlet has set dotcms.started.up=true.
        if (!"true".equals(System.getProperty("dotcms.started.up"))) {
            return new CheckResult(false, 0L,
                    "Velocity probe deferred: dotCMS startup not yet complete");
        }

        return measureExecution(() -> {
            final VelocityEngine engine = VelocityUtil.getEngine();
            final StringWriter writer = new StringWriter();
            engine.evaluate(new VelocityContext(), writer, PROBE_LOG_TAG, PROBE_TEMPLATE);
            final String rendered = writer.toString();
            if (rendered.contains(LITERAL_MARKER)) {
                throw new IllegalStateException(
                        "Velocity global macro library not registered: "
                        + "probe template rendered as literal text. See issues #35329 / #35601.");
            }
            return "Velocity macro library registered (#renderMarks resolved)";
        });
    }
}
