package com.dotmarketing.portlets.contentlet.transform.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.api.APIProvider;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link LongTextPreviewStrategy} (issue #37185, FR/AC-001, AC-007) — the strategy
 * that replaces WYSIWYG/TextArea/Story Block field values in a listing row's map with a
 * &lt;=150-character extracted plain-text preview.
 *
 * <p>{@code transform} is package-protected, so these tests call it directly rather than through
 * reflection, mirroring {@link StoryBlockViewStrategy}'s own construction (a mocked
 * {@link APIProvider} is enough — the strategy's logic never calls into the tool box).</p>
 */
public class LongTextPreviewStrategyTest {

    private static final String WYSIWYG_VAR = "ltpWysiwyg";
    private static final String TEXTAREA_VAR = "ltpTextArea";
    private static final String STORY_VAR = "ltpStory";

    private static Field mockField(final Class<? extends Field> type, final String variable) {
        final Field field = Mockito.mock(type);
        Mockito.when(field.variable()).thenReturn(variable);
        return field;
    }

    private static ContentType mockContentType(final List<Field> wysiwygFields,
            final List<Field> textAreaFields, final List<Field> storyBlockFields) {
        final ContentType contentType = Mockito.mock(ContentType.class);
        Mockito.when(contentType.id()).thenReturn("content-type-1");
        Mockito.when(contentType.fields(WysiwygField.class)).thenReturn(wysiwygFields);
        Mockito.when(contentType.fields(TextAreaField.class)).thenReturn(textAreaFields);
        Mockito.when(contentType.fields(StoryBlockField.class)).thenReturn(storyBlockFields);
        return contentType;
    }

    private static Contentlet mockContentlet(final ContentType contentType) {
        final Contentlet contentlet = Mockito.mock(Contentlet.class);
        Mockito.when(contentlet.getContentType()).thenReturn(contentType);
        Mockito.when(contentlet.getIdentifier()).thenReturn("identifier-1");
        return contentlet;
    }

    private static LongTextPreviewStrategy newStrategy() {
        return new LongTextPreviewStrategy(Mockito.mock(APIProvider.class));
    }

    // --- T010: WYSIWYG/TextArea -- HTML stripped, plain text truncated ------------------------

    /**
     * The map already carries the raw stored HTML under the field's variable name (the base map
     * is a copy of the contentlet's own field map, seeded before any strategy runs). This must be
     * replaced with Jsoup-extracted plain text, truncated to 150 characters -- not 150 characters
     * of the raw HTML with tags still embedded.
     */
    @Test
    public void transform_wysiwygField_htmlStrippedAndTruncatedToPlainText() throws Exception {
        final Field wysiwygField = mockField(WysiwygField.class, WYSIWYG_VAR);
        final ContentType contentType = mockContentType(List.of(wysiwygField), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        final String longBody = "<p>" + "word ".repeat(60) + "</p>";
        final Map<String, Object> map = new HashMap<>();
        map.put(WYSIWYG_VAR, longBody);

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        final Object result = map.get(WYSIWYG_VAR);
        assertTrue("Result must be a String", result instanceof String);
        final String preview = (String) result;
        assertTrue("Preview must be <=150 chars", preview.length() <= 150);
        assertFalse("Preview must not contain HTML tags", preview.contains("<") || preview.contains(">"));
        assertTrue("Preview must contain the extracted plain text", preview.startsWith("word word"));
    }

    /** Same extraction/truncation rule applies to TextArea fields, not just WYSIWYG. */
    @Test
    public void transform_textAreaField_htmlStrippedAndTruncatedToPlainText() throws Exception {
        final Field textAreaField = mockField(TextAreaField.class, TEXTAREA_VAR);
        final ContentType contentType = mockContentType(List.of(), List.of(textAreaField), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        final Map<String, Object> map = new HashMap<>();
        map.put(TEXTAREA_VAR, "<b>Short</b> body");

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        assertEquals("Short body", map.get(TEXTAREA_VAR));
    }

    /** A short WYSIWYG value under 150 characters is not padded or altered beyond HTML stripping. */
    @Test
    public void transform_wysiwygField_shortValue_isNotTruncated() throws Exception {
        final Field wysiwygField = mockField(WysiwygField.class, WYSIWYG_VAR);
        final ContentType contentType = mockContentType(List.of(wysiwygField), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        final Map<String, Object> map = new HashMap<>();
        map.put(WYSIWYG_VAR, "<p>Hello world</p>");

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        assertEquals("Hello world", map.get(WYSIWYG_VAR));
    }

    // --- T011: Story Block -- recursive traversal + truncation, run after StoryBlockViewStrategy

    /**
     * By the time this strategy runs (declared after {@code STORY_BLOCK_VIEW} in the enum), the
     * map entry for a Story Block field is already the {@link LinkedHashMap} that
     * {@link StoryBlockViewStrategy} produced. The traversal must walk nested {@code content}
     * arrays/tables and collect every {@code text} leaf value into a single, truncated preview.
     */
    @Test
    public void transform_storyBlockField_nestedListsAndTables_extractsAndConcatenatesText()
            throws Exception {
        final Field storyField = mockField(StoryBlockField.class, STORY_VAR);
        final ContentType contentType = mockContentType(List.of(), List.of(), List.of(storyField));
        final Contentlet contentlet = mockContentlet(contentType);

        // A doc with a paragraph, a bullet list (two items) and a table cell, mirroring the
        // Story Block JSON shape StoryBlockViewStrategy produces.
        final Map<String, Object> textNode1 = Map.of("type", "text", "text", "Launch announcement");
        final Map<String, Object> paragraph = Map.of("type", "paragraph", "content", List.of(textNode1));

        final Map<String, Object> listItemText1 = Map.of("type", "text", "text", "First point");
        final Map<String, Object> listItemText2 = Map.of("type", "text", "text", "Second point");
        final Map<String, Object> listItem1 = Map.of("type", "listItem", "content",
                List.of(Map.of("type", "paragraph", "content", List.of(listItemText1))));
        final Map<String, Object> listItem2 = Map.of("type", "listItem", "content",
                List.of(Map.of("type", "paragraph", "content", List.of(listItemText2))));
        final Map<String, Object> bulletList = Map.of("type", "bulletList", "content",
                List.of(listItem1, listItem2));

        final Map<String, Object> tableCellText = Map.of("type", "text", "text", "Cell value");
        final Map<String, Object> tableCell = Map.of("type", "tableCell", "content",
                List.of(Map.of("type", "paragraph", "content", List.of(tableCellText))));
        final Map<String, Object> tableRow = Map.of("type", "tableRow", "content", List.of(tableCell));
        final Map<String, Object> table = Map.of("type", "table", "content", List.of(tableRow));

        final LinkedHashMap<String, Object> storyBlockDoc = new LinkedHashMap<>();
        storyBlockDoc.put("type", "doc");
        storyBlockDoc.put("content", List.of(paragraph, bulletList, table));

        final Map<String, Object> map = new HashMap<>();
        map.put(STORY_VAR, storyBlockDoc);

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        final Object result = map.get(STORY_VAR);
        assertTrue("Result must be a plain String, not the LinkedHashMap", result instanceof String);
        final String preview = (String) result;
        assertTrue(preview.contains("Launch announcement"));
        assertTrue(preview.contains("First point"));
        assertTrue(preview.contains("Second point"));
        assertTrue(preview.contains("Cell value"));
        assertTrue("Preview must be <=150 chars", preview.length() <= 150);
    }

    /**
     * {@link StoryBlockViewStrategy} falls back to the raw string when the field's value is not
     * valid JSON. The traversal must treat that raw string as plain text (truncate, don't throw).
     */
    @Test
    public void transform_storyBlockField_nonJsonFallbackString_truncatesWithoutThrowing()
            throws Exception {
        final Field storyField = mockField(StoryBlockField.class, STORY_VAR);
        final ContentType contentType = mockContentType(List.of(), List.of(), List.of(storyField));
        final Contentlet contentlet = mockContentlet(contentType);

        final Map<String, Object> map = new HashMap<>();
        map.put(STORY_VAR, "not valid json at all, just plain legacy text ".repeat(5));

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        final Object result = map.get(STORY_VAR);
        assertTrue(result instanceof String);
        assertTrue(((String) result).length() <= 150);
    }

    /**
     * {@link StoryBlockViewStrategy} leaves the map entry {@code null} when JSON parsing itself
     * throws. The traversal must not throw on {@code null} and must resolve to an empty preview.
     */
    @Test
    public void transform_storyBlockField_nullAfterParseFailure_doesNotThrow() throws Exception {
        final Field storyField = mockField(StoryBlockField.class, STORY_VAR);
        final ContentType contentType = mockContentType(List.of(), List.of(), List.of(storyField));
        final Contentlet contentlet = mockContentlet(contentType);

        final Map<String, Object> map = new HashMap<>();
        map.put(STORY_VAR, null);

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        assertEquals("", map.get(STORY_VAR));
    }

    /** No in-scope fields on the content type: the map passes through untouched. */
    @Test
    public void transform_noInScopeFields_mapUnchanged() throws Exception {
        final ContentType contentType = mockContentType(List.of(), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        final Map<String, Object> map = new HashMap<>();
        map.put("title", "Some title");

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        assertEquals(1, map.size());
        assertEquals("Some title", map.get("title"));
    }

    // --- T012: TransformOptions ordinal placement ----------------------------------------------

    /**
     * {@code LONG_TEXT_PREVIEW} must be declared after both {@code STORY_BLOCK_VIEW} and
     * {@code JSON_VIEW} so {@code EnumSet} iteration order (which {@code StrategyResolverImpl}
     * relies on) runs this strategy last, seeing the fully-decorated map. Guards against a future
     * enum reorder silently breaking that ordering.
     */
    @Test
    public void longTextPreview_ordinalIsAfterStoryBlockViewAndJsonView() {
        assertTrue("LONG_TEXT_PREVIEW must sort after STORY_BLOCK_VIEW",
                TransformOptions.LONG_TEXT_PREVIEW.ordinal() > TransformOptions.STORY_BLOCK_VIEW.ordinal());
        assertTrue("LONG_TEXT_PREVIEW must sort after JSON_VIEW",
                TransformOptions.LONG_TEXT_PREVIEW.ordinal() > TransformOptions.JSON_VIEW.ordinal());
    }

    // --- T013: defaultOptions unaffected (AC-007) -----------------------------------------------

    /**
     * {@code LONG_TEXT_PREVIEW} must never be part of the shared {@code defaultOptions} set --
     * every consumer that builds a transformer via {@code .defaultOptions()} (URL content map,
     * ContentResource, GraphQL, the Content Editor) must stay byte-identical to today. It is wired
     * opt-in, only at {@code BrowserAPIImpl#dotContentMap}'s specific call site.
     */
    @Test
    public void defaultOptions_neverIncludesLongTextPreview() {
        assertFalse("LONG_TEXT_PREVIEW must not be part of the shared defaultOptions set",
                DotContentletTransformerImpl.defaultOptions.contains(TransformOptions.LONG_TEXT_PREVIEW));
    }

    // --- T014: StrategyResolverImpl registers the new option-triggered strategy ----------------

    /**
     * Confirms {@code StrategyResolverImpl.resolveStrategies} actually resolves and returns a
     * {@link LongTextPreviewStrategy} instance when {@code LONG_TEXT_PREVIEW} is requested -- not
     * just that the class itself constructs.
     */
    @Test
    public void resolveStrategies_longTextPreviewOption_resolvesLongTextPreviewStrategy() {
        final StrategyResolverImpl resolver = new StrategyResolverImpl(Mockito.mock(APIProvider.class));

        final List<AbstractTransformStrategy> strategies = resolver.resolveStrategies(null,
                EnumSet.of(TransformOptions.LONG_TEXT_PREVIEW));

        assertTrue("Must resolve a LongTextPreviewStrategy instance",
                strategies.stream().anyMatch(s -> s instanceof LongTextPreviewStrategy));
    }

    /** Without the option, no LongTextPreviewStrategy is resolved. */
    @Test
    public void resolveStrategies_withoutLongTextPreviewOption_doesNotResolveIt() {
        final StrategyResolverImpl resolver = new StrategyResolverImpl(Mockito.mock(APIProvider.class));

        final List<AbstractTransformStrategy> strategies = resolver.resolveStrategies(null,
                EnumSet.of(TransformOptions.STORY_BLOCK_VIEW));

        assertTrue(strategies.stream().noneMatch(s -> s instanceof LongTextPreviewStrategy));
    }
}
