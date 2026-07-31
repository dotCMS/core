package com.dotcms.tiptap;

import com.dotcms.UnitTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the {@code <dotcms-*>} rich-node vocabulary on the HTML leg (#36659): the same
 * seven labels the Markdown fence vocabulary froze in #36658, spelled as namespaced custom
 * elements. Scalar payloads ride as hyphenated attributes, structured payloads
 * ({@code dotcms-ai}, {@code dotcms-grid}, {@code dotcms-node}) ride the fence-identical JSON
 * as the element body, and {@code <!-- dotcms:attrs {…} -->} comments decorate the next block.
 * The headline section is the parity suite: HTML input and Markdown input MUST produce
 * equal Tiptap JSON for the same rich content, because both carriers are validated by the
 * same code ({@code TiptapMarkdown.DotcmsFences}).
 *
 * @author hassandotcms
 */
public class TiptapHtmlRichNodeTest extends UnitTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String GRID_JSON =
            "{\"type\":\"gridBlock\",\"attrs\":{\"columns\":[8,4]},\"content\":["
                    + "{\"type\":\"gridColumn\",\"content\":[{\"type\":\"paragraph\",\"content\":"
                    + "[{\"type\":\"text\",\"text\":\"left\"}]}]},"
                    + "{\"type\":\"gridColumn\",\"content\":[{\"type\":\"paragraph\",\"content\":"
                    + "[{\"type\":\"text\",\"text\":\"right\"}]}]}]}";

    private static JsonNode json(final String s) {
        try {
            return MAPPER.readTree(s);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode convert(final String html) {
        return TiptapHtml.toTiptap(html);
    }

    private static JsonNode firstBlock(final String html) {
        return convert(html).path("content").path(0);
    }

    // =====================================================================
    // Authoring: each element produces the fence-identical node shape
    // =====================================================================

    @Test
    public void dotcms_content_maps_to_dotContent() {
        final JsonNode node = firstBlock(
                "<dotcms-content identifier=\"2d5d1c4c-aaaa\" language-id=\"2\"></dotcms-content>");
        assertEquals(json("{\"type\":\"dotContent\",\"attrs\":{\"data\":"
                + "{\"identifier\":\"2d5d1c4c-aaaa\",\"languageId\":2}}}"), node);
    }

    @Test
    public void dotcms_content_defaults_language_id() {
        final JsonNode node = firstBlock("<dotcms-content identifier=\"abc\"></dotcms-content>");
        assertEquals(1, node.path("attrs").path("data").path("languageId").asInt());
    }

    @Test
    public void dotcms_image_maps_hyphenated_attrs_to_camel_case() {
        final JsonNode node = firstBlock("<dotcms-image identifier=\"img-1\" language-id=\"1\" "
                + "src=\"/dA/img-1/photo.jpg\" alt=\"Team\" title=\"T\" href=\"https://dotcms.com\" "
                + "target=\"_blank\" text-wrap=\"wrap-right\" text-align=\"center\"></dotcms-image>");
        assertEquals("dotImage", node.path("type").asText());
        final JsonNode attrs = node.path("attrs");
        assertEquals("wrap-right", attrs.path("textWrap").asText());
        assertEquals("center", attrs.path("textAlign").asText());
        assertEquals("_blank", attrs.path("target").asText());
        assertEquals("img-1", attrs.path("data").path("identifier").asText());
        assertEquals(1, attrs.path("data").path("languageId").asInt());
        assertFalse("lowercased key must not leak into the node", node.toString().contains("text-wrap"));
    }

    @Test
    public void dotcms_image_src_only_carries_no_data() {
        final JsonNode node = firstBlock("<dotcms-image src=\"/dA/x.jpg\" alt=\"a\"></dotcms-image>");
        assertEquals("dotImage", node.path("type").asText());
        assertTrue(node.path("attrs").path("data").isMissingNode());
    }

    @Test
    public void dotcms_video_with_dimensions_and_no_orientation() {
        final JsonNode node = firstBlock("<dotcms-video identifier=\"v-1\" "
                + "src=\"https://demo.dotcms.com/dA/v-1/clip.mp4\" mime-type=\"video/mp4\" "
                + "width=\"1280\" height=\"720\"></dotcms-video>");
        assertEquals("dotVideo", node.path("type").asText());
        assertEquals("video/mp4", node.path("attrs").path("mimeType").asText());
        assertEquals(1280, node.path("attrs").path("width").asInt());
        assertEquals(720, node.path("attrs").path("height").asInt());
        assertTrue("orientation is editor-derived, never stored",
                node.path("attrs").path("orientation").isMissingNode());
        assertEquals("v-1", node.path("attrs").path("data").path("identifier").asText());
    }

    @Test
    public void dotcms_youtube_maps_src_and_timing() {
        final JsonNode node = firstBlock("<dotcms-youtube "
                + "src=\"https://www.youtube.com/watch?v=abc\" start=\"42\" width=\"640\" "
                + "height=\"480\"></dotcms-youtube>");
        assertEquals(json("{\"type\":\"youtube\",\"attrs\":{\"src\":"
                + "\"https://www.youtube.com/watch?v=abc\",\"start\":42,\"width\":640,"
                + "\"height\":480}}"), node);
    }

    @Test
    public void flattened_lowercase_attribute_names_accepted() {
        // A client authoring camelCase attributes has them lowercased by the HTML parse; the
        // flattened form must still bind (languageId= arrives as languageid=).
        final JsonNode node = firstBlock(
                "<dotcms-content identifier=\"abc\" languageid=\"3\"></dotcms-content>");
        assertEquals(3, node.path("attrs").path("data").path("languageId").asInt());
        final JsonNode video = firstBlock(
                "<dotcms-video src=\"/dA/v.mp4\" mimetype=\"video/mp4\"></dotcms-video>");
        assertEquals("video/mp4", video.path("attrs").path("mimeType").asText());
    }

    @Test
    public void dotcms_ai_json_body() {
        final JsonNode node = firstBlock("<dotcms-ai>{\"content\":\"generated text\"}</dotcms-ai>");
        assertEquals(json("{\"type\":\"aiContent\",\"attrs\":{\"content\":\"generated text\"}}"), node);
    }

    @Test
    public void dotcms_grid_json_body_stored_verbatim() {
        final JsonNode node = firstBlock("<dotcms-grid>" + GRID_JSON + "</dotcms-grid>");
        assertEquals(json(GRID_JSON), node);
    }

    @Test
    public void dotcms_node_generic_body() {
        final JsonNode node = firstBlock(
                "<dotcms-node>{\"type\":\"customWidget\",\"attrs\":{\"kind\":\"map\"}}</dotcms-node>");
        assertEquals(json("{\"type\":\"customWidget\",\"attrs\":{\"kind\":\"map\"}}"), node);
    }

    @Test
    public void entity_escaped_body_round_trips() {
        final JsonNode node = firstBlock("<dotcms-node>{\"type\":\"paragraph\",\"content\":"
                + "[{\"type\":\"text\",\"text\":\"a &lt;b&gt; &amp; c\"}]}</dotcms-node>");
        assertEquals("a <b> & c", node.path("content").path(0).path("text").asText());
    }

    @Test
    public void rich_element_between_paragraphs_keeps_order() {
        final JsonNode doc = convert("<p>before</p>"
                + "<dotcms-content identifier=\"abc\"></dotcms-content><p>after</p>");
        assertEquals(3, doc.path("content").size());
        assertEquals("paragraph", doc.path("content").path(0).path("type").asText());
        assertEquals("dotContent", doc.path("content").path(1).path("type").asText());
        assertEquals("paragraph", doc.path("content").path(2).path("type").asText());
    }

    @Test
    public void self_closing_element_does_not_lose_following_content() {
        // HTML parsing ignores '/' on a non-void custom element, so the <p> is swallowed as a
        // CHILD of the video element; the walker must still emit both, in order.
        final JsonNode doc = convert(
                "<dotcms-video src=\"/dA/v.mp4\" mime-type=\"video/mp4\"/><p>after</p>");
        assertEquals("dotVideo", doc.path("content").path(0).path("type").asText());
        assertEquals("paragraph", doc.path("content").path(1).path("type").asText());
        assertEquals("after", doc.path("content").path(1).path("content").path(0).path("text").asText());
    }

    // =====================================================================
    // Validation: invalid input degrades, never throws
    // =====================================================================

    @Test
    public void content_missing_identifier_drops_node_keeps_text() {
        final JsonNode doc = convert(
                "<dotcms-content language-id=\"1\">fallback</dotcms-content>");
        assertEquals(0, count(doc, "dotContent"));
        assertEquals("fallback",
                doc.path("content").path(0).path("content").path(0).path("text").asText());
    }

    @Test
    public void unknown_dotcms_element_stays_transparent() {
        final JsonNode doc = convert("<dotcms-bogus foo=\"1\">kept text</dotcms-bogus>");
        assertEquals(0, count(doc, "dotcms-bogus"));
        assertEquals("kept text",
                doc.path("content").path(0).path("content").path(0).path("text").asText());
    }

    @Test
    public void grid_with_wrong_column_count_degrades_to_code_block() {
        final String threeCols = "{\"type\":\"gridBlock\",\"content\":["
                + "{\"type\":\"gridColumn\"},{\"type\":\"gridColumn\"},{\"type\":\"gridColumn\"}]}";
        final JsonNode node = firstBlock("<dotcms-grid>" + threeCols + "</dotcms-grid>");
        assertEquals("codeBlock", node.path("type").asText());
        assertEquals("dotcms-grid", node.path("attrs").path("language").asText());
        assertTrue("body stays visible for a human to fix",
                node.path("content").path(0).path("text").asText().contains("gridColumn"));
    }

    @Test
    public void doc_smuggling_in_node_body_degrades_to_code_block() {
        final JsonNode node = firstBlock(
                "<dotcms-node>{\"type\":\"doc\",\"content\":[]}</dotcms-node>");
        assertEquals("codeBlock", node.path("type").asText());
        assertEquals("no nested doc may ever be stored", 0,
                count(firstBlock("<dotcms-node>{\"type\":\"doc\"}</dotcms-node>"), "doc"));
    }

    @Test
    public void oversized_body_degrades_to_code_block() {
        final String big = "{\"content\": \"" + "x".repeat(70_000) + "\"}";
        final JsonNode node = firstBlock("<dotcms-ai>" + big + "</dotcms-ai>");
        assertEquals("codeBlock", node.path("type").asText());
    }

    @Test
    public void deeply_nested_body_degrades_to_code_block() {
        final StringBuilder deep = new StringBuilder();
        final int depth = 80; // above the 64-node depth cap
        for (int i = 0; i < depth; i++) {
            deep.append("{\"type\":\"blockquote\",\"content\":[");
        }
        deep.append("{\"type\":\"paragraph\"}");
        deep.append("]}".repeat(depth));
        final JsonNode node = firstBlock("<dotcms-node>" + deep + "</dotcms-node>");
        assertEquals("codeBlock", node.path("type").asText());
    }

    @Test
    public void non_numeric_language_id_falls_back_to_default() {
        final JsonNode node = firstBlock(
                "<dotcms-content identifier=\"abc\" language-id=\"evil\"></dotcms-content>");
        assertEquals(1, node.path("attrs").path("data").path("languageId").asInt());
    }

    @Test
    public void inline_position_degrades_to_text() {
        // Rich nodes are block-level (a fence cannot occur mid-paragraph in Markdown either):
        // inside a paragraph the element is transparent — its text kept, the node not created.
        final JsonNode doc = convert(
                "<p>a <dotcms-content identifier=\"abc\"></dotcms-content> b</p>");
        assertEquals(0, count(doc, "dotContent"));
        assertEquals("paragraph", doc.path("content").path(0).path("type").asText());
    }

    @Test
    public void hostile_inputs_never_throw() {
        final List<String> hostile = List.of(
                "<dotcms-content>",
                "<dotcms-content identifier=\"x\"",
                "<dotcms-grid>not json</dotcms-grid>",
                "<dotcms-grid></dotcms-grid>",
                "<dotcms-ai>{\"content\":42}</dotcms-ai>",
                "<dotcms-node>{\"type\":\"\"}</dotcms-node>",
                "<dotcms-node>[1,2,3]</dotcms-node>",
                "<dotcms->x</dotcms->",
                "<dotcms-video src=\"// evil\"></dotcms-video>",
                "<!-- dotcms:attrs --><p>x</p>",
                "<!-- dotcms:attrs {\"a\":} --><p>x</p>");
        for (final String html : hostile) {
            final JsonNode doc = convert(html);
            assertEquals("doc", doc.path("type").asText());
        }
    }

    // =====================================================================
    // Sanitization: hostile attributes ON the new elements (AC #3)
    // =====================================================================

    @Test
    public void event_handler_attributes_are_never_copied() {
        final String out = convert("<dotcms-content identifier=\"abc\" "
                + "onclick=\"evil()\" onerror=\"evil()\"></dotcms-content>").toString();
        assertTrue(out.contains("dotContent"));
        assertFalse(out.contains("onclick"));
        assertFalse(out.contains("evil"));
    }

    @Test
    public void javascript_src_on_video_drops_the_node() {
        final JsonNode doc = convert(
                "<dotcms-video src=\"javascript:alert(1)\" mime-type=\"video/mp4\"></dotcms-video>");
        assertEquals(0, count(doc, "dotVideo"));
        assertFalse(doc.toString().contains("javascript"));
    }

    @Test
    public void data_url_on_youtube_drops_the_node() {
        final JsonNode doc = convert(
                "<dotcms-youtube src=\"data:text/html;base64,PHNjcmlwdD4=\"></dotcms-youtube>");
        assertEquals(0, count(doc, "youtube"));
        assertFalse(doc.toString().contains("data:"));
    }

    @Test
    public void javascript_href_on_image_is_omitted_but_safe_src_kept() {
        final JsonNode node = firstBlock("<dotcms-image src=\"/dA/x.jpg\" "
                + "href=\"javascript:alert(1)\"></dotcms-image>");
        assertEquals("dotImage", node.path("type").asText());
        assertEquals("/dA/x.jpg", node.path("attrs").path("src").asText());
        assertTrue(node.path("attrs").path("href").isMissingNode());
    }

    @Test
    public void script_smuggled_into_body_degrades_and_never_survives_as_node() {
        final JsonNode doc = convert(
                "<dotcms-grid>{\"type\":\"gridBlock\",<script>alert(1)</script>}</dotcms-grid>");
        assertEquals(0, count(doc, "gridBlock"));
        assertEquals(0, count(doc, "script"));
    }

    @Test
    public void closing_tag_inside_body_string_degrades_and_loses_nothing() {
        // jsoup closes the element at the literal "</dotcms-grid>" inside the JSON string, so
        // the body truncates -> invalid JSON -> visible codeBlock; the remainder and the
        // following sibling survive as ordinary content. Never throws, nothing vanishes.
        final JsonNode doc = convert("<dotcms-grid>{\"type\":\"gridBlock\",\"attrs\":"
                + "{\"x\":\"</dotcms-grid>\"}}</dotcms-grid><p>after</p>");
        assertEquals(0, count(doc, "gridBlock"));
        assertTrue("truncated body stays visible as a codeBlock", count(doc, "codeBlock") > 0);
        assertTrue("the following sibling paragraph survives", doc.toString().contains("after"));
    }

    @Test
    public void decoration_comment_inside_body_is_inert() {
        // A dotcms:attrs comment smuggled INSIDE a body-label element is not walked — it can
        // neither decorate anything nor leak: the body simply parses without the comment text.
        final JsonNode doc = convert("<dotcms-grid>{\"type\":\"gridBlock\""
                + "<!-- dotcms:attrs {\"textAlign\":\"center\"} -->}</dotcms-grid><p>x</p>");
        assertFalse("comment must not decorate the following block",
                doc.toString().contains("textAlign"));
    }

    @Test
    public void rich_element_inside_table_cell_stores() {
        // HTML can position a rich node where Markdown syntactically cannot (a fence cannot
        // live in a table cell). The stored doc is valid and hydrates; its Markdown EXPORT
        // remains a residual loss (#36658 §11) — pinned here as intended behavior.
        final JsonNode doc = convert("<table><tr><td>"
                + "<dotcms-content identifier=\"abc\"></dotcms-content></td></tr></table>");
        assertEquals(1, count(doc, "dotContent"));
        assertEquals(1, count(doc, "table"));
    }

    @Test
    public void drop_subtree_unchanged_around_rich_elements() {
        final JsonNode doc = convert("<script>evil()</script>"
                + "<dotcms-content identifier=\"abc\"></dotcms-content><iframe src=\"x\"></iframe>");
        assertEquals(1, doc.path("content").size());
        assertEquals("dotContent", doc.path("content").path(0).path("type").asText());
        assertFalse(doc.toString().contains("evil"));
    }

    // =====================================================================
    // dotcms:attrs decoration comments (parity with #36658 §2.4)
    // =====================================================================

    @Test
    public void decoration_comment_decorates_next_paragraph() {
        final JsonNode node = firstBlock(
                "<!-- dotcms:attrs {\"textAlign\":\"center\"} --><p>centered</p>");
        assertEquals("center", node.path("attrs").path("textAlign").asText());
    }

    @Test
    public void decoration_structural_attr_wins_on_collision() {
        final JsonNode node = firstBlock("<!-- dotcms:attrs {\"level\":9} --><h2>t</h2>");
        assertEquals(2, node.path("attrs").path("level").asInt());
    }

    @Test
    public void decoration_before_rich_element_is_consumed_not_applied() {
        final JsonNode doc = convert("<!-- dotcms:attrs {\"textAlign\":\"center\"} -->"
                + "<dotcms-content identifier=\"abc\"></dotcms-content><p>after</p>");
        final JsonNode rich = doc.path("content").path(0);
        assertEquals("dotContent", rich.path("type").asText());
        assertTrue(rich.path("attrs").path("textAlign").isMissingNode());
        assertTrue("consumed: must not leak onto the following block",
                doc.path("content").path(1).path("attrs").path("textAlign").isMissingNode());
    }

    @Test
    public void decoration_mid_paragraph_is_ignored() {
        final JsonNode doc = convert(
                "text <!-- dotcms:attrs {\"textAlign\":\"center\"} --> more");
        final JsonNode para = doc.path("content").path(0);
        assertTrue(para.path("attrs").isMissingNode());
        assertEquals(1, doc.path("content").size());
    }

    @Test
    public void decoration_invalid_payload_is_dropped() {
        final JsonNode node = firstBlock(
                "<!-- dotcms:attrs {\"a\":{\"nested\":1}} --><p>x</p>");
        assertTrue(node.path("attrs").isMissingNode());
    }

    @Test
    public void decoration_last_wins() {
        final JsonNode node = firstBlock("<!-- dotcms:attrs {\"textAlign\":\"right\"} -->"
                + "<!-- dotcms:attrs {\"textAlign\":\"center\"} --><p>x</p>");
        assertEquals("center", node.path("attrs").path("textAlign").asText());
    }

    @Test
    public void decoration_applies_to_loose_text_paragraph() {
        final JsonNode node = firstBlock(
                "<!-- dotcms:attrs {\"textAlign\":\"right\"} -->loose text");
        assertEquals("paragraph", node.path("type").asText());
        assertEquals("right", node.path("attrs").path("textAlign").asText());
    }

    @Test
    public void ordinary_comments_are_still_ignored() {
        final JsonNode doc = convert("<!-- just a note --><p>x</p>");
        assertEquals(1, doc.path("content").size());
        assertTrue(doc.path("content").path(0).path("attrs").isMissingNode());
    }

    // =====================================================================
    // Routing predicate (save-path carve-out)
    // =====================================================================

    @Test
    public void starts_with_dotcms_element_predicate() {
        assertTrue(TiptapHtml.startsWithDotcmsElement(
                "<dotcms-content identifier=\"x\"></dotcms-content>"));
        assertTrue(TiptapHtml.startsWithDotcmsElement("  <dotcms-video src=\"x\"/>"));
        assertTrue(TiptapHtml.startsWithDotcmsElement("<DOTCMS-GRID>"));
        assertFalse(TiptapHtml.startsWithDotcmsElement("<p>x</p>"));
        assertFalse(TiptapHtml.startsWithDotcmsElement("<dotcmsx foo>"));
        // Only the vocabulary's own labels qualify: an unknown hyphenated element — a typo, or
        // an unrelated dotCMS web component — must keep the route it has today.
        assertFalse(TiptapHtml.startsWithDotcmsElement("<dotcms-bogus>t</dotcms-bogus>"));
        assertFalse(TiptapHtml.startsWithDotcmsElement("<dotcms-block-editor>x</dotcms-block-editor>"));
        assertFalse(TiptapHtml.startsWithDotcmsElement("plain markdown ```dotcms-content"));
        assertFalse(TiptapHtml.startsWithDotcmsElement("<3 things I like"));
        assertFalse(TiptapHtml.startsWithDotcmsElement(null));
    }

    // =====================================================================
    // PARITY: HTML input ≡ Markdown input for the same rich content (AC #2)
    // =====================================================================

    private static void assertParity(final String markdown, final String html) {
        final JsonNode fromMarkdown = TiptapMarkdown.toTiptap(markdown);
        final JsonNode fromHtml = TiptapHtml.toTiptap(html);
        assertNotNull(fromMarkdown);
        assertEquals("HTML and Markdown must produce the same document", fromMarkdown, fromHtml);
    }

    @Test
    public void parity_dot_content() {
        assertParity(
                "```dotcms-content\n{\"identifier\":\"2d5d1c4c-aaaa\",\"languageId\":2}\n```",
                "<dotcms-content identifier=\"2d5d1c4c-aaaa\" language-id=\"2\"></dotcms-content>");
    }

    @Test
    public void parity_dot_image() {
        assertParity(
                "```dotcms-image\n{\"identifier\":\"img-1\",\"languageId\":1,"
                        + "\"src\":\"/dA/img-1/photo.jpg\",\"alt\":\"Team\","
                        + "\"href\":\"https://dotcms.com\",\"target\":\"_blank\","
                        + "\"textWrap\":\"wrap-right\",\"textAlign\":\"center\"}\n```",
                "<dotcms-image identifier=\"img-1\" language-id=\"1\" src=\"/dA/img-1/photo.jpg\" "
                        + "alt=\"Team\" href=\"https://dotcms.com\" target=\"_blank\" "
                        + "text-wrap=\"wrap-right\" text-align=\"center\"></dotcms-image>");
    }

    @Test
    public void parity_dot_video() {
        assertParity(
                "```dotcms-video\n{\"identifier\":\"v-1\",\"languageId\":1,"
                        + "\"src\":\"/dA/v-1/clip.mp4\",\"mimeType\":\"video/mp4\","
                        + "\"width\":1280,\"height\":720}\n```",
                "<dotcms-video identifier=\"v-1\" language-id=\"1\" src=\"/dA/v-1/clip.mp4\" "
                        + "mime-type=\"video/mp4\" width=\"1280\" height=\"720\"></dotcms-video>");
    }

    @Test
    public void parity_youtube() {
        assertParity(
                "```dotcms-youtube\n{\"src\":\"https://www.youtube.com/watch?v=abc\","
                        + "\"start\":42,\"width\":640,\"height\":480}\n```",
                "<dotcms-youtube src=\"https://www.youtube.com/watch?v=abc\" start=\"42\" "
                        + "width=\"640\" height=\"480\"></dotcms-youtube>");
    }

    @Test
    public void parity_ai_content() {
        assertParity(
                "```dotcms-ai\n{\"content\":\"generated text\"}\n```",
                "<dotcms-ai>{\"content\":\"generated text\"}</dotcms-ai>");
    }

    @Test
    public void parity_grid() {
        assertParity(
                "```dotcms-grid\n" + GRID_JSON + "\n```",
                "<dotcms-grid>" + GRID_JSON + "</dotcms-grid>");
    }

    @Test
    public void parity_generic_node() {
        final String payload = "{\"type\":\"customWidget\",\"attrs\":{\"kind\":\"map\"}}";
        assertParity(
                "```dotcms-node\n" + payload + "\n```",
                "<dotcms-node>" + payload + "</dotcms-node>");
    }

    @Test
    public void parity_decorated_paragraph() {
        assertParity(
                "<!-- dotcms:attrs {\"textAlign\":\"center\"} -->\n\nCentered text",
                "<!-- dotcms:attrs {\"textAlign\":\"center\"} --><p>Centered text</p>");
    }

    @Test
    public void parity_degraded_invalid_fence_and_element() {
        // Both carriers degrade an invalid grid payload to the SAME visible codeBlock.
        final String bad = "{\"type\":\"gridBlock\",\"content\":[{\"type\":\"gridColumn\"}]}";
        assertParity(
                "```dotcms-grid\n" + bad + "\n```",
                "<dotcms-grid>" + bad + "</dotcms-grid>");
    }

    @Test
    public void parity_empty_body_degrade() {
        // An empty payload degrades identically too — including the codeBlock's empty (but
        // present) content array, the byte-exact shape the Markdown leg produces.
        assertParity("```dotcms-grid\n```", "<dotcms-grid></dotcms-grid>");
        assertEquals(json("{\"type\":\"codeBlock\",\"attrs\":{\"language\":\"dotcms-grid\"},"
                        + "\"content\":[]}"),
                firstBlock("<dotcms-grid></dotcms-grid>"));
    }

    @Test
    public void parity_mixed_document() {
        assertParity(
                "Intro paragraph\n\n"
                        + "```dotcms-content\n{\"identifier\":\"abc\",\"languageId\":1}\n```\n\n"
                        + "Closing paragraph",
                "<p>Intro paragraph</p>"
                        + "<dotcms-content identifier=\"abc\" language-id=\"1\"></dotcms-content>"
                        + "<p>Closing paragraph</p>");
    }

    // ---- helpers --------------------------------------------------------

    private static int count(final JsonNode node, final String type) {
        int c = type.equals(node.path("type").asText()) ? 1 : 0;
        for (final JsonNode child : node.path("content")) {
            c += count(child, type);
        }
        return c;
    }
}
