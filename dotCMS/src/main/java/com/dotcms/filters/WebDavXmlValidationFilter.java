package com.dotcms.filters;

import com.dotmarketing.util.Logger;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
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
 * <p>Detection is a parse with {@code disallow-doctype-decl} rather than a search of the raw bytes,
 * because a byte search is defeated by the document's encoding. It uses the same XML implementation
 * the servlet will use, on the same bytes, so the two cannot reach different conclusions.
 *
 * <p>Anything that fails to validate is refused, malformed XML included, which turns what would
 * have been a 500 into a more accurate 400.
 */
public class WebDavXmlValidationFilter implements Filter {

    /** The only WebDAV methods whose bodies the servlet parses as XML. */
    private static final Set<String> XML_BODY_METHODS = Set.of("PROPFIND", "PROPPATCH", "LOCK");

    /**
     * RFC 4918 property and lock bodies are tiny. Validating one means holding it in memory, so
     * this bounds how much a caller can make us buffer.
     */
    static final int MAX_BODY_BYTES = 512 * 1024;

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

        if (!XML_BODY_METHODS.contains(request.getMethod().toUpperCase())) {
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
        // a lock refresh. Only verify when there is something to verify.
        if (body.get().length > 0 && !isFreeOfDoctype(body.get())) {
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
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            final byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
                if (buffer.size() > MAX_BODY_BYTES) {
                    return Optional.empty();
                }
            }
            return Optional.of(buffer.toByteArray());
        }
    }

    /**
     * True when the document parses and declares no DOCTYPE. Fails closed: if the hardened parser
     * cannot be built, or the document is malformed, the answer is false. Letting a document
     * through because we failed to inspect it would defeat the point of the filter.
     */
    static boolean isFreeOfDoctype(final byte[] body) {
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            factory.setXIncludeAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            final XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setContentHandler(new DefaultHandler());
            reader.setErrorHandler(new DefaultHandler());
            reader.parse(new InputSource(new ByteArrayInputStream(body)));
            return true;
        } catch (final Exception e) {
            Logger.debug(WebDavXmlValidationFilter.class,
                    () -> "WebDAV body rejected during XML verification: " + e.getMessage());
            return false;
        }
    }

    /** Replays the already-consumed body so the WebDAV servlet can read it again. */
    private static final class BufferedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private BufferedBodyRequest(final HttpServletRequest request, final byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream source = new ByteArrayInputStream(body);
            return new ServletInputStream() {
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
                    // The body is fully buffered, so there is nothing to notify about.
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            final String encoding = getCharacterEncoding();
            final Charset charset = encoding == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
