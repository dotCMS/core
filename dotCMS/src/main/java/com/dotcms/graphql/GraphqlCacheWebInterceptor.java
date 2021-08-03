package com.dotcms.graphql;

import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class GraphqlCacheWebInterceptor implements WebInterceptor {

    final static String GRAPHQL_QUERY = "GRAPHQL_QUERY";
    final static String GRAPHQL_CACHE_TTL = "dotcachettl";
    final static String GRAPHQL_CACHE_KEY = "dotcachekey";
    private static final String API_CALL = "/api/v1/graphql";

    private final GraphQLCache graphCache = GraphQLCache.INSTANCE.get();

    @Override
    public String[] getFilters() {
        return new String[] {API_CALL + "*"};
    }

    @Override
    public Result intercept(final HttpServletRequest requestIn, final HttpServletResponse response) throws IOException {

        if (!"POST".equals(requestIn.getMethod()) || !LicenseManager.getInstance().isEnterprise()) {
            return Result.NEXT;
        }
        final  HttpRequestReaderWrapper wrapper = new HttpRequestReaderWrapper(requestIn);

        final Optional<String> query = wrapper.getGraphQLQuery();
        if (!query.isPresent()) {
            return Result.NEXT;
        }

        final String syncKey = getParamOrHeader(requestIn, GRAPHQL_CACHE_KEY)
                .orElseGet(() -> query.get().intern());

        final boolean bypassCache = bypassCache(requestIn);

        Optional<String> graphResponseFromCache = bypassCache ? Optional.empty() : graphCache.get(syncKey);

        if (graphResponseFromCache.isPresent()) {
            corsHeaders.forEach(response::setHeader);
            response.setContentType("application/json;charset=UTF-8");
            response.setContentLength(graphResponseFromCache.get().getBytes().length);
            response.getWriter().write(graphResponseFromCache.get());
            return Result.SKIP_NO_CHAIN;
        }

        synchronized (syncKey) {
            graphResponseFromCache = bypassCache ? Optional.empty() : graphCache.get(syncKey);

            if (graphResponseFromCache.isPresent()) {
                corsHeaders.forEach(response::setHeader);
                response.setContentType("application/json;charset=UTF-8");
                response.setContentLength(graphResponseFromCache.get().getBytes().length);
                response.getWriter().write(graphResponseFromCache.get());
                return Result.SKIP_NO_CHAIN;
            }

            wrapper.setAttribute(GRAPHQL_QUERY, syncKey);
            return new Result.Builder().wrap(new MockHttpCaptureResponse(response)).wrap(wrapper).next().build();
        }
    }

    private Boolean bypassCache(HttpServletRequest requestIn) {
        final Optional<String> paramOrHeader = getParamOrHeader(requestIn, GRAPHQL_CACHE_TTL);

        return Try.of(() -> paramOrHeader.isPresent() &&
                        Integer.parseInt(paramOrHeader.get()) <= 0
        ).getOrElse(false);
    }

    @Override
    public boolean afterIntercept(final HttpServletRequest request, final HttpServletResponse response) {
        final String query = (String) request.getAttribute(GRAPHQL_QUERY);

        if(response.getStatus() ==200 && response instanceof MockHttpCaptureResponse
                && UtilMethods.isSet(query)) {

            final MockHttpCaptureResponse mockResponse = (MockHttpCaptureResponse) response;
            response.setHeader("x-graphql-cache", "miss, writing to cache");
            final String graphqlResponse = mockResponse.writer.toString();

            Map<String,Object> map = Try.of(()->new ObjectMapper().readValue(graphqlResponse, Map.class)).getOrNull();


            Try.run(() -> mockResponse.originalResponse.getWriter().write(graphqlResponse));
            final int cacheTTL = Try.of(()->Integer.parseInt(request.getParameter(GRAPHQL_CACHE_TTL)))
                    .getOrElse(0);
            if(map!=null && map.get("data")!=null && cacheTTL>0) {
                final String key = getParamOrHeader(request, GRAPHQL_CACHE_KEY).orElse(query);
                graphCache.put(key, graphqlResponse, cacheTTL);
            }
        }
        return true;
    }

    private final Map<String,String> corsHeaders = ImmutableMap.<String, String>builder()
                        .put("access-control-allow-origin", "*")
                        .put("access-control-allow-credentials", "true")
                        .put("access-control-allow-headers", "*")
                        .put("access-control-allow-methods", "GET,PUT,POST,DELETE,HEAD,OPTIONS,PATCH")
                        .put("access-control-expose-headers", "*")
                        .put("x-graphql-cache", "hit")
                        .build();

    private Optional<String> getParamOrHeader(final HttpServletRequest request,
            final String paramOrHeaderName) {

        if(UtilMethods.isSet(request.getParameter(paramOrHeaderName))) {
            return Optional.of(request.getParameter(paramOrHeaderName));
        } else if(UtilMethods.isSet(request.getHeader(paramOrHeaderName))) {
            return Optional.of(request.getHeader(paramOrHeaderName));
        }

        return Optional.empty();
    }

}
