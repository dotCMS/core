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

import com.dotcms.repackage.org.owasp.esapi.ESAPI;
import com.dotcms.repackage.org.owasp.esapi.errors.EncodingException;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

/**
 * <a href="Xss.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @author Clarence Shen
 * @version $Revision: 1.3 $
 */
public class Xss {

    public static final String XSS_REGEXP_PATTERN = GetterUtil.getString( SystemProperties.get( Xss.class.getName() + ".regexp.pattern" ) );
    public static final String XSS_REGEXP_PATTERN_URI = GetterUtil.getString( SystemProperties.get( Xss.class.getName() + ".regexp.pattern.uri" ) );
    public static final Boolean ESAPI_VALIDATION = GetterUtil.getBoolean( SystemProperties.get( Xss.class.getName() + ".ESAPI.validation" ), true );

    private static Set<String> excludeList = null;

    /**
     * Removes from the given text possible XSS hacks
     *
     * @param text
     * @return
     * @deprecated Is recommended to use instead methods like URIHasXSS or ParamsHaveXSS and handle properly a possible XSS attack
     */
    public static String strip ( String text ) {
        if ( text == null ) {
            return null;
        }
        return RegEX.replace( text, "", XSS_REGEXP_PATTERN );
    }

    /**
     * Checks into the request query string for possible XSS hacks and return true if any possible XSS fragment is found
     *
     * @param request
     * @return true if any possible XSS fragment is found
     */
    @SuppressWarnings ("unchecked")
    public static boolean ParamsHaveXSS ( HttpServletRequest request ) {
        return ParamsHaveXSS( request.getQueryString() );
    }

    /**
     * Checks into a given query string for possible XSS hacks and return true if any possible XSS fragment is found
     *
     * @param queryString
     * @return true if any possible XSS fragment is found
     */
    @SuppressWarnings ("unchecked")
    public static boolean ParamsHaveXSS ( String queryString ) {

        if ( ESAPI_VALIDATION ) {
        	if ( queryString != null && !queryString.isEmpty() ) {

                if ( excludeList == null ) {
                    buildExcludeList();
                }

                /*
                 Having in the query string something like "&or" will create problems when we are trying
                 to canonicalize the content before the security check because the HTMLEntityCodec used inside by the ESAPI.encoder().canonicalize
                 will see it as a html character, in this example the html character for the logical "OR" and will replace it with a "∨".
                 So content like:
                    param1=123&param2=456&orderBy=status
                 will be replaced by:
                    param1=123&param2=456vderBy=status
                 making the security check to fail as it is not longer a valid query string.

                 A legal html character should end with a semi-colon "&or;" but for the ESPI: "Formats all are legal both with and without semi-colon, upper/lower case..."
                 */
                for ( String toExclude : excludeList ) {
                    if ( queryString.contains( toExclude ) ) {
                        queryString = queryString.replaceAll( toExclude, "&XYZ" );
                    }
                }

                //Canonicalizes input before validation to prevent bypassing filters with encoded attacks.
                queryString = ESAPI.encoder().canonicalize( queryString, false );

        		//Validate the query string
                return !ESAPI.validator().isValidInput( "URLContext", queryString, "HTTPQueryString", queryString.length(), true );
        	}

        	 return false;

        } else {
            queryString = UtilMethods.decodeURL( queryString );
            return RegEX.contains( queryString, XSS_REGEXP_PATTERN );
        }
    }

    /**
     * Checks in the given uri for possible XSS hacks and return true if any possible XSS fragment is found
     *
     * @param uri
     * @return true if any possible XSS fragment is found
     */
    public static boolean URIHasXSS ( String uri ) {

        if ( uri == null ) {
            return false;
        } else {
            //ESAPI doesnt allow blacklist of characters, only whitelists so we dont use ESAPI here.
            return RegEX.contains( uri, XSS_REGEXP_PATTERN_URI );
        }
    }

    /**
     * Checks in the given url for possible XSS hacks and return true if any possible XSS fragment is found
     *
     * @param url
     * @return true if any possible XSS fragment is found
     * @deprecated Use instead individually URIHasXSS and ParamsHaveXSS
     */
    public static boolean URLHasXSS ( String url ) {

        if ( url == null ) {
            return false;
        }
        if ( ESAPI_VALIDATION ) {

            boolean isValid;

            //Verify if the given value is a query string or a URI
            if ( !url.startsWith( "/" ) || url.contains( "&" ) || url.contains( "=" ) ) {
                if ( url.contains( "=" ) ) {
                    isValid = ESAPI.validator().isValidInput( "URLContext", url, "HTTPQueryString", url.length(), false );
                } else {
                    isValid = ESAPI.validator().isValidInput( "URLContext", url, "HTTPParameterValue", url.length(), false );
                }
            } else {
                isValid = !RegEX.contains(url, XSS_REGEXP_PATTERN_URI);
            }

            return !isValid;
        } else {
            return RegEX.contains( url, XSS_REGEXP_PATTERN );
        }
    }

    public static String encodeForURL ( String value ) throws EncodingException {
        return value != null ? ESAPI.encoder().encodeForURL( value ) : "";
    }

    public static String escapeHTMLAttrib ( String value ) {
        return value != null ? ESAPI.encoder().encodeForHTMLAttribute( value ) : "";
    }


    public static String unEscapeHTMLAttrib ( String value ) {
        return value != null ? ESAPI.encoder().decodeForHTML( value ) : "";
    }

    private static void buildExcludeList () {
        if ( excludeList != null ) return;
        excludeList = new HashSet<String>();
        excludeList.add( "&or" );
        excludeList.add( "&Or" );
    }

}