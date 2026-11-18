package com.dotcms.inference.rest;

import com.dotcms.auth.dotAuth.rest.DotAuthSessionCredentialProcessorImpl;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenAuthCredentialProcessorImpl;
import com.dotcms.inference.model.InferenceError;
import com.dotcms.inference.rest.view.InferenceErrorView;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;

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
 * <p>That matters more here than it would elsewhere, because another decision was made on the
 * assumption that it was already true. This family emits no cross-origin headers, and
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

    /**
     * Resolves the caller from the bearer token itself, ignoring any session on the request.
     *
     * <p>Checking the header's shape is not authentication. {@code WebResource.getCurrentUser}
     * reads {@code PortalUtil.getUser(request)} before it authenticates anything, so a request
     * carrying both a session cookie and an unparseable {@code Authorization: Bearer} header is
     * served as the session user — the token is never examined. That accepts a credential this
     * family refuses, and where the token is valid but belongs to someone else, it runs the
     * request as the session's owner rather than the token's.</p>
     *
     * <p>The same two processors {@code WebResource} uses are called here, in its order, so a
     * token accepted elsewhere in dotCMS is accepted here. What differs is that nothing else is
     * consulted when they decline.</p>
     *
     * @param request the inbound request
     * @return the token's user, or null when the token authenticates nobody
     */
    public static User bearerUser(final HttpServletRequest request) {
        try {
            final User sessionRefUser = DotAuthSessionCredentialProcessorImpl.getInstance()
                    .processAuthHeaderFromSessionRef(request);
            if (sessionRefUser != null) {
                return sessionRefUser;
            }
            return JsonWebTokenAuthCredentialProcessorImpl.getInstance()
                    .processAuthHeaderFromJWT(request);
        } catch (final Exception e) {
            // A credential that cannot be processed is a credential that did not authenticate.
            // The reason is logged rather than returned: it describes the token the caller sent,
            // and a caller learning why their forgery failed is being told how to forge better.
            Logger.warn(BearerOnlyAuthFilter.class,
                    "Bearer credential could not be processed: " + e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * @return the refusal for a bearer token that authenticated nobody
     */
    public static InferenceError invalidBearerToken() {
        return new InferenceError(
                "invalid_request_error",
                "The bearer token is not valid.",
                null,
                Response.Status.UNAUTHORIZED.getStatusCode());
    }

    /**
     * @return the refusal for a request whose session names a different caller than its token
     */
    public static InferenceError credentialConflict() {
        return new InferenceError(
                "invalid_request_error",
                "The request carries a session for a different user than its bearer token."
                        + " Send the token without a session.",
                null,
                Response.Status.UNAUTHORIZED.getStatusCode());
    }
}
