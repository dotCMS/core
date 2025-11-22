package com.dotcms.ai.workflow;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.api.DotAIAPIFacadeImpl;
import com.dotcms.ai.api.ImageAPI;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.model.AIImageRequestDTO;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class contains unit tests for the {@link OpenAIGenerateImageActionlet} class.
 *
 * @author jsanca
 */
public class OpenAIGenerateImageActionletTest {

    private static Host host;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        host = new SiteDataGen().nextPersisted();
        int port = 8080;
        final Map<String, Secret> secrets = Map.of(
                AppKeys.API_URL.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue(String.format("test", port).toCharArray())
                        .build(),

                AppKeys.API_IMAGE_URL.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue(String.format(AiTest.API_IMAGE_URL, port).toCharArray())
                        .build(),

                AppKeys.API_KEY.key,
                Secret.builder().withType(Type.STRING).withValue(AiTest.API_KEY.toCharArray()).build(),

                AppKeys.TEXT_MODEL_NAMES.key,
                Secret.builder().withType(Type.STRING).withValue(AiTest.MODEL.toCharArray()).build(),

                AppKeys.IMAGE_MODEL_NAMES.key,
                Secret.builder().withType(Type.STRING).withValue(AiTest.IMAGE_MODEL.toCharArray()).build(),

                AppKeys.IMAGE_SIZE.key,
                Secret.builder().withType(Type.SELECT).withValue(AiTest.IMAGE_SIZE.toCharArray()).build(),

                AppKeys.LISTENER_INDEXER.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue("{\"default\":\"blog\"}".toCharArray())
                        .build());
        new AppConfig(host.getHostname(), secrets);
        DotAIAPIFacadeImpl.setDefaultImageAPIProvider(initArguments -> new ImageAPI() {
            @Override
            public JSONObject sendTextPrompt(String prompt) {
                return new JSONObject("{\n" +
                        "  \"response\":\"image_id123\",\n" +
                        "  \"tempFile\":\"image_id123\"\n" +
                        "}");
            }

            @Override
            public JSONObject sendRawRequest(String prompt) {
                return null;
            }

            @Override
            public JSONObject sendRequest(JSONObject jsonObject) {
                return null;
            }

            @Override
            public JSONObject sendRequest(AIImageRequestDTO dto) {
                return null;
            }
        });
    }

    /**
     * Method to test: {@link OpenAIGenerateImageActionlet#executeAction(WorkflowProcessor, Map)}
     * Given Scenario: Creates a contentlet with a title, binary, but without any prompt
     * ExpectedResult: since the prompt is null, should not generate the image
     */
    @Test ()
    public void test_content_no_prompt_do_not_generated() {
        // 1) create a content type with title, body and tags
        final ContentTypeDataGen dataGen = new ContentTypeDataGen();

        //Add new fields
        final List<Field> inputFields = new ArrayList<>();
        inputFields.add(new FieldDataGen().velocityVarName("title").next());
        inputFields.add(new FieldDataGen().type(BinaryField.class).velocityVarName("image").next());
        dataGen.fields(inputFields);
        final ContentType contentType = dataGen.nextPersisted();

        // 2) create an instance with non integer
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(contentType);
        contentlet.setProperty("title", "dotCMS Party");
        contentlet.setIdentifier(UUIDGenerator.generateUuid());

        final WorkflowProcessor processor = new WorkflowProcessor(contentlet, APILocator.systemUser());
        final Map<String, WorkflowActionClassParameter> params = Map.of(
                OpenAIParams.OPEN_AI_PROMPT.key, new WorkflowActionClassParameter(""),
                OpenAIParams.OVERWRITE_FIELDS.key, new WorkflowActionClassParameter("true"),
                OpenAIParams.FIELD_TO_WRITE.key, new WorkflowActionClassParameter("image")
        );

        new OpenAIGenerateImageActionlet().executeAction(processor, params);

        Assert.assertNull("No prompt sent, no image generated",contentlet.get("image"));
    }

    /**
     * Method to test: {@link OpenAIGenerateImageActionlet#executeAction(WorkflowProcessor, Map)}
     * Given Scenario: Creates a contentlet with a title, No binary and NO prompt
     * ExpectedResult: since the does not have any prompt  do not generated any body
     */
    @Test ()
    public void test_content_without_prompt_field_do_not_generated() {
        // 1) create a content type with title, body and tags
        final ContentTypeDataGen dataGen = new ContentTypeDataGen();

        //Add new fields
        final List<Field> inputFields = new ArrayList<>();
        inputFields.add(new FieldDataGen().velocityVarName("title").next());
        inputFields.add(new FieldDataGen().type(StoryBlockField.class).velocityVarName("body").next());
        dataGen.fields(inputFields);
        final ContentType contentType = dataGen.nextPersisted();

        // 2) create an instance with non integer
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(contentType);
        contentlet.setProperty("title", "dotCMS Party");
        contentlet.setIdentifier(UUIDGenerator.generateUuid());

        final WorkflowProcessor processor = new WorkflowProcessor(contentlet, APILocator.systemUser());
        final Map<String, WorkflowActionClassParameter> params = Map.of(
                OpenAIParams.OPEN_AI_PROMPT.key, new WorkflowActionClassParameter("Create an image about dotCMS party"),
                OpenAIParams.OVERWRITE_FIELDS.key, new WorkflowActionClassParameter("true"),
                OpenAIParams.FIELD_TO_WRITE.key, new WorkflowActionClassParameter("image")
        );

        new OpenAIGenerateImageActionlet().executeAction(processor, params);
        final Object object = contentlet.get("body");
        Assert.assertNull("When not binary field as part of the contentlet, should not generate the image", object);
    }


    /**
     * Method to test: {@link OpenAIContentPromptActionlet#executeAction(WorkflowProcessor, Map)}
     * Given Scenario: Creates a contentlet with a title, body
     * ExpectedResult: The body should be generated
     */
    @Test
    public void test_body_generation() {
        // 1) create a content type with title, body and tags
        final ContentTypeDataGen dataGen = new ContentTypeDataGen();

        //Add new fields
        final List<Field> inputFields = new ArrayList<>();
        inputFields.add(new FieldDataGen().velocityVarName("title").next());
        inputFields.add(new FieldDataGen().type(BinaryField.class).velocityVarName("image").next());

        dataGen.fields(inputFields);
        final ContentType contentType = dataGen.nextPersisted();
        // 2) create an instance with some text and publish it
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(contentType);
        contentlet.setProperty("title", "Write an article about dotCMS");
        contentlet.setIdentifier(UUIDGenerator.generateUuid());
        contentlet.setHost(host.getIdentifier());

        // 3) Run the actionlet with the content
        final WorkflowProcessor processor = new WorkflowProcessor(contentlet, APILocator.systemUser());
        final Map<String, WorkflowActionClassParameter> params = Map.of(
                OpenAIParams.OPEN_AI_PROMPT.key, new WorkflowActionClassParameter("Create an image about dotCMS party"),
                OpenAIParams.OVERWRITE_FIELDS.key, new WorkflowActionClassParameter("true"),
                OpenAIParams.FIELD_TO_WRITE.key, new WorkflowActionClassParameter("image")
        );

        new OpenAIGenerateImageActionlet().executeAction(processor, params);

        final Object bodyObject = contentlet.get("image");
        Assert.assertNotNull("Body returned can not be null",bodyObject);
        Assert.assertTrue("Body returned should be a String",bodyObject instanceof File);
        final File body = (File) bodyObject;
        Assert.assertEquals("Body returned should be not empty", body.getName(), "image_id123");
    }

}
