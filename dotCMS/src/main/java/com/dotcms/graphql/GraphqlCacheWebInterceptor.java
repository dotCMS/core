package com.dotcms.graphql;

import static com.dotcms.graphql.GraphQLCache.GRAPHQL_CACHE_RESULTS_CONFIG_PROPERTY;
import static com.dotcms.util.HttpRequestDataUtil.getHeaderCaseInsensitive;
import static com.dotcms.util.HttpRequestDataUtil.getParamCaseInsensitive;

import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.mock.request.DotCMSMockRequest;
import com.dotcms.mock.request.HttpRequestReaderWrapper;
import com.dotcms.mock.response.MockHttpWriterCaptureResponse;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This interceptor returns the response for a GraphQL request from cache, if available.
 * Otherwise it lets the request to be processed as normal and caches the GraphQL response,
 * on a expiring cache {@link GraphQLCache}, after the request is processed.
 *
 * By default it uses the GraphQL query as the key if no custom key is specified.
 * <p>
 * It takes the following parameters from the request, which can be sent as query params or headers:
 * <p>
 * <code>dotcachettl</code>: amount of time in seconds to cache the response for if not already in cache, otherwise it will be ignored.
 * Special values:
 * <ul>
 *     <li>0: bypasses the cache
 *     <li>-1:bypasses the cache and clears this entry for the given key/query
 * </ul>
 * <p>
 * <code>dotcachekey</code>: uses this as the cache key instead of the GraphQL query
 * <p>
 * <code>dotcacherefresh</code>: background refreshes the cache entry for this key/query
 *
 * If no parameters are provided it will cache the response based on the default config of the underlying cache implementation {@link GraphQLCache}
 *   and using the GraphQL query as the key.
 */
public class GraphqlCacheWebInterceptor implements WebInterceptor {

    final static String GRAPHQL_CACHE_KEY = "GRAPHQL_CACHE_KEY";
    final static String GRAPHQL_CACHE_TTL_PARAM = "dotcachettl";
    final static String GRAPHQL_CACHE_KEY_PARAM = "dotcachekey";
    final static String GRAPHQL_CACHE_REFRESH_PARAM = "dotcacherefresh";
    private static final String API_CALL = "/api/v1/graphql";
    private static final String INTROSPECTION_OPERATION_NAME = "\"operationName\":\"IntrospectionQuery\"";
    private static final String GRAPHQL_BYPASS_CACHE = "GRAPHQL_BYPASS_CACHE";
    private static final int INVALIDATE_CACHE = -1;

    private final GraphQLCache graphCache = CacheLocator.getGraphQLCache();

    private final Lazy<Boolean> ENABLED_FROM_CONFIG = Lazy.of(()->Config
                    .getBooleanProperty(GRAPHQL_CACHE_RESULTS_CONFIG_PROPERTY, true));

    private final DotGraphQLHttpServlet graphQLHttpServlet = new DotGraphQLHttpServlet();

    private final Lazy<Integer> defaultTTL = Lazy.of(()->Config.getIntProperty("cache.graphqlquerycache.seconds", 15));

    @Override
    public String[] getFilters() {
        return new String[] {API_CALL + "*"};
    }

    @Override
    public Result intercept(final HttpServletRequest requestIn, final HttpServletResponse response) throws IOException {

        if (!"POST".equals(requestIn.getMethod()) || !LicenseManager.getInstance().isEnterprise()) {
            return Result.NEXT;
        }
        final HttpRequestReaderWrapper wrapper = new HttpRequestReaderWrapper(requestIn);

        final Optional<String> query = wrapper.getRawRequest();
        if (query.isEmpty() ) {
            return Result.NEXT;
        }

        final String syncKey = getParamOrHeader(requestIn, GRAPHQL_CACHE_KEY_PARAM)
                .orElseGet(() -> query.get().intern());

        final boolean bypassCache = bypassCacheByTTL(requestIn)
                || query.get().contains(INTROSPECTION_OPERATION_NAME);

        final Integer cacheTTL = getCacheTTL(requestIn).isPresent() ? getCacheTTL(requestIn).get()
                : null;

        Optional<String> graphResponseFromCache = bypassCache
                ? Optional.empty()
                : refreshKey(wrapper) ?
                        getAndRefresh(wrapper, response, syncKey, cacheTTL)
                        : graphCache.get(syncKey);

        if (graphResponseFromCache.isPresent()) {
            writeResponse(response, graphResponseFromCache.get());
            return Result.SKIP_NO_CHAIN;
        }

        synchronized (syncKey) {
            graphResponseFromCache = bypassCache ? Optional.empty() : graphCache.get(syncKey);

            if (graphResponseFromCache.isPresent()) {
                writeResponse(response, graphResponseFromCache.get());
                return Result.SKIP_NO_CHAIN;
            }

            wrapper.setAttribute(GRAPHQL_CACHE_KEY, syncKey);
            wrapper.setAttribute(GRAPHQL_BYPASS_CACHE, bypassCache);
            return new Result.Builder().wrap(new MockHttpWriterCaptureResponse(response))
                    .wrap(wrapper).next().build();
        }
    }

    private Optional<String> getAndRefresh(HttpRequestReaderWrapper wrapper, HttpServletResponse response, String syncKey,
            Integer cacheTTL) {
        // copy request
        final DotCMSMockRequest requestCopy = getRequestCopy(wrapper);

        return graphCache.getAndRefresh(syncKey, ()
                        -> processGraphQLRequest(requestCopy, response),
                cacheTTL);
    }

    private DotCMSMockRequest getRequestCopy(HttpRequestReaderWrapper wrapper) {
        final DotCMSMockRequest requestCopy = new DotCMSMockRequest();
        requestCopy.setContent(wrapper.getRawRequest().get());
        requestCopy.setParameterMap(wrapper.getParameterMap());

        final Enumeration<String> attributes = wrapper.getAttributeNames();
        
        while(attributes.hasMoreElements()) {
            String name = attributes.nextElement();
            requestCopy.setAttribute(name, wrapper.getAttribute(name));
        }
        return requestCopy;
    }

    private void writeResponse(final HttpServletResponse response,
            final String graphResponseFromCache) throws IOException {
        corsHeaders.forEach(response::setHeader);
        response.setContentType("application/json;charset=UTF-8");
        response.setContentLength(graphResponseFromCache.getBytes().length);
        response.getWriter().write(graphResponseFromCache);
    }

    private boolean bypassCacheByTTL(HttpServletRequest requestIn) {
        final Optional<Integer> cacheTTL = getCacheTTL(requestIn);
        return cacheTTL.isPresent() && cacheTTL.get() <=0;
    }

    private boolean refreshKey(HttpServletRequest requestIn) {
        final Optional<String> backgroundRefreshParam = getParamOrHeader(requestIn,
                GRAPHQL_CACHE_REFRESH_PARAM);

        return backgroundRefreshParam.isPresent()
                && backgroundRefreshParam.get().equalsIgnoreCase("true");
    }

    private Optional<Integer> getCacheTTL(HttpServletRequest requestIn) {
        final Optional<String> paramOrHeader = getParamOrHeader(requestIn, GRAPHQL_CACHE_TTL_PARAM);

        if(paramOrHeader.isPresent()) {
            return Try.of(()->
                Optional.of(Integer.parseInt(paramOrHeader.get()))
            ).getOrElse(Optional.empty());
        }

        return Optional.of(defaultTTL.get());
    }

    ObjectMapper getObjectMapper(){
        return DotObjectMapperProvider.getInstance().getIso8610ObjectMapper();
    }

    @Override
    public boolean afterIntercept(final HttpServletRequest request,
            final HttpServletResponse response) {
        final String cacheKey = (String) request.getAttribute(GRAPHQL_CACHE_KEY);

        if(response.getStatus() ==200 && response instanceof MockHttpWriterCaptureResponse
                && UtilMethods.isSet(cacheKey)) {
            final MockHttpWriterCaptureResponse mockResponse = (MockHttpWriterCaptureResponse) response;
            response.setHeader("x-graphql-cache", "miss, writing to cache");
            final String graphqlResponse = mockResponse.writer.toString();

            final Map<String,Object> map = Try.of(()->getObjectMapper()
                    .readValue(graphqlResponse, Map.class)).getOrNull();

            Try.run(() -> mockResponse.originalResponse.getWriter().write(graphqlResponse));

            final boolean bypassCache = (boolean) request.getAttribute(GRAPHQL_BYPASS_CACHE);
            final Lazy<Optional<Integer>> cacheTTL = Lazy.of(()-> getCacheTTL(request));

            if(map!=null && map.get("data")!=null && !bypassCache && cacheTTL.get().isPresent()) {
                graphCache.put(cacheKey, graphqlResponse, cacheTTL.get().get());
            } else if(map!=null && map.get("data")!=null && !bypassCache) {
                graphCache.put(cacheKey, graphqlResponse);
            }

            if(cacheTTL.get().isPresent() && cacheTTL.get().get()==INVALIDATE_CACHE) {
                graphCache.remove(cacheKey);
            }
        } else if(response instanceof MockHttpWriterCaptureResponse) {
            final MockHttpWriterCaptureResponse mockResponse = (MockHttpWriterCaptureResponse) response;
            final String graphqlResponse = mockResponse.writer.toString();
            Try.run(() -> mockResponse.originalResponse.getWriter().write(graphqlResponse));
        }
        return true;
    }

    private String processGraphQLRequest(final HttpServletRequest request,
            final HttpServletResponse response)  {
        try {
            graphQLHttpServlet.init();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        MockHttpWriterCaptureResponse mockHttpResponse = new MockHttpWriterCaptureResponse(response);
        graphQLHttpServlet.doPost(request, mockHttpResponse);
        return mockHttpResponse.writer.toString();
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
        final Optional<String> param = getParamCaseInsensitive(request, paramOrHeaderName);
        return param.isPresent() ? param : getHeaderCaseInsensitive(request, paramOrHeaderName);
    }

    @Override
    public boolean isActive() {
        return LicenseManager.getInstance().isEnterprise() && ENABLED_FROM_CONFIG.get();
    }

}
