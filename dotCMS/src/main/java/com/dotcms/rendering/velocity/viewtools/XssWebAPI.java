package com.dotcms.rendering.velocity.viewtools;

import com.liferay.util.Xss;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * Velocity view tool ({@code $xsstool}) that exposes context-aware output encoding via the
 * <a href="https://owasp.org/www-project-java-encoder/">OWASP Java Encoder</a> library.
 *
 * <p>Use the appropriate method for the output context to prevent XSS:
 * <ul>
 *   <li>{@link #encodeForHTML(String)} — inside HTML element content</li>
 *   <li>{@link #encodeForHTMLAttribute(String)} — inside a quoted HTML attribute value</li>
 *   <li>{@link #encodeForJavaScript(String)} — inside a JavaScript string literal</li>
 *   <li>{@link #encodeForURL(String)} — inside a URI component (query param, path segment)</li>
 *   <li>{@link #encodeForCSS(String)} — inside a CSS string or identifier</li>
 * </ul>
 *
 * <p>Example usage in a Velocity template:
 * <pre>
 *   &lt;p&gt;$xsstool.encodeForHTML($request.getParameter("name"))&lt;/p&gt;
 *   &lt;a href="/search?q=$xsstool.encodeForURL($request.getParameter("q"))"&gt;Search&lt;/a&gt;
 *   &lt;script&gt;var msg = "$xsstool.encodeForJavaScript($message)";&lt;/script&gt;
 * </pre>
 *
 * <p>Registered in {@code toolbox.xml} under the key {@code xsstool}.
 *
 * @see Xss
 */
public class XssWebAPI implements ViewTool {

    @Override
    public void init(Object obj) {
    }

    /**
     * Encodes the given value for safe inclusion in HTML body content.
     * Replaces characters such as {@code <}, {@code >}, {@code &}, {@code "}, and {@code '}
     * with their HTML entity equivalents.
     *
     * @param value the raw value to encode
     * @return the HTML-encoded value, or an empty string if {@code value} is null
     */
    public String encodeForHTML(final String value) {
        return Xss.encodeForHTML(value);
    }

    /**
     * Encodes the given value for safe inclusion inside a quoted HTML attribute value.
     *
     * @param value the raw value to encode
     * @return the HTML-attribute-encoded value, or an empty string if {@code value} is null
     */
    public String encodeForHTMLAttribute(final String value) {
        return Xss.encodeForHTMLAttribute(value);
    }

    /**
     * Encodes the given value for safe embedding inside a JavaScript string literal.
     * Use this when rendering user data inside {@code <script>} blocks or inline event handlers.
     *
     * @param value the raw value to encode
     * @return the JavaScript-encoded value, or an empty string if {@code value} is null
     */
    public String encodeForJavaScript(final String value) {
        return Xss.encodeForJavaScript(value);
    }

    /**
     * Encodes the given value for safe use as a URI component (query parameter value, path
     * segment, etc.). Percent-encodes all characters that are not unreserved URI characters.
     *
     * @param value the raw value to encode
     * @return the URI-component-encoded value, or an empty string if {@code value} is null
     */
    public String encodeForURL(final String value) {
        return Xss.encodeForURL(value);
    }

    /**
     * Encodes the given value for safe use as a CSS string or identifier.
     *
     * @param value the raw value to encode
     * @return the CSS-encoded value, or an empty string if {@code value} is null
     */
    public String encodeForCSS(final String value) {
        return Xss.encodeForCSS(value);
    }

    // -------------------------------------------------------------------------
    // Legacy / compatibility methods
    // -------------------------------------------------------------------------

    /**
     * Strips XSS patterns from the given string using a regex.
     *
     * @param value the raw value to strip
     * @return the stripped value
     * @deprecated Use context-specific encoding methods instead of stripping content.
     */
    @Deprecated
    public String strip(final String value) {
        return Xss.strip(value);
    }

    /**
     * HTML-encodes the given value. Delegates to {@link #encodeForHTML(String)}.
     *
     * @param value the raw value to encode
     * @return the HTML-encoded value
     * @deprecated Use {@link #encodeForHTML(String)} for explicit context.
     */
    @Deprecated
    public String escapeHTMLAttrib(final String value) {
        return Xss.escapeHTMLAttrib(value);
    }

    /**
     * Convenience alias for {@link #escapeHTMLAttrib(String)}.
     *
     * @param value the raw value to encode
     * @return the HTML-encoded value
     * @deprecated Use {@link #encodeForHTML(String)} for explicit context.
     */
    @Deprecated
    public String escape(final String value) {
        return escapeHTMLAttrib(value);
    }

    /**
     * Decodes HTML entities in the given value.
     *
     * @param value the HTML-encoded string to decode
     * @return the decoded value
     */
    public String unEscape(final String value) {
        return Xss.unEscapeHTMLAttrib(value);
    }

    /**
     * Returns {@code true} if the given value contains potential XSS patterns.
     *
     * @param value the string to check
     * @return {@code true} if XSS patterns are detected
     */
    public boolean hasXss(final String value) {
        return Xss.urlHasXSS(value);
    }

}
