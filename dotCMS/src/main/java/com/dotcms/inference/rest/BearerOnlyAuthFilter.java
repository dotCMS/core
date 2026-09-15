package com.dotcms.inference.rest;

import com.dotcms.inference.model.InferenceError;
import com.dotcms.inference.rest.view.InferenceErrorView;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Optional;

/**
 * Refuses any credential on this family other than a bearer token.
 *
 * <p>Without this, the surrounding dotCMS authentication accepts what it accepts everywhere else:
 * {@code WebResource.authenticate} falls through to {@code PortalUtil.getUser(request)}, so a live
 * session cookie — or basic auth — authenticates a caller who sent no {@code Authorization} header
 * at all.</p>
 *
 * <p>That matters more here than it would elsewhere, because it is load-bearing for a decision
 * made on the assumption that it was already true. This family emits no cross-origin headers, and
 * the stated reason is that its credential is a long-lived token someone deliberately issued and
 * placed on a server — not an ambient credential a browser attaches on its own. If a session
 * cookie authenticates, that reasoning collapses: any page the user has open is one fetch away
 * from spending the site's AI budget, and the absence of CORS headers becomes a formality rather
 * than a control.</p>
 *
 * <p>The rule itself lives in {@link #bearerCredentialProblem(String)} rather than in this filter,
 * and the resources call it directly as well. A filter alone would be a guarantee that exists only
 * inside the JAX-RS chain — invisible to anything invoking a resource method directly, which is
 * how this family's integration tests reach it. A guarantee that cannot be tested where it is
 * relied upon is not much of a guarantee. The filter earns its place by applying automatically to
 * every resource in the family, including ones not yet written.</p>
 */
@Provider
@InferenceEndpoint
public class BearerOnlyAuthFilter implements ContainerRequestFilter {

    /** The only credential scheme this family accepts. */
    static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        bearerCredentialProblem(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION))
                .ifPresent(error -> requestContext.abortWith(
                        Response.status(error.httpStatus())
                                .entity(InferenceErrorView.of(error))
                                .type(MediaType.APPLICATION_JSON)
                                .build()));
    }

    /**
     * Checks a credential, without deciding what to do about a bad one.
     *
     * <p>Returns the problem rather than throwing, so the filter can abort the exchange and a
     * resource can fold it into whatever it already does with errors, from one implementation of
     * the rule.</p>
     *
     * @param authorizationHeader the request's Authorization header, or null when absent
     * @return the refusal to send, or empty when the credential is an acceptable bearer token
     */
    public static Optional<InferenceError> bearerCredentialProblem(final String authorizationHeader) {
        if (authorizationHeader != null
                && authorizationHeader.startsWith(BEARER_PREFIX)
                && !authorizationHeader.substring(BEARER_PREFIX.length()).isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new InferenceError(
                "invalid_request_error",
                "This endpoint accepts a dotCMS API token as 'Authorization: Bearer <token>'."
                        + " Session and basic credentials are not accepted.",
                null,
                Response.Status.UNAUTHORIZED.getStatusCode()));
    }
}
