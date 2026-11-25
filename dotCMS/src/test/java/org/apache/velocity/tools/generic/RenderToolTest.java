package org.apache.velocity.tools.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rendering.velocity.viewtools.RestrictedViewToolAccess;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RenderTool}, the tool behind {@code $render.eval}.
 */
public class RenderToolTest {

    @AfterEach
    public void tearDown() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    /**
     * Method to test: {@link RenderTool#eval(Context, String)}
     * When: code rendered on behalf of a known author evaluates a string
     * Should: run that string with no author of its own, and give the surrounding author back
     * afterwards. The string is data the surrounding code assembled, often out of request input,
     * so it must not inherit that code's standing with the view tools that check who wrote the
     * code being run.
     */
    @Test
    public void eval_runsTheEvaluatedCodeWithNoAuthorOfItsOwn() throws Exception {
        final HttpServletRequest request = requestWithAttributes();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        final Object nothingBefore =
                RestrictedViewToolAccess.announceRenderingAuthor(request, "container-author");
        final Object surrounding = request.getAttribute(RestrictedViewToolAccess.RENDERING_AUTHOR_ID);
        assertNotNull(surrounding, "The surrounding render announced an author");

        final AtomicReference<Boolean> toolAllowedDuringEval = new AtomicReference<>(null);
        final RenderTool renderTool = new RenderTool();
        renderTool.setVelocityEngine(engineRecording(request, toolAllowedDuringEval));

        renderTool.eval(new VelocityContext(), "$anything");

        assertEquals(Boolean.FALSE, toolAllowedDuringEval.get(),
                "The evaluated string must not run as the author of the code that evaluated it");
        assertNotNull(request.getAttribute(RestrictedViewToolAccess.RENDERING_AUTHOR_ID),
                "The surrounding author must be back once the evaluation is done");

        RestrictedViewToolAccess.restoreRenderingAuthor(request, nothingBefore);
        assertNull(request.getAttribute(RestrictedViewToolAccess.RENDERING_AUTHOR_ID));
    }

    /**
     * @return an engine that, instead of evaluating, records what a view tool would read while
     * the evaluated code runs
     */
    private static VelocityEngine engineRecording(final HttpServletRequest request,
            final AtomicReference<Boolean> toolAllowedDuringEval) throws Exception {
        final VelocityEngine engine = mock(VelocityEngine.class);
        when(engine.evaluate(any(Context.class), any(Writer.class), anyString(), anyString()))
                .thenAnswer(call -> {
                    toolAllowedDuringEval.set(
                            RestrictedViewToolAccess.renderingAuthorHasAccess(request));
                    return true;
                });
        return engine;
    }

    /**
     * @return a request whose attributes behave like a real one, which a plain mock does not do
     */
    private static HttpServletRequest requestWithAttributes() {
        final Map<String, Object> attributes = new HashMap<>();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenAnswer(call -> attributes.get(call.getArgument(0)));
        doAnswer(call -> attributes.put(call.getArgument(0), call.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        doAnswer(call -> attributes.remove(call.getArgument(0)))
                .when(request).removeAttribute(anyString());
        return request;
    }
}
