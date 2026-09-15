package com.dotcms.inference.rest;

import com.dotcms.ai.AiTest;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.inference.model.InferenceLimits;
import com.dotcms.inference.rest.view.ImageGenerationRequestView;
import com.dotcms.inference.rest.view.ImageGenerationView;
import com.dotcms.inference.rest.view.InferenceErrorView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Specifies {@code POST /api/inference/v1/images/generations} through
 * {@link ImagesResource#generations(HttpServletRequest, HttpServletResponse, String, ImageGenerationRequestView)}.
 *
 * <p>Two decisions in FR-012 are the reason this file exists rather than a copy of the chat tests
 * with a different noun.</p>
 *
 * <p><strong>The image comes back inline, as base64, and never as a URL.</strong> That is a
 * deliberate choice and not an incidental one: a hosted URL would mean deciding storage,
 * authentication and lifetime for an artifact generated from a prompt that may carry customer
 * data, so the family avoids creating a separately-addressable artifact at all. A decision like
 * that reverses quietly — an implementation that passes the provider's {@code url} straight
 * through returns a 200 with a perfectly plausible body — so it is asserted on the serialized wire
 * shape, where a {@code url} property appearing later is caught whether or not anyone remembers
 * why it must not.</p>
 *
 * <p><strong>Deliberately not asserted here: that dotCMS asks the provider for base64 upstream.</strong>
 * FR-012 does require it — a provider-minted URL is a real artifact on someone else's
 * infrastructure, frequently public and long-lived, which is a larger exposure than anything this
 * family's own response shape can create — but it requires it only "where a provider offers the
 * choice", and the seven configured vendors do not agree on whether that choice exists or what it
 * is called. Where no such choice exists the requirement says the opposite thing: dotCMS fetches
 * the provider's URL and re-encodes it rather than refusing the provider, and FR-012 now states
 * the residual openly — on those providers an addressable artifact really does exist upstream, and
 * this family's guarantee is only that a caller never receives one. An assertion on the outbound
 * request body would therefore pin one vendor's spelling of a field the requirement itself makes
 * conditional, and would fail the moment the provider abstraction changed how it phrases a request
 * it is still phrasing correctly. It is the same reason the streaming tests stopped asserting that
 * {@code include_usage} never reached the provider: what is on the wire upstream belongs to the
 * client library, and testing it tests the library. What this family owns, and what is asserted
 * below, is that the <em>response</em> a caller receives carries {@code b64_json} and no
 * {@code url}. This paragraph exists so that the absence reads as a decision rather than as an
 * oversight somebody helpfully corrects.</p>
 *
 * <p><strong>{@code size}, by contrast, is asserted on the outbound request</strong>, and the
 * difference is not inconsistency. Asking for the inline form is a provider capability — whether
 * the ask exists at all varies by vendor, so a test of it is a test of the vendor. Passing a
 * caller's {@code size} through is dotCMS's own behaviour: the caller named a size, FR-012 says it
 * reaches the provider "where the provider accepts it", and the stub here is a provider that
 * accepts it. It is the same logic FR-013 applies to the sampling parameters — {@code size}
 * changes both what the caller receives and what the site pays for, so dropping it silently is a
 * cost and correctness failure rather than a compatibility courtesy, and a dropped {@code size} is
 * invisible in the response: a 1024x1024 image is a perfectly plausible answer to a request for
 * something else.</p>
 *
 * <p><strong>The model is validated against the site's {@code image} section</strong>, not its
 * chat models and not its embeddings model. A site configures all three separately, so reusing the
 * chat gate here would accept the wrong model and refuse the right one while still answering
 * 200/404 in the right shapes.</p>
 *
 * <ul>
 *     <li>FR-012 — a prompt returns the standard image-generation shape: a {@code created}
 *     timestamp and one entry in {@code data}.</li>
 *     <li>FR-012 — that entry carries {@code b64_json} and carries no hosted {@code url}.</li>
 *     <li>FR-012 — {@code n} is honored: a request for two images answers with two distinct
 *     entries in {@code data}, while {@code n} of exactly 1 and {@code n} omitted are each one
 *     image.</li>
 *     <li>FR-012 — an {@code n} below 1 is refused naming the field — zero and negative alike.</li>
 *     <li>FR-012 / FR-013 — {@code size} reaches the provider rather than being dropped.</li>
 *     <li>FR-023 / R7 — {@code model} is validated against the site's <strong>image</strong>
 *     section; the site's chat and embeddings models are both refused here.</li>
 *     <li>FR-024 — {@code model} is required, and so is {@code prompt}; there is nothing to
 *     generate without one.</li>
 *     <li>FR-015 — an anonymous caller is refused.</li>
 * </ul>
 *
 * <p>The provider is a WireMock server standing in for an OpenAI-compatible endpoint, wired in
 * through the same dotAI app secrets the rest of the AI integration tests use, so an accepted call
 * travels the real client path rather than a stubbed one.</p>
 */
public class InferenceImagesTest {

    /**
     * The model the site's {@code image} section is configured with. Read from {@link AiTest}
     * rather than restated, because the three-way separation this file relies on is a property of
     * that shared configuration.
     */
    private static final String IMAGE_MODEL = AiTest.IMAGE_MODEL;

    /** The model the same site's {@code embeddings} section is configured with. */
    private static final String EMBEDDINGS_MODEL = AiTest.EMBEDDINGS_MODEL;

    /** The model the same site's {@code chat} section is configured with. */
    private static final String CHAT_MODEL = "gpt-4o-mini";

    /** Path an OpenAI-compatible provider serves image generation on. */
    private static final String IMAGES_PATH = "/images/generations";

    private static final String REQUEST_URI = "/api/inference/v1/images/generations";

    private static final String ERROR_TYPE_INVALID_REQUEST = "invalid_request_error";

    /** The prompt these tests generate from. */
    private static final String PROMPT = "A cat in a hammock";

    /** The size these tests ask for; the one every configured provider supports. */
    private static final String SIZE = AiTest.IMAGE_SIZE;

    /**
     * A second size the image model supports, asked for only by the pass-through test.
     *
     * <p>It has to differ from {@link #SIZE} for that test to mean anything: a request that asks
     * for the default cannot distinguish a size that travelled from a size the provider would have
     * used anyway. Neither string is a substring of the other, so the body match that separates the
     * two stubs cannot fire on the wrong request.</p>
     */
    private static final String ALTERNATE_SIZE = "1792x1024";

    /** A 1x1 PNG. Small enough to read, and real base64, so decoding it proves something. */
    private static final String IMAGE_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQ"
                    + "AAAABJRU5ErkJggg==";

    /**
     * A different 1x1 PNG — a second real image, distinct from {@link #IMAGE_BASE64} — served only
     * by the stub that matches on {@link #ALTERNATE_SIZE}. It is what makes "the size reached the
     * provider" visible in the response as well as in the request log: an implementation that drops
     * {@code size} falls through to the catch-all stub and comes back with the other image.
     */
    private static final String ALTERNATE_IMAGE_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9aw"
                    + "AAAABJRU5ErkJggg==";

    /** How many images the multi-image test asks for: the smallest number that is not one. */
    private static final int TWO_IMAGES = 2;

    /** An image model on a provider that implements only the single-image call. */
    private static final String GEMINI_IMAGE_MODEL = "gemini-2.5-flash-image";

    /**
     * What a provider request that really asked for {@link #TWO_IMAGES} images carries, and the
     * body match that separates the two-image stub from the single-image one.
     *
     * <p>It has to be the outbound {@code n} rather than anything about the prompt: a stub keyed on
     * the prompt would answer two images to an implementation that never told the provider how many
     * it wanted, and the test would pass on a response the provider was never asked for.</p>
     *
     * <p>Matched as a JSON path rather than as a substring of the body. The provider client writes
     * its request pretty-printed — {@code "n" : 2}, with spaces around the colon — so a substring
     * match on {@code "n":2} silently fails to match a request that did carry the field, falls
     * through to the single-image stub, and reports the implementation as returning one image when
     * what actually went wrong was the assertion. A path match is indifferent to formatting.</p>
     */
    private static final String TWO_IMAGES_MARKER = "$[?(@.n == " + TWO_IMAGES + ")]";

    /**
     * A third 1x1 PNG, distinct from both {@link #IMAGE_BASE64} and {@link #ALTERNATE_IMAGE_BASE64},
     * carried by the second entry of {@link #PROVIDER_TWO_IMAGES_RESPONSE}.
     *
     * <p>The second entry has to be a different image from the first for the multi-image test to
     * mean anything: two copies of one payload would satisfy "two entries in {@code data}" while
     * the implementation generated once and duplicated the answer. It is the same hole the batch
     * embeddings test closes by asserting that its vectors are distinct.</p>
     */
    private static final String SECOND_IMAGE_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR42mNg+M8AAAICAQBF9FLUAA"
                    + "AAAElFTkSuQmCC";

    /**
     * What the stubbed provider answers. Modelled on the checked-in dotAI image stubs under
     * {@code src/test/resources/mappings} — {@code created}, and a {@code data} entry carrying the
     * provider's rewritten prompt — except that the payload is the image itself rather than a
     * hosted link, which is what an OpenAI-compatible provider returns for a base64 request and
     * what FR-012 requires this family to deal in.
     */
    private static final String PROVIDER_RESPONSE = """
            {
              "created": 1789000000,
              "data": [
                {
                  "revised_prompt": "A contented cat asleep in a woven hammock, warm afternoon light.",
                  "b64_json": "%s"
                }
              ]
            }
            """.formatted(IMAGE_BASE64);

    /**
     * What the stubbed provider answers a request that carried {@link #ALTERNATE_SIZE}: the same
     * shape, a different image. Only a request whose body really contains the size the caller asked
     * for can reach this stub — see {@link #stubProvider()}.
     */
    private static final String PROVIDER_ALTERNATE_SIZE_RESPONSE = """
            {
              "created": 1789000000,
              "data": [
                {
                  "revised_prompt": "A contented cat asleep in a wide woven hammock, warm afternoon light.",
                  "b64_json": "%s"
                }
              ]
            }
            """.formatted(ALTERNATE_IMAGE_BASE64);

    /**
     * What the stubbed provider answers a request that really asked for {@link #TWO_IMAGES}
     * images: two entries, each carrying its own image, which is what an OpenAI-compatible
     * provider returns for an {@code n} of two. Only a request whose body carries
     * {@link #TWO_IMAGES_MARKER} can reach this stub — see {@link #stubProvider()}.
     */
    private static final String PROVIDER_TWO_IMAGES_RESPONSE = """
            {
              "created": 1789000000,
              "data": [
                {
                  "revised_prompt": "A contented cat asleep in a woven hammock, warm afternoon light.",
                  "b64_json": "%s"
                },
                {
                  "revised_prompt": "A tabby cat curled in a rope hammock, late afternoon sun.",
                  "b64_json": "%s"
                }
              ]
            }
            """.formatted(IMAGE_BASE64, SECOND_IMAGE_BASE64);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static WireMockServer wireMockServer;

    /** A backend user who is also an administrator, so site READ is never the thing under test. */
    private static User user;
    private static String bearerToken;

    private Host host;
    private final ImagesResource resource = new ImagesResource();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        stubProvider();

        // A bare UserDataGen user has no roles at all, so it is neither a backend nor a frontend
        // user and FR-016 rejects it; it also cannot read the site these tests pass as an explicit
        // override, which FR-019 checks. Two roles are needed, not one: the check in
        // WebResource.checkRolePermissions is doesUserHaveRole(user, "DOTCMS_BACK_END_USER") by key
        // and does not walk inheritance, so being an admin does not imply it. Admin is what grants
        // read on the site. Role-specific behaviour is US3's tests, not these.
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
        host = new SiteDataGen()
                .name("inference-images-" + UUID.randomUUID() + ".dotcms.com")
                .nextPersisted();
        // Configures chat, embeddings and image as three separate sections, each with its own
        // model. The chat and embeddings models given here are the ones this file expects the
        // image gate to refuse.
        AiTest.aiAppSecretsWithProviderConfig(
                host, AiTest.providerConfigJson(AiTest.PORT, CHAT_MODEL));
        wireMockServer.resetRequests();
    }

    @After
    public void after() throws Exception {
        AiTest.removeAiAppSecrets(host);
    }

    /**
     * Given a site configured with an image model, and a request carrying a prompt
     * When the image is requested
     * Then it comes back in the standard image-generation shape — a {@code created} timestamp and
     * one entry in {@code data}
     */
    @Test
    public void test_generations_withPrompt_returnsImageInStandardShape() {
        final Response response = resource.generations(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), generationFor(IMAGE_MODEL, PROMPT));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ImageGenerationView);

        final ImageGenerationView view = (ImageGenerationView) response.getEntity();
        assertTrue("The answer must carry a creation timestamp", view.created() > 0);
        assertNotNull(view.data());
        assertEquals(1, view.data().size());
        assertNotNull(view.data().get(0));

        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(IMAGES_PATH)));
    }

    /**
     * Given a generated image
     * When the answer is serialized as a client would receive it
     * Then the entry carries a non-blank, decodable {@code b64_json} payload and no hosted
     * {@code url}
     *
     * <p>FR-012. The assertion is made against the serialized wire shape rather than against the
     * view's accessors, because that is where the decision can be undone: a {@code url} property
     * added to the payload later — by widening the view, or by letting the provider's own entry
     * through unmapped — would restore exactly the separately-addressable artifact the requirement
     * refuses to create, while every status-code and shape assertion in this file kept passing.
     * Decoding the payload is what separates a real image from a placeholder string that merely
     * occupies the field.</p>
     */
    @Test
    public void test_generations_withGeneratedImage_returnsBase64AndNoHostedUrl() {
        final Response response = resource.generations(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), generationFor(IMAGE_MODEL, PROMPT));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ImageGenerationView);

        final ImageGenerationView view = (ImageGenerationView) response.getEntity();
        final String b64Json = view.data().get(0).b64Json();
        assertNotNull("FR-012 returns the image inline, so b64_json is not optional", b64Json);
        assertFalse(b64Json.isBlank());

        try {
            assertTrue("An empty payload is not an image",
                    Base64.getDecoder().decode(b64Json).length > 0);
        } catch (final IllegalArgumentException e) {
            fail("b64_json must be valid base64, not an opaque placeholder: " + e.getMessage());
        }

        final JsonNode payload = OBJECT_MAPPER.valueToTree(view);
        final JsonNode entry = payload.get("data").get(0);
        assertTrue("The wire payload names the image b64_json, as standard clients read it",
                entry.hasNonNull("b64_json"));
        assertEquals(b64Json, entry.get("b64_json").asText());
        assertFalse("FR-012 creates no hosted, separately-addressable artifact, so no url may "
                + "reach the caller", entry.hasNonNull("url"));
    }

    /**
     * Given requests asking for no images and for a negative number of them
     * When each generation is requested
     * Then each is refused as a client error naming {@code n}, and the provider is never contacted
     *
     * <p>FR-012 draws its line below 1, not around it: zero and a negative are not quantities of
     * images a caller can mean, and they are the two ways of expressing that. They are also what a
     * clamp swallows — {@code Math.max(1, n)} answers a request for no images with an image and a
     * request for minus one with a bill, while every other assertion in this file keeps passing.
     * Zero is not an empty success either: a caller who sends it has made a mistake, and answering
     * it with an empty {@code data} list would be the same false success FR-011 refuses for an
     * empty batch of embeddings.</p>
     *
     * <p>That is why the provider must not be contacted at all. Clamping is the plausible
     * implementation of this field — the request reaches the provider once, a perfectly valid
     * single-image 200 comes back, and every other assertion in this file still passes. A refusal
     * that never leaves dotCMS is the only observable difference.</p>
     *
     * <p>An {@code n} of 2 is deliberately not among these values. An earlier draft of FR-012
     * refused any {@code n} other than 1, on the claim that the provider abstraction returns a
     * single image per call; the claim was false — {@code ImageModel.generate(prompt, n)} returns a
     * list, and the adopted format documents several images per request — and the requirement was
     * corrected. Two images are served, by
     * {@link #test_generations_withNOfTwo_returnsTwoDistinctImages()}.</p>
     */
    @Test
    public void test_generations_withNBelowOne_isRejectedNamingTheField() {
        assertCountRefusedNamingTheField(0);
        assertCountRefusedNamingTheField(-1);
    }

    /**
     * Given a request asking for two images
     * When the generation is requested
     * Then two entries come back in {@code data}, each carrying its own non-blank {@code b64_json},
     * and the two are not the same image
     *
     * <p>FR-012 as corrected: {@code n} is honored, because the adopted format supports several
     * images in one request and this site's provider implements the multi-image call. Refusing would be
     * this family declining something both the standard and this provider support, so the count a
     * caller asks for is the count they receive. A provider that cannot is a different case, with
     * its own test below.</p>
     *
     * <p>The two payloads are asserted to <em>differ</em>, not merely to be present. "Two entries"
     * is a count an implementation can reach without generating twice — by asking the provider for
     * one image and copying the answer into a list of the right length — and that implementation
     * returns the caller one image under two headings while passing every shape assertion in this
     * file. It is the same hole the batch embeddings test closes by asserting that its three
     * vectors are distinct.</p>
     *
     * <p>The outbound request is verified as well, for the reason the {@code size} test gives: what
     * the provider was asked for is invisible in the response. The stub that serves two images is
     * keyed on that outbound {@code n}, so an implementation that never sends it falls through to
     * the single-image stub and fails on the count too — and one that loops, calling the provider
     * once per image, doubles the round trips and leaves a request half-charged when the second
     * call fails after the first succeeded, which the single matching call catches.</p>
     */
    @Test
    public void test_generations_withNOfTwo_returnsTwoDistinctImages() {
        final Response response = resource.generations(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), generationFor(IMAGE_MODEL, PROMPT, TWO_IMAGES));

        assertNotNull(response);
        assertEquals("n is honored, so a request for two images is served rather than refused",
                200, response.getStatus());
        assertTrue(response.getEntity() instanceof ImageGenerationView);

        final ImageGenerationView view = (ImageGenerationView) response.getEntity();
        assertTrue("The answer must carry a creation timestamp", view.created() > 0);
        assertNotNull(view.data());
        assertEquals("A request for two images is two entries in data, not one",
                TWO_IMAGES, view.data().size());

        for (int index = 0; index < TWO_IMAGES; index++) {
            final ImageGenerationView.ImageView entry = view.data().get(index);
            assertNotNull("Entry " + index + " is missing entirely", entry);
            assertNotNull("FR-012 returns every image inline, so entry " + index
                    + " carries a b64_json", entry.b64Json());
            assertFalse("A blank payload is not an image, and entry " + index + " carries one",
                    entry.b64Json().isBlank());
        }

        assertEquals("Each entry must carry a distinct image; two copies of one would mean a "
                        + "single image was generated and handed back twice",
                TWO_IMAGES,
                view.data().stream()
                        .map(ImageGenerationView.ImageView::b64Json)
                        .distinct().count());

        // One request, asking for two images — not two requests asking for one each.
        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(IMAGES_PATH))
                .withRequestBody(matchingJsonPath(TWO_IMAGES_MARKER)));
    }

    /**
     * Given a request naming a size other than the one the provider would default to
     * When the image is requested
     * Then the size reaches the provider, and the answer is the one only a provider that was told
     * that size returns
     *
     * <p>FR-012 requires {@code size} to be passed through where the provider accepts it, on the
     * same reasoning FR-013 applies to the sampling parameters: it changes both what the caller
     * receives and what the site pays, so dropping it is a cost and correctness failure rather than
     * the harmless compatibility courtesy that ignoring an incidental field is. Unlike the base64
     * ask this file deliberately does not assert, passing a caller's own parameter through is
     * dotCMS's behaviour rather than a provider capability that varies by vendor — the stub here is
     * a provider that accepts {@code size}, and that is all FR-012 conditions the requirement
     * on.</p>
     *
     * <p>It has to be asserted on the outbound request because it is invisible in the response: a
     * provider asked for nothing in particular returns a perfectly well-formed image, and every
     * shape assertion in this file passes while the caller receives a size they did not ask for and
     * a bill for a size they did not choose. The stub matched on the size makes the two outcomes
     * distinguishable twice over — the request log records a body carrying the size, and the answer
     * carries the image only that stub serves, so an implementation that drops {@code size} falls
     * through to the catch-all and fails on the payload as well as on the verification.</p>
     */
    @Test
    public void test_generations_withSize_passesTheSizeToTheProvider() {
        final Response response = resource.generations(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), generationFor(IMAGE_MODEL, PROMPT, 1, ALTERNATE_SIZE));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof ImageGenerationView);

        final ImageGenerationView view = (ImageGenerationView) response.getEntity();
        assertNotNull(view.data());
        assertEquals(1, view.data().size());
        assertEquals("The answer must be the one the provider gave for the size the caller asked "
                        + "for, not the one it gives when asked for nothing in particular",
                ALTERNATE_IMAGE_BASE64, view.data().get(0).b64Json());

        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(IMAGES_PATH))
                .withRequestBody(containing(ALTERNATE_SIZE)));
    }

    /**
     * Given two requests that differ only in {@code n} — one asking for exactly 1, one omitting
     * the field
     * When each image is generated
     * Then both are served, each reaching the provider once and each answering with a single
     * entry
     *
     * <p>The boundary from the other side, and the reason it is a separate test rather than
     * another assertion inside one: what FR-012 refuses is an {@code n} below 1, not the presence
     * of the field, and not the absence of it either. An implementation that rejected any request
     * carrying {@code n} would satisfy the refusal test on its own while breaking every standard
     * client that sends the format's own default of 1, and an implementation that rejected a
     * request omitting {@code n} would break the clients that leave it out. Both halves are
     * asserted against the same site, in the same configuration, so neither can be read as a
     * configuration accident.</p>
     */
    @Test
    public void test_generations_withNOfOneOrAbsent_returnsOneImage() {
        final Response explicitOne = resource.generations(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), generationFor(IMAGE_MODEL, PROMPT, 1));

        assertNotNull(explicitOne);
        assertEquals("n of 1 is what a standard client sends by default, and one image is what "
                + "it must answer with", 200, explicitOne.getStatus());
        assertTrue(explicitOne.getEntity() instanceof ImageGenerationView);
        assertEquals(1, ((ImageGenerationView) explicitOne.getEntity()).data().size());

        final Response absent = resource.generations(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), generationFor(IMAGE_MODEL, PROMPT, null));

        assertNotNull(absent);
        assertEquals("n is optional; omitting it means one image, not a malformed request",
                200, absent.getStatus());
        assertTrue(absent.getEntity() instanceof ImageGenerationView);
        assertEquals(1, ((ImageGenerationView) absent.getEntity()).data().size());

        wireMockServer.verify(2, postRequestedFor(urlPathEqualTo(IMAGES_PATH)));
    }

    /**
     * Given one site whose {@code chat}, {@code embeddings} and {@code image} sections name three
     * different models
     * When the same prompt is generated from three times, once naming each model
     * Then only the image model is served; the chat model and the embeddings model are both
     * refused as models this site has not configured — because neither is configured
     * <em>for images</em>
     *
     * <p>The three halves belong in one test, against one site, in one configuration. An
     * implementation validating against the chat section would fail the first refusal; one
     * validating against the embeddings section would fail the second; one validating against
     * nothing would fail both while still serving the accepted case. Split across three tests, any
     * single one of them could be read as flaky configuration rather than as the wrong section.</p>
     */
    @Test
    public void test_generations_withSiteChatOrEmbeddingsModel_isRefusedEvenThoughImageModelSucceeds() {
        final Response accepted = resource.generations(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), generationFor(IMAGE_MODEL, PROMPT));

        assertNotNull(accepted);
        assertEquals("The model the site configured for images must be served",
                200, accepted.getStatus());
        assertTrue(accepted.getEntity() instanceof ImageGenerationView);

        assertRefusedAsNoSuchModel(CHAT_MODEL);
        assertRefusedAsNoSuchModel(EMBEDDINGS_MODEL);

        // Exactly one call reached the provider: the accepted one. Neither refusal did.
        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(IMAGES_PATH)));
    }

    /**
     * Given a request that omits {@code model}
     * When the image is requested
     * Then it is refused as a client error naming {@code model}, with no implicit default applied
     */
    @Test
    public void test_generations_withoutModel_isRejectedNamingTheField() {
        final Response response = resource.generations(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), generationFor(null, PROMPT));

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView.Body error = ((InferenceErrorView) response.getEntity()).error();
        assertNotNull(error);
        assertEquals(ERROR_TYPE_INVALID_REQUEST, error.type());
        assertEquals("model", error.param());

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(IMAGES_PATH)));
    }

    /**
     * Given a request naming a configured model but omitting {@code prompt}
     * When the image is requested
     * Then it is refused as a client error naming {@code prompt}, and the provider is never
     * contacted
     *
     * <p>Named as its own field rather than folded into a generic "malformed request": there is
     * nothing to generate without a prompt, and a caller sent back to guessing which of two
     * required fields they missed is a support call.</p>
     */
    @Test
    public void test_generations_withoutPrompt_isRejectedNamingTheField() {
        final Response response = resource.generations(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), generationFor(IMAGE_MODEL, null));

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView.Body error = ((InferenceErrorView) response.getEntity()).error();
        assertNotNull(error);
        assertEquals(ERROR_TYPE_INVALID_REQUEST, error.type());
        assertEquals("prompt", error.param());

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(IMAGES_PATH)));
    }

    /**
     * Given a request carrying no credential at all
     * When the image is requested
     * Then it is refused as unauthorized and the provider is never contacted
     *
     * <p>FR-015. Accepts a thrown {@link WebApplicationException} as well as a returned 401, since
     * the surrounding authentication handshake may refuse before the resource body runs and either
     * is a valid refusal.</p>
     */
    @Test
    public void test_generations_withNoCredential_isUnauthorized() {
        final HttpServletRequest request = mockRequest(host.getHostname(), null);

        try {
            final Response response = resource.generations(
                    request, mockResponse(), host.getIdentifier(),
                    generationFor(IMAGE_MODEL, PROMPT));
            assertEquals("An anonymous request must be refused as unauthorized",
                    Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            assertTrue("a refusal carries the standard error shape",
                    response.getEntity() instanceof InferenceErrorView);
        } catch (final WebApplicationException e) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                    e.getResponse().getStatus());
        }

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(IMAGES_PATH)));
    }

    /**
     * Given a request asking for more images than the configured ceiling allows
     * When the generation is requested
     * Then it is refused with a 400 naming {@code n}, and nothing reaches the provider
     *
     * <p>FR-012 and FR-037. Images are priced per image, so without a ceiling one accepted request
     * multiplies a site's provider spend by whatever number it carried, and FR-032 puts per-site
     * spend quotas out of scope — there is no second line of defence behind this one. The earlier
     * draft that refused every {@code n} above 1 capped the spend by accident; correcting it to
     * honor {@code n} removed that cap, and this is the deliberate replacement.</p>
     *
     * <p>Refused rather than clamped, and asserted as such: an implementation that quietly served
     * {@link InferenceLimits#DEFAULT_MAX_IMAGES_PER_REQUEST} images here would return a 200 and a
     * bill for ten, and the caller would never learn that the four hundred they asked for was not
     * what they got. The zero provider calls are the other half — a clamp would show up here as a
     * request that did reach the provider.</p>
     */
    @Test
    public void test_generations_aboveTheImageCeiling_isRejectedNamingTheField() {
        final int aboveCeiling = InferenceLimits.current().maxImagesPerRequest() + 1;

        final Response response = resource.generations(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), generationFor(IMAGE_MODEL, PROMPT, aboveCeiling));

        assertNotNull(response);
        assertEquals("An n above the configured ceiling is one request carrying an unbounded bill, "
                + "and must be refused rather than quietly clamped", 400, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView.Body error = ((InferenceErrorView) response.getEntity()).error();
        assertNotNull(error);
        assertEquals(ERROR_TYPE_INVALID_REQUEST, error.type());
        assertEquals("The caller has to be told which field put them over the limit",
                "n", error.param());
        assertNotNull(error.message());
        assertFalse(error.message().isBlank());

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(IMAGES_PATH)));
    }

    /**
     * Given a site whose image provider can only ever produce one image per request
     * When several images are requested
     * Then it is refused with a 400 naming {@code n} — not a retryable upstream failure
     *
     * <p>FR-012. The multi-image call is a default method on the provider abstraction that throws
     * unless the implementation overrides it: the OpenAI-backed models override it, the Gemini one
     * does not. Left to the ordinary upstream translation of FR-031 that throw becomes a 502, which
     * is the wrong answer in a way that costs the caller real time — 502 is retryable, so a
     * standard client's back-off keeps re-sending a request that cannot succeed however long it
     * waits. The status is the assertion that matters here; the param name is what lets the caller
     * fix it on the first try.</p>
     *
     * <p>No provider stub is needed and none is reached: the site is configured against a provider
     * that never answers in these tests, and the refusal is settled from the model itself before
     * any call is made. That is also why this is safe to assert — it pins the behaviour to what the
     * library actually implements rather than to a list of provider names in a test.</p>
     */
    @Test
    public void test_generations_severalFromAModelThatCannotIsRefusedNotTreatedAsUpstreamFailure()
            throws Exception {
        AiTest.aiAppSecretsWithProviderConfig(host, geminiImageProviderConfigJson());

        final Response response = resource.generations(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), generationFor(GEMINI_IMAGE_MODEL, PROMPT, TWO_IMAGES));

        assertNotNull(response);
        assertEquals("A provider that can never honor n must be a 400 naming the field, not a "
                + "retryable 502 that sends the caller's back-off into an unwinnable loop",
                400, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView.Body error = ((InferenceErrorView) response.getEntity()).error();
        assertNotNull(error);
        assertEquals(ERROR_TYPE_INVALID_REQUEST, error.type());
        assertEquals("n", error.param());
        assertNotNull(error.message());
        assertFalse(error.message().isBlank());
    }

    /**
     * @return a provider configuration whose image section names a provider that implements only
     *         the single-image call, leaving chat and embeddings pointed at the WireMock stub
     */
    private static String geminiImageProviderConfigJson() {
        return String.format(
                "{"
                + "\"chat\":{\"provider\":\"openai\",\"apiKey\":\"%1$s\",\"model\":\"%2$s\","
                + "\"endpoint\":\"%3$s\",\"maxRetries\":0},"
                + "\"embeddings\":{\"provider\":\"openai\",\"apiKey\":\"%1$s\",\"model\":\"%4$s\","
                + "\"endpoint\":\"%3$s\",\"maxRetries\":0},"
                + "\"image\":{\"provider\":\"google_ai\",\"apiKey\":\"%1$s\",\"model\":\"%5$s\","
                + "\"maxRetries\":0}"
                + "}",
                AiTest.API_KEY, CHAT_MODEL, String.format("http://localhost:%d/", AiTest.PORT),
                EMBEDDINGS_MODEL, GEMINI_IMAGE_MODEL);
    }

    /**
     * Asserts that an {@code n} below 1 is refused here — as a 400 in the standard error shape,
     * naming the field, with nothing reaching the provider.
     *
     * <p>Shared by the values that differ only in how far below 1 they fall, so that both stay
     * pinned to the same param name and the same status: a caller who asked for no images and one
     * who asked for minus one have made the same mistake and deserve the same answer.</p>
     *
     * @param count the number of images to ask for; below 1, or this asserts the wrong thing
     */
    private void assertCountRefusedNamingTheField(final int count) {
        final Response response = resource.generations(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), generationFor(IMAGE_MODEL, PROMPT, count));

        assertNotNull(response);
        assertEquals("An n of " + count + " is below 1, which is not a number of images anyone "
                + "can be served", 400, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView.Body error = ((InferenceErrorView) response.getEntity()).error();
        assertNotNull(error);
        assertEquals(ERROR_TYPE_INVALID_REQUEST, error.type());
        assertEquals("A caller asking for a number of images that is not a number of images must "
                + "be told which field is the problem", "n", error.param());
        assertNotNull(error.message());
        assertFalse(error.message().isBlank());

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(IMAGES_PATH)));
    }

    /**
     * Asserts that a model the site has configured for some other section is refused here.
     *
     * @param model the model to ask for
     */
    private void assertRefusedAsNoSuchModel(final String model) {
        final Response response = resource.generations(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), generationFor(model, PROMPT));

        assertNotNull(response);
        assertEquals("'" + model + "' is not an image model, however well configured it is "
                + "elsewhere on this site", 404, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView.Body error = ((InferenceErrorView) response.getEntity()).error();
        assertNotNull(error);
        assertEquals(ERROR_TYPE_INVALID_REQUEST, error.type());
        assertEquals("model", error.param());
        assertNotNull(error.message());
        assertTrue(error.message().contains(model));

        // What the refusal must not leak: which model the site does generate with, where its
        // provider lives, what key reaches it, or which site is behind the host name.
        assertFalse(error.message().contains(IMAGE_MODEL));
        assertFalse(error.message().contains(AiTest.API_KEY));
        assertFalse(error.message().contains(String.valueOf(AiTest.PORT)));
        assertFalse(error.message().contains(host.getHostname()));
        assertFalse(error.message().contains(host.getIdentifier()));
    }

    /**
     * Stubs the OpenAI-compatible provider: one canned image for an ordinary request, a different
     * one for a request that really carried {@link #ALTERNATE_SIZE}, and a two-entry answer for a
     * request that really asked for {@link #TWO_IMAGES} of them — so that what the caller asked for
     * is distinguishable from what the provider would have chosen on its own.
     *
     * <p>The three are separated by priority and by a body match on the value itself, which can
     * only appear in the provider request when the field was passed through. The two matched stubs
     * cannot collide: the size request asks for one image and the two-image request asks for the
     * default size, and neither value is a substring of the other. All three are registered above
     * the default priority on purpose: WireMock also loads the checked-in mappings under
     * {@code src/test/resources/mappings}, several of which answer
     * {@code POST /images/generations}, and without explicit priorities this file would be
     * asserting against whichever stub happened to win.</p>
     */
    private static void stubProvider() {
        wireMockServer.stubFor(post(urlPathEqualTo(IMAGES_PATH))
                .atPriority(1)
                .withRequestBody(containing(ALTERNATE_SIZE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_ALTERNATE_SIZE_RESPONSE)));

        wireMockServer.stubFor(post(urlPathEqualTo(IMAGES_PATH))
                .atPriority(1)
                .withRequestBody(matchingJsonPath(TWO_IMAGES_MARKER))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_TWO_IMAGES_RESPONSE)));

        wireMockServer.stubFor(post(urlPathEqualTo(IMAGES_PATH))
                .atPriority(2)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_RESPONSE)));
    }

    /**
     * @param model  the model to ask for, or null to omit the field
     * @param prompt the prompt to generate from, or null to omit the field
     * @return the smallest well-formed image-generation request, asking for one image
     */
    private static ImageGenerationRequestView generationFor(final String model,
                                                            final String prompt) {
        return generationFor(model, prompt, 1);
    }

    /**
     * @param model  the model to ask for, or null to omit the field
     * @param prompt the prompt to generate from, or null to omit the field
     * @param count  how many images to ask for, or null to omit {@code n} entirely — which is
     *               why it is boxed: "not sent" is a value a caller can express and FR-012 has to
     *               answer with one image, and a primitive would silently turn it into the 0 that
     *               FR-012 refuses
     * @return an image-generation request asking for that many images
     */
    private static ImageGenerationRequestView generationFor(final String model,
                                                            final String prompt,
                                                            final Integer count) {
        return generationFor(model, prompt, count, SIZE);
    }

    /**
     * @param model  the model to ask for, or null to omit the field
     * @param prompt the prompt to generate from, or null to omit the field
     * @param count  how many images to ask for, or null to omit {@code n} entirely
     * @param size   the size to ask for
     * @return an image-generation request asking for that many images at that size
     */
    private static ImageGenerationRequestView generationFor(final String model,
                                                            final String prompt,
                                                            final Integer count,
                                                            final String size) {
        return new ImageGenerationRequestView(model, prompt, count, size);
    }

    /**
     * Builds a request arriving at a given host name, with or without a bearer credential.
     *
     * <p>Request attributes are backed by a real map rather than left as mock no-ops, because both
     * the authentication handshake and FR-020's site attribution publish through them, and a mock
     * that forgot what was set on it would not behave like a servlet container.</p>
     *
     * @param serverName the host name the request arrives on
     * @param credential the {@code Authorization} header value, or null for no credential
     * @return the mocked request
     */
    private static HttpServletRequest mockRequest(final String serverName,
                                                  final String credential) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Map<String, Object> attributes = new HashMap<>();

        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://" + serverName + REQUEST_URI));
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getServerName()).thenReturn(serverName);
        when(request.getHeader("Authorization")).thenReturn(credential);

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
