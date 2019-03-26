package com.dotcms.graphql;

import com.liferay.portal.model.User;
import graphql.servlet.GraphQLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DotGraphQLContext extends GraphQLContext {

  private final User user;

  public DotGraphQLContext(
      final HttpServletRequest httpServletRequest,
      final HttpServletResponse httpServletResponse,
      final User user) {
    super(httpServletRequest, httpServletResponse);
    this.user = user;
  }

  public User getUser() {
    return user;
  }
}
