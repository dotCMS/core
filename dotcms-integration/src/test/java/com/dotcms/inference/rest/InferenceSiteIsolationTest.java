package com.dotcms.inference.rest;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.inference.rest.view.ChatCompletionRequestView;
import com.dotcms.inference.rest.view.ChatCompletionRequestView.MessageView;
import com.dotcms.inference.rest.view.ChatCompletionView;
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Specifies cross-site isolation for
 * {@link ChatCompletionsResource#completions(HttpServletRequest, HttpServletResponse, String, ChatCompletionRequestView)}
 * — FR-027 and FR-028.
 *
 * <p>Isolation here is not enforced by a check anywhere; it is a property of one line, the cache
 * key {@code host + ":" + providerConfigHash} in
 * {@link com.dotcms.ai.client.langchain4j.LangChain4jAIClient}. That makes it exactly the kind of
 * behaviour that survives refactoring right up until it silently does not: nothing throws when two
 * sites start sharing a provider instance, and nothing throws when a rotated credential keeps
 * being used until the cache's one-hour TTL expires. The failure mode is a site's key spending
 * another site's budget, or a revoked key continuing to work, and neither shows up anywhere but
 * here.</p>
 *
 * <p>So each site is given a provider at a <strong>different URL path</strong> on the same
 * WireMock server, answering with its own text. Where a request landed is then a fact, not an
 * inference: the path WireMock recorded says which site's configuration built the client that
 * made it.</p>
 *
 * <ul>
 *     <li>FR-028 — two sites with different provider configurations, driven with interleaved
 *     requests, are each served by their own provider.</li>
 *     <li>FR-028 — rotating one site's configuration takes effect on its next request and leaves
 *     the other site untouched.</li>
 *     <li>FR-020 / FR-027 — a completion never reports another site's resolved site id.</li>
 * </ul>
 */
public class InferenceSiteIsolationTest {

    /** URL path segment the first site's provider is reached on. */
    private static final String ALPHA_PATH = "/alpha/chat/completions";

    /** URL path segment the second site's provider is reached on. */
    private static final String BETA_PATH = "/beta/chat/completions";

    /** Where the first site's provider moves to when its configuration is rotated. */
    private static final String ALPHA_ROTATED_PATH = "/alpha-rotated/chat/completions";

    private static final String ALPHA_MODEL = "alpha-chat-model";
    private static final String BETA_MODEL = "beta-chat-model";

    private static final String ALPHA_ANSWER = "Answer from the ALPHA provider.";
    private static final String BETA_ANSWER = "Answer from the BETA provider.";
    private static final String ALPHA_ROTATED_ANSWER = "Answer from the ROTATED alpha provider.";

    private static WireMockServer wireMockServer;
    private static User user;
    private static String bearerToken;

    private Host alphaSite;
    private Host betaSite;
    private final ChatCompletionsResource resource = new ChatCompletionsResource();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        stubProviders();

        // Both roles are needed, not one: WebResource.checkRolePermissions matches
        // DOTCMS_BACK_END_USER by key without walking inheritance, so being an admin does not
        // imply it, and admin is what grants read on the sites passed as explicit overrides.
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
    public static void afterClass() {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    @Before
    public void before() throws Exception {
        alphaSite = new SiteDataGen().nextPersisted();
        betaSite = new SiteDataGen().nextPersisted();
        configure(alphaSite, "alpha", ALPHA_MODEL);
        configure(betaSite, "beta", BETA_MODEL);
        wireMockServer.resetRequests();
    }

    @After
    public void after() throws Exception {
        AiTest.removeAiAppSecrets(alphaSite);
        AiTest.removeAiAppSecrets(betaSite);
    }

    /**
     * Given two sites whose dotAI configurations name different providers
     * When completions for the two are interleaved
     * Then each answer comes from that site's own provider, and each provider saw only the
     * requests belonging to its site
     */
    @Test
    public void test_completions_forTwoSitesInterleaved_eachUsesItsOwnProvider() {
        final String firstAlpha = contentOf(complete(alphaSite, ALPHA_MODEL));
        final String firstBeta = contentOf(complete(betaSite, BETA_MODEL));
        final String secondAlpha = contentOf(complete(alphaSite, ALPHA_MODEL));
        final String secondBeta = contentOf(complete(betaSite, BETA_MODEL));

        assertEquals(ALPHA_ANSWER, firstAlpha);
        assertEquals(ALPHA_ANSWER, secondAlpha);
        assertEquals(BETA_ANSWER, firstBeta);
        assertEquals(BETA_ANSWER, secondBeta);
        assertNotEquals(firstAlpha, firstBeta);

        // Where each request landed, rather than what it happened to say.
        wireMockServer.verify(2, postRequestedFor(urlPathEqualTo(ALPHA_PATH)));
        wireMockServer.verify(2, postRequestedFor(urlPathEqualTo(BETA_PATH)));
    }

    /**
     * Given two configured sites, one of which has already served a request and therefore has a
     * cached provider instance
     * When that site's dotAI secrets are re-saved with a different provider configuration
     * Then its next request uses the new configuration, and the other site is unaffected
     *
     * <p>Two mechanisms should each make this hold on their own, which is why the test asserts the
     * outcome rather than either of them: the cache key carries the configuration's hash, so a
     * changed configuration is a different key, and {@code AIAppListener} additionally flushes the
     * site's entries when its secrets are saved. A regression in one is invisible while the other
     * still works — but a regression in both is a revoked credential that keeps working for an
     * hour, so what is pinned here is the observable result.</p>
     */
    @Test
    public void test_completions_afterRotatingOneSitesConfiguration_usesTheNewOneAndLeavesTheOtherAlone()
            throws Exception {
        // Warm the cache: without a first request there is no stale instance to invalidate and the
        // test would pass on an implementation that never invalidates anything.
        assertEquals(ALPHA_ANSWER, contentOf(complete(alphaSite, ALPHA_MODEL)));
        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(ALPHA_PATH)));

        configure(alphaSite, "alpha-rotated", ALPHA_MODEL);
        wireMockServer.resetRequests();

        assertEquals(ALPHA_ROTATED_ANSWER, contentOf(complete(alphaSite, ALPHA_MODEL)));
        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(ALPHA_ROTATED_PATH)));
        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(ALPHA_PATH)));

        // The other site's provider was never rebuilt and never moved.
        assertEquals(BETA_ANSWER, contentOf(complete(betaSite, BETA_MODEL)));
        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(BETA_PATH)));
    }

    /**
     * Given two configured sites
     * When a completion is requested for each
     * Then each publishes its own site as the one that served, and never the other's
     *
     * <p>The serving site is what FR-020 puts on every response and what a reconciliation
     * pipeline would bill against, so reporting a neighbour's id would be a billing error rather
     * than a cosmetic one.</p>
     */
    @Test
    public void test_completions_forEitherSite_neverReportsTheOtherSitesResolvedSiteId() {
        final HttpServletRequest alphaRequest = mockRequest();
        final HttpServletRequest betaRequest = mockRequest();

        resource.completions(alphaRequest, mockResponse(),
                alphaSite.getIdentifier(), completionFor(ALPHA_MODEL));
        resource.completions(betaRequest, mockResponse(),
                betaSite.getIdentifier(), completionFor(BETA_MODEL));

        verify(alphaRequest).setAttribute(
                InferenceRequestAttributes.RESOLVED_SITE_ID, alphaSite.getIdentifier());
        verify(alphaRequest, never()).setAttribute(
                InferenceRequestAttributes.RESOLVED_SITE_ID, betaSite.getIdentifier());

        verify(betaRequest).setAttribute(
                InferenceRequestAttributes.RESOLVED_SITE_ID, betaSite.getIdentifier());
        verify(betaRequest, never()).setAttribute(
                InferenceRequestAttributes.RESOLVED_SITE_ID, alphaSite.getIdentifier());
    }

    /**
     * Runs one completion for a site and asserts only that it was served, so the tests above can
     * assert what matters about it.
     *
     * @param site  the site to bill
     * @param model the model that site has configured
     * @return the completed answer
     */
    private ChatCompletionView complete(final Host site, final String model) {
        final Response response = resource.completions(
                mockRequest(), mockResponse(), site.getIdentifier(), completionFor(model));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);
        return (ChatCompletionView) response.getEntity();
    }

    /**
     * @param view a completed answer
     * @return the assistant's text, which identifies the provider that produced it
     */
    private static String contentOf(final ChatCompletionView view) {
        assertNotNull(view.choices());
        assertEquals(1, view.choices().size());
        assertNotNull(view.choices().get(0).message());
        return view.choices().get(0).message().content();
    }

    /**
     * Points a site's dotAI configuration at one of the stubbed providers, and waits until that
     * configuration is the one being resolved — re-saving alone would leave a rotation test racing
     * the secrets cache.
     *
     * @param site        the site to configure
     * @param pathSegment the provider's URL path segment on the WireMock server
     * @param model       the chat model the site accepts
     */
    private static void configure(final Host site, final String pathSegment, final String model)
            throws Exception {
        final String json = providerConfigJson(pathSegment, model);
        AiTest.aiAppSecretsWithProviderConfig(site, json);
        await().atMost(5, SECONDS).until(() -> {
            final String resolved = ConfigService.INSTANCE.config(site).getProviderConfig();
            return resolved != null && resolved.contains("/" + pathSegment + "/");
        });
    }

    /**
     * Builds a chat-only dotAI provider configuration whose endpoint is distinct per site, so the
     * path a request arrives on names the configuration that produced it.
     *
     * @param pathSegment the provider's URL path segment on the WireMock server
     * @param model       the chat model
     * @return the {@code providerConfig} JSON
     */
    private static String providerConfigJson(final String pathSegment, final String model) {
        return String.format(
                "{\"chat\":{\"provider\":\"openai\",\"apiKey\":\"%s\",\"model\":\"%s\","
                        + "\"endpoint\":\"http://localhost:%d/%s/\",\"maxRetries\":0}}",
                AiTest.API_KEY, model, AiTest.PORT, pathSegment);
    }

    /**
     * Stubs three OpenAI-compatible providers on one server, each on its own path and each
     * answering with text that names it.
     */
    private static void stubProviders() {
        stubProvider(ALPHA_PATH, ALPHA_ANSWER);
        stubProvider(BETA_PATH, BETA_ANSWER);
        stubProvider(ALPHA_ROTATED_PATH, ALPHA_ROTATED_ANSWER);
    }

    private static void stubProvider(final String path, final String answer) {
        wireMockServer.stubFor(post(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(providerResponse(answer))));
    }

    /**
     * @param answer the assistant text this provider replies with
     * @return an OpenAI-compatible completion payload carrying it
     */
    private static String providerResponse(final String answer) {
        return String.format("""
                {
                  "id": "chatcmpl-isolation",
                  "object": "chat.completion",
                  "created": 1789000000,
                  "model": "stubbed",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "%s"
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {"prompt_tokens": 9, "completion_tokens": 6, "total_tokens": 15}
                }
                """, answer);
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
     * @return a request authenticated with the test user's bearer token, as the family requires
     */
    private static HttpServletRequest mockRequest() {
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
