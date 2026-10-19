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
import static org.mockito.Mockito.doAnswer;
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
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Contract test for {@link ApiTokenResource#getRemoteToken}. It pins the secure remote-issuance
 * sequence: a JSON login against {@code /api/v1/authentication} yields a {@code JSESSIONID}
 * session cookie that is carried to {@code /api/v1/apitoken}, and no Basic credential header is
 * ever sent on the wire. The admin gate, status pass-through and client close contract are
 * retained. The current implementation still posts directly with Basic, so the success and
 * failure cases are expected to be Red until the remote flow is migrated.
 *
 * <p>Limitation: the mocked client cannot observe redirect policy; the tests only prove that the
 * issuer does not send Basic credentials and does not issue a token without a login session.</p>
 */
public class RemoteApiTokenAuthenticationTest extends UnitTestBase {

    private static final String ENABLE_PROXY_TOKEN_REQUESTS = "ENABLE_PROXY_TOKEN_REQUESTS";

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
        final List<Cookie> cookiesOnTokenRequest = new ArrayList<>();
        remote.recordCookies(cookiesOnTokenRequest);

        final Response result = newApiTokenResource(remote.admin())
                .getRemoteToken(mock(HttpServletRequest.class), remoteForm());

        remote.assertNeverSentBasic();
        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        verify(remote.builder, times(2)).post(any());
        assertTrue("the login session cookie must be forwarded to the token request",
                cookiesOnTokenRequest.stream()
                        .anyMatch(cookie -> "JSESSIONID".equals(cookie.getName())));
        verify(remote.client, atLeastOnce()).close();
    }

    /** A rejected login must not fall through to issuing a remote token. */
    @Test
    public void remoteToken_loginFailure_neverIssuesToken() throws Exception {
        final RemoteCall remote = new RemoteCall();
        remote.loginFailsUnauthorized();
        remote.tokenSucceeds();

        final Response result = newApiTokenResource(remote.admin())
                .getRemoteToken(mock(HttpServletRequest.class), remoteForm());

        remote.assertNeverSentBasic();
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

        final Response result = newApiTokenResource(remote.admin())
                .getRemoteToken(mock(HttpServletRequest.class), remoteForm());

        remote.assertNeverSentBasic();
        assertNotEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        verify(remote.builder, times(1)).post(any());
        verify(remote.client, atLeastOnce()).close();
    }

    /** The admin gate must survive the migration; a non-admin must never touch the remote host. */
    @Test
    public void remoteToken_nonAdmin_isForbiddenWithoutRemoteCalls() {
        final RemoteCall remote = new RemoteCall();

        assertThrows(ForbiddenException.class,
                () -> newApiTokenResource(remote.nonAdmin())
                        .getRemoteToken(mock(HttpServletRequest.class), remoteForm()));

        verifyNoInteractions(remote.client);
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
     * Mocks the Jersey client graph with a single recording builder. The login and token posts are
     * distinguished by order only, so the test does not depend on the implementation's URL or path
     * construction.
     */
    private static final class RemoteCall {

        private final Client client = mock(Client.class);
        private final WebTarget target = mock(WebTarget.class);
        private final Invocation.Builder builder = mock(Invocation.Builder.class);
        private final Response tokenResponse = mock(Response.class);
        private final AtomicInteger posts = new AtomicInteger();
        private Response authResponse = mock(Response.class);

        private RemoteCall() {
            when(client.target(anyString())).thenReturn(target);
            when(target.path(anyString())).thenReturn(target);
            when(target.request()).thenReturn(builder);
            when(target.request(any(MediaType[].class))).thenReturn(builder);
            when(target.request(anyString())).thenReturn(builder);
            when(builder.accept(any(MediaType[].class))).thenReturn(builder);
            when(builder.accept(anyString())).thenReturn(builder);
            when(builder.header(anyString(), any())).thenReturn(builder);
            when(builder.cookie(any(NewCookie.class))).thenReturn(builder);
            when(builder.cookie(any(Cookie.class))).thenReturn(builder);
            when(builder.cookie(anyString(), any())).thenReturn(builder);
            when(builder.post(any())).thenAnswer(invocation ->
                    posts.getAndIncrement() == 0 ? authResponse : tokenResponse);
        }

        private void loginSucceeds() {
            authResponse = response(Response.Status.OK.getStatusCode());
            when(authResponse.getCookies())
                    .thenReturn(Map.of("JSESSIONID", new NewCookie("JSESSIONID", "session-value")));
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

        private void recordCookies(final List<Cookie> cookies) {
            doAnswer(invocation -> {
                cookies.add(invocation.getArgument(0));
                return builder;
            }).when(builder).cookie(any(NewCookie.class));
            doAnswer(invocation -> {
                cookies.add(invocation.getArgument(0));
                return builder;
            }).when(builder).cookie(any(Cookie.class));
        }

        private void assertNeverSentBasic() {
            verify(builder, never()).header(eq("Authorization"), startsWith("Basic "));
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
}
