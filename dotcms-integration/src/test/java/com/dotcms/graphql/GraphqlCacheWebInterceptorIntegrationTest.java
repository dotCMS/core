package com.dotcms.graphql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.LicenseTestUtil;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.mock.request.DotCMSMockRequest;
import com.dotcms.mock.request.DotCMSMockRequestWithSession;
import com.dotcms.mock.request.MockSession;
import com.dotcms.mock.response.DotCMSMockResponse;
import com.dotcms.mock.response.MockHttpStatusResponse;
import com.dotcms.mock.response.MockHttpWriterCaptureResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.util.WebKeys;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Drives {@link GraphqlCacheWebInterceptor} the way the interceptor chain does: {@code intercept}
 * first, then the servlet (simulated here by writing a body), then {@code afterIntercept}. The
 * servlet body is unique per call, so a response that comes back from a previous call proves a
 * cache hit.
 */
public class GraphqlCacheWebInterceptorIntegrationTest {

    private static final String TOKEN_A = "Bearer " + UUID.randomUUID();
    private static final String TOKEN_B = "Bearer " + UUID.randomUUID();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    @After
    public void restoreConfig() {
        Config.setProperty(GraphqlCacheWebInterceptor.GRAPHQL_CACHE_MAX_TTL_PROPERTY,
                GraphqlCacheWebInterceptor.GRAPHQL_CACHE_MAX_TTL_DEFAULT);
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#intercept} and {@link GraphqlCacheWebInterceptor#afterIntercept}
     * Given Scenario: a caller with a credential fills the cache, then an anonymous caller sends the same body
     * ExpectedResult: the anonymous caller is not served the credentialed response
     */
    @Test
    public void anonymousCallerDoesNotGetACredentialedEntry() throws IOException {
        final String body = uniqueBody();

        final Served credentialed = serve(anonymousRequest(body).withHeader("Authorization", TOKEN_A));
        final Served anonymous = serve(anonymousRequest(body));

        assertFalse(credentialed.hit);
        assertFalse(anonymous.hit);
        assertFalse(anonymous.body.equals(credentialed.body));
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#intercept} and {@link GraphqlCacheWebInterceptor#afterIntercept}
     * Given Scenario: an anonymous caller fills the cache, then a credentialed caller sends the same body
     * ExpectedResult: the credentialed caller gets a fresh execution, and each side keeps its own entry
     */
    @Test
    public void callersKeepTheirOwnEntries() throws IOException {
        final String body = uniqueBody();

        final Served anonymousFirst = serve(anonymousRequest(body));
        final Served credentialedFirst = serve(anonymousRequest(body).withHeader("Authorization", TOKEN_A));
        final Served anonymousAgain = serve(anonymousRequest(body));
        final Served credentialedAgain = serve(anonymousRequest(body).withHeader("Authorization", TOKEN_A));

        assertFalse(anonymousFirst.hit);
        assertFalse(credentialedFirst.hit);
        assertTrue(anonymousAgain.hit);
        assertEquals(anonymousFirst.body, anonymousAgain.body);
        assertTrue(credentialedAgain.hit);
        assertEquals(credentialedFirst.body, credentialedAgain.body);
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#intercept}
     * Given Scenario: two different tokens send the same body
     * ExpectedResult: the second token is not served the first token's entry
     */
    @Test
    public void differentTokensDoNotShareEntries() throws IOException {
        final String body = uniqueBody();

        final Served tokenA = serve(anonymousRequest(body).withHeader("Authorization", TOKEN_A));
        final Served tokenB = serve(anonymousRequest(body).withHeader("Authorization", TOKEN_B));

        assertFalse(tokenA.hit);
        assertFalse(tokenB.hit);
        assertFalse(tokenA.body.equals(tokenB.body));
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#intercept}
     * Given Scenario: the same anonymous body addressed to two sites and two languages
     * ExpectedResult: each site and language combination gets its own entry
     */
    @Test
    public void siteAndLanguageGetTheirOwnEntries() throws IOException {
        final String body = uniqueBody();

        final Served siteA = serve(anonymousRequest(body).withParam("Host", "site-a"));
        final Served siteB = serve(anonymousRequest(body).withParam("Host", "site-b"));
        final Served siteALangTwo = serve(anonymousRequest(body).withParam("Host", "site-a").withParam("language_id", "2"));
        final Served otherServerName = serve(anonymousRequest(body).withServerName("other.dotcms.com"));
        final Served siteAAgain = serve(anonymousRequest(body).withParam("Host", "site-a"));

        assertFalse(siteA.hit);
        assertFalse(siteB.hit);
        assertFalse(siteALangTwo.hit);
        assertFalse(otherServerName.hit);
        assertTrue(siteAAgain.hit);
        assertEquals(siteA.body, siteAAgain.body);
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#intercept}
     * Given Scenario: a session with a logged-in user, then an anonymous request with the same body
     * ExpectedResult: the anonymous request is not served the session user's entry
     */
    @Test
    public void sessionUserEntryIsNotServedAnonymously() throws IOException {
        final String body = uniqueBody();
        final MockSession session = new MockSession(UUID.randomUUID().toString());
        session.setAttribute(WebKeys.USER_ID, "dotcms.org.1");

        final Served sessionUser = serve(new RequestBuilder(body, session));
        final Served anonymous = serve(anonymousRequest(body));

        assertFalse(sessionUser.hit);
        assertFalse(anonymous.hit);
        assertFalse(anonymous.body.equals(sessionUser.body));
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#intercept}
     * Given Scenario: a logged-in session sends a body, the servlet stores the default language in
     * the session while serving it, and the same body is sent again
     * ExpectedResult: the second request is a hit, since the stored default language changes nothing
     */
    @Test
    public void defaultLanguageStoredInSessionKeepsTheEntry() throws IOException {
        final String body = uniqueBody();
        final MockSession session = new MockSession(UUID.randomUUID().toString());
        session.setAttribute(WebKeys.USER_ID, "dotcms.org.1");

        final Served first = serve(new RequestBuilder(body, session));
        session.setAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE,
                String.valueOf(APILocator.getLanguageAPI().getDefaultLanguage().getId()));
        final Served second = serve(new RequestBuilder(body, session));
        session.setAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE,
                String.valueOf(APILocator.getLanguageAPI().getDefaultLanguage().getId() + 1));
        final Served otherLanguage = serve(new RequestBuilder(body, session));

        assertFalse(first.hit);
        assertTrue(second.hit);
        assertEquals(first.body, second.body);
        assertFalse(otherLanguage.hit);
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#afterIntercept}
     * Given Scenario: a TTL above the configured maximum is requested
     * ExpectedResult: the entry expires at the maximum, not at the requested TTL
     */
    @Test
    public void requestedTTLIsCapped() throws Exception {
        Config.setProperty(GraphqlCacheWebInterceptor.GRAPHQL_CACHE_MAX_TTL_PROPERTY, 1);
        final String body = uniqueBody();

        final Served filled = serve(anonymousRequest(body).withParam("dotcachettl", "999999"));
        final Served withinCap = serve(anonymousRequest(body));
        Thread.sleep(1500);
        final Served afterCap = serve(anonymousRequest(body));

        assertFalse(filled.hit);
        assertTrue(withinCap.hit);
        assertFalse(afterCap.hit);
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#afterIntercept}
     * Given Scenario: an anonymous caller sends -1 for an entry anonymous callers share
     * ExpectedResult: the request bypasses the cache but the entry survives
     */
    @Test
    public void anonymousCallerCannotClearEntries() throws IOException {
        final String body = uniqueBody();

        final Served filled = serve(anonymousRequest(body));
        final Served invalidate = serve(anonymousRequest(body).withParam("dotcachettl", "-1"));
        final Served afterInvalidate = serve(anonymousRequest(body));

        assertFalse(filled.hit);
        assertFalse(invalidate.hit);
        assertTrue(afterInvalidate.hit);
        assertEquals(filled.body, afterInvalidate.body);
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#afterIntercept}
     * Given Scenario: a credentialed caller sends -1 for its own entry
     * ExpectedResult: the entry is cleared and the next request executes again
     */
    @Test
    public void credentialedCallerClearsItsOwnEntry() throws IOException {
        final String body = uniqueBody();

        final Served filled = serve(anonymousRequest(body).withHeader("Authorization", TOKEN_A));
        final Served invalidate = serve(anonymousRequest(body).withHeader("Authorization", TOKEN_A).withParam("dotcachettl", "-1"));
        final Served afterInvalidate = serve(anonymousRequest(body).withHeader("Authorization", TOKEN_A));

        assertFalse(filled.hit);
        assertFalse(invalidate.hit);
        assertFalse(afterInvalidate.hit);
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#afterIntercept}
     * Given Scenario: a request carries a credential the servlet does not accept, so it is served
     * as anonymous
     * ExpectedResult: nothing is stored under that credential, and the anonymous entry is untouched
     */
    @Test
    public void unresolvedCredentialIsNotCached() throws IOException {
        final String body = uniqueBody();

        final Served anonymous = serve(anonymousRequest(body));
        final Served garbageFirst = serve(anonymousRequest(body)
                .withHeader("Authorization", "Garbage " + UUID.randomUUID())
                .resolvedAs(UserAPI.CMS_ANON_USER_ID));
        final Served garbageAgain = serve(anonymousRequest(body)
                .withHeader("Authorization", "Garbage " + UUID.randomUUID())
                .resolvedAs(UserAPI.CMS_ANON_USER_ID));
        final Served anonymousAgain = serve(anonymousRequest(body));

        assertFalse(anonymous.hit);
        assertFalse(garbageFirst.hit);
        assertFalse(garbageAgain.hit);
        assertTrue(anonymousAgain.hit);
        assertEquals(anonymous.body, anonymousAgain.body);
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#afterIntercept}
     * Given Scenario: the servlet answers without a data member
     * ExpectedResult: nothing is cached
     */
    @Test
    public void responsesWithoutDataAreNotCached() throws IOException {
        final String body = uniqueBody();

        serve(anonymousRequest(body), "{\"errors\":[{\"message\":\"nope\"}]}");
        final Served again = serve(anonymousRequest(body));

        assertFalse(again.hit);
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#intercept}
     * Given Scenario: the same mutation body is sent twice by the same caller
     * ExpectedResult: neither request is served from cache
     */
    @Test
    public void mutationsAreNeverCached() throws IOException {
        final String body = "{\"query\":\"mutation { publish(id: \\\"" + UUID.randomUUID() + "\\\") { title } }\"}";

        final Served first = serve(anonymousRequest(body).withHeader("Authorization", TOKEN_A));
        final Served second = serve(anonymousRequest(body).withHeader("Authorization", TOKEN_A));

        assertFalse(first.hit);
        assertFalse(second.hit);
    }

    private static String uniqueBody() {
        return "{\"query\":\"{ Collection_" + UUID.randomUUID().toString().replace("-", "") + " { title } }\"}";
    }

    private static RequestBuilder anonymousRequest(final String body) {
        return new RequestBuilder(body, null);
    }

    private static Served serve(final RequestBuilder builder) throws IOException {
        return serve(builder, "{\"data\":{\"result\":\"" + UUID.randomUUID() + "\"}}");
    }

    /**
     * Runs one request through the interceptor. On a hit the cached body is written straight to
     * the response. On a miss the servlet is simulated by leaving the resolved user id on the
     * request and writing {@code servletBody} with a 200 status, after which
     * {@code afterIntercept} stores it.
     */
    private static Served serve(final RequestBuilder builder, final String servletBody) throws IOException {
        final GraphqlCacheWebInterceptor interceptor = new GraphqlCacheWebInterceptor();
        final MockHttpWriterCaptureResponse liveResponse = new MockHttpWriterCaptureResponse(
                new MockHttpStatusResponse(new DotCMSMockResponse()));

        final Result result = interceptor.intercept(builder.build(), liveResponse);
        if (result == Result.SKIP_NO_CHAIN) {
            return new Served(true, liveResponse.writer.toString());
        }

        result.getRequest().setAttribute(WebKeys.USER_ID, builder.resolvedUserId());
        final HttpServletResponse wrapped = result.getResponse();
        wrapped.setStatus(HttpServletResponse.SC_OK);
        wrapped.getWriter().write(servletBody);
        interceptor.afterIntercept(result.getRequest(), wrapped);
        return new Served(false, liveResponse.writer.toString());
    }

    private static final class Served {
        final boolean hit;
        final String body;

        Served(final boolean hit, final String body) {
            this.hit = hit;
            this.body = body;
        }
    }

    private static final class RequestBuilder {
        private final String body;
        private final MockSession session;
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, String[]> params = new HashMap<>();
        private String serverName = "demo.dotcms.com";
        private String resolvedUserId;

        RequestBuilder(final String body, final MockSession session) {
            this.body = body;
            this.session = session;
        }

        /**
         * The user the simulated servlet resolves: by default a real user when the request carries
         * a credential, the anonymous user otherwise.
         */
        RequestBuilder resolvedAs(final String userId) {
            this.resolvedUserId = userId;
            return this;
        }

        String resolvedUserId() {
            if (resolvedUserId != null) {
                return resolvedUserId;
            }
            return headers.containsKey("Authorization") || session != null
                    ? "dotcms.org.1" : UserAPI.CMS_ANON_USER_ID;
        }

        RequestBuilder withHeader(final String name, final String value) {
            headers.put(name, value);
            return this;
        }

        RequestBuilder withParam(final String name, final String value) {
            params.put(name, new String[] {value});
            return this;
        }

        RequestBuilder withServerName(final String serverName) {
            this.serverName = serverName;
            return this;
        }

        DotCMSMockRequest build() {
            final DotCMSMockRequest request = session == null
                    ? new DotCMSMockRequest()
                    : new DotCMSMockRequestWithSession(session, false);
            request.setMethod("POST");
            request.setRequestURI("/api/v1/graphql");
            request.setServerName(serverName);
            request.setRemoteAddr("127.0.0.1");
            request.setContent(body);
            request.setParameterMap(params);
            headers.forEach(request::addHeader);
            return request;
        }
    }
}
