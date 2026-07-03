package com.dotcms.health.checks.cdi;

import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Unit tests for {@link VelocityHealthCheck}. Exercises the probe via a mocked
 * {@link VelocityEngine}; the engine's {@code evaluate} call writes a controlled
 * output to the supplied Writer to simulate either a registered macro
 * (any non-literal output) or an unregistered macro (literal directive text).
 */
public class VelocityHealthCheckTest {

    private static final String STARTED_UP_PROPERTY = "dotcms.started.up";

    private MockedStatic<VelocityUtil> velocityUtilMock;
    private String previousStartedUpProperty;

    @Before
    public void setUp() {
        // The probe defers until dotCMS startup completes, which InitServlet signals via this
        // property in production. Set it here so the probe logic under test actually runs
        // instead of short-circuiting on every test.
        previousStartedUpProperty = System.getProperty(STARTED_UP_PROPERTY);
        System.setProperty(STARTED_UP_PROPERTY, "true");
        velocityUtilMock = mockStatic(VelocityUtil.class);
    }

    @After
    public void tearDown() {
        velocityUtilMock.close();
        if (previousStartedUpProperty == null) {
            System.clearProperty(STARTED_UP_PROPERTY);
        } else {
            System.setProperty(STARTED_UP_PROPERTY, previousStartedUpProperty);
        }
    }

    @Test
    public void returnsUpWhenRenderMarksResolves() {
        stubEngineToWrite("<span class=\"editor-marks\"></span>");

        final HealthCheckResult result = new VelocityHealthCheck().check();

        assertEquals(HealthStatus.UP, result.status());
    }

    @Test
    public void returnsDownWhenStartupNotComplete() {
        // Even a healthy-looking engine shouldn't matter: the probe must defer before touching
        // VelocityUtil.getEngine() at all until InitServlet marks startup complete.
        System.clearProperty(STARTED_UP_PROPERTY);
        stubEngineToWrite("<span class=\"editor-marks\"></span>");

        final HealthCheckResult result = new VelocityHealthCheck().check();

        assertEquals(HealthStatus.DOWN, result.status());
    }

    @Test
    public void returnsDownWhenRenderMarksRendersLiterally() {
        // When the global macro library failed to load, Velocity renders the
        // directive source verbatim — the failure signature this check detects.
        stubEngineToWrite("#renderMarks($null)");

        final HealthCheckResult result = new VelocityHealthCheck().check();

        assertEquals(HealthStatus.DOWN, result.status());
    }

    @Test
    public void returnsDownWhenEngineThrows() {
        // Defense-in-depth: with #35601's fail-loud flag enabled, engine init
        // throws and VelocityUtil.getEngine() propagates a DotRuntimeException.
        // The health check should report DOWN, not blow up the probe endpoint.
        velocityUtilMock.when(VelocityUtil::getEngine)
                .thenThrow(new RuntimeException("engine init failed"));

        final HealthCheckResult result = new VelocityHealthCheck().check();

        assertEquals(HealthStatus.DOWN, result.status());
    }

    private void stubEngineToWrite(final String output) {
        final VelocityEngine engine = mock(VelocityEngine.class);
        doAnswer(invocation -> {
            final Writer writer = invocation.getArgument(1);
            writer.write(output);
            return true;
        }).when(engine).evaluate(any(Context.class), any(Writer.class), anyString(), anyString());
        velocityUtilMock.when(VelocityUtil::getEngine).thenReturn(engine);
    }
}
