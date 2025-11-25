package com.dotcms.graphql;

import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import graphql.kickstart.execution.context.GraphQLKickstartContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;


public class DotGraphQLContextBuilder implements GraphQLServletContextBuilder {

    @Override
    public GraphQLKickstartContext build(final HttpServletRequest httpServletRequest,
                                         final HttpServletResponse httpServletResponse) {
        final InitDataObject initDataObject =
                new WebResource.InitBuilder()
                        .requestAndResponse(httpServletRequest, httpServletResponse)
                        .rejectWhenNoUser(false)
                        .requiredAnonAccess(AnonymousAccess.systemSetting())
                        .init();
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
