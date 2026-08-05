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
