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
        final List<Field> allFields = new java.util.ArrayList<>();
        allFields.addAll(wysiwygFields);
        allFields.addAll(textAreaFields);
        allFields.addAll(storyBlockFields);
        Mockito.when(contentType.fields()).thenReturn(allFields);
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

    /**
     * Every Story Block field carries an untouched {@code <var>_raw} companion holding the full,
     * untruncated JSON schema (written upstream by {@code ContentletJsonAPIImpl} for consumers
     * that read it directly off the {@code Contentlet}, e.g. nested Block Editor reference
     * resolution). Left in a listing row, it rides alongside the trimmed preview and carries the
     * exact bytes this strategy exists to shed (issue #37185 QA follow-up: Rafael found both
     * {@code body} and {@code body_raw} in the search response for a Block Editor field).
     */
    @Test
    public void transform_storyBlockField_removesRawCompanionKey() throws Exception {
        final Field storyField = mockField(StoryBlockField.class, STORY_VAR);
        final ContentType contentType = mockContentType(List.of(), List.of(), List.of(storyField));
        final Contentlet contentlet = mockContentlet(contentType);

        final Map<String, Object> textNode = Map.of("type", "text", "text", "Launch announcement");
        final Map<String, Object> paragraph = Map.of("type", "paragraph", "content", List.of(textNode));
        final LinkedHashMap<String, Object> storyBlockDoc = new LinkedHashMap<>();
        storyBlockDoc.put("type", "doc");
        storyBlockDoc.put("content", List.of(paragraph));

        final Map<String, Object> map = new HashMap<>();
        map.put(STORY_VAR, storyBlockDoc);
        map.put(STORY_VAR + "_raw", "{\"type\":\"doc\",\"content\":[/* the full, untruncated schema */]}");

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        assertFalse("The _raw companion key must not survive into a trimmed listing row",
                map.containsKey(STORY_VAR + "_raw"));
        assertTrue("The trimmed preview itself must still be present",
                ((String) map.get(STORY_VAR)).contains("Launch announcement"));
    }

    /** A content type with no Story Block field at all must not touch any {@code _raw} key. */
    @Test
    public void transform_noStoryBlockFields_leavesUnrelatedRawKeyAlone() throws Exception {
        final Field wysiwygField = mockField(WysiwygField.class, WYSIWYG_VAR);
        final ContentType contentType = mockContentType(List.of(wysiwygField), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        final Map<String, Object> map = new HashMap<>();
        map.put(WYSIWYG_VAR, "<p>Hello world</p>");
        map.put("someOtherField_raw", "unrelated value");

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        assertEquals("unrelated value", map.get("someOtherField_raw"));
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

    /**
     * AC-008 (revised, issue #37185 QA follow-up): when a content type's title-source field is
     * itself a WYSIWYG or TextArea field (i.e. its variable is literally {@code "title"}), the
     * {@code title} map key must still be trimmed like any other in-scope field.
     *
     * <p>An earlier version of this test asserted the opposite -- that {@code title} must stay
     * untouched -- on the theory that {@code COMMON_PROPS}/{@code Contentlet#getTitle()} having
     * already populated it meant it should not be touched again. In practice {@code getTitle()}
     * returns that field's own raw value verbatim (no HTML stripping, no length bound), so
     * "already populated" just meant "holds the same raw HTML every other in-scope field starts
     * from" -- exempting it left a WYSIWYG/TextArea title at full, untruncated length in the
     * listing response, defeating this strategy's purpose for that one field and failing the
     * issue's own acceptance criterion ("a content type whose title field is itself a long-text
     * field still returns a correct title"). Confirmed against a live repro (QA content type "QA
     * 37185 Long Text Title").</p>
     */
    @Test
    public void transform_wysiwygFieldNamedTitle_trimsLikeAnyOtherInScopeField() throws Exception {
        final Field titleField = mockField(WysiwygField.class, "title");
        final ContentType contentType = mockContentType(List.of(titleField), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        // Simulates DefaultTransformStrategy/COMMON_PROPS having already run and populated
        // "title" from Contentlet#getTitle() -- untruncated, HTML markup and all, since
        // getTitle() does not itself strip HTML.
        final String realTitle = "<p>" + "word ".repeat(60) + "</p>";
        final Map<String, Object> map = new HashMap<>();
        map.put("title", realTitle);

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        final String preview = (String) map.get("title");
        assertTrue("The 'title' key must be trimmed to <=150 chars like any other in-scope field",
                preview.length() <= 150);
        assertFalse("Preview must not contain HTML tags",
                preview.contains("<") || preview.contains(">"));
        assertTrue("A truncated title must carry the truncation marker", preview.endsWith("…"));
    }

    /** A short WYSIWYG-backed title (under 150 chars) is trimmed of HTML but not truncated. */
    @Test
    public void transform_wysiwygFieldNamedTitle_shortValue_isNotTruncated() throws Exception {
        final Field titleField = mockField(WysiwygField.class, "title");
        final ContentType contentType = mockContentType(List.of(titleField), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        final Map<String, Object> map = new HashMap<>();
        map.put("title", "<p>Short title</p>");

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        assertEquals("Short title", map.get("title"));
    }

    /**
     * Dotbot review finding on PR #37663: {@code COMMON_PROPS} always writes an independent copy
     * of {@link Contentlet#getTitle()}'s raw value into the {@code "title"} key, regardless of the
     * title-source field's own variable name -- {@code DefaultTransformStrategy#addCommonProperties}
     * does {@code map.put("title", contentlet.getTitle())} unconditionally. When that field is
     * named e.g. {@code titleLongText} rather than exactly {@code "title"} (dotCMS's own
     * {@code Contentlet#getFieldWithVarStartingWithTitleWord} fallback), {@code applyPreview} only
     * trims {@code titleLongText}'s own key -- the separate {@code "title"} copy is a different map
     * entry entirely and would otherwise ride through at full length.
     */
    @Test
    public void transform_titleDerivedFromDifferentlyNamedWysiwygField_trimsBothKeys() throws Exception {
        final Field titleSourceField = mockField(WysiwygField.class, "titleLongText");
        final ContentType contentType = mockContentType(List.of(titleSourceField), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        final String longBody = "<p>" + "word ".repeat(60) + "</p>";
        final Map<String, Object> map = new HashMap<>();
        // Both keys start out holding the exact same raw HTML -- "titleLongText" as the field's
        // own entry, "title" as COMMON_PROPS's independent copy from Contentlet#getTitle().
        map.put("titleLongText", longBody);
        map.put("title", longBody);

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        final String fieldPreview = (String) map.get("titleLongText");
        final String titlePreview = (String) map.get("title");
        assertTrue("The field's own key must be trimmed", fieldPreview.length() <= 150);
        assertTrue("The separate 'title' copy must also be trimmed", titlePreview.length() <= 150);
        assertFalse("The 'title' copy must not contain HTML tags",
                titlePreview.contains("<") || titlePreview.contains(">"));
    }

    /**
     * A dedicated, short "title" field (a normal Text column, not WYSIWYG/TextArea/Story Block)
     * means no field's variable starts with "title" among the in-scope lists -- nothing to trim.
     */
    @Test
    public void transform_dedicatedShortTitleField_leavesTitleKeyAlone() throws Exception {
        final Field bodyField = mockField(WysiwygField.class, "body");
        final ContentType contentType = mockContentType(List.of(bodyField), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        final Map<String, Object> map = new HashMap<>();
        map.put("title", "A normal, short title");
        map.put("body", "<p>Short body</p>");

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        assertEquals("A normal, short title", map.get("title"));
    }

    /**
     * Dotbot review finding on PR #37663: a content type can carry a short, dedicated Text field
     * ({@code titleName}) that {@code Contentlet#getFieldWithVarStartingWithTitleWord} actually
     * resolves the title from, AND an unrelated WYSIWYG field ({@code titleBody}) whose variable
     * also happens to start with "title" but plays no part in title resolution. Trimming must
     * follow the field dotCMS would actually pick (the first "title"-prefixed field in content-type
     * order) rather than firing whenever ANY WYSIWYG/TextArea field's variable is title-prefixed --
     * otherwise a short plain-text title gets run through Jsoup HTML extraction it was never meant
     * to need, silently decoding any HTML entity it happens to contain.
     */
    @Test
    public void transform_shortTitleFieldPrecedesUnrelatedTitlePrefixedWysiwygField_leavesTitleKeyAlone()
            throws Exception {
        final Field titleNameField = mockField(Field.class, "titleName");
        final Field titleBodyField = mockField(WysiwygField.class, "titleBody");

        final ContentType contentType = Mockito.mock(ContentType.class);
        Mockito.when(contentType.id()).thenReturn("content-type-1");
        Mockito.when(contentType.fields(WysiwygField.class)).thenReturn(List.of(titleBodyField));
        Mockito.when(contentType.fields(TextAreaField.class)).thenReturn(List.of());
        Mockito.when(contentType.fields(StoryBlockField.class)).thenReturn(List.of());
        // titleName resolves first -- the same order Contentlet#getFieldWithVarStartingWithTitleWord
        // would see it in.
        Mockito.when(contentType.fields()).thenReturn(List.of(titleNameField, titleBodyField));

        final Contentlet contentlet = mockContentlet(contentType);

        final Map<String, Object> map = new HashMap<>();
        // A plain-text title carrying a literal HTML entity -- Jsoup would decode "&amp;" to "&" if
        // this were wrongly run through HTML extraction, silently changing a value that was never
        // HTML to begin with.
        map.put("title", "Smith &amp; Sons");
        map.put("titleName", "Smith &amp; Sons");
        map.put("titleBody", "<p>" + "word ".repeat(60) + "</p>");

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        assertEquals("The 'title' key must be left alone -- the resolved title source is a plain "
                        + "Text field, not the unrelated WYSIWYG field that merely shares the prefix",
                "Smith &amp; Sons", map.get("title"));
    }

    /**
     * A field that IS in scope (a WYSIWYG field on the content type) but whose key is entirely
     * absent from the row's map -- distinct from {@code transform_noInScopeFields_mapUnchanged},
     * which has no in-scope field at all -- must stay absent rather than gaining a synthesized
     * {@code ""} entry (found in review; the fix this test pins).
     */
    @Test
    public void transform_inScopeFieldAbsentFromMap_staysAbsent() throws Exception {
        final Field wysiwygField = mockField(WysiwygField.class, WYSIWYG_VAR);
        final ContentType contentType = mockContentType(List.of(wysiwygField), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        final Map<String, Object> map = new HashMap<>();
        map.put("title", "Some title");

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        assertFalse("A field absent from the row's map must stay absent, not gain a synthesized entry",
                map.containsKey(WYSIWYG_VAR));
    }

    // --- Truncation marker (found in review) ----------------------------------------------------

    /** A value longer than the 150-character bound must be marked as truncated. */
    @Test
    public void transform_wysiwygField_longValue_previewEndsWithTruncationMarker() throws Exception {
        final Field wysiwygField = mockField(WysiwygField.class, WYSIWYG_VAR);
        final ContentType contentType = mockContentType(List.of(wysiwygField), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        final Map<String, Object> map = new HashMap<>();
        map.put(WYSIWYG_VAR, "<p>" + "word ".repeat(60) + "</p>");

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        final String preview = (String) map.get(WYSIWYG_VAR);
        assertTrue("A truncated preview must end with the truncation marker", preview.endsWith("…"));
        assertTrue("Preview must still honor the <=150-character bound including the marker",
                preview.length() <= 150);
    }

    /** A value at or under the bound is a complete value, not a truncation -- no marker. */
    @Test
    public void transform_wysiwygField_shortValue_previewHasNoTruncationMarker() throws Exception {
        final Field wysiwygField = mockField(WysiwygField.class, WYSIWYG_VAR);
        final ContentType contentType = mockContentType(List.of(wysiwygField), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        final Map<String, Object> map = new HashMap<>();
        map.put(WYSIWYG_VAR, "<p>Hello world</p>");

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        assertFalse("A short, complete value must not carry the truncation marker",
                ((String) map.get(WYSIWYG_VAR)).endsWith("…"));
    }

    /**
     * A story block whose text-leaf traversal lands exactly on the 150-character bound, with more
     * text still pending in the tree, must still be marked as truncated -- the one-character gap
     * between {@code collectText}'s stop condition and {@code truncate}'s marker condition (found
     * in review).
     */
    @Test
    public void transform_storyBlockField_traversalStopsExactlyAtBound_stillMarksTruncation()
            throws Exception {
        final Field storyField = mockField(StoryBlockField.class, STORY_VAR);
        final ContentType contentType = mockContentType(List.of(), List.of(), List.of(storyField));
        final Contentlet contentlet = mockContentlet(contentType);

        // The first leaf alone lands collectText's running length at exactly the 150-character
        // bound. A pre-fix `>= MAX_PREVIEW_LENGTH` stop condition would end the traversal right
        // there without ever visiting the second leaf, so the dropped text left no trace and
        // `truncate` (seeing a text length of exactly 150, not greater than it) added no marker --
        // this is the exact bug this test pins the fix for.
        final Map<String, Object> firstLeaf = Map.of("type", "text", "text", "a".repeat(150));
        final Map<String, Object> secondLeaf = Map.of("type", "text", "text", "b".repeat(75));
        final Map<String, Object> paragraph1 = Map.of("type", "paragraph", "content", List.of(firstLeaf));
        final Map<String, Object> paragraph2 = Map.of("type", "paragraph", "content", List.of(secondLeaf));

        final LinkedHashMap<String, Object> storyBlockDoc = new LinkedHashMap<>();
        storyBlockDoc.put("type", "doc");
        storyBlockDoc.put("content", List.of(paragraph1, paragraph2));

        final Map<String, Object> map = new HashMap<>();
        map.put(STORY_VAR, storyBlockDoc);

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        final String preview = (String) map.get(STORY_VAR);
        assertTrue("A traversal that stopped with more text pending must still be marked truncated",
                preview.endsWith("…"));
        assertTrue("Preview must still honor the <=150-character bound including the marker",
                preview.length() <= 150);
    }

    /**
     * An emoji (a UTF-16 surrogate pair) straddling the 150-character cut boundary must not be
     * split into a lone, invalid surrogate at the end of the preview (found in review).
     */
    @Test
    public void transform_wysiwygField_emojiAtTruncationBoundary_isNotSplit() throws Exception {
        final Field wysiwygField = mockField(WysiwygField.class, WYSIWYG_VAR);
        final ContentType contentType = mockContentType(List.of(wysiwygField), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        // truncate()'s cut budget is 149 (150 minus the marker's length), and it inspects the
        // character immediately before that index -- 148 plain characters put the emoji's high
        // surrogate (a 2-char UTF-16 pair) exactly there, straddling the cut point. More text
        // follows so the value is genuinely truncated.
        final String body = "a".repeat(148) + "😀" + "more text after the emoji";
        final Map<String, Object> map = new HashMap<>();
        map.put(WYSIWYG_VAR, "<p>" + body + "</p>");

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        final String preview = (String) map.get(WYSIWYG_VAR);
        assertFalse("Preview must not end with a lone (invalid) high surrogate",
                Character.isHighSurrogate(preview.charAt(preview.length() - 2)));
    }

    /**
     * A WYSIWYG body that opens with a large non-text span (a base64 image data URI) can spend the
     * entire {@code HTML_PARSE_BUDGET} inside that one attribute value, so {@code Jsoup.text()} on
     * the narrow-budget prefix alone returns nothing. The strategy must retry with a wider budget
     * rather than ship an empty preview for a body that does have visible text further in (found in
     * review -- this was a real regression introduced by the budget itself).
     */
    @Test
    public void transform_wysiwygField_largeLeadingDataUri_stillExtractsTrailingText() throws Exception {
        final Field wysiwygField = mockField(WysiwygField.class, WYSIWYG_VAR);
        final ContentType contentType = mockContentType(List.of(wysiwygField), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        // A base64 payload well past HTML_PARSE_BUDGET (4096), so the narrow-budget prefix lands
        // entirely inside the attribute value with no visible text at all.
        final String base64Payload = "A".repeat(6000);
        final String html = "<img src=\"data:image/png;base64," + base64Payload + "\">"
                + "<p>Real visible text after the embedded image</p>";
        final Map<String, Object> map = new HashMap<>();
        map.put(WYSIWYG_VAR, html);

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        final String preview = (String) map.get(WYSIWYG_VAR);
        assertTrue("Preview must recover the trailing visible text rather than come back empty",
                preview.contains("Real visible text"));
    }

    /**
     * The budget-limited extract can land on exactly {@code MAX_PREVIEW_LENGTH} visible characters
     * while the raw HTML is longer than {@code HTML_PARSE_BUDGET} and genuinely carries more text
     * past the cut point -- the same one-character gap already closed for the Story Block path,
     * mirrored here: {@code truncate} alone cannot distinguish "150 characters and nothing more"
     * from "150 characters because that is all the budget-limited prefix had room for" (found in
     * review).
     */
    @Test
    public void transform_wysiwygField_exactBudgetBoundary_stillMarksTruncation() throws Exception {
        final Field wysiwygField = mockField(WysiwygField.class, WYSIWYG_VAR);
        final ContentType contentType = mockContentType(List.of(wysiwygField), List.of(), List.of());
        final Contentlet contentlet = mockContentlet(contentType);

        // Exactly 150 visible characters, then an HTML comment long enough that it is still open
        // (unclosed) at the HTML_PARSE_BUDGET (4096) cut point -- Jsoup contributes no text for an
        // unterminated comment, so the budget-limited extract comes back at exactly 150, even
        // though the paragraph after the comment is never seen by that first pass.
        final String html = "<p>" + "a".repeat(150) + "</p>"
                + "<!--" + "x".repeat(6000) + "-->"
                + "<p>more visible text after the cutoff</p>";
        final Map<String, Object> map = new HashMap<>();
        map.put(WYSIWYG_VAR, html);

        newStrategy().transform(contentlet, map, EnumSet.noneOf(TransformOptions.class), null);

        final String preview = (String) map.get(WYSIWYG_VAR);
        assertTrue("A budget-limited extract that landed exactly on the bound must still be "
                        + "marked as truncated, since the html itself was cut",
                preview.endsWith("…"));
        assertTrue("Preview must still honor the <=150-character bound including the marker",
                preview.length() <= 150);
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
    public void defaultOptions_neverIncludesLongTextPreview() throws Exception {
        // Package-private field on a different package (com.dotmarketing.portlets.contentlet.
        // transform, not this class's ...transform.strategy) -- read via reflection.
        final java.lang.reflect.Field defaultOptionsField = Class
                .forName("com.dotmarketing.portlets.contentlet.transform.DotContentletTransformerImpl")
                .getDeclaredField("defaultOptions");
        defaultOptionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final java.util.Set<TransformOptions> defaultOptions =
                (java.util.Set<TransformOptions>) defaultOptionsField.get(null);

        assertFalse("LONG_TEXT_PREVIEW must not be part of the shared defaultOptions set",
                defaultOptions.contains(TransformOptions.LONG_TEXT_PREVIEW));
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
