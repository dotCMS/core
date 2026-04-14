package com.liferay.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link Xss} encoding methods.
 *
 * <p>Verifies that each context-specific encoder (HTML, HTML attribute, JavaScript, URL, CSS)
 * correctly encodes dangerous characters and that null inputs return empty strings rather than
 * throwing.</p>
 */
public class XssTest {

    // -------------------------------------------------------------------------
    // encodeForHTML
    // -------------------------------------------------------------------------

    @Test
    public void encodeForHTML_encodesScriptTag() {
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;",
                Xss.encodeForHTML("<script>alert(1)</script>"));
    }

    @Test
    public void encodeForHTML_encodesAmpersandAndQuotes() {
        final String result = Xss.encodeForHTML("<p class=\"x\">a & b</p>");
        // OWASP encoder uses &#34; (numeric) for double-quotes and &amp; for ampersands — both valid HTML
        assertFalse("Raw angle bracket must not appear", result.contains("<p"));
        assertFalse("Raw double-quote must not appear", result.contains("\""));
        assertFalse("Raw unencoded ampersand-space must not appear", result.contains("& b"));
        assertTrue("Ampersand must be encoded as &amp;", result.contains("&amp;"));
    }

    @Test
    public void encodeForHTML_returnsEmptyStringForNull() {
        assertEquals("", Xss.encodeForHTML(null));
    }

    @Test
    public void encodeForHTML_passesThroughPlainText() {
        assertEquals("Hello World", Xss.encodeForHTML("Hello World"));
    }

    // -------------------------------------------------------------------------
    // encodeForHTMLAttribute
    // -------------------------------------------------------------------------

    @Test
    public void encodeForHTMLAttribute_encodesDoubleQuote() {
        final String result = Xss.encodeForHTMLAttribute("\" onmouseover=\"alert(1)");
        assertFalse("Unencoded double-quote must not appear in attribute value",
                result.contains("\""));
    }

    @Test
    public void encodeForHTMLAttribute_returnsEmptyStringForNull() {
        assertEquals("", Xss.encodeForHTMLAttribute(null));
    }

    // -------------------------------------------------------------------------
    // encodeForJavaScript
    // -------------------------------------------------------------------------

    @Test
    public void encodeForJavaScript_encodesScriptBreakout() {
        final String input = "'; alert(1); var x='";
        final String result = Xss.encodeForJavaScript(input);
        assertFalse("Single quote must be encoded to prevent JS string breakout",
                result.contains("'"));
    }

    @Test
    public void encodeForJavaScript_encodesBackslash() {
        // OWASP encodes \ as \\ in JS string context
        final String result = Xss.encodeForJavaScript("back\\slash");
        // The single backslash must have been doubled
        assertTrue("Backslash must be doubled in JS output (\\\\)",
                result.contains("\\\\"));
        assertTrue("Result must still contain recognizable word content",
                result.contains("back") && result.contains("slash"));
    }

    @Test
    public void encodeForJavaScript_returnsEmptyStringForNull() {
        assertEquals("", Xss.encodeForJavaScript(null));
    }

    // -------------------------------------------------------------------------
    // encodeForURL
    // -------------------------------------------------------------------------

    @Test
    public void encodeForURL_encodesSpaceAndSpecialChars() {
        final String result = Xss.encodeForURL("hello world & more");
        assertFalse("Space must be percent-encoded", result.contains(" "));
        assertFalse("Ampersand must be percent-encoded", result.contains("&"));
    }

    @Test
    public void encodeForURL_encodesScriptPayload() {
        final String result = Xss.encodeForURL("<script>alert(1)</script>");
        assertFalse("Angle brackets must be percent-encoded", result.contains("<"));
        assertFalse("Angle brackets must be percent-encoded", result.contains(">"));
    }

    @Test
    public void encodeForURL_returnsEmptyStringForNull() {
        assertEquals("", Xss.encodeForURL(null));
    }

    @Test
    public void encodeForURL_preservesUnreservedCharacters() {
        final String safe = "hello-world_123~";
        assertEquals("Unreserved URI chars must not be encoded", safe, Xss.encodeForURL(safe));
    }

    // -------------------------------------------------------------------------
    // encodeForCSS
    // -------------------------------------------------------------------------

    @Test
    public void encodeForCSS_encodesQuotesAndParens() {
        // Inside a CSS string literal, single/double quotes and parens are breakout vectors
        final String input = "'; } body { background: red; x: '";
        final String result = Xss.encodeForCSS(input);
        assertFalse("Single quote must be encoded to prevent CSS string breakout",
                result.contains("'"));
    }

    @Test
    public void encodeForCSS_returnsEmptyStringForNull() {
        assertEquals("", Xss.encodeForCSS(null));
    }

    // -------------------------------------------------------------------------
    // escapeHTMLAttrib (legacy — delegates to encodeForHTML)
    // -------------------------------------------------------------------------

    @Test
    public void escapeHTMLAttrib_encodesHtmlEntities() {
        assertEquals("&lt;b&gt;bold&lt;/b&gt;", Xss.escapeHTMLAttrib("<b>bold</b>"));
    }

    @Test
    public void escapeHTMLAttrib_returnsEmptyStringForNull() {
        assertEquals("", Xss.escapeHTMLAttrib(null));
    }

    // -------------------------------------------------------------------------
    // unEscapeHTMLAttrib
    // -------------------------------------------------------------------------

    @Test
    public void unEscapeHTMLAttrib_decodesHtmlEntities() {
        assertEquals("<b>bold</b>", Xss.unEscapeHTMLAttrib("&lt;b&gt;bold&lt;/b&gt;"));
    }

    @Test
    public void unEscapeHTMLAttrib_returnsEmptyStringForNull() {
        assertEquals("", Xss.unEscapeHTMLAttrib(null));
    }

    // -------------------------------------------------------------------------
    // URLHasXSS / URIHasXSS
    // -------------------------------------------------------------------------

    @Test
    public void URLHasXSS_detectsScriptTag() {
        assertTrue(Xss.URLHasXSS("<script>alert(1)</script>"));
    }

    @Test
    public void URLHasXSS_returnsFalseForCleanInput() {
        assertFalse(Xss.URLHasXSS("hello world"));
    }

    @Test
    public void URLHasXSS_returnsFalseForNull() {
        assertFalse(Xss.URLHasXSS(null));
    }

}
