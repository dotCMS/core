package com.dotcms.inference.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Reports which site's configuration served the request, on every response.
 *
 * <p>This is the answer to the one real risk in keeping standard site resolution: the fallbacks
 * are convenient and correct, but they make it possible for a request to be served by a site it
 * never named, and nobody could tell. A header answers that for every call — including the ones
 * that resolved perfectly, which is more than an error on the unmatched case would have done,
 * since it also surfaces a host that matched the <em>wrong</em> site through an alias
 * collision.</p>
 *
 * <p>A header rather than a body field on purpose. The payloads of this family have to
 * deserialize into a standard client library's own result types with no adapter, and an extra
 * top-level field risks strict deserializers; a header is invisible to them, is identical for
 * streamed and non-streamed responses, survives on errors where a body field could not, and is
 * readable by the proxy or log pipeline that would actually do the spend reconciliation.</p>
 */
@Provider
@InferenceEndpoint
public class ResolvedSiteHeaderFilter implements ContainerResponseFilter {

    /** Names the site whose dotAI configuration served the request. */
    public static final String RESOLVED_SITE_HEADER = "X-dotCMS-Resolved-Site";

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext responseContext) throws IOException {
        if (request == null) {
            return;
        }
        final Object siteId = request.getAttribute(InferenceRequestAttributes.RESOLVED_SITE_ID);
        if (siteId instanceof String resolved && !resolved.isBlank()) {
            responseContext.getHeaders().putSingle(RESOLVED_SITE_HEADER, resolved);
        }
    }
}
