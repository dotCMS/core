package com.dotcms.graphql;

import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import graphql.servlet.GraphQLServletListener;

public class DotGraphQLServletListener implements GraphQLServletListener {

    @Override
    public RequestCallback onRequest(HttpServletRequest request, HttpServletResponse response) {


        return new RequestCallback() {
            @Override
            public void onError(HttpServletRequest request, HttpServletResponse response, Throwable throwable) {
                Logger.error(throwable.getClass(), throwable.getMessage(), throwable);
            }
        };

    }
}
