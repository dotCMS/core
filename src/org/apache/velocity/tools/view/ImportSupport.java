/*
 * Copyright 2003 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.velocity.tools.view;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletException;

import com.dotmarketing.util.Logger;

import java.io.InputStream;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;

/**
 * <p>Provides methods to import arbitrary local or remote resources as strings.</p>
 * <p>Based on ImportSupport from the JSTL taglib by Shawn Bayern</p>
 *
 * @author <a href="mailto:marinoj@centrum.is">Marino A. Jonsson</a>
 * @since VelocityTools 1.1
 * @version $Revision: 158702 $ $Date: 2005-03-22 17:46:53 -0800 (Tue, 22 Mar 2005) $
 */
public abstract class ImportSupport {

    protected ServletContext application;
    protected HttpServletRequest request;
    protected HttpServletResponse response;

    protected boolean isAbsoluteUrl; // is our URL absolute?

    protected static final String VALID_SCHEME_CHARS =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+.-";

    /** Default character encoding for response. */
    protected static final String DEFAULT_ENCODING = "ISO-8859-1";

    //*********************************************************************
    // URL importation logic

    /*
     * Overall strategy:  we have two entry points, acquireString() and
     * acquireReader().  The latter passes data through unbuffered if
     * possible (but note that it is not always possible -- specifically
     * for cases where we must use the RequestDispatcher.  The remaining
     * methods handle the common.core logic of loading either a URL or a local
     * resource.
     *
     * We consider the 'natural' form of absolute URLs to be Readers and
     * relative URLs to be Strings.  Thus, to avoid doing extra work,
     * acquireString() and acquireReader() delegate to one another as
     * appropriate.  (Perhaps I could have spelled things out more clearly,
     * but I thought this implementation was instructive, not to mention
     * somewhat cute...)
     */

    /**
     *
     * @param url the URL resource to return as string
     * @return the URL resource as string
     * @throws IOException
     * @throws java.lang.Exception
     */
    protected String acquireString(String url) throws IOException, Exception {
        // Record whether our URL is absolute or relative
        this.isAbsoluteUrl = isAbsoluteUrl(url);
        if (this.isAbsoluteUrl)
        {
            // for absolute URLs, delegate to our peer
            BufferedReader r = new BufferedReader(acquireReader(url));
            StringBuffer sb = new StringBuffer();
            int i;
            // under JIT, testing seems to show this simple loop is as fast
            // as any of the alternatives
            while ((i = r.read()) != -1)
            {
                sb.append((char)i);
            }
            r.close();

            return sb.toString();
        }
        else // handle relative URLs ourselves
        {
            // URL is relative, so we must be an HTTP request
            if (!(request instanceof HttpServletRequest
                  && response instanceof HttpServletResponse))
            {
                throw new Exception("Relative import from non-HTTP request not allowed");
            }

            // retrieve an appropriate ServletContext
            // normalize the URL if we have an HttpServletRequest
            if (!url.startsWith("/"))
            {
                String sp = ((HttpServletRequest)request).getServletPath();
                url = sp.substring(0, sp.lastIndexOf('/')) + '/' + url;
            }

            // strip the session id from the url
            url = stripSession(url);

            // from this context, get a dispatcher
            RequestDispatcher rd = application.getRequestDispatcher(url);
            if (rd == null)
            {
                throw new Exception("Couldn't get a RequestDispatcher for \""
                                    + url + "\"");
            }

            // include the resource, using our custom wrapper
            ImportResponseWrapper irw =
                new ImportResponseWrapper((HttpServletResponse)response);
            try
            {
                rd.include(request, irw);
            }
            catch (IOException ex)
            {
                throw new Exception("Problem importing the relative URL \""
                                    + url + "\". " + ex);
            }
            catch (RuntimeException ex)
            {
                throw new Exception("Problem importing the relative URL \""
                                    + url + "\". " + ex);
            }

            // disallow inappropriate response codes per JSTL spec
            if (irw.getStatus() < 200 || irw.getStatus() > 299)
            {
                throw new Exception("Invalid response code '" + irw.getStatus()
                                    + "' for \"" + url + "\"");
            }

            // recover the response String from our wrapper
            return irw.getString();
        }
    }

    /**
     *
     * @param url the URL to read
     * @return a Reader for the InputStream created from the supplied URL
     * @throws IOException
     * @throws java.lang.Exception
     */
    protected Reader acquireReader(String url) throws IOException, Exception
    {
        if (!this.isAbsoluteUrl)
        {
            // for relative URLs, delegate to our peer
            return new StringReader(acquireString(url));
        }
        else
        {
            // absolute URL
            try
            {
                // handle absolute URLs ourselves, using java.net.URL
                URL u = new URL(url);
                //URL u = new URL("http", "proxy.hi.is", 8080, target);
                URLConnection uc = u.openConnection();
                uc.setReadTimeout(15000);
                InputStream i = uc.getInputStream();

                // okay, we've got a stream; encode it appropriately
                Reader r = null;
                String charSet;

                // charSet extracted according to RFC 2045, section 5.1
                String contentType = uc.getContentType();
                if (contentType != null)
                {
                    charSet = this.getContentTypeAttribute(contentType, "charset");
                    if (charSet == null)
                    {
                        charSet = DEFAULT_ENCODING;
                    }
                }
                else
                {
                    charSet = DEFAULT_ENCODING;
                }

                try
                {
                    r = new InputStreamReader(i, charSet);
                }
                catch (Exception ex)
                {
                    r = new InputStreamReader(i, DEFAULT_ENCODING);
                }

                // check response code for HTTP URLs before returning, per spec,
                // before returning
                if (uc instanceof HttpURLConnection)
                {
                    int status = ((HttpURLConnection)uc).getResponseCode();
                    if (status < 200 || status > 299)
                    {
                        throw new Exception(status + " " + url);
                    }
                }
                return r;
            }
            catch (IOException ex)
            {
            	Logger.error(this, "Problem accessing the absolute URL "+ url + " : " + ex.getMessage(), ex);
                throw new Exception("Problem accessing the absolute URL \""
                                    + url + "\". " + ex,ex);
            }
            catch (RuntimeException ex)
            {
            	Logger.error(this, "Problem accessing the absolute URL "+ url + " : " + ex.getMessage(), ex);
                // because the spec makes us
                throw new Exception("Problem accessing the absolute URL \""
                                    + url + "\". " + ex,ex);
            }
        }
    }

    /** Wraps responses to allow us to retrieve results as Strings. */
    protected class ImportResponseWrapper extends HttpServletResponseWrapper
    {
        /*
         * We provide either a Writer or an OutputStream as requested.
         * We actually have a true Writer and an OutputStream backing
         * both, since we don't want to use a character encoding both
         * ways (Writer -> OutputStream -> Writer).  So we use no
         * encoding at all (as none is relevant) when the target resource
         * uses a Writer.  And we decode the OutputStream's bytes
         * using OUR tag's 'charEncoding' attribute, or ISO-8859-1
         * as the default.  We thus ignore setLocale() and setContentType()
         * in this wrapper.
         *
         * In other words, the target's asserted encoding is used
         * to convert from a Writer to an OutputStream, which is typically
         * the medium through with the target will communicate its
         * ultimate response.  Since we short-circuit that mechanism
         * and read the target's characters directly if they're offered
         * as such, we simply ignore the target's encoding assertion.
         */

        /** The Writer we convey. */
        private StringWriter sw;

        /** A buffer, alternatively, to accumulate bytes. */
        private ByteArrayOutputStream bos;

        /** 'True' if getWriter() was called; false otherwise. */
        private boolean isWriterUsed;

        /** 'True if getOutputStream() was called; false otherwise. */
        private boolean isStreamUsed;

        /** The HTTP status set by the target. */
        private int status = 200;

        //************************************************************
        // Constructor and methods

        /**
         * Constructs a new ImportResponseWrapper.
         * @param response the response to wrap
         */
        public ImportResponseWrapper(HttpServletResponse response)
        {
            super(response);
        }

        /**
         * @return a Writer designed to buffer the output.
         */
        public PrintWriter getWriter()
        {
            if (isStreamUsed)
            {
                throw new IllegalStateException("Unexpected internal error during import: "
                                                + "Target servlet called getWriter(), then getOutputStream()");
            }
            isWriterUsed = true;
            if (sw == null)
            {
                sw = new StringWriter();
            }
            return new PrintWriter(sw);
        }

        /**
         * @return a ServletOutputStream designed to buffer the output.
         */
        public ServletOutputStream getOutputStream()
        {
            if (isWriterUsed)
            {
                throw new IllegalStateException("Unexpected internal error during import: "
                                                + "Target servlet called getOutputStream(), then getWriter()");
            }
            isStreamUsed = true;
            if (bos == null)
            {
                bos = new ByteArrayOutputStream();
            }
            ServletOutputStream sos = new ServletOutputStream()
                {
                    public void write(int b) throws IOException
                    {
                        bos.write(b);
                    }
                };
            return sos;
        }

        /** Has no effect. */
        public void setContentType(String x)
        {
            // ignore
        }

        /** Has no effect. */
        public void setLocale(Locale x)
        {
            // ignore
        }

        /**
         * Sets the status of the response
         * @param status the status code
         */
        public void setStatus(int status)
        {
            this.status = status;
        }

        /**
         * @return the status of the response
         */
        public int getStatus()
        {
            return status;
        }

        /**
         * Retrieves the buffered output, using the containing tag's
         * 'charEncoding' attribute, or the tag's default encoding,
         * <b>if necessary</b>.
         * @return the buffered output
         * @throws UnsupportedEncodingException if the encoding is not supported
         */
        public String getString() throws UnsupportedEncodingException
        {
            if (isWriterUsed)
            {
                return sw.toString();
            }
            else if (isStreamUsed)
            {
                return bos.toString(this.getCharacterEncoding());
            }
            else
            {
                return ""; // target didn't write anything
            }
        }
    }

    //*********************************************************************
    // Public utility methods

    /**
     * Returns <tt>true</tt> if our current URL is absolute,
     * <tt>false</tt> otherwise.
     *
     * @param url the url to check out
     * @return true if the url is absolute
     */
    public static boolean isAbsoluteUrl(String url) {
        // a null URL is not absolute, by our definition
        if (url == null)
        {
            return false;
        }

        // do a fast, simple check first
        int colonPos;
        if ((colonPos = url.indexOf(":")) == -1)
        {
            return false;
        }

        // if we DO have a colon, make sure that every character
        // leading up to it is a valid scheme character
        for (int i = 0; i < colonPos; i++)
        {
            if (VALID_SCHEME_CHARS.indexOf(url.charAt(i)) == -1)
            {
                return false;
            }
        }
        // if so, we've got an absolute url
        return true;
    }

    /**
     * Strips a servlet session ID from <tt>url</tt>.  The session ID
     * is encoded as a URL "path parameter" beginning with "jsessionid=".
     * We thus remove anything we find between ";jsessionid=" (inclusive)
     * and either EOS or a subsequent ';' (exclusive).
     *
     * @param url the url to strip the session id from
     * @return the stripped url
     */
    public static String stripSession(String url)
    {
        StringBuffer u = new StringBuffer(url);
        int sessionStart;
        while ((sessionStart = u.toString().indexOf(";jsessionid=")) != -1)
        {
            int sessionEnd = u.toString().indexOf(";", sessionStart + 1);
            if (sessionEnd == -1)
            {
                sessionEnd = u.toString().indexOf("?", sessionStart + 1);
            }
            if (sessionEnd == -1)
            {
                // still
                sessionEnd = u.length();
            }
            u.delete(sessionStart, sessionEnd);
        }
        return u.toString();
    }

    /**
     * Get the value associated with a content-type attribute.
     * Syntax defined in RFC 2045, section 5.1.
     *
     * @param input the string containing the attributes
     * @param name the name of the content-type attribute
     * @return the value associated with a content-type attribute
     */
    public static String getContentTypeAttribute(String input, String name)
    {
        int begin;
        int end;
        int index = input.toUpperCase().indexOf(name.toUpperCase());
        if (index == -1)
        {
            return null;
        }
        index = index + name.length(); // positioned after the attribute name
        index = input.indexOf('=', index); // positioned at the '='
        if (index == -1)
        {
            return null;
        }
        index += 1; // positioned after the '='
        input = input.substring(index).trim();

        if (input.charAt(0) == '"')
        {
            // attribute value is a quoted string
            begin = 1;
            end = input.indexOf('"', begin);
            if (end == -1)
            {
                return null;
            }
        }
        else
        {
            begin = 0;
            end = input.indexOf(';');
            if (end == -1)
            {
                end = input.indexOf(' ');
            }
            if (end == -1)
            {
                end = input.length();
            }
        }
        return input.substring(begin, end).trim();
    }

}
