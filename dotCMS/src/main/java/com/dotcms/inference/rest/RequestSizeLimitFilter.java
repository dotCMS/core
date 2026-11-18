package com.dotcms.inference.rest;

import com.dotcms.inference.model.InferenceError;
import com.dotcms.inference.model.InferenceLimits;
import com.dotcms.inference.rest.view.InferenceErrorView;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.WebApplicationException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Refuses a request body larger than the configured ceiling, before it is read.
 *
 * <p>Removing the legacy 4096-character prompt cap was right — it was a character count standing
 * in for a model's context window, which is the wrong control in the wrong place. But it left
 * request size unbounded, and dotCMS parses and forwards a payload before any provider gets the
 * chance to reject it, so the memory cost lands on this node and the token cost on the site's
 * bill first.</p>
 *
 * <p>Rejecting here, with a typed error naming the limit, rather than leaving it to the servlet
 * container: a container-level rejection is invisible to the caller's client library and varies
 * per deployment.</p>
 */
@Provider
@InferenceEndpoint
public class RequestSizeLimitFilter implements ContainerRequestFilter {

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        final int declaredLength = requestContext.getLength();
        final InferenceLimits limits = InferenceLimits.current();

        if (declaredLength < 0) {
            // Chunked, or a client that simply did not declare a length — which several HTTP
            // clients do by default when they stream a body. There is no number to check here,
            // so the ceiling is enforced on the way through instead: the entity stream is
            // replaced with one that counts bytes and refuses past the limit. An earlier version
            // returned at this point, with a comment claiming the resource enforced the ceiling
            // as it read. No resource did, which left the single control on body size skippable
            // by omitting one header.
            requestContext.setEntityStream(
                    new CeilingEnforcingStream(requestContext.getEntityStream(),
                            limits.maxRequestBytes()));
            return;
        }

        if (limits.exceedsMaxRequestBytes(declaredLength)) {
            final InferenceError error = new InferenceError("invalid_request_error",
                    "Request body of " + declaredLength + " bytes exceeds the maximum of "
                            + limits.maxRequestBytes() + " bytes", null,
                    Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode());
            requestContext.abortWith(
                    Response.status(error.httpStatus())
                            .entity(InferenceErrorView.of(error))
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
    }

    /**
     * Builds the refusal sent when a body exceeds the ceiling.
     *
     * @param maxRequestBytes the ceiling in force
     * @return the 413, in the standard error shape
     */
    static Response tooLarge(final int maxRequestBytes) {
        final InferenceError error = new InferenceError("invalid_request_error",
                "Request body exceeds the maximum of " + maxRequestBytes + " bytes", null,
                Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode());
        return Response.status(error.httpStatus())
                .entity(InferenceErrorView.of(error))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * An entity stream that refuses to yield more than the ceiling allows.
     *
     * <p>Used when the request declares no length. Counting on the way through is the only way to
     * bound a body whose size is not known until it ends, and refusing mid-read is the point: the
     * alternative is to buffer the whole thing to measure it, which hands an attacker exactly the
     * memory cost the ceiling exists to prevent.</p>
     *
     * <p>The refusal is a {@link WebApplicationException} carrying the same typed 413 the declared
     * path returns, so a caller cannot tell which route refused it — and neither can a client
     * library, which is the point of a contract.</p>
     */
    static final class CeilingEnforcingStream extends FilterInputStream {

        private final int maxRequestBytes;
        private long seen;

        CeilingEnforcingStream(final InputStream delegate, final int maxRequestBytes) {
            super(delegate);
            this.maxRequestBytes = maxRequestBytes;
        }

        @Override
        public int read() throws IOException {
            final int value = super.read();
            if (value != -1) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(final byte[] buffer, final int offset, final int length)
                throws IOException {
            final int read = super.read(buffer, offset, length);
            if (read > 0) {
                count(read);
            }
            return read;
        }

        /**
         * @param justRead how many bytes the last read produced
         */
        private void count(final int justRead) {
            seen += justRead;
            if (seen > maxRequestBytes) {
                throw new WebApplicationException(tooLarge(maxRequestBytes));
            }
        }
    }
}
