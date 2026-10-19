package com.dotcms.graphql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.util.CookieKeys;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.Test;

/**
 * Tests for {@link GraphqlCacheKey}: the key must change whenever the caller or the request
 * context changes, and must stay the same for repeated requests from the same caller.
 */
public class GraphqlCacheKeyTest extends UnitTestBase {

    private static final String BODY = "{\"query\":\"{ BlogCollection { title } }\"}";
    private static final String TOKEN_A = "Bearer aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String TOKEN_B = "Bearer bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    private static HttpServletRequest request() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn("demo.dotcms.com");
        when(request.getParameterMap()).thenReturn(new HashMap<>());
        when(request.getCookies()).thenReturn(new Cookie[0]);
        when(request.getSession(false)).thenReturn(null);
        return request;
    }

    private static GraphqlCacheKey key(final HttpServletRequest request) {
        return GraphqlCacheKey.from(request, BODY, Optional.empty());
    }

    /**
     * Method to test: {@link GraphqlCacheKey#from(HttpServletRequest, String, Optional)}
     * Given Scenario: two anonymous requests with the same body and context
     * ExpectedResult: both get the same key, with no credential and no session dependency
     */
    @Test
    public void anonymousRequestsShareTheKey() {
        final GraphqlCacheKey first = key(request());
        final GraphqlCacheKey second = key(request());

        assertEquals(first.value(), second.value());
        assertFalse(first.isCredentialed());
        assertFalse(first.isSessionScoped());
        assertTrue(first.value().matches("[0-9a-f]{64}"));
    }

    /**
     * Method to test: {@link GraphqlCacheKey#from(HttpServletRequest, String, Optional)}
     * Given Scenario: the same body sent anonymously and with an Authorization header
     * ExpectedResult: the keys differ and the header value is not part of the key
     */
    @Test
    public void authorizationHeaderChangesTheKey() {
        final HttpServletRequest withToken = request();
        when(withToken.getHeader("Authorization")).thenReturn(TOKEN_A);

        final GraphqlCacheKey anonymous = key(request());
        final GraphqlCacheKey credentialed = key(withToken);

        assertNotEquals(anonymous.value(), credentialed.value());
        assertTrue(credentialed.isCredentialed());
        assertFalse(credentialed.isSessionScoped());
        assertFalse(credentialed.value().contains("aaaa"));
    }

    /**
     * Method to test: {@link GraphqlCacheKey#from(HttpServletRequest, String, Optional)}
     * Given Scenario: the same body sent with two different tokens, then the first one again
     * ExpectedResult: different tokens get different keys and the same token reuses its key
     */
    @Test
    public void differentTokensNeverShareTheKey() {
        final HttpServletRequest tokenA = request();
        when(tokenA.getHeader("Authorization")).thenReturn(TOKEN_A);
        final HttpServletRequest tokenB = request();
        when(tokenB.getHeader("Authorization")).thenReturn(TOKEN_B);
        final HttpServletRequest tokenAAgain = request();
        when(tokenAAgain.getHeader("Authorization")).thenReturn(TOKEN_A);

        assertNotEquals(key(tokenA).value(), key(tokenB).value());
        assertEquals(key(tokenA).value(), key(tokenAAgain).value());
    }

    /**
     * Method to test: {@link GraphqlCacheKey#from(HttpServletRequest, String, Optional)}
     * Given Scenario: a DOTAUTH header is sent instead of Authorization
     * ExpectedResult: the request counts as credentialed and gets its own key
     */
    @Test
    public void dotAuthHeaderChangesTheKey() {
        final HttpServletRequest withDotAuth = request();
        when(withDotAuth.getHeader("DOTAUTH")).thenReturn("dXNlcjpwYXNz");

        final GraphqlCacheKey credentialed = key(withDotAuth);

        assertNotEquals(key(request()).value(), credentialed.value());
        assertTrue(credentialed.isCredentialed());
    }

    /**
     * Method to test: {@link GraphqlCacheKey#from(HttpServletRequest, String, Optional)}
     * Given Scenario: the remember-me cookie is present
     * ExpectedResult: the key differs from the anonymous one and cannot be refreshed in the background
     */
    @Test
    public void rememberMeCookieChangesTheKey() {
        final HttpServletRequest withCookie = request();
        when(withCookie.getCookies()).thenReturn(
                new Cookie[] {new Cookie(CookieKeys.JWT_ACCESS_TOKEN, "cookie-token")});

        final GraphqlCacheKey credentialed = key(withCookie);

        assertNotEquals(key(request()).value(), credentialed.value());
        assertTrue(credentialed.isCredentialed());
        assertTrue(credentialed.isSessionScoped());
    }

    /**
     * Method to test: {@link GraphqlCacheKey#from(HttpServletRequest, String, Optional)}
     * Given Scenario: a session with a logged-in user, then another session for the same user
     * ExpectedResult: the user gets a key distinct from anonymous, shared across their sessions
     */
    @Test
    public void sessionUserChangesTheKey() {
        final GraphqlCacheKey firstSession = key(requestWithSessionUser("dotcms.org.1"));
        final GraphqlCacheKey secondSession = key(requestWithSessionUser("dotcms.org.1"));
        final GraphqlCacheKey otherUser = key(requestWithSessionUser("dotcms.org.2"));

        assertNotEquals(key(request()).value(), firstSession.value());
        assertEquals(firstSession.value(), secondSession.value());
        assertNotEquals(firstSession.value(), otherUser.value());
        assertTrue(firstSession.isCredentialed());
        assertTrue(firstSession.isSessionScoped());
    }

    /**
     * Method to test: {@link GraphqlCacheKey#from(HttpServletRequest, String, Optional)}
     * Given Scenario: the same body addressed to two server names
     * ExpectedResult: the keys differ
     */
    @Test
    public void serverNameChangesTheKey() {
        final HttpServletRequest otherSite = request();
        when(otherSite.getServerName()).thenReturn("other.dotcms.com");

        assertNotEquals(key(request()).value(), key(otherSite).value());
    }

    /**
     * Method to test: {@link GraphqlCacheKey#from(HttpServletRequest, String, Optional)}
     * Given Scenario: the same body with different Host and language_id parameters
     * ExpectedResult: each combination gets its own key, regardless of parameter order
     */
    @Test
    public void requestParametersChangeTheKey() {
        final GraphqlCacheKey siteA = key(requestWithParams(Map.of("Host", "site-a")));
        final GraphqlCacheKey siteB = key(requestWithParams(Map.of("Host", "site-b")));
        final GraphqlCacheKey siteALangTwo = key(requestWithParams(
                orderedParams("Host", "site-a", "language_id", "2")));
        final GraphqlCacheKey langTwoSiteA = key(requestWithParams(
                orderedParams("language_id", "2", "Host", "site-a")));

        assertNotEquals(key(request()).value(), siteA.value());
        assertNotEquals(siteA.value(), siteB.value());
        assertNotEquals(siteA.value(), siteALangTwo.value());
        assertEquals(siteALangTwo.value(), langTwoSiteA.value());
    }

    /**
     * Method to test: {@link GraphqlCacheKey#from(HttpServletRequest, String, Optional)}
     * Given Scenario: the cache control parameters are sent with different values
     * ExpectedResult: they do not partition the cache, while a client key does
     */
    @Test
    public void cacheControlParametersDoNotChangeTheKey() {
        final GraphqlCacheKey plain = key(request());
        final GraphqlCacheKey withControls = key(requestWithParams(
                orderedParams("dotcachettl", "600", "DotCacheRefresh", "true")));
        final GraphqlCacheKey withClientKey = GraphqlCacheKey.from(request(), BODY,
                Optional.of("home"));

        assertEquals(plain.value(), withControls.value());
        assertNotEquals(plain.value(), withClientKey.value());
    }

    /**
     * Method to test: {@link GraphqlCacheKey#from(HttpServletRequest, String, Optional)}
     * Given Scenario: an anonymous session carrying a selected language and a persona
     * ExpectedResult: the key differs from a bare anonymous request and depends on the session
     */
    @Test
    public void sessionLanguageAndPersonaChangeTheKey() {
        final HttpSession languageSession = mock(HttpSession.class);
        when(languageSession.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn("2");
        final HttpServletRequest withLanguage = request();
        when(withLanguage.getSession(false)).thenReturn(languageSession);

        final Persona persona = mock(Persona.class);
        when(persona.getKeyTag()).thenReturn("firstTimeVisitor");
        final Visitor visitor = mock(Visitor.class);
        when(visitor.getPersona()).thenReturn(persona);
        final HttpSession personaSession = mock(HttpSession.class);
        when(personaSession.getAttribute(WebKeys.VISITOR)).thenReturn(visitor);
        final HttpServletRequest withPersona = request();
        when(withPersona.getSession(false)).thenReturn(personaSession);

        final HttpServletRequest emptySessionRequest = request();
        when(emptySessionRequest.getSession(false)).thenReturn(mock(HttpSession.class));

        final GraphqlCacheKey anonymous = key(request());
        final GraphqlCacheKey emptySession = key(emptySessionRequest);
        final GraphqlCacheKey language = key(withLanguage);
        final GraphqlCacheKey personalized = key(withPersona);

        assertEquals(anonymous.value(), emptySession.value());
        assertFalse(emptySession.isSessionScoped());
        assertNotEquals(anonymous.value(), language.value());
        assertNotEquals(anonymous.value(), personalized.value());
        assertNotEquals(language.value(), personalized.value());
        assertTrue(language.isSessionScoped());
        assertTrue(personalized.isSessionScoped());
        assertFalse(language.isCredentialed());
    }

    /**
     * Method to test: {@link GraphqlCacheKey#from(HttpServletRequest, String, Optional)}
     * Given Scenario: two different bodies from the same anonymous caller
     * ExpectedResult: the keys differ
     */
    @Test
    public void bodyChangesTheKey() {
        final GraphqlCacheKey first = GraphqlCacheKey.from(request(), BODY, Optional.empty());
        final GraphqlCacheKey second = GraphqlCacheKey.from(request(),
                "{\"query\":\"{ NewsCollection { title } }\"}", Optional.empty());

        assertNotEquals(first.value(), second.value());
    }

    private static HttpServletRequest requestWithSessionUser(final String userId) {
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID)).thenReturn(userId);
        final HttpServletRequest request = request();
        when(request.getSession(false)).thenReturn(session);
        return request;
    }

    private static HttpServletRequest requestWithParams(final Map<String, String> params) {
        final Map<String, String[]> parameterMap = new LinkedHashMap<>();
        params.forEach((name, value) -> parameterMap.put(name, new String[] {value}));
        final HttpServletRequest request = request();
        when(request.getParameterMap()).thenReturn(parameterMap);
        return request;
    }

    private static Map<String, String> orderedParams(final String... namesAndValues) {
        final Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            params.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return params;
    }
}
