package com.dotcms.inference.rest;

import com.dotcms.ai.AiTest;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.inference.rest.view.ChatCompletionRequestView;
import com.dotcms.inference.rest.view.ChatCompletionRequestView.MessageView;
import com.dotcms.inference.rest.view.ChatCompletionView;
import com.dotcms.inference.rest.view.InferenceErrorView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Specifies who may call {@code /api/inference/v1} and on whose behalf — the authentication and
 * site-authorization half of User Story 3.
 *
 * <p>The three requirements pull in different directions on purpose, and each is only meaningful
 * next to the others:</p>
 *
 * <ul>
 *     <li>FR-015 — the credential must be a dotCMS API token presented as a bearer. Anonymous
 *     callers are refused, and so are the session cookies that surrounding dotCMS filters
 *     otherwise accept: this family is server-side only by design, and admitting ambient browser
 *     credentials would make the absence of cross-origin headers a formality rather than a
 *     control.</li>
 *     <li>FR-016 — <em>any</em> authenticated user is admitted, backend or frontend alike, because
 *     a site calling AI on behalf of a visitor is a supported use case. The bar is authentication,
 *     not privilege.</li>
 *     <li>FR-019 — but the moment a caller names a site explicitly, READ on that site is enforced,
 *     and a caller who lacks it is refused. This is the one check that stands between an
 *     authenticated caller and another site's credentials.</li>
 * </ul>
 *
 * <p>Refusals arrive in two different shapes, which is a property of the contract rather than an
 * inconsistency. An authentication failure is raised by {@code WebResource} before the resource
 * body runs, so it surfaces as a {@link WebApplicationException} carrying the status; a site
 * authorization failure is caught by the resource and rendered as an {@link InferenceErrorView},
 * so that a standard client can deserialize it with no adapter.</p>
 */
public class InferenceAuthorizationTest {

    /** Path an OpenAI-compatible provider serves completions on. */
    private static final String COMPLETIONS_PATH = "/chat/completions";

    private static final String REQUEST_URI = "/api/inference/v1/chat/completions";

    /** The chat model the site under test is configured with. */
    private static final String CHAT_MODEL = "gpt-4o-mini";

    private static final String ERROR_TYPE_INVALID_REQUEST = "invalid_request_error";

    /** What the stubbed provider answers whenever a call gets that far. */
    private static final String PROVIDER_RESPONSE = """
            {
              "id": "chatcmpl-auth-1",
              "object": "chat.completion",
              "created": 1789000000,
              "model": "gpt-4o-mini",
              "choices": [
                {
                  "index": 0,
                  "message": {"role": "assistant", "content": "Answered."},
                  "finish_reason": "stop"
                }
              ],
              "usage": {"prompt_tokens": 10, "completion_tokens": 2, "total_tokens": 12}
            }
            """;

    private static WireMockServer wireMockServer;

    /** A backend user who is also an administrator, so site READ is never the thing under test. */
    private static User backendUser;
    private static String backendBearerToken;

    /** A frontend-only user: authenticated, unprivileged, and admitted by FR-016 all the same. */
    private static User frontendUser;
    private static String frontendBearerToken;

    /** A backend user who is not an administrator, and so cannot read an arbitrary site. */
    private static User unprivilegedUser;
    private static String unprivilegedBearerToken;

    private Host host;

    private final ChatCompletionsResource resource = new ChatCompletionsResource();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        stubProvider();

        // Two roles, not one. WebResource.checkRolePermissions matches DOTCMS_BACK_END_USER by
        // key and does not walk inheritance, so being an administrator does not imply it; the
        // administrator role is separately what grants READ on a site.
        backendUser = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(),
                        APILocator.getRoleAPI().loadCMSAdminRole())
                .nextPersisted();
        backendBearerToken = bearerTokenFor(backendUser);

        frontendUser = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadFrontEndUserRole())
                .nextPersisted();
        frontendBearerToken = bearerTokenFor(frontendUser);

        // Deliberately no administrator role: this user passes authentication and the role gate,
        // and then fails on site READ, which is exactly the boundary FR-019 draws.
        unprivilegedUser = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
        unprivilegedBearerToken = bearerTokenFor(unprivilegedUser);
    }

    @AfterClass
    public static void afterClass() {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    @Before
    public void before() throws Exception {
        host = new SiteDataGen()
                .name("inference-auth-" + UUID.randomUUID() + ".dotcms.com")
                .nextPersisted();
        AiTest.aiAppSecretsWithProviderConfig(
                host, AiTest.providerConfigJson(AiTest.PORT, CHAT_MODEL));
        wireMockServer.resetRequests();
    }

    @After
    public void after() throws Exception {
        AiTest.removeAiAppSecrets(host);
    }

    /**
     * Given a request carrying no credential at all
     * When a completion is requested
     * Then it is refused as unauthorized
     *
     * <p>FR-015. The refusal carries the standard error shape, as the contract's status table
     * requires of a 401 — a caller's client library should be able to deserialize a refusal into
     * its own error type exactly as it would a success. Accepts a thrown
     * {@link WebApplicationException} too, since the surrounding authentication handshake may
     * refuse before the resource body runs and either is a valid refusal.</p>
     */
    @Test
    public void test_completions_withNoCredential_isUnauthorized() {
        final HttpServletRequest request = mockRequest(host.getHostname(), null);

        try {
            final Response response =
                    resource.completions(request, mockResponse(), null, completionFor(CHAT_MODEL));
            assertEquals("An anonymous request must be refused as unauthorized",
                    Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            assertTrue("a refusal carries the standard error shape",
                    response.getEntity() instanceof InferenceErrorView);
        } catch (final WebApplicationException e) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                    e.getResponse().getStatus());
        }
    }

    /**
     * Given a request authenticated by a session rather than by a bearer token, and otherwise
     * entirely valid
     * When a completion is requested
     * Then it is refused as unauthorized
     *
     * <p>FR-015 accepts a dotCMS API token presented as a bearer credential and nothing else. The
     * session cookies the surrounding dotCMS filters generally honour must not be honoured here:
     * this family is server-side only by design, and a browser that can be made to carry an
     * ambient session would otherwise be able to spend a site's AI credentials.</p>
     */
    @Test
    public void test_completions_withSessionCredentialOnly_isUnauthorized() {
        final HttpServletRequest request = mockRequest(host.getHostname(), null);
        givenSessionAuthenticatedAs(request, backendUser);

        try {
            final Response response = resource.completions(
                    request, mockResponse(), null, completionFor(CHAT_MODEL));
            assertEquals("A session credential must not authenticate this family",
                    Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            assertTrue("a refusal carries the standard error shape",
                    response.getEntity() instanceof InferenceErrorView);
        } catch (final WebApplicationException e) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                    e.getResponse().getStatus());
        }
    }

    /**
     * Given an authenticated caller who cannot read the site they name explicitly
     * When a completion is requested with that site as the override
     * Then it is refused as forbidden, in the standard error shape, naming the offending field
     *
     * <p>FR-019. The caller authenticates and clears the role gate; what stops them is READ on the
     * site whose credentials they asked to spend.</p>
     */
    @Test
    public void test_completions_withOverrideForUnreadableSite_isForbidden() {
        final HttpServletRequest request =
                mockRequest(host.getHostname(), unprivilegedBearerToken);

        final Response response = resource.completions(
                request, mockResponse(), host.getIdentifier(), completionFor(CHAT_MODEL));

        assertNotNull(response);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView errorView = (InferenceErrorView) response.getEntity();
        assertNotNull(errorView.error());
        assertEquals(ERROR_TYPE_INVALID_REQUEST, errorView.error().type());
        assertEquals("siteId", errorView.error().param());
    }

    /**
     * Given a backend user authenticated by bearer token
     * When a completion is requested
     * Then it is served
     *
     * <p>FR-016, one half. Stated alongside the frontend case because the requirement is that
     * <em>both</em> are admitted; either one alone would be satisfied by a rule that excluded the
     * other.</p>
     */
    @Test
    public void test_completions_withBackendUser_isAccepted() {
        final HttpServletRequest request = mockRequest(host.getHostname(), backendBearerToken);

        final Response response = resource.completions(
                request, mockResponse(), null, completionFor(CHAT_MODEL));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);
    }

    /**
     * Given a frontend user authenticated by bearer token
     * When a completion is requested
     * Then it is served
     *
     * <p>FR-016, the other half. A site calling AI on behalf of a visitor is a supported use case,
     * so the bar on this family is authentication, not privilege.</p>
     */
    @Test
    public void test_completions_withFrontendUser_isAccepted() {
        final HttpServletRequest request = mockRequest(host.getHostname(), frontendBearerToken);

        final Response response = resource.completions(
                request, mockResponse(), null, completionFor(CHAT_MODEL));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);
    }

    /** Answers every completion that gets as far as the provider with the same finished response. */
    private static void stubProvider() {
        wireMockServer.stubFor(post(urlPathEqualTo(COMPLETIONS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_RESPONSE)));
    }

    /**
     * @param user the caller to mint a credential for
     * @return the {@code Authorization} header value carrying that caller's API token
     */
    private static String bearerTokenFor(final User user) throws Exception {
        final ApiToken apiToken = APILocator.getApiTokenAPI().persistApiToken(
                user.getUserId(),
                Date.from(Instant.now().plus(Duration.ofDays(1))),
                APILocator.systemUser().getUserId(),
                "127.0.0.1");
        return "Bearer " + APILocator.getApiTokenAPI().getJWT(apiToken, user);
    }

    /**
     * @param model the model to ask for
     * @return the smallest well-formed completion request
     */
    private static ChatCompletionRequestView completionFor(final String model) {
        return new ChatCompletionRequestView(
                model,
                List.of(new MessageView("user", "May I call you?", null, null, null)),
                null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Gives the request a logged-in session, the way a browser carrying a dotCMS session cookie
     * would present itself to the servlet layer.
     *
     * @param request the request to attach the session to
     * @param user    the user the session is logged in as
     */
    private static void givenSessionAuthenticatedAs(final HttpServletRequest request,
                                                    final User user) {
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        when(session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID))
                .thenReturn(user.getUserId());
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
    }

    /**
     * Builds a request arriving at a given host name, with or without a bearer credential.
     *
     * <p>Request attributes are backed by a real map rather than left as mock no-ops, because the
     * authentication handshake publishes the resolved caller as an attribute and a mock that
     * forgot it would not behave like a servlet container.</p>
     *
     * @param serverName  the host name the request arrives on
     * @param bearerToken the {@code Authorization} header value, or null for no credential
     * @return the mocked request
     */
    private static HttpServletRequest mockRequest(final String serverName,
                                                  final String bearerToken) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Map<String, Object> attributes = new HashMap<>();

        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://" + serverName + REQUEST_URI));
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getServerName()).thenReturn(serverName);
        when(request.getHeader("Authorization")).thenReturn(bearerToken);

        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        when(request.getAttribute(anyString()))
                .thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));

        return request;
    }

    private static HttpServletResponse mockResponse() {
        return mock(HttpServletResponse.class);
    }
}
