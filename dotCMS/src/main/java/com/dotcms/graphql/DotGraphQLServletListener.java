package com.dotcms.graphql;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.util.Logger;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import graphql.servlet.GraphQLServletListener;

public class DotGraphQLServletListener implements GraphQLServletListener {

    @Override
    public RequestCallback onRequest(HttpServletRequest request, HttpServletResponse response) {

        return new RequestCallback() {
            @Override
            public void onError(HttpServletRequest request, HttpServletResponse servletResponse, Throwable throwable) {
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
