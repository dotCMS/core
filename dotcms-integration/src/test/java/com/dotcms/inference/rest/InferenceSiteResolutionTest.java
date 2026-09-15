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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Specifies which site's dotAI credentials serve a request on {@code /api/inference/v1}, and how a
 * caller may say so — User Story 3 of the OpenAI-compatible inference feature.
 *
 * <p>Per-site credential governance is the reason to route AI through dotCMS at all, so the
 * question these tests answer is never "did the call succeed" but "whose provider configuration
 * paid for it". That is asserted structurally rather than by inspecting secrets: the two sites are
 * configured with <em>different chat model names</em>, and
 * {@link ChatCompletionsResource} refuses any model the resolved site has not configured. A 200
 * for site A's model is therefore proof that site A's configuration was the one consulted, and a
 * 404 for site B's model on the same host name is proof that site B's was not.</p>
 *
 * <ul>
 *     <li>FR-017 — with no override, the site comes from the request's host name.</li>
 *     <li>FR-018 — an explicit override wins, given either as the {@code siteId} query parameter
 *     or as the {@code X-dotCMS-Site} header; the header wins when the two disagree.</li>
 *     <li>FR-025 — the legacy {@code host_id} and {@code Host} request parameters, which elsewhere
 *     in dotCMS act as a de-facto site override, are ignored here.</li>
 *     <li>FR-020 — every response identifies the site whose configuration served it.</li>
 * </ul>
 *
 * <p><strong>What is asserted for FR-020.</strong> The serving site reaches a real caller as the
 * {@code X-dotCMS-Resolved-Site} response header, which {@link ResolvedSiteHeaderFilter} writes
 * from the request attribute {@link InferenceRequestAttributes#RESOLVED_SITE_ID}. These tests call
 * the resource method directly, so no JAX-RS response filter runs and no header exists to read.
 * They therefore assert <strong>the request attribute</strong> — the filter's sole input, and the
 * only part of the chain the resource is responsible for.</p>
 */
public class InferenceSiteResolutionTest {

    /** Path an OpenAI-compatible provider serves completions on. */
    private static final String COMPLETIONS_PATH = "/chat/completions";

    private static final String REQUEST_URI = "/api/inference/v1/chat/completions";

    /** Header form of the site override, as {@link ChatCompletionsResource} reads it. */
    private static final String SITE_HEADER = "X-dotCMS-Site";

    /**
     * Legacy request parameter that elsewhere in dotCMS selects the current site. FR-025 requires
     * this family to ignore it.
     */
    private static final String LEGACY_HOST_ID_PARAM = "host_id";

    /**
     * The other legacy site-selecting request parameter. dotCMS reads it under the velocity
     * variable name of the Host content type, {@link Host#HOST_VELOCITY_VAR_NAME}; both that
     * spelling and the lower-case one a caller would more naturally type are stubbed, so the test
     * cannot pass merely because it aimed at the wrong string.
     */
    private static final String LEGACY_HOST_PARAM = Host.HOST_VELOCITY_VAR_NAME;

    private static final String LEGACY_HOST_PARAM_LOWERCASE = "host";

    /** The chat model configured on the first site, and on no other. */
    private static final String MODEL_SITE_A = "gpt-4o-mini";

    /** The chat model configured on the second site, and on no other. */
    private static final String MODEL_SITE_B = "gpt-4o";

    /** What the stubbed provider answers, whichever site's configuration reached it. */
    private static final String PROVIDER_RESPONSE = """
            {
              "id": "chatcmpl-site-1",
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
    private static User user;
    private static String bearerToken;

    private Host siteA;
    private Host siteB;

    private final ChatCompletionsResource resource = new ChatCompletionsResource();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        stubProvider();

        // Two roles, not one. WebResource.checkRolePermissions matches DOTCMS_BACK_END_USER by
        // key and does not walk inheritance, so being an administrator does not imply it; the
        // administrator role is separately what grants READ on the sites used as explicit
        // overrides here. Refusals for callers who lack either live in
        // InferenceAuthorizationTest.
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
        siteA = new SiteDataGen().name(uniqueSiteName("a")).nextPersisted();
        siteB = new SiteDataGen().name(uniqueSiteName("b")).nextPersisted();
        AiTest.aiAppSecretsWithProviderConfig(
                siteA, AiTest.providerConfigJson(AiTest.PORT, MODEL_SITE_A));
        AiTest.aiAppSecretsWithProviderConfig(
                siteB, AiTest.providerConfigJson(AiTest.PORT, MODEL_SITE_B));
        wireMockServer.resetRequests();
    }

    @After
    public void after() throws Exception {
        AiTest.removeAiAppSecrets(siteA);
        AiTest.removeAiAppSecrets(siteB);
    }

    /**
     * Given a request whose host name is the host name of a configured site, and no override
     * When a completion is requested
     * Then that site's configuration serves it — its own model is accepted while the other site's
     * model is refused as unconfigured — and the serving site is reported as that site
     */
    @Test
    public void test_completions_withNoOverride_isServedByTheHostNameSite() {
        final HttpServletRequest request = mockRequest(siteA.getHostname());

        final Response response = resource.completions(
                request, mockResponse(), null, completionFor(MODEL_SITE_A));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);
        assertResolvedSite(request, siteA);

        // The other site's model on the same host name proves the configuration consulted was
        // site A's and not site B's; a call that merely succeeded would prove neither.
        final HttpServletRequest otherModelRequest = mockRequest(siteA.getHostname());
        final Response otherModelResponse = resource.completions(
                otherModelRequest, mockResponse(), null, completionFor(MODEL_SITE_B));

        assertNotNull(otherModelResponse);
        assertEquals(404, otherModelResponse.getStatus());
        assertTrue(otherModelResponse.getEntity() instanceof InferenceErrorView);
        assertResolvedSite(otherModelRequest, siteA);
    }

    /**
     * Given a request whose host name is site A's, carrying {@code siteId} naming site B
     * When a completion is requested for the model only site B has configured
     * Then site B's configuration serves it and site B is reported as the serving site
     */
    @Test
    public void test_completions_withSiteIdQueryParameter_overridesTheHostName() {
        final HttpServletRequest request = mockRequest(siteA.getHostname());

        final Response response = resource.completions(
                request, mockResponse(), siteB.getIdentifier(), completionFor(MODEL_SITE_B));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);
        assertResolvedSite(request, siteB);
    }

    /**
     * Given a request whose host name is site A's, carrying the {@code X-dotCMS-Site} header
     * naming site B by identifier
     * When a completion is requested for the model only site B has configured
     * Then site B's configuration serves it and site B is reported as the serving site
     */
    @Test
    public void test_completions_withSiteHeader_overridesTheHostName() {
        final HttpServletRequest request = mockRequest(siteA.getHostname());
        when(request.getHeader(SITE_HEADER)).thenReturn(siteB.getIdentifier());

        final Response response = resource.completions(
                request, mockResponse(), null, completionFor(MODEL_SITE_B));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);
        assertResolvedSite(request, siteB);
    }

    /**
     * Given a request carrying both overrides, the header naming site B and the query parameter
     * naming site A
     * When a completion is requested for the model only site B has configured
     * Then the header wins: site B serves the request and is reported as the serving site
     */
    @Test
    public void test_completions_withHeaderAndQueryParameterDisagreeing_prefersTheHeader() {
        final HttpServletRequest request = mockRequest(siteA.getHostname());
        when(request.getHeader(SITE_HEADER)).thenReturn(siteB.getIdentifier());

        final Response response = resource.completions(
                request, mockResponse(), siteA.getIdentifier(), completionFor(MODEL_SITE_B));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);
        assertResolvedSite(request, siteB);
    }

    /**
     * Given a request whose host name is site A's, carrying the legacy {@code host_id} request
     * parameter naming site B
     * When a completion is requested for the model only site A has configured
     * Then the legacy parameter is ignored: site A serves the request and is reported as the
     * serving site
     *
     * <p>FR-025. The parameter is a pre-existing de-facto site override on the rest of the
     * product, and a token-authenticated API must not inherit it silently — a caller who never
     * asked for another site must not be able to spend its credentials by copying a query string.
     * The model is site A's, so honouring the parameter cannot merely look like success: it would
     * surface as a 404 for a model site B never configured.</p>
     */
    @Test
    public void test_completions_withLegacyHostIdParameter_ignoresIt() {
        final HttpServletRequest request = mockRequest(siteA.getHostname());
        when(request.getParameter(LEGACY_HOST_ID_PARAM)).thenReturn(siteB.getIdentifier());

        final Response response = resource.completions(
                request, mockResponse(), null, completionFor(MODEL_SITE_A));

        assertNotNull(response);
        assertEquals("The legacy host_id parameter must not select the site; site A's model must "
                        + "still be accepted", 200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);
        assertResolvedSite(request, siteA);
    }

    /**
     * Given a request whose host name is site A's, carrying the legacy {@code Host} request
     * parameter naming site B
     * When a completion is requested for the model only site A has configured
     * Then the legacy parameter is ignored: site A serves the request and is reported as the
     * serving site
     */
    @Test
    public void test_completions_withLegacyHostParameter_ignoresIt() {
        final HttpServletRequest request = mockRequest(siteA.getHostname());
        when(request.getParameter(LEGACY_HOST_PARAM)).thenReturn(siteB.getHostname());
        when(request.getParameter(LEGACY_HOST_PARAM_LOWERCASE)).thenReturn(siteB.getHostname());

        final Response response = resource.completions(
                request, mockResponse(), null, completionFor(MODEL_SITE_A));

        assertNotNull(response);
        assertEquals("The legacy Host parameter must not select the site; site A's model must "
                        + "still be accepted", 200, response.getStatus());
        assertTrue(response.getEntity() instanceof ChatCompletionView);
        assertResolvedSite(request, siteA);
    }

    /**
     * Given a request whose host name matches no site or alias, carrying the legacy
     * {@code host_id} request parameter naming site B
     * When a completion is requested
     * Then the legacy parameter is still ignored: the default site serves the request, exactly as
     * it would have without the parameter
     *
     * <p>FR-025 has no exception for the fallback path, and it is the path that matters most. The
     * unmatched host name is not an edge case here — FR-020 keeps the default-site fallback
     * precisely because the server-side callers this family exists for routinely arrive on
     * internal DNS, container service names or {@code localhost}, none of which are site aliases.
     * A legacy override that is ignored only while the host name happens to match is not ignored;
     * it is ignored where it could do no harm and honoured where it could.</p>
     */
    @Test
    public void test_completions_withLegacyHostIdParameterAndUnmatchedHostName_ignoresIt()
            throws Exception {
        final Host defaultSite = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final HttpServletRequest request = mockRequest(unmatchedHostName());
        when(request.getParameter(LEGACY_HOST_ID_PARAM)).thenReturn(siteB.getIdentifier());

        resource.completions(request, mockResponse(), null, completionFor(MODEL_SITE_A));

        assertEquals("The legacy host_id parameter must not select the site on the default-site "
                        + "fallback path either; it named site B (" + siteB.getIdentifier() + ")",
                defaultSite.getIdentifier(),
                request.getAttribute(InferenceRequestAttributes.RESOLVED_SITE_ID));
    }

    /**
     * Given a request whose host name matches no site or alias, carrying the legacy {@code Host}
     * request parameter naming site B
     * When a completion is requested
     * Then the legacy parameter is still ignored and the default site serves the request
     */
    @Test
    public void test_completions_withLegacyHostParameterAndUnmatchedHostName_ignoresIt()
            throws Exception {
        final Host defaultSite = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final HttpServletRequest request = mockRequest(unmatchedHostName());
        when(request.getParameter(LEGACY_HOST_PARAM)).thenReturn(siteB.getHostname());
        when(request.getParameter(LEGACY_HOST_PARAM_LOWERCASE)).thenReturn(siteB.getHostname());

        resource.completions(request, mockResponse(), null, completionFor(MODEL_SITE_A));

        assertEquals("The legacy Host parameter must not select the site on the default-site "
                        + "fallback path either; it named site B (" + siteB.getIdentifier() + ")",
                defaultSite.getIdentifier(),
                request.getAttribute(InferenceRequestAttributes.RESOLVED_SITE_ID));
    }

    /**
     * Given a request that will be refused because the model is not configured for its site
     * When the completion is requested
     * Then the serving site is still reported
     *
     * <p>FR-020 asks for the serving site on <em>every</em> response, not only the ones that
     * worked. A refusal is precisely the case an operator reconciling spend needs attributed, and
     * a value only the happy path published would not deliver it.</p>
     */
    @Test
    public void test_completions_whenRefused_stillReportsTheServingSite() {
        final HttpServletRequest request = mockRequest(siteA.getHostname());

        final Response response = resource.completions(
                request, mockResponse(), null, completionFor("a-model-nobody-configured"));

        assertNotNull(response);
        assertEquals(404, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);
        assertResolvedSite(request, siteA);
    }

    /**
     * Asserts the site the resource published as having served the request.
     *
     * @param request  the request the resource was called with
     * @param expected the site expected to have served it
     */
    private static void assertResolvedSite(final HttpServletRequest request, final Host expected) {
        assertEquals("The resolved-site request attribute, which ResolvedSiteHeaderFilter turns "
                        + "into the X-dotCMS-Resolved-Site header, must name the serving site",
                expected.getIdentifier(),
                request.getAttribute(InferenceRequestAttributes.RESOLVED_SITE_ID));
    }

    /** Answers every completion with the same finished response; the site under test is the variable. */
    private static void stubProvider() {
        wireMockServer.stubFor(post(urlPathEqualTo(COMPLETIONS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_RESPONSE)));
    }

    /**
     * @param model the model to ask for
     * @return the smallest well-formed completion request
     */
    private static ChatCompletionRequestView completionFor(final String model) {
        return new ChatCompletionRequestView(
                model,
                List.of(new MessageView("user", "Which site is serving me?", null, null, null)),
                null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * @param prefix distinguishes the two sites
     * @return a host name no other test or run can collide with
     */
    private static String uniqueSiteName(final String prefix) {
        return "inference-" + prefix + "-" + UUID.randomUUID() + ".dotcms.com";
    }

    /**
     * @return a host name no site or alias can match, standing in for the internal DNS and
     *         container service names the server-side callers of this family actually arrive on
     */
    private static String unmatchedHostName() {
        return "no-such-site-" + UUID.randomUUID() + ".invalid";
    }

    /**
     * Builds a bearer-authenticated request arriving at a given host name.
     *
     * <p>Request attributes are backed by a real map rather than left as mock no-ops, because the
     * resolved site is published as an attribute and reading it back is how these tests observe
     * which site served.</p>
     *
     * @param serverName the host name the request arrives on
     * @return the mocked request
     */
    private static HttpServletRequest mockRequest(final String serverName) {
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
