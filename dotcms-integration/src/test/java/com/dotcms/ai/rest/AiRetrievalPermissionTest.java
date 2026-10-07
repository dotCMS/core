package com.dotcms.ai.rest;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.EmbeddingsDTODataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.PermissionUtilTest;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.json.JSONObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Proves that dotAI search and completions only use embedded content the caller can READ
 * (#37151), going through the real REST resources, permission API and pgvector table.
 *
 * <p>Every test builds its own index with fixed vectors: an "anchor" row whose text is the query
 * (so the query reuses its stored vector instead of calling the provider), restricted chunks very
 * close to the query, and readable chunks a little further away. Restricted content is readable
 * only by one role; readable content is readable by the Anonymous role, which every logged-in
 * user also gets when front-end roles are respected.</p>
 *
 * @author hassandotcms
 */
public class AiRetrievalPermissionTest {

    private static final int DIMENSIONS = 1536;
    private static final float THRESHOLD = .25f;
    private static final String PASSWORD = "Ai-Retrieval-1!";
    private static final String CHAT_PATH = ".*/chat/completions";
    private static final String NO_MATCH = "no matching content found in the index for your query";

    private static WireMockServer wireMockServer;
    private static ContentType contentType;
    private static Field bodyField;
    private static Role restrictedRole;
    private static User userWithoutRole;
    private static User userWithRole;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        AiTest.aiAppSecretsWithProviderConfig(
                APILocator.systemHost(), AiTest.providerConfigJson(AiTest.PORT, "gpt-4o-mini"));

        bodyField = new FieldDataGen().type(TextField.class).next();
        contentType = new ContentTypeDataGen().field(bodyField).nextPersisted();
        restrictedRole = new RoleDataGen().nextPersisted();
        userWithoutRole = new UserDataGen().password(PASSWORD)
                .roles(TestUserUtils.getBackendRole(), TestUserUtils.getFrontendRole()).nextPersisted();
        userWithRole = new UserDataGen().password(PASSWORD)
                .roles(TestUserUtils.getBackendRole(), TestUserUtils.getFrontendRole(), restrictedRole).nextPersisted();
    }

    @AfterClass
    public static void afterClass() {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    @Before
    public void before() {
        wireMockServer.resetRequests();
    }

    /**
     * Given readable and restricted content both matching a search
     * When a user without the role searches
     * Then only the readable content comes back, nothing of the restricted content appears in the
     * response, {@code count} matches {@code dotCMSResults} and {@code total} the chunks returned
     * And a user with the role gets the restricted content too (AC-001)
     */
    @Test
    public void test_search_mixed_onlyReadableForUserWithoutRole_bothForUserWithRole() throws Exception {
        final Fixture fixture = new Fixture();
        final Contentlet restricted = fixture.restrictedChunks(2);
        final Contentlet readable = fixture.readableChunks(2);

        final JSONObject withoutRole = search(userWithoutRole, fixture, 10);
        final String withoutRoleJson = withoutRole.toString();
        assertTrue(withoutRoleJson.contains(readable.getInode()));
        assertFalse(withoutRoleJson.contains(restricted.getInode()));
        assertFalse(withoutRoleJson.contains(fixture.restrictedMarker));
        assertEquals(withoutRole.getJSONArray("dotCMSResults").size(), withoutRole.getInt("count"));
        assertEquals(2, withoutRole.getInt("total"));

        final String withRoleJson = search(userWithRole, fixture, 10).toString();
        assertTrue(withRoleJson.contains(readable.getInode()));
        assertTrue(withRoleJson.contains(restricted.getInode()));
    }

    /**
     * Given readable and restricted content both matching a completion prompt
     * When a user without the role asks for a completion
     * Then the request sent to the model carries the readable text and never the restricted text
     * (AC-002)
     */
    @Test
    public void test_completions_mixed_providerReceivesOnlyReadableText() throws Exception {
        final Fixture fixture = new Fixture();
        fixture.restrictedChunks(2);
        fixture.readableChunks(2);
        fixture.stubChat();

        complete(userWithoutRole, fixture, 50);

        wireMockServer.verify(postRequestedFor(urlPathMatching(CHAT_PATH))
                .withRequestBody(containing(fixture.readableMarker)));
        wireMockServer.verify(0, postRequestedFor(urlPathMatching(CHAT_PATH))
                .withRequestBody(containing(fixture.restrictedMarker)));
    }

    /**
     * Given only restricted content matches a completion prompt
     * When a user without the role asks for a completion
     * Then the response is the existing "no matching content" error and the model is never called
     * (AC-003)
     */
    @Test
    public void test_completions_onlyRestricted_noMatchingContent_noChatCall() throws Exception {
        final Fixture fixture = new Fixture();
        fixture.restrictedChunks(3);
        fixture.stubChat();

        final JSONObject result = complete(userWithoutRole, fixture, 50);

        assertEquals(NO_MATCH, result.optString("error"));
        wireMockServer.verify(0, postRequestedFor(urlPathMatching(CHAT_PATH))
                .withRequestBody(containing(fixture.queryMarker)));
    }

    /**
     * Given more restricted chunks than the page size, all ranked above the readable ones
     * When a user without the role searches and asks for a completion with that page size
     * Then search still returns a full page of readable chunks, and the completion is answered
     * from the readable content instead of "no matching content" (AC-007)
     */
    @Test
    public void test_restrictedOutnumberLimit_fullPageAndAnswerFromReadable() throws Exception {
        final Fixture fixture = new Fixture();
        fixture.restrictedChunks(6);
        fixture.readableChunks(3);
        fixture.stubChat();

        final JSONObject searchResult = search(userWithoutRole, fixture, 3);
        assertEquals(3, searchResult.getInt("total"));
        assertFalse(searchResult.toString().contains(fixture.restrictedMarker));

        final JSONObject completion = complete(userWithoutRole, fixture, 3);
        assertFalse(NO_MATCH.equals(completion.optString("error")));
        wireMockServer.verify(postRequestedFor(urlPathMatching(CHAT_PATH))
                .withRequestBody(containing(fixture.readableMarker)));
    }

    private static JSONObject search(final User user, final Fixture fixture, final int limit) {
        final Response response = new SearchResource().searchByPost(
                requestFor(user), new EmptyHttpResponse(), fixture.form(limit));
        return new JSONObject((String) response.getEntity());
    }

    private static JSONObject complete(final User user, final Fixture fixture, final int limit) {
        final Response response = new CompletionsResource().summarizeFromContent(
                requestFor(user), new EmptyHttpResponse(), fixture.form(limit));
        return new JSONObject((String) response.getEntity());
    }

    private static HttpServletRequest requestFor(final User user) {
        final MockHeaderRequest request = new MockHeaderRequest(new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request());
        request.setHeader("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString((user.getEmailAddress() + ":" + PASSWORD).getBytes()));
        return request;
    }

    /**
     * A unit vector at the given cosine distance from the query vector (the first axis).
     */
    private static float[] vectorAtDistance(final double distance) {
        final float[] vector = new float[DIMENSIONS];
        final double cos = 1 - distance;
        vector[0] = (float) cos;
        vector[1] = (float) Math.sqrt(1 - cos * cos);
        return vector;
    }

    /**
     * One test's private index: the query anchor plus restricted and readable chunks, each with
     * unique markers so assertions never match another test's data.
     */
    private static final class Fixture {

        private final String id = UUID.randomUUID().toString().replace("-", "");
        private final String indexName = "aiperm" + id;
        private final String queryMarker = "query" + id;
        private final String restrictedMarker = "restricted" + id;
        private final String readableMarker = "readable" + id;
        private final String query = "What does the handbook say " + queryMarker;

        private Fixture() {
            // the anchor has no contentlet behind it, so it is never returned once filtering is in place
            persistChunk("anchor" + id, "anchor" + id, query, vectorAtDistance(0));
        }

        private Contentlet restrictedChunks(final int count) throws Exception {
            final Contentlet contentlet = publishedContentlet(restrictedMarker);
            APILocator.getPermissionAPI().save(
                    new Permission(contentlet.getPermissionId(), restrictedRole.getId(),
                            PermissionAPI.PERMISSION_READ, true),
                    contentlet, APILocator.systemUser(), false);
            persistChunks(contentlet, restrictedMarker, count, .05);
            return contentlet;
        }

        private Contentlet readableChunks(final int count) throws Exception {
            final Contentlet contentlet = publishedContentlet(readableMarker);
            PermissionUtilTest.addAnonymousUser(contentlet);
            persistChunks(contentlet, readableMarker, count, .15);
            return contentlet;
        }

        private void stubChat() {
            wireMockServer.stubFor(post(urlPathMatching(CHAT_PATH))
                    .withRequestBody(containing(queryMarker))
                    .willReturn(okJson("{\"id\":\"chatcmpl-" + id + "\",\"object\":\"chat.completion\","
                            + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\","
                            + "\"content\":\"answer " + id + "\"},\"finish_reason\":\"stop\"}]}")));
        }

        private CompletionsForm form(final int limit) {
            return new CompletionsForm.Builder()
                    .prompt(query)
                    .indexName(indexName)
                    .searchLimit(limit)
                    .threshold(THRESHOLD)
                    .build();
        }

        private Contentlet publishedContentlet(final String marker) {
            final Contentlet contentlet = new ContentletDataGen(contentType)
                    .setProperty(bodyField.variable(), marker + " body text")
                    .nextPersisted();
            ContentletDataGen.publish(contentlet);
            return contentlet;
        }

        private void persistChunks(final Contentlet contentlet, final String marker,
                                   final int count, final double distance) {
            for (int i = 0; i < count; i++) {
                persistChunk(contentlet.getInode(), contentlet.getIdentifier(),
                        marker + " chunk " + i, vectorAtDistance(distance));
            }
        }

        private void persistChunk(final String inode, final String identifier,
                                  final String text, final float[] vector) {
            new EmbeddingsDTODataGen()
                    .withInode(inode)
                    .withIdentifier(identifier)
                    .withLanguage(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                    .withTitle(text)
                    .withContentType(contentType.variable())
                    .withExtractedText(text)
                    .withHost(APILocator.systemHost().getIdentifier())
                    .withIndexName(indexName)
                    .withOperator("<=>")
                    .withQuery(query)
                    .withTokenCount(10)
                    .withEmbeddings(vector)
                    .nextPersisted();
        }
    }

}
