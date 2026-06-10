/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util;

import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import javax.servlet.http.HttpServletRequest;
import org.owasp.encoder.Encode;

/**
 * <a href="Xss.java.html"><b><i>View Source</i></b></a>
 *
 * <p>XSS detection and encoding utilities. Encoding methods now delegate to the
 * <a href="https://owasp.org/www-project-java-encoder/">OWASP Java Encoder</a> library,
 * which provides context-aware, standards-compliant output encoding for HTML, JavaScript,
 * CSS, and URI contexts.</p>
 *
 * @author Brian Wing Shun Chan
 * @author Clarence Shen
 * @version $Revision: 1.3 $
 */
public class Xss {

    public static final String XSS_REGEXP_PATTERN = GetterUtil.getString( SystemProperties.get( Xss.class.getName() + ".regexp.pattern" ) );

    /**
     * Removes from the given text possible XSS hacks.
     *
     * @param text
     * @return
     * @deprecated Use context-specific encoding methods such as {@link #escapeHTMLAttrib(String)},
     *             {@link #encodeForHTML(String)}, or {@link #encodeForURL(String)} instead of
     *             stripping content.
     */
    public static String strip ( String text ) {
        if ( text == null ) {
            return null;
        }
        return RegEX.replace( text, "", XSS_REGEXP_PATTERN );
    }

    /**
     * Checks the request query string for possible XSS hacks.
     *
     * @param request the incoming HTTP request
     * @return {@code true} if any possible XSS fragment is found in the query string
     */
    public static boolean paramsHaveXSS ( final HttpServletRequest request ) {
        return paramsHaveXSS( request.getQueryString() );
    }

    /**
     * Checks the given query string for possible XSS hacks.
     *
     * @param queryString the raw query string to inspect
     * @return {@code true} if any possible XSS fragment is found
     */
    public static boolean paramsHaveXSS ( final String queryString ) {
        return RegEX.contains( UtilMethods.decodeURL( queryString ), XSS_REGEXP_PATTERN );
    }

    /**
     * Checks the given URI for possible XSS hacks.
     *
     * @param uri the URI to inspect
     * @return {@code true} if any possible XSS fragment is found
     */
    public static boolean uriHasXSS ( final String uri ) {
        if ( uri == null ) {
            return false;
        }
        return RegEX.contains( uri, XSS_REGEXP_PATTERN );
    }

    /**
     * Checks the given URL for possible XSS hacks.
     *
     * @param url the URL to inspect
     * @return {@code true} if any possible XSS fragment is found
     * @deprecated Use {@link #uriHasXSS(String)} and {@link #paramsHaveXSS(String)} individually.
     */
    @Deprecated
    public static boolean urlHasXSS ( final String url ) {
        if ( url == null ) {
            return false;
        }
        return RegEX.contains( url, XSS_REGEXP_PATTERN );
    }

    /** @deprecated Use {@link #paramsHaveXSS(HttpServletRequest)} */
    @Deprecated
    public static boolean ParamsHaveXSS ( final HttpServletRequest request ) {
        return paramsHaveXSS( request );
    }

    /** @deprecated Use {@link #paramsHaveXSS(String)} */
    @Deprecated
    public static boolean ParamsHaveXSS ( final String queryString ) {
        return paramsHaveXSS( queryString );
    }

    /** @deprecated Use {@link #uriHasXSS(String)} */
    @Deprecated
    public static boolean URIHasXSS ( final String uri ) {
        return uriHasXSS( uri );
    }

    /** @deprecated Use {@link #urlHasXSS(String)} */
    @Deprecated
    public static boolean URLHasXSS ( final String url ) {
        return urlHasXSS( url );
    }

    /**
     * Encodes a value for safe use in a URI component (query parameter value, path segment, etc.)
     * using the OWASP Java Encoder.
     *
     * @param value the raw value to encode
     * @return the percent-encoded value, or an empty string if {@code value} is null
     */
    public static String encodeForURL ( String value ) {
        return value != null ? Encode.forUriComponent( value ) : "";
    }

    /**
     * Encodes a value for safe inclusion in HTML content or an HTML attribute using the
     * OWASP Java Encoder. Replaces the previous {@code StringEscapeUtils.escapeHtml()} call
     * with a standards-compliant encoder that covers a broader set of dangerous characters.
     *
     * @param value the raw value to encode
     * @return the HTML-encoded value, or an empty string if {@code value} is null
     */
    public static String escapeHTMLAttrib ( String value ) {
        return value != null ? Encode.forHtml( value ) : "";
    }

    /**
     * Encodes a value for safe inclusion in HTML body content using the OWASP Java Encoder.
     *
     * @param value the raw value to encode
     * @return the HTML-encoded value, or an empty string if {@code value} is null
     */
    public static String encodeForHTML ( String value ) {
        return value != null ? Encode.forHtml( value ) : "";
    }

    /**
     * Encodes a value for safe inclusion in an HTML attribute (inside a quoted attribute value)
     * using the OWASP Java Encoder.
     *
     * @param value the raw value to encode
     * @return the HTML-attribute-encoded value, or an empty string if {@code value} is null
     */
    public static String encodeForHTMLAttribute ( String value ) {
        return value != null ? Encode.forHtmlAttribute( value ) : "";
    }

    /**
     * Encodes a value for safe embedding inside a JavaScript string literal using the
     * OWASP Java Encoder.
     *
     * @param value the raw value to encode
     * @return the JavaScript-encoded value, or an empty string if {@code value} is null
     */
    public static String encodeForJavaScript ( String value ) {
        return value != null ? Encode.forJavaScript( value ) : "";
    }

    /**
     * Encodes a value for safe use as a CSS string or identifier using the OWASP Java Encoder.
     *
     * @param value the raw value to encode
     * @return the CSS-encoded value, or an empty string if {@code value} is null
     */
    public static String encodeForCSS ( String value ) {
        return value != null ? Encode.forCssString( value ) : "";
    }

    /**
     * Decodes HTML entities in the given value.
     *
     * @param value the HTML-encoded value to decode
     * @return the decoded value, or an empty string if {@code value} is null
     */
    public static String unEscapeHTMLAttrib ( String value ) {
        if (value == null) {
            return "";
        }
        // OWASP encoder does not provide a decoder; Apache Commons Text is the appropriate tool.
        return org.apache.commons.lang.StringEscapeUtils.unescapeHtml( value );
    }

}
