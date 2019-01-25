package com.dotcms.graphql;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

import graphql.servlet.GraphQLContext;
import graphql.servlet.GraphQLContextBuilder;

public class DotGraphQLContextBuilder implements GraphQLContextBuilder {
    private final WebResource webResource;

    DotGraphQLContextBuilder() {
        this.webResource = new WebResource();
    }

    @Override
    public GraphQLContext build(final HttpServletRequest httpServletRequest,
                                final HttpServletResponse httpServletResponse) {
        final InitDataObject initDataObject = this.webResource.init
            (null, true, httpServletRequest, true, null);

        return new DotGraphQLContext(httpServletRequest, httpServletResponse, initDataObject.getUser());
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
