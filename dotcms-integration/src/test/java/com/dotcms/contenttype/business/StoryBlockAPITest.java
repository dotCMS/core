
package com.dotcms.contenttype.business;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link StoryBlockAPI}
 * @author jsanca
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class StoryBlockAPITest extends IntegrationTestBase {

    @DataProvider
    public static Object[] depthValues() {
        return new Integer[]{0, 1, 2, 3};
    }

    private static final String JSON_SELF_REFERENCE =
            "{\n" +
                    "   \"type\":\"doc\",\n" +
                    "   \"attrs\":{\n" +
                    "      \"charCount\":2,\n" +
                    "      \"wordCount\":1,\n" +
                    "      \"readingTime\":1\n" +
                    "   },\n" +
                    "   \"content\":[\n" +
                    "      {\n" +
                    "         \"type\":\"dotContent\",\n" +
                    "         \"attrs\":{\n" +
                    "            \"data\":{\n" +
                    "               \"hostName\":\"demo.dotcms.com\",\n" +
                    "               \"modDate\":\"2020-09-02 16:45:51.583\",\n" +
                    "               \"publishDate\":\"2020-09-02 16:45:51.583\",\n" +
                    "               \"title\":\"Let Us Help Pack Your Bags!\",\n" +
                    "               \"body\":\"<h2>Let Us Help Pack Your Bags!</h2>\\n<h3>Save 30% of your entire Purchase.</h3>\",\n" +
                    "               \"baseType\":\"CONTENT\",\n" +
                    "               \"inode\":\"177d31ac-3068-414e-ae32-72aa173c2a7b\",\n" +
                    "               \"archived\":false,\n" +
                    "               \"host\":\"48190c8c-42c4-46af-8d1a-0cd5db894797\",\n" +
                    "               \"working\":true,\n" +
                    "               \"variantId\":\"DEFAULT\",\n" +
                    "               \"locked\":false,\n" +
                    "               \"stInode\":\"2a3e91e4-fbbf-4876-8c5b-2233c1739b05\",\n" +
                    "               \"contentType\":\"webPageContent\",\n" +
                    "               \"live\":true,\n" +
                    "               \"owner\":\"036fd43a-6d98-46e0-b22e-bae02cb86f0c\",\n" +
                    "               \"identifier\":\"3d3a99c4-9b94-4840-8390-704fb6d1d998\",\n" +
                    "               \"languageId\":1,\n" +
                    "               \"url\":\"/content.18f8e5d1-30fd-4282-9123-2e7704ff1567\",\n" +
                    "               \"titleImage\":\"TITLE_IMAGE_NOT_FOUND\",\n" +
                    "               \"modUserName\":\"Admin User\",\n" +
                    "               \"hasLiveVersion\":true,\n" +
                    "               \"folder\":\"SYSTEM_FOLDER\",\n" +
                    "               \"hasTitleImage\":false,\n" +
                    "               \"sortOrder\":0,\n" +
                    "               \"modUser\":\"dotcms.org.1\",\n" +
                    "               \"__icon__\":\"contentIcon\",\n" +
                    "               \"contentTypeIcon\":\"wysiwyg\",\n" +
                    "               \"language\":\"en-US\"\n" +
                    "            }\n" +
                    "         }\n" +
                    "      },\n" +
                    "      {\n" +
                    "         \"type\":\"dotContent\",\n" +
                    "         \"attrs\":{\n" +
                    "            \"data\":{\n" +
                    "               \"hostName\":\"demo.dotcms.com\",\n" +
                    "               \"modDate\":\"2023-04-28 18:58:46.164\",\n" +
                    "               \"publishDate\":\"2023-04-28 18:58:46.164\",\n" +
                    "               \"title\":\"test3\",\n" +
                    "               \"body\":{\n" +
                    "                  \"type\":\"doc\",\n" +
                    "                  \"attrs\":{\n" +
                    "                     \"charCount\":1,\n" +
                    "                     \"wordCount\":1,\n" +
                    "                     \"readingTime\":1\n" +
                    "                  },\n" +
                    "                  \"content\":[\n" +
                    "                     {\n" +
                    "                        \"type\":\"dotContent\",\n" +
                    "                        \"attrs\":{\n" +
                    "                           \"data\":{\n" +
                    "                              \"hostName\":\"demo.dotcms.com\",\n" +
                    "                              \"modDate\":\"2020-09-02 16:45:51.583\",\n" +
                    "                              \"publishDate\":\"2020-09-02 16:45:51.583\",\n" +
                    "                              \"title\":\"Let Us Help Pack Your Bags!\",\n" +
                    "                              \"body\":\"<h2>Let Us Help Pack Your Bags!</h2>\\n<h3>Save 30% of your entire Purchase.</h3>\",\n" +
                    "                              \"baseType\":\"CONTENT\",\n" +
                    "                              \"inode\":\"177d31ac-3068-414e-ae32-72aa173c2a7b\",\n" +
                    "                              \"archived\":false,\n" +
                    "                              \"host\":\"48190c8c-42c4-46af-8d1a-0cd5db894797\",\n" +
                    "                              \"working\":true,\n" +
                    "                              \"variantId\":\"DEFAULT\",\n" +
                    "                              \"locked\":false,\n" +
                    "                              \"stInode\":\"2a3e91e4-fbbf-4876-8c5b-2233c1739b05\",\n" +
                    "                              \"contentType\":\"webPageContent\",\n" +
                    "                              \"live\":true,\n" +
                    "                              \"owner\":\"036fd43a-6d98-46e0-b22e-bae02cb86f0c\",\n" +
                    "                              \"identifier\":\"3d3a99c4-9b94-4840-8390-704fb6d1d998\",\n" +
                    "                              \"languageId\":1,\n" +
                    "                              \"url\":\"/content.18f8e5d1-30fd-4282-9123-2e7704ff1567\",\n" +
                    "                              \"titleImage\":\"TITLE_IMAGE_NOT_FOUND\",\n" +
                    "                              \"modUserName\":\"Admin User\",\n" +
                    "                              \"hasLiveVersion\":true,\n" +
                    "                              \"folder\":\"SYSTEM_FOLDER\",\n" +
                    "                              \"hasTitleImage\":false,\n" +
                    "                              \"sortOrder\":0,\n" +
                    "                              \"modUser\":\"dotcms.org.1\",\n" +
                    "                              \"__icon__\":\"contentIcon\",\n" +
                    "                              \"contentTypeIcon\":\"wysiwyg\",\n" +
                    "                              \"language\":\"en-US\"\n" +
                    "                           }\n" +
                    "                        }\n" +
                    "                     },\n" +
                    "                     {\n" +
                    "                        \"type\":\"paragraph\",\n" +
                    "                        \"attrs\":{\n" +
                    "                           \"textAlign\":\"left\"\n" +
                    "                        }\n" +
                    "                     }\n" +
                    "                  ]\n" +
                    "               },\n" +
                    "               \"baseType\":\"CONTENT\",\n" +
                    "               \"inode\":\"dcb46b55-13b6-4a49-b694-4c53e2e0e58b\",\n" +
                    "               \"archived\":false,\n" +
                    "               \"host\":\"48190c8c-42c4-46af-8d1a-0cd5db894797\",\n" +
                    "               \"working\":true,\n" +
                    "               \"variantId\":\"DEFAULT\",\n" +
                    "               \"locked\":false,\n" +
                    "               \"stInode\":\"a7278304de440313a1d0fcda65f237a0\",\n" +
                    "               \"contentType\":\"TestBlockEditor\",\n" +
                    "               \"live\":false,\n" +
                    "               \"owner\":\"dotcms.org.1\",\n" +
                    "               \"identifier\":\"53275900853ae0115707c97d4617efb5\",\n" +
                    "               \"languageId\":1,\n" +
                    "               \"url\":\"/content.dcb46b55-13b6-4a49-b694-4c53e2e0e58b\",\n" +
                    "               \"titleImage\":\"TITLE_IMAGE_NOT_FOUND\",\n" +
                    "               \"modUserName\":\"Admin User\",\n" +
                    "               \"hasLiveVersion\":false,\n" +
                    "               \"folder\":\"SYSTEM_FOLDER\",\n" +
                    "               \"hasTitleImage\":false,\n" +
                    "               \"sortOrder\":0,\n" +
                    "               \"modUser\":\"dotcms.org.1\",\n" +
                    "               \"__icon__\":\"contentIcon\",\n" +
                    "               \"contentTypeIcon\":\"event_note\",\n" +
                    "               \"language\":\"en-US\"\n" +
                    "            }\n" +
                    "         }\n" +
                    "      },\n" +
                    "      {\n" +
                    "         \"type\":\"paragraph\",\n" +
                    "         \"attrs\":{\n" +
                    "            \"textAlign\":\"left\"\n" +
                    "         }\n" +
                    "      }\n" +
                    "   ]\n" +
                    "}";
    private static final String JSON =

                    "{\n" +
                    "            \"type\":\"doc\",\n" +
                    "            \"content\":[\n" +
                    "               {\n" +
                    "                  \"type\":\"horizontalRule\"\n" +
                    "               },\n" +
                    "               {\n" +
                    "                  \"type\":\"heading\",\n" +
                    "                  \"content\":[\n" +
                    "                     {\n" +
                    "                        \"text\":\"Heading\",\n" +
                    "                        \"type\":\"text\"\n" +
                    "                     },\n" +
                    "                     {\n" +
                    "                        \"marks\":[\n" +
                    "                           {\n" +
                    "                              \"type\":\"italic\"\n" +
                    "                           }\n" +
                    "                        ],\n" +
                    "                        \"text\":\" 1\",\n" +
                    "                        \"type\":\"text\"\n" +
                    "                     }\n" +
                    "                  ],\n" +
                    "                  \"attrs\":{\n" +
                    "                     \"textAlign\":\"left\",\n" +
                    "                     \"level\":1\n" +
                    "                  }\n" +
                    "               },\n" +
                    "               {\n" +
                    "                  \"type\":\"paragraph\",\n" +
                    "                  \"content\":[\n" +
                    "                     {\n" +
                    "                        \"text\":\"Paragraph\",\n" +
                    "                        \"type\":\"text\"\n" +
                    "                     },\n" +
                    "                     {\n" +
                    "                        \"marks\":[\n" +
                    "                           {\n" +
                    "                              \"type\":\"bold\"\n" +
                    "                           }\n" +
                    "                        ],\n" +
                    "                        \"text\":\" yeah\",\n" +
                    "                        \"type\":\"text\"\n" +
                    "                     }\n" +
                    "                  ],\n" +
                    "                  \"attrs\":{\n" +
                    "                     \"textAlign\":\"left\"\n" +
                    "                  }\n" +
                    "               }\n" +
                    "            ]\n" +
                    "         }";



    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link StoryBlockAPI#refreshStoryBlockValueReferences(Object,String)}
     * Given Scenario: This will pass a self reference json, should not throw any exception.
     * The method should not fail on self reference with a exception.
     *
     */
    @Test
    public void test_refresh_references_on_self_reference() throws DotDataException, DotSecurityException, JsonProcessingException {

        try {
            final StoryBlockReferenceResult refreshResult = APILocator.getStoryBlockAPI()
                    .refreshStoryBlockValueReferences(JSON_SELF_REFERENCE, "3d3a99c4-9b94-4840-8390-704fb6d1d998");
        } catch (Throwable e) {
            Assert.fail("Should not throw any exception");
        }
    }

    /**
     * Method to test: {@link StoryBlockAPI#refreshStoryBlockValueReferences(Object,String)}
     * Given Scenario: This will create a story block contentlet, adds a rich content and retrieve the json.
     * Then, will update the rich content previously added, the story block contentlet should reflect the new rich text changed.
     * ExpectedResult: The new json will reflect the rich text changes
     *
     */
    @Test
    public void test_refresh_references() throws DotDataException, DotSecurityException, JsonProcessingException {
        //1) create a rich text contentlet with some initial values
        final ContentType contentTypeRichText = APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent");
        final Contentlet richTextContentlet   = new ContentletDataGen(contentTypeRichText).setProperty("title","Title1").setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT).nextPersisted();

        // 2) add the contentlet to the static story block created previously
        final Object newStoryBlockJson        = APILocator.getStoryBlockAPI().addContentlet(JSON, richTextContentlet);

        // 3) convert the json to map, to start the test
        final Map    newStoryBlockMap         = ContentletJsonHelper.INSTANCE.get().objectMapper()
                                                        .readValue(Try.of(newStoryBlockJson::toString)
                                                                           .getOrElse(StringPool.BLANK), LinkedHashMap.class);

        assertNotNull(newStoryBlockMap);
        final List contentList = (List) newStoryBlockMap.get("content");
        final Optional<Object> firstContentletMap = contentList.stream()
                .filter(content -> "dotContent".equals(Map.class.cast(content).get("type"))).findFirst();

        assertTrue(firstContentletMap.isPresent());
        final Map contentletMap = (Map) Map.class.cast(Map.class.cast(firstContentletMap.get()).get(StoryBlockAPI.ATTRS_KEY)).get(StoryBlockAPI.DATA_KEY);
        assertEquals(contentletMap.get("identifier"), richTextContentlet.getIdentifier());
        assertEquals(contentletMap.get("title"), richTextContentlet.getStringProperty("title"));
        assertEquals(contentletMap.get("body"),  richTextContentlet.getStringProperty("body"));

        // 4) checkout/publish the contentlet in order to do new changes
        final Contentlet newRichTextContentlet = APILocator.getContentletAPI().checkout(richTextContentlet.getInode(), APILocator.systemUser(), false);
        newRichTextContentlet.setProperty("title","Title2");
        newRichTextContentlet.setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT);
        APILocator.getContentletAPI().publish(
                APILocator.getContentletAPI().checkin(newRichTextContentlet, APILocator.systemUser(), false), APILocator.systemUser(), false);

        final HttpServletRequest oldThreadRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse oldThreadResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();

        try {
            final HttpServletRequest request = new MockAttributeRequest(mock(HttpServletRequest.class));
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            final HttpServletResponse response = mock(HttpServletResponse.class);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(response);

            // 5) ask for refreshing references, the new changes of the rich text contentlet should be reflected on the json
            final StoryBlockReferenceResult refreshResult = APILocator.getStoryBlockAPI().refreshStoryBlockValueReferences(newStoryBlockJson, "1234");

            // 6) check if the results are ok.
            assertTrue(refreshResult.isRefreshed());
            assertNotNull(refreshResult.getValue());
            final Map refreshedStoryBlockMap = ContentletJsonHelper.INSTANCE.get().objectMapper()
                    .readValue(Try.of(() -> refreshResult.getValue().toString())
                            .getOrElse(StringPool.BLANK), LinkedHashMap.class);
            final List refreshedContentList = (List) refreshedStoryBlockMap.get("content");
            final Optional<Object> refreshedfirstContentletMap = refreshedContentList.stream()
                    .filter(content -> "dotContent".equals(Map.class.cast(content).get("type"))).findFirst();

            assertTrue(refreshedfirstContentletMap.isPresent());
            final Map refreshedContentletMap = (Map) Map.class.cast(Map.class.cast(refreshedfirstContentletMap.get()).get(StoryBlockAPI.ATTRS_KEY)).get(StoryBlockAPI.DATA_KEY);
            assertEquals(refreshedContentletMap.get("identifier"), newRichTextContentlet.getIdentifier());
            assertEquals("Expected Generic Content title doesn't match the one in the Contentlet", "Title2", newRichTextContentlet.getStringProperty("title"));
            assertEquals("Expected Generic Content body doesn't match the one in the Contentlet", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT, newRichTextContentlet.getStringProperty("body"));
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(oldThreadRequest);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(oldThreadResponse);
        }
    }

    /**
     * Method to test: {@link StoryBlockAPI#getDependencies(Object)}
     * Given Scenario: Creates a story block and adds 3 contentlets
     * ExpectedResult: The contentlets added should be retrieved
     *
     */
    @Test
    public void test_get_dependencies() throws DotDataException, DotSecurityException, JsonProcessingException {

        //1) create a rich text contentlets with some initial values
        final ContentType contentTypeRichText = APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent");
        final Contentlet richTextContentlet1   = new ContentletDataGen(contentTypeRichText).setProperty("title","Title1").setProperty("body", JSON).nextPersisted();
        final Contentlet richTextContentlet2   = new ContentletDataGen(contentTypeRichText).setProperty("title","Title1").setProperty("body", JSON).nextPersisted();
        final Contentlet richTextContentlet3   = new ContentletDataGen(contentTypeRichText).setProperty("title","Title1").setProperty("body", JSON).nextPersisted();

        // 2) adds the contentlets to the static story block created previously
        final Object newStoryBlockJson1        = APILocator.getStoryBlockAPI().addContentlet(JSON, richTextContentlet1);
        final Object newStoryBlockJson2        = APILocator.getStoryBlockAPI().addContentlet(newStoryBlockJson1, richTextContentlet2);
        final Object newStoryBlockJson3        = APILocator.getStoryBlockAPI().addContentlet(newStoryBlockJson2, richTextContentlet3);

        // 3) convert the json to map, to start the test
        final Map    newStoryBlockMap         = ContentletJsonHelper.INSTANCE.get().objectMapper()
                                                        .readValue(Try.of(() -> newStoryBlockJson3.toString())
                                                                           .getOrElse(StringPool.BLANK), LinkedHashMap.class);


        assertNotNull(newStoryBlockMap);
        final List<String> contentletIdList = APILocator.getStoryBlockAPI().getDependencies(newStoryBlockJson3);
        assertNotNull(contentletIdList);
        assertEquals(3, contentletIdList.size());
        assertTrue(contentletIdList.contains(richTextContentlet1.getIdentifier()));
        assertTrue(contentletIdList.contains(richTextContentlet2.getIdentifier()));
        assertTrue(contentletIdList.contains(richTextContentlet3.getIdentifier()));
    }

    /**
     * Method to test: {@link StoryBlockAPI#getDependencies(Object)}
     * Given Scenario: Test a story block value that is a json (html in this case) see (https://github.com/dotCMS/core/issues/24299)
     * ExpectedResult: Do not throw exception and must return zero dependencies
     */
    @Test
    public void test_get_dependencies_with_non_json_value()  {

        final Object newStoryBlockJson1        = "<html>pufff</html>";

        final List<String> contentletIdList = APILocator.getStoryBlockAPI().getDependencies(newStoryBlockJson1);
        assertNotNull(contentletIdList);
        assertTrue(contentletIdList.isEmpty());
    }
    
    /**
     * Method to test: {@link StoryBlockAPI#getDependencies(Object)}
     * Given Scenario: Test a story block value that is a json (html in this case) see (https://github.com/dotCMS/core/issues/24299)
     * ExpectedResult: Do not throw exception and must return zero dependencies
     */
    @Test
    public void test_get_dependencies_with_empty_json_value()  {

        final Object newStoryBlockJson1        = "{\"test\":\"test\"}";

        final List<String> contentletIdList = APILocator.getStoryBlockAPI().getDependencies(newStoryBlockJson1);
        assertNotNull(contentletIdList);
        assertTrue(contentletIdList.isEmpty());
    }
    
    /**
     * Method to test: {@link StoryBlockAPI#getDependencies(Object)}
     * Given Scenario: Test a story block value that is a json (html in this case) see (https://github.com/dotCMS/core/issues/24299)
     * ExpectedResult: Do not throw exception and must return zero dependencies
     */
    @Test
    public void test_get_dependencies_with_bad_content_value()  {

        final Object newStoryBlockJson1        = "{\"content\":\"test\"}";

        final List<String> contentletIdList = APILocator.getStoryBlockAPI().getDependencies(newStoryBlockJson1);
        assertNotNull(contentletIdList);
        assertTrue(contentletIdList.isEmpty());
    }
    
    @Test
    public void test_get_refreshStoryBlockValueReferences_with_bad_content_value()  {
    
        final Object newStoryBlockJson1        = "{\"test\":\"test\"}";
        
        StoryBlockReferenceResult result = APILocator.getStoryBlockAPI().refreshStoryBlockValueReferences(newStoryBlockJson1, "xxx");
        assertNotNull(result);
        assertTrue(result.isRefreshed());
        
        final Object newStoryBlockJson2        = "{\"content\":\"test\"}";
        result = APILocator.getStoryBlockAPI().refreshStoryBlockValueReferences(newStoryBlockJson2, "xxx");
        assertNotNull(result);
        assertTrue(result.isRefreshed());
    
        
    }

    /**
     * Method to test: {@link StoryBlockAPI#refreshStoryBlockValueReferences(Object, String)}
     * Given Scenario: Test a story block value that is not a json
     * ExpectedResult: Do not throw exception and must return zero dependencies
     */
    @Test
    public void test_refreshStoryBlockValueReferences_with_json_value()  {

        final Object newStoryBlockJson1        = "bu bu bu}";

        StoryBlockReferenceResult result = APILocator.getStoryBlockAPI().refreshStoryBlockValueReferences(newStoryBlockJson1, "xxx");
        assertNotNull(result);
        assertFalse(result.isRefreshed());

    }

    /**
     * Method to test: {@link StoryBlockAPIImpl#refreshReferences(Contentlet)}
     * When:
     * - We have a Content Type with 3 fields:
     *      TextField this is like the title
     *      RelationshipField MANY_TO_MANY relationship to itself
     *      BlocEditorField
     * - Now we are going to create 3 Contentlets:
     *      A: related to C
     *         Add in BLockEditor: B and C
     *      B: related to A
     *         Add in BLockEditor: A and C
     *      C: related to B
     *         Add in BLockEditor: A and B
     * And we are going to load A
     *
     * Should: return the right depth all the time and don't throw a {@link OutOfMemoryError} or {@link StackOverflowError}
     * @param depth
     * @throws Exception
     */
    @Test
    @UseDataProvider("depthValues")
    public void testCycleRelationshipAndBlockEditor(final int depth) throws Exception {
        final Language language = new LanguageDataGen().nextPersisted();

        ContentType contentType = new ContentTypeDataGen().nextPersisted();
        
        final Field storyBlockField = new FieldDataGen()
                .type(StoryBlockField.class)
                .contentTypeId(contentType.id())
                .nextPersisted();
        
        final Field relationshipField = APILocator.getContentTypeFieldAPI().save(
                FieldBuilder.builder(RelationshipField.class)
                    .name("rel")
                    .contentTypeId(contentType.id())
                    .values(String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                    .relationType(contentType.variable()).build(), APILocator.systemUser());

        final Field titleField = new FieldDataGen().contentTypeId(contentType.id()).type(TextField.class).nextPersisted();

        contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentType.id());

        Contentlet contentA = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "A").nextPersisted();
        Contentlet contentB = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "B").nextPersisted();
        Contentlet contentC = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "C").nextPersisted();

        final Contentlet contentACompleteANdPublish = setFieldsAndPublishBothAsBlockEditor(contentA, relationshipField,
                storyBlockField, contentC, contentB);

        final Contentlet contentBCompleteANdPublish = setFieldsAndPublishBothAsBlockEditor(contentB, relationshipField,
                storyBlockField, contentA, contentC);

        final Contentlet contentCCompleteANdPublish = setFieldsAndPublishBothAsBlockEditor(contentC, relationshipField,
                storyBlockField, contentB, contentA);

        final HttpServletRequest oldThreadRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse oldThreadResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();

        try {
            final HttpServletRequest request = new MockAttributeRequest(mock(HttpServletRequest.class));
            request.setAttribute(WebKeys.HTMLPAGE_DEPTH, String.valueOf(depth));
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            final HttpServletResponse response = mock(HttpServletResponse.class);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(response);

            Contentlet contentAFromAPI = APILocator.getContentletAPI()
                    .find(contentACompleteANdPublish.getInode(), APILocator.systemUser(), false);

            Map<String, Object> blockEditorMap = JsonUtil.getJsonFromString(contentAFromAPI.getStringProperty(storyBlockField.variable()));
            List<Map<String, Object>> blockValue = (List<Map<String, Object>>) blockEditorMap.get("content");

            assertEquals(3, blockValue.size());

            for (int i = 0; i < blockValue.size(); i++) {

                if (blockValue.get(i).get("type").equals("dotContent")) {
                    Map<String, Object> blockEditorItem = (Map<String, Object>)
                            ((Map<String, Object>) blockValue.get(i).get("attrs")).get("data");

                    assertEquals(i == 0 ? contentB.getIdentifier() : contentC.getIdentifier(), blockEditorItem.get("identifier"));

                    List<Object> relatedContent = (List<Object>) blockEditorItem.get(relationshipField.variable());
                    assertEquals(1, relatedContent.size());

                    if (depth == 0) {
                        assertEquals( i == 0 ? contentA.getIdentifier() : contentB.getIdentifier(),
                                relatedContent.get(0).toString());
                    } else if (depth == 1){
                        assertEquals(  i == 0 ? contentA.getIdentifier() : contentB.getIdentifier(),
                                ((Map<String, Object>) relatedContent.get(0)).get("identifier"));

                        assertNull( ((Map<String, Object>) relatedContent.get(0)).get(relationshipField.variable()));
                    } else if (depth > 1 && i == 0) {
                        assertEquals(i == 0 ? contentA.getIdentifier() : contentB.getIdentifier(),
                                ((Map<String, Object>) relatedContent.get(0)).get("identifier"));

                        final List<Object> secondLevelRelatedContents = (List<Object>)
                                ((Map<String, Object>) relatedContent.get(0)).get(relationshipField.variable());

                        assertEquals(1, secondLevelRelatedContents.size());

                        if (depth == 2) {
                            assertEquals(i == 0 ? contentC.getIdentifier() : contentA.getIdentifier(),
                                    secondLevelRelatedContents.get(0).toString());
                        } else {
                           final  Map<String, Object> secondLevelRelatedContent = (Map<String, Object>) secondLevelRelatedContents.get(0);
                            assertEquals(i == 0 ? contentC.getIdentifier() : contentA.getIdentifier(),
                                    secondLevelRelatedContent.get("identifier"));

                            assertNull(secondLevelRelatedContent.get(relationshipField.variable()));
                        }
                    }
                }
            }
        }finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(oldThreadRequest);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(oldThreadResponse);
        }
    }

    /**
     * When:
     * - We have a Content Type with 3 fields:
     *      TextField this is like the title
     *      RelationshipField ONE_TO_ONE relationship to itself
     *      BlocEditorField
     * - Now we are going to create some Contentlets:
     *      A: related to B
     *         Add in BLockEditor C
     *      B: related to D
     *         Add in BLockEditor: E
     *      C: related to F
     *         Add in BLockEditor G
     *      F: related to H
     *         Add in BLockEditor I
     *      H: related to K
     *         Add in BLockEditor K
     * And we are going to load A
     *
     * Should: return the right depth all the time
     * @throws Exception
     */
    @Test
    @UseDataProvider("depthValues")
    public void hydrateWithBlockEditorAndRelationship(final int depth) throws Exception {

        final Language language = new LanguageDataGen().nextPersisted();

        ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final Field storyBlockField = new FieldDataGen()
                .type(StoryBlockField.class)
                .contentTypeId(contentType.id())
                .nextPersisted();

        final Field relationshipField = APILocator.getContentTypeFieldAPI().save(
                FieldBuilder.builder(RelationshipField.class)
                        .name("rel")
                        .contentTypeId(contentType.id())
                        .values(String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal()))
                        .relationType(contentType.variable()).build(), APILocator.systemUser());

        final Field titleField = new FieldDataGen().contentTypeId(contentType.id()).type(TextField.class).nextPersisted();

        contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentType.id());

        Contentlet contentA = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "A").nextPersisted();
        Contentlet contentB = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "B").nextPersisted();
        Contentlet contentC = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "C").nextPersisted();
        Contentlet contentD = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "D").nextPersistedAndPublish();
        Contentlet contentE = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "E").nextPersistedAndPublish();
        Contentlet contentF = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "F").nextPersisted();
        Contentlet contentG = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "G").nextPersistedAndPublish();
        Contentlet contentH = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "H").nextPersisted();
        Contentlet contentI = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "I").nextPersistedAndPublish();
        Contentlet contentJ = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "J").nextPersistedAndPublish();
        Contentlet contentK = new ContentletDataGen(contentType).languageId(language.getId()).setProperty(titleField.variable(), "K").nextPersistedAndPublish();

        final Contentlet contentACompleteANdPublish = setFieldsAndPublish(contentA, relationshipField,
                storyBlockField, contentB, contentC);

        final Contentlet contentBCompleteANdPublish = setFieldsAndPublish(contentB, relationshipField,
                storyBlockField, contentD, contentE);

        final Contentlet contentCCompleteANdPublish = setFieldsAndPublish(contentC, relationshipField,
                storyBlockField, contentF, contentG);

        final Contentlet contentFCompleteANdPublish = setFieldsAndPublish(contentF, relationshipField,
                storyBlockField, contentH, contentI);

        final Contentlet contentHCompleteANdPublish = setFieldsAndPublish(contentH, relationshipField,
                storyBlockField, contentJ, contentK);

        final HttpServletRequest oldThreadRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse oldThreadResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();

        try {
            final HttpServletRequest request  = mock(HttpServletRequest.class);
            when(request.getAttribute(WebKeys.HTMLPAGE_DEPTH)).thenReturn(String.valueOf(depth));
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            final HttpServletResponse response  = mock(HttpServletResponse.class);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(response);

            Contentlet contentAFromAPI = APILocator.getContentletAPI()
                    .find(contentACompleteANdPublish.getInode(), APILocator.systemUser(), false);

            Map<String, Object> blockEditorMap = JsonUtil.getJsonFromString(contentAFromAPI.getStringProperty(storyBlockField.variable()));
            List<Map<String, Object>> blockValue = (List<Map<String, Object>>) blockEditorMap.get("content");

            assertEquals(2, blockValue.size());

            Map<String, Object> blockEditorItem = blockValue.get(0);
            final Map<String, Object> blockEditorContent = (Map<String, Object>) ((Map<String, Object>) blockEditorItem.get("attrs")).get("data");
            assertEquals(contentC.getIdentifier(), blockEditorContent.get("identifier"));

            if (depth == 0) {
                assertEquals(contentF.getIdentifier(), blockEditorContent.get(relationshipField.variable()));
            } else if (depth == 1) {
                assertEquals(contentF.getIdentifier(),
                        ((Map<String, Object>) blockEditorContent.get(relationshipField.variable())).get("identifier"));

                assertNull(contentF.getIdentifier(),
                        ((Map<String, Object>) blockEditorContent.get(relationshipField.variable())).get(relationshipField.variable()));
            } else if (depth > 1) {
                assertEquals(contentF.getIdentifier(),
                        ((Map<String, Object>) blockEditorContent.get(relationshipField.variable())).get("identifier"));

                final Map<String, Object> secondLevelRelatedContent = (Map<String, Object>) blockEditorContent.get(relationshipField.variable());
                assertEquals(contentF.getIdentifier(), secondLevelRelatedContent.get("identifier"));

                if (depth == 2) {
                    assertEquals(contentH.getIdentifier(), secondLevelRelatedContent.get(relationshipField.variable()));
                } else {
                    assertEquals(contentH.getIdentifier(),
                            ((Map<String, Object>) secondLevelRelatedContent.get(relationshipField.variable())).get("identifier"));

                    assertNull(
                            ((Map<String, Object>) secondLevelRelatedContent.get(relationshipField.variable()))
                                    .get(relationshipField.variable()));
                }
            }
        }finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(oldThreadRequest);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(oldThreadResponse);
        }
    }

    private static Contentlet setFieldsAndPublish(final Contentlet parentContent, final Field relationshipField,
                                                                   final Field storyBlockField, Contentlet relatedContent, Contentlet insideContentEditor)
            throws DotDataException, DotSecurityException {

        final Contentlet checkout = ContentletDataGen.checkout(parentContent);

        final ContentletRelationships contentletARelationships = setRelationshipField(relationshipField, checkout, relatedContent);
        setBlockEditorField(checkout, storyBlockField, insideContentEditor);

        //Checkin of the parent to validate Relationships
        final Contentlet contentAComplete = APILocator.getContentletAPI().checkin(checkout, contentletARelationships,
                null, null, APILocator.systemUser(), false);
        return ContentletDataGen.publish(contentAComplete);
    }

    private static Contentlet setFieldsAndPublishBothAsBlockEditor(final Contentlet parentContent, final Field relationshipField,
                                                   final Field storyBlockField, Contentlet relatedContent, Contentlet insideContentEditor)
            throws DotDataException, DotSecurityException {

        final Contentlet checkout = ContentletDataGen.checkout(parentContent);

        final ContentletRelationships contentletARelationships = setRelationshipField(relationshipField, checkout, relatedContent);
        setBlockEditorField(checkout, storyBlockField, insideContentEditor, relatedContent);

        //Checkin of the parent to validate Relationships
        final Contentlet contentAComplete = APILocator.getContentletAPI().checkin(checkout, contentletARelationships,
                null, null, APILocator.systemUser(), false);
        return ContentletDataGen.publish(contentAComplete);
    }

    private static void setBlockEditorField(final Contentlet parentContent,
                                            final Field storyBlockField, final Contentlet insideBlockEditor1, Contentlet insideBlockEditor2) {
        final String storyBlockJSON = "{" +
            "\"type\": \"doc\"," +
            "\"content\": [" +
                "{" +
                    "\"type\": \"dotContent\"," +
                    "\"attrs\": {" +
                        "\"data\": {" +
                            "\"identifier\": \"%s\"," +
                            "\"languageId\": %s" +
                        "}" +
                    "}" +
                "}," +
                "{" +
                    "\"type\": \"dotContent\"," +
                    "\"attrs\": {" +
                        "\"data\": {" +
                            "\"identifier\": \"%s\"," +
                            "\"languageId\": %s" +
                        "}" +
                    "}" +
                "}," +
                "{" +
                    "\"type\": \"paragraph\"," +
                    "\"attrs\": {" +
                        "\"textAlign\": \"left\"" +
                    "}" +
                "}" +
            "]" +
        "}";

        parentContent.setProperty(storyBlockField.variable(), String.format(storyBlockJSON, insideBlockEditor1.getIdentifier(),
                insideBlockEditor1.getLanguageId(), insideBlockEditor2.getIdentifier(), insideBlockEditor2.getLanguageId()));
    }

    private static void setBlockEditorField(final Contentlet parentContent,
                                            final Field storyBlockField, final Contentlet insideBlockEditor1) {
        final String storyBlockJSON = "{" +
            "\"type\": \"doc\"," +
            "\"content\": [" +
                "{" +
                    "\"type\": \"dotContent\"," +
                    "\"attrs\": {" +
                        "\"data\": {" +
                            "\"identifier\": \"%s\"," +
                            "\"languageId\": %s" +
                        "}" +
                    "}" +
                "}," +
                "{" +
                    "\"type\": \"paragraph\"," +
                    "\"attrs\": {" +
                        "\"textAlign\": \"left\"" +
                    "}" +
                "}" +
            "]" +
        "}";

        parentContent.setProperty(storyBlockField.variable(), String.format(storyBlockJSON, insideBlockEditor1.getIdentifier(),
                insideBlockEditor1.getLanguageId()));
    }

    private static ContentletRelationships setRelationshipField(final Field relationshipField,
                                                                final Contentlet parentContent, final Contentlet relatedContent)
            throws DotDataException, DotSecurityException {

        final Relationship relationship = APILocator.getRelationshipAPI().getRelationshipFromField(relationshipField, APILocator.systemUser());
        //Relate contentlets
        final ContentletRelationships contentletRelationships = new ContentletRelationships(parentContent);

        final ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords =
                contentletRelationships.new ContentletRelationshipRecords(relationship, true);
        contentletRelationshipRecords.setRecords(CollectionsUtils.list(relatedContent));
        contentletRelationships.getRelationshipsRecords().add(contentletRelationshipRecords);
        return contentletRelationships;
    }

}
