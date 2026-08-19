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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
     * U+0130 LATIN CAPITAL LETTER I WITH DOT ABOVE — the one code point (under a non-Turkish
     * default locale) whose {@code toLowerCase()} form is longer than itself: {@code i} + U+0307.
     * Any index derived from a lowercased copy of the HTML drifts by one per occurrence. See #37072.
     */
    private static final String DOTTED_CAPITAL_I = "İ";

    /** Occurrence counts that mangled the closing tags (1, 9), coincidentally worked (7, 14), or threw (20). */
    private static final int[] DOTTED_CAPITAL_I_COUNTS = {1, 7, 9, 14, 20};

    /**
     * Builds a well-formed page whose body carries {@code count} occurrences of {@code İ} ahead of
     * the closing {@code </body>} tag.
     */
    private static String htmlWithDottedCapitalI(final int count) {
        return "<html><head><title>Test</title></head><body><p>"
                + DOTTED_CAPITAL_I.repeat(count) + "</p></body></html>";
    }

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

    /**
     * The reported defect: the injection index was computed from {@code html.toLowerCase()} and then
     * used to slice the original string, so every {@code İ} before {@code </body>} pushed the splice
     * one char too far — into the middle of the closing tags, and eventually past the end of the
     * string entirely.
     */
    @Test
    public void shouldInjectBeforeClosingBodyWhenHtmlContainsDottedCapitalI() throws Exception {
        for (final int count : DOTTED_CAPITAL_I_COUNTS) {
            final String result = invokeInjectUVEScript(
                    createBuilder(), htmlWithDottedCapitalI(count), Collections.emptyList());

            final int scriptIdx = result.indexOf(HTMLPageAssetRenderedBuilder.SDK_EDITOR_SCRIPT_SOURCE);
            assertTrue("Script should be injected with " + count + " dotted capital I chars",
                    scriptIdx != -1);
            assertTrue("Script should be injected before </body> with " + count + " dotted capital I chars",
                    scriptIdx < result.indexOf("</body>"));
            assertTrue("Closing tags should remain intact with " + count + " dotted capital I chars",
                    result.endsWith("</body></html>"));
        }
    }

    /**
     * Pins the exact symptom from the report: with 9 occurrences the script landed inside
     * {@code </html>}, leaving {@code </<script ...>} for the parser to swallow as a bogus comment
     * and a bare {@code html>} text node at the end of the body.
     */
    @Test
    public void shouldNotSplitClosingTagsWithNineDottedCapitalI() throws Exception {
        final String result = invokeInjectUVEScript(
                createBuilder(), htmlWithDottedCapitalI(9), Collections.emptyList());

        assertFalse("Closing tags should not be split by the injected script",
                result.contains("</<script"));
        assertFalse("No bare 'html>' text node should be emitted",
                result.contains("></script>html>"));
    }

    @Test
    public void shouldInjectBeforeUpperCaseClosingBodyTag() throws Exception {
        final String result = invokeInjectUVEScript(
                createBuilder(), "<html><BODY><p>Hello</p></BODY></html>", Collections.emptyList());

        final int scriptIdx = result.indexOf(HTMLPageAssetRenderedBuilder.SDK_EDITOR_SCRIPT_SOURCE);
        assertTrue("Script should be injected for an upper-case closing body tag", scriptIdx != -1);
        assertTrue("Script should be injected before </BODY>", scriptIdx < result.indexOf("</BODY>"));
    }

    /**
     * The original implementation used {@code lastIndexOf}; the replacement must keep picking the
     * last occurrence rather than the first.
     */
    @Test
    public void shouldInjectBeforeTheLastClosingBodyTag() throws Exception {
        final String html = "<html><body><p>first</body><p>second</body></html>";

        final String result = invokeInjectUVEScript(createBuilder(), html, Collections.emptyList());

        final int scriptIdx = result.indexOf(HTMLPageAssetRenderedBuilder.SDK_EDITOR_SCRIPT_SOURCE);
        assertTrue("Script should be injected after the first </body>",
                scriptIdx > result.indexOf("</body>"));
        assertTrue("Closing tags should remain intact", result.endsWith("</body></html>"));
    }

    /**
     * {@code String.toLowerCase()} with no argument uses {@code Locale.getDefault()}, and the set of
     * code points that grow when lowercased is locale-dependent (none under {@code tr}, four under
     * {@code lt}). The injection result must not depend on the JVM default locale.
     */
    @Test
    public void shouldProduceIdenticalOutputAcrossDefaultLocales() throws Exception {
        final Locale originalLocale = Locale.getDefault();
        try {
            final Set<String> outputs = new HashSet<>();
            for (final String languageTag : new String[]{"en", "tr", "lt"}) {
                Locale.setDefault(Locale.forLanguageTag(languageTag));
                outputs.add(invokeInjectUVEScript(
                        createBuilder(), htmlWithDottedCapitalI(9), Collections.emptyList()));
            }
            assertEquals("Injection result should not depend on the JVM default locale",
                    1, outputs.size());
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

}
