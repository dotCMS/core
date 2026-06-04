package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for the UVE script injection logic in {@link HTMLPageAssetRenderedBuilder}.
 * <p>
 * Uses {@code Mockito.mock(Class, CALLS_REAL_METHODS)} to create instances without triggering
 * the constructor (which depends on {@code APILocator} and Elasticsearch). The private
 * {@code injectUVEScript} method is invoked via reflection.
 * <p>
 * {@code injectUVEScript} now accepts pre-resolved {@code List<JsonNode>} schemas (computed once
 * by {@code resolveStyleEditorSchemas}) rather than raw containers, so tests supply schemas
 * directly.
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
     * Invokes the private {@code injectUVEScript(String, List)} method via reflection.
     */
    private String invokeInjectUVEScript(final HTMLPageAssetRenderedBuilder builder,
                                         final String html,
                                         final List<JsonNode> schemas)
            throws Exception {

        final Method method = HTMLPageAssetRenderedBuilder.class.getDeclaredMethod(
                "injectUVEScript", String.class, List.class);
        method.setAccessible(true);
        return (String) method.invoke(builder, html, schemas);
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
    public void shouldFallBackToPlainScriptWhenSchemasAreEmpty() throws Exception {
        final String result = invokeInjectUVEScript(createBuilder(), SIMPLE_HTML, Collections.emptyList());

        assertTrue("Should contain plain SDK script tag",
                result.contains(HTMLPageAssetRenderedBuilder.SDK_EDITOR_SCRIPT_SOURCE));
        assertFalse("Should NOT contain initDotUVE when no schemas exist",
                result.contains("initDotUVE"));
    }

    @Test
    public void shouldInjectInitFunctionWhenSchemasArePresent() throws Exception {
        final JsonNode schema = new ObjectMapper().readTree("{\"contentType\":\"Blog\",\"sections\":[]}");

        final String result = invokeInjectUVEScript(createBuilder(), SIMPLE_HTML, List.of(schema));

        // When schemas exist the full UVE_SCRIPTS_TEMPLATE is used: it contains the initDotUVE()
        // inline function + a <script src onload="initDotUVE()"> tag — NOT the plain SDK_EDITOR_SCRIPT_SOURCE.
        assertTrue("Should contain initDotUVE when schemas are present",
                result.contains(HTMLPageAssetRenderedBuilder.UVE_INIT_FUNCTION_PREFIX));
        assertTrue("Should contain the dot-uve.js src reference",
                result.contains("/ext/uve/dot-uve.js"));
        assertTrue("Should embed the schema JSON",
                result.contains("\"contentType\":\"Blog\""));

        final int initIdx = result.indexOf(HTMLPageAssetRenderedBuilder.UVE_INIT_FUNCTION_PREFIX);
        final int bodyIdx = result.indexOf("</body>");
        assertTrue("Init function should appear before </body>", initIdx < bodyIdx);
    }

}
