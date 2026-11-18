package com.dotcms.inference.rest;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.inference.rest.view.InferenceErrorView;
import com.dotcms.inference.rest.view.ModelListView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Specifies model discovery through
 * {@link ModelsResource#models(HttpServletRequest, HttpServletResponse, String)}.
 *
 * <p>Every reserved alias and every implicit default was removed from this family, which makes
 * this endpoint something callers depend on rather than read: the list it returns is the
 * <em>only</em> way a caller can learn which {@code model} values the chat endpoint will
 * accept. That is what the
 * tests here pin down — that the list is complete, that it is exactly the configured set with
 * nothing invented, that it is drawn from the site the request resolved to, and that an
 * unconfigured instance says so with an empty list rather than with somebody else's models.</p>
 *
 * <ul>
 *     <li>Every entry of a fallback chain is listed, in configured order, because each
 *     one is a real choice a caller may name.</li>
 *     <li>Nothing synthetic is added; the count equals the configured count.</li>
 *     <li>No configuration at either the site or the system level yields an empty
 *     {@code data} array, never another site's models.</li>
 *     <li>Every configured section is listed, chat first, and each entry reports the
 *     {@code type} of the section it came from, so a caller can pick a model for the operation
 *     it is about to call.</li>
 *     <li>Bearer-only authentication and site attribution behave exactly as on the sibling
 *     completions endpoint.</li>
 * </ul>
 *
 * <p>This endpoint contacts no provider — it reads configuration and nothing else. The WireMock
 * server is started only because the dotAI app secrets these tests save must name an endpoint,
 * and no stub on it is ever expected to be hit.</p>
 */
public class InferenceModelsTest {

    /** The chat model the site created for every test is configured with. */
    private static final String CHAT_MODEL = "gpt-4o-mini";

    /** A second, distinct chat model, used to tell one site's configuration from another's. */
    private static final String OTHER_CHAT_MODEL = "claude-sonnet-4-6";

    /** The primary of a fallback chain — the entry a caller wanting "whatever the site runs" takes. */
    private static final String CHAIN_PRIMARY = "gpt-4o-mini";

    /** The fallback of that chain; a name the chat endpoint accepts just as readily. */
    private static final String CHAIN_FALLBACK = "gpt-4o";

    /** A third chain entry, so "the count matches" is not satisfied by a coincidence of two. */
    private static final String CHAIN_LAST_RESORT = "gpt-4o-mini-2024-07-18";

    /** An embeddings model, configured to prove it does not leak into the chat listing. */
    private static final String EMBEDDINGS_MODEL = "text-embedding-3-small";

    /** An image model, configured for the same reason. */
    private static final String IMAGE_MODEL = "dall-e-3";

    /** Object type of the list envelope. */
    private static final String OBJECT_LIST = "list";

    /** Object type of each entry in it. */
    private static final String OBJECT_MODEL = "model";

    /** Owner every entry reports; dotCMS serves the model, whoever built it. */
    private static final String OWNED_BY = "dotcms";

    /** Type of an entry configured in the {@code chat} section. */
    private static final String TYPE_CHAT = "chat";

    /** Type of an entry configured in the {@code embeddings} section — singular, as on the wire. */
    private static final String TYPE_EMBEDDING = "embedding";

    /** Type of an entry configured in the {@code image} section. */
    private static final String TYPE_IMAGE = "image";

    private static WireMockServer wireMockServer;
    private static User user;
    private static String bearerToken;

    /** The site created for each test, configured with {@link #CHAT_MODEL}. */
    private Host host;

    /** Additional sites a test configured, torn down with it. */
    private final List<Host> configuredSites = new ArrayList<>();

    private final ModelsResource resource = new ModelsResource();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();

        // A bare UserDataGen user has no roles at all, so it is neither a backend nor a frontend
        // user, and is rejected; it also cannot read the sites these tests pass as an
        // explicit override, which is also checked. Two roles are needed, not one: the check in
        // WebResource.checkRolePermissions is doesUserHaveRole(user, "DOTCMS_BACK_END_USER") by
        // key and does not walk inheritance, so being an admin does not imply it. Admin is what
        // grants read on the site.
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
        host = siteConfiguredWith(CHAT_MODEL);
    }

    @After
    public void after() throws Exception {
        for (final Host configured : configuredSites) {
            AiTest.removeAiAppSecrets(configured);
        }
        configuredSites.clear();
    }

    /**
     * Given a site configured with a single chat model
     * When the models are listed
     * Then exactly that model comes back, in a {@code list} envelope, as a {@code model} entry
     */
    @Test
    public void test_models_withSingleConfiguredModel_listsThatModel() {
        final Response response = resource.models(
                mockRequest(), mockResponse(), host.getIdentifier());

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ModelListView);

        final ModelListView view = (ModelListView) response.getEntity();
        assertEquals(OBJECT_LIST, view.object());
        assertNotNull(view.data());
        // The site also configures an embeddings and an image model, and both are listed — the
        // chat model is simply first, which is the ordering a caller taking entry zero relies on.
        assertEquals(List.of(CHAT_MODEL, AiTest.EMBEDDINGS_MODEL, AiTest.IMAGE_MODEL),
                modelIds(view));

        final ModelListView.ModelView model = view.data().get(0);
        assertEquals(CHAT_MODEL, model.id());
        assertEquals(OBJECT_MODEL, model.object());
        assertEquals(OWNED_BY, model.ownedBy());
        assertEquals(TYPE_CHAT, model.type());
        assertTrue("An entry must carry a creation time", model.created() > 0);
    }

    /**
     * Given a site whose chat model is a fallback chain
     * When the models are listed
     * Then every entry of the chain is listed, in configured order, the primary first
     *
     * <p>Each entry of the chain is a name the completions endpoint accepts, so a list
     * that showed only the primary would hide choices the caller is entitled to make.</p>
     */
    @Test
    public void test_models_withFallbackChain_listsEveryEntryInOrder() throws Exception {
        final Host chainSite = siteConfiguredWith(CHAIN_PRIMARY + "," + CHAIN_FALLBACK);

        final Response response = resource.models(
                mockRequest(), mockResponse(), chainSite.getIdentifier());

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ModelListView);

        final ModelListView view = (ModelListView) response.getEntity();
        assertEquals(OBJECT_LIST, view.object());
        assertEquals("Every entry of the chat chain is listed, in configured order, ahead of the "
                        + "other capabilities' models",
                List.of(CHAIN_PRIMARY, CHAIN_FALLBACK,
                        AiTest.EMBEDDINGS_MODEL, AiTest.IMAGE_MODEL),
                modelIds(view));
        assertEquals("The primary model is the first entry",
                CHAIN_PRIMARY, view.data().get(0).id());
        view.data().forEach(
                (final ModelListView.ModelView model) -> assertEquals(OBJECT_MODEL, model.object()));
    }

    /**
     * Given a site configured with a three-entry chain and nothing else
     * When the models are listed
     * Then the list is exactly those three — no alias, no placeholder, no invented entry
     *
     * <p>The reserved alias was removed precisely so the list equals the set of acceptable
     * {@code model} values. An extra entry here would be a name the completions endpoint refuses,
     * which is worse than no list at all.</p>
     */
    @Test
    public void test_models_withConfiguredChain_addsNothingSynthetic() throws Exception {
        final List<String> configured =
                List.of(CHAIN_PRIMARY, CHAIN_FALLBACK, CHAIN_LAST_RESORT);
        final Host chainSite = siteConfiguredWith(String.join(",", configured));

        final Response response = resource.models(
                mockRequest(), mockResponse(), chainSite.getIdentifier());

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        final ModelListView view = (ModelListView) response.getEntity();
        final List<String> expected = new ArrayList<>(configured);
        expected.add(AiTest.EMBEDDINGS_MODEL);
        expected.add(AiTest.IMAGE_MODEL);

        assertEquals("The list must hold one entry per configured model, and no more",
                expected.size(), view.data().size());
        assertEquals(expected, modelIds(view));
    }

    /**
     * Given a site with no dotAI configuration, on an instance with none at the system level
     * either
     * When the models are listed
     * Then the answer is a successful, empty list
     *
     * <p>The failure this guards against is not an error but a wrong success: falling
     * back to whichever site happens to be configured would hand a caller model names their own
     * site will refuse, and would disclose that some other site has dotAI set up.</p>
     */
    @Test
    public void test_models_withNoConfigurationAnywhere_returnsEmptyList() {
        // The system level is the only inheritance an unconfigured site has (ConfigService falls
        // back to SYSTEM_HOST). If something else in this JVM has configured it, the condition
        // under test does not hold and the assertion below would be meaningless rather than wrong.
        Assume.assumeFalse("The system level must be unconfigured for this test to mean anything",
                ConfigService.INSTANCE.config(APILocator.systemHost()).isEnabled());

        final Host unconfiguredSite = new SiteDataGen().nextPersisted();

        final Response response = resource.models(
                mockRequest(), mockResponse(), unconfiguredSite.getIdentifier());

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ModelListView);

        final ModelListView view = (ModelListView) response.getEntity();
        assertEquals(OBJECT_LIST, view.object());
        assertNotNull("An unconfigured site gets an empty list, not a null one", view.data());
        assertTrue("An unconfigured site must not be shown another site's models",
                view.data().isEmpty());
    }

    /**
     * Given two sites configured with different chat models
     * When the models are listed with an explicit site override naming the second
     * Then the second site's models come back and the first's do not
     */
    @Test
    public void test_models_withSiteOverride_listsTheResolvedSitesModels() throws Exception {
        final Host otherSite = siteConfiguredWith(OTHER_CHAT_MODEL);

        final Response response = resource.models(
                mockRequest(), mockResponse(), otherSite.getIdentifier());

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        final ModelListView view = (ModelListView) response.getEntity();
        assertEquals(List.of(OTHER_CHAT_MODEL, AiTest.EMBEDDINGS_MODEL, AiTest.IMAGE_MODEL),
                modelIds(view));
        assertFalse("The other site's model must not appear",
                modelIds(view).contains(CHAT_MODEL));
    }

    /**
     * Given an authenticated request carrying an explicit site override
     * When the models are listed
     * Then the resolved site is published on the request, so every response can report it
     *
     * <p>Asserted on the request attribute rather than on the header because invoking the
     * resource method directly never runs the JAX-RS response filter that turns the attribute into
     * {@code X-dotCMS-Resolved-Site} — the attribute is the part this endpoint is responsible
     * for.</p>
     */
    @Test
    public void test_models_withSiteOverride_publishesTheResolvedSiteId() {
        final HttpServletRequest request = mockRequest();

        final Response response = resource.models(
                request, mockResponse(), host.getIdentifier());

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        verify(request).setAttribute(
                InferenceRequestAttributes.RESOLVED_SITE_ID, host.getIdentifier());
    }

    /**
     * Given a request carrying no bearer token
     * When the models are listed
     * Then it is refused as unauthorized, in the standard error shape
     *
     * <p>The model list names the site's configured vendors and models, which is exactly
     * the kind of reconnaissance an anonymous caller should not get for free.</p>
     */
    @Test
    public void test_models_withoutBearerToken_isUnauthorized() {
        final Response response = resource.models(
                anonymousRequest(), mockResponse(), host.getIdentifier());

        assertNotNull(response);
        assertEquals(401, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView errorView = (InferenceErrorView) response.getEntity();
        assertNotNull(errorView.error());
        assertNotNull(errorView.error().message());
        assertFalse(errorView.error().message().isBlank());
    }

    /**
     * Given a site configured with a chat model, a different embeddings model and a different
     * image model
     * When the models are listed
     * Then all three are listed, with the chat model first
     *
     * <p>Every operation in this family requires an exact model name and there is no implicit
     * default, so this listing is the only way a caller can learn one. Listing chat alone — which
     * is what shipped first — left the embeddings and image operations with no discovery at all:
     * a caller had to be told their model names out of band, which is precisely what a discovery
     * endpoint exists to avoid.</p>
     *
     * <p>Chat first is asserted rather than incidental. The documented way to ask for "whatever
     * this site runs" is to read this list and take the first entry, so an ordering that put an
     * embeddings model at the head would silently break every caller following that advice.</p>
     *
     * <p>Which entry serves which operation is reported by each entry's {@code type}, covered by
     * {@link #test_models_withDistinctSectionModels_reportsEachSectionsType()}.</p>
     */
    @Test
    public void test_models_withDistinctSectionModels_listsEveryCapability() throws Exception {
        final Host site = siteConfiguredWith(CHAT_MODEL, EMBEDDINGS_MODEL, IMAGE_MODEL);

        final Response response = resource.models(
                mockRequest(), mockResponse(), site.getIdentifier());

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        final ModelListView view = (ModelListView) response.getEntity();
        assertEquals("Every configured model is discoverable, chat first",
                List.of(CHAT_MODEL, EMBEDDINGS_MODEL, IMAGE_MODEL), modelIds(view));
        assertEquals("A caller taking the first entry must still get the site's primary chat "
                + "model", CHAT_MODEL, modelIds(view).get(0));
    }

    /**
     * Given a site configured with a chat model, a different embeddings model and a different
     * image model
     * When the models are listed
     * Then each entry reports the type of the section it was configured in
     *
     * <p>The model is required on every operation and each operation refuses a model the site
     * configured for a different one, so without this a caller had to learn out of band which
     * listed name was the chat model and which the embeddings one. The value comes from the
     * section, never from the name: {@code text-embedding-3-small} is an embedding model here
     * because the site put it in {@code embeddings}, not because its name says so.</p>
     */
    @Test
    public void test_models_withDistinctSectionModels_reportsEachSectionsType() throws Exception {
        final Host site = siteConfiguredWith(CHAT_MODEL, EMBEDDINGS_MODEL, IMAGE_MODEL);

        final Response response = resource.models(
                mockRequest(), mockResponse(), site.getIdentifier());

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        final ModelListView view = (ModelListView) response.getEntity();
        assertEquals(List.of(CHAT_MODEL, EMBEDDINGS_MODEL, IMAGE_MODEL), modelIds(view));
        assertEquals(List.of(TYPE_CHAT, TYPE_EMBEDDING, TYPE_IMAGE), modelTypes(view));
    }

    /**
     * Given a site whose chat model is a three-entry fallback chain
     * When the models are listed
     * Then every chain entry reports {@code chat}
     *
     * <p>A fallback is as much a chat model as the primary: the chat endpoint accepts any of them
     * by name, so none may be reported as anything else.</p>
     */
    @Test
    public void test_models_withFallbackChain_reportsEveryChainEntryAsChat() throws Exception {
        final Host chainSite = siteConfiguredWith(
                String.join(",", CHAIN_PRIMARY, CHAIN_FALLBACK, CHAIN_LAST_RESORT));

        final Response response = resource.models(
                mockRequest(), mockResponse(), chainSite.getIdentifier());

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals(List.of(TYPE_CHAT, TYPE_CHAT, TYPE_CHAT, TYPE_EMBEDDING, TYPE_IMAGE),
                modelTypes((ModelListView) response.getEntity()));
    }

    /**
     * Given a site that names the same model in its chat and embeddings sections
     * When the models are listed
     * Then it appears once, reported as {@code chat}
     *
     * <p>Clients key a model list by {@code id}, so a repeated id would make one of the two
     * entries unreachable, and which one would depend on the client. One entry typed by the first
     * section it appears in keeps the listing's order rule — chat, then embeddings, then image —
     * as the single rule a caller has to know.</p>
     */
    @Test
    public void test_models_withNameInChatAndEmbeddings_listsItOnceAsChat() throws Exception {
        final Host site = siteConfiguredWith(CHAT_MODEL, CHAT_MODEL, IMAGE_MODEL);

        final Response response = resource.models(
                mockRequest(), mockResponse(), site.getIdentifier());

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        final ModelListView view = (ModelListView) response.getEntity();
        assertEquals(List.of(CHAT_MODEL, IMAGE_MODEL), modelIds(view));
        assertEquals(List.of(TYPE_CHAT, TYPE_IMAGE), modelTypes(view));
    }

    /**
     * Given a site that names the same model in its embeddings and image sections
     * When the models are listed
     * Then it appears once, reported as {@code embedding}
     */
    @Test
    public void test_models_withNameInEmbeddingsAndImage_listsItOnceAsEmbedding()
            throws Exception {
        final Host site = siteConfiguredWith(CHAT_MODEL, EMBEDDINGS_MODEL, EMBEDDINGS_MODEL);

        final Response response = resource.models(
                mockRequest(), mockResponse(), site.getIdentifier());

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        final ModelListView view = (ModelListView) response.getEntity();
        assertEquals(List.of(CHAT_MODEL, EMBEDDINGS_MODEL), modelIds(view));
        assertEquals(List.of(TYPE_CHAT, TYPE_EMBEDDING), modelTypes(view));
    }

    /**
     * Given a site whose sections name the same model
     * When the models are listed
     * Then it appears once
     *
     * <p>A repeat would be harmless to a client but would misrepresent the site as running two
     * things, and a caller counting entries to decide what is available would be wrong.</p>
     */
    @Test
    public void test_models_withOneModelServingEverySection_listsItOnce() throws Exception {
        final Host site = siteConfiguredWith(CHAT_MODEL, CHAT_MODEL, CHAT_MODEL);

        final Response response = resource.models(
                mockRequest(), mockResponse(), site.getIdentifier());

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        final ModelListView view = (ModelListView) response.getEntity();
        assertEquals(List.of(CHAT_MODEL), modelIds(view));
        assertEquals(List.of(TYPE_CHAT), modelTypes(view));
    }

    /**
     * Creates a site whose dotAI chat section names the given model, and registers it for teardown.
     *
     * @param chatModel the chat model, or a comma-separated fallback chain
     * @return the configured site
     */
    private Host siteConfiguredWith(final String chatModel) throws Exception {
        return siteConfiguredWith(chatModel, AiTest.EMBEDDINGS_MODEL, AiTest.IMAGE_MODEL);
    }

    /**
     * Creates a site configuring each section independently, and registers it for teardown.
     *
     * <p>Built here rather than with {@link AiTest#providerConfigJson(int, String)} because that
     * helper fixes the embeddings and image models, and telling the sections apart is the point of
     * one of these tests.</p>
     *
     * @param chatModel       the chat model, or a comma-separated fallback chain
     * @param embeddingsModel the embeddings model
     * @param imageModel      the image model
     * @return the configured site
     */
    private Host siteConfiguredWith(final String chatModel,
                                    final String embeddingsModel,
                                    final String imageModel) throws Exception {
        final Host site = new SiteDataGen().nextPersisted();
        AiTest.aiAppSecretsWithProviderConfig(
                site, providerConfigJson(chatModel, embeddingsModel, imageModel));
        configuredSites.add(site);
        return site;
    }

    /**
     * @param chatModel       the chat model, or a comma-separated fallback chain
     * @param embeddingsModel the embeddings model
     * @param imageModel      the image model
     * @return the dotAI {@code providerConfig} JSON for a site configured that way
     */
    private static String providerConfigJson(final String chatModel,
                                             final String embeddingsModel,
                                             final String imageModel) {
        final String endpoint = String.format("http://localhost:%d/", AiTest.PORT);
        return String.format(
                "{"
                        + "\"chat\":{\"provider\":\"openai\",\"apiKey\":\"%s\",\"model\":\"%s\","
                        + "\"endpoint\":\"%s\",\"maxRetries\":0},"
                        + "\"embeddings\":{\"provider\":\"openai\",\"apiKey\":\"%s\",\"model\":\"%s\","
                        + "\"endpoint\":\"%s\",\"maxRetries\":0},"
                        + "\"image\":{\"provider\":\"openai\",\"apiKey\":\"%s\",\"model\":\"%s\","
                        + "\"endpoint\":\"%s\",\"maxRetries\":0},"
                        + "\"settings\":{\"listenerIndexer\":{\"default\":\"blog\"}}"
                        + "}",
                AiTest.API_KEY, chatModel, endpoint,
                AiTest.API_KEY, embeddingsModel, endpoint,
                AiTest.API_KEY, imageModel, endpoint);
    }

    /**
     * @param view the listing
     * @return the model ids it carries, in the order it carries them
     */
    private static List<String> modelIds(final ModelListView view) {
        assertNotNull(view.data());
        return view.data().stream().map(ModelListView.ModelView::id).toList();
    }

    /**
     * @param view the listing
     * @return the type of each entry it carries, in the order it carries them
     */
    private static List<String> modelTypes(final ModelListView view) {
        assertNotNull(view.data());
        return view.data().stream().map(ModelListView.ModelView::type).toList();
    }

    /**
     * @return a request authenticated with the test user's bearer token, as the family requires
     */
    private static HttpServletRequest mockRequest() {
        final HttpServletRequest request = anonymousRequest();
        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        return request;
    }

    /**
     * @return a request carrying no credential at all
     */
    private static HttpServletRequest anonymousRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/inference/v1/models");
        when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://localhost/api/inference/v1/models"));
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        return request;
    }

    private static HttpServletResponse mockResponse() {
        return mock(HttpServletResponse.class);
    }
}
