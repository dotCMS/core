package com.dotcms.tiptap;

import com.dotcms.UnitTestBase;
import com.dotcms.tiptap.TiptapMarkdown.Flavor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the {@code dotcms-*} fence vocabulary and the {@link Flavor#ROUNDTRIP} emission
 * mode (#36658): rich Block Editor nodes travel through Markdown as fenced code blocks with a
 * small JSON payload, cosmetic block attrs travel as {@code <!-- dotcms:attrs {…} -->}
 * comment decorations, and every invalid payload degrades to an ordinary code block — the
 * converter never throws on content.
 *
 * @author hassandotcms
 */
public class TiptapMarkdownFenceVocabularyTest extends UnitTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(final String s) {
        try {
            return MAPPER.readTree(s);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode firstBlock(final String markdown) {
        return TiptapMarkdown.toTiptap(markdown).path("content").path(0);
    }

    private static int count(final JsonNode node, final String type) {
        int c = type.equals(node.path("type").asText()) ? 1 : 0;
        for (final JsonNode child : node.path("content")) {
            c += count(child, type);
        }
        return c;
    }

    private static JsonNode findFirst(final JsonNode node, final String type) {
        if (type.equals(node.path("type").asText())) {
            return node;
        }
        for (final JsonNode child : node.path("content")) {
            final JsonNode found = findFirst(child, type);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    // =====================================================================
    // Flavor plumbing
    // =====================================================================

    @Test
    public void default_overloads_are_readable_flavor() {
        final JsonNode doc = json("{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"youtube\",\"attrs\":{\"src\":\"https://youtu.be/a\"}},"
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"hi\"}]}]}");
        assertEquals(TiptapMarkdown.toMarkdown(doc), TiptapMarkdown.toMarkdown(doc, Flavor.READABLE));
        assertEquals(TiptapMarkdown.toMarkdown(doc), TiptapMarkdown.toMarkdown(doc, null));
    }

    @Test
    public void readable_flavor_emits_no_fences_or_decorations_for_rich_docs() {
        final JsonNode doc = json("{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"dotVideo\",\"attrs\":{\"src\":\"/dA/v.mp4\"}},"
                + "{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"center\"},\"content\":[{\"type\":\"text\",\"text\":\"x\"}]}]}");
        final String md = TiptapMarkdown.toMarkdown(doc, Flavor.READABLE);
        assertFalse(md.contains("dotcms-"));
        assertFalse(md.contains("dotcms:attrs"));
    }

    // =====================================================================
    // Round-trip fixed point (§7.1-2)
    // =====================================================================

    /**
     * {@code toTiptap(toMarkdown(doc, ROUNDTRIP))} preserves every block for a document
     * combining the plain whitelist with all six first-party rich nodes plus a custom type.
     * Asset nodes compare by identifier+languageId (the stored fat {@code attrs.data} is a
     * hydration cache the server rebuilds on read) and derived attrs (orientation) are
     * excluded by never being emitted.
     */
    @Test
    public void roundtrip_fixed_point_for_all_rich_nodes() {
        final JsonNode doc = json("{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"heading\",\"attrs\":{\"level\":2},\"content\":[{\"type\":\"text\",\"text\":\"Title\"}]},"
                + "{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"identifier\":\"id-a\",\"languageId\":1,"
                +     "\"title\":\"Hydrated\",\"inode\":\"fat1\",\"hostName\":\"demo\",\"contentType\":\"Blog\"}}},"
                + "{\"type\":\"dotVideo\",\"attrs\":{\"src\":\"/dA/v.mp4\",\"mimeType\":\"video/mp4\",\"width\":640,\"height\":360,"
                +     "\"orientation\":\"horizontal\",\"data\":{\"identifier\":\"id-b\",\"languageId\":1}}},"
                + "{\"type\":\"youtube\",\"attrs\":{\"src\":\"https://youtu.be/abc\",\"start\":30,\"width\":640,\"height\":480}},"
                + "{\"type\":\"aiContent\",\"attrs\":{\"content\":\"gen text\"}},"
                + "{\"type\":\"gridBlock\",\"attrs\":{\"columns\":[4,8]},\"content\":["
                +     "{\"type\":\"gridColumn\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"left\"}]}]},"
                +     "{\"type\":\"gridColumn\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"right\"}]}]}]},"
                + "{\"type\":\"myCustomBlock\",\"attrs\":{\"foo\":\"bar\"},\"content\":["
                +     "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"inner\"}]}]},"
                + "{\"type\":\"dotImage\",\"attrs\":{\"src\":\"/dA/i.png\",\"alt\":\"a\",\"href\":\"https://x\","
                +     "\"data\":{\"identifier\":\"id-c\",\"languageId\":1,\"title\":\"img\"}}},"
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"outro\"}]}]}");

        final String md = TiptapMarkdown.toMarkdown(doc, Flavor.ROUNDTRIP);
        final JsonNode back = TiptapMarkdown.toTiptap(md);

        assertEquals(doc.path("content").size(), back.path("content").size());
        for (int i = 0; i < doc.path("content").size(); i++) {
            assertEquals("block " + i + " type",
                    doc.path("content").path(i).path("type").asText(),
                    back.path("content").path(i).path("type").asText());
        }
        // Asset identity survives.
        assertEquals("id-a", findFirst(back, "dotContent").path("attrs").path("data").path("identifier").asText());
        assertEquals(1, findFirst(back, "dotContent").path("attrs").path("data").path("languageId").asInt());
        assertEquals("id-b", findFirst(back, "dotVideo").path("attrs").path("data").path("identifier").asText());
        assertEquals("id-c", findFirst(back, "dotImage").path("attrs").path("data").path("identifier").asText());
        // Fat hydration cache is NOT round-tripped; the reference is.
        assertFalse(findFirst(back, "dotContent").path("attrs").path("data").has("inode"));
        // Derived attr is never emitted.
        assertFalse(findFirst(back, "dotVideo").path("attrs").has("orientation"));
        // Scalar embed attrs survive exactly.
        assertEquals(30, findFirst(back, "youtube").path("attrs").path("start").asInt());
        assertEquals("gen text", findFirst(back, "aiContent").path("attrs").path("content").asText());
        // Grid survives verbatim: columns and both children.
        final JsonNode grid = findFirst(back, "gridBlock");
        assertEquals(4, grid.path("attrs").path("columns").path(0).asInt());
        assertEquals(2, grid.path("content").size());
        // Custom node survives verbatim.
        assertEquals("bar", findFirst(back, "myCustomBlock").path("attrs").path("foo").asText());
    }

    /**
     * The emitted Markdown reaches a fixed point after one normalizing cycle. (Pass 1 may
     * differ from pass 2 by exactly the display-only courtesy fields — {@code title} on a
     * {@code dotcms-content} fence is emitted from hydrated data but ignored on parse.)
     */
    @Test
    public void roundtrip_reaches_a_fixed_point_after_one_cycle() {
        final JsonNode doc = json("{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"identifier\":\"id-a\",\"languageId\":1,\"title\":\"T\"}}},"
                + "{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"center\"},\"content\":[{\"type\":\"text\",\"text\":\"c\"}]}]}");
        final String md1 = TiptapMarkdown.toMarkdown(doc, Flavor.ROUNDTRIP);
        final String md2 = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap(md1), Flavor.ROUNDTRIP);
        final String md3 = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap(md2), Flavor.ROUNDTRIP);
        assertEquals(md2, md3);
        assertTrue("identity must survive every pass", md2.contains("\"identifier\":\"id-a\""));
    }

    // =====================================================================
    // Emission specifics (§2.3)
    // =====================================================================

    @Test
    public void plain_dotimage_stays_plain_markdown_in_roundtrip() {
        final JsonNode doc = json("{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"dotImage\",\"attrs\":{\"src\":\"https://ext/p.png\",\"alt\":\"plain\",\"title\":\"cap\"}}]}");
        assertEquals("![plain](https://ext/p.png \"cap\")", TiptapMarkdown.toMarkdown(doc, Flavor.ROUNDTRIP));
    }

    @Test
    public void fat_asset_data_is_trimmed_inside_grid_fences() {
        final JsonNode doc = json("{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"gridBlock\",\"attrs\":{\"columns\":[6,6]},\"content\":["
                + "{\"type\":\"gridColumn\",\"content\":[{\"type\":\"dotContent\",\"attrs\":{\"data\":{"
                +     "\"identifier\":\"id-x\",\"languageId\":2,\"title\":\"Keep\",\"inode\":\"fat\",\"hostName\":\"demo\"}}}]},"
                + "{\"type\":\"gridColumn\",\"content\":[{\"type\":\"paragraph\"}]}]}]}");
        final String md = TiptapMarkdown.toMarkdown(doc, Flavor.ROUNDTRIP);
        assertTrue(md.contains("\"identifier\":\"id-x\""));
        assertTrue("courtesy title kept for dotContent", md.contains("\"title\":\"Keep\""));
        assertFalse("hydration cache must not leak into the fence", md.contains("inode"));
        assertFalse(md.contains("hostName"));
    }

    @Test
    public void merged_cell_table_travels_as_node_fence_in_roundtrip() {
        final JsonNode doc = json("{\"type\":\"doc\",\"content\":[{\"type\":\"table\",\"content\":["
                + "{\"type\":\"tableRow\",\"content\":[{\"type\":\"tableHeader\","
                + "\"attrs\":{\"colspan\":2,\"rowspan\":1,\"colwidth\":null},"
                + "\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"merged\"}]}]}]}]}]}");
        final String md = TiptapMarkdown.toMarkdown(doc, Flavor.ROUNDTRIP);
        assertTrue(md.startsWith("```dotcms-node"));
        final JsonNode back = TiptapMarkdown.toTiptap(md);
        assertEquals(2, findFirst(back, "tableHeader").path("attrs").path("colspan").asInt());
    }

    @Test
    public void unmerged_table_stays_a_pipe_table_in_roundtrip() {
        final JsonNode doc = json("{\"type\":\"doc\",\"content\":[{\"type\":\"table\",\"content\":["
                + "{\"type\":\"tableRow\",\"content\":[{\"type\":\"tableHeader\","
                + "\"attrs\":{\"colspan\":1,\"rowspan\":1,\"colwidth\":null},"
                + "\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"h\"}]}]}]}]}]}");
        final String md = TiptapMarkdown.toMarkdown(doc, Flavor.ROUNDTRIP);
        assertTrue(md.startsWith("|"));
    }

    @Test
    public void ai_content_with_backtick_runs_escalates_the_fence() {
        final JsonNode doc = json("{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"aiContent\",\"attrs\":{\"content\":\"uses ```three``` ticks\"}}]}");
        final String md = TiptapMarkdown.toMarkdown(doc, Flavor.ROUNDTRIP);
        assertTrue("fence must be longer than the payload's backtick run", md.startsWith("````dotcms-ai"));
        assertEquals("uses ```three``` ticks",
                findFirst(TiptapMarkdown.toTiptap(md), "aiContent").path("attrs").path("content").asText());
    }

    @Test
    public void dotcontent_without_identifier_falls_back_to_readable_title() {
        final JsonNode doc = json("{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"title\":\"No Ref\"}}}]}");
        assertEquals("No Ref", TiptapMarkdown.toMarkdown(doc, Flavor.ROUNDTRIP));
    }

    // =====================================================================
    // Authoring direction (§7.1-3): hand-written Markdown -> expected node
    // =====================================================================

    @Test
    public void authoring_dotcms_content_fence() {
        final JsonNode block = firstBlock("```dotcms-content\n"
                + "{\"identifier\": \"2d5d1c4c-3557-4bc2-a067-fca9a3f4a2a5\", \"languageId\": 2, \"title\": \"Q3\"}\n"
                + "```");
        assertEquals("dotContent", block.path("type").asText());
        final JsonNode data = block.path("attrs").path("data");
        assertEquals("2d5d1c4c-3557-4bc2-a067-fca9a3f4a2a5", data.path("identifier").asText());
        assertEquals(2, data.path("languageId").asInt());
        assertFalse("title is display-only and ignored on parse", data.has("title"));
    }

    @Test
    public void authoring_language_id_defaults_to_one() {
        final JsonNode block = firstBlock("```dotcms-content\n{\"identifier\": \"abc\"}\n```");
        assertEquals(1, block.path("attrs").path("data").path("languageId").asInt());
    }

    @Test
    public void authoring_dotcms_image_fence_with_identifier_and_decorations() {
        final JsonNode block = firstBlock("```dotcms-image\n"
                + "{\"identifier\": \"img-1\", \"alt\": \"a\", \"href\": \"https://x\", \"target\": \"_blank\","
                + " \"textWrap\": \"wrap-left\", \"textAlign\": \"center\"}\n```");
        assertEquals("dotImage", block.path("type").asText());
        assertEquals("img-1", block.path("attrs").path("data").path("identifier").asText());
        assertEquals("_blank", block.path("attrs").path("target").asText());
        assertEquals("wrap-left", block.path("attrs").path("textWrap").asText());
    }

    @Test
    public void authoring_dotcms_video_fence_by_src_only() {
        final JsonNode block = firstBlock("```dotcms-video\n"
                + "{\"src\": \"/dA/v.mp4\", \"mimeType\": \"video/mp4\", \"width\": 640, \"height\": 360}\n```");
        assertEquals("dotVideo", block.path("type").asText());
        assertEquals(640, block.path("attrs").path("width").asInt());
        assertFalse("orientation is editor-derived, never set by the parser",
                block.path("attrs").has("orientation"));
        assertFalse(block.path("attrs").has("data"));
    }

    @Test
    public void authoring_dotcms_youtube_fence() {
        final JsonNode block = firstBlock("```dotcms-youtube\n"
                + "{\"src\": \"https://www.youtube.com/watch?v=x\", \"start\": 30}\n```");
        assertEquals("youtube", block.path("type").asText());
        assertEquals(30, block.path("attrs").path("start").asInt());
    }

    @Test
    public void authoring_dotcms_grid_fence() {
        final JsonNode block = firstBlock("```dotcms-grid\n"
                + "{\"type\":\"gridBlock\",\"attrs\":{\"columns\":[4,8]},\"content\":["
                + "{\"type\":\"gridColumn\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"l\"}]}]},"
                + "{\"type\":\"gridColumn\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"r\"}]}]}]}\n```");
        assertEquals("gridBlock", block.path("type").asText());
        assertEquals(2, block.path("content").size());
    }

    @Test
    public void authoring_generic_dotcms_node_fence() {
        final JsonNode block = firstBlock("```dotcms-node\n"
                + "{\"type\": \"myCustomBlock\", \"attrs\": {\"foo\": \"bar\"}}\n```");
        assertEquals("myCustomBlock", block.path("type").asText());
        assertEquals("bar", block.path("attrs").path("foo").asText());
    }

    @Test
    public void fences_parse_inside_blockquotes_and_lists() {
        final JsonNode quoted = TiptapMarkdown.toTiptap("> ```dotcms-content\n> {\"identifier\": \"q1\"}\n> ```");
        assertNotNull(findFirst(quoted, "dotContent"));
        final JsonNode listed = TiptapMarkdown.toTiptap("- item\n\n  ```dotcms-content\n  {\"identifier\": \"l1\"}\n  ```");
        assertNotNull(findFirst(listed, "dotContent"));
    }

    /** Every inbound Markdown may carry fences — parsing has no mode or opt-in. */
    @Test
    public void parsing_needs_no_opt_in() {
        assertEquals("dotContent", firstBlock("```dotcms-content\n{\"identifier\":\"a\"}\n```").path("type").asText());
    }

    // =====================================================================
    // Validation matrix (§7.1-4): every bad payload degrades to codeBlock
    // =====================================================================

    private static void assertDegradesToCodeBlock(final String markdown, final String reason) {
        final JsonNode block = firstBlock(markdown);
        assertEquals(reason, "codeBlock", block.path("type").asText());
    }

    @Test
    public void validation_matrix_degrades_to_code_block() {
        assertDegradesToCodeBlock("```dotcms-bogus\n{\"x\": 1}\n```", "unknown dotcms- label");
        assertDegradesToCodeBlock("```dotcms-content\nnot json {{{\n```", "unparseable payload");
        assertDegradesToCodeBlock("```dotcms-content\n[1,2]\n```", "payload not an object");
        assertDegradesToCodeBlock("```dotcms-content\n{\"languageId\": 1}\n```", "missing identifier");
        assertDegradesToCodeBlock("```dotcms-content\n{\"identifier\": 42}\n```", "identifier wrong type");
        assertDegradesToCodeBlock("```dotcms-content\n{\"identifier\": \"  \"}\n```", "blank identifier");
        assertDegradesToCodeBlock("```dotcms-image\n{\"alt\": \"no ref\"}\n```", "image without identifier or src");
        assertDegradesToCodeBlock("```dotcms-video\n{\"mimeType\": \"video/mp4\"}\n```", "video without identifier or src");
        assertDegradesToCodeBlock("```dotcms-youtube\n{\"start\": 5}\n```", "youtube without src");
        assertDegradesToCodeBlock("```dotcms-ai\n{\"content\": 42}\n```", "ai content wrong type");
        assertDegradesToCodeBlock("```dotcms-node\n{\"type\": \"doc\", \"content\": []}\n```", "doc smuggling");
        assertDegradesToCodeBlock("```dotcms-node\n{\"type\": \"x\", \"content\": [{\"type\": \"doc\"}]}\n```",
                "nested doc smuggling");
        assertDegradesToCodeBlock("```dotcms-node\n{\"type\": \"\"}\n```", "blank type");
        assertDegradesToCodeBlock("```dotcms-node\n{\"attrs\": {}}\n```", "missing type");
        assertDegradesToCodeBlock("```dotcms-node\n{\"type\": \"p\", \"attrs\": \"notobject\"}\n```",
                "attrs wrong shape");
        assertDegradesToCodeBlock("```dotcms-node\n{\"type\": \"p\", \"content\": {}}\n```", "content wrong shape");
        assertDegradesToCodeBlock("```dotcms-grid\n{\"type\": \"paragraph\"}\n```", "grid label with non-grid type");
        assertDegradesToCodeBlock("```dotcms-grid\n{\"type\":\"gridBlock\",\"content\":[{\"type\":\"gridColumn\"}]}\n```",
                "grid must have exactly two columns");
        assertDegradesToCodeBlock("```dotcms-grid\n{\"type\":\"gridBlock\",\"attrs\":{\"columns\":\"6,6\"},"
                + "\"content\":[{\"type\":\"gridColumn\"},{\"type\":\"gridColumn\"}]}\n```", "columns wrong shape");
    }

    @Test
    public void oversized_payload_degrades_to_code_block() {
        final String big = "{\"identifier\": \"a\", \"pad\": \"" + "x".repeat(70 * 1024) + "\"}";
        assertDegradesToCodeBlock("```dotcms-content\n" + big + "\n```", "payload above the 64KB cap");
    }

    @Test
    public void type_is_hard_coded_per_label_no_smuggling() {
        // A "type" field inside a non-verbatim payload is ignored, never honored.
        final JsonNode block = firstBlock("```dotcms-content\n{\"identifier\": \"a\", \"type\": \"paragraph\"}\n```");
        assertEquals("dotContent", block.path("type").asText());
    }

    @Test
    public void deeply_nested_verbatim_payload_degrades() {
        final StringBuilder payload = new StringBuilder();
        final int depth = 80; // above the 64-node depth cap
        for (int i = 0; i < depth; i++) {
            payload.append("{\"type\":\"x\",\"content\":[");
        }
        payload.append("{\"type\":\"x\"}");
        for (int i = 0; i < depth; i++) {
            payload.append("]}");
        }
        assertDegradesToCodeBlock("```dotcms-node\n" + payload + "\n```", "past the depth cap");
    }

    @Test
    public void degraded_fence_keeps_label_and_body_as_code_block() {
        final JsonNode block = firstBlock("```dotcms-bogus\nsome body\n```");
        assertEquals("dotcms-bogus", block.path("attrs").path("language").asText());
        assertEquals("some body", block.path("content").path(0).path("text").asText());
    }

    // =====================================================================
    // dotcms:attrs decorations (§2.4, §7.1-5)
    // =====================================================================

    @Test
    public void decoration_applies_to_next_paragraph() {
        final JsonNode doc = TiptapMarkdown.toTiptap(
                "<!-- dotcms:attrs {\"textAlign\":\"center\"} -->\n\ncentered text\n");
        assertEquals(1, doc.path("content").size());
        final JsonNode p = doc.path("content").path(0);
        assertEquals("paragraph", p.path("type").asText());
        assertEquals("center", p.path("attrs").path("textAlign").asText());
    }

    @Test
    public void decoration_applies_to_next_heading_and_structural_attr_wins() {
        final JsonNode doc = TiptapMarkdown.toTiptap(
                "<!-- dotcms:attrs {\"textAlign\":\"right\",\"level\":99} -->\n\n## H\n");
        final JsonNode h = doc.path("content").path(0);
        assertEquals("heading", h.path("type").asText());
        assertEquals("right", h.path("attrs").path("textAlign").asText());
        assertEquals("the parsed heading level must beat a decoration key of the same name",
                2, h.path("attrs").path("level").asInt());
    }

    @Test
    public void orphaned_decoration_at_end_of_doc_is_dropped() {
        final JsonNode doc = TiptapMarkdown.toTiptap("para\n\n<!-- dotcms:attrs {\"textAlign\":\"center\"} -->\n");
        assertEquals(1, doc.path("content").size());
        assertFalse(doc.path("content").path(0).has("attrs"));
    }

    @Test
    public void duplicated_decoration_last_one_wins() {
        final JsonNode doc = TiptapMarkdown.toTiptap(
                "<!-- dotcms:attrs {\"textAlign\":\"center\"} -->\n\n"
                + "<!-- dotcms:attrs {\"textAlign\":\"right\"} -->\n\npara\n");
        assertEquals(1, doc.path("content").size());
        assertEquals("right", doc.path("content").path(0).path("attrs").path("textAlign").asText());
    }

    @Test
    public void mangled_decoration_json_is_dropped_block_unaffected() {
        final JsonNode doc = TiptapMarkdown.toTiptap("<!-- dotcms:attrs {bad json} -->\n\npara\n");
        assertEquals(1, doc.path("content").size());
        assertFalse(doc.path("content").path(0).has("attrs"));
    }

    @Test
    public void decoration_rejects_non_scalar_values_and_bad_keys() {
        final JsonNode nested = TiptapMarkdown.toTiptap(
                "<!-- dotcms:attrs {\"style\":{\"color\":\"red\"}} -->\n\npara\n");
        assertFalse(nested.path("content").path(0).has("attrs"));
        final JsonNode badKey = TiptapMarkdown.toTiptap(
                "<!-- dotcms:attrs {\"1bad key!\":\"x\"} -->\n\npara\n");
        assertFalse(badKey.path("content").path(0).has("attrs"));
    }

    /**
     * The save path's HTML-vs-Markdown router must not mistake a leading decoration comment
     * for an HTML fragment (found live: the {@code <!--} start matched the HTML regex and the
     * whole document went to the HTML converter, silently dropping the decoration).
     */
    @Test
    public void leading_attrs_comment_is_recognized_as_markdown_vocabulary() {
        assertTrue(TiptapMarkdown.startsWithDotcmsAttrsComment(
                "<!-- dotcms:attrs {\"textAlign\":\"center\"} -->\n\npara"));
        assertTrue(TiptapMarkdown.startsWithDotcmsAttrsComment(
                "  \n<!--   dotcms:attrs {\"x\":1} -->"));
        assertFalse(TiptapMarkdown.startsWithDotcmsAttrsComment("<!-- plain comment -->"));
        assertFalse(TiptapMarkdown.startsWithDotcmsAttrsComment("<p>html</p>"));
        assertFalse(TiptapMarkdown.startsWithDotcmsAttrsComment("# markdown"));
        assertFalse(TiptapMarkdown.startsWithDotcmsAttrsComment(null));
    }

    @Test
    public void ordinary_html_comments_keep_legacy_behavior() {
        final JsonNode doc = TiptapMarkdown.toTiptap("<!-- plain comment -->\n\npara\n");
        assertEquals(2, doc.path("content").size());
        assertTrue(doc.path("content").path(0).path("content").path(0).path("text").asText()
                .contains("plain comment"));
    }

    @Test
    public void decoration_is_consumed_by_a_fence_not_applied_to_it() {
        final JsonNode doc = TiptapMarkdown.toTiptap(
                "<!-- dotcms:attrs {\"textAlign\":\"center\"} -->\n\n"
                + "```dotcms-content\n{\"identifier\":\"a\"}\n```\n\npara\n");
        final JsonNode content = doc.path("content").path(0);
        assertEquals("dotContent", content.path("type").asText());
        assertFalse("fence attrs are validated as a unit; decorations must not override them",
                content.path("attrs").has("textAlign"));
        assertFalse("consumed, not deferred to the paragraph",
                doc.path("content").path(1).has("attrs"));
    }

    @Test
    public void decoration_emission_skips_default_text_align_and_terminator_values() {
        final JsonNode defaultAlign = json("{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"text\":\"x\"}]}]}");
        assertEquals("x", TiptapMarkdown.toMarkdown(defaultAlign, Flavor.ROUNDTRIP));

        final JsonNode terminator = json("{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"a-->b\"},\"content\":[{\"type\":\"text\",\"text\":\"x\"}]}]}");
        assertEquals("a value containing --> cannot travel in a comment; skip the decoration",
                "x", TiptapMarkdown.toMarkdown(terminator, Flavor.ROUNDTRIP));
    }

    // =====================================================================
    // Guard predicate (§7.1-6)
    // =====================================================================

    @Test
    public void representable_matrix() {
        // Plain constructs: representable.
        assertTrue(TiptapMarkdown.isMarkdownRepresentable(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"},{\"type\":\"table\",\"content\":["
                + "{\"type\":\"tableRow\",\"content\":[{\"type\":\"tableCell\",\"attrs\":{\"colspan\":1,\"rowspan\":1}}]}]}]}"));
        // Whitelist bugfix: youtube and generic image ARE expressible in plain markdown.
        assertTrue(TiptapMarkdown.isMarkdownRepresentable(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"youtube\",\"attrs\":{\"src\":\"https://y\"}}]}"));
        assertTrue(TiptapMarkdown.isMarkdownRepresentable(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"image\",\"attrs\":{\"src\":\"https://x/p.png\"}}]}"));
        // Rich first-party nodes: not representable in plain markdown.
        assertFalse(TiptapMarkdown.isMarkdownRepresentable(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"identifier\":\"a\"}}}]}"));
        assertFalse(TiptapMarkdown.isMarkdownRepresentable(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"dotVideo\",\"attrs\":{\"src\":\"/dA/v.mp4\"}}]}"));
        assertFalse(TiptapMarkdown.isMarkdownRepresentable(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"gridBlock\",\"content\":[]}]}"));
        // Unknown custom types: not representable.
        assertFalse(TiptapMarkdown.isMarkdownRepresentable(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"myCustomBlock\"}]}"));
        // Merged cells: pipe tables cannot express them (#36658 §2.5).
        assertFalse(TiptapMarkdown.isMarkdownRepresentable(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"table\",\"content\":[{\"type\":\"tableRow\",\"content\":["
                + "{\"type\":\"tableCell\",\"attrs\":{\"colspan\":2,\"rowspan\":1}}]}]}]}"));
        assertFalse(TiptapMarkdown.isMarkdownRepresentable(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"table\",\"content\":[{\"type\":\"tableRow\",\"content\":["
                + "{\"type\":\"tableHeader\",\"attrs\":{\"colspan\":1,\"rowspan\":3}}]}]}]}"));
        // Null / blank / non-JSON: nothing to protect.
        assertTrue(TiptapMarkdown.isMarkdownRepresentable((String) null));
        assertTrue(TiptapMarkdown.isMarkdownRepresentable("  "));
        assertTrue(TiptapMarkdown.isMarkdownRepresentable("# just markdown"));
    }

    // =====================================================================
    // missingRichBlocks — the replacement-warning diff
    // =====================================================================

    private static final String EXISTING_RICH = "{\"type\":\"doc\",\"content\":["
            + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"intro\"}]},"
            + "{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"identifier\":\"id-1\",\"languageId\":1,\"title\":\"T\"}}},"
            + "{\"type\":\"dotVideo\",\"attrs\":{\"src\":\"/dA/v.mp4\",\"data\":{\"identifier\":\"id-2\"}}}]}";

    @Test
    public void missing_rich_blocks_lists_replaced_nodes() {
        final JsonNode incoming = TiptapMarkdown.toTiptap("# plain rewrite");
        final List<String> missing = TiptapMarkdown.missingRichBlocks(EXISTING_RICH, incoming);
        assertEquals(2, missing.size());
        assertTrue(missing.stream().anyMatch(s -> s.contains("dotContent") && s.contains("id-1")));
        assertTrue(missing.stream().anyMatch(s -> s.contains("dotVideo") && s.contains("id-2")));
    }

    @Test
    public void missing_rich_blocks_empty_when_fences_carry_them() {
        final JsonNode incoming = TiptapMarkdown.toTiptap("# rewrite\n\n"
                + "```dotcms-content\n{\"identifier\":\"id-1\",\"languageId\":1}\n```\n\n"
                + "```dotcms-video\n{\"identifier\":\"id-2\",\"src\":\"/dA/v.mp4\"}\n```");
        assertTrue(TiptapMarkdown.missingRichBlocks(EXISTING_RICH, incoming).isEmpty());
    }

    @Test
    public void missing_rich_blocks_counts_merged_cell_tables() {
        final String existing = "{\"type\":\"doc\",\"content\":[{\"type\":\"table\",\"content\":["
                + "{\"type\":\"tableRow\",\"content\":[{\"type\":\"tableCell\",\"attrs\":{\"colspan\":2}}]}]}]}";
        final List<String> missing = TiptapMarkdown.missingRichBlocks(existing,
                TiptapMarkdown.toTiptap("| a | b |\n| - | - |\n| c | d |"));
        assertEquals(1, missing.size());
        assertTrue(missing.get(0).contains("merged"));
    }

    /**
     * A youtube node is markdown-representable (plain link), but it is still an embed: a
     * rewrite that drops it must be reported — a link is not a player. Carrying it over as a
     * fence (or not having one) raises nothing.
     */
    @Test
    public void missing_rich_blocks_reports_dropped_youtube_embeds() {
        final String existing = "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"youtube\",\"attrs\":{\"src\":\"https://youtu.be/x\"}}]}";
        final List<String> missing =
                TiptapMarkdown.missingRichBlocks(existing, TiptapMarkdown.toTiptap("plain rewrite"));
        assertEquals(1, missing.size());
        assertTrue(missing.get(0).contains("youtube"));
        assertTrue(TiptapMarkdown.missingRichBlocks(existing, TiptapMarkdown.toTiptap(
                "```dotcms-youtube\n{\"src\":\"https://youtu.be/x\"}\n```")).isEmpty());
    }

    @Test
    public void missing_rich_blocks_ignores_plain_and_invalid_existing() {
        assertTrue(TiptapMarkdown.missingRichBlocks(null, TiptapMarkdown.toTiptap("x")).isEmpty());
        assertTrue(TiptapMarkdown.missingRichBlocks("not json", TiptapMarkdown.toTiptap("x")).isEmpty());
        assertTrue(TiptapMarkdown.missingRichBlocks(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}",
                TiptapMarkdown.toTiptap("x")).isEmpty());
    }

    // =====================================================================
    // Editor schema pin (§7.3 minimum bar)
    // =====================================================================

    /**
     * Every node type the fence parser can emit must be registered in BOTH Block Editor
     * schemas, or a server-converted document will fail to load and a subsequent editor save
     * wipes it (the #35728 {@code image} lesson). Registration sources:
     * legacy — {@code core-web/libs/block-editor/src/lib/shared/utils/prosemirror.utils.ts}
     * ({@code dot-block-editor.component.ts} {@code _customNodes}); new —
     * {@code core-web/libs/new-block-editor/src/lib/editor/extensions}. If this pin must
     * change, re-verify both registration lists first.
     */
    @Test
    public void fence_parser_emits_only_editor_registered_types() {
        final Set<String> editorRegistered = Set.of(
                "dotContent", "dotImage", "dotVideo", "youtube", "aiContent",
                "gridBlock", "gridColumn");
        final Set<String> emitted = new TreeSet<>();
        for (final String md : new String[] {
                "```dotcms-content\n{\"identifier\":\"a\"}\n```",
                "```dotcms-image\n{\"identifier\":\"a\"}\n```",
                "```dotcms-video\n{\"src\":\"/dA/v.mp4\"}\n```",
                "```dotcms-youtube\n{\"src\":\"https://y\"}\n```",
                "```dotcms-ai\n{\"content\":\"x\"}\n```",
                "```dotcms-grid\n{\"type\":\"gridBlock\",\"content\":[{\"type\":\"gridColumn\"},{\"type\":\"gridColumn\"}]}\n```"}) {
            collectTypes(firstBlock(md), emitted);
        }
        for (final String type : emitted) {
            assertTrue("fence parser emitted a type the editors do not register: " + type,
                    editorRegistered.contains(type));
        }
    }

    private static void collectTypes(final JsonNode node, final Set<String> types) {
        final String type = node.path("type").asText("");
        if (!type.isEmpty()) {
            types.add(type);
        }
        for (final JsonNode child : node.path("content")) {
            collectTypes(child, types);
        }
    }
}
