package com.dotcms.graphql;

import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.util.Logger;
import graphql.kickstart.execution.context.GraphQLKickstartContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import io.vavr.control.Try;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;


public class DotGraphQLContextBuilder implements GraphQLServletContextBuilder {

    /**
     * Authenticates the caller and builds the GraphQL context for the request.
     * <p>
     * When authentication is refused, for example an anonymous caller while
     * {@code CONTENT_APIS_ALLOW_ANONYMOUS} is {@code NONE}, the error status (401 by default) is
     * sent with a generic "Unauthorized" body before the exception is rethrown; the detailed reason
     * goes to the debug log only. Without this the GraphQL servlet reports the failure
     * as an unparseable request and answers 400 with an empty body.
     *
     * @param httpServletRequest  the GraphQL HTTP request
     * @param httpServletResponse the response; it is committed with the error status when access is refused
     * @return the context holding the request, the response and the authenticated user
     * @throws SecurityException when the caller may not use the GraphQL API
     */
    @Override
    public GraphQLKickstartContext build(final HttpServletRequest httpServletRequest,
                                         final HttpServletResponse httpServletResponse) {
        final InitDataObject initDataObject;
        try {
            initDataObject = new WebResource.InitBuilder()
                    .requestAndResponse(httpServletRequest, httpServletResponse)
                    .rejectWhenNoUser(false)
                    .requiredAnonAccess(AnonymousAccess.systemSetting())
                    .init();
        } catch (final SecurityException e) {
            // the reason can name the setting that refused the caller, so keep it out of the body
            Logger.debug(this, () -> "Rejected GraphQL request: " + e.getMessage());
            if (!httpServletResponse.isCommitted()) {
                final int status = null != e.getResponse() ? e.getResponse().getStatus()
                        : HttpServletResponse.SC_UNAUTHORIZED;
                Try.run(() -> httpServletResponse.sendError(status, "Unauthorized"))
                        .onFailure(sendFailure -> Logger.debug(this, () ->
                                "Could not send the GraphQL auth error: " + sendFailure.getMessage()));
            }
            throw e;
        }
        return DotGraphQLContext.createServletContext()
                .with(httpServletRequest)
                .with(httpServletResponse)
                .with(initDataObject.getUser()).build();
    }

    @Override
    public GraphQLKickstartContext build(final Session session, final HandshakeRequest handshakeRequest) {
        return null;
    }

    @Override
    public GraphQLKickstartContext build() {
        return null;
    }
}
