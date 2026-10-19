package com.dotcms.rest.api.v1.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
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
 * <p>Limitations: the mocked client cannot observe real network redirects; the tests only prove
 * that the issuer disables client redirects, never sends Basic credentials, and does not issue a
 * token without a login session cookie.</p>
 */
public class RemoteApiTokenAuthenticationTest extends UnitTestBase {

    private static final String ENABLE_PROXY_TOKEN_REQUESTS = "ENABLE_PROXY_TOKEN_REQUESTS";
    private static final String LOGIN_PATH = "/api/v1/authentication";
    private static final String TOKEN_PATH = "/api/v1/apitoken";
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
        remote.assertLoginRequest();
        remote.assertTokenRequest(form.getTokenInfo());
        remote.assertOnlySessionCookieForwarded();
        remote.assertRedirectsDisabled();
        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        verify(remote.builder, times(2)).post(any());
        verify(remote.client, atLeastOnce()).close();
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
        assertNotEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        verify(remote.builder, times(1)).post(any());
        verify(remote.client, atLeastOnce()).close();
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
        assertNotEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        verify(remote.builder, times(1)).post(any());
        verify(remote.client, atLeastOnce()).close();
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
     * Mocks the Jersey client graph with a single recording builder. Each request is keyed by the
     * route it actually targets, so a request to the wrong URL or with the wrong entity fails
     * instead of being accepted because it happened to be first.
     */
    private static final class RemoteCall {

        private final Client client = mock(Client.class);
        private final Invocation.Builder builder = mock(Invocation.Builder.class);
        private final Response tokenResponse = mock(Response.class);
        private final List<RecordedRequest> requests = new ArrayList<>();
        private final List<Cookie> forwardedCookies = new ArrayList<>();
        private Response authResponse = mock(Response.class);
        private String pendingRoute;

        private RemoteCall() {
            when(client.target(anyString())).thenAnswer(invocation -> target(invocation.getArgument(0)));
            when(client.property(anyString(), any())).thenReturn(client);

            when(builder.accept(any(MediaType[].class))).thenReturn(builder);
            when(builder.accept(anyString())).thenReturn(builder);
            when(builder.header(anyString(), any())).thenReturn(builder);
            when(builder.cookie(any(NewCookie.class))).thenAnswer(invocation -> {
                forwardedCookies.add(invocation.getArgument(0));
                return builder;
            });
            when(builder.cookie(any(Cookie.class))).thenAnswer(invocation -> {
                forwardedCookies.add(invocation.getArgument(0));
                return builder;
            });
            when(builder.cookie(anyString(), any())).thenReturn(builder);
            when(builder.post(any())).thenAnswer(invocation -> {
                requests.add(new RecordedRequest(pendingRoute, (Entity<?>) invocation.getArgument(0)));
                return pendingRoute != null && pendingRoute.endsWith(LOGIN_PATH)
                        ? authResponse : tokenResponse;
            });
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
            when(authResponse.getCookies()).thenReturn(
                    Map.of(SESSION_COOKIE, new NewCookie(SESSION_COOKIE, "session-value")));
        }

        private void loginSucceedsWithoutCookie() {
            loginSucceeds();
            when(authResponse.getCookies()).thenReturn(Map.of());
        }

        private void loginFailsUnauthorized() {
            authResponse = response(Response.Status.UNAUTHORIZED.getStatusCode());
            when(authResponse.getCookies()).thenReturn(Map.of());
        }

        private void tokenSucceeds() {
            when(tokenResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
            when(tokenResponse.readEntity(String.class)).thenReturn("{\"entity\":{\"jwt\":\"jwt\"}}");
        }

        private void assertNeverSentBasic() {
            verify(builder, never()).header(eq("Authorization"), startsWith("Basic "));
        }

        private void assertLoginRequest() {
            final RecordedRequest login = requestTo(LOGIN_PATH);
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
            final RecordedRequest token = requestTo(TOKEN_PATH);
            assertEquals(expectedBody, token.entity.getEntity());
            assertTrue("the token must be posted as JSON",
                    MediaType.APPLICATION_JSON_TYPE.equals(token.entity.getMediaType()));
        }

        private void assertOnlySessionCookieForwarded() {
            assertTrue("the login session cookie must be forwarded to the token request",
                    forwardedCookies.stream().anyMatch(this::isSessionCookie));
            assertTrue("no cookie other than the login session may be forwarded",
                    forwardedCookies.stream().allMatch(this::isSessionCookie));
        }

        private boolean isSessionCookie(final Cookie cookie) {
            return SESSION_COOKIE.equals(cookie.getName()) && "session-value".equals(cookie.getValue());
        }

        private void assertRedirectsDisabled() {
            verify(client).property(ClientProperties.FOLLOW_REDIRECTS, false);
        }

        private RecordedRequest requestTo(final String path) {
            return requests.stream()
                    .filter(request -> request.route != null && request.route.endsWith(path))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no request was sent to " + path));
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

    /** A single recorded request: the route it targeted and the entity that was posted. */
    private static final class RecordedRequest {

        private final String route;
        private final Entity<?> entity;

        private RecordedRequest(final String route, final Entity<?> entity) {
            this.route = route;
            this.entity = entity;
        }
    }
}
