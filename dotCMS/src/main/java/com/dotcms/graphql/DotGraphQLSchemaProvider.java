package com.dotcms.graphql;

import com.dotcms.auth.providers.jwt.services.JsonWebTokenAuthCredentialProcessorImpl;
import com.dotcms.graphql.business.GraphqlAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import graphql.kickstart.servlet.config.GraphQLSchemaServletProvider;
import graphql.schema.GraphQLSchema;
import graphql.schema.visibility.NoIntrospectionGraphqlFieldVisibility;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.HandshakeRequest;

public class DotGraphQLSchemaProvider implements GraphQLSchemaServletProvider {
    @Override
    public GraphQLSchema getSchema(final HttpServletRequest request) {
        final User user = getUser(request);
        Logger.debug(DotGraphQLSchemaProvider.class, ()->String.format("user [%s] requesting graphQL schema.", user));
        return getSchema(user);
    }

    @Override
    public GraphQLSchema getSchema(HandshakeRequest request) {
        return null;
    }

    @Override
    public GraphQLSchema getSchema() {
        return getSchema(APILocator.systemUser());
    }

    /**
     * User aware schema generation
     * This limits the {@link GraphQLSchema} to be built using {@link NoIntrospectionGraphqlFieldVisibility} when no authenticated user sends the request
     * @param user current user requesting
     * @return GraphQL Schema
     */
     GraphQLSchema getSchema(final User user) {
        try {
            final GraphqlAPI graphqlAPI = APILocator.getGraphqlAPI();
            graphqlAPI.printSchema();
            return graphqlAPI.getSchema(user);
        } catch(DotDataException e) {
            Logger.error(this, "Error with Schema retrieval/generation", e);
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public GraphQLSchema getReadOnlySchema() {
        return null;
    }

    @Override
    public GraphQLSchema getReadOnlySchema(HttpServletRequest request) {
        return null;
    }

    /**
     * Retrieve user info from the request
     * @param request
     * @return
     */
    private User getUser(final HttpServletRequest request) {

        User user = PortalUtil.getUser(request);
        if (null == user) {
            user = JsonWebTokenAuthCredentialProcessorImpl.getInstance().processAuthHeaderFromJWT(request);
        }
        if (null == user) {
            user = APILocator.getUserAPI().getAnonymousUserNoThrow();
        }
        return user;
    }

}
