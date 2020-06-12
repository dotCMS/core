package com.dotcms.graphql;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;

import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import org.dataloader.DataLoaderRegistry;


public class DotGraphQLContextBuilder implements GraphQLServletContextBuilder {
    private final WebResource webResource;

    DotGraphQLContextBuilder() {
        this.webResource = new WebResource();
    }

    @Override
    public GraphQLContext build(final HttpServletRequest httpServletRequest,
                                final HttpServletResponse httpServletResponse) {
        final InitDataObject initDataObject = this.webResource.init
            (null, true, httpServletRequest, false, null);

        return DotGraphQLContext.createServletContext()
                .with(httpServletRequest)
                .with(httpServletResponse)
                .with(initDataObject.getUser()).build();
    }

    @Override
    public GraphQLContext build(final Session session, final HandshakeRequest handshakeRequest) {
        return null;
    }

    @Override
    public GraphQLContext build() {
        return null;
    }
}
