
package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.util.StringPool;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Test for {@link StoryBlockAPI}
 * @author jsanca
 */
public class StoryBlockAPITest extends IntegrationTestBase {

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
     * Method to test: {@link StoryBlockAPI#refreshStoryBlockValueReferences(Object)}
     * Given Scenario: This will create a story block contentlet, adds a rich content and retrieve the json.
     * Then, will update the rich content previously added, the story block contentlet should reflect the new rich text changed.
     * ExpectedResult: The new json will reflect the rich text changes
     *
     */
    @Test
    public void test_refresh_references() throws DotDataException, DotSecurityException, JsonProcessingException {

        //1) create a rich text contentlet with some initial values
        final ContentType contentTypeRichText = APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent");
        final Contentlet richTextContentlet   = new ContentletDataGen(contentTypeRichText).setProperty("title","Title1").setProperty("body","Body1").nextPersisted();

        // 2) add the contentlet to the static story block created previously
        final Object newStoryBlockJson        = APILocator.getStoryBlockAPI().addContentlet(JSON, richTextContentlet);

        // 3) convert the json to map, to start the test
        final Map    newStoryBlockMap         = ContentletJsonHelper.INSTANCE.get().objectMapper()
                                                        .readValue(Try.of(() -> newStoryBlockJson.toString())
                                                                           .getOrElse(StringPool.BLANK), LinkedHashMap.class);

        Assert.assertNotNull(newStoryBlockMap);
        final List contentList = (List) newStoryBlockMap.get("content");
        final Optional<Object> firstContentletMap = contentList.stream()
                .filter(content -> "dotContent".equals(Map.class.cast(content).get("type"))).findFirst();

        Assert.assertTrue(firstContentletMap.isPresent());
        final Map contentletMap = (Map) Map.class.cast(Map.class.cast(firstContentletMap.get()).get(StoryBlockAPI.ATTRS_KEY)).get(StoryBlockAPI.DATA_KEY);
        Assert.assertEquals(contentletMap.get("identifier"), richTextContentlet.getIdentifier());
        Assert.assertEquals(contentletMap.get("title"), richTextContentlet.getStringProperty("title"));
        Assert.assertEquals(contentletMap.get("body"),  richTextContentlet.getStringProperty("body"));

        // 4) checkout/publish the contentlet in order to do new changes
        final Contentlet newRichTextContentlet = APILocator.getContentletAPI().checkout(richTextContentlet.getInode(), APILocator.systemUser(), false);
        newRichTextContentlet.setProperty("title","Title2");
        newRichTextContentlet.setProperty("body","Body2");
        APILocator.getContentletAPI().publish(
                APILocator.getContentletAPI().checkin(newRichTextContentlet, APILocator.systemUser(), false), APILocator.systemUser(), false);

        // 5) ask for refreshing references, the new changes of the rich text contentlet should be reflected on the json
        final StoryBlockReferenceResult refreshResult = APILocator.getStoryBlockAPI().refreshStoryBlockValueReferences(newStoryBlockJson);

        // 6) check if the results are ok.
        Assert.assertTrue(refreshResult.isRefreshed());
        Assert.assertNotNull(refreshResult.getValue());
        final Map    refreshedStoryBlockMap         = ContentletJsonHelper.INSTANCE.get().objectMapper()
                                                              .readValue(Try.of(() -> refreshResult.getValue().toString())
                                                                                 .getOrElse(StringPool.BLANK), LinkedHashMap.class);
        final List refreshedContentList = (List) refreshedStoryBlockMap.get("content");
        final Optional<Object> refreshedfirstContentletMap = refreshedContentList.stream()
                                                                     .filter(content -> "dotContent".equals(Map.class.cast(content).get("type"))).findFirst();

        Assert.assertTrue(refreshedfirstContentletMap.isPresent());
        final Map refreshedContentletMap = (Map) Map.class.cast(Map.class.cast(refreshedfirstContentletMap.get()).get(StoryBlockAPI.ATTRS_KEY)).get(StoryBlockAPI.DATA_KEY);
        Assert.assertEquals(refreshedContentletMap.get("identifier"), newRichTextContentlet.getIdentifier());
        Assert.assertEquals("Title2", newRichTextContentlet.getStringProperty("title"));
        Assert.assertEquals("Body2",  newRichTextContentlet.getStringProperty("body"));
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
        final Contentlet richTextContentlet1   = new ContentletDataGen(contentTypeRichText).setProperty("title","Title1").setProperty("body","Body1").nextPersisted();
        final Contentlet richTextContentlet2   = new ContentletDataGen(contentTypeRichText).setProperty("title","Title1").setProperty("body","Body1").nextPersisted();
        final Contentlet richTextContentlet3   = new ContentletDataGen(contentTypeRichText).setProperty("title","Title1").setProperty("body","Body1").nextPersisted();

        // 2) adds the contentlets to the static story block created previously
        final Object newStoryBlockJson1        = APILocator.getStoryBlockAPI().addContentlet(JSON, richTextContentlet1);
        final Object newStoryBlockJson2        = APILocator.getStoryBlockAPI().addContentlet(newStoryBlockJson1, richTextContentlet2);
        final Object newStoryBlockJson3        = APILocator.getStoryBlockAPI().addContentlet(newStoryBlockJson2, richTextContentlet3);

        // 3) convert the json to map, to start the test
        final Map    newStoryBlockMap         = ContentletJsonHelper.INSTANCE.get().objectMapper()
                                                        .readValue(Try.of(() -> newStoryBlockJson3.toString())
                                                                           .getOrElse(StringPool.BLANK), LinkedHashMap.class);


        Assert.assertNotNull(newStoryBlockMap);
        final List<String> contentletIdList = APILocator.getStoryBlockAPI().getDependencies(newStoryBlockJson3);
        Assert.assertNotNull(contentletIdList);
        Assert.assertEquals(3, contentletIdList.size());
        Assert.assertTrue(contentletIdList.contains(richTextContentlet1.getIdentifier()));
        Assert.assertTrue(contentletIdList.contains(richTextContentlet2.getIdentifier()));
        Assert.assertTrue(contentletIdList.contains(richTextContentlet3.getIdentifier()));
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
        Assert.assertNotNull(contentletIdList);
        Assert.assertTrue(contentletIdList.isEmpty());
    }
}
