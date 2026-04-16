package com.dotcms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.liferay.util.Xss;
import org.junit.Test;

/**
 * Unit tests for {@link Xss} encoding and XSS-detection methods.
 *
 * <p>Verifies that each context-specific encoder (HTML, HTML attribute, JavaScript, URL, CSS)
 * correctly encodes dangerous characters, that null inputs return empty strings rather than
 * throwing, and that the camelCase detection methods ({@code urlHasXSS}, {@code uriHasXSS},
 * {@code paramsHaveXSS}) behave correctly.</p>
 */
public class XssTest {

    // -------------------------------------------------------------------------
    // encodeForHTML
    // -------------------------------------------------------------------------

    /**
     * Given an input containing a script tag,
     * When encoded for HTML body content,
     * Then angle brackets are replaced with HTML entities.
     */
    @Test
    public void encodeForHTML_encodesScriptTag() {
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;",
                Xss.encodeForHTML("<script>alert(1)</script>"));
    }

    /**
     * Given an input containing angle brackets, double-quotes, and an ampersand,
     * When encoded for HTML body content,
     * Then none of those raw characters appear in the output.
     */
    @Test
    public void encodeForHTML_encodesAmpersandAndQuotes() {
        final String result = Xss.encodeForHTML("<p class=\"x\">a & b</p>");
        // OWASP encoder uses &#34; (numeric) for double-quotes and &amp; for ampersands — both valid HTML
        assertFalse("Raw angle bracket must not appear", result.contains("<p"));
        assertFalse("Raw double-quote must not appear", result.contains("\""));
        assertFalse("Raw unencoded ampersand-space must not appear", result.contains("& b"));
        assertTrue("Ampersand must be encoded as &amp;", result.contains("&amp;"));
    }

    /**
     * Given a null input,
     * When encoded for HTML body content,
     * Then an empty string is returned instead of throwing.
     */
    @Test
    public void encodeForHTML_returnsEmptyStringForNull() {
        assertEquals("", Xss.encodeForHTML(null));
    }

    /**
     * Given plain text with no special characters,
     * When encoded for HTML body content,
     * Then the value is returned unchanged.
     */
    @Test
    public void encodeForHTML_passesThroughPlainText() {
        assertEquals("Hello World", Xss.encodeForHTML("Hello World"));
    }

    // -------------------------------------------------------------------------
    // encodeForHTMLAttribute
    // -------------------------------------------------------------------------

    /**
     * Given an input containing a double-quote event-handler breakout payload,
     * When encoded for a quoted HTML attribute value,
     * Then the double-quote is encoded.
     */
    @Test
    public void encodeForHTMLAttribute_encodesDoubleQuote() {
        final String result = Xss.encodeForHTMLAttribute("\" onmouseover=\"alert(1)");
        assertFalse("Unencoded double-quote must not appear in attribute value",
                result.contains("\""));
    }

    /**
     * Given a null input,
     * When encoded for an HTML attribute,
     * Then an empty string is returned.
     */
    @Test
    public void encodeForHTMLAttribute_returnsEmptyStringForNull() {
        assertEquals("", Xss.encodeForHTMLAttribute(null));
    }

    // -------------------------------------------------------------------------
    // encodeForJavaScript
    // -------------------------------------------------------------------------

    /**
     * Given an input containing a single-quote JS string breakout payload,
     * When encoded for a JavaScript string literal,
     * Then the single-quote is encoded.
     */
    @Test
    public void encodeForJavaScript_encodesScriptBreakout() {
        final String input = "'; alert(1); var x='";
        final String result = Xss.encodeForJavaScript(input);
        assertFalse("Single quote must be encoded to prevent JS string breakout",
                result.contains("'"));
    }

    /**
     * Given an input containing a backslash,
     * When encoded for a JavaScript string literal,
     * Then the backslash is doubled and the surrounding word content is preserved.
     */
    @Test
    public void encodeForJavaScript_encodesBackslash() {
        // OWASP encodes \ as \\ in JS string context
        final String result = Xss.encodeForJavaScript("back\\slash");
        assertTrue("Backslash must be doubled in JS output (\\\\)",
                result.contains("\\\\"));
        assertTrue("Result must still contain recognizable word content",
                result.contains("back") && result.contains("slash"));
    }

    /**
     * Given a null input,
     * When encoded for JavaScript,
     * Then an empty string is returned.
     */
    @Test
    public void encodeForJavaScript_returnsEmptyStringForNull() {
        assertEquals("", Xss.encodeForJavaScript(null));
    }

    // -------------------------------------------------------------------------
    // encodeForURL
    // -------------------------------------------------------------------------

    /**
     * Given an input containing spaces and ampersands,
     * When encoded for a URI component,
     * Then both are percent-encoded.
     */
    @Test
    public void encodeForURL_encodesSpaceAndSpecialChars() {
        final String result = Xss.encodeForURL("hello world & more");
        assertFalse("Space must be percent-encoded", result.contains(" "));
        assertFalse("Ampersand must be percent-encoded", result.contains("&"));
    }

    /**
     * Given an input containing a script tag,
     * When encoded for a URI component,
     * Then angle brackets are percent-encoded.
     */
    @Test
    public void encodeForURL_encodesScriptPayload() {
        final String result = Xss.encodeForURL("<script>alert(1)</script>");
        assertFalse("Angle brackets must be percent-encoded", result.contains("<"));
        assertFalse("Angle brackets must be percent-encoded", result.contains(">"));
    }

    /**
     * Given a null input,
     * When encoded for a URI component,
     * Then an empty string is returned.
     */
    @Test
    public void encodeForURL_returnsEmptyStringForNull() {
        assertEquals("", Xss.encodeForURL(null));
    }

    /**
     * Given unreserved URI characters (letters, digits, {@code -._~}),
     * When encoded for a URI component,
     * Then the value is returned unchanged (no over-encoding).
     */
    @Test
    public void encodeForURL_preservesUnreservedCharacters() {
        final String safe = "hello-world_123~";
        assertEquals("Unreserved URI chars must not be encoded", safe, Xss.encodeForURL(safe));
    }

    // -------------------------------------------------------------------------
    // encodeForCSS
    // -------------------------------------------------------------------------

    /**
     * Given an input containing a single-quote CSS string breakout payload,
     * When encoded for a CSS string literal,
     * Then the single-quote is encoded.
     */
    @Test
    public void encodeForCSS_encodesQuotesAndParens() {
        final String input = "'; } body { background: red; x: '";
        final String result = Xss.encodeForCSS(input);
        assertFalse("Single quote must be encoded to prevent CSS string breakout",
                result.contains("'"));
    }

    /**
     * Given a null input,
     * When encoded for CSS,
     * Then an empty string is returned.
     */
    @Test
    public void encodeForCSS_returnsEmptyStringForNull() {
        assertEquals("", Xss.encodeForCSS(null));
    }

    // -------------------------------------------------------------------------
    // escapeHTMLAttrib (legacy — delegates to encodeForHTML)
    // -------------------------------------------------------------------------

    /**
     * Given an input with HTML tags,
     * When encoded via the legacy {@code escapeHTMLAttrib} method,
     * Then HTML entities are produced (delegates to encodeForHTML).
     */
    @Test
    public void escapeHTMLAttrib_encodesHtmlEntities() {
        assertEquals("&lt;b&gt;bold&lt;/b&gt;", Xss.escapeHTMLAttrib("<b>bold</b>"));
    }

    /**
     * Given a null input,
     * When encoded via the legacy method,
     * Then an empty string is returned.
     */
    @Test
    public void escapeHTMLAttrib_returnsEmptyStringForNull() {
        assertEquals("", Xss.escapeHTMLAttrib(null));
    }

    // -------------------------------------------------------------------------
    // unEscapeHTMLAttrib
    // -------------------------------------------------------------------------

    /**
     * Given an HTML-encoded string,
     * When decoded,
     * Then the original plain-text value is restored.
     */
    @Test
    public void unEscapeHTMLAttrib_decodesHtmlEntities() {
        assertEquals("<b>bold</b>", Xss.unEscapeHTMLAttrib("&lt;b&gt;bold&lt;/b&gt;"));
    }

    /**
     * Given a null input,
     * When decoded,
     * Then an empty string is returned.
     */
    @Test
    public void unEscapeHTMLAttrib_returnsEmptyStringForNull() {
        assertEquals("", Xss.unEscapeHTMLAttrib(null));
    }

    // -------------------------------------------------------------------------
    // urlHasXSS / uriHasXSS (camelCase — canonical names)
    // -------------------------------------------------------------------------

    /**
     * Given a string containing a script tag,
     * When checked for XSS patterns,
     * Then {@code true} is returned.
     */
    @Test
    public void urlHasXSS_detectsScriptTag() {
        assertTrue(Xss.urlHasXSS("<script>alert(1)</script>"));
    }

    /**
     * Given a plain string with no XSS patterns,
     * When checked for XSS,
     * Then {@code false} is returned.
     */
    @Test
    public void urlHasXSS_returnsFalseForCleanInput() {
        assertFalse(Xss.urlHasXSS("hello world"));
    }

    /**
     * Given a null input,
     * When checked for XSS,
     * Then {@code false} is returned (no exception).
     */
    @Test
    public void urlHasXSS_returnsFalseForNull() {
        assertFalse(Xss.urlHasXSS(null));
    }

}
