package com.dotcms.graphql;

import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import graphql.servlet.GraphQLContext;

public class DotGraphQLContext extends GraphQLContext {

    private User user;

    public DotGraphQLContext(final HttpServletRequest httpServletRequest,
                             final HttpServletResponse httpServletResponse, User user) {
        super(httpServletRequest, httpServletResponse);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
