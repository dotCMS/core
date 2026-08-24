package com.dotcms.filters;

import com.dotmarketing.business.portal.ThreadLocalSaxParserFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Validates the XML bodies sent to the WebDAV endpoints, rejecting any document that carries a
 * document type declaration.
 *
 * <p>WebDAV clients do not send a DTD: the request bodies defined by RFC 4918 are plain namespaced
 * XML. Accepting one only widens what the parser has to do, so this refuses them up front and
 * keeps the parse predictable.
 *
 * <p>The check lives here rather than in the parser because the WebDAV servlet comes from a
 * third-party library whose parsers cannot be configured from dotCMS code. One of the three parses
 * happens in a static method, so supplying the library a custom parser would not cover it either.
 * A filter covers all three.
 *
 * <p>Detection is a real parse rather than a search of the raw bytes, because a byte search is
 * defeated by the document's encoding: in UTF-16 the literal "DOCTYPE" does not appear as those
 * seven bytes. The parse only looks for the declaration, via the lexical handler, and deliberately
 * says nothing about whether the rest of the document is well formed. The servlet tolerates
 * malformed bodies -- a PROPFIND that fails to parse falls back to allprop and still answers 207 --
 * so failing those here would change behaviour that has nothing to do with DTDs.
 */
public class WebDavXmlValidationFilter implements Filter {

    /** The only WebDAV methods whose bodies the servlet parses as XML. */
    private static final Set<String> XML_BODY_METHODS = Set.of("PROPFIND", "PROPPATCH", "LOCK");

    /**
     * RFC 4918 property and lock bodies are tiny. Validating one means holding it in memory, so
     * this bounds how much a caller can make us buffer.
     */
    static final int MAX_BODY_BYTES =
            Config.getIntProperty("WEBDAV_MAX_XML_BODY_BYTES", 512 * 1024);

    /**
     * Aborts the detection parse as soon as the declaration is seen. Recognised by type and
     * through the cause chain, never by instance: a parser that wraps a handler exception would
     * defeat an identity check, and the failure mode of that would be a DTD forwarded silently.
     */
    private static final class DoctypeSeenException extends SAXException {
        private DoctypeSeenException() {
            super("DOCTYPE declared");
        }
    }

    @Override
    public void init(final FilterConfig filterConfig) {
        // Nothing to configure.
    }

    @Override
    public void destroy() {
        // Nothing to release.
    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
            throws IOException, ServletException {

        if (!(req instanceof HttpServletRequest) || !(res instanceof HttpServletResponse)) {
            chain.doFilter(req, res);
            return;
        }
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        if (!XML_BODY_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT))) {
            chain.doFilter(req, res);
            return;
        }

        final Optional<byte[]> body = readCappedBody(request);
        if (body.isEmpty()) {
            Logger.warn(this, String.format("Rejecting oversized WebDAV %s body from %s",
                    request.getMethod(), request.getRemoteAddr()));
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }

        // An absent body is legal: PROPFIND with no body means allprop, and LOCK with no body is
        // a lock refresh. Only inspect when there is something to inspect.
        if (body.get().length > 0 && declaresDoctype(body.get())) {
            Logger.warn(this, String.format("Rejecting WebDAV %s body declaring a DOCTYPE from %s",
                    request.getMethod(), request.getRemoteAddr()));
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        chain.doFilter(new BufferedBodyRequest(request, body.get()), res);
    }

    /**
     * Reads the body, giving up if it exceeds {@link #MAX_BODY_BYTES}. An empty Optional means the
     * body was over the cap, not that it was absent.
     */
    private static Optional<byte[]> readCappedBody(final HttpServletRequest request) throws IOException {
        try (InputStream in = request.getInputStream()) {
            // Reading one byte past the cap is enough to know it was exceeded.
            final byte[] body = in.readNBytes(MAX_BODY_BYTES + 1);
            return body.length > MAX_BODY_BYTES ? Optional.empty() : Optional.of(body);
        }
    }

    /**
     * True when the document declares a DOCTYPE. A body that cannot be parsed at all is reported
     * as not declaring one, because the servlet already tolerates malformed XML and this filter is
     * not the place to change that. If a detecting parser cannot be built the answer is true: that
     * is a systemic misconfiguration rather than something about this request, and passing bodies
     * through unchecked would defeat the filter.
     */
    static boolean declaresDoctype(final byte[] body) {
        final XMLReader reader;
        try {
            // This factory disables external entities and external DTD loading but still reports
            // the declaration, which is exactly what is needed to detect one safely.
            reader = ThreadLocalSaxParserFactory.getSaxParser().getXMLReader();
            reader.setProperty("http://xml.org/sax/properties/lexical-handler", new DefaultHandler2() {
                @Override
                public void startDTD(final String name, final String publicId, final String systemId)
                        throws SAXException {
                    throw new DoctypeSeenException();
                }
            });
            reader.setErrorHandler(new DefaultHandler());
        } catch (final Exception unconfigurable) {
            Logger.error(WebDavXmlValidationFilter.class,
                    "Could not build a DOCTYPE-detecting XML parser; refusing the WebDAV body",
                    unconfigurable);
            return true;
        }

        try {
            reader.parse(new InputSource(new ByteArrayInputStream(body)));
            return false;
        } catch (final Exception e) {
            // Search the whole chain, not just the throwable itself: a parser is free to wrap what
            // a handler throws. indexOfType tolerates a cyclic chain.
            if (ExceptionUtils.indexOfType(e, DoctypeSeenException.class) != -1) {
                return true;
            }
            Logger.debug(WebDavXmlValidationFilter.class,
                    () -> "WebDAV body did not parse, forwarding it unchanged: " + e.getMessage());
            return false;
        }
    }

    /** Replays the already-consumed body so the WebDAV servlet can read it again. */
    private static final class BufferedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        /**
         * Built once and handed back on every call. Returning a fresh stream each time would let a
         * second caller silently re-read the body from the start, which is not how a request body
         * behaves.
         */
        private ServletInputStream stream;

        private BufferedBodyRequest(final HttpServletRequest request, final byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            if (stream != null) {
                return stream;
            }
            final ByteArrayInputStream source = new ByteArrayInputStream(body);
            stream = new ServletInputStream() {
                @Override
                public int read() {
                    return source.read();
                }

                @Override
                public int read(final byte[] target, final int off, final int len) {
                    return source.read(target, off, len);
                }

                @Override
                public boolean isFinished() {
                    return source.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(final ReadListener readListener) {
                    // Unreachable in practice: the WebDAV servlet reads the body with blocking
                    // I/O and never registers a listener (milton-api and milton-servlet 1.8.1.4
                    // contain no reference to ReadListener, AsyncContext or startAsync). The body
                    // is fully buffered here anyway, so there would be nothing to notify about.
                    throw new UnsupportedOperationException();
                }
            };
            return stream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            final String encoding = getCharacterEncoding();
            // The servlet spec defaults to ISO-8859-1 when the request states no encoding, and
            // requires a checked UnsupportedEncodingException for one it cannot honour.
            Charset charset = StandardCharsets.ISO_8859_1;
            if (encoding != null) {
                try {
                    charset = Charset.forName(encoding);
                } catch (final IllegalArgumentException badCharset) {
                    throw new UnsupportedEncodingException(encoding);
                }
            }
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
