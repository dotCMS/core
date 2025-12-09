package com.dotcms.ai.workflow;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.api.CompletionRequest;
import com.dotcms.ai.api.CompletionResponse;
import com.dotcms.ai.api.CompletionsAPI;
import com.dotcms.ai.api.DotAIAPIFacadeImpl;
import com.dotcms.ai.api.SummarizeRequest;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TagField;
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
 * This class contains unit tests for the {@link OpenAIAutoTagActionlet} class.
 *
 * @author jsanca
 */
public class OpenAIAutoTagActionletTest {

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
        DotAIAPIFacadeImpl.addCompletionsAPIImplementation("default", (Object... initArguments) -> new CompletionsAPI() {
            @Override
            public JSONObject summarize(CompletionsForm searcher) {
                return null;
            }

            @Override
            public JSONObject summarize(SummarizeRequest summarizeRequest) {
                return null;
            }

            @Override
            public void summarize(SummarizeRequest summarizeRequest, OutputStream out) {

            }

            @Override
            public void summarizeStream(CompletionsForm searcher, OutputStream out) {

            }

            @Override
            public JSONObject raw(JSONObject promptJSON, final String userId) {
                return null;
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
                        "          \"text\": \"Here are some popular programming languages and their primary use cases:\\n\\n1. **Python**: Data science, machine learning, web development\\n2. **JavaScript**: Web development, frontend and backend development\\n3. **Java**: Enterprise applications, Android app development\\n4. **C++**: System programming, game development\\n5. **Ruby**: Web development, particularly with Ruby on Rails\",\n" +
                        "          \"keywords\": [\"Python\", \"JavaScript\", \"Java\", \"C++\", \"Ruby\", \"data science\", \"web development\", \"machine learning\", \"enterprise applications\", \"Android app development\", \"system programming\", \"game development\", \"Ruby on Rails\"]\n" +
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
            public CompletionResponse raw(CompletionRequest completionRequest) {
                return null;
            }
        });
    }

    /**
     * Method to test: {@link OpenAIAutoTagActionlet#executeAction(WorkflowProcessor, Map)}
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
        inputFields.add(new FieldDataGen().type(TagField.class).velocityVarName("tags").next());
        dataGen.fields(inputFields);
        final ContentType contentType = dataGen.nextPersisted();

        // 2) create an instance with non integer
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(contentType);

        final WorkflowProcessor processor = new WorkflowProcessor(contentlet, APILocator.systemUser());
        final Map<String, WorkflowActionClassParameter> params = Map.of(
                OpenAIParams.OVERWRITE_FIELDS.key, new WorkflowActionClassParameter("true"),
                OpenAIParams.LIMIT_TAGS_TO_HOST.key, new WorkflowActionClassParameter("false"),
                OpenAIParams.MODEL.key, new WorkflowActionClassParameter("gpt-3.5-turbo"),
                OpenAIParams.TEMPERATURE.key, new WorkflowActionClassParameter("0.5")
        );

        new OpenAIAutoTagActionlet().executeAction(processor, params);
    }

    /**
     * Method to test: {@link OpenAIAutoTagActionlet#executeAction(WorkflowProcessor, Map)}
     * Given Scenario: Creates a contentlet with a title, body and NO tags fields
     * ExpectedResult: since the does not have any tag fields do not generated any tag
     */
    @Test ()
    public void test_content_without_tag_field_do_not_generated() {
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
        contentlet.setIdentifier(UUIDGenerator.generateUuid());

        final WorkflowProcessor processor = new WorkflowProcessor(contentlet, APILocator.systemUser());
        final Map<String, WorkflowActionClassParameter> params = Map.of(
                OpenAIParams.OVERWRITE_FIELDS.key, new WorkflowActionClassParameter("true"),
                OpenAIParams.LIMIT_TAGS_TO_HOST.key, new WorkflowActionClassParameter("false"),
                OpenAIParams.MODEL.key, new WorkflowActionClassParameter("gpt-3.5-turbo"),
                OpenAIParams.TEMPERATURE.key, new WorkflowActionClassParameter("0.5")
        );

        new OpenAIAutoTagActionlet().executeAction(processor, params);
        // just do not fail
    }


    /**
     * Method to test: {@link OpenAIAutoTagActionlet#executeAction(WorkflowProcessor, Map)}
     * Given Scenario: Creates a contentlet with a title, body and tags fields
     * ExpectedResult: The tags should be generated
     */
    @Test
    public void test_tag_generation() {
        // 1) create a content type with title, body and tags
        final ContentTypeDataGen dataGen = new ContentTypeDataGen();

        //Add new fields
        final List<Field> inputFields = new ArrayList<>();
        inputFields.add(new FieldDataGen().velocityVarName("title").next());
        inputFields.add(new FieldDataGen().type(StoryBlockField.class).velocityVarName("body").next());
        inputFields.add(new FieldDataGen().type(TagField.class).velocityVarName("tags").next());

        dataGen.fields(inputFields);
        final ContentType contentType = dataGen.nextPersisted();
        // 2) create an instance with some text and publish it
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(contentType);

        contentlet.setProperty("title", "This is a title");
        contentlet.setProperty("body", "This is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a body" +
                "This is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a body" +
                "This is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a body" +
                "This is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a bodyThis is a body");
        contentlet.setIdentifier(UUIDGenerator.generateUuid());
        contentlet.setHost(host.getIdentifier());

        // 3) Run the actionlet with the content
        final WorkflowProcessor processor = new WorkflowProcessor(contentlet, APILocator.systemUser());
        final Map<String, WorkflowActionClassParameter> params = Map.of(
                OpenAIParams.OVERWRITE_FIELDS.key, new WorkflowActionClassParameter("true"),
                OpenAIParams.LIMIT_TAGS_TO_HOST.key, new WorkflowActionClassParameter("false"),
                OpenAIParams.MODEL.key, new WorkflowActionClassParameter("gpt-3.5-turbo"),
                OpenAIParams.TEMPERATURE.key, new WorkflowActionClassParameter("0.5")
        );

        new OpenAIAutoTagActionlet().executeAction(processor, params);

        final Object tagsObject = contentlet.get("tags");
        Assert.assertNotNull("Tags returned can not be null",tagsObject);
        Assert.assertTrue("Tags returned should be a List",tagsObject instanceof List);
        final List<String> tags = (List<String>) tagsObject;
        Assert.assertFalse("Tags returned should be not empty",tags.isEmpty());
        Assert.assertTrue("Tags returned should include Python",tags.contains("Python"));
        Assert.assertTrue("Tags returned should include Python",tags.contains("Java"));


    }

}
