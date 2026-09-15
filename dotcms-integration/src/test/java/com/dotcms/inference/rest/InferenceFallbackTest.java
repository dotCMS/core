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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Specifies the two silent fallbacks that
 * {@link ChatCompletionsResource#completions(HttpServletRequest, HttpServletResponse, String, ChatCompletionRequestView)}
 * deliberately <strong>keeps</strong>, and the one case where falling back is refused instead.
 *
 * <p>The source issue asked for both fallbacks to be removed for this endpoint family. That was
 * declined, so they need tests that state the kept behaviour rather than tests that would pass
 * either way: a family which resolved differently from the rest of dotCMS would break the
 * server-side callers it exists for, because internal DNS names, container service names and
 * {@code localhost} are not site aliases, and because many installations configure dotAI once at
 * system level rather than per site.</p>
 *
 * <ul>
 *     <li>FR-020 — a host name matching no site or alias is served by the default site, not
 *     refused, and the site that served is the one reported as having resolved.</li>
 *     <li>FR-021 — a site with no dotAI configuration of its own inherits the system-level one,
 *     exactly as every other dotAI endpoint does, and the request succeeds.</li>
 *     <li>FR-021 — when neither the resolved site nor the system level has any configuration, the
 *     request is refused rather than served from some unrelated site's credentials.</li>
 * </ul>
 *
 * <p>The provider is a WireMock server standing in for an OpenAI-compatible endpoint, wired in
 * through the same dotAI app secrets the rest of the AI integration tests use, so the exchange
 * travels the real client path rather than a stubbed one.</p>
 *
 * <p>These tests move the <strong>system-level</strong> dotAI configuration, which is shared
 * state. Whatever SYSTEM_HOST carried when the class started is captured and put back in
 * {@link #afterClass()}.</p>
 */
public class InferenceFallbackTest {

    /** The chat model the system-level configuration carries, and the only one asked for here. */
    private static final String CHAT_MODEL = "gpt-4o-mini";

    /** Path an OpenAI-compatible provider serves completions on. */
    private static final String COMPLETIONS_PATH = "/chat/completions";

    /**
     * A host name no site and no alias can match, standing in for the internal DNS name, container
     * service name or {@code localhost} a server-side caller routinely presents.
     */
    private static final String UNMATCHED_HOST_NAME = "inference-fallback-no-such-site.invalid";

    /** What the stubbed provider answers whenever it is reached at all. */
    private static final String PROVIDER_RESPONSE = """
            {
              "id": "chatcmpl-fallback-1",
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
    private static User user;
    private static String bearerToken;
    private static Host defaultHost;

    /** The system-level providerConfig as found, so these tests can put it back. */
    private static String originalSystemProviderConfig;

    private Host unconfiguredSite;
    private final ChatCompletionsResource resource = new ChatCompletionsResource();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        stubProvider();

        defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        originalSystemProviderConfig =
                ConfigService.INSTANCE.config(APILocator.systemHost()).getProviderConfig();

        // Both roles are needed, not one: WebResource.checkRolePermissions matches
        // DOTCMS_BACK_END_USER by key without walking inheritance, so being an admin does not
        // imply it, and admin is what grants read on the site passed as an explicit override.
        user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(),
                        APILocator.getRoleAPI().loadCMSAdminRole())
                .nextPersisted();
        final ApiToken apiToken = APILocator.getApiTokenAPI().persistApiToken(
                user.getUserId(),
                Date.from(Instant.now().plus(Duration.ofDays(1))),
                APILocator.systemUser().getUserId(),
                "127.0.0.1");
        bearerToken = "Bearer " + APILocator.getApiTokenAPI().getJWT(apiToken, user);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        restoreSystemLevelConfiguration();
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    @Before
    public void before() {
        unconfiguredSite = new SiteDataGen().nextPersisted();
        wireMockServer.resetRequests();
    }

    @After
    public void after() throws Exception {
        clearSystemLevelConfiguration();
    }

    /**
     * Given a request whose host name matches no site and no alias, and a system-level dotAI
     * configuration the default site inherits
     * When the completion is requested with no explicit site override
     * Then it is served rather than refused
     */
    @Test
    public void test_completions_withUnmatchedHostName_isServedByDefaultSite() throws Exception {
        configureSystemLevel();
        final HttpServletRequest request = mockRequest(UNMATCHED_HOST_NAME);

        final Response response = resource.completions(
                request, mockResponse(), null, completionFor(CHAT_MODEL));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);

        final ChatCompletionView view = (ChatCompletionView) response.getEntity();
        assertNotNull(view.choices());
        assertEquals(1, view.choices().size());
        assertNotNull(view.choices().get(0).message());
        assertNotNull(view.choices().get(0).message().content());
        assertFalse(view.choices().get(0).message().content().isBlank());
    }

    /**
     * Given a request whose host name matches no site and no alias
     * When the completion is requested with no explicit site override
     * Then the site reported as having served is the default site
     *
     * <p>FR-020 asks for the fallback to be logged as well. Asserting on the log would mean
     * attaching an appender to dotCMS's Log4j configuration from an integration test, which is
     * both brittle and outside what this family owns, so what is asserted here is the observable
     * half of the same requirement: the serving site published for the response header. The log
     * line itself lives in {@code AiHostResolver.resolveFromRequest} and carries the unmatched
     * host name.</p>
     */
    @Test
    public void test_completions_withUnmatchedHostName_reportsDefaultSiteAsServingSite()
            throws Exception {
        configureSystemLevel();
        final HttpServletRequest request = mockRequest(UNMATCHED_HOST_NAME);

        resource.completions(request, mockResponse(), null, completionFor(CHAT_MODEL));

        verify(request).setAttribute(
                InferenceRequestAttributes.RESOLVED_SITE_ID, defaultHost.getIdentifier());
    }

    /**
     * Given a site with no dotAI configuration of its own and a system-level configuration
     * When the completion is requested against that site
     * Then it is served from the system-level configuration and the serving site is still the
     * requested one
     */
    @Test
    public void test_completions_siteWithoutOwnConfiguration_isServedFromSystemLevel()
            throws Exception {
        configureSystemLevel();
        final HttpServletRequest request = mockRequest(UNMATCHED_HOST_NAME);

        final Response response = resource.completions(
                request, mockResponse(), unconfiguredSite.getIdentifier(), completionFor(CHAT_MODEL));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);

        // The configuration was inherited, but the site that resolved — and that a reconciliation
        // pipeline would bill — is the one the caller named, not SYSTEM_HOST.
        verify(request).setAttribute(
                InferenceRequestAttributes.RESOLVED_SITE_ID, unconfiguredSite.getIdentifier());
        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH)));
    }

    /**
     * Given a site with no dotAI configuration of its own and no system-level configuration either
     * When the completion is requested against that site
     * Then it is refused with an explanatory error and no provider is contacted
     *
     * <p>The refusal is the 404 "no such model" shape rather than a distinct "site not
     * configured" error, deliberately: from where the caller stands a model nobody configured and
     * a model on a site nobody configured are the same absence, and separating them would tell a
     * caller which sites have dotAI set up.</p>
     */
    @Test
    public void test_completions_withNoConfigurationAnywhere_isRefused() throws Exception {
        clearSystemLevelConfiguration();
        final HttpServletRequest request = mockRequest(UNMATCHED_HOST_NAME);

        final Response response = resource.completions(
                request, mockResponse(), unconfiguredSite.getIdentifier(), completionFor(CHAT_MODEL));

        assertNotNull(response);
        assertEquals(404, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView errorView = (InferenceErrorView) response.getEntity();
        assertNotNull(errorView.error());
        assertNotNull(errorView.error().message());
        assertFalse(errorView.error().message().isBlank());
        assertEquals("model", errorView.error().param());

        // Nothing was served from anywhere: no unrelated site's credentials were spent.
        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH)));
    }

    /**
     * Installs the dotAI configuration at system level, where every site inherits it from.
     */
    private static void configureSystemLevel() throws Exception {
        AiTest.aiAppSecretsWithProviderConfig(
                APILocator.systemHost(), AiTest.providerConfigJson(AiTest.PORT, CHAT_MODEL));
    }

    /**
     * Removes the system-level dotAI configuration and waits until it has stopped resolving, so a
     * test asserting the unconfigured case is not racing the secrets cache.
     */
    private static void clearSystemLevelConfiguration() throws Exception {
        AiTest.removeAiAppSecrets(APILocator.systemHost());
        await().atMost(5, SECONDS)
                .until(() -> !ConfigService.INSTANCE.config(APILocator.systemHost()).isEnabled());
    }

    /**
     * Puts SYSTEM_HOST back the way this class found it. The system-level dotAI configuration is
     * shared with every other test in the JVM, so leaving it moved would be a defect in this file
     * rather than in the code under test.
     */
    private static void restoreSystemLevelConfiguration() throws Exception {
        if (originalSystemProviderConfig != null && !originalSystemProviderConfig.isBlank()) {
            AiTest.aiAppSecretsWithProviderConfig(
                    APILocator.systemHost(), originalSystemProviderConfig);
        } else {
            clearSystemLevelConfiguration();
        }
    }

    /**
     * Stubs the OpenAI-compatible provider with a single canned answer; which model or site
     * reached it is not what this file is about, only whether it was reached at all.
     */
    private static void stubProvider() {
        wireMockServer.stubFor(post(urlPathEqualTo(COMPLETIONS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_RESPONSE)));
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
     * @param serverName the host name the caller presented
     * @return a request authenticated with the test user's bearer token, as the family requires
     */
    private static HttpServletRequest mockRequest(final String serverName) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/inference/v1/chat/completions");
        when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://localhost/api/inference/v1/chat/completions"));
        when(request.getMethod()).thenReturn("POST");
        when(request.getServerName()).thenReturn(serverName);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        return request;
    }

    private static HttpServletResponse mockResponse() {
        return mock(HttpServletResponse.class);
    }
}
