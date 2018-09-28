/*
 * Copyright MITRE
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.dotcms.rendering.proxy.rendermode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.URI;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Formatter;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.HeaderGroup;
import org.apache.http.util.EntityUtils;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;

/**
 * An HTTP reverse proxy/gateway servlet. It is designed to be extended for customization if
 * desired. Most of the work is handled by
 * <a href="http://hc.apache.org/httpcomponents-client-ga/">Apache HttpClient</a>.
 * <p>
 * There are alternatives to a servlet based proxy such as Apache mod_proxy if that is available to
 * you. However this servlet is easily customizable by Java, secure-able by your web application's
 * security (e.g. spring-security), portable across servlet engines, and is embeddable into another
 * web application.
 * </p>
 * <p>
 * Inspiration: http://httpd.apache.org/docs/2.0/mod/mod_proxy.html
 * </p>
 *
 * @author David Smiley dsmiley@apache.org
 */

public class ProxyRequest {

    private final ByteArrayOutputStream outputOverride;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final PageMode mode;
    private final boolean doLog = true;
    private final boolean doForwardIP = Config.getBooleanProperty("PROXY_MODE_P_FORWARDEDFOR", true);
    private final boolean doPreserveHost = Config.getBooleanProperty("PROXY_MODE_P_PRESERVEHOST", false);
    private final boolean doPreserveCookies = Config.getBooleanProperty("PROXY_MODE_P_PRESERVECOOKIES", false);
    private final boolean doHandleRedirects = Config.getBooleanProperty("PROXY_MODE_P_HANDLEREDIRECTS", true);
    private final int connectTimeout = Config.getIntProperty("PROXY_MODE_P_CONNECTTIMEOUT", 30000);
    private final int readTimeout = Config.getIntProperty("PROXY_MODE_P_CONNECTTIMEOUT", 30000);
    private final boolean useSystemProperties = Config.getBooleanProperty("PROXY_MODE_P_USESYSTEMPROPERTIES", true);
    private final String targetUri = Config.getStringProperty("PROXY_MODE_TARGET_URI", "");
    private final HttpHost targetHost = new HttpHost(Config.getStringProperty("PROXY_MODE_TARGET_HOST", "localhost"),
            Config.getIntProperty("PROXY_MODE_TARGET_HOST", 3000));
    private final boolean doSendUrlFragment = Config.getBooleanProperty("PROXY_MODE_P_DOSENDURLFRAGMENTS", true);
    private final HttpClient proxyClient = createHttpClient(buildRequestConfig());

    public ProxyRequest(HttpServletRequest request, HttpServletResponse response, PageMode mode) {
        this.outputOverride = new ByteArrayOutputStream();
        this.request = request;
        this.response = response;
        this.mode=mode;

    }


    /**
     * Sub-classes can override specific behaviour of
     * {@link org.apache.http.client.config.RequestConfig}.
     */
    private RequestConfig buildRequestConfig() {
        return RequestConfig.custom().setRedirectsEnabled(doHandleRedirects).setCookieSpec(CookieSpecs.IGNORE_COOKIES) // we handle them in
                                                                                                                       // the servlet
                                                                                                                       // instead
                .setConnectTimeout(connectTimeout).setSocketTimeout(readTimeout).build();
    }


    /**
     * Called from {@link #init(javax.servlet.ServletConfig)}. HttpClient offers many opportunities for
     * customization. In any case, it should be thread-safe.
     */
    private HttpClient createHttpClient(final RequestConfig requestConfig) {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig);
        if (useSystemProperties) {
            clientBuilder = clientBuilder.useSystemProperties();
        }
        return clientBuilder.build();
    }


    
    public ByteArrayOutputStream getResponse() {
        return outputOverride;
    }
    
    
    
    
    public ProxyRequest service() throws IOException {


        // Make the Request
        // note: we won't transfer the protocol version because I'm not sure it would truly be compatible
        String method = this.request.getMethod();
        
        if(mode==PageMode.EDIT_MODE) {
            method="POST";
        }
        
        
        String proxyRequestUri = rewriteUrlFromRequest();
        HttpRequest proxyRequest;
        // spec: RFC 2616, sec 4.3: either of these two headers signal that there is a message body.
        if (this.request.getHeader(HttpHeaders.CONTENT_LENGTH) != null || this.request.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
            proxyRequest = newProxyRequestWithEntity(method, proxyRequestUri);
        } else {
            proxyRequest = new BasicHttpRequest(method, proxyRequestUri);
        }

        copyRequestHeaders(proxyRequest);

        setXForwardedForHeader(proxyRequest);

        HttpResponse proxyResponse = null;
        try {
            // Execute the request
            proxyResponse = doExecute(proxyRequest);

            // Process the response:

            // Pass the response code. This method with the "reason phrase" is deprecated but it's the
            // only way to pass the reason along too.
            int statusCode = proxyResponse.getStatusLine().getStatusCode();
            // noinspection deprecation
            this.response.setStatus(statusCode, proxyResponse.getStatusLine().getReasonPhrase());

            // Copying response headers to make sure SESSIONID or other Cookie which comes from the remote
            // server will be saved in client when the proxied url was redirected to another one.
            // See issue [#51](https://github.com/mitre/HTTP-Proxy-Servlet/issues/51)
            copyResponseHeaders(proxyResponse);

            if (statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
                // 304 needs special handling. See:
                // http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
                // Don't send body entity/content!
                this.response.setIntHeader(HttpHeaders.CONTENT_LENGTH, 0);
            } else {
                // Send the content to the client
                copyResponseEntity(proxyResponse, proxyRequest);
            }

        } catch (Exception e) {
            handleRequestException(proxyRequest, e);
        } finally {
            // make sure the entire entity was consumed, so the connection is released
            if (proxyResponse != null)
                EntityUtils.consumeQuietly(proxyResponse.getEntity());
            // Note: Don't need to close servlet outputStream:
            // http://stackoverflow.com/questions/1159168/should-one-call-close-on-httpservletresponse-getoutputstream-getwriter
        }
        return this;
    }

    private void handleRequestException(HttpRequest proxyRequest, Exception e)  {
        // abort request, according to best practice with HttpClient
        if (proxyRequest instanceof AbortableHttpRequest) {
            AbortableHttpRequest abortableHttpRequest = (AbortableHttpRequest) proxyRequest;
            abortableHttpRequest.abort();
        }

        throw new RuntimeException(e);
    }

    private HttpResponse doExecute(HttpRequest proxyRequest) throws IOException {
        if (doLog) {
            Logger.info(this.getClass(), "proxy " + this.request.getMethod() + " uri: " + this.request.getRequestURI() + " -- "
                    + proxyRequest.getRequestLine().getUri());
        }
        return proxyClient.execute(targetHost, proxyRequest);
    }

    private HttpRequest newProxyRequestWithEntity(String method, String proxyRequestUri) throws IOException {
        HttpEntityEnclosingRequest eProxyRequest = new BasicHttpEntityEnclosingRequest(method, proxyRequestUri);
        // Add the input entity (streamed)
        // note: we don't bother ensuring we close the servletInputStream since the container handles it
        eProxyRequest.setEntity(new InputStreamEntity(this.request.getInputStream(), getContentLength(this.request)));
        return eProxyRequest;
    }

    // Get the header value as a long in order to more correctly proxy very large requests
    private long getContentLength(HttpServletRequest request) {
        String contentLengthHeader = request.getHeader("Content-Length");
        if (contentLengthHeader != null) {
            return Long.parseLong(contentLengthHeader);
        }
        return -1L;
    }



    /**
     * These are the "hop-by-hop" headers that should not be copied.
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html I use an HttpClient HeaderGroup class
     * instead of Set&lt;String&gt; because this approach does case insensitive lookup faster.
     */
    private static final HeaderGroup hopByHopHeaders;
    static {
        hopByHopHeaders = new HeaderGroup();
        String[] headers = new String[] {"Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization", "TE", "Trailers",
                "Transfer-Encoding", "Upgrade"};
        for (String header : headers) {
            hopByHopHeaders.addHeader(new BasicHeader(header, null));
        }
    }

    /**
     * Copy request headers from the servlet client to the proxy request. This is easily overridden to
     * add your own.
     */
    private void copyRequestHeaders(HttpRequest proxyRequest) {
        // Get an Enumeration of all of the header names sent by the client

        Enumeration<String> enumerationOfHeaderNames = this.request.getHeaderNames();
        while (enumerationOfHeaderNames.hasMoreElements()) {
            String headerName = enumerationOfHeaderNames.nextElement();
            copyRequestHeader(proxyRequest, headerName);
        }
    }

    /**
     * Copy a request header from the servlet client to the proxy request. This is easily overridden to
     * filter out certain headers if desired.
     */
    private void copyRequestHeader(HttpRequest proxyRequest, String headerName) {
        // Instead the content-length is effectively set via InputStreamEntity
        if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH))
            return;
        if (hopByHopHeaders.containsHeader(headerName))
            return;


        Enumeration<String> headers = this.request.getHeaders(headerName);
        while (headers.hasMoreElements()) {// sometimes more than one value
            String headerValue = headers.nextElement();
            // In case the proxy host is running multiple virtual servers,
            // rewrite the Host header to ensure that we get content from
            // the correct virtual server
            if (!doPreserveHost && headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
                HttpHost host = targetHost;
                headerValue = host.getHostName();
                if (host.getPort() != -1)
                    headerValue += ":" + host.getPort();
            } else if (!doPreserveCookies && headerName.equalsIgnoreCase(org.apache.http.cookie.SM.COOKIE)) {
                headerValue = getRealCookie(headerValue);
            }
            proxyRequest.addHeader(headerName, headerValue);
        }
    }

    private void setXForwardedForHeader(HttpRequest proxyRequest) {
        if (doForwardIP) {
            String forHeaderName = "X-Forwarded-For";
            String forHeader = this.request.getRemoteAddr();
            String existingForHeader = this.request.getHeader(forHeaderName);
            if (existingForHeader != null) {
                forHeader = existingForHeader + ", " + forHeader;
            }
            proxyRequest.setHeader(forHeaderName, forHeader);

            String protoHeaderName = "X-Forwarded-Proto";
            String protoHeader = this.request.getScheme();
            proxyRequest.setHeader(protoHeaderName, protoHeader);
        }
    }

    /** Copy proxied response headers back to the servlet client. */
    private void copyResponseHeaders(HttpResponse proxyResponse) {
        for (Header header : proxyResponse.getAllHeaders()) {
            copyResponseHeader(header);
        }
    }

    /**
     * Copy a proxied response header back to the servlet client. This is easily overwritten to filter
     * out certain headers if desired.
     */
    private void copyResponseHeader(Header header) {
        String headerName = header.getName();
        if (hopByHopHeaders.containsHeader(headerName))
            return;
        String headerValue = header.getValue();
        if (headerName.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE)
                || headerName.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE2)) {
            copyProxyCookie(headerValue);
        } else if (headerName.equalsIgnoreCase(HttpHeaders.LOCATION)) {
            // LOCATION Header may have to be rewritten.
            this.response.addHeader(headerName, rewriteUrlFromResponse(headerValue));
        } else {
            this.response.addHeader(headerName, headerValue);
        }
    }

    /**
     * Copy cookie from the proxy to the servlet client. Replaces cookie path to local path and renames
     * cookie to avoid collisions.
     */
    private void copyProxyCookie(String headerValue) {
        // build path for resulting cookie
        String path = this.request.getContextPath(); // path starts with / or is empty string
        path += this.request.getServletPath(); // servlet path starts with / or is empty string
        if (path.isEmpty()) {
            path = "/";
        }

        for (HttpCookie cookie : HttpCookie.parse(headerValue)) {
            // set cookie name prefixed w/ a proxy value so it won't collide w/ other cookies
            String proxyCookieName = doPreserveCookies ? cookie.getName() : getCookieNamePrefix(cookie.getName()) + cookie.getName();
            Cookie servletCookie = new Cookie(proxyCookieName, cookie.getValue());
            servletCookie.setComment(cookie.getComment());
            servletCookie.setMaxAge((int) cookie.getMaxAge());
            servletCookie.setPath(path); // set to the path of the proxy servlet
            // don't set cookie domain
            servletCookie.setSecure(cookie.getSecure());
            servletCookie.setVersion(cookie.getVersion());
            this.response.addCookie(servletCookie);
        }
    }

    /**
     * Take any client cookies that were originally from the proxy and prepare them to send to the
     * proxy. This relies on cookie headers being set correctly according to RFC 6265 Sec 5.4. This also
     * blocks any local cookies from being sent to the proxy.
     */
    private String getRealCookie(String cookieValue) {
        StringBuilder escapedCookie = new StringBuilder();
        String cookies[] = cookieValue.split("[;,]");
        for (String cookie : cookies) {
            String cookieSplit[] = cookie.split("=");
            if (cookieSplit.length == 2) {
                String cookieName = cookieSplit[0].trim();
                if (cookieName.startsWith(getCookieNamePrefix(cookieName))) {
                    cookieName = cookieName.substring(getCookieNamePrefix(cookieName).length());
                    if (escapedCookie.length() > 0) {
                        escapedCookie.append("; ");
                    }
                    escapedCookie.append(cookieName).append("=").append(cookieSplit[1].trim());
                }
            }
        }
        return escapedCookie.toString();
    }

    /** The string prefixing rewritten cookies. */
    private String getCookieNamePrefix(String name) {
        return "!Proxy!" + this.getClass().getSimpleName();
    }

    /** Copy response body data (the entity) from the proxy to the servlet client. */
    private void copyResponseEntity(HttpResponse proxyResponse, HttpRequest proxyRequest) throws IOException {
        HttpEntity entity = proxyResponse.getEntity();
        if (entity != null) {

            entity.writeTo(this.outputOverride);
        }
    }

    /**
     * Reads the request URI from {@code servletRequest} and rewrites it, considering targetUri. It's
     * used to make the new request.
     */
    private String rewriteUrlFromRequest() {
        StringBuilder uri = new StringBuilder(500);
        uri.append(targetHost);
        // Handle the path given to the servlet
        String pathInfo =  ( request.getAttribute("javax.servlet.forward.request_uri")==null) ?  request.getRequestURI():  (String) request.getAttribute("javax.servlet.forward.request_uri");
        if (pathInfo != null) {// ex: /my/path.html

            pathInfo = pathInfo.replace("/api/v1/page/render", "");
            pathInfo = pathInfo.replace("/index", "/");

            // getPathInfo() returns decoded string, so we need encodeUriQuery to encode "%" characters
            uri.append(encodeUriQuery(pathInfo, true));
        }
        // Handle the query string & fragment
        String queryString = this.request.getQueryString();// ex:(following '?'): name=value&foo=bar#fragment
        String fragment = null;
        // split off fragment from queryString, updating queryString if found
        if (queryString != null) {
            int fragIdx = queryString.indexOf('#');
            if (fragIdx >= 0) {
                fragment = queryString.substring(fragIdx + 1);
                queryString = queryString.substring(0, fragIdx);
            }
        }

        queryString = rewriteQueryStringFromRequest(queryString);
        if (queryString != null && queryString.length() > 0) {
            uri.append('?');
            // queryString is not decoded, so we need encodeUriQuery not to encode "%" characters, to avoid
            // double-encoding
            uri.append(encodeUriQuery(queryString, false));
        }

        if (doSendUrlFragment && fragment != null) {
            uri.append('#');
            // fragment is not decoded, so we need encodeUriQuery not to encode "%" characters, to avoid
            // double-encoding
            uri.append(encodeUriQuery(fragment, false));
        }
        return uri.toString();
    }

    protected String rewriteQueryStringFromRequest(String queryString) {
        return queryString;
    }

    /**
     * For a redirect response from the target server, this translates {@code theUrl} to redirect to and
     * translates it to one the original client can use.
     */
    protected String rewriteUrlFromResponse(String theUrl) {
        // TODO document example paths
        final String targetUri = this.targetUri;
        if (theUrl.startsWith(targetUri)) {
            /*-
             * The URL points back to the back-end server.
             * Instead of returning it verbatim we replace the target path with our
             * source path in a way that should instruct the original client to
             * request the URL pointed through this Proxy.
             * We do this by taking the current request and rewriting the path part
             * using this servlet's absolute path and the path from the returned URL
             * after the base target URL.
             */
            StringBuffer curUrl = this.request.getRequestURL();// no query
            int pos;
            // Skip the protocol part
            if ((pos = curUrl.indexOf("://")) >= 0) {
                // Skip the authority part
                // + 3 to skip the separator between protocol and authority
                if ((pos = curUrl.indexOf("/", pos + 3)) >= 0) {
                    // Trim everything after the authority part.
                    curUrl.setLength(pos);
                }
            }
            // Context path starts with a / if it is not blank
            curUrl.append(this.request.getContextPath());
            // Servlet path starts with a / if it is not blank
            curUrl.append(this.request.getServletPath());
            curUrl.append(theUrl, targetUri.length(), theUrl.length());
            return curUrl.toString();
        }
        return theUrl;
    }

    /** The target URI as configured. Not null. */
    public String getTargetUri() {
        return targetUri;
    }

    /**
     * Encodes characters in the query or fragment part of the URI.
     *
     * <p>
     * Unfortunately, an incoming URI sometimes has characters disallowed by the spec. HttpClient
     * insists that the outgoing proxied request has a valid URI because it uses Java's {@link URI}. To
     * be more forgiving, we must escape the problematic characters. See the URI class for the spec.
     *
     * @param in example: name=value&amp;foo=bar#fragment
     * @param encodePercent determine whether percent characters need to be encoded
     */
    protected static CharSequence encodeUriQuery(CharSequence in, boolean encodePercent) {
        // Note that I can't simply use URI.java to encode because it will escape pre-existing escaped
        // things.
        StringBuilder outBuf = null;
        Formatter formatter = null;
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            boolean escape = true;
            if (c < 128) {
                if (asciiQueryChars.get((int) c) && !(encodePercent && c == '%')) {
                    escape = false;
                }
            } else if (!Character.isISOControl(c) && !Character.isSpaceChar(c)) {// not-ascii
                escape = false;
            }
            if (!escape) {
                if (outBuf != null)
                    outBuf.append(c);
            } else {
                // escape
                if (outBuf == null) {
                    outBuf = new StringBuilder(in.length() + 5 * 3);
                    outBuf.append(in, 0, i);
                    formatter = new Formatter(outBuf);
                }
                // leading %, 0 padded, width 2, capital hex
                formatter.format("%%%02X", (int) c);// TODO
            }
        }
        return outBuf != null ? outBuf : in;
    }

    protected static final BitSet asciiQueryChars;
    static {
        char[] c_unreserved = "_-!.~'()*".toCharArray();// plus alphanum
        char[] c_punct = ",;:$&+=".toCharArray();
        char[] c_reserved = "?/[]@".toCharArray();// plus punct

        asciiQueryChars = new BitSet(128);
        for (char c = 'a'; c <= 'z'; c++)
            asciiQueryChars.set((int) c);
        for (char c = 'A'; c <= 'Z'; c++)
            asciiQueryChars.set((int) c);
        for (char c = '0'; c <= '9'; c++)
            asciiQueryChars.set((int) c);
        for (char c : c_unreserved)
            asciiQueryChars.set((int) c);
        for (char c : c_punct)
            asciiQueryChars.set((int) c);
        for (char c : c_reserved)
            asciiQueryChars.set((int) c);

        asciiQueryChars.set((int) '%');// leave existing percent escapes in place
    }

}
