package com.dotmarketing.portlets.contentlet.transform.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.rendering.velocity.viewtools.RestrictedViewToolAccess;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the view tool restrictions applied by {@link RenderFieldStrategy}.
 */
public class RenderFieldStrategyToolsTest {

    private static final String AUTHOR_ID = "author-id";

    private MockedStatic<RestrictedViewToolAccess> mockedAccess;
    private HttpServletRequest request;
    private Contentlet contentlet;
    private ChainedContext context;

    @BeforeEach
    public void setUp() {
        mockedAccess = mockStatic(RestrictedViewToolAccess.class);
        mockedAccess.when(RestrictedViewToolAccess::restrictedToolKeys).thenReturn(
                new LinkedHashSet<>(List.of("config", "filetool", "macro", "mailer")));

        request = mock(HttpServletRequest.class);
        contentlet = mock(Contentlet.class);
        when(contentlet.getModUser()).thenReturn(AUTHOR_ID);
        when(contentlet.getInode()).thenReturn("1234-abcd");

        context = new ChainedContext(null, null, request, null);
        final Map<String, Object> toolbox = new HashMap<>();
        for (final String key : List.of("config", "filetool", "macro", "mailer", "dotcontent")) {
            toolbox.put(key, new Object());
        }
        context.setToolbox(toolbox);
    }

    @AfterEach
    public void tearDown() {
        mockedAccess.close();
    }

    private void givenAccess(final boolean author, final boolean caller) {
        mockedAccess.when(() -> RestrictedViewToolAccess.userIdHasAccess(eq(AUTHOR_ID))).thenReturn(author);
        mockedAccess.when(() -> RestrictedViewToolAccess.requestUserHasAccess(any())).thenReturn(caller);
    }

    /**
     * When: neither the content author nor the caller has access
     * Should: remove the restricted tools and keep the others
     */
    @Test
    public void noAccess_removesRestrictedTools() {
        givenAccess(false, false);

        assertTrue(RenderFieldStrategy.restrictViewTools(context, request, contentlet));

        for (final String key : List.of("config", "filetool", "macro", "mailer")) {
            assertNull(context.get(key), "Should be removed: " + key);
        }
        assertNotNull(context.get("dotcontent"));
    }

    /**
     * When: the content author has access
     * Should: keep every tool
     */
    @Test
    public void authorAccess_keepsTools() {
        givenAccess(true, false);

        assertFalse(RenderFieldStrategy.restrictViewTools(context, request, contentlet));
        assertNotNull(context.get("config"));
        assertNotNull(context.get("filetool"));
    }

    /**
     * When: the caller has access
     * Should: keep every tool
     */
    @Test
    public void callerAccess_keepsTools() {
        givenAccess(false, true);

        assertFalse(RenderFieldStrategy.restrictViewTools(context, request, contentlet));
        assertNotNull(context.get("macro"));
    }

    /**
     * When: the context has no toolbox
     * Should: leave it as it is
     */
    @Test
    public void basicContext_isNotChanged() {
        givenAccess(false, false);
        assertFalse(RenderFieldStrategy.restrictViewTools(new VelocityContext(), request, contentlet));
    }

    /**
     * When: a field value is evaluated
     * Should: use a template name that identifies the content being rendered
     */
    @Test
    public void fieldTemplateName_identifiesContent() {
        assertEquals("/LIVE/1234-abcd/body", RenderFieldStrategy.getFieldTemplateName(contentlet, "body"));
        assertEquals("", RenderFieldStrategy.getFieldTemplateName(null, "body"));
    }
}
