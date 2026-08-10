package com.dotcms.rendering.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.util.Config;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for {@link HtmlMinifier}.
 * <p>
 * These cover the two things that make HTML minification risky: whitespace that looks removable but
 * is actually rendered (between inline elements), and regions whose content must never be touched
 * ({@code pre}, {@code textarea}, {@code script}, {@code style}).
 *
 * @author Freddy Montes
 */
public class HtmlMinifierTest {

    @BeforeClass
    public static void prepare() {
        Config.initializeConfig();
    }

    /**
     * Given: markup indented with newlines between block elements.
     * Expected: the insignificant whitespace is removed entirely.
     */
    @Test
    public void test_minify_removes_whitespace_between_block_elements() {
        assertEquals("<div><p>Hello</p></div>",
                HtmlMinifier.minify("<div>\n    <p>Hello</p>\n</div>"));
        assertEquals("<div>a</div><div>b</div>",
                HtmlMinifier.minify("<div>a</div>\n<div>b</div>"));
        assertEquals("<ul><li>a</li><li>b</li></ul>",
                HtmlMinifier.minify("<ul>\n  <li>a</li>\n  <li>b</li>\n</ul>"));
        assertEquals("<head><meta charset=\"utf-8\"><title>T</title></head>",
                HtmlMinifier.minify("<head>\n  <meta charset=\"utf-8\">\n  <title>T</title>\n</head>"));
    }

    /**
     * Given: whitespace separating inline elements, where the space is actually rendered.
     * Expected: a single space survives, so words are never joined together.
     */
    @Test
    public void test_minify_keeps_significant_whitespace_between_inline_elements() {
        assertEquals("<span>a</span> <span>b</span>",
                HtmlMinifier.minify("<span>a</span> <span>b</span>"));
        assertEquals("<span>a</span> <span>b</span>",
                HtmlMinifier.minify("<span>a</span>\n   <span>b</span>"));
        assertEquals("<a href=\"#\">one</a> <a href=\"#\">two</a>",
                HtmlMinifier.minify("<a href=\"#\">one</a>\n<a href=\"#\">two</a>"));
        assertEquals("<img src=\"a\"> <img src=\"b\">",
                HtmlMinifier.minify("<img src=\"a\"> <img src=\"b\">"));
    }

    /**
     * Given: text adjacent to inline markup.
     * Expected: the separating space is preserved on both sides.
     */
    @Test
    public void test_minify_keeps_whitespace_around_text() {
        assertEquals("Hello <b>world</b>", HtmlMinifier.minify("Hello <b>world</b>"));
        assertEquals("<b>Hello</b> world", HtmlMinifier.minify("<b>Hello</b> world"));
        assertEquals("<p>Hello world</p>", HtmlMinifier.minify("<p>Hello   world</p>"));
        assertEquals("<p><span>a</span> <span>b</span></p>",
                HtmlMinifier.minify("<p>\n  <span>a</span> <span>b</span>\n</p>"));
    }

    /**
     * Given: elements whose content is whitespace sensitive.
     * Expected: their content is copied through byte for byte.
     */
    @Test
    public void test_minify_preserves_whitespace_sensitive_elements() {
        assertEquals("<div><pre>line1\n   line2</pre></div>",
                HtmlMinifier.minify("<div>\n<pre>line1\n   line2</pre>\n</div>"));
        assertEquals("<textarea>a\n  b</textarea>",
                HtmlMinifier.minify("<textarea>a\n  b</textarea>"));
        assertEquals("<style>\n.a { color: red }\n</style>",
                HtmlMinifier.minify("<style>\n.a { color: red }\n</style>"));
        assertEquals("<PRE>a\n b</PRE>", HtmlMinifier.minify("<PRE>a\n b</PRE>"));
    }

    /**
     * Given: JavaScript relying on newlines for automatic semicolon insertion.
     * Expected: the script body is untouched, so the script keeps working.
     */
    @Test
    public void test_minify_does_not_break_javascript_asi() {
        assertEquals("<script>\nvar a=1\nvar b=2\n</script>",
                HtmlMinifier.minify("<script>\nvar a=1\nvar b=2\n</script>"));
    }

    /**
     * Given: preserved regions whose closing tag varies in case or carries trailing whitespace.
     * Expected: the region's end is still found, so the markup <i>after</i> it is minified as normal.
     * <p>
     * This guards a failure mode nothing else here can see. A lookup that misses the closing tag
     * falls back to the end of the document and copies the remainder verbatim, which is perfectly
     * faithful -- so every integrity check still passes -- while silently minifying nothing from that
     * point on. The only visible symptom is the whitespace after the region surviving, which is what
     * these assertions pin down.
     */
    @Test
    public void test_minify_finds_preserved_region_boundaries() {
        assertEquals("<div><SCRIPT>\nvar a=1\n</SCRIPT><p>x</p></div>",
                HtmlMinifier.minify("<div>\n  <SCRIPT>\nvar a=1\n</SCRIPT>\n  <p>x</p>\n</div>"));
        assertEquals("<div><script>\nvar a=1\n</script ><p>x</p></div>",
                HtmlMinifier.minify("<div>\n  <script>\nvar a=1\n</script >\n  <p>x</p>\n</div>"));
        assertEquals("<div><PRE>a\n b</PRE><p>x</p></div>",
                HtmlMinifier.minify("<div>\n  <PRE>a\n b</PRE>\n  <p>x</p>\n</div>"));
        assertEquals("<div><StYlE>\n.a{color:red}\n</StYlE><p>x</p></div>",
                HtmlMinifier.minify("<div>\n  <StYlE>\n.a{color:red}\n</StYlE>\n  <p>x</p>\n</div>"));
    }

    /**
     * Given: a tag that merely starts with a preserved tag name.
     * Expected: it is not mistaken for the preserved element.
     */
    @Test
    public void test_minify_does_not_match_preserved_tag_prefixes() {
        assertEquals("<scriptural>x</scriptural>",
                HtmlMinifier.minify("<scriptural>\n  x</scriptural>"));
    }

    /**
     * Given: regular and downlevel conditional comments.
     * Expected: regular comments are stripped, conditional comments are kept as markup.
     */
    @Test
    public void test_minify_strips_comments_but_keeps_conditional_ones() {
        assertEquals("<div><p>x</p></div>",
                HtmlMinifier.minify("<div><!-- hi --><p>x</p></div>"));
        assertEquals("<!--[if IE]><b>x</b><![endif]-->",
                HtmlMinifier.minify("<!--[if IE]><b>x</b><![endif]-->"));
        assertEquals("<div>a</div><div>b</div>",
                HtmlMinifier.minify("<div>a</div>  <!-- c -->  <div>b</div>"));
    }

    /**
     * Given: a removed comment sitting between content that whitespace separates.
     * Expected: the separating space survives. A comment is transparent to whitespace collapsing, so
     * what decides significance is the content on the far side of it, not the comment itself.
     * <p>
     * Getting this wrong joins words together: {@code <span>a</span> <!--c--><span>b</span>} renders
     * as "a b" but would collapse to "ab".
     */
    @Test
    public void test_minify_keeps_whitespace_across_a_removed_comment() {
        assertEquals("<span>a</span> <span>b</span>",
                HtmlMinifier.minify("<span>a</span> <!--c--><span>b</span>"));
        assertEquals("<p>a <b>b</b></p>", HtmlMinifier.minify("<p>a <!--c--><b>b</b></p>"));
        assertEquals("<span>a</span> <span>b</span>",
                HtmlMinifier.minify("<span>a</span> <!--c--><!--d--><span>b</span>"));
        // Whitespace on both sides of the comment must still collapse to a single space.
        assertEquals("<span>a</span> <span>b</span>",
                HtmlMinifier.minify("<span>a</span> <!--c--> <span>b</span>"));
        // A comment before a block element keeps nothing alive: the block is what matters.
        assertEquals("<div>a</div><div>b</div>",
                HtmlMinifier.minify("<div>a</div> <!--c--><div>b</div>"));
        assertEquals("<span>a</span><div>b</div>",
                HtmlMinifier.minify("<span>a</span> <!--c--><div>b</div>"));
    }

    /**
     * Given: an element that generates no box ({@code display: none}) sitting between content that
     * whitespace separates.
     * Expected: the separating whitespace survives. Such an element paints nothing, so the runs on
     * either side of it end up adjacent in the inline flow and the whitespace between them is
     * rendered. This is the same transparency a removed comment has.
     * <p>
     * Dropping it joins words: {@code hola <script>x</script> mundo} would render "holamundo".
     */
    @Test
    public void test_minify_keeps_whitespace_beside_invisible_elements() {
        assertEquals("hola <script>var x=1</script> mundo",
                HtmlMinifier.minify("hola <script>var x=1</script> mundo"));
        assertEquals("<b>a</b> <style>.x{}</style> <b>b</b>",
                HtmlMinifier.minify("<b>a</b> <style>.x{}</style> <b>b</b>"));
        assertEquals("<b>a</b> <template><i>t</i></template> <b>b</b>",
                HtmlMinifier.minify("<b>a</b> <template><i>t</i></template> <b>b</b>"));
        assertEquals("<b>a</b> <noscript>n</noscript> <b>b</b>",
                HtmlMinifier.minify("<b>a</b> <noscript>n</noscript> <b>b</b>"));
        // Indentation around an invisible element between block elements is still removable.
        assertEquals("<div>a</div><script>var x=1</script><div>b</div>",
                HtmlMinifier.minify("<div>a</div>\n<script>var x=1</script>\n<div>b</div>"));
    }

    /**
     * Given: replaced inline elements, the kind modern templates are full of.
     * Expected: whitespace beside them is kept, because they sit in an inline formatting context.
     * <p>
     * {@code svg} is the one that matters most in practice: it is how icons are rendered, so
     * {@code Ver <svg/>} losing its space glues the icon to the preceding word.
     */
    @Test
    public void test_minify_keeps_whitespace_beside_replaced_inline_elements() {
        assertEquals("Ver <svg width=\"8\"></svg>",
                HtmlMinifier.minify("Ver <svg width=\"8\"></svg>"));
        assertEquals("<a href=\"#\">x</a> <svg></svg>",
                HtmlMinifier.minify("<a href=\"#\">x</a> <svg></svg>"));
        assertEquals("<b>a</b> <iframe src=\"x\"></iframe> <b>b</b>",
                HtmlMinifier.minify("<b>a</b> <iframe src=\"x\"></iframe> <b>b</b>"));
        assertEquals("<b>a</b> <canvas></canvas> <b>b</b>",
                HtmlMinifier.minify("<b>a</b> <canvas></canvas> <b>b</b>"));
        assertEquals("<b>a</b> <video></video> <b>b</b>",
                HtmlMinifier.minify("<b>a</b> <video></video> <b>b</b>"));
        assertEquals("<b>a</b> <audio></audio> <b>b</b>",
                HtmlMinifier.minify("<b>a</b> <audio></audio> <b>b</b>"));
    }

    /**
     * Given: a retained conditional comment sitting between content that whitespace separates.
     * Expected: the separating space survives on the side <i>before</i> it as well as after.
     * <p>
     * A conditional comment is markup only to browsers nobody ships any more; to every modern engine
     * it is a comment, so it paints nothing and the runs either side become adjacent. The whitespace
     * between them is therefore rendered. It only looked correct before because whitespace on the
     * far side happened to restore it.
     */
    @Test
    public void test_minify_keeps_whitespace_beside_conditional_comments() {
        assertEquals("a <!--[if IE]>x<![endif]-->b",
                HtmlMinifier.minify("a <!--[if IE]>x<![endif]-->b"));
        assertEquals("<b>a</b> <!--[if IE]><b>x</b><![endif]--><b>b</b>",
                HtmlMinifier.minify("<b>a</b> <!--[if IE]><b>x</b><![endif]--><b>b</b>"));
        // Whitespace on both sides must still collapse to a single space on each.
        assertEquals("a <!--[if IE]>x<![endif]--> b",
                HtmlMinifier.minify("a <!--[if IE]>x<![endif]--> b"));
        // A block element either side still has its whitespace removed.
        assertEquals("<div>a</div><!--[if IE]>x<![endif]--><div>b</div>",
                HtmlMinifier.minify("<div>a</div>\n<!--[if IE]>x<![endif]-->\n<div>b</div>"));
    }

    /**
     * Given: a {@code <dialog>} without {@code open}, which the user-agent stylesheet hides.
     * Expected: whitespace beside it survives, like any other element that generates no box.
     */
    @Test
    public void test_minify_keeps_whitespace_beside_dialog() {
        assertEquals("a <dialog>x</dialog> b", HtmlMinifier.minify("a <dialog>x</dialog> b"));
    }

    /**
     * Given: a payload that is not HTML at all, which a VTL page can render through the same seams.
     * Expected: it is returned untouched.
     * <p>
     * Collapsing whitespace changes data rather than formatting here. A run of spaces inside a JSON
     * string is part of the value, and a newline in CSV separates records, so minifying a CSV body
     * merges every row into one line.
     */
    @Test
    public void test_minifyBestEffort_leaves_non_html_payloads_alone() {
        final String json = "{\"msg\": \"hello   world\", \"id\": 1}";
        assertEquals(json, HtmlMinifier.minifyBestEffort(json));
        final String csv = "name,note\nalpha,\"two  spaces\"\n";
        assertEquals(csv, HtmlMinifier.minifyBestEffort(csv));
        final String xml = "<?xml version=\"1.0\"?>\n<root>\n    <item>a   b</item>\n</root>";
        assertEquals(xml, HtmlMinifier.minifyBestEffort(xml));
        final String text = "just  some   text\nover two lines";
        assertEquals(text, HtmlMinifier.minifyBestEffort(text));
        // ... while HTML still goes through.
        assertEquals("<div><p>a</p></div>",
                HtmlMinifier.minifyBestEffort("<div>\n  <p>a</p>\n</div>"));
    }

    /**
     * Given: malformed markup with an unclosed preserved tag.
     * Expected: the content still round trips instead of being truncated or throwing.
     */
    @Test
    public void test_minify_handles_malformed_markup() {
        assertEquals("<div><pre>abc", HtmlMinifier.minify("<div><pre>abc"));
    }

    /**
     * Given: already minified markup.
     * Expected: minifying again is a no-op, which matters because LIVE mode may minify on write and
     * again through {@code eval()}.
     */
    @Test
    public void test_minify_is_idempotent() {
        final String once = HtmlMinifier.minify("<div>\n  <p>a</p>\n</div>\n<span>x</span> <span>y</span>");
        assertEquals(once, HtmlMinifier.minify(once));
        assertEquals("<div><p>a</p></div><span>x</span> <span>y</span>", once);
    }

    /**
     * Given: extra whitespace inside a tag's attribute list, and around the document.
     * Expected: it is collapsed and trimmed.
     */
    @Test
    public void test_minify_collapses_attribute_and_document_whitespace() {
        assertEquals("<div class=\"a\" id=\"b\">x</div>",
                HtmlMinifier.minify("<div  class=\"a\"   id=\"b\">x</div>"));
        assertEquals("<div>x</div>", HtmlMinifier.minify("\n  <div>x</div>\n  "));
    }

    /**
     * Given: whitespace inside a quoted attribute value, which is part of the value rather than
     * markup formatting.
     * Expected: it survives byte for byte. Collapsing it would silently rewrite submitted form
     * values, JSON data attributes and accessible text.
     */
    @Test
    public void test_minify_preserves_whitespace_inside_attribute_values() {
        assertEquals("<input value=\"a    b\">",
                HtmlMinifier.minify("<input value=\"a    b\">"));
        assertEquals("<div data-config='{\"key\": \"a   b\"}'>x</div>",
                HtmlMinifier.minify("<div data-config='{\"key\": \"a   b\"}'>x</div>"));
        assertEquals("<img alt=\"a\nb\">", HtmlMinifier.minify("<img alt=\"a\nb\">"));
        // A '>' inside a quoted value must not be taken for the end of the tag.
        assertEquals("<div title=\"a > b\">x</div>",
                HtmlMinifier.minify("<div title=\"a > b\">x</div>"));
        // Whitespace *between* attributes is still collapsed.
        assertEquals("<input type=\"text\" value=\"a   b\">",
                HtmlMinifier.minify("<input  type=\"text\"   value=\"a   b\">"));
    }

    /**
     * Given: {@code <} and {@code >} used as literal text, which is legal in HTML.
     * Expected: they are treated as text, so the whitespace around them is left alone instead of
     * being judged against whatever tag happens to precede them.
     */
    @Test
    public void test_minify_keeps_literal_angle_brackets_in_text() {
        assertEquals("<p>Home > About</p>", HtmlMinifier.minify("<p>Home > About</p>"));
        assertEquals("<p>3 < 4</p>", HtmlMinifier.minify("<p>3 < 4</p>"));
        assertEquals("<p>a > b > c</p>", HtmlMinifier.minify("<p>a > b > c</p>"));
    }

    /**
     * Given: Unicode spaces that HTML renders rather than collapses, such as the ideographic space
     * used in CJK copy.
     * Expected: they are left untouched, while the ASCII whitespace around them still collapses.
     */
    @Test
    public void test_minify_preserves_unicode_whitespace_that_html_renders() {
        // U+3000, the ideographic space common in CJK copy.
        assertEquals("<p>a\u3000b</p>", HtmlMinifier.minify("<p>a\u3000b</p>"));
        // U+2009, a thin space used for typographic spacing.
        assertEquals("<p>a\u2009b</p>", HtmlMinifier.minify("<p>a\u2009b</p>"));
        assertEquals("<div>\u3000</div>", HtmlMinifier.minify("<div>\u3000</div>"));
        assertEquals("<p>a \u3000 b</p>", HtmlMinifier.minify("<p>a  \u3000  b</p>"));
    }

    /**
     * Given: tags a browser tolerates but a naive parser trips on.
     * Expected: they round trip without throwing.
     */
    @Test
    public void test_minify_handles_edge_case_tags() {
        // The space before a self closing '/' is kept on purpose: dropping it would append the
        // slash to an unquoted attribute value.
        assertEquals("<div><br /></div>", HtmlMinifier.minify("<div>\n<br />\n</div>"));
        assertEquals("<div></></div>", HtmlMinifier.minify("<div>\n</>\n</div>"));
    }

    /**
     * Given: markup where a comment token is only a comment because of the whitespace in it, or
     * only *not* a comment because of it.
     * Expected: the whitespace survives, so minification can never forge or destroy a
     * {@code <!--} / {@code -->} boundary. Whitespace between two pieces of text is always
     * collapsed to a single space rather than removed, which is what guarantees this.
     */
    @Test
    public void test_minify_does_not_forge_comment_boundaries() {
        // '< !--' is not a comment opener. Joining the two would turn the rest into a comment and
        // silently delete it.
        assertEquals("<div>< !-- not a comment --></div>",
                HtmlMinifier.minify("<div>< !-- not a comment --></div>"));
        assertEquals("<div><! -- x --></div>", HtmlMinifier.minify("<div><! -- x --></div>"));
        // Joining '--' and '>' would forge a terminator.
        assertEquals("<div>a -- > b</div>", HtmlMinifier.minify("<div>a -- > b</div>"));
    }

    /**
     * Given: tags that appear inside a comment rather than as real markup.
     * Expected: they are never treated as markup. Comments are resolved before preserved-tag
     * matching, so a commented-out {@code <pre>} does not start a preserved region, and a
     * commented-out {@code </body>} is removed rather than left for a downstream
     * {@code lastIndexOf("</body>")} to match on.
     */
    @Test
    public void test_minify_ignores_tags_inside_comments() {
        assertEquals("<div><p>real</p></div>",
                HtmlMinifier.minify("<div>\n  <!-- <pre>x\n  y</pre> -->\n  <p>real</p>\n</div>"));
        assertEquals("<div><p>real</p></div>",
                HtmlMinifier.minify("<div>\n  <!-- <script>var a=1</script> -->\n  <p>real</p>\n</div>"));
        assertEquals("<body><p>x</p></body></html>",
                HtmlMinifier.minify("<body>\n  <p>x</p>\n</body>\n<!-- </body> -->\n</html>"));
        // A comment inside a script is content, not a comment, so it is left alone.
        assertEquals("<script>\n// <!-- legacy hide\nvar a=1;\n// -->\n</script>",
                HtmlMinifier.minify("<script>\n// <!-- legacy hide\nvar a=1;\n// -->\n</script>"));
    }

    /**
     * Given: a retained downlevel conditional comment with indented content.
     * Expected: it is copied verbatim. Its content is markup for the browsers that read it, and the
     * whitespace in the {@code <!--[if ...]>} token itself is load bearing.
     */
    @Test
    public void test_minify_leaves_conditional_comment_content_verbatim() {
        assertEquals("<!--[if IE]>\n  <link href=\"ie.css\">\n<![endif]-->",
                HtmlMinifier.minify("<!--[if IE]>\n  <link href=\"ie.css\">\n<![endif]-->"));
    }

    /**
     * Given: null or empty input.
     * Expected: it is returned untouched rather than throwing.
     */
    @Test
    public void test_minifyIfEnabled_handles_null_and_empty() {
        assertEquals(null, HtmlMinifier.minifyIfEnabled(null));
        assertEquals("", HtmlMinifier.minifyIfEnabled(""));
    }

    /**
     * Given: the feature flag toggled off and then on.
     * Expected: {@code minifyIfEnabled} is a no-op when off and minifies when on. This covers the
     * wiring the production call sites depend on -- the flag name and the {@link Config} lookup.
     */
    @Test
    public void test_minifyIfEnabled_respects_the_feature_flag() {

        final String html = "<div>\n  <p>a</p>\n</div>";
        final boolean original =
                Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_MINIFY_HTML, false);
        try {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_MINIFY_HTML, false);
            assertFalse(HtmlMinifier.isEnabled());
            assertEquals(html, HtmlMinifier.minifyIfEnabled(html));

            Config.setProperty(FeatureFlagName.FEATURE_FLAG_MINIFY_HTML, true);
            assertTrue(HtmlMinifier.isEnabled());
            assertEquals("<div><p>a</p></div>", HtmlMinifier.minifyIfEnabled(html));
        } finally {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_MINIFY_HTML, original);
        }
    }
}
