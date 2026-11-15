package com.dotcms.ai.workflow;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.ai.util.VelocityContextFactory;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.context.Context;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/** The runner must not let a request left bound by a nested page render stand in for the caller's request. */
public class OpenAIGenerateImageRunnerRequestTest {

    private HttpServletRequest previous;

    /** Remembers the request bound to the thread before the test. */
    @Before
    public void saveRequest() {
        previous = HttpServletRequestThreadLocal.INSTANCE.getRequest();
    }

    /** Puts the thread-bound request back so no other test sees this one's. */
    @After
    public void restoreRequest() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(previous);
    }

    /**
     * Given building the context renders a page that leaves its own request bound to the thread,
     * When the runner evaluates the prompt,
     * Then the thread is back on the caller's request (here none), and it is back on it afterwards even
     * though the run fails.
     */
    @Test
    public void request_left_by_the_context_build_is_not_kept_during_or_after_the_run() throws Exception {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        final HttpServletRequest leaked = mock(HttpServletRequest.class);
        final HttpServletRequest contextRequest = mock(HttpServletRequest.class);
        final Context ctx = mock(Context.class);
        when(ctx.get("request")).thenReturn(contextRequest);
        final AtomicReference<HttpServletRequest> seenDuringEval = new AtomicReference<>();

        final Field field = mock(Field.class);
        final ContentType type = mock(ContentType.class);
        when(type.fieldMap()).thenReturn(Map.of("image", field));
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(type);
        when(contentlet.getTitle()).thenReturn("a page");
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("dotcms.org.1");

        try (MockedStatic<VelocityContextFactory> contexts = mockStatic(VelocityContextFactory.class);
                MockedStatic<VelocityUtil> velocity = mockStatic(VelocityUtil.class);
                MockedStatic<APILocator> locator = mockStatic(APILocator.class);
                MockedStatic<SystemMessageEventUtil> messages = mockStatic(SystemMessageEventUtil.class)) {
            contexts.when(() -> VelocityContextFactory.getMockContext(any(Contentlet.class), any(User.class)))
                    .thenAnswer(invocation -> {
                        HttpServletRequestThreadLocal.INSTANCE.setRequest(leaked);
                        return ctx;
                    });
            velocity.when(() -> VelocityUtil.eval(anyString(), any(Context.class)))
                    .thenAnswer(invocation -> {
                        seenDuringEval.set(HttpServletRequestThreadLocal.INSTANCE.getRequest());
                        throw new IllegalStateException("stop after the evaluation");
                    });
            messages.when(SystemMessageEventUtil::getInstance)
                    .thenReturn(mock(SystemMessageEventUtil.class));

            assertThrows(DotRuntimeException.class,
                    () -> new OpenAIGenerateImageRunner(contentlet, user, "a prompt", true, "image").run());
        }

        assertSame("the request the context build left behind must not be kept",
                contextRequest, seenDuringEval.get());
        assertSame("the thread must end on the request it started with",
                null, HttpServletRequestThreadLocal.INSTANCE.getRequest());
    }
}
