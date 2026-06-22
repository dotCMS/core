
package com.dotcms.contenttype.business;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.business.StoryBlockDependency;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.TagDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.rendering.velocity.viewtools.VelocityRequestWrapper;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

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
     * Method to test: {@link StoryBlockAPI#refreshStoryBlockValueReferences(Object, String)}
     * Given Scenario: Creates a story block whose paragraph holds an inline contentlet reference
     * ({@code dotInlineContent}) nested inside the paragraph's {@code content} array — alongside a
     * text node — and then renames the referenced contentlet.
     * ExpectedResult: Refreshing references descends into the paragraph and re-hydrates the nested
     * inline node's {@code attrs.data.title} with the contentlet's current title. This proves the
     * "live reference" promise for inline references.
     */
    @Test
    public void test_refresh_references_for_nested_inline_content()
            throws DotDataException, DotSecurityException, JsonProcessingException {
        // 1) create a contentlet to be referenced inline
        final ContentType contentTypeRichText = APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent");
        final Contentlet referenced = new ContentletDataGen(contentTypeRichText)
                .setProperty("title", "Inline Title 1")
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT).nextPersisted();

        // 2) build a story block with a dotInlineContent node nested inside a paragraph
        final String storyBlockJson = String.format(
                "{" +
                "  \"type\": \"doc\"," +
                "  \"content\": [" +
                "    {" +
                "      \"type\": \"paragraph\"," +
                "      \"content\": [" +
                "        { \"type\": \"text\", \"text\": \"See \" }," +
                "        {" +
                "          \"type\": \"dotInlineContent\"," +
                "          \"attrs\": {" +
                "            \"data\": {" +
                "              \"identifier\": \"%s\"," +
                "              \"languageId\": 1" +
                "            }" +
                "          }" +
                "        }" +
                "      ]" +
                "    }" +
                "  ]" +
                "}",
                referenced.getIdentifier());

        // 3) rename the referenced contentlet so the stored (empty) title differs from the live one
        final Contentlet newVersion = APILocator.getContentletAPI().checkout(referenced.getInode(), APILocator.systemUser(), false);
        newVersion.setProperty("title", "Inline Title 2");
        newVersion.setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT);
        APILocator.getContentletAPI().publish(
                APILocator.getContentletAPI().checkin(newVersion, APILocator.systemUser(), false), APILocator.systemUser(), false);

        final HttpServletRequest oldThreadRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse oldThreadResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();

        try {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(new MockAttributeRequest(mock(HttpServletRequest.class)));
            HttpServletResponseThreadLocal.INSTANCE.setResponse(mock(HttpServletResponse.class));

            // 4) refresh references — the nested inline node must be reached and hydrated
            final StoryBlockReferenceResult refreshResult =
                    APILocator.getStoryBlockAPI().refreshStoryBlockValueReferences(storyBlockJson, "1234");

            assertTrue("Nested inline reference should be refreshed", refreshResult.isRefreshed());

            final Map refreshedMap = ContentletJsonHelper.INSTANCE.get().objectMapper()
                    .readValue(Try.of(() -> refreshResult.getValue().toString()).getOrElse(StringPool.BLANK),
                            LinkedHashMap.class);
            final List docContent = (List) refreshedMap.get("content");
            final Map paragraph = (Map) docContent.get(0);
            final List paragraphContent = (List) paragraph.get("content");
            final Optional<Object> inlineNode = paragraphContent.stream()
                    .filter(node -> "dotInlineContent".equals(Map.class.cast(node).get("type"))).findFirst();

            assertTrue("Inline node should remain in the paragraph content", inlineNode.isPresent());
            final Map inlineData = (Map) Map.class.cast(Map.class.cast(inlineNode.get()).get(StoryBlockAPI.ATTRS_KEY))
                    .get(StoryBlockAPI.DATA_KEY);
            assertEquals("Inline reference identifier should be preserved",
                    referenced.getIdentifier(), inlineData.get("identifier"));
            assertEquals("Inline reference title should be hydrated to the live title",
                    "Inline Title 2", inlineData.get("title"));
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
        final List<StoryBlockDependency> dependencyList = APILocator.getStoryBlockAPI().getDependencies(newStoryBlockJson3);
        assertNotNull(dependencyList);
        assertEquals(3, dependencyList.size());
        assertTrue(dependencyList.stream().anyMatch(dep -> dep.identifier().equals(richTextContentlet1.getIdentifier())));
        assertTrue(dependencyList.stream().anyMatch(dep -> dep.identifier().equals(richTextContentlet2.getIdentifier())));
        assertTrue(dependencyList.stream().anyMatch(dep -> dep.identifier().equals(richTextContentlet3.getIdentifier())));
    }

    /**
     * Method to test: {@link StoryBlockAPI#getDependencies(Object)}
     * Given Scenario: Test a story block value that is a json (html in this case) see (https://github.com/dotCMS/core/issues/24299)
     * ExpectedResult: Do not throw exception and must return zero dependencies
     */
    @Test
    public void test_get_dependencies_with_non_json_value()  {

        final Object newStoryBlockJson1        = "<html>pufff</html>";

        final List<StoryBlockDependency> dependencyList = APILocator.getStoryBlockAPI().getDependencies(newStoryBlockJson1);
        assertNotNull(dependencyList);
        assertTrue(dependencyList.isEmpty());
    }

    /**
     * Method to test: {@link StoryBlockAPI#getDependencies(Object)}
     * Given Scenario: Test a story block value that is a json (html in this case) see (https://github.com/dotCMS/core/issues/24299)
     * ExpectedResult: Do not throw exception and must return zero dependencies
     */
    @Test
    public void test_get_dependencies_with_empty_json_value()  {

        final Object newStoryBlockJson1        = "{\"test\":\"test\"}";

        final List<StoryBlockDependency> dependencyList = APILocator.getStoryBlockAPI().getDependencies(newStoryBlockJson1);
        assertNotNull(dependencyList);
        assertTrue(dependencyList.isEmpty());
    }

    /**
     * Method to test: {@link StoryBlockAPI#getDependencies(Object)}
     * Given Scenario: Test a story block value that is a json (html in this case) see (https://github.com/dotCMS/core/issues/24299)
     * ExpectedResult: Do not throw exception and must return zero dependencies
     */
    @Test
    public void test_get_dependencies_with_bad_content_value()  {

        final Object newStoryBlockJson1        = "{\"content\":\"test\"}";

        final List<StoryBlockDependency> dependencyList = APILocator.getStoryBlockAPI().getDependencies(newStoryBlockJson1);
        assertNotNull(dependencyList);
        assertTrue(dependencyList.isEmpty());
    }

    /**
     * Method to test: {@link StoryBlockAPI#getDependencies(Object)}
     * Given Scenario: Creates a story block with nested images inside lists.
     * This tests the recursive dependency detection fix where images nested
     * within list items were not being detected for push publishing bundles.
     * ExpectedResult: The nested image identifiers should be retrieved
     */
    @Test
    public void test_get_dependencies_with_nested_images_in_lists() throws Exception {

        final Host host = APILocator.systemHost();
        final Folder imageFolder = new FolderDataGen().site(host).nextPersisted();
        File tempFile1 = File.createTempFile("nestedImageTest1", ".jpg");
        File tempFile2 = File.createTempFile("nestedImageTest2", ".jpg");

        URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        Assert.assertNotNull("Can't find the test image file", url);
        File testImage = new File(url.getFile());
        FileUtils.copyFile(testImage, tempFile1);
        FileUtils.copyFile(testImage, tempFile2);

        try {
            final Contentlet imageFileAsset1 = new FileAssetDataGen(tempFile1)
                    .host(host)
                    .languageId(1L)
                    .folder(imageFolder)
                    .nextPersisted();

            final Contentlet imageFileAsset2 = new FileAssetDataGen(tempFile2)
                    .host(host)
                    .languageId(1L)
                    .folder(imageFolder)
                    .nextPersisted();

            // JSON with top level image and a bullet list containing a nested image
            final String storyBlockJsonWithNestedImages = String.format(
                    "{" +
                    "  \"type\": \"doc\"," +
                    "  \"content\": [" +
                    "    {" +
                    "      \"type\": \"dotImage\"," +
                    "      \"attrs\": {" +
                    "        \"data\": {" +
                    "          \"identifier\": \"%s\"," +
                    "          \"languageId\": 1" +
                    "        }" +
                    "      }" +
                    "    }," +
                    "    {" +
                    "      \"type\": \"bulletList\"," +
                    "      \"content\": [" +
                    "        {" +
                    "          \"type\": \"listItem\"," +
                    "          \"content\": [" +
                    "            {" +
                    "              \"type\": \"paragraph\"," +
                    "              \"content\": [" +
                    "                {" +
                    "                  \"type\": \"text\"," +
                    "                  \"text\": \"Image inside list:\"" +
                    "                }" +
                    "              ]" +
                    "            }," +
                    "            {" +
                    "              \"type\": \"dotImage\"," +
                    "              \"attrs\": {" +
                    "                \"data\": {" +
                    "                  \"identifier\": \"%s\"," +
                    "                  \"languageId\": 1" +
                    "                }" +
                    "              }" +
                    "            }" +
                    "          ]" +
                    "        }" +
                    "      ]" +
                    "    }" +
                    "  ]" +
                    "}",
                    imageFileAsset1.getIdentifier(),
                    imageFileAsset2.getIdentifier()
            );


            final List<StoryBlockDependency> dependencyList = APILocator.getStoryBlockAPI()
                    .getDependencies(storyBlockJsonWithNestedImages);

            // Verify both images are detected
            assertNotNull("Dependency list should not be null", dependencyList);
            assertEquals("Should find 2 image dependencies", 2, dependencyList.size());
            assertTrue("Should contain top-level image",
                    dependencyList.stream().anyMatch(dep -> dep.identifier().equals(imageFileAsset1.getIdentifier())));
            assertTrue("Should contain nested image inside list",
                    dependencyList.stream().anyMatch(dep -> dep.identifier().equals(imageFileAsset2.getIdentifier())));

        } finally {
            
            if (tempFile1.exists()) {
                tempFile1.delete();
            }
            if (tempFile2.exists()) {
                tempFile2.delete();
            }
        }
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
     * Method to test: {@link StoryBlockAPI#refreshStoryBlockValueReferences(Object, String)}
     * Given Scenario: A non-object JSON scalar (number, string, boolean, array) is passed in —
     * these are valid JSON tokens but are not Story Block documents. They can reach this method
     * via {@code refreshNestedStoryBlockValues} when iterating over scalar field values on
     * related contentlets.
     * ExpectedResult: No exception must be thrown and the original value must be returned
     * unchanged. Regression test for "/api/content/_search failing with
     * MismatchedInputException: Cannot deserialize value of type LinkedHashMap from Integer".
     */
    @Test
    public void test_refreshStoryBlockValueReferences_with_non_object_json_scalars() {
        final StoryBlockAPI storyBlockAPI = APILocator.getStoryBlockAPI();

        // Bare integer — valid JSON, but not an object. Was the trigger of the original bug.
        StoryBlockReferenceResult result = storyBlockAPI.refreshStoryBlockValueReferences("42", "parent-id");
        assertNotNull(result);
        assertFalse(result.isRefreshed());
        assertEquals("42", result.getValue());

        // Bare quoted string — also valid JSON.
        result = storyBlockAPI.refreshStoryBlockValueReferences("\"hello\"", "parent-id");
        assertNotNull(result);
        assertFalse(result.isRefreshed());

        // Bare boolean.
        result = storyBlockAPI.refreshStoryBlockValueReferences("true", "parent-id");
        assertNotNull(result);
        assertFalse(result.isRefreshed());

        // JSON array — valid JSON, but not a Story Block document object.
        result = storyBlockAPI.refreshStoryBlockValueReferences("[1,2,3]", "parent-id");
        assertNotNull(result);
        assertFalse(result.isRefreshed());

        // Untransformed HTML body content — not JSON at all, must be returned untouched.
        final String html = "<p>Hello <strong>world</strong></p>";
        result = storyBlockAPI.refreshStoryBlockValueReferences(html, "parent-id");
        assertNotNull(result);
        assertFalse(result.isRefreshed());
        assertEquals(html, result.getValue());
    }

    /**
     * Method to test: {@link StoryBlockAPI#refreshStoryBlockValueReferences(Object, String)}
     * Given Scenario: A Story Block document that contains two children, where the first
     * child is malformed (missing the {@code type} key, which would have caused a
     * NullPointerException inside {@code isRefreshed}). The second child is well-formed.
     * ExpectedResult: No exception is propagated. The bad child is skipped and the call
     * still returns a non-null result so the surrounding contentlet (and the rest of the
     * search response) is not aborted by a single bad nested reference.
     */
    /**
     * Method to test: {@link StoryBlockAPI#refreshReferences(Contentlet)}
     * Given Scenario: A contentlet has a Story Block field whose value contains a malformed
     * nested child (missing the {@code type} key).
     * ExpectedResult: refreshReferences must complete normally (no thrown exception). This is
     * the resilience boundary that prevents one bad contentlet from aborting an entire
     * /api/content/_search response when ContentletTransformer iterates over the result set.
     */
    @Test
    public void test_refreshReferences_does_not_throw_on_malformed_nested_block()
            throws DotDataException, DotSecurityException {
        ContentType storyBlockType = null;
        try {
            // Reuse an existing helper pattern: any content type with a Story Block field.
            final long timestamp = System.currentTimeMillis();
            storyBlockType = new ContentTypeDataGen()
                    .name("storyBlockResilience" + timestamp)
                    .velocityVarName("storyBlockResilience" + timestamp)
                    .nextPersisted();
            final Field storyBlockField = new FieldDataGen()
                    .type(StoryBlockField.class)
                    .contentTypeId(storyBlockType.id())
                    .nextPersisted();

            final String malformedStoryBlock =
                    "{"
                    + "\"type\":\"doc\","
                    + "\"attrs\":{},"
                    + "\"content\":["
                    + "  {\"attrs\":{\"data\":{\"identifier\":\"missing-type-key\"}}}"
                    + "]"
                    + "}";

            final Contentlet contentlet = new ContentletDataGen(storyBlockType.id())
                    .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                    .setProperty(storyBlockField.variable(), malformedStoryBlock)
                    .nextPersisted();

            try {
                APILocator.getStoryBlockAPI().refreshReferences(contentlet);
            } catch (final Throwable t) {
                Assert.fail("refreshReferences must not propagate exceptions for a single "
                        + "malformed Story Block: " + t.getMessage());
            }
        } finally {
            if (storyBlockType != null) {
                ContentTypeDataGen.remove(storyBlockType);
            }
        }
    }

    @Test
    public void test_refreshStoryBlockValueReferences_isolates_bad_child_block() {
        final String storyBlockWithBadChild =
                "{"
                + "\"type\":\"doc\","
                + "\"attrs\":{},"
                + "\"content\":["
                + "  {\"attrs\":{\"data\":{\"identifier\":\"missing-type-key\"}}},"
                + "  {\"type\":\"paragraph\",\"content\":[]}"
                + "]"
                + "}";

        StoryBlockReferenceResult result = null;
        try {
            result = APILocator.getStoryBlockAPI()
                    .refreshStoryBlockValueReferences(storyBlockWithBadChild, "parent-resilience");
        } catch (final Throwable t) {
            Assert.fail("A malformed nested block must not abort the parent refresh: " + t.getMessage());
        }
        assertNotNull(result);
    }

    /**
     * Method to test: {@link StoryBlockAPI#refreshReferences(Contentlet)}
     * Given Scenario: This will create 2 block contents, adds a rich content to each block content and retrieve the json.
     * ExpectedResult: The new json will contain the rich text data map for each block content.
     */
    @Test
    public void test_refresh_references_multiple_blocks()
            throws DotDataException, DotSecurityException, JsonProcessingException, IOException {

        ContentType storyBlockType = null;

        final HttpServletRequest oldThreadRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse oldThreadResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();

        try {
            // 1) get the default language
            final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

            // 2) create 2 rich text contentlets with some initial values
            final ContentType contentTypeRichText = APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent");
            final Contentlet richTextContentlet1 = new ContentletDataGen(contentTypeRichText)
                    .languageId(defaultLanguage.getId())
                    .setProperty("title","Title1")
                    .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                    .nextPersistedAndPublish();
            final Contentlet richTextContentlet2 = new ContentletDataGen(contentTypeRichText)
                    .languageId(defaultLanguage.getId())
                    .setProperty("title","Title2")
                    .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                    .nextPersistedAndPublish();

            // 3) create a StoryBlockField and a ContentType with it
            final Field storyBlockField = new FieldDataGen()
                    .type(StoryBlockField.class)
                    .name("StoryBlockTestField")
                    .velocityVarName("storyBlockTestField")
                    .next();
            storyBlockType = new ContentTypeDataGen().field(storyBlockField).nextPersisted();

            // 4) create first block content with first rich content
            final Contentlet firstBlockContentlet = new ContentletDataGen(storyBlockType)
                    .languageId(defaultLanguage.getId())
                    .nextPersisted();
            final Contentlet firstBlockCheckout = ContentletDataGen.checkout(firstBlockContentlet);
            setBlockEditorField(firstBlockCheckout, storyBlockField, richTextContentlet1);
            final Contentlet firstBlockComplete = APILocator.getContentletAPI().checkin(
                    firstBlockCheckout, APILocator.systemUser(), false);
            ContentletDataGen.publish(firstBlockComplete);

            // 5) create second block content with second rich content
            final Contentlet secondBlockContentlet = new ContentletDataGen(storyBlockType)
                    .languageId(defaultLanguage.getId())
                    .nextPersisted();
            final Contentlet secondBlockCheckout = ContentletDataGen.checkout(secondBlockContentlet);
            setBlockEditorField(secondBlockCheckout, storyBlockField, richTextContentlet2);
            final Contentlet secondBlockComplete = APILocator.getContentletAPI().checkin(
                    secondBlockCheckout, APILocator.systemUser(), false);
            ContentletDataGen.publish(secondBlockComplete);

            // 6) now we have 2 block contents, each one with a rich content, we are going to refresh the references
            final HttpServletRequest attrRequest = new MockAttributeRequest(mock(HttpServletRequest.class));
            attrRequest.setAttribute("USER", APILocator.systemUser());
            attrRequest.setAttribute(WebKeys.PAGE_MODE_PARAMETER, PageMode.LIVE);
            attrRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");

            final HttpServletRequest request = VelocityRequestWrapper.wrapVelocityRequest(attrRequest);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            final HttpServletResponse response = mock(HttpServletResponse.class);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(response);

            // 7) verify first rich content
            final Contentlet firstBlockPublished = APILocator.getContentletAPI().find(
                    firstBlockComplete.getInode(), APILocator.systemUser(), false);
            assertNotNull(firstBlockPublished);

            final Map<?, ?> firstDataMap = getDataMap(firstBlockPublished, storyBlockField.variable());
            assertNotNull(firstDataMap);

            assertEquals(richTextContentlet1.getIdentifier(), firstDataMap.get("identifier"));
            assertEquals(richTextContentlet1.getStringProperty("title"), firstDataMap.get("title"));

            // 8) verify second rich content
            final Contentlet secondBlockPublished = APILocator.getContentletAPI().find(
                    secondBlockComplete.getInode(), APILocator.systemUser(), false);
            assertNotNull(secondBlockPublished);

            final Map<?, ?> secondDataMap = getDataMap(secondBlockPublished, storyBlockField.variable());
            assertNotNull(secondDataMap);

            assertEquals(richTextContentlet2.getIdentifier(), secondDataMap.get("identifier"));
            assertEquals(richTextContentlet2.getStringProperty("title"), secondDataMap.get("title"));

        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(oldThreadRequest);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(oldThreadResponse);
            if (storyBlockType != null) {
                ContentTypeDataGen.remove(storyBlockType);
            }
        }
    }

    /**
     * Helper method to get the data map from the first Contentlet that is referenced in a StoryBlockField.
     * @param storyBlockContentlet The Contentlet that contains the StoryBlockField.
     * @param storyBlockField The StoryBlockField variable name.
     * @return A map containing the data from the referenced Contentlet, or null if not found.
     */
    private Map<?, ?> getDataMap(final Contentlet storyBlockContentlet, final String storyBlockField)
            throws JsonProcessingException {

        if (storyBlockContentlet == null || storyBlockField == null) return null;

        final Object storyBlockValue = storyBlockContentlet.getStringProperty(storyBlockField);
        if (storyBlockValue == null) return null;

        final Map<String, Object> blockEditorMap =
                APILocator.getStoryBlockAPI().toMap(storyBlockValue);
        if (blockEditorMap == null || blockEditorMap.isEmpty()) return null;

        final List<?> contentsMap = (List<?>) blockEditorMap.get(StoryBlockAPI.CONTENT_KEY);
        if (contentsMap == null || contentsMap.isEmpty()) return null;

        final Optional<?> firstContentletMap = contentsMap.stream()
                .filter(contentMap -> "dotContent".equals(
                    ((Map<?,?>)contentMap).get("type")))
                .findFirst();
        if (firstContentletMap.isEmpty()) return null;

        final Map<?, ?> contentletMap = (Map<?, ?>) firstContentletMap.get();
        final Map<?, ?> attrsMap = (Map<?, ?>) contentletMap.get(StoryBlockAPI.ATTRS_KEY);
        if (attrsMap == null) return null;

        return (Map<?, ?>) attrsMap.get(StoryBlockAPI.DATA_KEY);

    }

    /**
     * Extracts the first dotContent block data payload from a Story Block value represented either
     * as JSON text or map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getFirstStoryBlockContentData(final Object storyBlockValue)
            throws JsonProcessingException {
        final Map<String, Object> storyBlockMap = storyBlockValue instanceof Map
                ? (Map<String, Object>) storyBlockValue
                : APILocator.getStoryBlockAPI().toMap(storyBlockValue);
        final List<Map<String, Object>> storyBlockContent =
                (List<Map<String, Object>>) storyBlockMap.get(StoryBlockAPI.CONTENT_KEY);
        final Map<String, Object> firstContent = storyBlockContent.stream()
                .filter(item -> "dotContent".equals(item.get(StoryBlockAPI.TYPE_KEY)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No dotContent block found"));
        return (Map<String, Object>) ((Map<String, Object>) firstContent.get(StoryBlockAPI.ATTRS_KEY))
                .get(StoryBlockAPI.DATA_KEY);
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

    @Test
    @SuppressWarnings("unchecked")
    public void refreshesNestedStoryBlockContentInRelationshipChainAfterNestedPublish()
            throws Exception {
        final Language language = new LanguageDataGen().nextPersisted();

        ContentType nestedContentType = new ContentTypeDataGen().nextPersisted();

        final Field storyBlockField = new FieldDataGen()
                .type(StoryBlockField.class)
                .name("storyBlock")
                .contentTypeId(nestedContentType.id())
                .nextPersisted();

        final Field relationshipField = APILocator.getContentTypeFieldAPI().save(
                FieldBuilder.builder(RelationshipField.class)
                        .name("rel")
                        .contentTypeId(nestedContentType.id())
                        .values(String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal()))
                        .relationType(nestedContentType.variable()).build(), APILocator.systemUser());

        final Field titleField = new FieldDataGen().name("title")
                .contentTypeId(nestedContentType.id()).type(TextField.class).nextPersisted();
        // Reload the content type so subsequent contentlets include the newly persisted fields.
        nestedContentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find(nestedContentType.id());

        final Contentlet post = new ContentletDataGen(nestedContentType).languageId(language.getId())
                .setProperty(titleField.variable(), "post").nextPersisted();
        final Contentlet similarNews = new ContentletDataGen(nestedContentType).languageId(language.getId())
                .setProperty(titleField.variable(), "similar-news").nextPersisted();
        final Contentlet news = new ContentletDataGen(nestedContentType).languageId(language.getId())
                .setProperty(titleField.variable(), "news").nextPersisted();
        final Contentlet nested = new ContentletDataGen(nestedContentType).languageId(language.getId())
                .setProperty(titleField.variable(), "nested-v1").nextPersistedAndPublish();

        final Contentlet newsCheckout = ContentletDataGen.checkout(news);
        setBlockEditorField(newsCheckout, storyBlockField, nested);
        final Contentlet publishedNews = ContentletDataGen.publish(
                APILocator.getContentletAPI().checkin(newsCheckout, APILocator.systemUser(), false));

        final Contentlet similarNewsCheckout = ContentletDataGen.checkout(similarNews);
        final ContentletRelationships similarNewsRelationships =
                setRelationshipField(relationshipField, similarNewsCheckout, publishedNews);
        final Contentlet publishedSimilarNews = ContentletDataGen.publish(
                APILocator.getContentletAPI().checkin(similarNewsCheckout, similarNewsRelationships, null, null,
                        APILocator.systemUser(), false));

        final Contentlet postCheckout = ContentletDataGen.checkout(post);
        setBlockEditorField(postCheckout, storyBlockField, publishedSimilarNews);
        final Contentlet publishedPost = ContentletDataGen.publish(
                APILocator.getContentletAPI().checkin(postCheckout, APILocator.systemUser(), false));

        final HttpServletRequest oldThreadRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse oldThreadResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();

        try {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            // Depth 3 is required to traverse: post block editor -> relationship -> related news
            // block editor -> nested contentlet.
            when(request.getAttribute(WebKeys.HTMLPAGE_DEPTH)).thenReturn("3");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            final HttpServletResponse response = mock(HttpServletResponse.class);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(response);

            final Contentlet initialPost = APILocator.getContentletAPI()
                    .find(publishedPost.getInode(), APILocator.systemUser(), false);
            final Map<String, Object> initialPostData =
                    getFirstStoryBlockContentData(initialPost.get(storyBlockField.variable()));
            final Map<String, Object> initialNewsData =
                    (Map<String, Object>) initialPostData.get(relationshipField.variable());
            final Map<String, Object> initialNestedData =
                    getFirstStoryBlockContentData(initialNewsData.get(storyBlockField.variable()));
            assertEquals("nested-v1", initialNestedData.get(titleField.variable()));

            final Contentlet nestedCheckout = ContentletDataGen.checkout(nested);
            nestedCheckout.setProperty(titleField.variable(), "nested-v2");
            ContentletDataGen.publish(
                    APILocator.getContentletAPI().checkin(nestedCheckout, APILocator.systemUser(), false));

            final Contentlet refreshedPost = APILocator.getContentletAPI()
                    .find(publishedPost.getInode(), APILocator.systemUser(), false);
            final Map<String, Object> refreshedPostData =
                    getFirstStoryBlockContentData(refreshedPost.get(storyBlockField.variable()));
            final Map<String, Object> refreshedNewsData =
                    (Map<String, Object>) refreshedPostData.get(relationshipField.variable());
            final Map<String, Object> refreshedNestedData =
                    getFirstStoryBlockContentData(refreshedNewsData.get(storyBlockField.variable()));
            assertEquals("nested-v2", refreshedNestedData.get(titleField.variable()));
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(oldThreadRequest);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(oldThreadResponse);
        }
    }

    /**
     * Method to test: {@link StoryBlockAPIImpl#refreshReferences(Contentlet)}
     * When: A ONE_TO_MANY relationship chain exists:
     *   post (StoryBlock → similarNews) → similarNews (ONE_TO_MANY rel → [news1, news2])
     *     → news1 (StoryBlock → nested)
     * And the deeply-nested {@code nested} contentlet is updated and re-published.
     * Expected: The refreshed {@code post} contentlet's StoryBlock reflects the updated
     * {@code nested} title propagated through the one-to-many list.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void refreshesNestedStoryBlockContentInOneToManyRelationshipChainAfterNestedPublish()
            throws Exception {
        final Language language = new LanguageDataGen().nextPersisted();

        ContentType nestedContentType = new ContentTypeDataGen().nextPersisted();

        final Field storyBlockField = new FieldDataGen()
                .type(StoryBlockField.class)
                .name("storyBlock")
                .contentTypeId(nestedContentType.id())
                .nextPersisted();

        final Field relationshipField = APILocator.getContentTypeFieldAPI().save(
                FieldBuilder.builder(RelationshipField.class)
                        .name("rel")
                        .contentTypeId(nestedContentType.id())
                        .values(String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()))
                        .relationType(nestedContentType.variable()).build(), APILocator.systemUser());

        final Field titleField = new FieldDataGen().name("title")
                .contentTypeId(nestedContentType.id()).type(TextField.class).nextPersisted();
        nestedContentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find(nestedContentType.id());

        final Contentlet post = new ContentletDataGen(nestedContentType).languageId(language.getId())
                .setProperty(titleField.variable(), "post").nextPersisted();
        final Contentlet similarNews = new ContentletDataGen(nestedContentType).languageId(language.getId())
                .setProperty(titleField.variable(), "similar-news").nextPersisted();
        final Contentlet news1 = new ContentletDataGen(nestedContentType).languageId(language.getId())
                .setProperty(titleField.variable(), "news1").nextPersisted();
        final Contentlet news2 = new ContentletDataGen(nestedContentType).languageId(language.getId())
                .setProperty(titleField.variable(), "news2").nextPersisted();
        final Contentlet nested = new ContentletDataGen(nestedContentType).languageId(language.getId())
                .setProperty(titleField.variable(), "nested-v1").nextPersistedAndPublish();

        // news1 has a StoryBlock containing nested
        final Contentlet news1Checkout = ContentletDataGen.checkout(news1);
        setBlockEditorField(news1Checkout, storyBlockField, nested);
        final Contentlet publishedNews1 = ContentletDataGen.publish(
                APILocator.getContentletAPI().checkin(news1Checkout, APILocator.systemUser(), false));

        // similarNews has a ONE_TO_MANY relationship to [news1, news2]
        final Contentlet similarNewsCheckout = ContentletDataGen.checkout(similarNews);
        final ContentletRelationships similarNewsRelationships =
                setRelationshipFieldMultiple(relationshipField, similarNewsCheckout, publishedNews1, news2);
        final Contentlet publishedSimilarNews = ContentletDataGen.publish(
                APILocator.getContentletAPI().checkin(similarNewsCheckout, similarNewsRelationships, null, null,
                        APILocator.systemUser(), false));

        // post has a StoryBlock containing similarNews
        final Contentlet postCheckout = ContentletDataGen.checkout(post);
        setBlockEditorField(postCheckout, storyBlockField, publishedSimilarNews);
        final Contentlet publishedPost = ContentletDataGen.publish(
                APILocator.getContentletAPI().checkin(postCheckout, APILocator.systemUser(), false));

        final HttpServletRequest oldThreadRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse oldThreadResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();

        try {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            // Depth 3 traverses: post block-editor → similarNews → one-to-many list → news1 block-editor → nested.
            when(request.getAttribute(WebKeys.HTMLPAGE_DEPTH)).thenReturn("3");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            final HttpServletResponse response = mock(HttpServletResponse.class);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(response);

            // Verify initial state: nested title is "nested-v1" through the 1-M chain
            final Contentlet initialPost = APILocator.getContentletAPI()
                    .find(publishedPost.getInode(), APILocator.systemUser(), false);
            final Map<String, Object> initialSimilarNewsData =
                    getFirstStoryBlockContentData(initialPost.get(storyBlockField.variable()));
            final List<Map<String, Object>> initialNewsList =
                    (List<Map<String, Object>>) initialSimilarNewsData.get(relationshipField.variable());
            final Map<String, Object> initialNews1Data = initialNewsList.stream()
                    .filter(n -> publishedNews1.getIdentifier().equals(n.get("identifier")))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("news1 not found in 1-M relationship list"));
            final Map<String, Object> initialNestedData =
                    getFirstStoryBlockContentData(initialNews1Data.get(storyBlockField.variable()));
            assertEquals("nested-v1", initialNestedData.get(titleField.variable()));

            // Update nested to v2
            final Contentlet nestedCheckout = ContentletDataGen.checkout(nested);
            nestedCheckout.setProperty(titleField.variable(), "nested-v2");
            ContentletDataGen.publish(
                    APILocator.getContentletAPI().checkin(nestedCheckout, APILocator.systemUser(), false));

            // Verify post reflects "nested-v2" through the one-to-many chain
            final Contentlet refreshedPost = APILocator.getContentletAPI()
                    .find(publishedPost.getInode(), APILocator.systemUser(), false);
            final Map<String, Object> refreshedSimilarNewsData =
                    getFirstStoryBlockContentData(refreshedPost.get(storyBlockField.variable()));
            final List<Map<String, Object>> refreshedNewsList =
                    (List<Map<String, Object>>) refreshedSimilarNewsData.get(relationshipField.variable());
            final Map<String, Object> refreshedNews1Data = refreshedNewsList.stream()
                    .filter(n -> publishedNews1.getIdentifier().equals(n.get("identifier")))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("news1 not found in 1-M relationship list"));
            final Map<String, Object> refreshedNestedData =
                    getFirstStoryBlockContentData(refreshedNews1Data.get(storyBlockField.variable()));
            assertEquals("nested-v2", refreshedNestedData.get(titleField.variable()));
        } finally {
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

    private static ContentletRelationships setRelationshipFieldMultiple(final Field relationshipField,
                                                                        final Contentlet parentContent,
                                                                        final Contentlet... relatedContents)
            throws DotDataException, DotSecurityException {

        final Relationship relationship = APILocator.getRelationshipAPI().getRelationshipFromField(relationshipField, APILocator.systemUser());
        final ContentletRelationships contentletRelationships = new ContentletRelationships(parentContent);

        final ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords =
                contentletRelationships.new ContentletRelationshipRecords(relationship, true);
        contentletRelationshipRecords.setRecords(CollectionsUtils.list(relatedContents));
        contentletRelationships.getRelationshipsRecords().add(contentletRelationshipRecords);
        return contentletRelationships;
    }


    /**
     * Method to test: {@link StoryBlockAPI#addContentlet(Object, Contentlet)} in conjunction with {@link StoryBlockAPIImpl#toMap(Object)}
     * Given Scenario: Test that when a contentlet with an image is added to a story block,
     * the title image information is properly included in the data map
     * ExpectedResult: The story block data should contain hasTitleImage=true and titleImage=field variable name
     */
    @Test
    public void test_loadCommonContentletProps_with_title_image()
            throws DotDataException, DotSecurityException, IOException {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mock(HttpServletRequest.class));
        final StoryBlockAPI storyBlockAPI = APILocator.getStoryBlockAPI();
        ContentType contentTypeWithImage = null;
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        long time = System.currentTimeMillis();
        final String parentContentTypeName = "ParentImageTestContentType_" + time;
        final String childContentTypeName = "TitleImageTestContentType_" + time;
        final Host host = APILocator.systemHost();
        final Folder imageFolder = new FolderDataGen().site(host).nextPersisted();
        File tempFile = File.createTempFile("contentWithImageBundleTest", ".jpg");
        URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        Assert.assertNotNull("Can't find the test image file",url);
        File testImage = new File(url.getFile());
        FileUtils.copyFile(testImage, tempFile);

        try {
            // 1) Create the content-type and the respective fields
            final Contentlet imageFileAsset = new FileAssetDataGen(tempFile)
                    .host(host)
                    .languageId(1L)
                    .folder(imageFolder).nextPersisted();

            // Create the title field
            com.dotcms.contenttype.model.field.Field titleField = new FieldDataGen()
                    .name("title")
                    .velocityVarName("title")
                    .type(TextField.class)
                    .required(true)
                    .next();

            // Create the Image field
            com.dotcms.contenttype.model.field.Field imageField = new FieldDataGen()
                    .name("image")
                    .velocityVarName("image")
                    .type(ImageField.class)
                    .required(false)
                    .next();

            // Add a binary image
            com.dotcms.contenttype.model.field.Field binaryField = new FieldDataGen()
                    .name("fileAsset")
                    .velocityVarName("fileAsset")
                    .type(BinaryField.class)
                    .next();

            // Create a content type with both fields
            ContentType childContentType = new ContentTypeDataGen()
                    .name(childContentTypeName)
                    .velocityVarName(childContentTypeName)
                    .host(host)
                    .fields(List.of(titleField, imageField, binaryField))
                    .nextPersisted();

            // 2) Create the contentlet with the image field
            final Contentlet contentletWithImage = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId())
                    .host(host)
                    .setProperty("title", "Testing StoryBlock image property")
                    .setProperty(imageField.variable(), imageFileAsset.getIdentifier())
                    .setProperty("fileAsset",tempFile)
                    .nextPersisted();

            //  create the content that has the BlockEditor
            com.dotcms.contenttype.model.field.Field blockEditoField = new FieldDataGen()
                    .name("blockEditor")
                    .velocityVarName("blockEditor")
                    .type(StoryBlockField.class)
                    .next();

            ContentType parentContentType = new ContentTypeDataGen()
                    .name(parentContentTypeName)
                    .velocityVarName(parentContentTypeName)
                    .host(host)
                    .fields(List.of(blockEditoField))
                    .nextPersisted();

            // Create a contentlet with a blockEditor to hold contentlets
            final Contentlet parentWithBlockEditor = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setProperty("blockEditor", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                    .host(host)
                    .nextPersisted();

            //Modify the block editor adding the contentlet with the images
            final Contentlet checkout = ContentletDataGen.checkout(parentWithBlockEditor);
            final Object newStoryBlockJson = APILocator.getStoryBlockAPI().addContentlet(JSON, contentletWithImage);
            //Set the blockEditor json to the parent
            checkout.setProperty("blockEditor", newStoryBlockJson);
            final Contentlet checkin = ContentletDataGen.checkin(checkout);
            final Contentlet published = ContentletDataGen.publish(checkin);

            //Call the apis
            final StoryBlockReferenceResult storyBlockReferenceResult = storyBlockAPI
                    .refreshReferences(published);

            //Extract updated fields
            Assert.assertTrue(storyBlockReferenceResult.isRefreshed());
            final Contentlet parentContent = (Contentlet)storyBlockReferenceResult.getValue();
            final Object blockEditor = parentContent.get("blockEditor");
            final Map<String, Object> storyBlockMap = storyBlockAPI.toMap(blockEditor);
            assertNotNull(storyBlockMap);

            final List<Map<String, Object>> contentList = (List<Map<String, Object>>) storyBlockMap.get("content");
            final Optional<Map<String, Object>> contentletMap = contentList.stream()
                    .filter(content -> "dotContent".equals(content.get("type")))
                    .findFirst();

            assertTrue(contentletMap.isPresent());
            final Map<String, Object> dataMap = (Map<String, Object>)
                    ((Map<String, Object>) contentletMap.get().get(StoryBlockAPI.ATTRS_KEY)).get(StoryBlockAPI.DATA_KEY);

            // And Verify...
            //Verify that we have a hasImageTitle and the imageTitle per se
            assertEquals("Should have title image", true, dataMap.get(Contentlet.HAS_TITLE_IMAGE_KEY));
            assertEquals("Title image field should match", "fileAsset", dataMap.get(Contentlet.TITLE_IMAGE_KEY));

            // Verify the image field is set correctly
            assertEquals("Image field should match", imageFileAsset.getIdentifier(), dataMap.get("image"));

            // Verify the binary field is correctly set as an url
            assertThat(dataMap.get("fileAsset").toString(), startsWith("http"));

        } finally {
            if (contentTypeWithImage != null) {
                ContentTypeDataGen.remove(contentTypeWithImage);
            }
        }
    }

    /**
     * Method to test: {@link StoryBlockAPI#refreshReferences(Contentlet)}
     * Given Scenario: Test that when a nested contentlet inside a story block has a Category field
     * with assigned categories, the category field is hydrated in the nested data map.
     * ExpectedResult: The nested contentlet data should contain the category field with its category values.
     */
    @Test
    public void test_loadCommonContentletProps_with_category_field()
            throws DotDataException, DotSecurityException, JsonProcessingException {
        final HttpServletRequest oldThreadRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mock(HttpServletRequest.class));
        final StoryBlockAPI storyBlockAPI = APILocator.getStoryBlockAPI();
        ContentType parentContentType = null;
        ContentType childContentType = null;
        Category parentCategory = null;
        Category childCategory = null;
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final long time = System.currentTimeMillis();
        final String parentContentTypeName = "ParentCategoryTestContentType_" + time;
        final String childContentTypeName = "NestedCategoryTestContentType_" + time;
        final Host host = APILocator.systemHost();

        try {
            parentCategory = new CategoryDataGen()
                    .setCategoryName("ParentCategory_" + time)
                    .setKey("ParentCategoryKey_" + time)
                    .setCategoryVelocityVarName("parentCategoryVar_" + time)
                    .nextPersisted();

            childCategory = new CategoryDataGen()
                    .setCategoryName("ChildCategory_" + time)
                    .setKey("ChildCategoryKey_" + time)
                    .setCategoryVelocityVarName("childCategoryVar_" + time)
                    .parent(parentCategory)
                    .nextPersisted();

            final Field titleField = new FieldDataGen()
                    .name("title")
                    .velocityVarName("title")
                    .type(TextField.class)
                    .required(true)
                    .next();

            final Field categoryField = new FieldDataGen()
                    .name("targeting")
                    .velocityVarName("targeting")
                    .type(CategoryField.class)
                    .values(parentCategory.getInode())
                    .next();

            childContentType = new ContentTypeDataGen()
                    .name(childContentTypeName)
                    .velocityVarName(childContentTypeName)
                    .host(host)
                    .fields(List.of(titleField, categoryField))
                    .nextPersisted();

            final Contentlet contentletWithCategory = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId())
                    .host(host)
                    .setProperty("title", "Testing StoryBlock category property")
                    .addCategory(childCategory)
                    .nextPersistedAndPublish();

            final Field blockEditorField = new FieldDataGen()
                    .name("blockEditor")
                    .velocityVarName("blockEditor")
                    .type(StoryBlockField.class)
                    .next();

            parentContentType = new ContentTypeDataGen()
                    .name(parentContentTypeName)
                    .velocityVarName(parentContentTypeName)
                    .host(host)
                    .fields(List.of(blockEditorField))
                    .nextPersisted();

            final Contentlet parentWithBlockEditor = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setProperty("blockEditor", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                    .host(host)
                    .nextPersisted();

            final Contentlet checkout = ContentletDataGen.checkout(parentWithBlockEditor);
            final Object newStoryBlockJson = storyBlockAPI.addContentlet(JSON, contentletWithCategory);
            checkout.setProperty("blockEditor", newStoryBlockJson);
            final Contentlet checkin = ContentletDataGen.checkin(checkout);
            final Contentlet published = ContentletDataGen.publish(checkin);

            final StoryBlockReferenceResult storyBlockReferenceResult = storyBlockAPI.refreshReferences(published);
            Assert.assertTrue(storyBlockReferenceResult.isRefreshed());

            final Contentlet parentContent = (Contentlet) storyBlockReferenceResult.getValue();
            final Map<String, Object> storyBlockMap = storyBlockAPI.toMap(parentContent.get("blockEditor"));
            assertNotNull(storyBlockMap);

            final List<Map<String, Object>> contentList = (List<Map<String, Object>>) storyBlockMap.get("content");
            final Optional<Map<String, Object>> contentletMap = contentList.stream()
                    .filter(content -> "dotContent".equals(content.get("type")))
                    .findFirst();

            assertTrue("Expected dotContent type in story block", contentletMap.isPresent());
            final Map<String, Object> dataMap = (Map<String, Object>)
                    ((Map<String, Object>) contentletMap.get().get(StoryBlockAPI.ATTRS_KEY)).get(StoryBlockAPI.DATA_KEY);
            assertTrue("Category field should be present", dataMap.containsKey("targeting"));
            assertNotNull("Category field value should not be null", dataMap.get("targeting"));

            final Map<String, Object> targeting = (Map<String, Object>) dataMap.get("targeting");
            assertNotNull("Targeting category map should not be null", targeting);
            assertNotNull("Category list should be present", targeting.get("categories"));
            final List<Map<String, Object>> categories = (List<Map<String, Object>>) targeting.get("categories");
            assertNotNull(categories);
            assertFalse("Category values should not be empty", categories.isEmpty());

            final Map<String, Object> firstCategory = categories.get(0);
            assertEquals("Category inode should match", childCategory.getInode(), firstCategory.get("inode"));
            assertEquals("Category name should match", childCategory.getCategoryName(), firstCategory.get("name"));
            assertEquals("Category key should match", childCategory.getKey(), firstCategory.get("key"));
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(oldThreadRequest);
            if (childContentType != null) {
                ContentTypeDataGen.remove(childContentType);
            }
            if (parentContentType != null) {
                ContentTypeDataGen.remove(parentContentType);
            }
            if (childCategory != null && UtilMethods.isSet(childCategory.getInode())) {
                APILocator.getCategoryAPI().delete(childCategory, APILocator.systemUser(), false);
            }
            if (parentCategory != null && UtilMethods.isSet(parentCategory.getInode())) {
                APILocator.getCategoryAPI().delete(parentCategory, APILocator.systemUser(), false);
            }
        }
    }

    /**
     * Method to test: {@link StoryBlockAPI#refreshReferences(Contentlet)}
     * Given Scenario: A nested contentlet in a StoryBlock has a {@link TagField}. After calling
     * {@code refreshReferences(...)}, the Tag field should be hydrated in the nested contentlet's
     * {@code attrs.data} map as a comma-separated string of tag names. The empty-tags case (no tags
     * assigned) should result in no entry for the field in the map.
     * ExpectedResult: The nested contentlet data should contain the tag field with its tag value.
     */
    @Test
    public void test_loadCommonContentletProps_with_tag_field()
            throws DotDataException, DotSecurityException, JsonProcessingException {
        final HttpServletRequest oldThreadRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mock(HttpServletRequest.class));
        final StoryBlockAPI storyBlockAPI = APILocator.getStoryBlockAPI();
        ContentType parentContentType = null;
        ContentType childContentType = null;
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final long time = System.currentTimeMillis();
        final String parentContentTypeName = "ParentTagTestContentType_" + time;
        final String childContentTypeName = "NestedTagTestContentType_" + time;
        final Host host = APILocator.systemHost();

        try {
            final String tagName = "storytag_" + time;
            new TagDataGen().name(tagName).nextPersisted();

            final Field titleField = new FieldDataGen()
                    .name("title")
                    .velocityVarName("title")
                    .type(TextField.class)
                    .required(true)
                    .next();

            final Field tagField = new FieldDataGen()
                    .name("tags")
                    .velocityVarName("tags")
                    .type(TagField.class)
                    .next();

            childContentType = new ContentTypeDataGen()
                    .name(childContentTypeName)
                    .velocityVarName(childContentTypeName)
                    .host(host)
                    .fields(List.of(titleField, tagField))
                    .nextPersisted();

            final Contentlet contentletWithTag = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId())
                    .host(host)
                    .setProperty("title", "Testing StoryBlock tag property")
                    .setProperty("tags", tagName)
                    .nextPersistedAndPublish();

            final Field blockEditorField = new FieldDataGen()
                    .name("blockEditor")
                    .velocityVarName("blockEditor")
                    .type(StoryBlockField.class)
                    .next();

            parentContentType = new ContentTypeDataGen()
                    .name(parentContentTypeName)
                    .velocityVarName(parentContentTypeName)
                    .host(host)
                    .fields(List.of(blockEditorField))
                    .nextPersisted();

            final Contentlet parentWithBlockEditor = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setProperty("blockEditor", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                    .host(host)
                    .nextPersisted();

            final Contentlet checkout = ContentletDataGen.checkout(parentWithBlockEditor);
            final Object newStoryBlockJson = storyBlockAPI.addContentlet(JSON, contentletWithTag);
            checkout.setProperty("blockEditor", newStoryBlockJson);
            final Contentlet checkin = ContentletDataGen.checkin(checkout);
            final Contentlet published = ContentletDataGen.publish(checkin);

            final StoryBlockReferenceResult storyBlockReferenceResult = storyBlockAPI.refreshReferences(published);
            Assert.assertTrue(storyBlockReferenceResult.isRefreshed());

            final Contentlet parentContent = (Contentlet) storyBlockReferenceResult.getValue();
            final Map<String, Object> storyBlockMap = storyBlockAPI.toMap(parentContent.get("blockEditor"));
            assertNotNull(storyBlockMap);

            final List<Map<String, Object>> contentList = (List<Map<String, Object>>) storyBlockMap.get("content");
            final Optional<Map<String, Object>> contentletMap = contentList.stream()
                    .filter(content -> "dotContent".equals(content.get("type")))
                    .findFirst();

            assertTrue("Expected dotContent type in story block", contentletMap.isPresent());
            final Map<String, Object> dataMap = (Map<String, Object>)
                    ((Map<String, Object>) contentletMap.get().get(StoryBlockAPI.ATTRS_KEY)).get(StoryBlockAPI.DATA_KEY);
            assertTrue("Tag field should be present in data map", dataMap.containsKey("tags"));
            assertNotNull("Tag field value should not be null", dataMap.get("tags"));

            final String tagValue = (String) dataMap.get("tags");
            assertTrue("Tag value should contain the tag name", tagValue.contains(tagName));
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(oldThreadRequest);
            if (childContentType != null) {
                ContentTypeDataGen.remove(childContentType);
            }
            if (parentContentType != null) {
                ContentTypeDataGen.remove(parentContentType);
            }
        }
    }

    /**
     * Method to test: {@link StoryBlockAPI#refreshStoryBlockValueReferences(Object, String)}
     * Given Scenario: Test the fix for issue-33900 where the data attribute could be a JSON String
     * instead of a Map, causing a ClassCastException. This test covers three scenarios:
     * 1. Data as a JSON String (the bug case) - should parse successfully
     * 2. Data as a Map (normal case) - should process successfully
     * 3. Data as an invalid type (Integer) - should handle gracefully
     * 4. Data as an invalid JSON String - should handle gracefully
     * ExpectedResult: All scenarios should be handled without throwing ClassCastException
     */
    @Test
    public void test_refreshStoryBlockValueReferences_data_field_as_string_vs_map() throws DotDataException, DotSecurityException {
        final ContentType contentTypeRichText = APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent");
        final Contentlet richTextContentlet = new ContentletDataGen(contentTypeRichText)
                .setProperty("title", "Test Content")
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .nextPersistedAndPublish();

        final HttpServletRequest oldThreadRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse oldThreadResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();

        try {
            final HttpServletRequest request = new MockAttributeRequest(mock(HttpServletRequest.class));
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            final HttpServletResponse response = mock(HttpServletResponse.class);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(response);

            // Test Case 1: Data field as JSON String (the bug case that caused ClassCastException)
            final String storyBlockJsonWithDataAsString = String.format(
                    "{" +
                    "  \"type\": \"doc\"," +
                    "  \"content\": [" +
                    "    {" +
                    "      \"type\": \"dotContent\"," +
                    "      \"attrs\": {" +
                    "        \"data\": \"{\\\"identifier\\\":\\\"%s\\\",\\\"languageId\\\":%s}\"" +
                    "      }" +
                    "    }" +
                    "  ]" +
                    "}",
                    richTextContentlet.getIdentifier(),
                    richTextContentlet.getLanguageId()
            );

            final StoryBlockReferenceResult resultWithString = APILocator.getStoryBlockAPI()
                    .refreshStoryBlockValueReferences(storyBlockJsonWithDataAsString, "parent-123");

            assertNotNull("Result should not be null for data as String", resultWithString);
            assertTrue("Should successfully process data field as JSON String", resultWithString.isRefreshed());

            // Test Case 2: Data field as Map (normal/expected case)
            final String storyBlockJsonWithDataAsMap = String.format(
                    "{" +
                    "  \"type\": \"doc\"," +
                    "  \"content\": [" +
                    "    {" +
                    "      \"type\": \"dotContent\"," +
                    "      \"attrs\": {" +
                    "        \"data\": {" +
                    "          \"identifier\": \"%s\"," +
                    "          \"languageId\": %s" +
                    "        }" +
                    "      }" +
                    "    }" +
                    "  ]" +
                    "}",
                    richTextContentlet.getIdentifier(),
                    richTextContentlet.getLanguageId()
            );

            final StoryBlockReferenceResult resultWithMap = APILocator.getStoryBlockAPI()
                    .refreshStoryBlockValueReferences(storyBlockJsonWithDataAsMap, "parent-456");

            assertNotNull("Result should not be null for data as Map", resultWithMap);
            assertTrue("Should successfully process data field as Map", resultWithMap.isRefreshed());

            // Test Case 3: Data field as invalid type (Integer) - should handle gracefully
            final String storyBlockJsonWithDataAsInteger =
                    "{" +
                    "  \"type\": \"doc\"," +
                    "  \"content\": [" +
                    "    {" +
                    "      \"type\": \"dotContent\"," +
                    "      \"attrs\": {" +
                    "        \"data\": 12345" +
                    "      }" +
                    "    }" +
                    "  ]" +
                    "}";

            final StoryBlockReferenceResult resultWithInteger = APILocator.getStoryBlockAPI()
                    .refreshStoryBlockValueReferences(storyBlockJsonWithDataAsInteger, "parent-789");

            assertNotNull("Result should not be null for data as Integer", resultWithInteger);
            // Should return false in isRefreshed since data type is unexpected
            assertFalse("Should handle unexpected data type gracefully", resultWithInteger.isRefreshed());

            // Test Case 4: Data field as invalid JSON String - should handle gracefully
            final String storyBlockJsonWithInvalidJsonString =
                    "{" +
                    "  \"type\": \"doc\"," +
                    "  \"content\": [" +
                    "    {" +
                    "      \"type\": \"dotContent\"," +
                    "      \"attrs\": {" +
                    "        \"data\": \"this is not valid json {{{\"" +
                    "      }" +
                    "    }" +
                    "  ]" +
                    "}";

            final StoryBlockReferenceResult resultWithInvalidJson = APILocator.getStoryBlockAPI()
                    .refreshStoryBlockValueReferences(storyBlockJsonWithInvalidJsonString, "parent-999");

            assertNotNull("Result should not be null for invalid JSON string", resultWithInvalidJson);
            // Should return false since JSON parsing failed
            assertFalse("Should handle invalid JSON string gracefully", resultWithInvalidJson.isRefreshed());

        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(oldThreadRequest);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(oldThreadResponse);
        }
    }
}
