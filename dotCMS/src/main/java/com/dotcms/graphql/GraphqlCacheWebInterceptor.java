package com.dotcms.graphql;

import static com.dotcms.util.HttpRequestDataUtil.getHeaderCaseInsensitive;
import static com.dotcms.util.HttpRequestDataUtil.getParamCaseInsensitive;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This interceptor caches the GraphQL responses based on the following:
 * <p>
 * If no parameters provided it will cache the response based on the config params of the underlying cache implementation {@link GraphQLCache}
 *   and using the GraphQL query has the key
 * <p>
 * If <code>dotcachettl</code> (in seconds) is provided it will cache the response for the provided time. This parameter supports two special values:
 * <ul>
 *     <li>0: bypasses the cache
 *     <li>-1:bypasses the cache and clears this entry
 * </ul>
 * <p>
 * If `dotcachekey` is provided it will cache the response using this as the cache key instead of the GraphQL query
 * <p>
 * If `dotcacherefresh` is provided it will background refresh the cache entry for this key/query
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
        if (!query.isPresent() ) {
            return Result.NEXT;
        }

        final String syncKey = getParamOrHeader(requestIn, GRAPHQL_CACHE_KEY_PARAM)
                .orElseGet(() -> query.get().intern());

        final boolean bypassCache = bypassCacheByTTL(requestIn)
                || query.get().contains(INTROSPECTION_OPERATION_NAME);

        Optional<String> graphResponseFromCache = bypassCache
                ? Optional.empty()
                : graphCache.get(syncKey);

        if (graphResponseFromCache.isPresent()) {
            writeResponse(response, graphResponseFromCache.get());
            refreshCacheEntryInBackgroundIfRequested(requestIn, syncKey);
            return Result.SKIP_NO_CHAIN;
        }

        synchronized (syncKey) {
            graphResponseFromCache = bypassCache ? Optional.empty() : graphCache.get(syncKey);

            if (graphResponseFromCache.isPresent()) {
                writeResponse(response, graphResponseFromCache.get());
                refreshCacheEntryInBackgroundIfRequested(requestIn, syncKey);
                return Result.SKIP_NO_CHAIN;
            }

            wrapper.setAttribute(GRAPHQL_CACHE_KEY, syncKey);
            wrapper.setAttribute(GRAPHQL_BYPASS_CACHE, bypassCache);
            return new Result.Builder().wrap(new MockHttpCaptureResponse(response))
                    .wrap(wrapper).next().build();
        }
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

    private Optional<Integer> getCacheTTL(HttpServletRequest requestIn) {
        final Optional<String> paramOrHeader = getParamOrHeader(requestIn, GRAPHQL_CACHE_TTL_PARAM);

        if(paramOrHeader.isPresent()) {
            return Try.of(()->
                Optional.of(Integer.parseInt(paramOrHeader.get()))
            ).getOrElse(Optional.empty());
        }

        return Optional.empty();
    }

    @Override
    public boolean afterIntercept(final HttpServletRequest request,
            final HttpServletResponse response) {
        final String cacheKey = (String) request.getAttribute(GRAPHQL_CACHE_KEY);

        if(response.getStatus() ==200 && response instanceof MockHttpCaptureResponse
                && UtilMethods.isSet(cacheKey)) {

            final MockHttpCaptureResponse mockResponse = (MockHttpCaptureResponse) response;
            response.setHeader("x-graphql-cache", "miss, writing to cache");
            final String graphqlResponse = mockResponse.writer.toString();

            final Map<String,Object> map = Try.of(()->new ObjectMapper()
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

        }
        return true;
    }

    private void refreshCacheEntryInBackgroundIfRequested(final HttpServletRequest request, final String key) {
        final Optional<String> backgroundRefreshParam = getParamOrHeader(request,
                GRAPHQL_CACHE_REFRESH_PARAM);

        if(backgroundRefreshParam.isPresent()
                && backgroundRefreshParam.get().equalsIgnoreCase("true")) {
            DotConcurrentFactory.getInstance().getSingleSubmitter()
                    .submit(()-> graphCache.remove(key));
        }
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

}
