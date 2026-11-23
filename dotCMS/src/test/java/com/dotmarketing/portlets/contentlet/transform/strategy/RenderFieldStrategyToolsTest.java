package com.dotmarketing.portlets.contentlet.transform.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.Set;
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
    private static final List<String> RESTRICTED = List.of("filetool", "macro", "mailer");

    private MockedStatic<RestrictedViewToolAccess> mockedAccess;
    private Contentlet contentlet;
    private ChainedContext context;

    @BeforeEach
    public void setUp() {
        mockedAccess = mockStatic(RestrictedViewToolAccess.class);
        mockedAccess.when(RestrictedViewToolAccess::restrictedToolKeys)
                .thenReturn(new LinkedHashSet<>(RESTRICTED));

        contentlet = mock(Contentlet.class);
        when(contentlet.getModUser()).thenReturn(AUTHOR_ID);

        context = new ChainedContext(null, null, mock(HttpServletRequest.class), null);
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

    private void givenAuthorAccess(final boolean author) {
        mockedAccess.when(() -> RestrictedViewToolAccess.userIdHasAccess(eq(AUTHOR_ID))).thenReturn(author);
    }

    /**
     * When: the content author has no access
     * Should: remove the restricted tools, return them, and keep the others
     */
    @Test
    public void noAccess_removesRestrictedTools() {
        givenAuthorAccess(false);

        final Map<String, Object> removed = RenderFieldStrategy.restrictViewTools(context, contentlet);

        assertEquals(Set.copyOf(RESTRICTED), removed.keySet());
        for (final String key : RESTRICTED) {
            assertNull(context.get(key), "Should be removed: " + key);
        }
        assertNotNull(context.get("config"));
        assertNotNull(context.get("dotcontent"));
    }

    /**
     * When: the content author has access
     * Should: keep every tool
     */
    @Test
    public void authorAccess_keepsTools() {
        givenAuthorAccess(true);

        assertTrue(RenderFieldStrategy.restrictViewTools(context, contentlet).isEmpty());
        assertNotNull(context.get("filetool"));
        assertNotNull(context.get("macro"));
    }

    /**
     * When: there is no content to resolve the author from
     * Should: remove the restricted tools
     */
    @Test
    public void noContent_removesRestrictedTools() {
        assertEquals(Set.copyOf(RESTRICTED),
                RenderFieldStrategy.restrictViewTools(context, null).keySet());
    }

    /**
     * When: the removed tools are restored
     * Should: be available again under their keys
     */
    @Test
    public void restoreViewTools_putsToolsBack() {
        givenAuthorAccess(false);
        final Map<String, Object> removed = RenderFieldStrategy.restrictViewTools(context, contentlet);
        assertNull(context.get("macro"));

        RenderFieldStrategy.restoreViewTools(context, removed);

        for (final String key : RESTRICTED) {
            assertEquals(removed.get(key), context.get(key), "Should be restored: " + key);
        }
        // restoring nothing is harmless, for any kind of context
        RenderFieldStrategy.restoreViewTools(context, Map.of());
        RenderFieldStrategy.restoreViewTools(new VelocityContext(), removed);
    }

    /**
     * When: the context has no toolbox
     * Should: leave it as it is
     */
    @Test
    public void basicContext_isNotChanged() {
        givenAuthorAccess(false);
        assertTrue(RenderFieldStrategy.restrictViewTools(new VelocityContext(), contentlet).isEmpty());
    }
}
