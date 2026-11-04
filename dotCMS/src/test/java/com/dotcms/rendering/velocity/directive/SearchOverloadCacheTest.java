package com.dotcms.rendering.velocity.directive;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.index.SearchRequestFailure;
import com.dotcms.content.index.SearchUnavailableException;
import com.dotmarketing.business.BlockDirectiveCache;
import com.dotmarketing.business.CacheLocator;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;

/** Real rendering/cache boundaries must refuse output produced after a swallowed search failure. */
public class SearchOverloadCacheTest {

    /** Restores request-local state even when a render throws. */
    @After
    public void cleanup() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    /** A nested #dotParse merge must propagate overload, not report a partial render as successful. */
    @Test
    public void nestedDirectiveDoesNotAbsorbSearchFailure() throws Exception {
        final SearchUnavailableException failure = new SearchUnavailableException(
                SearchUnavailableException.Reason.REJECTED, null);
        final TestDirective directive = new TestDirective();
        final InternalContextAdapter context = mock(InternalContextAdapter.class);
        final StringWriter output = new StringWriter();
        final Template template = mock(Template.class);
        final SimpleNode body = mock(SimpleNode.class);
        when(template.getData()).thenReturn(body);
        doThrow(new RuntimeException(failure)).when(body).render(context, output);
        assertSame(failure, assertThrows(SearchUnavailableException.class,
                () -> directive.renderTemplate(context, output, template, "test.vtl")));
        assertEquals("", output.toString());
        verify(context).popCurrentTemplateName();
    }

    /** A swallowed failure in a #dotcache block must not write the partial block or insert it. */
    @Test
    public void dotcacheDoesNotStoreOrEmitPartialBlock() throws Exception {
        final Map<String, Object> attributes = new HashMap<>();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        when(request.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        final InternalContextAdapter context = mock(InternalContextAdapter.class);
        when(context.get("request")).thenReturn(request);
        final Node directiveNode = mock(Node.class);
        final Node key = mock(Node.class);
        final Node ttl = mock(Node.class);
        final Node body = mock(Node.class);
        when(directiveNode.jjtGetChild(0)).thenReturn(key);
        when(directiveNode.jjtGetChild(1)).thenReturn(ttl);
        when(directiveNode.jjtGetChild(2)).thenReturn(body);
        when(key.value(context)).thenReturn("cache-key");
        when(ttl.value(context)).thenReturn(60);
        doAnswer(invocation -> {
            final Writer output = invocation.getArgument(1);
            output.write("partial-content");
            SearchRequestFailure.record(new SearchUnavailableException(
                    SearchUnavailableException.Reason.DEADLINE, null));
            return true;
        }).when(body).render(same(context), any(Writer.class));
        final BlockDirectiveCache cache = mock(BlockDirectiveCache.class);
        when(cache.get("cache-key")).thenReturn(Map.of());
        final DotCacheDirective directive = new DotCacheDirective() {
            /** Avoids licensing dependencies in a cache-boundary unit test. */
            @Override
            boolean shouldCache(final Context ignored, final HttpServletRequest ignoredRequest) {
                return true;
            }
            /** This test fills a fresh cache, not an explicit refresh. */
            @Override
            boolean refreshCache(final HttpServletRequest ignored) {
                return false;
            }
        };
        final StringWriter output = new StringWriter();
        try (MockedStatic<CacheLocator> caches = mockStatic(CacheLocator.class)) {
            caches.when(CacheLocator::getBlockDirectiveCache).thenReturn(cache);
            assertThrows(SearchUnavailableException.class,
                    () -> directive.render(context, output, directiveNode));
            verify(cache, never()).add(anyString(), anyMap(), any());
            assertEquals("", output.toString());
        }
    }

    /** Only renderTemplate is exercised; no template lookup or cache implementation is needed. */
    private static final class TestDirective extends DotDirective {
        /** Names the test directive. */
        @Override
        public String getName() {
            return "test";
        }
        /** This test invokes a supplied template directly, so no path needs resolution. */
        @Override
        String resolveTemplatePath(final Context context, final Writer writer,
                final RenderParams params, final String[] arguments) {
            return "test.vtl";
        }
    }
}
