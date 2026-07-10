package com.dotcms.rest.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Base64;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test cases for ESContentResourcePortlet
 * @author nollymar
 */
@RunWith(DataProviderRunner.class)
public class ESContentResourcePortletTest extends IntegrationTestBase {

    private static LanguageAPI languageAPI;
    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static PermissionAPI permissionAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        user = APILocator.getUserAPI().getSystemUser();

        contentTypeAPI = APILocator.getContentTypeAPI(user);
        contentletAPI = APILocator.getContentletAPI();
        languageAPI = APILocator.getLanguageAPI();
        permissionAPI = APILocator.getPermissionAPI();

    }

    public static class TestCase {
        String depth;
        Boolean live;
        boolean anonymous;
        int statusCode;

        public TestCase(final String depth, final Boolean live, final boolean anonymous, final int statusCode) {
            this.depth      = depth;
            this.live       = live;
            this.anonymous  = anonymous;
            this.statusCode = statusCode;
        }
    }

    @DataProvider
    public static Object[] testCases(){
        return new TestCase[]{
                new TestCase(null, false, false, Status.OK.getStatusCode()),
                new TestCase("0", false, true, Status.OK.getStatusCode()),
                new TestCase("0", false, false, Status.OK.getStatusCode()),
                new TestCase("0", true, true, Status.OK.getStatusCode()),
                new TestCase("0", true, true, Status.OK.getStatusCode()),
                new TestCase("6", false, false, Status.BAD_REQUEST.getStatusCode()),
                new TestCase("test", false, false, Status.BAD_REQUEST.getStatusCode())
        };
    }


    @Test
    @UseDataProvider("testCases")
    public void testSearch(final TestCase testCase) throws Exception {

        final long time = System.currentTimeMillis();

        //creates content type
        ContentType contentType = contentTypeAPI
                .save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                        FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .name("testContentType" + time)
                        .owner(user.getUserId()).build());

        //creates contentlets
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());

        final long language = languageAPI.getDefaultLanguage().getId();
        final Contentlet workingContentlet = contentletDataGen.languageId(language).nextPersisted();
        final Contentlet liveContentlet = contentletDataGen.languageId(language).nextPersisted();

        if (testCase.anonymous) {
            final Role anonymous = TestUserUtils.getOrCreateAnonymousRole();
            permissionAPI
                    .save(new Permission(workingContentlet.getPermissionId(), anonymous.getId(),
                            PermissionAPI.PERMISSION_READ, true), workingContentlet, user, false);
            permissionAPI.save(new Permission(liveContentlet.getPermissionId(), anonymous.getId(),
                    PermissionAPI.PERMISSION_READ, true), liveContentlet, user, false);
        }
        contentletAPI.publish(liveContentlet, user, false);

        //calls endpoint
        final ESContentResourcePortlet esContentResourcePortlet = new ESContentResourcePortlet();
        final HttpServletRequest request = createHttpRequest(testCase.anonymous);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        final String jsonQuery = "{\n"
                + "    \"query\": {\n"
                + "        \"bool\": {\n"
                + "            \"must\": {\n"
                + "                \"term\": {\n"
                + "                    \"contenttype\": " + contentType.variable() + "\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}";

        final Response endpointResponse = esContentResourcePortlet
                .search(request, response, jsonQuery, testCase.depth, testCase.live, null, false);

        assertEquals(testCase.statusCode, endpointResponse.getStatus());

        if (testCase.statusCode == Status.OK.getStatusCode()) {

            final JSONObject json = new JSONObject(endpointResponse.getEntity().toString());
            final JSONArray contentlets = json.getJSONArray("contentlets");
            final JSONObject contentletResult = (JSONObject) contentlets.get(0);

            if (testCase.anonymous || testCase.live) {
                assertEquals(1, contentlets.length());
                assertEquals(liveContentlet.getIdentifier(),
                        contentletResult.get("identifier"));
            } else {
                assertEquals(2, contentlets.length());

                assertTrue(
                        CollectionsUtils.list(contentlets.get(0), contentlets.get(1)).stream()
                                .map(
                                        elem -> {
                                            try {
                                                return ((JSONObject) elem).get("identifier");
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                                return Optional.empty();
                                            }
                                        })
                                .allMatch(identifier ->
                                        workingContentlet.getIdentifier().equals(identifier)
                                                || liveContentlet.getIdentifier()
                                                .equals(identifier)));
            }
        }
    }

    /**
     * Method to test: {@link ESContentResourcePortlet#search(HttpServletRequest, HttpServletResponse, String, String, boolean, String, boolean)}
     * Given scenario: Create a Category with 3 levels of depth:
     *          Parent Category
     *                  Child Category
     *                          Grand Child Category
     *
     *                  Create a Content Type with a Category field, and a contentlet of it. To
     *                  the contentlet add the Grand Child Category.
     * Expected result: json response must include the grand child category info
     */
    @Test
    public void test_search_includeGrandChildCategory_success()
            throws Exception {
        final long currentTime = System.currentTimeMillis();
        //Create Parent Category.
        final Category parentCategory = new CategoryDataGen()
                .setCategoryName("CT-Category-Parent"+currentTime)
                .setKey("parent"+currentTime)
                .setCategoryVelocityVarName("parent"+currentTime)
                .nextPersisted();

        //Create First Child Category.
        final Category childCategoryA = new CategoryDataGen()
                .setCategoryName("CT-Category-A"+currentTime)
                .setKey("categoryA"+currentTime)
                .setCategoryVelocityVarName("categoryA"+currentTime)
                .next();

        //Second Level Category.
        final Category childCategoryA_1 = new CategoryDataGen()
                .setCategoryName("CT-Category-A-1"+currentTime)
                .setKey("categoryA-1"+currentTime)
                .setCategoryVelocityVarName("categoryA-1"+currentTime)
                .next();

        APILocator.getCategoryAPI().save(parentCategory, childCategoryA, user, false);
        APILocator.getCategoryAPI().save(childCategoryA, childCategoryA_1, user, false);

        // Content Type with category field
        final List<Field> fields = new ArrayList<>();
        fields.add(new FieldDataGen().name("Title").velocityVarName("title").next());
        fields.add(new FieldDataGen().type(CategoryField.class)
                .name(parentCategory.getCategoryName()).velocityVarName(parentCategory.getCategoryVelocityVarName())
                .values(parentCategory.getInode()).next());
        final ContentType contentType = new ContentTypeDataGen().fields(fields).nextPersisted();

        // Save content with grand child category
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty("title", "Test Contentlet"+currentTime)
                .addCategory(childCategoryA_1)
                .nextPersisted();

        //Call resource
        final String jsonQuery = "{\n"
                + "    \"query\": {\n"
                + "        \"bool\": {\n"
                + "            \"must\": {\n"
                + "                \"term\": {\n"
                + "                    \"contenttype\": " + contentType.variable() + "\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}";
        final Response responseResource = new ESContentResourcePortlet().search(createHttpRequest(false),new MockHttpResponse(),jsonQuery,"0",false,null,false);

        // Verify result
        assertEquals(Status.OK.getStatusCode(), responseResource.getStatus());
        // Verify JSON result
        final JSONObject json = new JSONObject(responseResource.getEntity().toString());
        final JSONArray contentlets = json.getJSONArray("contentlets");
        assertTrue(contentlets.toString().contains(childCategoryA_1.getCategoryName()));

    }

    /**
     * Method to test: {@link ESContentResourcePortlet#search(HttpServletRequest, HttpServletResponse, String, String, boolean, String, boolean)}
     * Given scenario: the backend now routes /api/es/search through the vendor-neutral phase-aware
     *          SearchAPI, but the "esresponse" field is rebuilt into the legacy Elasticsearch-wire
     *          shape by {@code ESContentResourcePortlet#toLegacyEsJson}.
     * Expected result: "esresponse" keeps the ES-wire contract the dot-es-search Angular portlet and
     *          external clients parse — {@code took}, {@code hits.total.value},
     *          {@code hits.hits[]._id}/{@code ._source}, and {@code aggregations.<name>.buckets}.
     */
    @Test
    public void test_search_esresponse_preservesLegacyEsWireShape() throws Exception {
        final long now = System.currentTimeMillis();
        final ContentType contentType = new ContentTypeDataGen()
                .field(new FieldDataGen().name("Title").velocityVarName("title").next())
                .nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty("title", "esresponse-shape-" + now)
                .nextPersisted();
        ContentletDataGen.publish(contentlet);
        APILocator.getContentletAPI().isInodeIndexed(contentlet.getInode(), true);

        final String jsonQuery = "{\n"
                + "    \"size\": 5,\n"
                + "    \"aggs\": { \"types\": { \"terms\": { \"field\": \"contenttype\", \"size\": 5 } } },\n"
                + "    \"query\": {\n"
                + "        \"bool\": {\n"
                + "            \"must\": {\n"
                + "                \"term\": {\n"
                + "                    \"contenttype\": " + contentType.variable() + "\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}";
        final Response responseResource = new ESContentResourcePortlet()
                .search(createHttpRequest(false), new MockHttpResponse(), jsonQuery, "0", true, null, false);
        assertEquals(Status.OK.getStatusCode(), responseResource.getStatus());

        final JSONObject json = new JSONObject(responseResource.getEntity().toString());
        final JSONObject esresponse = json.getJSONArray("esresponse").getJSONObject(0);

        // Legacy ES-wire timing + hits shape
        assertTrue("esresponse must expose legacy 'took'", esresponse.has("took"));
        final JSONObject hits = esresponse.getJSONObject("hits");
        assertTrue("hits.total.value must be present (ES-wire)", hits.getJSONObject("total").has("value"));
        final JSONArray hitArr = hits.getJSONArray("hits");
        assertTrue("query must return at least one hit", hitArr.length() > 0);
        final JSONObject firstHit = hitArr.getJSONObject(0);
        assertTrue("hit must carry legacy '_id'", firstHit.has("_id"));
        assertTrue("hit must carry legacy '_source'", firstHit.has("_source"));

        // Legacy ES-wire aggregations shape. The adapter emits ES-native typed keys
        // (e.g. "sterms#types") that the dot-es-search portlet's splitAggKey() parses; accept the
        // typed key or the plain name, and require the legacy 'buckets' array to be present.
        final JSONObject aggregations = esresponse.getJSONObject("aggregations");
        boolean typesAggFound = false;
        for (final Object rawKey : aggregations.keySet()) {
            final String key = (String) rawKey;
            if ((key.equals("types") || key.endsWith("#types"))
                    && aggregations.getJSONObject(key).getJSONArray("buckets").length() > 0) {
                typesAggFound = true;
                break;
            }
        }
        assertTrue("aggregations must contain the declared 'types' terms aggregation with a legacy "
                + "'buckets' array (ES-native typed key like 'sterms#types', or plain 'types')",
                typesAggFound);
    }

    /**
     * Method to test: {@link ESContentResourcePortlet#search(HttpServletRequest, HttpServletResponse, String, String, boolean, String, boolean)}
     * Given scenario: a query that <b>sorts by a field</b> and does not set {@code track_scores}.
     *          Elasticsearch/OpenSearch then return a non-finite ({@code NaN}) {@code _score} for
     *          every hit (the hits are not relevance-scored).
     * Expected result: HTTP 200 and each hit's {@code _score} serialized as {@code null} (the
     *          Elasticsearch-native wire format), <b>not</b> HTTP 500
     *          {@code "JSON does not allow non-finite numbers."}. Regression guard for
     *          <a href="https://github.com/dotCMS/core/issues/36478">#36478</a>.
     */
    @Test
    public void test_search_fieldSortedQuery_nonFiniteScore_returnsOkWithNullScore() throws Exception {
        final ContentType contentType = createContentTypeWithPublishedContent();

        final String jsonQuery = "{\n"
                + "  \"query\": { \"bool\": { \"must\": { \"term\": { \"contenttype\": \""
                + contentType.variable() + "\" } } } },\n"
                + "  \"sort\": [ { \"moddate\": \"desc\" } ]\n"
                + "}";

        final Response response = new ESContentResourcePortlet()
                .search(createHttpRequest(false), new MockHttpResponse(), jsonQuery, "0", true, null, false);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertHitScoresAreNull(response.getEntity().toString());
    }

    /**
     * Method to test: {@link ESContentResourcePortlet#searchRaw(HttpServletRequest)}
     * Given scenario: the same field-sorted (non-relevance-scored) query posted to
     *          {@code /api/es/raw}, whose body is read from the request input stream.
     * Expected result: HTTP 200 and {@code _score: null} per hit — {@code /api/es/raw} shares the
     *          {@code toLegacyEsJson} adapter with {@code /api/es/search}, so it is subject to the
     *          same #36478 regression and must be guarded too.
     */
    @Test
    public void test_searchRaw_fieldSortedQuery_nonFiniteScore_returnsOkWithNullScore() throws Exception {
        final ContentType contentType = createContentTypeWithPublishedContent();

        final String jsonQuery = "{\n"
                + "  \"query\": { \"bool\": { \"must\": { \"term\": { \"contenttype\": \""
                + contentType.variable() + "\" } } } },\n"
                + "  \"sort\": [ { \"moddate\": \"desc\" } ]\n"
                + "}";

        final Response response = new ESContentResourcePortlet()
                .searchRaw(createHttpRequestWithBody(jsonQuery));

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // searchRaw returns the legacy ES-wire object directly (no "esresponse" wrapper array).
        assertHitScoresAreNull(response.getEntity().toString(), false);
    }

    /**
     * Method to test: {@link ESContentResourcePortlet#search(HttpServletRequest, HttpServletResponse, String, String, boolean, String, boolean)}
     * Given scenario: a field-sorted query that <b>does</b> set {@code track_scores: true}, forcing
     *          Elasticsearch to compute a finite relevance score.
     * Expected result: HTTP 200 and a finite (non-null) {@code _score} — the non-finite guard must
     *          not alter legitimate finite scores.
     */
    @Test
    public void test_search_fieldSortedQuery_withTrackScores_preservesFiniteScore() throws Exception {
        final ContentType contentType = createContentTypeWithPublishedContent();

        final String jsonQuery = "{\n"
                + "  \"track_scores\": true,\n"
                + "  \"query\": { \"bool\": { \"must\": { \"term\": { \"contenttype\": \""
                + contentType.variable() + "\" } } } },\n"
                + "  \"sort\": [ { \"moddate\": \"desc\" } ]\n"
                + "}";

        final Response response = new ESContentResourcePortlet()
                .search(createHttpRequest(false), new MockHttpResponse(), jsonQuery, "0", true, null, false);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        final JSONObject esresponse = new JSONObject(response.getEntity().toString())
                .getJSONArray("esresponse").getJSONObject(0);
        final JSONArray hits = esresponse.getJSONObject("hits").getJSONArray("hits");
        assertTrue("query must return at least one hit", hits.length() > 0);
        assertTrue("a track_scores query must keep a finite (non-null) _score",
                !hits.getJSONObject(0).isNull("_score"));
    }

    private ContentType createContentTypeWithPublishedContent() throws Exception {
        final long now = System.currentTimeMillis();
        final ContentType contentType = new ContentTypeDataGen()
                .field(new FieldDataGen().name("Title").velocityVarName("title").next())
                .nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty("title", "nan-score-" + now)
                .nextPersisted();
        ContentletDataGen.publish(contentlet);
        APILocator.getContentletAPI().isInodeIndexed(contentlet.getInode(), true);
        return contentType;
    }

    /** Asserts every hit's {@code _score} is {@code null}, reading the {@code /api/es/search} "esresponse" wrapper. */
    private void assertHitScoresAreNull(final String entity) throws JSONException {
        assertHitScoresAreNull(entity, true);
    }

    private void assertHitScoresAreNull(final String entity, final boolean wrappedInEsResponse)
            throws JSONException {
        final JSONObject esresponse = wrappedInEsResponse
                ? new JSONObject(entity).getJSONArray("esresponse").getJSONObject(0)
                : new JSONObject(entity);
        final JSONArray hits = esresponse.getJSONObject("hits").getJSONArray("hits");
        assertTrue("field-sorted query must return at least one hit", hits.length() > 0);
        for (int i = 0; i < hits.length(); i++) {
            assertTrue("_score of a non-relevance-scored (field-sorted) hit must serialize as null",
                    hits.getJSONObject(i).isNull("_score"));
        }
    }

    private HttpServletRequest createHttpRequestWithBody(final String body) throws Exception {
        final HttpServletRequest request = createHttpRequest(false);
        when(request.getInputStream())
                .thenReturn(new MockServletInputStream(
                        new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))));
        return request;
    }

    /** Minimal {@link ServletInputStream} over a byte array, to feed a request body to {@code searchRaw}. */
    private static class MockServletInputStream extends ServletInputStream {

        private final InputStream sourceStream;

        MockServletInputStream(final InputStream sourceStream) {
            this.sourceStream = sourceStream;
        }

        @Override
        public int read() throws IOException {
            return sourceStream.read();
        }

        @Override
        public boolean isFinished() {
            try {
                return sourceStream.available() == 0;
            } catch (final IOException e) {
                return true;
            }
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(final ReadListener readListener) {
            // no-op: synchronous read is sufficient for the test
        }
    }

    private HttpServletRequest createHttpRequest(final boolean anonymous) throws Exception{
        final MockHeaderRequest request = new MockHeaderRequest(new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                        .request()).request());

        if (!anonymous){
            request.setHeader("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));
        }

        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON);

        return request;
    }

}
