package com.dotcms.graphql;

import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.mock.request.DotCMSMockRequest;
import com.dotcms.mock.request.HttpRequestReaderWrapper;
import com.dotcms.mock.response.DotCMSMockResponse;
import com.dotcms.mock.response.MockHttpStatusResponse;
import com.dotcms.mock.response.MockHttpWriterCaptureResponse;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.vavr.Lazy;
import io.vavr.control.Try;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.dotcms.graphql.GraphQLCache.GRAPHQL_CACHE_RESULTS_CONFIG_PROPERTY;
import static com.dotcms.util.HttpRequestDataUtil.getHeaderCaseInsensitive;
import static com.dotcms.util.HttpRequestDataUtil.getParamCaseInsensitive;

/**
 * This interceptor returns the response for a GraphQL request from cache, if available.
 * Otherwise it lets the request to be processed as normal and caches the GraphQL response,
 * on a expiring cache {@link GraphQLCache}, after the request is processed.
 * <p>
 * Entries are keyed by {@link GraphqlCacheKey}: the request body together with the caller and the
 * request context (site, language, variant, persona, page mode) that the GraphQL servlet resolves
 * from the request. Callers that present different credentials never share an entry, and anonymous
 * callers only share entries among themselves.
 * <p>
 * It takes the following parameters from the request, which can be sent as query params or headers:
 * <p>
 * Bodies that carry a mutation operation and introspection queries always bypass the cache.
 * <p>
 * <code>dotcachettl</code>: amount of time in seconds to cache the response for if not already in cache, otherwise it will be ignored.
 * Values above <code>cache.graphqlquerycache.maxseconds</code> are capped to that maximum.
 * Special values:
 * <ul>
 *     <li>0: bypasses the cache
 *     <li>-1: bypasses the cache and, for callers that present a credential, clears their entry for the given query
 * </ul>
 * <p>
 * <code>dotcachekey</code>: adds this value to the derived cache key, so the same query can be cached under
 * more than one entry for the same caller and context
 * <p>
 * <code>dotcacherefresh</code>: background refreshes the cache entry for this key/query. The refresh replays the
 * request with its headers; requests whose key depends on the session or on cookies are not refreshed in the
 * background, because that state cannot be replayed.
 *
 * If no parameters are provided it will cache the response based on the default config of the underlying cache implementation {@link GraphQLCache}
 *   and using the derived key.
 */
public class GraphqlCacheWebInterceptor implements WebInterceptor {

    final static String GRAPHQL_CACHE_KEY = "GRAPHQL_CACHE_KEY";
    final static String GRAPHQL_CACHE_TTL_PARAM = "dotcachettl";
    final static String GRAPHQL_CACHE_KEY_PARAM = "dotcachekey";
    final static String GRAPHQL_CACHE_REFRESH_PARAM = "dotcacherefresh";
    static final String GRAPHQL_CACHE_DEFAULT_TTL_PROPERTY = "cache.graphqlquerycache.seconds";
    static final String GRAPHQL_CACHE_MAX_TTL_PROPERTY = "cache.graphqlquerycache.maxseconds";
    static final int GRAPHQL_CACHE_MAX_TTL_DEFAULT = 3600;
    private static final String API_CALL = "/api/v1/graphql";
    private static final String INTROSPECTION_OPERATION_NAME = "\"operationName\":\"IntrospectionQuery\"";
    /**
     * Matches a body that carries a mutation operation. Mutations are never cached nor replayed in
     * the background, because serving a stored response, or re-executing them on a refresh, would
     * skip or repeat a write. The check is conservative: any body that mentions the keyword is
     * treated as a mutation, which only costs it the cache. The body is JSON, so a line break
     * before the keyword arrives as the two characters <code>\n</code>; those escapes count as
     * separators too.
     */
    private static final Pattern MUTATION_OPERATION =
            Pattern.compile("(?:^|[^A-Za-z0-9_]|\\\\[nrt])mutation\\b");
    private static final int LOCK_STRIPES = 256;
    private static final Object[] LOCKS = new Object[LOCK_STRIPES];

    static {
        for (int i = 0; i < LOCK_STRIPES; i++) {
            LOCKS[i] = new Object();
        }
    }
    private static final String GRAPHQL_BYPASS_CACHE = "GRAPHQL_BYPASS_CACHE";
    private static final String GRAPHQL_CACHE_CREDENTIALED = "GRAPHQL_CACHE_CREDENTIALED";
    static final int INVALIDATE_CACHE = -1;

    private final GraphQLCache graphCache = CacheLocator.getGraphQLCache();

    private final Lazy<Boolean> ENABLED_FROM_CONFIG = Lazy.of(()->Config
                    .getBooleanProperty(GRAPHQL_CACHE_RESULTS_CONFIG_PROPERTY, true));

    private final DotGraphQLHttpServlet graphQLHttpServlet = new DotGraphQLHttpServlet();

    private final Lazy<Integer> defaultTTL = Lazy.of(()->Config.getIntProperty(GRAPHQL_CACHE_DEFAULT_TTL_PROPERTY, 15));

    private final Lazy<Integer> maxTTL = Lazy.of(()->Config.getIntProperty(GRAPHQL_CACHE_MAX_TTL_PROPERTY, GRAPHQL_CACHE_MAX_TTL_DEFAULT));

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

        final GraphqlCacheKey cacheKey = GraphqlCacheKey.from(requestIn, query.get(),
                getParamOrHeader(requestIn, GRAPHQL_CACHE_KEY_PARAM));
        final String syncKey = cacheKey.value();

        final int cacheTTL = getCacheTTL(requestIn);
        final boolean bypassCache = cacheTTL <= 0
                || query.get().contains(INTROSPECTION_OPERATION_NAME)
                || isMutation(query.get());

        Optional<String> graphResponseFromCache = bypassCache
                ? Optional.empty()
                : refreshKey(requestIn) && !cacheKey.isSessionScoped()
                        ? getAndRefresh(wrapper, syncKey, cacheTTL)
                        : graphCache.get(syncKey);

        if (graphResponseFromCache.isPresent()) {
            writeResponse(response, graphResponseFromCache.get());
            return Result.SKIP_NO_CHAIN;
        }

        synchronized (lockFor(syncKey)) {
            graphResponseFromCache = bypassCache ? Optional.empty() : graphCache.get(syncKey);

            if (graphResponseFromCache.isPresent()) {
                writeResponse(response, graphResponseFromCache.get());
                return Result.SKIP_NO_CHAIN;
            }

            wrapper.setAttribute(GRAPHQL_CACHE_KEY, syncKey);
            wrapper.setAttribute(GRAPHQL_BYPASS_CACHE, bypassCache);
            wrapper.setAttribute(GRAPHQL_CACHE_CREDENTIALED, cacheKey.isCredentialed());
            return new Result.Builder().wrap(new MockHttpWriterCaptureResponse(response))
                    .wrap(wrapper).next().build();
        }
    }

    /**
     * Picks the monitor that serializes the first execution for a key. Keys are spread over a fixed
     * set of stripes, so no per-key object has to be kept (or interned) for the lifetime of the JVM.
     *
     * @param key the cache key
     * @return the stripe to synchronize on
     */
    private static Object lockFor(final String key) {
        return LOCKS[Math.floorMod(key.hashCode(), LOCK_STRIPES)];
    }

    /**
     * Whether the request body carries a mutation operation.
     *
     * @param body the raw request body
     * @return true when the body mentions a mutation
     */
    static boolean isMutation(final String body) {
        return MUTATION_OPERATION.matcher(body).find();
    }

    private Optional<String> getAndRefresh(final HttpRequestReaderWrapper wrapper,
            final String syncKey, final int cacheTTL) {
        // copy request
        final DotCMSMockRequest requestCopy = getRequestCopy(wrapper);

        return graphCache.getAndRefresh(syncKey, ()
                        -> processGraphQLRequest(requestCopy),
                cacheTTL);
    }

    /**
     * Copies what the GraphQL servlet needs to replay the request on another thread: body,
     * parameters, attributes, headers and the addressing details. Cookies and the session are not
     * copied, which is why requests whose key depends on them are never refreshed in the background.
     *
     * @param wrapper the request being served
     * @return a detached copy of the request
     */
    private DotCMSMockRequest getRequestCopy(final HttpRequestReaderWrapper wrapper) {
        final DotCMSMockRequest requestCopy = new DotCMSMockRequest();
        requestCopy.setContent(wrapper.getRawRequest().get());
        requestCopy.setParameterMap(wrapper.getParameterMap());
        requestCopy.setMethod(wrapper.getMethod());
        requestCopy.setRequestURI(wrapper.getRequestURI());
        requestCopy.setRequestURL(wrapper.getRequestURL());
        requestCopy.setQueryString(wrapper.getQueryString());
        requestCopy.setServerName(wrapper.getServerName());
        requestCopy.setRemoteAddr(wrapper.getRemoteAddr());
        requestCopy.setRemoteHost(wrapper.getRemoteHost());

        final Enumeration<String> attributes = wrapper.getAttributeNames();
        if (attributes != null) {
            for (final String name : Collections.list(attributes)) {
                requestCopy.setAttribute(name, wrapper.getAttribute(name));
            }
        }

        final Enumeration<String> headers = wrapper.getHeaderNames();
        if (headers != null) {
            for (final String name : Collections.list(headers)) {
                final String value = wrapper.getHeader(name);
                if (name != null && value != null) {
                    requestCopy.addHeader(name, value);
                }
            }
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

    private boolean refreshKey(HttpServletRequest requestIn) {
        final Optional<String> backgroundRefreshParam = getParamOrHeader(requestIn,
                GRAPHQL_CACHE_REFRESH_PARAM);

        return backgroundRefreshParam.isPresent()
                && backgroundRefreshParam.get().equalsIgnoreCase("true");
    }

    /**
     * Resolves the TTL, in seconds, to apply to this request: the <code>dotcachettl</code>
     * parameter or header when it is a valid integer, otherwise the configured default. The
     * result is capped by {@link #GRAPHQL_CACHE_MAX_TTL_PROPERTY}.
     *
     * @param requestIn the request
     * @return the TTL; 0 or a negative value means the cache is bypassed
     */
    private int getCacheTTL(final HttpServletRequest requestIn) {
        final int requested = getParamOrHeader(requestIn, GRAPHQL_CACHE_TTL_PARAM)
                .flatMap(value -> Try.of(() -> Integer.parseInt(value.trim())).toJavaOptional())
                .orElseGet(defaultTTL::get);

        return capTTL(requested, maxTTL.get());
    }

    /**
     * Caps a requested TTL to the configured maximum. Zero and negative values keep their special
     * meaning and are returned unchanged. A non-positive maximum disables the cap.
     *
     * @param requested the TTL the caller asked for, or the default
     * @param max the configured maximum
     * @return the TTL to apply
     */
    static int capTTL(final int requested, final int max) {
        if (requested <= 0 || max <= 0) {
            return requested;
        }
        return Math.min(requested, max);
    }

    @Override
    public boolean afterIntercept(final HttpServletRequest request,
            final HttpServletResponse response) {
        final String cacheKey = (String) request.getAttribute(GRAPHQL_CACHE_KEY);

        if(response.getStatus() == HttpServletResponse.SC_OK && response instanceof MockHttpWriterCaptureResponse
                && UtilMethods.isSet(cacheKey)) {
            final MockHttpWriterCaptureResponse mockResponse = (MockHttpWriterCaptureResponse) response;
            response.setHeader("x-graphql-cache", "miss, writing to cache");
            final String graphqlResponse = mockResponse.writer.toString();

            Try.run(() -> mockResponse.originalResponse.getWriter().write(graphqlResponse));

            final boolean bypassCache = Boolean.TRUE.equals(request.getAttribute(GRAPHQL_BYPASS_CACHE));
            final boolean credentialed = Boolean.TRUE.equals(request.getAttribute(GRAPHQL_CACHE_CREDENTIALED));
            final int cacheTTL = getCacheTTL(request);

            if(!bypassCache && hasData(graphqlResponse)) {
                graphCache.put(cacheKey, graphqlResponse, cacheTTL);
            }

            if(cacheTTL == INVALIDATE_CACHE && credentialed) {
                graphCache.remove(cacheKey);
            }
        } else if(response instanceof MockHttpWriterCaptureResponse) {
            final MockHttpWriterCaptureResponse mockResponse = (MockHttpWriterCaptureResponse) response;
            final String graphqlResponse = mockResponse.writer.toString();
            Try.run(() -> mockResponse.originalResponse.getWriter().write(graphqlResponse));
        }
        return true;
    }

    /**
     * Whether a GraphQL response body carries a non-null <code>data</code> member. Only such
     * responses are worth caching.
     *
     * @param graphqlResponse the response body
     * @return true when the body is a JSON object with data
     */
    private static boolean hasData(final String graphqlResponse) {
        final Map<String,Object> map = Try.of(()->new ObjectMapper()
                .readValue(graphqlResponse, Map.class)).getOrNull();
        return map != null && map.get("data") != null;
    }

    /**
     * Replays the request against the GraphQL servlet on a detached response and returns the
     * body, or an empty string when the execution did not succeed, so that the cache keeps the
     * previous entry instead of storing an error.
     *
     * @param request the request copy to replay
     * @return the response body, or empty when it must not be cached
     */
    private String processGraphQLRequest(final HttpServletRequest request)  {
        try {
            graphQLHttpServlet.init();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        final MockHttpWriterCaptureResponse mockHttpResponse = new MockHttpWriterCaptureResponse(
                new MockHttpStatusResponse(new DotCMSMockResponse()));
        graphQLHttpServlet.doPost(request, mockHttpResponse);
        final String graphqlResponse = mockHttpResponse.writer.toString();
        if (mockHttpResponse.getStatus() != HttpServletResponse.SC_OK || !hasData(graphqlResponse)) {
            Logger.debug(this, () -> "Background refresh did not produce a cacheable response, keeping the current entry");
            return "";
        }
        return graphqlResponse;
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
