package com.dotcms.tiptap;

import com.dotcms.UnitTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * End-to-end tests for {@link TiptapMarkdown} using real dotCMS Blog content
 * loaded from {@code blog-test.json}. Each contentlet's {@code body} field
 * holds a Tiptap document; this exercises the converter against production-
 * shaped input rather than synthetic fixtures.
 */
public class TiptapMarkdownBlogContentTest extends UnitTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static JsonNode contentlets;

    /** All node types our converter supports as block or atomic content. */
    private static final Set<String> SUPPORTED_NODE_TYPES = new HashSet<>();
    /** All mark types our converter supports. */
    private static final Set<String> SUPPORTED_MARK_TYPES = new HashSet<>();
    static {
        SUPPORTED_NODE_TYPES.add("doc");
        SUPPORTED_NODE_TYPES.add("paragraph");
        SUPPORTED_NODE_TYPES.add("heading");
        SUPPORTED_NODE_TYPES.add("blockquote");
        SUPPORTED_NODE_TYPES.add("bulletList");
        SUPPORTED_NODE_TYPES.add("orderedList");
        SUPPORTED_NODE_TYPES.add("listItem");
        SUPPORTED_NODE_TYPES.add("codeBlock");
        SUPPORTED_NODE_TYPES.add("horizontalRule");
        SUPPORTED_NODE_TYPES.add("hardBreak");
        SUPPORTED_NODE_TYPES.add("image");
        SUPPORTED_NODE_TYPES.add("table");
        SUPPORTED_NODE_TYPES.add("tableRow");
        SUPPORTED_NODE_TYPES.add("tableHeader");
        SUPPORTED_NODE_TYPES.add("tableCell");
        SUPPORTED_NODE_TYPES.add("text");
        // dotCMS-specific Tiptap extensions handled by the converter:
        SUPPORTED_NODE_TYPES.add("dotImage");   // rendered as standard markdown image
        SUPPORTED_NODE_TYPES.add("youtube");    // rendered as a plain markdown link to src

        SUPPORTED_MARK_TYPES.add("bold");
        SUPPORTED_MARK_TYPES.add("italic");
        SUPPORTED_MARK_TYPES.add("strike");
        SUPPORTED_MARK_TYPES.add("code");
        SUPPORTED_MARK_TYPES.add("link");
        // Marks that have no markdown syntax — dropped silently on conversion:
        SUPPORTED_MARK_TYPES.add("underline");
    }

    @BeforeClass
    public static void loadFixture() throws Exception {
        try (InputStream in = TiptapMarkdownBlogContentTest.class
                .getClassLoader().getResourceAsStream("blog-test.json")) {
            assertNotNull("blog-test.json must be on the test classpath", in);
            final JsonNode root = MAPPER.readTree(in);
            contentlets = root.path("contentlets");
            assertTrue("blog-test.json must contain a contentlets array",
                    contentlets.isArray() && contentlets.size() > 0);
        }
    }

    /**
     * Sanity check that the fixture has the structure we assume — a `contentlets`
     * array of Blog objects whose `body` field is a Tiptap doc node.
     */
    @Test
    public void fixture_has_blog_contentlets_with_tiptap_bodies() {
        assertTrue("expected multiple blog contentlets", contentlets.size() >= 1);
        for (final JsonNode c : contentlets) {
            assertTrue("each contentlet must have a body", c.has("body"));
            assertEquals("body must be a tiptap doc",
                    "doc", c.path("body").path("type").asText());
            assertTrue("body must have content nodes",
                    c.path("body").path("content").isArray()
                            && c.path("body").path("content").size() > 0);
        }
    }

    /**
     * Verify every node and mark in the fixture is one our converter supports.
     * Any unknown type would silently degrade, so flag it loudly here.
     */
    @Test
    public void every_node_type_in_fixture_is_supported() {
        final Set<String> seenNodes = new HashSet<>();
        final Set<String> seenMarks = new HashSet<>();
        for (final JsonNode c : contentlets) {
            collectTypes(c.path("body"), seenNodes, seenMarks);
        }

        final Set<String> unsupportedNodes = new HashSet<>(seenNodes);
        unsupportedNodes.removeAll(SUPPORTED_NODE_TYPES);
        final Set<String> unsupportedMarks = new HashSet<>(seenMarks);
        unsupportedMarks.removeAll(SUPPORTED_MARK_TYPES);

        assertTrue("fixture contains unsupported node types: " + unsupportedNodes,
                unsupportedNodes.isEmpty());
        assertTrue("fixture contains unsupported mark types: " + unsupportedMarks,
                unsupportedMarks.isEmpty());
    }

    /**
     * Convert each blog body to markdown and assert the output is non-empty
     * and contains structural markers proportional to what's in the body.
     */
    @Test
    public void every_blog_body_renders_to_non_empty_markdown() {
        for (final JsonNode c : contentlets) {
            final String title = c.path("title").asText("<no title>");
            final JsonNode body = c.path("body");
            final String md;
            try {
                md = TiptapMarkdown.toMarkdown(body);
            } catch (final Exception e) {
                fail("toMarkdown threw for blog '" + title + "': " + e.getMessage());
                return;
            }
            assertFalse("markdown for blog '" + title + "' should not be empty",
                    md.trim().isEmpty());

            // If the body has any headings, expect a `#` somewhere in the output.
            if (containsType(body, "heading")) {
                assertTrue("blog '" + title + "' has headings — md should contain `#`: "
                        + md.substring(0, Math.min(200, md.length())),
                        md.contains("#"));
            }
            // If the body has any code blocks, expect a fenced block in the output.
            if (containsType(body, "codeBlock")) {
                assertTrue("blog '" + title + "' has codeBlocks — md should contain ```",
                        md.contains("```"));
            }
            // If the body has any tables, expect a `|` row separator.
            if (containsType(body, "table")) {
                assertTrue("blog '" + title + "' has tables — md should contain `|`",
                        md.contains("|") && md.contains("---"));
            }
        }
    }

    /**
     * Re-parse each rendered markdown back to Tiptap JSON and verify we get a
     * non-empty doc. This is the end-to-end bidirectional check on real data.
     */
    @Test
    public void every_blog_body_round_trips_to_a_non_empty_doc() {
        for (final JsonNode c : contentlets) {
            final String title = c.path("title").asText("<no title>");
            final JsonNode body = c.path("body");
            final String md = TiptapMarkdown.toMarkdown(body);

            final JsonNode reparsed;
            try {
                reparsed = TiptapMarkdown.toTiptap(md);
            } catch (final Exception e) {
                fail("toTiptap threw for blog '" + title + "': " + e.getMessage());
                return;
            }

            assertEquals("re-parsed root must be a doc for blog '" + title + "'",
                    "doc", reparsed.path("type").asText());
            assertTrue("re-parsed doc must have content for blog '" + title + "'",
                    reparsed.path("content").isArray()
                            && reparsed.path("content").size() > 0);
        }
    }

    /**
     * After one normalization pass (markdown → tiptap → markdown → tiptap),
     * a second pass through the same pipeline must produce identical JSON —
     * i.e. the converter reaches a fixed point. Run on every blog body.
     */
    @Test
    public void every_blog_body_reaches_a_stable_fixed_point() {
        for (final JsonNode c : contentlets) {
            final String title = c.path("title").asText("<no title>");
            final JsonNode body = c.path("body");

            final String md1 = TiptapMarkdown.toMarkdown(body);
            final JsonNode once = TiptapMarkdown.toTiptap(md1);
            final String md2 = TiptapMarkdown.toMarkdown(once);
            final JsonNode twice = TiptapMarkdown.toTiptap(md2);

            assertEquals("conversion must be a fixed point for blog '" + title + "'",
                    once, twice);
        }
    }

    /**
     * Spot-check that distinctive text from each blog survives the conversion.
     * Pull the first text node out of the body and assert it appears in the
     * generated markdown (with markdown special chars stripped for the compare).
     */
    @Test
    public void first_text_snippet_appears_in_rendered_markdown() {
        for (final JsonNode c : contentlets) {
            final String title = c.path("title").asText("<no title>");
            final String firstText = findFirstText(c.path("body"));
            if (firstText == null || firstText.length() < 12) {
                // Some blogs may start with an image or table — skip those for this check.
                continue;
            }
            final String snippet = firstText.substring(0, Math.min(40, firstText.length()));
            final String md = TiptapMarkdown.toMarkdown(c.path("body"));

            // Compare after stripping markdown escape backslashes so escaping doesn't
            // cause a false negative on the substring search.
            final String stripped = md.replace("\\", "");
            assertTrue("blog '" + title + "': expected to find snippet '"
                            + snippet + "' in markdown",
                    stripped.contains(snippet));
        }
    }

    /**
     * Code mark content should appear literally in the output (no backslash
     * escaping), since inline-code text is rendered as-is between backticks.
     */
    @Test
    public void inline_code_content_appears_literally_in_markdown() {
        // Find a blog with inline code (the providerConfig migration blog certainly has it).
        for (final JsonNode c : contentlets) {
            final String literal = findFirstTextWithMark(c.path("body"), "code");
            if (literal == null || literal.isEmpty()) continue;

            final String md = TiptapMarkdown.toMarkdown(c.path("body"));
            assertTrue("inline-code text '" + literal + "' must appear literally in markdown",
                    md.contains("`" + literal + "`"));
            return; // one positive case is enough — others are covered by other tests
        }
        // If no blog had inline code, the fixture changed — note it but don't fail.
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static void collectTypes(final JsonNode n,
                                     final Set<String> nodes, final Set<String> marks) {
        if (n == null || n.isMissingNode() || n.isNull()) return;
        final String t = n.path("type").asText("");
        if (!t.isEmpty()) nodes.add(t);
        for (final JsonNode m : n.path("marks")) {
            final String mt = m.path("type").asText("");
            if (!mt.isEmpty()) marks.add(mt);
        }
        for (final JsonNode c : n.path("content")) {
            collectTypes(c, nodes, marks);
        }
    }

    private static boolean containsType(final JsonNode n, final String type) {
        if (n == null) return false;
        if (type.equals(n.path("type").asText())) return true;
        for (final JsonNode c : n.path("content")) {
            if (containsType(c, type)) return true;
        }
        return false;
    }

    private static String findFirstText(final JsonNode n) {
        if (n == null) return null;
        if ("text".equals(n.path("type").asText())) {
            final String s = n.path("text").asText("");
            if (!s.isEmpty()) return s;
        }
        for (final JsonNode c : n.path("content")) {
            final String hit = findFirstText(c);
            if (hit != null) return hit;
        }
        return null;
    }

    private static String findFirstTextWithMark(final JsonNode n, final String markType) {
        if (n == null) return null;
        if ("text".equals(n.path("type").asText())) {
            for (final JsonNode m : n.path("marks")) {
                if (markType.equals(m.path("type").asText())) {
                    return n.path("text").asText("");
                }
            }
        }
        for (final JsonNode c : n.path("content")) {
            final String hit = findFirstTextWithMark(c, markType);
            if (hit != null) return hit;
        }
        return null;
    }
}
