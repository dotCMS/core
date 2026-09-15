package com.dotcms.inference.rest;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.app.ConfigService;
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
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Specifies model validation in
 * {@link ChatCompletionsResource#completions(HttpServletRequest, HttpServletResponse, String, ChatCompletionRequestView)}
 * — which model names a site will accept, and from whom.
 *
 * <p>The interesting assertion in this file is the negative one: there is no administrator
 * exemption. A role-conditional passthrough is exactly the branching that let two shipped dotAI
 * endpoints drift to opposite model policies, and it would make SC-007 unstatable as a blanket
 * invariant. So an administrator naming an unconfigured model is refused with the same status and
 * the same body as a caller with no privileges at all, and that equality is asserted directly
 * rather than inferred from two separate tests.</p>
 *
 * <ul>
 *     <li>FR-023 — a model the site configured is accepted; one it did not is refused with the
 *     standard "no such model" shape, naming nothing the caller did not already send.</li>
 *     <li>FR-023 / SC-007 — the refusal is identical for an administrator and for a
 *     non-administrator, so no caller of any role can cause an unconfigured model to be invoked.</li>
 *     <li>FR-024 — a request omitting {@code model} is refused naming the field; there is no
 *     implicit default.</li>
 *     <li>FR-023 — a site configured with a fallback chain accepts any entry of the chain, not
 *     only the first.</li>
 * </ul>
 *
 * <p>The provider is a WireMock server standing in for an OpenAI-compatible endpoint, wired in
 * through the same dotAI app secrets the rest of the AI integration tests use, so an accepted
 * model travels the real client path rather than a stubbed one.</p>
 */
public class InferenceModelValidationTest {

    /** The chat model the site is configured with. */
    private static final String CHAT_MODEL = "gpt-4o-mini";

    /** The second entry of the fallback chain the site is given in one of these tests. */
    private static final String SECONDARY_MODEL = "gpt-4o-secondary";

    /** A model name no site in these tests ever configures. */
    private static final String UNCONFIGURED_MODEL = "some-other-vendors-model";

    /** Path an OpenAI-compatible provider serves completions on. */
    private static final String COMPLETIONS_PATH = "/chat/completions";

    private static final String ERROR_TYPE_INVALID_REQUEST = "invalid_request_error";

    /** What the stubbed provider answers whenever it is reached at all. */
    private static final String PROVIDER_RESPONSE = """
            {
              "id": "chatcmpl-model-1",
              "object": "chat.completion",
              "created": 1789000000,
              "model": "gpt-4o-mini",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "Served."
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {"prompt_tokens": 9, "completion_tokens": 2, "total_tokens": 11}
            }
            """;

    private static WireMockServer wireMockServer;

    /** A CMS administrator; the caller a role-conditional exemption would have privileged. */
    private static User adminUser;
    private static String adminToken;

    /** A backend user with no administrative role, granted nothing but read on the test site. */
    private static User limitedUser;
    private static String limitedToken;

    private Host host;
    private final ChatCompletionsResource resource = new ChatCompletionsResource();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        stubProvider();

        // Both roles are needed for the administrator, not one: WebResource.checkRolePermissions
        // matches DOTCMS_BACK_END_USER by key without walking inheritance, so being an admin does
        // not imply it, and admin is what grants read on the site passed as an explicit override.
        adminUser = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(),
                        APILocator.getRoleAPI().loadCMSAdminRole())
                .nextPersisted();
        adminToken = bearerTokenFor(adminUser);

        // The counterpart: admitted by FR-016 as an authenticated backend user, and nothing more.
        // Read on the site is granted per test, since the site is created per test.
        limitedUser = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
        limitedToken = bearerTokenFor(limitedUser);
    }

    @AfterClass
    public static void afterClass() {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    @Before
    public void before() throws Exception {
        host = new SiteDataGen().nextPersisted();
        AiTest.aiAppSecretsWithProviderConfig(host, AiTest.providerConfigJson(AiTest.PORT, CHAT_MODEL));
        grantSiteRead(host, limitedUser);
        wireMockServer.resetRequests();
    }

    @After
    public void after() throws Exception {
        AiTest.removeAiAppSecrets(host);
    }

    /**
     * Given a site configured with one chat model
     * When a completion names that model
     * Then it is accepted and the provider serves it
     */
    @Test
    public void test_completions_withConfiguredModel_isAccepted() {
        final Response response = resource.completions(
                mockRequest(adminToken), mockResponse(), host.getIdentifier(),
                completionFor(CHAT_MODEL));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);

        final ChatCompletionView view = (ChatCompletionView) response.getEntity();
        assertEquals(CHAT_MODEL, view.model());
        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH)));
    }

    /**
     * Given a site configured with one chat model
     * When a completion names a model the site has not configured
     * Then it is refused with a 404 in the "no such model" shape, the provider is never contacted,
     * and the message repeats nothing but what the caller already sent
     */
    @Test
    public void test_completions_withUnconfiguredModel_isRefusedAsNoSuchModel() {
        final Response response = resource.completions(
                mockRequest(adminToken), mockResponse(), host.getIdentifier(),
                completionFor(UNCONFIGURED_MODEL));

        assertNotNull(response);
        assertEquals(404, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView errorView = (InferenceErrorView) response.getEntity();
        assertNotNull(errorView.error());
        assertEquals(ERROR_TYPE_INVALID_REQUEST, errorView.error().type());
        assertEquals("model", errorView.error().param());

        final String message = errorView.error().message();
        assertNotNull(message);
        assertTrue(message.contains(UNCONFIGURED_MODEL));

        // What the refusal must not leak: which models the site does run, where its provider
        // lives, what key reaches it, or which site is behind the host name.
        assertFalse(message.contains(CHAT_MODEL));
        assertFalse(message.contains(AiTest.API_KEY));
        assertFalse(message.contains(String.valueOf(AiTest.PORT)));
        assertFalse(message.contains(host.getHostname()));
        assertFalse(message.contains(host.getIdentifier()));

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH)));
    }

    /**
     * Given a CMS administrator and a backend user with no administrative role, both able to read
     * the same site
     * When each names a model the site has not configured
     * Then both are refused, with the same status and the same error body
     *
     * <p>This is the SC-007 invariant stated as one assertion rather than two: no caller, of any
     * role, can cause a model outside the site's configuration to be invoked. Asserting the two
     * refusals are <em>equal</em> is what would catch an exemption added later, which two
     * independent single-role tests would not.</p>
     */
    @Test
    public void test_completions_withUnconfiguredModel_refusesAdministratorAndNonAdministratorAlike() {
        final Response adminResponse = resource.completions(
                mockRequest(adminToken), mockResponse(), host.getIdentifier(),
                completionFor(UNCONFIGURED_MODEL));
        final Response limitedResponse = resource.completions(
                mockRequest(limitedToken), mockResponse(), host.getIdentifier(),
                completionFor(UNCONFIGURED_MODEL));

        assertNotNull(adminResponse);
        assertNotNull(limitedResponse);
        assertEquals(404, adminResponse.getStatus());
        assertEquals(adminResponse.getStatus(), limitedResponse.getStatus());

        assertTrue(adminResponse.getEntity() instanceof InferenceErrorView);
        assertTrue(limitedResponse.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView.Body adminError =
                ((InferenceErrorView) adminResponse.getEntity()).error();
        final InferenceErrorView.Body limitedError =
                ((InferenceErrorView) limitedResponse.getEntity()).error();

        assertEquals(adminError.type(), limitedError.type());
        assertEquals(adminError.param(), limitedError.param());
        assertEquals(adminError.message(), limitedError.message());
        assertEquals(adminError.code(), limitedError.code());

        // Neither role reached the provider.
        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH)));
    }

    /**
     * Given a request that omits {@code model} entirely
     * When the completion is requested
     * Then it is refused as a client error naming {@code model}, with no implicit default applied
     */
    @Test
    public void test_completions_withoutModel_isRejectedNamingTheField() {
        final ChatCompletionRequestView requestView = new ChatCompletionRequestView(
                null,
                List.of(new MessageView("user", "Say something.", null, null, null)),
                null, null, null, null, null, null, null, null, null, null);

        final Response response = resource.completions(
                mockRequest(adminToken), mockResponse(), host.getIdentifier(), requestView);

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView errorView = (InferenceErrorView) response.getEntity();
        assertNotNull(errorView.error());
        assertEquals(ERROR_TYPE_INVALID_REQUEST, errorView.error().type());
        assertEquals("model", errorView.error().param());

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH)));
    }

    /**
     * Given a site configured with a fallback chain of two models
     * When a completion names the second entry of that chain
     * Then it is accepted, not refused as unconfigured
     *
     * <p>The set of accepted names is the whole chain, not its head. What actually serves is a
     * separate question the site's configuration answers, not the caller: the client walks the
     * chain from the front, so the served model reported back is an entry of the chain and need
     * not be the one that was asked for.</p>
     */
    @Test
    public void test_completions_withFallbackChainSecondEntry_isAccepted() throws Exception {
        final List<String> chain = List.of(CHAT_MODEL, SECONDARY_MODEL);
        configureChain(host, chain);

        final Response response = resource.completions(
                mockRequest(adminToken), mockResponse(), host.getIdentifier(),
                completionFor(SECONDARY_MODEL));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);

        final ChatCompletionView view = (ChatCompletionView) response.getEntity();
        assertTrue(chain.contains(view.model()));
        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH)));
    }

    /**
     * Re-saves the site's dotAI secrets with a comma-separated fallback chain, and waits until the
     * new configuration is the one being resolved — re-saving alone would leave the test racing
     * the secrets cache.
     *
     * @param site  the site to reconfigure
     * @param chain the models, in fallback order
     */
    private static void configureChain(final Host site, final List<String> chain) throws Exception {
        final String joined = String.join(",", chain);
        AiTest.aiAppSecretsWithProviderConfig(site, AiTest.providerConfigJson(AiTest.PORT, joined));
        await().atMost(5, SECONDS).until(() -> {
            final String resolved = ConfigService.INSTANCE.config(site).getProviderConfig();
            return resolved != null && resolved.contains(joined);
        });
    }

    /**
     * Grants read on a site to a user's own role, so a caller with no administrative role can
     * still name the site as an explicit override. FR-019 refuses the override otherwise, which
     * would make the non-administrator's refusal a 403 about the site rather than the 404 about
     * the model this file is comparing.
     *
     * @param site  the site to grant read on
     * @param grantee the user to grant it to
     */
    private static void grantSiteRead(final Host site, final User grantee) throws Exception {
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Permission readPermission = new Permission(
                site.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(grantee).getId(),
                PermissionAPI.PERMISSION_READ);
        permissionAPI.save(readPermission, site, APILocator.systemUser(), false);
    }

    /**
     * Stubs the OpenAI-compatible provider with a single canned answer; this file is about which
     * requests reach it, not what it says.
     */
    private static void stubProvider() {
        wireMockServer.stubFor(post(urlPathEqualTo(COMPLETIONS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_RESPONSE)));
    }

    /**
     * @param forUser the caller to mint a token for
     * @return the bearer credential that caller authenticates with
     */
    private static String bearerTokenFor(final User forUser) throws Exception {
        final ApiToken apiToken = APILocator.getApiTokenAPI().persistApiToken(
                forUser.getUserId(),
                Date.from(Instant.now().plus(Duration.ofDays(1))),
                APILocator.systemUser().getUserId(),
                "127.0.0.1");
        return "Bearer " + APILocator.getApiTokenAPI().getJWT(apiToken, forUser);
    }

    /**
     * @param model the model to ask for
     * @return the smallest valid completion request
     */
    private static ChatCompletionRequestView completionFor(final String model) {
        return new ChatCompletionRequestView(
                model,
                List.of(new MessageView("user", "Say something.", null, null, null)),
                null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * @param bearerToken the caller's credential
     * @return a request authenticated as that caller, as the family requires
     */
    private static HttpServletRequest mockRequest(final String bearerToken) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/inference/v1/chat/completions");
        when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://localhost/api/inference/v1/chat/completions"));
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        return request;
    }

    private static HttpServletResponse mockResponse() {
        return mock(HttpServletResponse.class);
    }
}
