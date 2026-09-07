package com.dotcms.tiptap;

import com.dotcms.UnitTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link TiptapMarkdown}. Verifies Markdown → Tiptap JSON,
 * Tiptap JSON → Markdown, and round-trip semantic stability for every
 * node type and mark we claim to support.
 */
public class TiptapMarkdownTest extends UnitTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // =====================================================================
    // Markdown -> Tiptap JSON
    // =====================================================================

    @Test
    public void parse_empty_input_yields_empty_doc() {
        final JsonNode doc = TiptapMarkdown.toTiptap("");
        assertEquals("doc", doc.path("type").asText());
        assertTrue(doc.path("content").isArray());
        assertEquals(0, doc.path("content").size());
    }

    @Test
    public void parse_null_input_does_not_throw() {
        final JsonNode doc = TiptapMarkdown.toTiptap(null);
        assertEquals("doc", doc.path("type").asText());
    }

    @Test
    public void parse_paragraph_with_plain_text() {
        final JsonNode doc = TiptapMarkdown.toTiptap("hello world");
        final JsonNode para = doc.path("content").get(0);
        assertEquals("paragraph", para.path("type").asText());
        final JsonNode text = para.path("content").get(0);
        assertEquals("text", text.path("type").asText());
        assertEquals("hello world", text.path("text").asText());
        assertFalse("plain text should carry no marks", text.has("marks"));
    }

    @Test
    public void parse_heading_levels_1_through_6() {
        for (int level = 1; level <= 6; level++) {
            final String md = repeat('#', level) + " title";
            final JsonNode doc = TiptapMarkdown.toTiptap(md);
            final JsonNode h = doc.path("content").get(0);
            assertEquals("heading at level " + level, "heading", h.path("type").asText());
            assertEquals(level, h.path("attrs").path("level").asInt());
            assertEquals("title", h.path("content").get(0).path("text").asText());
        }
    }

    @Test
    public void parse_bold_italic_strike_marks() {
        final JsonNode doc = TiptapMarkdown.toTiptap("**b** *i* ~~s~~");
        final JsonNode para = doc.path("content").get(0);
        // Children: "b" with bold, " ", "i" with italic, " ", "s" with strike
        assertHasMark(findTextWithValue(para, "b"), "bold");
        assertHasMark(findTextWithValue(para, "i"), "italic");
        assertHasMark(findTextWithValue(para, "s"), "strike");
    }

    @Test
    public void parse_inline_code_mark() {
        final JsonNode doc = TiptapMarkdown.toTiptap("a `code` b");
        final JsonNode codeText = findTextWithValue(doc, "code");
        assertNotNull(codeText);
        assertHasMark(codeText, "code");
    }

    @Test
    public void parse_link_mark_with_title() {
        final JsonNode doc = TiptapMarkdown.toTiptap("[dotcms](https://dotcms.com \"home\")");
        final JsonNode txt = findTextWithValue(doc, "dotcms");
        assertNotNull(txt);
        final JsonNode mark = firstMarkOfType(txt, "link");
        assertNotNull(mark);
        assertEquals("https://dotcms.com", mark.path("attrs").path("href").asText());
        assertEquals("home", mark.path("attrs").path("title").asText());
    }

    @Test
    public void parse_blockquote_wraps_paragraph() {
        final JsonNode doc = TiptapMarkdown.toTiptap("> quoted");
        final JsonNode bq = doc.path("content").get(0);
        assertEquals("blockquote", bq.path("type").asText());
        assertEquals("paragraph", bq.path("content").get(0).path("type").asText());
        assertEquals("quoted", bq.path("content").get(0).path("content").get(0).path("text").asText());
    }

    @Test
    public void parse_bullet_list_with_items() {
        final JsonNode doc = TiptapMarkdown.toTiptap("- a\n- b\n- c");
        final JsonNode list = doc.path("content").get(0);
        assertEquals("bulletList", list.path("type").asText());
        assertEquals(3, list.path("content").size());
        assertEquals("listItem", list.path("content").get(0).path("type").asText());
    }

    @Test
    public void parse_ordered_list_records_start_attr() {
        final JsonNode doc = TiptapMarkdown.toTiptap("3. three\n4. four");
        final JsonNode list = doc.path("content").get(0);
        assertEquals("orderedList", list.path("type").asText());
        assertEquals(3, list.path("attrs").path("start").asInt());
        assertEquals(2, list.path("content").size());
    }

    @Test
    public void parse_fenced_code_block_with_language() {
        final JsonNode doc = TiptapMarkdown.toTiptap("```java\nint x = 1;\n```");
        final JsonNode code = doc.path("content").get(0);
        assertEquals("codeBlock", code.path("type").asText());
        assertEquals("java", code.path("attrs").path("language").asText());
        assertEquals("int x = 1;", code.path("content").get(0).path("text").asText());
    }

    @Test
    public void parse_fenced_code_block_without_language() {
        final JsonNode doc = TiptapMarkdown.toTiptap("```\nplain\n```");
        final JsonNode code = doc.path("content").get(0);
        assertEquals("codeBlock", code.path("type").asText());
        assertFalse("no language attr expected", code.has("attrs"));
        assertEquals("plain", code.path("content").get(0).path("text").asText());
    }

    @Test
    public void parse_horizontal_rule() {
        final JsonNode doc = TiptapMarkdown.toTiptap("---");
        assertEquals("horizontalRule", doc.path("content").get(0).path("type").asText());
    }

    @Test
    public void parse_hard_break_inside_paragraph() {
        final JsonNode doc = TiptapMarkdown.toTiptap("line1  \nline2");
        final JsonNode para = doc.path("content").get(0);
        boolean foundHardBreak = false;
        for (final JsonNode child : para.path("content")) {
            if ("hardBreak".equals(child.path("type").asText())) {
                foundHardBreak = true;
                break;
            }
        }
        assertTrue("expected a hardBreak node inside paragraph", foundHardBreak);
    }

    @Test
    public void parse_image_inline_in_paragraph() {
        final JsonNode doc = TiptapMarkdown.toTiptap("![alt](http://x/y.png \"t\")");
        final JsonNode img = doc.path("content").get(0).path("content").get(0);
        // Emitted as dotImage (the dotCMS editor schema node), not the generic ProseMirror "image".
        assertEquals("dotImage", img.path("type").asText());
        assertEquals("http://x/y.png", img.path("attrs").path("src").asText());
        assertEquals("alt", img.path("attrs").path("alt").asText());
        assertEquals("t", img.path("attrs").path("title").asText());
    }

    @Test
    public void parse_gfm_table_produces_table_nodes_with_header_row() {
        final String md = "| h1 | h2 |\n|----|----|\n| a | b |\n| c | d |";
        final JsonNode doc = TiptapMarkdown.toTiptap(md);
        final JsonNode table = doc.path("content").get(0);
        assertEquals("table", table.path("type").asText());

        final JsonNode rows = table.path("content");
        assertEquals(3, rows.size());

        // header row → tableHeader cells
        final JsonNode headerRow = rows.get(0);
        assertEquals("tableRow", headerRow.path("type").asText());
        assertEquals("tableHeader", headerRow.path("content").get(0).path("type").asText());
        assertEquals("h1", deepText(headerRow.path("content").get(0)));

        // body rows → tableCell
        final JsonNode bodyRow = rows.get(1);
        assertEquals("tableCell", bodyRow.path("content").get(0).path("type").asText());
        assertEquals("a", deepText(bodyRow.path("content").get(0)));

        // cell attrs include colspan, rowspan, colwidth (null)
        final JsonNode attrs = bodyRow.path("content").get(0).path("attrs");
        assertEquals(1, attrs.path("colspan").asInt());
        assertEquals(1, attrs.path("rowspan").asInt());
        assertTrue("colwidth should be null", attrs.path("colwidth").isNull());
    }

    @Test
    public void parse_nested_marks_bold_italic_link() {
        final JsonNode doc = TiptapMarkdown.toTiptap("[**bold link**](https://x)");
        final JsonNode txt = findTextWithValue(doc, "bold link");
        assertNotNull(txt);
        assertHasMark(txt, "bold");
        assertHasMark(txt, "link");
    }

    @Test
    public void parse_multiple_paragraphs_separated_by_blank_lines() {
        final JsonNode doc = TiptapMarkdown.toTiptap("first\n\nsecond\n\nthird");
        assertEquals(3, doc.path("content").size());
        for (final JsonNode p : doc.path("content")) {
            assertEquals("paragraph", p.path("type").asText());
        }
    }

    // =====================================================================
    // Tiptap JSON -> Markdown
    // =====================================================================

    @Test
    public void render_empty_doc_returns_empty_string() {
        final ObjectNode doc = MAPPER.createObjectNode();
        doc.put("type", "doc");
        doc.putArray("content");
        assertEquals("", TiptapMarkdown.toMarkdown(doc));
    }

    @Test
    public void render_null_input_returns_empty_string() {
        assertEquals("", TiptapMarkdown.toMarkdown((JsonNode) null));
    }

    @Test
    public void render_heading_uses_hash_prefix_with_level() {
        final String md = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap("### hello"));
        assertTrue("expected `### hello` in output, got: " + md, md.startsWith("### hello"));
    }

    @Test
    public void render_paragraph_emits_text_then_blank_line() {
        final String md = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap("hello"));
        assertEquals("hello", md.trim());
    }

    @Test
    public void render_bold_uses_double_asterisks() {
        final String md = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap("**x**"));
        assertTrue(md.contains("**x**"));
    }

    @Test
    public void render_italic_uses_single_asterisk() {
        final String md = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap("*x*"));
        assertTrue(md.contains("*x*"));
    }

    @Test
    public void render_strike_uses_double_tilde() {
        final String md = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap("~~x~~"));
        assertTrue(md.contains("~~x~~"));
    }

    @Test
    public void render_inline_code_uses_backticks_and_does_not_escape_specials() {
        final String md = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap("`a*b_c`"));
        // The text inside the code mark must remain literal — no backslash escapes.
        assertTrue("code content must be literal, got: " + md, md.contains("`a*b_c`"));
    }

    @Test
    public void render_link_includes_href_and_title() {
        final String md = TiptapMarkdown.toMarkdown(
                TiptapMarkdown.toTiptap("[x](https://dot.cms \"t\")"));
        assertTrue(md.contains("[x](https://dot.cms \"t\")"));
    }

    @Test
    public void render_blockquote_prefixes_each_line() {
        final String md = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap("> a"));
        assertTrue("expected blockquote prefix, got: " + md, md.startsWith("> "));
    }

    @Test
    public void render_bullet_list_uses_dash_markers() {
        final String md = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap("- a\n- b"));
        assertTrue(md.contains("- a"));
        assertTrue(md.contains("- b"));
    }

    @Test
    public void render_ordered_list_starts_at_given_number() {
        final String md = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap("5. a\n6. b"));
        assertTrue("expected `5. a`, got: " + md, md.contains("5. a"));
        assertTrue("expected `6. b`, got: " + md, md.contains("6. b"));
    }

    @Test
    public void render_code_block_round_trips_language_and_body() {
        final String src = "```java\nint x = 1;\n```";
        final String md = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap(src));
        assertTrue(md.contains("```java"));
        assertTrue(md.contains("int x = 1;"));
        assertTrue(md.trim().endsWith("```"));
    }

    @Test
    public void render_code_block_picks_longer_fence_when_body_contains_triple_backticks() {
        final ObjectNode doc = MAPPER.createObjectNode();
        doc.put("type", "doc");
        final ObjectNode cb = doc.putArray("content").addObject();
        cb.put("type", "codeBlock");
        cb.putArray("content").addObject().put("type", "text").put("text", "look ``` here");
        final String md = TiptapMarkdown.toMarkdown(doc);
        // Triple-backtick fence would collide with body — must use 4+ backticks.
        assertTrue("expected fence with 4+ backticks, got: " + md, md.startsWith("````"));
    }

    @Test
    public void render_horizontal_rule_uses_three_dashes() {
        final String md = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap("---"));
        assertEquals("---", md.trim());
    }

    @Test
    public void render_image_emits_bang_bracket_form() {
        final String md = TiptapMarkdown.toMarkdown(
                TiptapMarkdown.toTiptap("![alt](http://x/y.png \"t\")"));
        assertTrue(md.contains("![alt](http://x/y.png \"t\")"));
    }

    @Test
    public void render_table_produces_pipe_delimited_rows_with_separator() {
        final String src = "| h1 | h2 |\n|----|----|\n| a | b |";
        final String md = TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap(src));
        final String[] lines = md.trim().split("\n");
        assertTrue("expected at least 3 lines, got: " + md, lines.length >= 3);
        assertTrue("header row must start with |", lines[0].trim().startsWith("|"));
        assertTrue("separator row must contain ---", lines[1].contains("---"));
        assertTrue("body row must include `a`", lines[2].contains("a"));
        assertTrue("body row must include `b`", lines[2].contains("b"));
    }

    @Test
    public void render_table_cell_escapes_pipe_character() {
        final ObjectNode doc = MAPPER.createObjectNode();
        doc.put("type", "doc");
        final ObjectNode table = doc.putArray("content").addObject();
        table.put("type", "table");
        final ObjectNode row = table.putArray("content").addObject();
        row.put("type", "tableRow");
        final ObjectNode cell = row.putArray("content").addObject();
        cell.put("type", "tableHeader");
        final ObjectNode para = cell.putArray("content").addObject();
        para.put("type", "paragraph");
        para.putArray("content").addObject().put("type", "text").put("text", "a|b");

        final String md = TiptapMarkdown.toMarkdown(doc);
        assertTrue("pipe inside cell must be escaped, got: " + md, md.contains("a\\|b"));
    }

    @Test
    public void render_text_escapes_markdown_specials_outside_code() {
        final ObjectNode doc = MAPPER.createObjectNode();
        doc.put("type", "doc");
        final ObjectNode p = doc.putArray("content").addObject();
        p.put("type", "paragraph");
        p.putArray("content").addObject().put("type", "text").put("text", "a*b_c[d]");

        final String md = TiptapMarkdown.toMarkdown(doc);
        // Each markdown special must be backslash-escaped so the regen markdown stays literal.
        assertTrue("expected escaped *, got: " + md, md.contains("\\*"));
        assertTrue("expected escaped _, got: " + md, md.contains("\\_"));
        assertTrue("expected escaped [, got: " + md, md.contains("\\["));
        assertTrue("expected escaped ], got: " + md, md.contains("\\]"));
    }

    @Test
    public void render_string_overload_parses_json_and_returns_markdown() {
        final String json = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"hi\"}]}]}";
        assertEquals("hi", TiptapMarkdown.toMarkdown(json));
    }

    @Test(expected = IllegalArgumentException.class)
    public void render_string_overload_throws_on_invalid_json() {
        TiptapMarkdown.toMarkdown("not json");
    }

    // =====================================================================
    // Round-trip stability
    // =====================================================================

    @Test
    public void roundtrip_paragraph_with_marks_is_stable() {
        assertRoundTripStable("**bold** and *italic* and ~~strike~~ and `code`.");
    }

    @Test
    public void roundtrip_heading_is_stable() {
        assertRoundTripStable("## A heading");
    }

    @Test
    public void roundtrip_link_is_stable() {
        assertRoundTripStable("see [docs](https://dotcms.com).");
    }

    @Test
    public void roundtrip_code_block_is_stable() {
        assertRoundTripStable("```java\nint x = 1;\n```");
    }

    @Test
    public void roundtrip_blockquote_is_stable() {
        assertRoundTripStable("> quoted text");
    }

    @Test
    public void roundtrip_horizontal_rule_is_stable() {
        assertRoundTripStable("---");
    }

    @Test
    public void roundtrip_image_is_stable() {
        assertRoundTripStable("![alt](http://x/y.png \"t\")");
    }

    @Test
    public void roundtrip_bullet_list_is_stable() {
        // Once normalized through one full round-trip the structure must be stable
        // (whitespace/cosmetic differences are absorbed by the first pass).
        final JsonNode once = TiptapMarkdown.toTiptap(TiptapMarkdown.toMarkdown(
                TiptapMarkdown.toTiptap("- a\n- b\n- c")));
        final JsonNode twice = TiptapMarkdown.toTiptap(TiptapMarkdown.toMarkdown(once));
        assertEquals(once, twice);
    }

    @Test
    public void roundtrip_ordered_list_is_stable() {
        final JsonNode once = TiptapMarkdown.toTiptap(TiptapMarkdown.toMarkdown(
                TiptapMarkdown.toTiptap("1. a\n2. b")));
        final JsonNode twice = TiptapMarkdown.toTiptap(TiptapMarkdown.toMarkdown(once));
        assertEquals(once, twice);
    }

    @Test
    public void roundtrip_table_is_stable() {
        final String md = "| h1 | h2 |\n|----|----|\n| a | b |\n| c | d |";
        final JsonNode once = TiptapMarkdown.toTiptap(TiptapMarkdown.toMarkdown(
                TiptapMarkdown.toTiptap(md)));
        final JsonNode twice = TiptapMarkdown.toTiptap(TiptapMarkdown.toMarkdown(once));
        assertEquals(once, twice);
    }

    // =====================================================================
    // helpers
    // =====================================================================

    /** Assert that two round-trips produce the same JSON (semantic stability). */
    private static void assertRoundTripStable(final String markdown) {
        final JsonNode once = TiptapMarkdown.toTiptap(
                TiptapMarkdown.toMarkdown(TiptapMarkdown.toTiptap(markdown)));
        final JsonNode twice = TiptapMarkdown.toTiptap(
                TiptapMarkdown.toMarkdown(once));
        assertEquals("round-trip should be stable for: " + markdown, once, twice);
    }

    /** Recursively search for a text node whose `text` equals the given value. */
    private static JsonNode findTextWithValue(final JsonNode root, final String value) {
        if (root == null) return null;
        if ("text".equals(root.path("type").asText()) && value.equals(root.path("text").asText())) {
            return root;
        }
        for (final JsonNode c : root.path("content")) {
            final JsonNode hit = findTextWithValue(c, value);
            if (hit != null) return hit;
        }
        return null;
    }

    /** Recursively concatenate all `text` nodes' content under a node. */
    private static String deepText(final JsonNode root) {
        final StringBuilder sb = new StringBuilder();
        collectText(root, sb);
        return sb.toString();
    }

    private static void collectText(final JsonNode n, final StringBuilder sb) {
        if (n == null) return;
        if ("text".equals(n.path("type").asText())) {
            sb.append(n.path("text").asText());
        }
        for (final JsonNode c : n.path("content")) collectText(c, sb);
    }

    private static void assertHasMark(final JsonNode textNode, final String markType) {
        assertNotNull("expected text node carrying mark " + markType, textNode);
        assertNotNull("text node must have a `marks` array for " + markType,
                firstMarkOfType(textNode, markType));
    }

    private static JsonNode firstMarkOfType(final JsonNode textNode, final String type) {
        if (textNode == null || !textNode.has("marks")) return null;
        for (final JsonNode m : textNode.get("marks")) {
            if (type.equals(m.path("type").asText())) return m;
        }
        return null;
    }

    private static String repeat(final char c, final int n) {
        final char[] a = new char[n];
        java.util.Arrays.fill(a, c);
        return new String(a);
    }
}
