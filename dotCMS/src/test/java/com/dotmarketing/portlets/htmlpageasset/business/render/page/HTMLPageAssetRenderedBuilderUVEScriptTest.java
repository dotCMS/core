package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for the UVE script injection logic in {@link HTMLPageAssetRenderedBuilder}.
 * <p>
 * Uses {@code Mockito.mock(Class, CALLS_REAL_METHODS)} to create instances without triggering
 * the constructor (which depends on {@code APILocator} and Elasticsearch). The private
 * {@code injectUVEScript} method is invoked via reflection.
 */
public class HTMLPageAssetRenderedBuilderUVEScriptTest {

    private static final String SIMPLE_HTML =
            "<html><head><title>Test</title></head><body><p>Hello</p></body></html>";

    private static final String HTML_WITHOUT_BODY =
            "<html><head><title>Test</title></head><p>Hello</p></html>";

    /**
     * Creates a builder instance without calling the constructor to avoid
     * APILocator/Elasticsearch initialization.
     */
    private HTMLPageAssetRenderedBuilder createBuilder() {
        return mock(HTMLPageAssetRenderedBuilder.class, Mockito.CALLS_REAL_METHODS);
    }

    /**
     * Invokes the private {@code injectUVEScript(String, Collection)} method via reflection.
     */
    private String invokeInjectUVEScript(final HTMLPageAssetRenderedBuilder builder,
                                         final String html,
                                         final Collection<? extends ContainerRaw> containers)
            throws Exception {

        final Method method = HTMLPageAssetRenderedBuilder.class.getDeclaredMethod(
                "injectUVEScript", String.class, Collection.class);
        method.setAccessible(true);
        return (String) method.invoke(builder, html, containers);
    }

    @Test
    public void shouldInjectScriptBeforeClosingBodyTag() throws Exception {
        final String result = invokeInjectUVEScript(createBuilder(), SIMPLE_HTML, Collections.emptyList());

        assertTrue("Result should contain the UVE script tag",
                result.contains(HTMLPageAssetRenderedBuilder.SDK_EDITOR_SCRIPT_SOURCE));

        final int scriptIdx = result.indexOf(HTMLPageAssetRenderedBuilder.SDK_EDITOR_SCRIPT_SOURCE);
        final int bodyIdx = result.indexOf("</body>");
        assertTrue("Script should appear before </body>", scriptIdx < bodyIdx);
    }

    @Test
    public void shouldAppendScriptWhenNoClosingBodyTag() throws Exception {
        final String result = invokeInjectUVEScript(createBuilder(), HTML_WITHOUT_BODY, Collections.emptyList());

        assertTrue("Result should contain the UVE script tag",
                result.contains(HTMLPageAssetRenderedBuilder.SDK_EDITOR_SCRIPT_SOURCE));
        assertTrue("Script should be appended at the end",
                result.endsWith(HTMLPageAssetRenderedBuilder.SDK_EDITOR_SCRIPT_SOURCE));
    }

    @Test
    public void shouldReturnNullWhenHtmlIsNull() throws Exception {
        final String result = invokeInjectUVEScript(createBuilder(), null, Collections.emptyList());

        assertNull("Null HTML should be returned as-is", result);
    }

    @Test
    public void shouldReturnEmptyWhenHtmlIsEmpty() throws Exception {
        final String result = invokeInjectUVEScript(createBuilder(), "", Collections.emptyList());

        assertEquals("Empty HTML should be returned as-is", "", result);
    }

    @Test
    public void shouldFallBackToPlainScriptWhenContainersAreEmpty() throws Exception {
        final String result = invokeInjectUVEScript(createBuilder(), SIMPLE_HTML, Collections.emptyList());

        assertTrue("Should contain plain SDK script tag",
                result.contains(HTMLPageAssetRenderedBuilder.SDK_EDITOR_SCRIPT_SOURCE));
        assertFalse("Should NOT contain initDotUVE when no schemas exist",
                result.contains("initDotUVE"));
    }

    @Test
    public void shouldFallBackToPlainScriptWhenContentletGetContentTypeThrows() throws Exception {
        final Contentlet faultyContentlet = mock(Contentlet.class);
        when(faultyContentlet.getContentType())
                .thenThrow(new RuntimeException("Simulated ContentType failure"));

        final Map<String, List<Contentlet>> contentletMap = new HashMap<>();
        contentletMap.put("uuid-1", List.of(faultyContentlet));

        final ContainerRaw containerRaw = new ContainerRaw(
                mock(Container.class),
                Collections.emptyList(),
                contentletMap
        );

        final String result = invokeInjectUVEScript(createBuilder(), SIMPLE_HTML, List.of(containerRaw));

        assertTrue("Should contain the UVE script tag despite ContentType exception",
                result.contains(HTMLPageAssetRenderedBuilder.SDK_EDITOR_SCRIPT_SOURCE));
        assertFalse("Should NOT contain initDotUVE (fallback to plain script)",
                result.contains("initDotUVE"));
    }

}
