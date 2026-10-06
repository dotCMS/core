package com.dotcms.tiptap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

/**
 * Unit tests for {@link TiptapHtml} (issue #36470): server-side HTML → Tiptap/ProseMirror JSON on
 * the content save path. Coverage is grouped into conversion fidelity, content-model validity
 * (the nesting rules the Block Editor enforces — an invalid doc is silently wiped on the next
 * editor save), security/sanitization, and mechanical invariants asserted across a battery of
 * inputs. Node shapes are validated against the same whitelist the Markdown leg produces
 * ({@link TiptapMarkdown#isMarkdownRepresentable(String)}).
 *
 * @author hassandotcms
 */
public class TiptapHtmlTest extends UnitTestBase {

    private static JsonNode convert(final String html) {
        return TiptapHtml.toTiptap(html);
    }

    private static String json(final String html) {
        return convert(html).toString();
    }

    // =====================================================================
    // Conversion fidelity
    // =====================================================================

    @Test
    public void heading_carries_level() {
        final JsonNode doc = convert("<h3>Title</h3>");
        final JsonNode h = doc.path("content").get(0);
        assertEquals("heading", h.path("type").asText());
        assertEquals(3, h.path("attrs").path("level").asInt());
        assertEquals("Title", h.path("content").get(0).path("text").asText());
    }

    @Test
    public void paragraph_with_inline_marks() {
        final JsonNode p = convert("<p>a <strong>b</strong> <em>c</em> <s>d</s> <code>e</code></p>")
                .path("content").get(0);
        assertEquals("paragraph", p.path("type").asText());
        assertTrue(json("<p><strong>b</strong></p>").contains("\"bold\""));
        assertTrue(json("<p><em>c</em></p>").contains("\"italic\""));
        assertTrue(json("<p><del>d</del></p>").contains("\"strike\""));
        assertTrue(json("<p><code>e</code></p>").contains("\"code\""));
    }

    @Test
    public void link_becomes_link_mark_with_href() {
        final String out = json("<p><a href=\"https://dotcms.com\" title=\"t\">go</a></p>");
        assertTrue(out.contains("\"link\""));
        assertTrue(out.contains("https://dotcms.com"));
        assertTrue(out.contains("\"go\""));
    }

    @Test
    public void nested_marks_both_applied() {
        final String out = json("<p><strong><em>x</em></strong></p>");
        assertTrue(out.contains("\"bold\""));
        assertTrue(out.contains("\"italic\""));
    }

    @Test
    public void bullet_list_with_items() {
        final JsonNode list = convert("<ul><li>one</li><li>two</li></ul>").path("content").get(0);
        assertEquals("bulletList", list.path("type").asText());
        assertEquals(2, list.path("content").size());
        assertEquals("listItem", list.path("content").get(0).path("type").asText());
    }

    @Test
    public void ordered_list_reads_start() {
        final JsonNode ol = convert("<ol start=\"5\"><li>x</li></ol>").path("content").get(0);
        assertEquals("orderedList", ol.path("type").asText());
        assertEquals(5, ol.path("attrs").path("start").asInt());
    }

    @Test
    public void code_block_preserves_text_and_language() {
        final JsonNode cb = convert("<pre><code class=\"language-java\">int x = 1 &lt; 2;</code></pre>")
                .path("content").get(0);
        assertEquals("codeBlock", cb.path("type").asText());
        assertEquals("java", cb.path("attrs").path("language").asText());
        // Entities are decoded; the '<' is preserved verbatim as code text.
        assertEquals("int x = 1 < 2;", cb.path("content").get(0).path("text").asText());
    }

    @Test
    public void table_with_header_and_colspan() {
        final JsonNode table = convert(
                "<table><thead><tr><th colspan=\"2\">H</th></tr></thead>"
                        + "<tbody><tr><td>a</td><td>b</td></tr></tbody></table>")
                .path("content").get(0);
        assertEquals("table", table.path("type").asText());
        assertEquals(2, table.path("content").size()); // two rows across thead+tbody
        final JsonNode headerCell = table.path("content").get(0).path("content").get(0);
        assertEquals("tableHeader", headerCell.path("type").asText());
        assertEquals(2, headerCell.path("attrs").path("colspan").asInt());
    }

    @Test
    public void image_becomes_dotImage() {
        final JsonNode img = convert("<p><img src=\"/dA/x/image.png\" alt=\"cat\" title=\"c\"></p>")
                .path("content").get(0).path("content").get(0);
        assertEquals("dotImage", img.path("type").asText());
        assertEquals("/dA/x/image.png", img.path("attrs").path("src").asText());
        assertEquals("cat", img.path("attrs").path("alt").asText());
    }

    @Test
    public void hard_break_and_horizontal_rule() {
        assertTrue(json("<p>a<br>b</p>").contains("\"hardBreak\""));
        assertTrue(json("<hr>").contains("\"horizontalRule\""));
    }

    // =====================================================================
    // Content-model validity (arity / coercion)
    // =====================================================================

    @Test
    public void empty_cell_gets_a_paragraph() {
        final JsonNode cell = convert("<table><tr><td></td><td>x</td></tr></table>")
                .path("content").get(0).path("content").get(0).path("content").get(0);
        assertEquals("tableCell", cell.path("type").asText());
        assertEquals("paragraph", cell.path("content").get(0).path("type").asText());
    }

    @Test
    public void list_item_always_starts_with_paragraph() {
        // A list item whose only child is a nested list must still open with a paragraph.
        final JsonNode item = convert("<ul><li><ul><li>inner</li></ul></li></ul>")
                .path("content").get(0).path("content").get(0);
        assertEquals("listItem", item.path("type").asText());
        assertEquals("paragraph", item.path("content").get(0).path("type").asText());
        assertEquals("bulletList", item.path("content").get(1).path("type").asText());
    }

    @Test
    public void nested_list_as_sibling_is_reparented_not_dropped() {
        // Legacy WYSIWYG emits the nested <ul> as a SIBLING of <li>, not inside it.
        final JsonNode list = convert("<ul><li>a</li><ul><li>b</li></ul></ul>").path("content").get(0);
        assertEquals(1, list.path("content").size()); // both fold into a single list item
        assertTrue("nested content b must survive", list.toString().contains("\"b\""));
    }

    @Test
    public void empty_containers_are_elided() {
        assertEquals(0, convert("<ul></ul>").path("content").size());
        assertEquals(0, convert("<table></table>").path("content").size());
        assertEquals(0, convert("<blockquote></blockquote>").path("content").size());
        assertEquals(0, convert("<table><tr></tr></table>").path("content").size());
    }

    @Test
    public void div_acts_as_block_boundary() {
        final JsonNode doc = convert("<div>one</div><div>two</div>");
        assertEquals(2, doc.path("content").size());
        assertEquals("paragraph", doc.path("content").get(0).path("type").asText());
    }

    @Test
    public void block_inside_heading_is_flattened_not_nested() {
        // heading is inline-only; a stray block child must flatten (text kept), never nest.
        final JsonNode h = convert("<h1>Title<p>sub</p></h1>").path("content").get(0);
        assertEquals("heading", h.path("type").asText());
        for (final JsonNode child : h.path("content")) {
            assertEquals("text", child.path("type").asText());
        }
        assertTrue(h.toString().contains("Title"));
        assertTrue(h.toString().contains("sub"));
    }

    @Test
    public void mark_wrapping_a_block_lifts_the_block_and_keeps_the_mark() {
        // <b>foo<p>bar</p>baz</b>: three paragraphs, all bold.
        final JsonNode doc = convert("<b>foo<p>bar</p>baz</b>");
        assertEquals(3, doc.path("content").size());
        for (final JsonNode p : doc.path("content")) {
            assertEquals("paragraph", p.path("type").asText());
            assertTrue("each run keeps the bold mark", p.toString().contains("\"bold\""));
        }
    }

    // =====================================================================
    // Whitespace
    // =====================================================================

    @Test
    public void spaces_around_inline_marks_are_preserved() {
        assertEquals("foo bar baz", plainText(convert("<p>foo <b>bar</b> baz</p>")));
        assertEquals("a b", plainText(convert("<p><b>a</b> <i>b</i></p>")));
    }

    @Test
    public void collapses_runs_of_whitespace() {
        assertEquals("a b", plainText(convert("<p>a    \n   b</p>")));
    }

    @Test
    public void space_around_inline_image_is_preserved() {
        // "foo <img> bar" must keep a space on each side of the image, not read "foo[img]bar".
        final JsonNode content = convert("<p>foo <img src=\"/x.png\"> bar</p>")
                .path("content").get(0).path("content");
        int imgIdx = -1;
        for (int i = 0; i < content.size(); i++) {
            if ("dotImage".equals(content.get(i).path("type").asText())) {
                imgIdx = i;
            }
        }
        assertTrue("image must be present", imgIdx > 0 && imgIdx < content.size() - 1);
        assertTrue("a space must precede the image",
                content.get(imgIdx - 1).path("text").asText().endsWith(" "));
        assertTrue("a space must follow the image",
                content.get(imgIdx + 1).path("text").asText().startsWith(" "));
    }

    // =====================================================================
    // Security / sanitization
    // =====================================================================

    @Test
    public void script_style_iframe_are_dropped_with_their_text() {
        final String out = json("<p>before<script>alert(1)</script>after"
                + "<style>.x{}</style><iframe src=\"//evil\"></iframe></p>");
        assertFalse(out.contains("alert"));
        assertFalse(out.contains(".x{}"));
        assertFalse(out.toLowerCase().contains("iframe"));
        assertTrue(out.contains("before"));
        assertTrue(out.contains("after"));
    }

    @Test
    public void event_handler_attributes_never_survive() {
        final String out = json("<p onclick=\"steal()\"><img src=\"/a.png\" onerror=\"x()\"></p>");
        assertFalse(out.contains("onclick"));
        assertFalse(out.contains("onerror"));
        assertFalse(out.contains("steal"));
    }

    @Test
    public void unsafe_url_schemes_are_rejected() {
        // Link with an unsafe href: the anchor degrades to plain text (mark dropped).
        for (final String bad : new String[]{
                "javascript:alert(1)", "JavaScript:alert(1)", "  javascript:alert(1)",
                "java\tscript:alert(1)", "vbscript:x", "data:text/html,<x>"}) {
            final String out = json("<p><a href=\"" + bad + "\">click</a></p>");
            assertFalse("must reject href [" + bad + "]", out.contains("\"link\""));
            assertTrue("link text must survive [" + bad + "]", out.contains("click"));
        }
        // Image with an unsafe src is dropped entirely.
        assertFalse(json("<p><img src=\"javascript:alert(1)\"></p>").contains("dotImage"));
    }

    @Test
    public void safe_url_schemes_pass() {
        assertTrue(json("<a href=\"https://x.com\">x</a>").contains("https://x.com"));
        assertTrue(json("<a href=\"/relative/path\">x</a>").contains("/relative/path"));
        assertTrue(json("<a href=\"mailto:a@b.com\">x</a>").contains("mailto:a@b.com"));
        assertTrue(json("<a href=\"//cdn.example.com/x\">x</a>").contains("//cdn.example.com/x"));
    }

    @Test
    public void attribute_breakout_characters_in_url_are_rejected() {
        // A scheme-valid URL that smuggles a quote/space to break out of the attribute downstream.
        final String out = json("<p><a href='https://x\" onmouseover=alert(1)'>c</a></p>");
        assertFalse(out.contains("\"link\""));
        assertFalse(out.contains("onmouseover"));
    }

    // =====================================================================
    // Mechanical invariants across a battery of inputs
    // =====================================================================

    @Test
    public void every_output_is_a_valid_representable_doc_and_never_throws() {
        final String[] inputs = {
                null, "", "   ", "plain text no tags",
                "<p>hi</p>", "<h1>a</h1><h2>b</h2>",
                "<ul><li>a<li>b</ul>", "<ol><li>x</li></ol>",
                "<table><tr><td>a</td></tr></table>",
                "<blockquote><p>q</p></blockquote>",
                "<div><span>x</span></div>",
                "<p><b><i><u>deep</u></i></b></p>",
                "<img src=\"/a.png\">",
                "<<>><malformed<<", "<p>unclosed", "&lt;&gt;&amp;",
                "<custom-el data-x='1'>content</custom-el>",
                "<textarea><script>x</script></textarea>",
                "<h1><ul><li>weird</li></ul></h1>",
                "<pre>  keep   spaces\n\tand tabs</pre>",
        };
        for (final String in : inputs) {
            final ObjectNode doc = TiptapHtml.toTiptap(in); // must not throw
            final String out = doc.toString();
            assertTrue("isTiptapDoc for input [" + in + "]", TiptapMarkdown.isTiptapDoc(out));
            assertTrue("representable for input [" + in + "]",
                    TiptapMarkdown.isMarkdownRepresentable(out));
            assertStructurallyValid(doc, in);
        }
    }

    @Test
    public void deeply_nested_html_does_not_overflow() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append("<div>");
        }
        sb.append("deep");
        // Must return (depth-capped), not StackOverflowError.
        assertTrue(TiptapMarkdown.isTiptapDoc(TiptapHtml.toTiptap(sb.toString()).toString()));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Concatenates all text-node text in document order (marks/structure ignored). */
    private static String plainText(final JsonNode node) {
        final StringBuilder sb = new StringBuilder();
        collectText(node, sb);
        return sb.toString();
    }

    private static void collectText(final JsonNode node, final StringBuilder sb) {
        if ("text".equals(node.path("type").asText())) {
            sb.append(node.path("text").asText());
        }
        for (final JsonNode child : node.path("content")) {
            collectText(child, sb);
        }
    }

    /**
     * Asserts the ProseMirror content-model rules the Block Editor enforces. A violation here is a
     * doc the editor would reject or silently wipe — the failure mode this converter must never
     * produce, and which the type-whitelist invariants cannot detect.
     */
    private static void assertStructurallyValid(final JsonNode node, final String input) {
        final String type = node.path("type").asText();
        if ("text".equals(type)) {
            assertTrue("empty text node from [" + input + "]",
                    node.path("text").asText().length() > 0);
            return;
        }
        if ("listItem".equals(type)) {
            assertTrue("listItem must be non-empty [" + input + "]", node.path("content").size() > 0);
            assertEquals("listItem must start with a paragraph [" + input + "]",
                    "paragraph", node.path("content").get(0).path("type").asText());
        }
        if ("bulletList".equals(type) || "orderedList".equals(type)) {
            assertTrue("list must have items [" + input + "]", node.path("content").size() > 0);
            for (final JsonNode child : node.path("content")) {
                assertEquals("list child must be listItem [" + input + "]",
                        "listItem", child.path("type").asText());
            }
        }
        if ("table".equals(type)) {
            assertTrue("table must have rows [" + input + "]", node.path("content").size() > 0);
        }
        if ("tableRow".equals(type)) {
            assertTrue("row must have cells [" + input + "]", node.path("content").size() > 0);
        }
        if ("tableCell".equals(type) || "tableHeader".equals(type)) {
            assertTrue("cell must have block content [" + input + "]", node.path("content").size() > 0);
        }
        if ("blockquote".equals(type)) {
            assertTrue("blockquote must be non-empty [" + input + "]", node.path("content").size() > 0);
        }
        for (final JsonNode child : node.path("content")) {
            assertStructurallyValid(child, input);
        }
    }
}
