package com.dotcms.graphql;

import graphql.kickstart.servlet.core.GraphQLServletListener;
import javax.ws.rs.core.Response;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.util.Logger;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DotGraphQLServletListener implements GraphQLServletListener {

    @Override
    public RequestCallback onRequest(final HttpServletRequest request, final HttpServletResponse response) {

        return new RequestCallback() {
            @Override
            public void onError(final HttpServletRequest request, final HttpServletResponse servletResponse,
                                final Throwable throwable) {
                Logger.error(throwable.getClass(), throwable.getMessage(), throwable);
                final Response response = ResponseUtil.mapExceptionResponse(throwable);
                try {
                    servletResponse.setStatus(response.getStatus());
                    servletResponse.getWriter().println(response.getHeaderString("error-message"));
                } catch (IOException e) {
                    Logger.error(this, "Unable to print error message", e);
                }
            }
        };
    }
}
