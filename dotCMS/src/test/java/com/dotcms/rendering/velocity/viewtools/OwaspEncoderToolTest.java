package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link OwaspEncoderTool}.
 *
 * <p>Verifies that each context-specific encoder delegates correctly to the OWASP Java Encoder
 * library, that null inputs return empty strings, and that the URL safety helpers
 * ({@code validateUrl}, {@code urlHasXSS}, {@code cleanUrl}) behave correctly.</p>
 */
public class OwaspEncoderToolTest {

    private OwaspEncoderTool tool;

    @Before
    public void setUp() {
        tool = new OwaspEncoderTool();
        tool.init(null);
    }

    // -------------------------------------------------------------------------
    // forHtml
    // -------------------------------------------------------------------------

    /**
     * Given an input containing a script tag,
     * When encoded for HTML body content,
     * Then angle brackets are replaced with HTML entities so the tag cannot execute.
     */
    @Test
    public void forHtml_encodesScriptTag() {
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;",
                tool.forHtml("<script>alert(1)</script>"));
    }

    /**
     * Given an input containing an ampersand,
     * When encoded for HTML body content,
     * Then the ampersand is replaced with {@code &amp;}.
     */
    @Test
    public void forHtml_encodesAmpersand() {
        assertTrue(tool.forHtml("a & b").contains("&amp;"));
    }

    /**
     * Given a null input,
     * When encoded for HTML body content,
     * Then an empty string is returned instead of throwing.
     */
    @Test
    public void forHtml_returnsEmptyStringForNull() {
        assertEquals("", tool.forHtml(null));
    }

    /**
     * Given plain text with no special characters,
     * When encoded for HTML body content,
     * Then the value is returned unchanged.
     */
    @Test
    public void forHtml_passesThroughPlainText() {
        assertEquals("Hello World", tool.forHtml("Hello World"));
    }

    // -------------------------------------------------------------------------
    // forHtmlContent
    // -------------------------------------------------------------------------

    /**
     * Given an input containing angle brackets,
     * When encoded for HTML text content (element body, not attribute),
     * Then raw angle brackets do not appear in the output.
     */
    @Test
    public void forHtmlContent_encodesAngleBrackets() {
        final String result = tool.forHtmlContent("<b>bold</b>");
        assertFalse("Raw angle bracket must not appear", result.contains("<b>"));
    }

    /**
     * Given a null input,
     * When encoded for HTML text content,
     * Then an empty string is returned.
     */
    @Test
    public void forHtmlContent_returnsEmptyStringForNull() {
        assertEquals("", tool.forHtmlContent(null));
    }

    // -------------------------------------------------------------------------
    // forHtmlAttribute
    // -------------------------------------------------------------------------

    /**
     * Given an input containing a double-quote followed by an event handler payload,
     * When encoded for a quoted HTML attribute value,
     * Then the double-quote is encoded so the attribute cannot be broken out of.
     */
    @Test
    public void forHtmlAttribute_encodesDoubleQuote() {
        final String result = tool.forHtmlAttribute("\" onmouseover=\"alert(1)");
        assertFalse("Unencoded double-quote must not appear", result.contains("\""));
    }

    /**
     * Given an input containing a single-quote followed by an event handler payload,
     * When encoded for a quoted HTML attribute value,
     * Then the single-quote is encoded so the attribute cannot be broken out of.
     */
    @Test
    public void forHtmlAttribute_encodesSingleQuote() {
        final String result = tool.forHtmlAttribute("' onmouseover='alert(1)");
        assertFalse("Unencoded single-quote must not appear", result.contains("'"));
    }

    /**
     * Given a null input,
     * When encoded for an HTML attribute,
     * Then an empty string is returned.
     */
    @Test
    public void forHtmlAttribute_returnsEmptyStringForNull() {
        assertEquals("", tool.forHtmlAttribute(null));
    }

    // -------------------------------------------------------------------------
    // forHtmlUnquotedAttribute
    // -------------------------------------------------------------------------

    /**
     * Given an input containing spaces,
     * When encoded for an unquoted HTML attribute value,
     * Then spaces are encoded so they cannot delimit a new attribute.
     */
    @Test
    public void forHtmlUnquotedAttribute_encodesSpaceAndQuotes() {
        final String result = tool.forHtmlUnquotedAttribute("value with spaces");
        assertFalse("Space must be encoded for unquoted attribute", result.contains(" "));
    }

    /**
     * Given a null input,
     * When encoded for an unquoted HTML attribute,
     * Then an empty string is returned.
     */
    @Test
    public void forHtmlUnquotedAttribute_returnsEmptyStringForNull() {
        assertEquals("", tool.forHtmlUnquotedAttribute(null));
    }

    // -------------------------------------------------------------------------
    // forCssString
    // -------------------------------------------------------------------------

    /**
     * Given an input containing a single-quote CSS breakout payload,
     * When encoded for a CSS string literal (surrounded by quotes),
     * Then the single-quote is encoded so the string cannot be escaped.
     */
    @Test
    public void forCssString_encodesSingleQuote() {
        final String result = tool.forCssString("'; } body { color: red; x: '");
        assertFalse("Single quote must be encoded for CSS string breakout prevention",
                result.contains("'"));
    }

    /**
     * Given a null input,
     * When encoded for a CSS string,
     * Then an empty string is returned.
     */
    @Test
    public void forCssString_returnsEmptyStringForNull() {
        assertEquals("", tool.forCssString(null));
    }

    // -------------------------------------------------------------------------
    // forCssUrl
    // -------------------------------------------------------------------------

    /**
     * Given an input containing single-quotes,
     * When encoded for a CSS url() context,
     * Then the single-quotes are encoded.
     */
    @Test
    public void forCssUrl_encodesQuotes() {
        final String result = tool.forCssUrl("'malicious'");
        assertFalse("Single quote must be encoded in CSS URL context", result.contains("'"));
    }

    /**
     * Given a null input,
     * When encoded for a CSS url(),
     * Then an empty string is returned.
     */
    @Test
    public void forCssUrl_returnsEmptyStringForNull() {
        assertEquals("", tool.forCssUrl(null));
    }

    // -------------------------------------------------------------------------
    // forUriComponent
    // -------------------------------------------------------------------------

    /**
     * Given an input containing spaces and ampersands,
     * When encoded for a URI component (query param value, path segment),
     * Then both characters are percent-encoded so they cannot break the URL structure.
     */
    @Test
    public void forUriComponent_encodesSpaceAndSpecialChars() {
        final String result = tool.forUriComponent("hello world & more");
        assertFalse("Space must be percent-encoded", result.contains(" "));
        assertFalse("Ampersand must be percent-encoded", result.contains("&"));
    }

    /**
     * Given an input containing angle brackets (a script tag),
     * When encoded for a URI component,
     * Then angle brackets are percent-encoded.
     */
    @Test
    public void forUriComponent_encodesAngleBrackets() {
        final String result = tool.forUriComponent("<script>alert(1)</script>");
        assertFalse("Angle bracket must be percent-encoded", result.contains("<"));
    }

    /**
     * Given an input consisting entirely of unreserved URI characters (letters, digits, {@code -._~}),
     * When encoded for a URI component,
     * Then the value is returned unchanged (no over-encoding).
     */
    @Test
    public void forUriComponent_preservesUnreservedChars() {
        final String safe = "hello-world_123~";
        assertEquals("Unreserved URI chars must not be encoded", safe, tool.forUriComponent(safe));
    }

    /**
     * Given a null input,
     * When encoded for a URI component,
     * Then an empty string is returned.
     */
    @Test
    public void forUriComponent_returnsEmptyStringForNull() {
        assertEquals("", tool.forUriComponent(null));
    }

    // -------------------------------------------------------------------------
    // forJavaScript
    // -------------------------------------------------------------------------

    /**
     * Given an input containing a single-quote JS string breakout payload,
     * When encoded for a JavaScript string literal,
     * Then the single-quote is encoded so the string cannot be terminated early.
     */
    @Test
    public void forJavaScript_encodesSingleQuote() {
        final String result = tool.forJavaScript("'; alert(1); var x='");
        assertFalse("Single quote must be encoded for JS string breakout prevention",
                result.contains("'"));
    }

    /**
     * Given an input containing a backslash,
     * When encoded for a JavaScript string literal,
     * Then the backslash is doubled so it cannot act as an escape character.
     */
    @Test
    public void forJavaScript_encodesBackslash() {
        final String result = tool.forJavaScript("back\\slash");
        assertTrue("Backslash must be doubled in JS output", result.contains("\\\\"));
    }

    /**
     * Given a null input,
     * When encoded for JavaScript,
     * Then an empty string is returned.
     */
    @Test
    public void forJavaScript_returnsEmptyStringForNull() {
        assertEquals("", tool.forJavaScript(null));
    }

    // -------------------------------------------------------------------------
    // forJavaScriptAttribute / forJavaScriptBlock / forJavaScriptSource
    // -------------------------------------------------------------------------

    /**
     * Given an input containing a single-quote,
     * When encoded for an inline HTML event attribute (e.g. {@code onclick='...'}),
     * Then the single-quote is encoded.
     */
    @Test
    public void forJavaScriptAttribute_encodesSingleQuote() {
        assertFalse(tool.forJavaScriptAttribute("'").contains("'"));
    }

    /**
     * Given a null input,
     * When encoded for a JavaScript event attribute,
     * Then an empty string is returned.
     */
    @Test
    public void forJavaScriptAttribute_returnsEmptyStringForNull() {
        assertEquals("", tool.forJavaScriptAttribute(null));
    }

    /**
     * Given an input containing a {@code </script>} closing tag,
     * When encoded for a JavaScript {@code <script>} block,
     * Then the tag is encoded so the HTML parser cannot prematurely close the script block.
     * Note: single-quotes are NOT encoded in this context — they are safe inside script blocks.
     */
    @Test
    public void forJavaScriptBlock_encodesScriptCloseTag() {
        final String result = tool.forJavaScriptBlock("</script>");
        assertFalse("</script> breakout must be prevented in script-block context",
                result.contains("</script>"));
    }

    /**
     * Given a null input,
     * When encoded for a JavaScript script block,
     * Then an empty string is returned.
     */
    @Test
    public void forJavaScriptBlock_returnsEmptyStringForNull() {
        assertEquals("", tool.forJavaScriptBlock(null));
    }

    /**
     * Given an input containing a backslash,
     * When encoded for a standalone JavaScript source file,
     * Then the backslash is doubled.
     * Note: single-quotes and angle brackets are safe in a pure JS source context (no HTML parser).
     */
    @Test
    public void forJavaScriptSource_encodesBackslash() {
        final String result = tool.forJavaScriptSource("back\\slash");
        assertTrue("Backslash must be doubled in JS source output", result.contains("\\\\"));
    }

    /**
     * Given a null input,
     * When encoded for a JavaScript source file,
     * Then an empty string is returned.
     */
    @Test
    public void forJavaScriptSource_returnsEmptyStringForNull() {
        assertEquals("", tool.forJavaScriptSource(null));
    }

    // -------------------------------------------------------------------------
    // forXml family
    // -------------------------------------------------------------------------

    /**
     * Given an XML element string with angle brackets,
     * When encoded for XML/XHTML content,
     * Then the angle brackets are replaced with HTML entities.
     */
    @Test
    public void forXml_encodesAngleBrackets() {
        final String result = tool.forXml("<element/>");
        assertFalse("Angle bracket must be encoded", result.contains("<element/>"));
        assertTrue(result.contains("&lt;"));
    }

    /** Given a null input, When encoded for XML, Then an empty string is returned. */
    @Test
    public void forXml_returnsEmptyStringForNull() {
        assertEquals("", tool.forXml(null));
    }

    /** Given a null input, When encoded for XML content, Then an empty string is returned. */
    @Test
    public void forXmlContent_returnsEmptyStringForNull() {
        assertEquals("", tool.forXmlContent(null));
    }

    /** Given a null input, When encoded for an XML attribute, Then an empty string is returned. */
    @Test
    public void forXmlAttribute_returnsEmptyStringForNull() {
        assertEquals("", tool.forXmlAttribute(null));
    }

    /** Given a null input, When encoded for an XML comment, Then an empty string is returned. */
    @Test
    public void forXmlComment_returnsEmptyStringForNull() {
        assertEquals("", tool.forXmlComment(null));
    }

    /** Given a null input, When encoded for a CDATA section, Then an empty string is returned. */
    @Test
    public void forCDATA_returnsEmptyStringForNull() {
        assertEquals("", tool.forCDATA(null));
    }

    // -------------------------------------------------------------------------
    // forJava
    // -------------------------------------------------------------------------

    /**
     * Given an input containing a backslash,
     * When encoded for a Java string literal,
     * Then the backslash is doubled.
     */
    @Test
    public void forJava_encodesBackslash() {
        final String result = tool.forJava("back\\slash");
        assertTrue("Backslash must be doubled in Java string output", result.contains("\\\\"));
    }

    /**
     * Given a null input,
     * When encoded for a Java string literal,
     * Then an empty string is returned.
     */
    @Test
    public void forJava_returnsEmptyStringForNull() {
        assertEquals("", tool.forJava(null));
    }

    // -------------------------------------------------------------------------
    // URL safety helpers — validateUrl
    // -------------------------------------------------------------------------

    /**
     * Given a well-formed https URL,
     * When validated,
     * Then {@code true} is returned.
     */
    @Test
    public void validateUrl_acceptsValidHttpsUrl() {
        assertTrue(tool.validateUrl("https://www.dotcms.com/page?q=1"));
    }

    /**
     * Given a {@code javascript:} URI,
     * When validated (only http/https are accepted),
     * Then {@code false} is returned.
     */
    @Test
    public void validateUrl_rejectsJavascriptScheme() {
        assertFalse(tool.validateUrl("javascript:alert(1)"));
    }

    /**
     * Given a null input,
     * When validated,
     * Then {@code false} is returned.
     */
    @Test
    public void validateUrl_rejectsNull() {
        assertFalse(tool.validateUrl(null));
    }

    /**
     * Given a malformed string that is not a URL,
     * When validated,
     * Then {@code false} is returned.
     */
    @Test
    public void validateUrl_rejectsMalformed() {
        assertFalse(tool.validateUrl("not a url"));
    }

    // -------------------------------------------------------------------------
    // URL safety helpers — urlHasXSS
    // -------------------------------------------------------------------------

    /**
     * Given a URL whose query parameters contain only safe values,
     * When checked for XSS,
     * Then {@code false} is returned.
     */
    @Test
    public void urlHasXSS_returnsFalseForCleanUrl() {
        assertFalse(tool.urlHasXSS("https://www.dotcms.com/page?name=hello"));
    }

    /**
     * Given a URL whose query parameter value contains a percent-encoded {@code <script>} tag,
     * When checked for XSS,
     * Then {@code true} is returned because the decoded value contains HTML-unsafe characters.
     */
    @Test
    public void urlHasXSS_returnsTrueWhenParamContainsHtmlTags() {
        assertTrue(tool.urlHasXSS("https://www.dotcms.com/page?q=%3Cscript%3Ealert(1)%3C%2Fscript%3E"));
    }

    /**
     * Given a string that is not a valid URL,
     * When checked for XSS (validation fails before parameter parsing),
     * Then {@code false} is returned.
     */
    @Test
    public void urlHasXSS_returnsFalseForInvalidUrl() {
        assertFalse(tool.urlHasXSS("not a url"));
    }

    /**
     * Given a null input,
     * When checked for XSS,
     * Then {@code false} is returned.
     */
    @Test
    public void urlHasXSS_returnsFalseForNull() {
        assertFalse(tool.urlHasXSS(null));
    }

    // -------------------------------------------------------------------------
    // URL safety helpers — cleanUrl
    // -------------------------------------------------------------------------

    /**
     * Given a valid URL with no special characters,
     * When cleaned,
     * Then the URL is returned HTML-attribute-encoded (unchanged for a plain URL).
     */
    @Test
    public void cleanUrl_returnsEncodedUrlForValidInput() {
        final String result = tool.cleanUrl("https://www.dotcms.com/page");
        assertEquals("https://www.dotcms.com/page", result);
    }

    /**
     * Given a {@code javascript:} URI (fails validation),
     * When cleaned,
     * Then {@code null} is returned to signal the URL is unsafe.
     */
    @Test
    public void cleanUrl_returnsNullForInvalidUrl() {
        assertNull(tool.cleanUrl("javascript:alert(1)"));
    }

    /**
     * Given a null input,
     * When cleaned,
     * Then {@code null} is returned.
     */
    @Test
    public void cleanUrl_returnsNullForNull() {
        assertNull(tool.cleanUrl(null));
    }
}
