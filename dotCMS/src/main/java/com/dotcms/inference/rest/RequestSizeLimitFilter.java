package com.dotcms.inference.rest;

import com.dotcms.inference.model.InferenceError;
import com.dotcms.inference.model.InferenceLimits;
import com.dotcms.inference.rest.view.InferenceErrorView;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

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
        if (declaredLength < 0) {
            // Chunked or unknown length; the resource enforces the ceiling as it reads.
            return;
        }
        final InferenceLimits limits = InferenceLimits.current();
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
}
