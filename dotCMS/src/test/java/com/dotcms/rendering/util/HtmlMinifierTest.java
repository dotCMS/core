package com.dotcms.rendering.util;

import static org.junit.Assert.assertEquals;

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
     * Given: null or empty input.
     * Expected: it is returned untouched rather than throwing.
     */
    @Test
    public void test_minifyIfEnabled_handles_null_and_empty() {
        assertEquals(null, HtmlMinifier.minifyIfEnabled(null));
        assertEquals("", HtmlMinifier.minifyIfEnabled(""));
    }
}
