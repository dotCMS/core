package com.dotcms.inference.rest;

import com.dotcms.ai.AiTest;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.inference.rest.view.EmbeddingListView;
import com.dotcms.inference.rest.view.EmbeddingsRequestView;
import com.dotcms.inference.rest.view.InferenceErrorView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Specifies {@code POST /api/inference/v1/embeddings} through
 * {@link EmbeddingsResource#embeddings(HttpServletRequest, HttpServletResponse, String, EmbeddingsRequestView)}.
 *
 * <p>FR-011 is not "expose an embeddings endpoint" — the vector round trip is the easy half. What
 * the requirement actually adds is that the model gate of FR-023 has to look at a
 * <strong>different section</strong> of the site's configuration than the chat endpoint does. A
 * site configures its chat models and its embeddings model separately, so an implementation that
 * reuses the chat section here would accept a chat model for an embeddings call and refuse the
 * embeddings model the site actually configured, and it would do so while returning a perfectly
 * well-shaped 200 for the wrong request. That is why the central test below asserts both halves of
 * the same site in one go: the embeddings model succeeds <em>and</em> the chat model is refused.
 * Either assertion alone is satisfied by the bug.</p>
 *
 * <p>The second thing FR-011 adds is that {@code input} is not a string. It is <em>either</em> a
 * string <em>or</em> an array of strings, because batching is how content is ordinarily embedded —
 * anyone indexing a site sends an array — and a scalar-only endpoint would fail that common case
 * while still looking correct against a one-off example. The array form is what makes the response
 * a list of more than one entry, and therefore what makes {@code index} load-bearing: it is the
 * only thing that lets a caller correlate a vector back to the text it sent. The tests below
 * assert that index explicitly, looking each entry up by it rather than by its position in
 * {@code data}, and they pin every refusal the array form introduces — nothing to embed, and
 * elements that are not text.</p>
 *
 * <p>FR-011 now spells out what "an array of strings" costs an implementation, and each clause is
 * a test below. <strong>Every</strong> element has to be a string, so the all-numeric array is
 * joined by a mixed one: an implementation that inspects only the first element passes the
 * all-numeric case and still accepts {@code ["some text", 5]}, which is the shape that actually
 * arrives when a caller's collection was built from mismatched sources. An <strong>absent, null or
 * empty</strong> input is refused naming the field rather than answered with an empty list, and
 * "null" has two wire forms — the JSON null and the field left out — which are separate code paths
 * and so are asserted separately.</p>
 *
 * <p>FR-011 also settles two things this file used to leave open. A <strong>blank or
 * whitespace-only</strong> string is refused on the same grounds as an absent, null or empty one,
 * whether it arrives alone or as one element of an array: there is no meaningful embedding of
 * nothing, and providers differ in whether they error on it or hand back a zero vector — which is
 * exactly the inconsistency this family exists to hide, so it cannot be left to whichever provider
 * a site configured. Both positions are asserted, because they are different code paths: the scalar
 * blank is caught by the same guard that catches the empty string, while a blank buried at
 * position two of an array is only caught by an implementation that inspects every element.</p>
 *
 * <p>And when one element of an array is at fault, {@code param} stays {@code input} — it is the
 * field the caller sent — but the <strong>message</strong> has to identify <em>which</em> element.
 * A caller who batched five hundred strings and is told only that "input is invalid" has been
 * handed the search, not the answer. The tests below therefore assert that the offending position
 * appears in the message, rather than settling for a message that is merely non-blank; the
 * offending element is deliberately placed at an index that appears nowhere else in the payload,
 * so a message that happens to contain a digit cannot pass for one that locates the fault.</p>
 *
 * <ul>
 *     <li>FR-011 — input text comes back as a vector in the standard embeddings shape, with the
 *     serving model and the token counts.</li>
 *     <li>FR-011 — {@code input} accepts a single string or an array of strings; an array yields
 *     one entry per input, each carrying the index of the input it corresponds to, and the
 *     reported usage covers the whole batch. An empty array, a null or absent input, a blank or
 *     whitespace-only string, and an array any of whose elements is not a string or is blank, are
 *     all refused naming {@code input}.</li>
 *     <li>FR-011 — where one element of an array is at fault, the message identifies which
 *     one.</li>
 *     <li>FR-011 / R7 — {@code model} is validated against the site's <strong>embeddings</strong>
 *     section; the site's chat model is refused here, even though the same site has it
 *     configured.</li>
 *     <li>FR-023 — a model the site configured for nothing at all is refused the same way.</li>
 *     <li>FR-024 — {@code model} is required; there is no implicit default.</li>
 *     <li>FR-015 — an anonymous caller is refused.</li>
 *     <li>FR-020 — the serving site is published for the response filter to report.</li>
 * </ul>
 *
 * <p>The provider is a WireMock server standing in for an OpenAI-compatible endpoint, wired in
 * through the same dotAI app secrets the rest of the AI integration tests use, so an accepted call
 * travels the real client path rather than a stubbed one.</p>
 */
public class InferenceEmbeddingsTest {

    /**
     * The model the site's {@code embeddings} section is configured with. Deliberately read from
     * {@link AiTest} rather than restated: the separation this file is about is a property of that
     * shared configuration, and a local copy would keep passing if the two drifted apart.
     */
    private static final String EMBEDDINGS_MODEL = AiTest.EMBEDDINGS_MODEL;

    /**
     * The model the same site's {@code chat} section is configured with — configured, valid, and
     * nevertheless not an embeddings model.
     */
    private static final String CHAT_MODEL = "gpt-4o-mini";

    /** A model name no site in these tests configures for any section at all. */
    private static final String UNCONFIGURED_MODEL = "some-other-vendors-model";

    /** Path an OpenAI-compatible provider serves embeddings on. */
    private static final String EMBEDDINGS_PATH = "/embeddings";

    private static final String REQUEST_URI = "/api/inference/v1/embeddings";

    private static final String ERROR_TYPE_INVALID_REQUEST = "invalid_request_error";

    /** The text these tests embed. */
    private static final String INPUT_TEXT = "The quick brown fox";

    /** A string with nothing in it at all — the emptiest thing a caller can ask to embed. */
    private static final String EMPTY_INPUT = "";

    /**
     * A string with nothing in it but whitespace. FR-011 refuses it on the same grounds as the
     * empty string, and it is asserted separately because it is a separate guard: an
     * {@code isEmpty()} check accepts it and passes three spaces to a provider that will either
     * error or bill for a zero vector.
     */
    private static final String WHITESPACE_INPUT = "   ";

    /**
     * The value that stands in for "not a string" inside a mixed array. Two digits, neither of
     * which is the index the element sits at, so that a message quoting the offending value cannot
     * be mistaken for one that locates it.
     */
    private static final int NON_STRING_ELEMENT = 42;

    /**
     * The batch these tests embed when they exercise the array form. Three entries rather than two,
     * so that "one entry per input" cannot be confused with "a pair", and the last one is the
     * marker the batch stub matches on — see {@link #stubProvider()}.
     */
    private static final String[] BATCH_INPUT = {
            INPUT_TEXT,
            "jumps over the lazy dog",
            "and the dog barks back"
    };

    /**
     * The last element of {@link #BATCH_INPUT}, which appears in the provider request only when a
     * batch was sent. Distinct enough that no scalar request can contain it by accident.
     */
    private static final String BATCH_MARKER = BATCH_INPUT[BATCH_INPUT.length - 1];

    /** Builds the array form of {@code input}; no configuration of its own is needed. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * What the stubbed provider answers. Modelled on what an OpenAI-compatible provider really
     * returns — the shape of
     * {@code dotcms-integration/src/test/resources/mappings/langchain4j-embeddings-stub.json},
     * trimmed to a vector short enough to read.
     */
    private static final String PROVIDER_RESPONSE = """
            {
              "object": "list",
              "data": [
                {
                  "object": "embedding",
                  "index": 0,
                  "embedding": [0.0023, -0.0091, 0.0117, -0.0042, 0.0008]
                }
              ],
              "model": "text-embedding-ada-002",
              "usage": {"prompt_tokens": 5, "total_tokens": 5}
            }
            """;

    /**
     * What the stubbed provider answers a batch of {@link #BATCH_INPUT} with: one entry per input,
     * each with its own vector, and a token count covering all three rather than one of them.
     *
     * <p>The vectors differ from one another on purpose. Three copies of the same numbers would
     * still satisfy "three entries with three indexes" while the implementation handed every
     * caller the first vector.</p>
     */
    private static final String PROVIDER_BATCH_RESPONSE = """
            {
              "object": "list",
              "data": [
                {
                  "object": "embedding",
                  "index": 0,
                  "embedding": [0.0023, -0.0091, 0.0117, -0.0042, 0.0008]
                },
                {
                  "object": "embedding",
                  "index": 1,
                  "embedding": [0.0512, 0.0034, -0.0077, 0.0101, -0.0013]
                },
                {
                  "object": "embedding",
                  "index": 2,
                  "embedding": [-0.0308, 0.0146, 0.0052, -0.0090, 0.0027]
                }
              ],
              "model": "text-embedding-ada-002",
              "usage": {"prompt_tokens": 17, "total_tokens": 17}
            }
            """;

    private static WireMockServer wireMockServer;

    /** A backend user who is also an administrator, so site READ is never the thing under test. */
    private static User user;
    private static String bearerToken;

    private Host host;
    private final EmbeddingsResource resource = new EmbeddingsResource();

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
                .name("inference-embeddings-" + UUID.randomUUID() + ".dotcms.com")
                .nextPersisted();
        // Configures chat, embeddings and image as three separate sections, each with its own
        // model. The chat model given here is the one this file expects the embeddings gate to
        // refuse.
        AiTest.aiAppSecretsWithProviderConfig(
                host, AiTest.providerConfigJson(AiTest.PORT, CHAT_MODEL));
        wireMockServer.resetRequests();
    }

    @After
    public void after() throws Exception {
        AiTest.removeAiAppSecrets(host);
    }

    /**
     * Given a site configured with an embeddings model, and a request carrying input text
     * When the embedding is requested
     * Then a vector comes back in the standard embeddings shape — a {@code list} of one
     * {@code embedding} at index 0 — alongside the serving model and the token counts
     */
    @Test
    public void test_embeddings_withInputText_returnsVectorInStandardShape() {
        final Response response = resource.embeddings(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), embeddingFor(EMBEDDINGS_MODEL, INPUT_TEXT));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof EmbeddingListView);

        final EmbeddingListView view = (EmbeddingListView) response.getEntity();
        assertEquals("list", view.object());
        assertEquals(EMBEDDINGS_MODEL, view.model());

        assertNotNull(view.data());
        assertEquals(1, view.data().size());

        final EmbeddingListView.EmbeddingView embedding = view.data().get(0);
        assertEquals("embedding", embedding.object());
        assertEquals(0, embedding.index());
        assertNotNull(embedding.embedding());
        assertFalse("An embedding with no numbers in it is not an embedding",
                embedding.embedding().isEmpty());

        assertNotNull(view.usage());
        assertNotNull(view.usage().promptTokens());
        assertTrue(view.usage().promptTokens() > 0);
        assertNotNull(view.usage().totalTokens());
        assertTrue(view.usage().totalTokens() > 0);

        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(EMBEDDINGS_PATH)));
    }

    /**
     * Given a request whose {@code input} is an array of three strings
     * When the embeddings are requested
     * Then three entries come back, one per input, each carrying the index of the input it
     * corresponds to and a vector of its own
     *
     * <p>FR-011's array form. The assertions look each entry up <em>by its index</em> rather than
     * reading positions out of {@code data}, because the index is the whole mechanism by which a
     * caller correlates a vector back to the text it sent; a test that trusted array position
     * would pass against an implementation that stamped indexes on positionally and never
     * populated them meaningfully at all.</p>
     */
    @Test
    public void test_embeddings_withArrayInput_returnsOneEntryPerInputInOrder() {
        final Response response = resource.embeddings(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), embeddingForAll(EMBEDDINGS_MODEL, BATCH_INPUT));

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity() instanceof EmbeddingListView);

        final EmbeddingListView view = (EmbeddingListView) response.getEntity();
        assertEquals("list", view.object());
        assertEquals(EMBEDDINGS_MODEL, view.model());

        assertNotNull(view.data());
        assertEquals("An array of three inputs is three embeddings, not one",
                BATCH_INPUT.length, view.data().size());

        for (int index = 0; index < BATCH_INPUT.length; index++) {
            final EmbeddingListView.EmbeddingView embedding = embeddingAtIndex(view, index);
            assertNotNull("No entry carries index " + index + ", so the caller cannot tell which "
                    + "input it embedded", embedding);
            assertEquals("embedding", embedding.object());
            assertNotNull(embedding.embedding());
            assertFalse("An embedding with no numbers in it is not an embedding",
                    embedding.embedding().isEmpty());
        }

        assertEquals("Every entry must carry a distinct vector; identical vectors would mean the "
                        + "same input was embedded three times",
                BATCH_INPUT.length,
                view.data().stream().map(EmbeddingListView.EmbeddingView::embedding)
                        .distinct().count());

        // One batch is one provider round trip, not one per input.
        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(EMBEDDINGS_PATH)));
    }

    /**
     * Given a request whose {@code input} is a single string rather than an array
     * When the embedding is requested
     * Then exactly one entry comes back, at index 0
     *
     * <p>The scalar form is the half of FR-011 that already worked, and widening {@code input} to
     * accept an array is exactly the kind of change that quietly breaks it — by normalising the
     * scalar into a one-element array and then losing it, or by rejecting anything that is not an
     * array. Both halves of "a string or an array of strings" have to hold at once, so the scalar
     * form gets its own guard rather than living only inside the standard-shape test above.</p>
     */
    @Test
    public void test_embeddings_withScalarInput_returnsSingleEntryAtIndexZero() {
        final Response response = resource.embeddings(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), embeddingFor(EMBEDDINGS_MODEL, INPUT_TEXT));

        assertNotNull(response);
        assertEquals("A single string is still valid input, not a malformed array",
                200, response.getStatus());
        assertTrue(response.getEntity() instanceof EmbeddingListView);

        final EmbeddingListView view = (EmbeddingListView) response.getEntity();
        assertNotNull(view.data());
        assertEquals("One input is one embedding", 1, view.data().size());

        final EmbeddingListView.EmbeddingView embedding = view.data().get(0);
        assertEquals("embedding", embedding.object());
        assertEquals("The only entry of a scalar request sits at index 0", 0, embedding.index());
        assertNotNull(embedding.embedding());
        assertFalse(embedding.embedding().isEmpty());

        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(EMBEDDINGS_PATH)));
    }

    /**
     * Given a request whose {@code input} is an empty array
     * When the embeddings are requested
     * Then it is refused as a client error naming {@code input}, and the provider is never
     * contacted
     *
     * <p>Embedding nothing is a caller mistake — a batch built from a query that matched no
     * content, most likely. Answering it with an empty {@code data} list and a 200 would look like
     * success, and the caller would index nothing and never find out why. FR-011 now says this in
     * as many words — an absent, null or empty input is refused naming the field — where before it
     * had to be inferred from the response being "one entry per input"; the test is unchanged,
     * which is the point of recording it.</p>
     */
    @Test
    public void test_embeddings_withEmptyArrayInput_isRejectedNamingTheField() {
        final Response response = resource.embeddings(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), embeddingForAll(EMBEDDINGS_MODEL));

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertTrue("An empty batch must be refused, not answered with an empty list",
                response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView.Body error = ((InferenceErrorView) response.getEntity()).error();
        assertNotNull(error);
        assertEquals(ERROR_TYPE_INVALID_REQUEST, error.type());
        assertEquals("input", error.param());
        assertNotNull(error.message());
        assertFalse(error.message().isBlank());

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(EMBEDDINGS_PATH)));
    }

    /**
     * Given a request whose {@code input} array holds numbers rather than strings
     * When the embeddings are requested
     * Then it is refused as a client error naming {@code input}, and the provider is never
     * contacted
     *
     * <p>The format this family follows also accepts arrays of token ids, and dotCMS does not.
     * That difference has to surface here, as a validation error pointing at {@code input}, rather
     * than several layers down as whatever the provider client makes of a list of integers — a
     * caller who sent pre-tokenized input deserves to be told that, not a deserialization
     * stacktrace.</p>
     */
    @Test
    public void test_embeddings_withNonStringArrayElements_isRejectedNamingTheField() {
        final ArrayNode tokenIds = OBJECT_MAPPER.createArrayNode();
        tokenIds.add(1);
        tokenIds.add(2);
        tokenIds.add(3);

        assertInputRefusedNamingTheField(tokenIds);
    }

    /**
     * Given a request whose {@code input} array holds three strings followed by a number
     * When the embeddings are requested
     * Then it is refused as a client error naming {@code input}, and the message identifies the
     * element at fault, and the provider is never contacted
     *
     * <p>FR-011 requires <strong>every</strong> element of the array to be a string, and this is
     * the test that makes that word load-bearing. The all-numeric array above is refused by an
     * implementation that looks only at {@code input.get(0)}; a mixed array is not, and a mixed
     * array is the one a caller actually sends — a list assembled from two sources where one
     * yielded identifiers instead of text. The refusal has to happen here, naming the field, for
     * the same reason as the all-numeric case: several layers down it becomes whatever the
     * provider client makes of a heterogeneous list. The number sits last rather than second so
     * that the strings ahead of it are not decoration: an implementation that checks the first two
     * elements and stops passes a two-element mixed array and fails this one.</p>
     *
     * <p>Naming the field is no longer enough on its own. FR-011 requires the message to identify
     * <em>which</em> element is at fault, because {@code param} can only ever say {@code input} and
     * a caller who sent a batch is otherwise left to find the bad one themselves. The assertion
     * looks for the offending position in the message; the position is one no other number in the
     * payload shares, so a message that merely quotes the value it choked on does not pass for one
     * that locates it.</p>
     *
     * <p>The provider check is not a formality either. An implementation that "cleans" the array
     * by keeping the strings it recognises would send a perfectly valid batch upstream and answer
     * 200 — silently embedding less than the caller asked for and shifting every index they meant
     * to correlate on.</p>
     */
    @Test
    public void test_embeddings_withMixedArrayElements_isRejectedIdentifyingTheElement() {
        final ArrayNode mixed = OBJECT_MAPPER.createArrayNode();
        for (final String text : BATCH_INPUT) {
            mixed.add(text);
        }
        mixed.add(NON_STRING_ELEMENT);

        assertInputRefusedIdentifyingTheElement(mixed, BATCH_INPUT.length);
    }

    /**
     * Given a request whose {@code input} is a blank string — first empty, then whitespace only
     * When the embeddings are requested
     * Then both are refused as a client error naming {@code input}, and the provider is never
     * contacted
     *
     * <p>FR-011 refuses a blank or whitespace-only string on the same grounds as an absent, null or
     * empty input: there is nothing there to embed. Leaving it to the provider is the failure this
     * family exists to prevent — some error, some return a zero vector, and a caller indexing a
     * site would silently store a meaningless vector against a document and retrieve it forever
     * after. The two forms are asserted together because they are one requirement and two guards:
     * {@code isEmpty()} catches the first and waves the second through.</p>
     */
    @Test
    public void test_embeddings_withBlankStringInput_isRejectedNamingTheField() {
        assertInputRefusedNamingTheField(TextNode.valueOf(EMPTY_INPUT));
        assertInputRefusedNamingTheField(TextNode.valueOf(WHITESPACE_INPUT));
    }

    /**
     * Given a request whose {@code input} array holds two strings and then a whitespace-only one
     * When the embeddings are requested
     * Then it is refused as a client error naming {@code input}, and the message identifies the
     * element at fault, and the provider is never contacted
     *
     * <p>The array position of the blank-string rule, and the harder half of it. A blank arriving
     * alone is the caller's whole request and hard to miss; a blank buried in a batch is what
     * actually happens — a column that was empty for one row of five hundred — and an
     * implementation that validates the array as a whole rather than element by element sends it
     * upstream without noticing. Refusing the batch rather than skipping the blank is the point:
     * dropping it would shift the index of every entry after it, and FR-011 makes those indexes the
     * caller's only means of correlating vectors back to what they sent.</p>
     *
     * <p>As with the mixed array, the message must say which element — and the blank sits at a
     * position that appears nowhere else in the payload, so nothing but locating it will do.</p>
     */
    @Test
    public void test_embeddings_withBlankArrayElement_isRejectedIdentifyingTheElement() {
        final ArrayNode withBlank = OBJECT_MAPPER.createArrayNode();
        withBlank.add(BATCH_INPUT[0]);
        withBlank.add(BATCH_INPUT[1]);
        withBlank.add(WHITESPACE_INPUT);

        assertInputRefusedIdentifyingTheElement(withBlank, 2);
    }

    /**
     * Given a request whose {@code input} is null — first as a JSON null, then as a field left out
     * of the payload entirely
     * When the embeddings are requested
     * Then both are refused as a client error naming {@code input}, and the provider is never
     * contacted
     *
     * <p>FR-011 lists "absent" and "null" alongside "empty", and the two are asserted together
     * because they are not the same code path. A JSON {@code "input": null} deserializes to a
     * {@link NullNode} — an object, present, and non-null as far as a {@code != null} guard is
     * concerned — while an omitted field leaves the component null outright. An implementation
     * guarding on one reaches the provider with the other, where a null input is either an empty
     * batch answered as success or a stacktrace, neither of which tells the caller they sent
     * nothing.</p>
     */
    @Test
    public void test_embeddings_withNullInput_isRejectedNamingTheField() {
        assertInputRefusedNamingTheField(NullNode.getInstance());
        assertInputRefusedNamingTheField(null);
    }

    /**
     * Given the same site embedding one short string and then a batch of three
     * When the reported usage of each is compared
     * Then the batch reports more prompt tokens than the single string did
     *
     * <p>Usage is what the caller is billed on, so it has to describe the whole batch rather than
     * whichever element happened to be counted. The assertion is relative — the batch is larger
     * than the single string — rather than a fixed number: the count is the provider's to report,
     * and pinning it would make this test a statement about the stub instead of about the
     * endpoint.</p>
     */
    @Test
    public void test_embeddings_withArrayInput_reportsUsageForWholeBatch() {
        final Response single = resource.embeddings(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), embeddingFor(EMBEDDINGS_MODEL, INPUT_TEXT));

        assertNotNull(single);
        assertEquals(200, single.getStatus());
        assertTrue(single.getEntity() instanceof EmbeddingListView);

        final EmbeddingListView singleView = (EmbeddingListView) single.getEntity();
        assertNotNull(singleView.usage());
        assertNotNull(singleView.usage().promptTokens());

        final Response batch = resource.embeddings(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), embeddingForAll(EMBEDDINGS_MODEL, BATCH_INPUT));

        assertNotNull(batch);
        assertEquals(200, batch.getStatus());
        assertTrue(batch.getEntity() instanceof EmbeddingListView);

        final EmbeddingListView batchView = (EmbeddingListView) batch.getEntity();
        assertNotNull(batchView.usage());
        assertNotNull(batchView.usage().promptTokens());
        assertTrue("Usage must cover the whole batch, not one element of it",
                batchView.usage().promptTokens() > singleView.usage().promptTokens());
        assertNotNull(batchView.usage().totalTokens());
        assertTrue(batchView.usage().totalTokens() > 0);
    }

    /**
     * Given one site whose {@code chat} section and {@code embeddings} section name different
     * models
     * When the same input is embedded twice, once naming the embeddings model and once naming the
     * chat model
     * Then the embeddings model is served and the chat model is refused as a model this site has
     * not configured — because it is not configured <em>for embeddings</em>
     *
     * <p>FR-011 and R7, stated as one assertion rather than two files apart. An implementation
     * that validated against the chat section would fail exactly one of these two halves, and an
     * implementation that validated against nothing at all would fail only the second — so both
     * belong in the same test, against the same site, in the same configuration.</p>
     */
    @Test
    public void test_embeddings_withSiteChatModel_isRefusedEvenThoughEmbeddingsModelSucceeds() {
        final Response accepted = resource.embeddings(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), embeddingFor(EMBEDDINGS_MODEL, INPUT_TEXT));

        assertNotNull(accepted);
        assertEquals("The model the site configured for embeddings must be served",
                200, accepted.getStatus());
        assertTrue(accepted.getEntity() instanceof EmbeddingListView);

        final Response refused = resource.embeddings(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), embeddingFor(CHAT_MODEL, INPUT_TEXT));

        assertNotNull(refused);
        assertEquals("The site's chat model is not an embeddings model, however well configured "
                + "it is for chat", 404, refused.getStatus());
        assertTrue(refused.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView.Body error = ((InferenceErrorView) refused.getEntity()).error();
        assertNotNull(error);
        assertEquals(ERROR_TYPE_INVALID_REQUEST, error.type());
        assertEquals("model", error.param());
        assertNotNull(error.message());
        assertTrue(error.message().contains(CHAT_MODEL));

        // What the refusal must not leak: which model the site does embed with, where its provider
        // lives, what key reaches it, or which site is behind the host name.
        assertFalse(error.message().contains(EMBEDDINGS_MODEL));
        assertFalse(error.message().contains(AiTest.API_KEY));
        assertFalse(error.message().contains(String.valueOf(AiTest.PORT)));
        assertFalse(error.message().contains(host.getHostname()));
        assertFalse(error.message().contains(host.getIdentifier()));

        // Exactly one call reached the provider: the accepted one. The refusal never did.
        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(EMBEDDINGS_PATH)));
    }

    /**
     * Given a model name the site has configured for no section at all
     * When the embedding is requested
     * Then it is refused with a 404 in the "no such model" shape and the provider is never
     * contacted
     */
    @Test
    public void test_embeddings_withModelConfiguredForNothing_isRefusedAsNoSuchModel() {
        final Response response = resource.embeddings(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), embeddingFor(UNCONFIGURED_MODEL, INPUT_TEXT));

        assertNotNull(response);
        assertEquals(404, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView.Body error = ((InferenceErrorView) response.getEntity()).error();
        assertNotNull(error);
        assertEquals(ERROR_TYPE_INVALID_REQUEST, error.type());
        assertEquals("model", error.param());
        assertNotNull(error.message());
        assertTrue(error.message().contains(UNCONFIGURED_MODEL));

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(EMBEDDINGS_PATH)));
    }

    /**
     * Given a request that omits {@code model}
     * When the embedding is requested
     * Then it is refused as a client error naming {@code model}, with no implicit default applied
     *
     * <p>FR-024 holds across the family, not only on chat. A site has exactly one embeddings model
     * configured, which makes defaulting to it look harmless — and is precisely why it has to be
     * refused here: a caller who never named a model cannot tell when the site's changes.</p>
     */
    @Test
    public void test_embeddings_withoutModel_isRejectedNamingTheField() {
        final Response response = resource.embeddings(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), embeddingFor(null, INPUT_TEXT));

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView.Body error = ((InferenceErrorView) response.getEntity()).error();
        assertNotNull(error);
        assertEquals(ERROR_TYPE_INVALID_REQUEST, error.type());
        assertEquals("model", error.param());

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(EMBEDDINGS_PATH)));
    }

    /**
     * Given a request carrying no credential at all
     * When the embedding is requested
     * Then it is refused as unauthorized and the provider is never contacted
     *
     * <p>FR-015. Accepts a thrown {@link WebApplicationException} as well as a returned 401, since
     * the surrounding authentication handshake may refuse before the resource body runs and either
     * is a valid refusal.</p>
     */
    @Test
    public void test_embeddings_withNoCredential_isUnauthorized() {
        final HttpServletRequest request = mockRequest(host.getHostname(), null);

        try {
            final Response response = resource.embeddings(
                    request, mockResponse(), host.getIdentifier(),
                    embeddingFor(EMBEDDINGS_MODEL, INPUT_TEXT));
            assertEquals("An anonymous request must be refused as unauthorized",
                    Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            assertTrue("a refusal carries the standard error shape",
                    response.getEntity() instanceof InferenceErrorView);
        } catch (final WebApplicationException e) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                    e.getResponse().getStatus());
        }

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(EMBEDDINGS_PATH)));
    }

    /**
     * Given an authenticated request naming a site explicitly
     * When the embedding is requested
     * Then the resolved site is published on the request for the response filter to report
     *
     * <p>FR-020 asks for the serving site on every response, and it reaches a real caller as the
     * {@code X-dotCMS-Resolved-Site} header that {@link ResolvedSiteHeaderFilter} writes from
     * {@link InferenceRequestAttributes#RESOLVED_SITE_ID}. This test calls the resource method
     * directly, so no JAX-RS response filter runs and no header exists to read; the attribute is
     * the filter's sole input and the only part of the chain this resource is responsible for.</p>
     */
    @Test
    public void test_embeddings_withResolvedSite_publishesResolvedSiteId() {
        final HttpServletRequest request = mockRequest(host.getHostname(), bearerToken);

        resource.embeddings(request, mockResponse(), host.getIdentifier(),
                embeddingFor(EMBEDDINGS_MODEL, INPUT_TEXT));

        assertEquals("The site whose configuration served the embedding must be published for the "
                        + "response filter to report",
                host.getIdentifier(),
                request.getAttribute(InferenceRequestAttributes.RESOLVED_SITE_ID));
    }

    /**
     * Stubs the OpenAI-compatible provider: one canned vector for a single input, three for a
     * batch, so that the response really does carry one entry per input rather than a fixed list
     * the endpoint could return no matter what it was asked.
     *
     * <p>The two stubs are separated by priority and by a body match on {@link #BATCH_MARKER}, the
     * last element of {@link #BATCH_INPUT}, which can only appear in the provider request when a
     * batch was sent. Both are registered above the default priority on purpose: WireMock also
     * loads the checked-in mappings under {@code src/test/resources/mappings}, one of which answers
     * any {@code POST /embeddings} carrying the shared test API key, and without explicit
     * priorities this file would be asserting against whichever stub happened to win.</p>
     */
    private static void stubProvider() {
        wireMockServer.stubFor(post(urlPathEqualTo(EMBEDDINGS_PATH))
                .atPriority(1)
                .withRequestBody(containing(BATCH_MARKER))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_BATCH_RESPONSE)));

        wireMockServer.stubFor(post(urlPathEqualTo(EMBEDDINGS_PATH))
                .atPriority(2)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROVIDER_RESPONSE)));
    }

    /**
     * @param view the response to search
     * @param index the index a caller sent an input at
     * @return the entry that claims that index, or null when nothing does
     */
    private static EmbeddingListView.EmbeddingView embeddingAtIndex(final EmbeddingListView view,
                                                                    final int index) {
        return view.data().stream()
                .filter(embedding -> embedding.index() == index)
                .findFirst()
                .orElse(null);
    }

    /**
     * Asserts that an {@code input} FR-011 refuses is refused here — as a 400 in the standard
     * error shape, naming the field, with nothing reaching the provider.
     *
     * <p>Shared by the refusals that differ only in what was sent, so that a new one is a line
     * rather than a copied block, and so that all of them stay pinned to the same param name: a
     * caller cannot correct a payload they are not told the offending field of.</p>
     *
     * @param input the {@code input} value to send, or null to omit the field entirely
     * @return the error body, so that a caller with more to assert about the message can carry on
     *         from here rather than repeat all of this
     */
    private InferenceErrorView.Body assertInputRefusedNamingTheField(final JsonNode input) {
        final Response response = resource.embeddings(
                mockRequest(host.getHostname(), bearerToken), mockResponse(),
                host.getIdentifier(), new EmbeddingsRequestView(EMBEDDINGS_MODEL, input));

        assertNotNull(response);
        assertEquals("'" + input + "' is not something to embed", 400, response.getStatus());
        assertTrue(response.getEntity() instanceof InferenceErrorView);

        final InferenceErrorView.Body error = ((InferenceErrorView) response.getEntity()).error();
        assertNotNull(error);
        assertEquals(ERROR_TYPE_INVALID_REQUEST, error.type());
        assertEquals("input", error.param());
        assertNotNull(error.message());
        assertFalse(error.message().isBlank());

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(EMBEDDINGS_PATH)));

        return error;
    }

    /**
     * Asserts everything {@link #assertInputRefusedNamingTheField(JsonNode)} does, and additionally
     * that the message identifies <em>which</em> element of the array is at fault.
     *
     * <p>FR-011 keeps {@code param} at {@code input} — that is the field the caller sent — so the
     * message is the only place the offending position can appear, and a caller who batched five
     * hundred strings should not have to bisect their own payload to find it. The index is the
     * 0-based one the response's own {@code index} correlates on, so that the number in the error
     * means the same thing as the number in a success.</p>
     *
     * <p>The match is on the index as a standalone number rather than on a substring, so that the
     * digits of some unrelated value — a count, a limit, an element the message quotes — cannot
     * stand in for the position. The tests calling this place the offending element at an index no
     * other number in the payload shares, for the same reason.</p>
     *
     * @param input the {@code input} array to send
     * @param index the 0-based position of the element that is at fault
     */
    private void assertInputRefusedIdentifyingTheElement(final JsonNode input, final int index) {
        final InferenceErrorView.Body error = assertInputRefusedNamingTheField(input);

        assertTrue("A caller who sent a batch must be told which element is the problem, not just "
                        + "that 'input' is: the message must identify position " + index
                        + " (0-based, as the response's own index is), and '" + error.message()
                        + "' does not",
                Pattern.compile("\\b" + index + "\\b").matcher(error.message()).find());
    }

    /**
     * The scalar form of {@code input}: a single string, as
     * {@code {"model": "…", "input": "The quick brown fox"}}.
     *
     * @param model the model to ask for, or null to omit the field
     * @param input the text to embed
     * @return the smallest well-formed embeddings request
     */
    private static EmbeddingsRequestView embeddingFor(final String model, final String input) {
        return new EmbeddingsRequestView(model, TextNode.valueOf(input));
    }

    /**
     * The array form of {@code input}, as
     * {@code {"model": "…", "input": ["The quick brown fox", "…"]}}. Called with no inputs it
     * builds the empty array, which FR-011 refuses.
     *
     * @param model  the model to ask for, or null to omit the field
     * @param inputs the texts to embed, in the order the caller sent them
     * @return an embeddings request carrying a batch
     */
    private static EmbeddingsRequestView embeddingForAll(final String model,
                                                          final String... inputs) {
        final ArrayNode input = OBJECT_MAPPER.createArrayNode();
        for (final String text : inputs) {
            input.add(text);
        }

        return new EmbeddingsRequestView(model, input);
    }

    /**
     * Builds a request arriving at a given host name, with or without a bearer credential.
     *
     * <p>Request attributes are backed by a real map rather than left as mock no-ops, because both
     * the authentication handshake and FR-020's site attribution publish through them, and a mock
     * that forgot what was set on it would not behave like a servlet container.</p>
     *
     * @param serverName  the host name the request arrives on
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
