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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
                .search(request, response, jsonQuery, testCase.depth, testCase.live,false);

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
     * Method to test: {@link ESContentResourcePortlet#search(HttpServletRequest, HttpServletResponse, String, String, boolean, boolean)}
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
        final Response responseResource = new ESContentResourcePortlet().search(createHttpRequest(false),new MockHttpResponse(),jsonQuery,"0",false,false);

        // Verify result
        assertEquals(Status.OK.getStatusCode(), responseResource.getStatus());
        // Verify JSON result
        final JSONObject json = new JSONObject(responseResource.getEntity().toString());
        final JSONArray contentlets = json.getJSONArray("contentlets");
        assertTrue(contentlets.toString().contains(childCategoryA_1.getCategoryName()));

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
