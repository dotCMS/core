
package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link StoryBlockAPI}
 * @author jsanca
 */
public class StoryBlockAPITest extends IntegrationTestBase {

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

        // 5) ask for refreshing references, the new changes of the rich text contentlet should be reflected on the json
        final StoryBlockReferenceResult refreshResult = APILocator.getStoryBlockAPI().refreshStoryBlockValueReferences(newStoryBlockJson, "1234");

        // 6) check if the results are ok.
        assertTrue(refreshResult.isRefreshed());
        assertNotNull(refreshResult.getValue());
        final Map    refreshedStoryBlockMap         = ContentletJsonHelper.INSTANCE.get().objectMapper()
                                                              .readValue(Try.of(() -> refreshResult.getValue().toString())
                                                                                 .getOrElse(StringPool.BLANK), LinkedHashMap.class);
        final List refreshedContentList = (List) refreshedStoryBlockMap.get("content");
        final Optional<Object> refreshedfirstContentletMap = refreshedContentList.stream()
                                                                     .filter(content -> "dotContent".equals(Map.class.cast(content).get("type"))).findFirst();

        assertTrue(refreshedfirstContentletMap.isPresent());
        final Map refreshedContentletMap = (Map) Map.class.cast(Map.class.cast(refreshedfirstContentletMap.get()).get(StoryBlockAPI.ATTRS_KEY)).get(StoryBlockAPI.DATA_KEY);
        assertEquals(refreshedContentletMap.get("identifier"), newRichTextContentlet.getIdentifier());
        assertEquals("Expected Generic Content title doesn't match the one in the Contentlet", "Title2", newRichTextContentlet.getStringProperty("title"));
        assertEquals("Expected Generic Content body doesn't match the one in the Contentlet", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT,  newRichTextContentlet.getStringProperty("body"));
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
    
}
