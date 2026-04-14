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

    @Test
    public void forHtml_encodesScriptTag() {
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;",
                tool.forHtml("<script>alert(1)</script>"));
    }

    @Test
    public void forHtml_encodesAmpersand() {
        assertTrue(tool.forHtml("a & b").contains("&amp;"));
    }

    @Test
    public void forHtml_returnsEmptyStringForNull() {
        assertEquals("", tool.forHtml(null));
    }

    @Test
    public void forHtml_passesThroughPlainText() {
        assertEquals("Hello World", tool.forHtml("Hello World"));
    }

    // -------------------------------------------------------------------------
    // forHtmlContent
    // -------------------------------------------------------------------------

    @Test
    public void forHtmlContent_encodesAngleBrackets() {
        final String result = tool.forHtmlContent("<b>bold</b>");
        assertFalse("Raw angle bracket must not appear", result.contains("<b>"));
    }

    @Test
    public void forHtmlContent_returnsEmptyStringForNull() {
        assertEquals("", tool.forHtmlContent(null));
    }

    // -------------------------------------------------------------------------
    // forHtmlAttribute
    // -------------------------------------------------------------------------

    @Test
    public void forHtmlAttribute_encodesDoubleQuote() {
        final String result = tool.forHtmlAttribute("\" onmouseover=\"alert(1)");
        assertFalse("Unencoded double-quote must not appear", result.contains("\""));
    }

    @Test
    public void forHtmlAttribute_encodesSingleQuote() {
        final String result = tool.forHtmlAttribute("' onmouseover='alert(1)");
        assertFalse("Unencoded single-quote must not appear", result.contains("'"));
    }

    @Test
    public void forHtmlAttribute_returnsEmptyStringForNull() {
        assertEquals("", tool.forHtmlAttribute(null));
    }

    // -------------------------------------------------------------------------
    // forHtmlUnquotedAttribute
    // -------------------------------------------------------------------------

    @Test
    public void forHtmlUnquotedAttribute_encodesSpaceAndQuotes() {
        final String result = tool.forHtmlUnquotedAttribute("value with spaces");
        assertFalse("Space must be encoded for unquoted attribute", result.contains(" "));
    }

    @Test
    public void forHtmlUnquotedAttribute_returnsEmptyStringForNull() {
        assertEquals("", tool.forHtmlUnquotedAttribute(null));
    }

    // -------------------------------------------------------------------------
    // forCssString
    // -------------------------------------------------------------------------

    @Test
    public void forCssString_encodesSingleQuote() {
        final String result = tool.forCssString("'; } body { color: red; x: '");
        assertFalse("Single quote must be encoded for CSS string breakout prevention",
                result.contains("'"));
    }

    @Test
    public void forCssString_returnsEmptyStringForNull() {
        assertEquals("", tool.forCssString(null));
    }

    // -------------------------------------------------------------------------
    // forCssUrl
    // -------------------------------------------------------------------------

    @Test
    public void forCssUrl_encodesQuotes() {
        final String result = tool.forCssUrl("'malicious'");
        assertFalse("Single quote must be encoded in CSS URL context", result.contains("'"));
    }

    @Test
    public void forCssUrl_returnsEmptyStringForNull() {
        assertEquals("", tool.forCssUrl(null));
    }

    // -------------------------------------------------------------------------
    // forUriComponent
    // -------------------------------------------------------------------------

    @Test
    public void forUriComponent_encodesSpaceAndSpecialChars() {
        final String result = tool.forUriComponent("hello world & more");
        assertFalse("Space must be percent-encoded", result.contains(" "));
        assertFalse("Ampersand must be percent-encoded", result.contains("&"));
    }

    @Test
    public void forUriComponent_encodesAngleBrackets() {
        final String result = tool.forUriComponent("<script>alert(1)</script>");
        assertFalse("Angle bracket must be percent-encoded", result.contains("<"));
    }

    @Test
    public void forUriComponent_preservesUnreservedChars() {
        final String safe = "hello-world_123~";
        assertEquals("Unreserved URI chars must not be encoded", safe, tool.forUriComponent(safe));
    }

    @Test
    public void forUriComponent_returnsEmptyStringForNull() {
        assertEquals("", tool.forUriComponent(null));
    }

    // -------------------------------------------------------------------------
    // forJavaScript
    // -------------------------------------------------------------------------

    @Test
    public void forJavaScript_encodesSingleQuote() {
        final String result = tool.forJavaScript("'; alert(1); var x='");
        assertFalse("Single quote must be encoded for JS string breakout prevention",
                result.contains("'"));
    }

    @Test
    public void forJavaScript_encodesBackslash() {
        final String result = tool.forJavaScript("back\\slash");
        assertTrue("Backslash must be doubled in JS output", result.contains("\\\\"));
    }

    @Test
    public void forJavaScript_returnsEmptyStringForNull() {
        assertEquals("", tool.forJavaScript(null));
    }

    // -------------------------------------------------------------------------
    // forJavaScriptAttribute / forJavaScriptBlock / forJavaScriptSource
    // -------------------------------------------------------------------------

    @Test
    public void forJavaScriptAttribute_encodesSingleQuote() {
        assertFalse(tool.forJavaScriptAttribute("'").contains("'"));
    }

    @Test
    public void forJavaScriptAttribute_returnsEmptyStringForNull() {
        assertEquals("", tool.forJavaScriptAttribute(null));
    }

    @Test
    public void forJavaScriptBlock_encodesScriptCloseTag() {
        // Inside a <script> block the breakout vector is </script>, not single quotes.
        // OWASP encodes the '<' to prevent the HTML parser closing the script early.
        final String result = tool.forJavaScriptBlock("</script>");
        assertFalse("</script> breakout must be prevented in script-block context",
                result.contains("</script>"));
    }

    @Test
    public void forJavaScriptBlock_returnsEmptyStringForNull() {
        assertEquals("", tool.forJavaScriptBlock(null));
    }

    @Test
    public void forJavaScriptSource_encodesBackslash() {
        // In a standalone .js file, backslash is the relevant encoding target.
        final String result = tool.forJavaScriptSource("back\\slash");
        assertTrue("Backslash must be doubled in JS source output", result.contains("\\\\"));
    }

    @Test
    public void forJavaScriptSource_returnsEmptyStringForNull() {
        assertEquals("", tool.forJavaScriptSource(null));
    }

    // -------------------------------------------------------------------------
    // forXml family
    // -------------------------------------------------------------------------

    @Test
    public void forXml_encodesAngleBrackets() {
        final String result = tool.forXml("<element/>");
        assertFalse("Angle bracket must be encoded", result.contains("<element/>"));
        assertTrue(result.contains("&lt;"));
    }

    @Test
    public void forXml_returnsEmptyStringForNull() {
        assertEquals("", tool.forXml(null));
    }

    @Test
    public void forXmlContent_returnsEmptyStringForNull() {
        assertEquals("", tool.forXmlContent(null));
    }

    @Test
    public void forXmlAttribute_returnsEmptyStringForNull() {
        assertEquals("", tool.forXmlAttribute(null));
    }

    @Test
    public void forXmlComment_returnsEmptyStringForNull() {
        assertEquals("", tool.forXmlComment(null));
    }

    @Test
    public void forCDATA_returnsEmptyStringForNull() {
        assertEquals("", tool.forCDATA(null));
    }

    // -------------------------------------------------------------------------
    // forJava
    // -------------------------------------------------------------------------

    @Test
    public void forJava_encodesBackslash() {
        final String result = tool.forJava("back\\slash");
        assertTrue("Backslash must be doubled in Java string output", result.contains("\\\\"));
    }

    @Test
    public void forJava_returnsEmptyStringForNull() {
        assertEquals("", tool.forJava(null));
    }

    // -------------------------------------------------------------------------
    // URL safety helpers — validateUrl
    // -------------------------------------------------------------------------

    @Test
    public void validateUrl_acceptsValidHttpsUrl() {
        assertTrue(tool.validateUrl("https://www.dotcms.com/page?q=1"));
    }

    @Test
    public void validateUrl_rejectsJavascriptScheme() {
        assertFalse(tool.validateUrl("javascript:alert(1)"));
    }

    @Test
    public void validateUrl_rejectsNull() {
        assertFalse(tool.validateUrl(null));
    }

    @Test
    public void validateUrl_rejectsMalformed() {
        assertFalse(tool.validateUrl("not a url"));
    }

    // -------------------------------------------------------------------------
    // URL safety helpers — urlHasXSS
    // -------------------------------------------------------------------------

    @Test
    public void urlHasXSS_returnsFalseForCleanUrl() {
        assertFalse(tool.urlHasXSS("https://www.dotcms.com/page?name=hello"));
    }

    @Test
    public void urlHasXSS_returnsTrueWhenParamContainsHtmlTags() {
        assertTrue(tool.urlHasXSS("https://www.dotcms.com/page?q=%3Cscript%3Ealert(1)%3C%2Fscript%3E"));
    }

    @Test
    public void urlHasXSS_returnsFalseForInvalidUrl() {
        assertFalse(tool.urlHasXSS("not a url"));
    }

    @Test
    public void urlHasXSS_returnsFalseForNull() {
        assertFalse(tool.urlHasXSS(null));
    }

    // -------------------------------------------------------------------------
    // URL safety helpers — cleanUrl
    // -------------------------------------------------------------------------

    @Test
    public void cleanUrl_returnsEncodedUrlForValidInput() {
        final String result = tool.cleanUrl("https://www.dotcms.com/page");
        assertEquals("https://www.dotcms.com/page", result);
    }

    @Test
    public void cleanUrl_returnsNullForInvalidUrl() {
        assertNull(tool.cleanUrl("javascript:alert(1)"));
    }

    @Test
    public void cleanUrl_returnsNullForNull() {
        assertNull(tool.cleanUrl(null));
    }
}
