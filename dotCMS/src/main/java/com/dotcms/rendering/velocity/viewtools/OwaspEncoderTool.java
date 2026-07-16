package com.dotcms.rendering.velocity.viewtools;

import io.vavr.Lazy;
import io.vavr.control.Try;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.velocity.tools.view.tools.ViewTool;
import org.owasp.encoder.Encode;

/**
 * Velocity view tool ({@code $owasp}) that exposes the full
 * <a href="https://owasp.org/www-project-java-encoder/">OWASP Java Encoder</a> API
 * directly to Velocity templates, covering every output context: HTML, HTML attributes,
 * JavaScript, CSS, URI components, and XML.
 *
 * <p>Registered in {@code toolbox.xml} under the key {@code owasp}.
 *
 * <p>Example Velocity usage:
 * <pre>
 *   &lt;p&gt;$owasp.forHtml($request.getParameter("name"))&lt;/p&gt;
 *   &lt;a href="/search?q=$owasp.forUriComponent($request.getParameter("q"))"&gt;Go&lt;/a&gt;
 *   &lt;script&gt;var msg = "$owasp.forJavaScript($message)";&lt;/script&gt;
 *   &lt;div style="color: $owasp.forCssString($color)"&gt;...&lt;/div&gt;
 * </pre>
 *
 * @see com.liferay.util.Xss
 */
public class OwaspEncoderTool implements ViewTool {

    private static final Lazy<UrlValidator> URL_VALIDATOR =
            Lazy.of(() -> new UrlValidator(new String[]{"http", "https"}));

    @Override
    public void init(final Object obj) {
        // no initialisation needed
    }

    // -------------------------------------------------------------------------
    // URL safety helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the given URL is syntactically valid (http/https only).
     *
     * @param url the URL to validate
     * @return {@code true} if valid
     */
    public boolean validateUrl(final String url) {
        return URL_VALIDATOR.get().isValid(url);
    }

    /**
     * Returns {@code true} if any query parameter name or value in the URL contains
     * characters that would be altered by HTML-attribute encoding — a strong signal of
     * an XSS payload.  Returns {@code false} for malformed or non-http(s) URLs.
     *
     * @param urlToTest the fully-qualified URL to inspect
     * @return {@code true} if suspicious content is found in any query parameter
     */
    public boolean urlHasXSS(final String urlToTest) {
        if (!URL_VALIDATOR.get().isValid(urlToTest)) {
            return false;
        }
        final URL url = Try.of(() -> new URL(urlToTest)).getOrNull();
        if (url == null) {
            return true;
        }
        final List<NameValuePair> params =
                URLEncodedUtils.parse(url.getQuery(), StandardCharsets.UTF_8);
        return params.stream().parallel().anyMatch(p ->
                (p.getName() != null && !p.getName().equals(forHtmlAttribute(p.getName())))
                        || (p.getValue() != null && !p.getValue().equals(forHtmlAttribute(p.getValue()))));
    }

    /**
     * Returns the URL HTML-attribute-encoded if it is valid, or {@code null} if it fails
     * validation.  Use this when outputting a URL inside an HTML attribute value.
     *
     * @param url the URL to clean
     * @return the encoded URL, or {@code null}
     */
    public String cleanUrl(final String url) {
        if (URL_VALIDATOR.get().isValid(url)) {
            return forHtmlAttribute(url);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // HTML encoding
    // -------------------------------------------------------------------------

    /**
     * Encodes for (X)HTML text content <em>and</em> quoted attribute values.
     * Prefer the more specific {@link #forHtmlContent(String)} or
     * {@link #forHtmlAttribute(String)} when the context is known.
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forHtml(final String input) {
        return input != null ? Encode.forHtml(input) : "";
    }

    /**
     * Encodes for HTML text content (inside an element, not inside an attribute).
     * Does not encode single or double quotes.
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forHtmlContent(final String input) {
        return input != null ? Encode.forHtmlContent(input) : "";
    }

    /**
     * Encodes for a quoted HTML attribute value (both single- and double-quoted).
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forHtmlAttribute(final String input) {
        return input != null ? Encode.forHtmlAttribute(input) : "";
    }

    /**
     * Encodes for an <em>unquoted</em> HTML attribute value.
     * Prefer {@link #forHtmlAttribute(String)} for quoted attributes.
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forHtmlUnquotedAttribute(final String input) {
        return input != null ? Encode.forHtmlUnquotedAttribute(input) : "";
    }

    // -------------------------------------------------------------------------
    // CSS encoding
    // -------------------------------------------------------------------------

    /**
     * Encodes for a CSS string literal (must be surrounded by quotation characters).
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forCssString(final String input) {
        return input != null ? Encode.forCssString(input) : "";
    }

    /**
     * Encodes for a CSS {@code url()} context (must be surrounded by {@code url(} / {@code )}).
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forCssUrl(final String input) {
        return input != null ? Encode.forCssUrl(input) : "";
    }

    // -------------------------------------------------------------------------
    // URI encoding
    // -------------------------------------------------------------------------

    /**
     * Percent-encodes a URI component (query parameter name/value, path segment, etc.).
     * This is the preferred method for embedding user data in URL query strings.
     *
     * @param input the raw value to encode
     * @return the percent-encoded value, or an empty string if {@code input} is null
     */
    public String forUriComponent(final String input) {
        return input != null ? Encode.forUriComponent(input) : "";
    }

    /**
     * Percent-encodes a full URI according to RFC 3986.
     * Note: a {@code javascript:} URI provided by a user would still pass through.
     * Prefer {@link #forUriComponent(String)} for individual components.
     *
     * @param input the raw URI to encode
     * @return the encoded value, or an empty string if {@code input} is null
     * @deprecated Use {@link #forUriComponent(String)} for URI components.
     */
    @Deprecated
    public String forUri(final String input) {
        return input != null ? Encode.forUri(input) : "";
    }

    // -------------------------------------------------------------------------
    // JavaScript encoding
    // -------------------------------------------------------------------------

    /**
     * Encodes for a JavaScript string literal. Safe in script blocks, HTML event attributes,
     * and JSON files. The caller must supply surrounding quotation characters.
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forJavaScript(final String input) {
        return input != null ? Encode.forJavaScript(input) : "";
    }

    /**
     * Encodes for a JavaScript inline event attribute (e.g. {@code onclick="..."}).
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forJavaScriptAttribute(final String input) {
        return input != null ? Encode.forJavaScriptAttribute(input) : "";
    }

    /**
     * Encodes for a JavaScript {@code <script>} block.
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forJavaScriptBlock(final String input) {
        return input != null ? Encode.forJavaScriptBlock(input) : "";
    }

    /**
     * Encodes for a standalone JavaScript source file.
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forJavaScriptSource(final String input) {
        return input != null ? Encode.forJavaScriptSource(input) : "";
    }

    // -------------------------------------------------------------------------
    // XML encoding
    // -------------------------------------------------------------------------

    /**
     * Encodes for XML/XHTML content and attributes (equivalent to {@link #forHtml(String)}).
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forXml(final String input) {
        return input != null ? Encode.forXml(input) : "";
    }

    /**
     * Encodes for XML/XHTML text content.
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forXmlContent(final String input) {
        return input != null ? Encode.forXmlContent(input) : "";
    }

    /**
     * Encodes for an XML/XHTML attribute value.
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forXmlAttribute(final String input) {
        return input != null ? Encode.forXmlAttribute(input) : "";
    }

    /**
     * Encodes for an XML comment. <strong>Not for use in (X)HTML comments.</strong>
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forXmlComment(final String input) {
        return input != null ? Encode.forXmlComment(input) : "";
    }

    /**
     * Encodes for an XML CDATA section. The caller must supply the {@code <![CDATA[} and
     * {@code ]]>} delimiters.
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forCDATA(final String input) {
        return input != null ? Encode.forCDATA(input) : "";
    }

    // -------------------------------------------------------------------------
    // Java / misc
    // -------------------------------------------------------------------------

    /**
     * Encodes for a Java string literal. Useful for code generators or debug output.
     *
     * @param input the raw value to encode
     * @return the encoded value, or an empty string if {@code input} is null
     */
    public String forJava(final String input) {
        return input != null ? Encode.forJava(input) : "";
    }
}
