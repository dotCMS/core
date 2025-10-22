package com.dotcms.ai.workflow;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.api.CompletionRequest;
import com.dotcms.ai.api.CompletionsAPI;
import com.dotcms.ai.api.DotAIAPIFacadeImpl;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.rest.forms.CompletionsForm;
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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class contains unit tests for the {@link OpenAIContentPromptActionlet} class.
 *
 * @author jsanca
 */
public class OpenAIContentPromptActionletTest {

    private static AppConfig config;
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
        config = new AppConfig(host.getHostname(), secrets);
        DotAIAPIFacadeImpl.addCompletionsAPIImplementation("default", (Object... initArguments)-> new CompletionsAPI() {
            @Override
            public JSONObject summarize(CompletionsForm searcher) {
                return null;
            }

            @Override
            public void summarizeStream(CompletionsForm searcher, OutputStream out) {

            }

            @Override
            public JSONObject raw(final JSONObject promptJSON, final String userId) {
                return new JSONObject("{\n" +
                        "  \"id\": \"chatcmpl-7bHkIY2cNQXV3yWZmZ1lM1b4AIlJ6\",\n" +
                        "  \"object\": \"chat.completion\",\n" +
                        "  \"created\": 1609459200,\n" +
                        "  \"model\": \"gpt-3.5-turbo-0301\",\n" +
                        "  \"choices\": [\n" +
                        "    {\n" +
                        "      \"index\": 0,\n" +
                        "      \"message\": {\n" +
                        "        \"role\": \"assistant\",\n" +
                        "        \"content\": {\n" +
                        "          \"pageTitle\": \"Your Title Goes Here\",\n" +
                        "          \"metaDescription\": \"Your meta description goes here. It should be brief and concise, providing a summary of your content in less than 300 characters.\"\n" +
                        "        }\n" +
                        "      },\n" +
                        "      \"finish_reason\": \"stop\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"usage\": {\n" +
                        "    \"prompt_tokens\": 12,\n" +
                        "    \"completion_tokens\": 47,\n" +
                        "    \"total_tokens\": 59\n" +
                        "  }\n" +
                        "}\n");
            }

            @Override
            public JSONObject raw(CompletionsForm promptForm) {
                return null;
            }

            @Override
            public JSONObject prompt(final String systemPrompt,
                                     final String userPrompt,
                                     final String model,
                                     final float temperature,
                                     final int maxTokens,
                                     final String userId) {
                return new JSONObject("{\n" +
                        "  \"id\": \"chatcmpl-7bHkIY2cNQXV3yWZmZ1lM1b4AIlJ6\",\n" +
                        "  \"object\": \"chat.completion\",\n" +
                        "  \"created\": 1609459200,\n" +
                        "  \"model\": \"gpt-3.5-turbo-0301\",\n" +
                        "  \"choices\": [\n" +
                        "    {\n" +
                        "      \"index\": 0,\n" +
                        "      \"message\": {\n" +
                        "        \"role\": \"assistant\",\n" +
                        "        \"content\": {\n" +
                        "          \"pageTitle\": \"Your Title Goes Here\",\n" +
                        "          \"metaDescription\": \"Your meta description goes here. It should be brief and concise, providing a summary of your content in less than 300 characters.\"\n" +
                        "        }\n" +
                        "      },\n" +
                        "      \"finish_reason\": \"stop\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"usage\": {\n" +
                        "    \"prompt_tokens\": 12,\n" +
                        "    \"completion_tokens\": 47,\n" +
                        "    \"total_tokens\": 59\n" +
                        "  }\n" +
                        "}\n");
            }

            @Override
            public void rawStream(CompletionsForm promptForm, OutputStream out) {

            }

            @Override
            public Object raw(CompletionRequest completionRequest) {
                return null;
            }
        });
    }

    /**
     * Method to test: {@link OpenAIContentPromptActionlet#executeAction(WorkflowProcessor, Map)}
     * Given Scenario: Creates a contentlet with a title, body and tags fields, but without any identifier
     * ExpectedResult: since the id is null, should throw IllegalArgumentException
     */
    @Test (expected = IllegalArgumentException.class)
    public void test_content_identifier_null_do_not_generated() {
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

        final WorkflowProcessor processor = new WorkflowProcessor(contentlet, APILocator.systemUser());
        final Map<String, WorkflowActionClassParameter> params = Map.of(
                OpenAIParams.OPEN_AI_PROMPT.key, new WorkflowActionClassParameter("We need an attractive search result in Google. Return a json object that includes the fields \"pageTitle\" for a meta title of less than 55 characters and \"metaDescription\" for the meta description of less than 300 characters using this content:\\n\\n${fieldContent}\\n\\n"),
                OpenAIParams.OVERWRITE_FIELDS.key, new WorkflowActionClassParameter("true"),
                OpenAIParams.FIELD_TO_WRITE.key, new WorkflowActionClassParameter("body"),
                OpenAIParams.MODEL.key, new WorkflowActionClassParameter("gpt-3.5-turbo"),
                OpenAIParams.TEMPERATURE.key, new WorkflowActionClassParameter("0.5")
        );

        new OpenAIContentPromptActionlet().executeAction(processor, params);
    }

    /**
     * Method to test: {@link OpenAIContentPromptActionlet#executeAction(WorkflowProcessor, Map)}
     * Given Scenario: Creates a contentlet with a title, body and NO prompt
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
        contentlet.setProperty("title", "Write an article about dotCMS");
        contentlet.setIdentifier(UUIDGenerator.generateUuid());

        final WorkflowProcessor processor = new WorkflowProcessor(contentlet, APILocator.systemUser());
        final Map<String, WorkflowActionClassParameter> params = Map.of(
                OpenAIParams.OPEN_AI_PROMPT.key, new WorkflowActionClassParameter(null),
                OpenAIParams.OVERWRITE_FIELDS.key, new WorkflowActionClassParameter("true"),
                OpenAIParams.FIELD_TO_WRITE.key, new WorkflowActionClassParameter("body"),
                OpenAIParams.MODEL.key, new WorkflowActionClassParameter("gpt-3.5-turbo"),
                OpenAIParams.TEMPERATURE.key, new WorkflowActionClassParameter("0.5")
        );

        new OpenAIContentPromptActionlet().executeAction(processor, params);
        final Object object = contentlet.get("body");
        Assert.assertNull("When not prompt, should not generate the body",object);
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
        inputFields.add(new FieldDataGen().type(StoryBlockField.class).velocityVarName("body").next());

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
                OpenAIParams.OPEN_AI_PROMPT.key, new WorkflowActionClassParameter("We need an attractive search result in Google. Return a json object that includes the fields \"pageTitle\" for a meta title of less than 55 characters and \"metaDescription\" for the meta description of less than 300 characters using this content:\\n\\n${fieldContent}\\n\\n"),
                OpenAIParams.OVERWRITE_FIELDS.key, new WorkflowActionClassParameter("true"),
                OpenAIParams.FIELD_TO_WRITE.key, new WorkflowActionClassParameter("body"),
                OpenAIParams.MODEL.key, new WorkflowActionClassParameter("gpt-3.5-turbo"),
                OpenAIParams.TEMPERATURE.key, new WorkflowActionClassParameter("0.5")
        );

        new OpenAIContentPromptActionlet().executeAction(processor, params);

        final Object bodyObject = contentlet.get("body");
        Assert.assertNotNull("Body returned can not be null",bodyObject);
        Assert.assertTrue("Body returned should be a String",bodyObject instanceof CharSequence);
        final CharSequence body = (CharSequence) bodyObject;
        Assert.assertTrue("Body returned should be not empty", body.length() > 0);
        Assert.assertTrue("Tags returned should include pageTitle", body.toString().contains("pageTitle"));
        Assert.assertTrue("Tags returned should include metaDescription", body.toString().contains("metaDescription"));


    }

}
