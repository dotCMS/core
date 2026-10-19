package com.dotcms.rest.api.v1.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.RestClientBuilder;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.util.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Contract test for {@link ApiTokenResource#getRemoteToken}. It pins the secure remote-issuance
 * sequence: a JSON login against {@code /api/v1/authentication} yields a {@code JSESSIONID}
 * session cookie that is carried to {@code /api/v1/apitoken}, and no Basic credential header is
 * ever sent on the wire. The admin gate, status pass-through and client-close contract are
 * retained.
 *
 * <p>Each remote request is matched by its <em>full</em> URL ({@value #BASE_URL} plus the exact
 * path), so a request sent to a different host, a different port or the wrong path fails instead
 * of matching a suffix. Cookies are recorded per request, so a session cookie can only satisfy
 * the token request it was forwarded on.</p>
 *
 * <p>Limitations: the mocked client cannot observe real network redirects; the tests only prove
 * that the issuer disables client redirects, never sends Basic credentials, and does not issue a
 * token without a login session cookie.</p>
 */
public class RemoteApiTokenAuthenticationTest extends UnitTestBase {

    private static final String ENABLE_PROXY_TOKEN_REQUESTS = "ENABLE_PROXY_TOKEN_REQUESTS";
    private static final String BASE_URL = "http://receiver.local:8090";
    private static final String LOGIN_PATH = "/api/v1/authentication";
    private static final String TOKEN_PATH = "/api/v1/apitoken";
    private static final String LOGIN_URL = BASE_URL + LOGIN_PATH;
    private static final String TOKEN_URL = BASE_URL + TOKEN_PATH;
    private static final String SESSION_COOKIE = "JSESSIONID";

    private String previousGate;

    /** Preserves the proxy-token gate so failures cannot leak configuration into other tests. */
    @Before
    public void saveGate() {
        previousGate = Config.getStringProperty(ENABLE_PROXY_TOKEN_REQUESTS, null);
        Config.setProperty(ENABLE_PROXY_TOKEN_REQUESTS, "true");
    }

    /** Restores the proxy-token gate after each test. */
    @After
    public void restoreGate() {
        Config.setProperty(ENABLE_PROXY_TOKEN_REQUESTS, previousGate);
    }

    /** A successful login cookie must be forwarded to token issuance without any Basic header. */
    @Test
    public void remoteToken_usesSessionLoginCookie_andNeverSendsBasic() throws Exception {
        final RemoteCall remote = new RemoteCall();
        remote.loginSucceeds();
        remote.tokenSucceeds();
        final RemoteAPITokenForm form = remoteForm();

        final Response result = invokeRemote(remote, remote.admin(), form);

        remote.assertNeverSentBasic();
        remote.assertSameRemoteBase();
        remote.assertLoginRequest();
        remote.assertTokenRequest(form.getTokenInfo());
        remote.assertOnlySessionCookieForwarded();
        remote.assertRedirectsDisabled();
        remote.assertResponsesClosed();
        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertEquals(remote.tokenBody(), result.getEntity());
        verify(remote.builder, times(2)).post(any());
        verify(remote.client, times(1)).close();
    }

    /** A rejected login must not fall through to issuing a remote token. */
    @Test
    public void remoteToken_loginFailure_neverIssuesToken() throws Exception {
        final RemoteCall remote = new RemoteCall();
        remote.loginFailsUnauthorized();
        remote.tokenSucceeds();

        final Response result = invokeRemote(remote, remote.admin(), remoteForm());

        remote.assertNeverSentBasic();
        remote.assertLoginRequest();
        remote.assertTokenNeverRequested();
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), result.getStatus());
        assertEquals(remote.loginBody(), result.getEntity());
        remote.assertLoginResponseClosed();
        verify(remote.builder, times(1)).post(any());
        verify(remote.client, times(1)).close();
    }

    /** A login that returns no session cookie must not fall through to issuing a remote token. */
    @Test
    public void remoteToken_missingSessionCookie_neverIssuesToken() throws Exception {
        final RemoteCall remote = new RemoteCall();
        remote.loginSucceedsWithoutCookie();
        remote.tokenSucceeds();

        final Response result = invokeRemote(remote, remote.admin(), remoteForm());

        remote.assertNeverSentBasic();
        remote.assertLoginRequest();
        remote.assertTokenNeverRequested();
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), result.getStatus());
        assertEquals(remote.loginBody(), result.getEntity());
        remote.assertLoginResponseClosed();
        verify(remote.builder, times(1)).post(any());
        verify(remote.client, times(1)).close();
    }

    /** A remote token failure must pass the status and body through and still close every resource. */
    @Test
    public void remoteToken_tokenFailure_passesStatusAndBodyThrough() throws Exception {
        final RemoteCall remote = new RemoteCall();
        remote.loginSucceeds();
        remote.tokenFails(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "{\"error\":\"token issue failed\"}");

        final Response result = invokeRemote(remote, remote.admin(), remoteForm());

        remote.assertNeverSentBasic();
        remote.assertSameRemoteBase();
        remote.assertLoginRequest();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), result.getStatus());
        assertEquals(remote.tokenBody(), result.getEntity());
        remote.assertResponsesClosed();
        verify(remote.builder, times(2)).post(any());
        verify(remote.client, times(1)).close();
    }

    /** An unresolvable remote host must be reported as 404 without leaking the exception. */
    @Test
    public void remoteToken_unreachableHost_returnsNotFound() throws Exception {
        final RemoteCall remote = new RemoteCall();
        remote.loginPostFails(new ProcessingException(new UnknownHostException("receiver.local")));

        final Response result = invokeRemote(remote, remote.admin(), remoteForm());

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), result.getStatus());
        remote.assertNeverSentBasic();
        remote.assertLoginAttempted();
        remote.assertTokenNeverRequested();
        verify(remote.client, times(1)).close();
    }

    /** An unexpected client failure must be rethrown, and the acquired login response closed. */
    @Test
    public void remoteToken_unexpectedFailure_rethrowsAndClosesAcquiredResponses() throws Exception {
        final RemoteCall remote = new RemoteCall();
        remote.loginSucceeds();
        remote.tokenPostFails(new ProcessingException(new IOException("broken pipe")));

        assertThrows(ProcessingException.class,
                () -> invokeRemote(remote, remote.admin(), remoteForm()));

        remote.assertLoginResponseClosed();
        verify(remote.client, times(1)).close();
    }

    /** The admin gate must survive the migration; a non-admin must never touch the remote host. */
    @Test
    public void remoteToken_nonAdmin_isForbiddenWithoutRemoteCalls() {
        final RemoteCall remote = new RemoteCall();

        assertThrows(ForbiddenException.class, () -> {
            try (MockedStatic<RestClientBuilder> builder = mockStatic(RestClientBuilder.class)) {
                builder.when(RestClientBuilder::newClient).thenReturn(remote.client);
                newApiTokenResource(remote.nonAdmin())
                        .getRemoteToken(mock(HttpServletRequest.class), remoteForm());
            }
        });

        verifyNoInteractions(remote.client);
    }

    /** The disabled proxy-token gate must reject before the remote host is ever contacted. */
    @Test
    public void remoteToken_proxyGateDisabled_isForbiddenWithoutRemoteCalls() throws Exception {
        Config.setProperty(ENABLE_PROXY_TOKEN_REQUESTS, "false");
        final RemoteCall remote = new RemoteCall();

        assertThrows(ForbiddenException.class, () -> {
            try (MockedStatic<RestClientBuilder> builder = mockStatic(RestClientBuilder.class)) {
                builder.when(RestClientBuilder::newClient).thenReturn(remote.client);
                newApiTokenResource(remote.admin())
                        .getRemoteToken(mock(HttpServletRequest.class), remoteForm());
            }
        });

        verifyNoInteractions(remote.client);
    }

    /**
     * Wires the scoped {@link RestClientBuilder} static mock to the recording client and runs the
     * remote-token call. The mock lives only for this call, so no static state leaks to other
     * tests.
     */
    private Response invokeRemote(final RemoteCall remote, final User user,
            final RemoteAPITokenForm form) throws Exception {
        try (MockedStatic<RestClientBuilder> builder = mockStatic(RestClientBuilder.class)) {
            builder.when(RestClientBuilder::newClient).thenReturn(remote.client);
            return newApiTokenResource(user).getRemoteToken(mock(HttpServletRequest.class), form);
        }
    }

    private ApiTokenResource newApiTokenResource(final User user) {
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = new InitDataObject();
        initDataObject.setUser(user);
        when(webResource.init(any(), anyBoolean(), any(), anyBoolean(), any())).thenReturn(initDataObject);

        return new ApiTokenResource(mock(ApiTokenAPI.class), webResource);
    }

    private static RemoteAPITokenForm remoteForm() throws Exception {
        final String password = Base64.getEncoder()
                .encodeToString("password".getBytes(StandardCharsets.UTF_8));
        final String json = "{\"token\":{\"network\":\"0.0.0.0/0\",\"expirationSeconds\":1000,"
                + "\"claims\":{\"label\":\"Remote token\"}},"
                + "\"remote\":{\"host\":\"receiver.local\",\"port\":8090,\"protocol\":\"http\"},"
                + "\"auth\":{\"login\":\"admin@dotcms.com\",\"password\":\"" + password + "\"}}";
        return new ObjectMapper().readValue(json, RemoteAPITokenForm.class);
    }

    /**
     * Mocks the Jersey client graph with a single recording builder. Every request is recorded with
     * the exact route it targeted and the cookies carried on that route, so a request to the wrong
     * host or a cookie attached to the wrong request fails instead of passing by suffix match.
     */
    private static final class RemoteCall {

        private final Client client = mock(Client.class);
        private final Invocation.Builder builder = mock(Invocation.Builder.class);
        private final Response tokenResponse = mock(Response.class);
        private final List<RecordedRequest> requests = new ArrayList<>();
        private final List<String> attemptedRoutes = new ArrayList<>();
        private final Map<String, List<Cookie>> cookiesByRoute = new HashMap<>();
        private Response authResponse = mock(Response.class);
        private String pendingRoute;
        private String loginBody = "{}";
        private String tokenBody = "{}";
        private RuntimeException loginPostFailure;
        private RuntimeException tokenPostFailure;

        private RemoteCall() {
            when(client.target(anyString())).thenAnswer(invocation -> target(invocation.getArgument(0)));
            when(client.property(anyString(), any())).thenReturn(client);

            when(builder.accept(any(MediaType[].class))).thenReturn(builder);
            when(builder.accept(anyString())).thenReturn(builder);
            when(builder.header(anyString(), any())).thenReturn(builder);
            when(builder.cookie(any(NewCookie.class))).thenAnswer(invocation -> {
                recordCookie(invocation.getArgument(0));
                return builder;
            });
            when(builder.cookie(any(Cookie.class))).thenAnswer(invocation -> {
                recordCookie(invocation.getArgument(0));
                return builder;
            });
            when(builder.cookie(anyString(), any())).thenReturn(builder);
            when(builder.post(any())).thenAnswer(invocation -> {
                attemptedRoutes.add(pendingRoute);
                if (isRoute(LOGIN_URL) && loginPostFailure != null) {
                    throw loginPostFailure;
                }
                if (isRoute(TOKEN_URL) && tokenPostFailure != null) {
                    throw tokenPostFailure;
                }
                requests.add(new RecordedRequest(pendingRoute, (Entity<?>) invocation.getArgument(0)));
                return isRoute(LOGIN_URL) ? authResponse : tokenResponse;
            });
        }

        private void recordCookie(final Cookie cookie) {
            cookiesByRoute.computeIfAbsent(pendingRoute, route -> new ArrayList<>()).add(cookie);
        }

        private boolean isRoute(final String route) {
            return route.equals(pendingRoute);
        }

        private WebTarget target(final String route) {
            final WebTarget target = mock(WebTarget.class);
            when(target.path(anyString())).thenAnswer(invocation ->
                    target(join(route, invocation.getArgument(0))));
            when(target.request()).thenAnswer(invocation -> {
                pendingRoute = route;
                return builder;
            });
            when(target.request(any(MediaType[].class))).thenAnswer(invocation -> {
                pendingRoute = route;
                return builder;
            });
            when(target.request(anyString())).thenAnswer(invocation -> {
                pendingRoute = route;
                return builder;
            });
            return target;
        }

        private static String join(final String route, final String segment) {
            return route.endsWith("/") ? route + segment : route + "/" + segment;
        }

        private void loginSucceeds() {
            authResponse = response(Response.Status.OK.getStatusCode());
            loginBody = "{\"entity\":{\"userId\":\"admin\"}}";
            when(authResponse.readEntity(String.class)).thenReturn(loginBody);
            // A login may also set unrelated cookies; only JSESSIONID may be forwarded on.
            when(authResponse.getCookies()).thenReturn(
                    Map.of(SESSION_COOKIE, new NewCookie(SESSION_COOKIE, "session-value"),
                            "access_token", new NewCookie("access_token", "unrelated-value")));
        }

        private void loginSucceedsWithoutCookie() {
            loginSucceeds();
            when(authResponse.getCookies()).thenReturn(Map.of());
        }

        private void loginFailsUnauthorized() {
            authResponse = response(Response.Status.UNAUTHORIZED.getStatusCode());
            loginBody = "{\"error\":\"bad credentials\"}";
            when(authResponse.readEntity(String.class)).thenReturn(loginBody);
            when(authResponse.getCookies()).thenReturn(Map.of());
        }

        private void loginPostFails(final RuntimeException failure) {
            this.loginPostFailure = failure;
        }

        private void tokenPostFails(final RuntimeException failure) {
            this.tokenPostFailure = failure;
        }

        private void tokenSucceeds() {
            tokenBody = "{\"entity\":{\"jwt\":\"jwt\"}}";
            when(tokenResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
            when(tokenResponse.readEntity(String.class)).thenReturn(tokenBody);
        }

        private void tokenFails(final int status, final String body) {
            tokenBody = body;
            when(tokenResponse.getStatus()).thenReturn(status);
            when(tokenResponse.readEntity(String.class)).thenReturn(tokenBody);
        }

        private String loginBody() {
            return loginBody;
        }

        private String tokenBody() {
            return tokenBody;
        }

        private void assertNeverSentBasic() {
            verify(builder, never()).header(eq("Authorization"), startsWith("Basic "));
        }

        private void assertSameRemoteBase() {
            assertTrue("the login must target the configured remote base",
                    requestTo(LOGIN_URL).route.startsWith(BASE_URL));
            assertTrue("the token request must target the same remote base",
                    requestTo(TOKEN_URL).route.startsWith(BASE_URL));
        }

        private void assertLoginRequest() {
            final RecordedRequest login = requestTo(LOGIN_URL);
            final Object body = login.entity.getEntity();
            assertTrue("the login body must be a JSON object", body instanceof Map);
            final Map<?, ?> payload = (Map<?, ?>) body;
            assertEquals("admin@dotcms.com", payload.get("userId"));
            assertEquals("password", payload.get("password"));
            assertEquals(Boolean.TRUE, payload.get("backEndLogin"));
            assertTrue("the login must be posted as JSON",
                    MediaType.APPLICATION_JSON_TYPE.equals(login.entity.getMediaType()));
        }

        private void assertTokenRequest(final Map<String, Object> expectedBody) {
            final RecordedRequest token = requestTo(TOKEN_URL);
            assertEquals(expectedBody, token.entity.getEntity());
            assertTrue("the token must be posted as JSON",
                    MediaType.APPLICATION_JSON_TYPE.equals(token.entity.getMediaType()));
        }

        private void assertTokenNeverRequested() {
            assertTrue("no request may be sent to the token route",
                    requests.stream().noneMatch(request -> TOKEN_URL.equals(request.route)));
        }

        private void assertLoginAttempted() {
            assertTrue("the login must be attempted at the configured remote base",
                    attemptedRoutes.stream().anyMatch(LOGIN_URL::equals));
        }

        private void assertOnlySessionCookieForwarded() {
            final List<Cookie> tokenCookies = cookiesByRoute.getOrDefault(TOKEN_URL, List.of());
            assertTrue("the login session cookie must be forwarded to the token request",
                    tokenCookies.stream().anyMatch(this::isSessionCookie));
            assertTrue("no cookie other than the login session may be forwarded",
                    tokenCookies.stream().allMatch(this::isSessionCookie));
            assertTrue("the login request must not carry a session cookie",
                    cookiesByRoute.getOrDefault(LOGIN_URL, List.of()).isEmpty());
        }

        private boolean isSessionCookie(final Cookie cookie) {
            return SESSION_COOKIE.equals(cookie.getName()) && "session-value".equals(cookie.getValue());
        }

        private void assertRedirectsDisabled() {
            verify(client).property(ClientProperties.FOLLOW_REDIRECTS, false);
        }

        private void assertResponsesClosed() {
            assertLoginResponseClosed();
            assertTokenResponseClosed();
        }

        private void assertLoginResponseClosed() {
            verify(authResponse, times(1)).close();
        }

        private void assertTokenResponseClosed() {
            verify(tokenResponse, times(1)).close();
        }

        private RecordedRequest requestTo(final String fullUrl) {
            return requests.stream()
                    .filter(request -> fullUrl.equals(request.route))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no request was sent to " + fullUrl));
        }

        private User admin() {
            return user(true);
        }

        private User nonAdmin() {
            return user(false);
        }

        private static Response response(final int status) {
            final Response response = mock(Response.class);
            when(response.getStatus()).thenReturn(status);
            when(response.readEntity(String.class)).thenReturn("{}");
            return response;
        }

        private static User user(final boolean admin) {
            final User user = mock(User.class);
            when(user.isAdmin()).thenReturn(admin);
            return user;
        }
    }

    /** A single recorded request: the full route it targeted and the entity that was posted. */
    private static final class RecordedRequest {

        private final String route;
        private final Entity<?> entity;

        private RecordedRequest(final String route, final Entity<?> entity) {
            this.route = route;
            this.entity = entity;
        }
    }
}
