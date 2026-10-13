package com.dotcms.ai.viewtool;

import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import org.junit.Test;
import org.owasp.encoder.Encode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AIViewToolOutputEscaper}, the routine that produces the HTML-escaped copy
 * of a dotAI viewtool payload (#37153). No container is needed: the escaper works on
 * {@link JSONObject}, {@link JSONArray}, plain {@link Map} and {@link List} values only.
 *
 * @author hassandotcms
 */
public class AIViewToolOutputEscaperTest {

    private static final String MARKUP = "<script>alert(1)</script><img src=x onerror=alert(1)>";
    private static final String MARKUP_ESCAPED =
            "&lt;script&gt;alert(1)&lt;/script&gt;&lt;img src=x onerror=alert(1)&gt;";
    private static final String EVERY_SPECIAL = "<a href=\"x\">Tom & Jerry's</a>";

    // ------------------------------------------------------------------ deepEscape(Object)

    /**
     * Scenario: a string containing every HTML-significant character.
     * Given the string {@code <a href="x">Tom & Jerry's</a>}
     * When deepEscape is applied
     * Then the result equals exactly what OWASP {@code Encode.forHtml} produces.
     */
    @Test
    public void deepEscape_string_isExactOwaspForHtmlOutput() {
        final Object out = AIViewToolOutputEscaper.deepEscape(EVERY_SPECIAL);

        assertEquals(Encode.forHtml(EVERY_SPECIAL), out);
        assertEquals("&lt;a href=&#34;x&#34;&gt;Tom &amp; Jerry&#39;s&lt;/a&gt;", out);
        assertEquals(MARKUP_ESCAPED, AIViewToolOutputEscaper.deepEscape(MARKUP));
    }

    /**
     * Scenario: nested provider-shaped object.
     * Given {@code {choices:[{message:{content:<markup>, role:"assistant"}, index:0}], model:"m<1>"}}
     * When deepEscape is applied
     * Then every string leaf at every depth is escaped, numbers keep their type and value,
     * and the shape (keys, array lengths) is identical.
     */
    @Test
    public void deepEscape_nestedJson_escapesEveryStringLeafAndKeepsShape() {
        final JSONObject source = providerShaped(MARKUP);

        final JSONObject out = AIViewToolOutputEscaper.deepEscape(source);

        assertSameShape(source, out);
        final JSONObject message = out.getJSONArray("choices").getJSONObject(0).getJSONObject("message");
        assertEquals(MARKUP_ESCAPED, message.getString("content"));
        assertEquals("assistant", message.getString("role"));
        assertEquals("m&lt;1&gt;", out.getString("model"));
        assertEquals(0, out.getJSONArray("choices").getJSONObject(0).get("index"));
        assertTrue(out.getJSONArray("choices").getJSONObject(0).get("index") instanceof Integer);
        assertTrue(out.get("ok") instanceof Boolean);
        assertEquals(Boolean.TRUE, out.get("ok"));
        assertNoRawMarkup(out);
    }

    /**
     * Scenario: a JSONArray of strings and an array of objects.
     * When deepEscape is applied
     * Then the result is a JSONArray of the same length with every string escaped.
     */
    @Test
    public void deepEscape_jsonArray_escapesElements() {
        final JSONArray source = new JSONArray();
        source.put(MARKUP);
        source.put(7);
        source.put(new JSONObject().put("t", "<b>"));

        final Object out = AIViewToolOutputEscaper.deepEscape(source);

        assertTrue(out instanceof JSONArray);
        final JSONArray outArray = (JSONArray) out;
        assertEquals(3, outArray.length());
        assertEquals(MARKUP_ESCAPED, outArray.get(0));
        assertEquals(7, outArray.get(1));
        assertEquals("&lt;b&gt;", outArray.getJSONObject(2).getString("t"));
        // source untouched
        assertEquals(MARKUP, source.get(0));
        assertEquals("<b>", source.getJSONObject(2).getString("t"));
    }

    /**
     * Scenario: the pre-#37154 error payload, an immutable {@code Map.of(...)}.
     * When deepEscape is applied
     * Then no exception is thrown, the result is a JSONObject whose strings are escaped, and the
     * immutable source is untouched.
     */
    @Test
    public void deepEscape_immutableMap_returnsEscapedJsonObjectWithoutThrowing() {
        final Map<String, Object> source = Map.of("error", "<b>boom</b>", "stackTrace", "<x>\n\tat a.b(C.java:1)");

        final Object out = AIViewToolOutputEscaper.deepEscape(source);

        assertTrue(out instanceof JSONObject);
        final JSONObject outObject = (JSONObject) out;
        assertEquals(source.keySet(), outObject.keySet());
        assertEquals("&lt;b&gt;boom&lt;/b&gt;", outObject.getString("error"));
        assertEquals("&lt;x&gt;\n\tat a.b(C.java:1)", outObject.getString("stackTrace"));
        assertEquals("<b>boom</b>", source.get("error"));
    }

    /**
     * Scenario: a plain List.
     * When deepEscape is applied
     * Then the result is a JSONArray with escaped strings.
     */
    @Test
    public void deepEscape_plainList_returnsJsonArray() {
        final List<Object> source = List.of("<i>", 1, Map.of("k", "<u>"));

        final Object out = AIViewToolOutputEscaper.deepEscape(source);

        assertTrue(out instanceof JSONArray);
        final JSONArray outArray = (JSONArray) out;
        assertEquals("&lt;i&gt;", outArray.get(0));
        assertEquals(1, outArray.get(1));
        assertEquals("&lt;u&gt;", outArray.getJSONObject(2).getString("k"));
    }

    /**
     * Scenario: non-string leaves.
     * Given numbers, booleans, {@code JSONObject.NULL} and a Java null value
     * When deepEscape is applied
     * Then numbers and booleans are the same instances, NULL stays NULL, and a Java null becomes
     * the JSON NULL sentinel exactly as {@code new JSONObject(Map)} would produce.
     */
    @Test
    public void deepEscape_nonStringLeaves_arePreserved() {
        final Integer count = 42;
        final Double distance = 0.31d;
        final Map<String, Object> source = new HashMap<>();
        source.put("count", count);
        source.put("distance", distance);
        source.put("flag", Boolean.FALSE);
        source.put("jsonNull", JSONObject.NULL);
        source.put("javaNull", null);

        final JSONObject out = (JSONObject) AIViewToolOutputEscaper.deepEscape(source);

        assertSame(count, out.get("count"));
        assertSame(distance, out.get("distance"));
        assertSame(Boolean.FALSE, out.get("flag"));
        assertSame(JSONObject.NULL, out.get("jsonNull"));
        assertTrue(JSONObject.NULL.equals(out.get("javaNull")));
        assertEquals(source.keySet(), out.keySet());

        assertNull(AIViewToolOutputEscaper.deepEscape((Object) null));
        assertSame(JSONObject.NULL, AIViewToolOutputEscaper.deepEscape(JSONObject.NULL));
        assertSame(count, AIViewToolOutputEscaper.deepEscape(count));
    }

    /**
     * Scenario: a leaf that is neither a JSON type nor a scalar (the SearchTool error payload carries
     * StackTraceElement objects). When deepEscape is applied, the leaf is stringified and encoded, so a
     * constructor frame's {@code <init>} cannot reach the page raw.
     */
    @Test
    public void deepEscape_nonJsonLeaf_isStringifiedAndEscaped() {
        final StackTraceElement frame = new StackTraceElement("com.dotcms.Foo", "<init>", "Foo.java", 12);
        final Object out = AIViewToolOutputEscaper.deepEscape(List.of(frame));

        assertEquals("com.dotcms.Foo.&lt;init&gt;(Foo.java:12)", ((JSONArray) out).get(0));
    }

    /**
     * Scenario: empty structures.
     * When deepEscape is applied to an empty object and an empty array
     * Then equally empty copies are returned.
     */
    @Test
    public void deepEscape_emptyStructures_returnEmptyCopies() {
        final JSONObject emptyObject = new JSONObject();
        final JSONArray emptyArray = new JSONArray();

        final JSONObject outObject = AIViewToolOutputEscaper.deepEscape(emptyObject);
        final Object outArray = AIViewToolOutputEscaper.deepEscape(emptyArray);

        assertTrue(outObject.isEmpty());
        assertNotSame(emptyObject, outObject);
        assertTrue(outArray instanceof JSONArray);
        assertEquals(0, ((JSONArray) outArray).length());
        assertEquals("", AIViewToolOutputEscaper.deepEscape(""));
    }

    /**
     * Scenario: text that already contains entities.
     * Given {@code Tom &amp; Jerry}
     * When deepEscape is applied
     * Then it is encoded again ({@code &amp;amp;}); the escaper is a correct encoder, not an
     * idempotent normaliser.
     */
    @Test
    public void deepEscape_alreadyEscapedText_isEncodedAgain() {
        assertEquals("Tom &amp;amp; Jerry &amp;lt;3", AIViewToolOutputEscaper.deepEscape("Tom &amp; Jerry &lt;3"));
    }

    /**
     * Scenario: the typed overload.
     * When deepEscape(JSONObject) is called
     * Then the declared return type is JSONObject (no cast needed by AIViewTool).
     */
    @Test
    public void deepEscape_typedOverload_returnsJsonObject() {
        final JSONObject out = AIViewToolOutputEscaper.deepEscape(new JSONObject().put("a", "<x>"));
        assertEquals("&lt;x&gt;", out.getString("a"));
    }

    /**
     * Scenario: the source object must not be mutated (the API layer's object is shared with REST).
     * When deepEscape is applied to a nested JSONObject
     * Then the source still holds the raw markup at every depth and the copy is a different instance.
     */
    @Test
    public void deepEscape_doesNotMutateSource() {
        final JSONObject source = providerShaped(MARKUP);
        final JSONArray sourceChoices = source.getJSONArray("choices");

        final JSONObject out = AIViewToolOutputEscaper.deepEscape(source);

        assertNotSame(source, out);
        assertNotSame(sourceChoices, out.getJSONArray("choices"));
        assertSame(sourceChoices, source.getJSONArray("choices"));
        assertEquals(MARKUP, source.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content"));
        assertEquals("m<1>", source.getString("model"));
    }

    // ------------------------------------------------------------ escapeSearchShaped(Object)

    /**
     * Scenario: the summarize success shape.
     * Given a payload with query, openAiResponse, dotCMSResults (title, body, matches) and metadata
     * When escapeSearchShaped is applied
     * Then query, every string under openAiResponse and matches[].extractedText are escaped, and
     * title, body, operator, distance, total and timeToEmbeddings are the very same instances.
     */
    @Test
    public void escapeSearchShaped_escapesOnlyTheFourItemsAndSharesTheRest() {
        final String title = MARKUP + " title";
        final String body = "<p>rich <b>body</b></p>";
        final String operator = "<=>";
        final Float distance = 0.31f;
        final Integer total = 3;
        final String timeToEmbeddings = "12ms";
        final JSONObject source = searchShaped(MARKUP, title, body, MARKUP + " extracted", operator,
                distance, total, timeToEmbeddings, providerShaped(MARKUP));

        final Object out = AIViewToolOutputEscaper.escapeSearchShaped(source);

        assertTrue(out instanceof JSONObject);
        final JSONObject outObject = (JSONObject) out;
        assertSameShape(source, outObject);

        // escaped
        assertEquals(MARKUP_ESCAPED, outObject.getString("query"));
        assertEquals(MARKUP_ESCAPED, outObject.getJSONObject("openAiResponse").getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message").getString("content"));
        assertEquals("m&lt;1&gt;", outObject.getJSONObject("openAiResponse").getString("model"));
        final JSONObject outResult = outObject.getJSONArray("dotCMSResults").getJSONObject(0);
        final JSONObject outMatch = outResult.getJSONArray("matches").getJSONObject(0);
        assertEquals(Encode.forHtml(MARKUP + " extracted"), outMatch.getString("extractedText"));
        assertNoRawMarkup(outObject.getJSONObject("openAiResponse"));

        // shared by reference
        assertSame(title, outResult.get("title"));
        assertSame(body, outResult.get("body"));
        assertSame(operator, outObject.get("operator"));
        assertSame(distance, outMatch.get("distance"));
        assertSame(total, outObject.get("total"));
        assertSame(timeToEmbeddings, outObject.get("timeToEmbeddings"));
        assertSame(source.getJSONArray("dotCMSResults").getJSONObject(0).get("categories"),
                outResult.get("categories"));

        // source untouched
        assertEquals(MARKUP, source.getString("query"));
        assertEquals(MARKUP + " extracted", source.getJSONArray("dotCMSResults").getJSONObject(0)
                .getJSONArray("matches").getJSONObject(0).getString("extractedText"));
        assertEquals(MARKUP, source.getJSONObject("openAiResponse").getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message").getString("content"));
    }

    /**
     * Scenario: the search shape (no openAiResponse) and a result with several matches.
     * When escapeSearchShaped is applied
     * Then only query and each match's extractedText change.
     */
    @Test
    public void escapeSearchShaped_withoutOpenAiResponse_escapesQueryAndExtractedTextOnly() {
        final JSONObject source = searchShaped("q <1>", "t <2>", "b <3>", "e <4>", "<=>", 0.5f, 1, "1ms", null);
        final JSONObject secondMatch = new JSONObject().put("distance", 0.7f).put("extractedText", "e <5>");
        source.getJSONArray("dotCMSResults").getJSONObject(0).getJSONArray("matches").put(secondMatch);

        final JSONObject out = (JSONObject) AIViewToolOutputEscaper.escapeSearchShaped(source);

        assertSameShape(source, out);
        assertFalse(out.has("openAiResponse"));
        assertEquals("q &lt;1&gt;", out.getString("query"));
        final JSONObject outResult = out.getJSONArray("dotCMSResults").getJSONObject(0);
        assertEquals("t <2>", outResult.getString("title"));
        assertEquals("b <3>", outResult.getString("body"));
        assertEquals("e &lt;4&gt;", outResult.getJSONArray("matches").getJSONObject(0).getString("extractedText"));
        assertEquals("e &lt;5&gt;", outResult.getJSONArray("matches").getJSONObject(1).getString("extractedText"));
        assertEquals("<=>", out.getString("operator"));
    }

    /**
     * Scenario: the no-hits summarize shape and the handled-error shape.
     * Given {@code {"error": "<b>no matching content</b>"}}
     * When escapeSearchShaped is applied
     * Then the error text is escaped and nothing else is added or removed.
     */
    @Test
    public void escapeSearchShaped_errorOnlyShape_escapesErrorText() {
        final JSONObject source = new JSONObject().put("error", "<b>no matching content</b>");

        final JSONObject out = (JSONObject) AIViewToolOutputEscaper.escapeSearchShaped(source);

        assertEquals(Set.of("error"), out.keySet());
        assertEquals("&lt;b&gt;no matching content&lt;/b&gt;", out.getString("error"));
        assertEquals("<b>no matching content</b>", source.getString("error"));
    }

    /**
     * Scenario: results lacking a matches array, or matches lacking extractedText, or a
     * dotCMSResults element that is not an object.
     * When escapeSearchShaped is applied
     * Then nothing throws and the odd elements are carried through unchanged.
     */
    @Test
    public void escapeSearchShaped_toleratesMissingKeysAndOddElements() {
        final JSONArray results = new JSONArray();
        results.put(new JSONObject().put("title", "<t>"));                                  // no matches
        results.put(new JSONObject().put("matches", new JSONArray().put(new JSONObject().put("distance", 0.1f))));
        results.put("<not an object>");
        final JSONObject source = new JSONObject().put("query", "<q>").put("dotCMSResults", results).put("total", 3);

        final JSONObject out = (JSONObject) AIViewToolOutputEscaper.escapeSearchShaped(source);

        assertSameShape(source, out);
        assertEquals("&lt;q&gt;", out.getString("query"));
        assertEquals("<t>", out.getJSONArray("dotCMSResults").getJSONObject(0).getString("title"));
        assertEquals("<not an object>", out.getJSONArray("dotCMSResults").get(2));
    }

    /**
     * Scenario: a non-Map payload reaches escapeSearchShaped (defensive path).
     * When the input is a String or a JSONArray
     * Then it behaves like deepEscape.
     */
    @Test
    public void escapeSearchShaped_nonMapInput_delegatesToDeepEscape() {
        assertEquals(MARKUP_ESCAPED, AIViewToolOutputEscaper.escapeSearchShaped(MARKUP));
        final Object out = AIViewToolOutputEscaper.escapeSearchShaped(new JSONArray().put("<x>"));
        assertTrue(out instanceof JSONArray);
        assertEquals("&lt;x&gt;", ((JSONArray) out).get(0));
        assertNull(AIViewToolOutputEscaper.escapeSearchShaped(null));
    }

    /**
     * Scenario: a plain immutable Map in the search shape (error payload from SearchTool today).
     * When escapeSearchShaped is applied
     * Then it returns an escaped JSONObject without throwing. (Callers route error payloads through
     * deepEscape; this only proves the search-shaped path is total over immutable maps.)
     */
    @Test
    public void escapeSearchShaped_immutableMap_doesNotThrow() {
        final Object out = AIViewToolOutputEscaper.escapeSearchShaped(Map.of("error", "<e>", "total", 0));

        assertTrue(out instanceof JSONObject);
        assertEquals("&lt;e&gt;", ((JSONObject) out).getString("error"));
        assertEquals(0, ((JSONObject) out).get("total"));
    }

    // ------------------------------------------------------------------------- helpers

    private static JSONObject providerShaped(final String content) {
        final JSONObject message = new JSONObject().put("role", "assistant").put("content", content);
        final JSONObject choice = new JSONObject().put("index", 0).put("message", message).put("finish_reason", "stop");
        return new JSONObject()
                .put("id", "chatcmpl-1")
                .put("model", "m<1>")
                .put("ok", Boolean.TRUE)
                .put("choices", new JSONArray().put(choice));
    }

    private static JSONObject searchShaped(final String query, final String title, final String body,
                                           final String extractedText, final String operator,
                                           final Float distance, final Integer total,
                                           final String timeToEmbeddings, final JSONObject openAiResponse) {
        final JSONObject match = new JSONObject().put("distance", distance).put("extractedText", extractedText);
        final JSONObject result = new JSONObject()
                .put("title", title)
                .put("body", body)
                .put("categories", new JSONArray().put("<cat>"))
                .put("matches", new JSONArray().put(match));
        final JSONObject payload = new JSONObject()
                .put("query", query)
                .put("dotCMSResults", new JSONArray().put(result))
                .put("total", total)
                .put("count", 3)
                .put("limit", 50)
                .put("offset", 0)
                .put("threshold", 0.5f)
                .put("operator", operator)
                .put("timeToEmbeddings", timeToEmbeddings);
        if (openAiResponse != null) {
            payload.put("openAiResponse", openAiResponse);
        }
        return payload;
    }

    /** Same keys at every depth, same array lengths, same non-string leaf values. */
    private static void assertSameShape(final Object expected, final Object actual) {
        if (expected instanceof JSONObject) {
            assertTrue(actual instanceof JSONObject);
            final JSONObject e = (JSONObject) expected;
            final JSONObject a = (JSONObject) actual;
            assertEquals(e.keySet(), a.keySet());
            for (final Object key : e.keySet()) {
                assertSameShape(e.get((String) key), a.get((String) key));
            }
        } else if (expected instanceof JSONArray) {
            assertTrue(actual instanceof JSONArray);
            final JSONArray e = (JSONArray) expected;
            final JSONArray a = (JSONArray) actual;
            assertEquals(e.length(), a.length());
            for (int i = 0; i < e.length(); i++) {
                assertSameShape(e.get(i), a.get(i));
            }
        } else if (!(expected instanceof String)) {
            assertEquals(expected, actual);
        }
    }

    /** No string leaf anywhere contains a raw HTML-significant character. */
    private static void assertNoRawMarkup(final Object value) {
        if (value instanceof JSONObject) {
            for (final Object key : ((JSONObject) value).keySet()) {
                assertNoRawMarkup(((JSONObject) value).get((String) key));
            }
        } else if (value instanceof JSONArray) {
            final JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                assertNoRawMarkup(array.get(i));
            }
        } else if (value instanceof String) {
            final String s = (String) value;
            assertFalse("raw markup in: " + s, s.contains("<") || s.contains(">") || s.contains("\"") || s.contains("'"));
        }
    }

}
