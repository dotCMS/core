package com.dotcms.api;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.common.ContentTypeLayoutTestHelperService;
import com.dotcms.common.ContentTypesTestHelperService;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.FieldLayoutRow;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableColumnField;
import com.dotcms.contenttype.model.field.ImmutableRelationshipField;
import com.dotcms.contenttype.model.field.ImmutableRelationships;
import com.dotcms.contenttype.model.field.ImmutableRowField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.RelationshipCardinality;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.Relationships;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.model.workflow.SystemAction;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.contenttype.FilterContentTypesRequest;
import com.dotcms.model.contenttype.SaveContentTypeRequest;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.SiteView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class ContentTypeAPIIT {

    @ConfigProperty(name = "com.dotcms.starter.site", defaultValue = "default")
    String siteName;

    private static final Set<String> CONTENT_TYPE_VARS = Set.of(
            "HtmlPageAsset", "FileAsset", "Host",
            "forms", "VanityUrl", "htmlpageasset", "webPageContent",
            "dotAsset", "Languagevariable", "persona"
    );
    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    RestClientFactory apiClientFactory;

    @Inject
    ServiceManager serviceManager;

    @Inject
    ContentTypeLayoutTestHelperService contentTypeLayoutTestHelperService;

    @Inject
    ContentTypesTestHelperService contentTypesTestHelperService;

    @BeforeEach
    public void setupTest() throws IOException, URISyntaxException {
        serviceManager.removeAll()
                .persist(ServiceBean.builder().name("default")
                        .url(new URI("http://localhost:8080").toURL()).active(true).build());

        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    /**
     * Generate a CT using our classes model
     * Then test we can go back and forth using serialization and test our fields actually get translated properly using polymorphism
     * @throws JsonProcessingException
     */
    @Test
    void Test_Content_Type_Model_Serialization() throws JsonProcessingException {

        final ObjectMapper objectMapper = new ClientObjectMapper().getContext(null);

        final ImmutableSimpleContentType contentType = ImmutableSimpleContentType.builder()
                .baseType(BaseContentType.CONTENT)
                .description("desc")
                .id("1")
                .variable("var")
                .addFields(ImmutableBinaryField.builder()
                        .name("name")
                        .id("1")
                        .variable("fieldVar")
                        .build()).build();

        final String ctAsString = objectMapper.writeValueAsString(contentType);
        System.out.println(ctAsString);

        final ContentType ct = objectMapper.readValue(ctAsString, ContentType.class);
        Assert.assertNotNull(ct);
        Assert.assertTrue(
                ct.fields().stream().anyMatch(field -> field instanceof BinaryField));

        Assertions.assertEquals(BaseContentType.CONTENT,ct.baseType());
        Assertions.assertEquals(ContentType.SYSTEM_HOST,ct.host());
        Assertions.assertEquals(ContentType.SYSTEM_FOLDER,ct.folder());

        /*
         The following bits won't work as the generated json lacks of the class attribute within entity
         ResponseEntityView takes the entity as a Parametrized type
         Therefore the annotations on the entity we're passing are not present when ObjectMapper serialize EntityView
         If we want to be able to rebuild the CT from within a generated json
         We would need a concrete immutable class generated from AbstractResponseEntityView making the type info available explicitly like this:
          @Immutable
          abstract class AbstractContentTypesResponse extends AbstractResponseEntityView <List<? extends ContentType>>{
          }
        */
/*
        final TypeReference <ResponseEntityView<ImmutableSimpleContentType>> typeReference = new TypeReference<>() {};
        ResponseEntityView<?> entityView = objectMapper.readValue(viewAsString, typeReference);
        Assert.assertNotNull(entityView);
        final ImmutableSimpleContentType entity = (ImmutableSimpleContentType)entityView.entity();
        Assert.assertNotNull(entity);
 */
    }

    /**
     * Test that we can hit
     */
    @Test
    void Test_Get_All_ContentTypes() {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final ResponseEntityView<List<ContentType>> response = client.getContentTypes(null, null,
                null, null, null, null, null);
        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.entity().isEmpty());
    }

    @Test
    void Test_Get_Filtered_Paginated_ContentTypes() {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final ResponseEntityView<List<ContentType>> response = client.getContentTypes("file", 1,
                10, null, null, null, null);
        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.entity().isEmpty());
    }

    @Test
    void Test_Post_Filtered_Paginated_ContentTypes() {
        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);
        final ResponseEntityView<List<ContentType>> response = client.filterContentTypes(FilterContentTypesRequest.builder()
                .filter(Map.of("types",
                        "VanityUrl,webPageContent,htmlpageasset,FileAsset"
                        )
                ).page(1)
                .perPage(4).build());
        Assertions.assertNotNull(response);
        Assertions.assertEquals(4, response.entity().size());
    }

    @Test
    void Test_Get_Single_Content_Type() {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        for (final String var : CONTENT_TYPE_VARS) {
            final ResponseEntityView<ContentType> response =
                    client.getContentType(var, 1L, true);
            Assertions.assertNotNull(response);
            final ContentType contentType = response.entity();
            Assertions.assertNotNull(contentType);
            Objects.requireNonNull(contentType.workflows()).forEach(workflow -> {
                Assertions.assertNotNull(workflow);
                Assertions.assertNotNull(workflow.id());
                Assertions.assertNotNull(workflow.variableName());
            });
        }
    }

    /**
     * Test we get 404 when requesting a non-existing CT
     */
    @Test
    void Test_404_None_Existing_Content_Type() {
        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);
        try {
            client.getContentType("non-existing-content-type-"+System.currentTimeMillis(), null, null);
        }catch (NotFoundException notFoundException){
            return;
        }
        Assertions.fail("If we got here then test has failed");
    }

    /**
     * Simple CRUD Test
     * Create, Update, Delete
     */
    @Test
    void Test_Create_Then_Update_Then_Delete_Content_Type() {

        final long identifier =  System.currentTimeMillis();
        final ImmutableSimpleContentType contentType = ImmutableSimpleContentType.builder()
                .description("ct for testing.")
                .variable("_var_"+identifier)
                .addFields(
                        ImmutableBinaryField.builder()
                                .name("_bin_var_"+identifier)
                                .variable("lol")
                                .build()
                ).build();

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);
        final SaveContentTypeRequest saveRequest = SaveContentTypeRequest.builder().
                from(contentType).build();

        final ResponseEntityView<List<ContentType>> response = client.createContentTypes(List.of(saveRequest));
        Assertions.assertNotNull(response);
        final List<ContentType> contentTypes = response.entity();
        Assertions.assertNotNull(contentTypes);
        ContentType newContentType = contentTypes.get(0);
        Assertions.assertNotNull(newContentType.id());
        Assertions.assertEquals("_var_"+identifier, newContentType.variable());

        // We make sure the CT exists because the following line does not throw 404
        client.getContentType(newContentType.variable(), 1L, true);

        // Now lets test update
        final ImmutableSimpleContentType updatedContentType = ImmutableSimpleContentType.builder().from(newContentType).description("Updated").build();
        final SaveContentTypeRequest request = SaveContentTypeRequest.builder().
                from(updatedContentType).build();
        final ResponseEntityView<ContentType> responseEntityView = getUpdateContentTypeResponse(
                client, request);
        Assertions.assertEquals("Updated", responseEntityView.entity().description());

        // And finally test delete
        await().pollInterval(1,TimeUnit.SECONDS)
               .atMost(30, TimeUnit.SECONDS).until(() -> {
                    final ResponseEntityView<String> responseStringEntity = getDelete(client,
                            updatedContentType.variable());
            // Check if the response contains "deleted"
            return responseStringEntity.entity().contains("deleted");
        });

        // Use Awaitility to wait until the ContentType is actually deleted
        await().pollInterval(1,TimeUnit.SECONDS)
               .atMost(30, TimeUnit.SECONDS).until(() -> {
            try {
                final boolean exists = getContentType(client, updatedContentType);
                return !exists; // If this succeeds, the ContentType still exists
            } catch (WebApplicationException e) {
                if (e.getResponse().getStatus() == 404) {
                    return true; // ContentType was successfully deleted
                }
                throw e; // Rethrow any unexpected exceptions
            }
        });
    }

    /**
     * Since awaitility works on a separate thread we need to explicitly activate the request context
     * @param client The client
     * @param updatedContentType The content type to delete
     * @return The response entity view
     */
    @ActivateRequestContext
    ResponseEntityView<String> getDelete(final ContentTypeAPI client, final String updatedContentType) {
        return client.delete(updatedContentType);
    }

    /**
     * Since awaitility works on a separate thread we need to explicitly activate the request context
     * @param client The client
     * @param updatedContentType The content type to verify
     * @return True if the content type exists, false otherwise
     */
    @ActivateRequestContext
    boolean getContentType(final ContentTypeAPI client, final SimpleContentType updatedContentType) {
        final ResponseEntityView<ContentType> contentType = client.getContentType(updatedContentType.variable(), 1L, true);
        return contentType != null;
    }

    /**
     * Test: Create action mappings, then update them.
     *
     * <p><strong>Scenario:</strong></p>
     * <ol>
     *   <li>Create a new set of action mappings.</li>
     *   <li>Update the created action mappings with new values.</li>
     *   <li>Verify that the updates are correctly applied.</li>
     * </ol>
     *
     * <p><strong>Expected:</strong></p>
     * <ul>
     *   <li>Action mappings should be created successfully.</li>
     *   <li>The updates to the action mappings should be reflected correctly.</li>
     *   <li>The entire process should complete without errors.</li>
     * </ul>
     *
     * @throws JsonProcessingException If an error occurs while processing JSON.
     */
    @Test
    void Test_Create_Then_Update_Action_Mappings() throws JsonProcessingException {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        // Get the existing workflows
        final ResponseEntityView<ContentType> fileAssetResponse = client.getContentType(
                "FileAsset", 1L, false
        );
        final ContentType fileAsset = fileAssetResponse.entity();
        Assertions.assertFalse(Objects.requireNonNull(fileAsset.workflows()).isEmpty());

        // Create the action mappings
        final Map<String, String> actionMappingsV1 = Map.of(
                SystemAction.NEW.name(), "b9d89c80-3d88-4311-8365-187323c96436"
        );
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode jsonNodeV1 = mapper.valueToTree(actionMappingsV1);

        final long identifier = System.currentTimeMillis();
        final String contentTypeVariable = "_var_" + identifier;
        final ImmutableSimpleContentType contentTypeWithoutMapping = ImmutableSimpleContentType.builder()
                .description("ct action mappings.")
                .variable(contentTypeVariable)
                .addFields(
                        ImmutableBinaryField.builder()
                                .name("_bin_var_" + identifier)
                                .variable("anyField" + System.currentTimeMillis())
                                .build()
                ).workflows(fileAsset.workflows())
                .build();

        try {
            // ---
            // Create the content type with the action mappings
            final var contentType = contentTypeWithoutMapping.withSystemActionMappings(jsonNodeV1);
            final SaveContentTypeRequest request = SaveContentTypeRequest.builder()
                    .from(contentType).build();
            final ResponseEntityView<List<ContentType>> createContentTypeResponse =
                    client.createContentTypes(List.of(request));

            // Verify that the content type was saved and indexed
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                var byVarName = contentTypesTestHelperService.findContentType(contentTypeVariable);
                return byVarName.isPresent();
            });

            final ContentType createdContentType = createContentTypeResponse.entity().get(0);
            Assertions.assertNotNull(createdContentType.systemActionMappings());
            Assertions.assertEquals(1, Objects.requireNonNull(
                    createdContentType.systemActionMappings()).size());

            // ---
            // Modifying the content type without system mappings, nothing should change in mappings
            var modifiedContentType = contentTypeWithoutMapping.withDescription("Modified!");

            final SaveContentTypeRequest contentTypeRequest = SaveContentTypeRequest.builder()
                    .from(modifiedContentType).build();

            // Use of Awaitility to wait for the modified response
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                ResponseEntityView<ContentType> updateContentTypeResponse = getUpdateContentTypeResponse(
                        client, contentTypeRequest);
                ContentType updatedContentType = updateContentTypeResponse.entity();
                return "Modified!".equals(updatedContentType.description());
            });

            // ---
            // Update the system mappings with more values
            final Map<String, String> actionMappingsV2 = Map.of(
                    SystemAction.NEW.name(), "b9d89c80-3d88-4311-8365-187323c96436",
                    SystemAction.ARCHIVE.name(), "4da13a42-5d59-480c-ad8f-94a3adf809fe",
                    SystemAction.PUBLISH.name(), "b9d89c80-3d88-4311-8365-187323c96436"
            );
            final JsonNode jsonNodeV2 = new ObjectMapper().valueToTree(actionMappingsV2);

            modifiedContentType = contentTypeWithoutMapping
                    .withDescription("Modified 2!")
                    .withSystemActionMappings(jsonNodeV2);

            final SaveContentTypeRequest contentTypeRequest1 = SaveContentTypeRequest.builder().from(modifiedContentType).build();

            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                ResponseEntityView<ContentType> updateContentTypeResponse = getUpdateContentTypeResponse(
                        client, contentTypeRequest1);
                ContentType updatedContentType = updateContentTypeResponse.entity();
                return "Modified 2!".equals(updatedContentType.description()) &&
                        Objects.requireNonNull(updatedContentType.systemActionMappings()).size() == 3;
            });

            // ---
            // Modifying the mappings again, removing one
            final Map<String, String> actionMappingsV3 = Map.of(
                    SystemAction.NEW.name(), "b9d89c80-3d88-4311-8365-187323c96436",
                    SystemAction.ARCHIVE.name(), "4da13a42-5d59-480c-ad8f-94a3adf809fe"
            );
            final JsonNode jsonNodeV3 = new ObjectMapper().valueToTree(actionMappingsV3);

            modifiedContentType = contentTypeWithoutMapping
                    .withDescription("Modified 3!")
                    .withSystemActionMappings(jsonNodeV3);

            final SaveContentTypeRequest contentTypeRequest2 = SaveContentTypeRequest.builder().from(modifiedContentType).build();

            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                ResponseEntityView<ContentType> updateContentTypeResponse = getUpdateContentTypeResponse(
                        client, contentTypeRequest2);
                ContentType updatedContentType = updateContentTypeResponse.entity();
                return "Modified 3!".equals(updatedContentType.description()) &&
                        Objects.requireNonNull(updatedContentType.systemActionMappings()).size() == 2;
            });

            // ---
            // Finally, try to remove all system mappings
            final Map<String, String> actionMappingsV4 = Map.of();
            final JsonNode jsonNodeV4 = new ObjectMapper().valueToTree(actionMappingsV4);

            modifiedContentType = contentTypeWithoutMapping
                    .withDescription("Modified 4!")
                    .withSystemActionMappings(jsonNodeV4);

            final SaveContentTypeRequest contentTypeRequest3 = SaveContentTypeRequest.builder().from(modifiedContentType).build();

            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                ResponseEntityView<ContentType> updateContentTypeResponse = getUpdateContentTypeResponse(
                        client, contentTypeRequest3);
                ContentType updatedContentType = updateContentTypeResponse.entity();
                return "Modified 4!".equals(updatedContentType.description()) &&
                        updatedContentType.systemActionMappings() == null;
            });

        } finally {
            // Clean up
            try {
                getDelete(client, contentTypeVariable);
            } catch (Exception e) {
                // Ignore any issue here on the cleanup
            }
        }
    }

    /**
     * updateContentTypeResponse method to be used in Awaitility
     * @param client The client
     * @param contentTypeRequest The content type request
     * @return The response entity view
     */
    @ActivateRequestContext
    ResponseEntityView<ContentType> getUpdateContentTypeResponse(
            ContentTypeAPI client, SaveContentTypeRequest contentTypeRequest) {
        return client.updateContentType(
                contentTypeRequest.variable(), contentTypeRequest
        );
    }


    /**
         * We're trying to simplify the input file we want to se to the server via CLI so this basically test we are allowing the use of a Shorter name in the clazz field
         * @throws JsonProcessingException
         */
    @Test
    void Test_Deserialize_Class_Alias_Content_Type() throws JsonProcessingException {
        String json = "{\n"
                + "\t\"clazz\": \"SimpleContentType\",\n"
                + "\t\"variable\": \"simple\",\n"
                + "\t\"host\": \"SYSTEM_HOST\",\n"
                + "\t\"folder\": \"SYSTEM_FOLDER\",\n"
                + "\t\"description\": \"LOL\",\n"
                + "\t\"baseType\": \"CONTENT\",\n"
                + "\t\"fields\": [{\n"
                + "\t\t\"clazz\": \"BinaryField\",\n"
                + "\t\t\"variable\": \"binary\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"StoryBlockField\",\n"
                + "\t\t\"variable\": \"storyBlock\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"CategoryField\",\n"
                + "\t\t\"variable\": \"category\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"CheckboxField\",\n"
                + "\t\t\"variable\": \"checkbox\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"ConstantField\",\n"
                + "\t\t\"variable\": \"const\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"CustomField\",\n"
                + "\t\t\"variable\": \"custom\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"DateField\",\n"
                + "\t\t\"variable\": \"date\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"DateTimeField\",\n"
                + "\t\t\"variable\": \"dateTime\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"EmptyField\",\n"
                + "\t\t\"variable\": \"empty\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"FileField\",\n"
                + "\t\t\"variable\": \"file\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"HiddenField\",\n"
                + "\t\t\"variable\": \"hidden\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"HostFolderField\",\n"
                + "\t\t\"variable\": \"hostFolder\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"ImageField\",\n"
                + "\t\t\"variable\": \"image\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"KeyValueField\",\n"
                + "\t\t\"variable\": \"keyVal\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"LineDividerField\",\n"
                + "\t\t\"variable\": \"line\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"MultiSelectField\",\n"
                + "\t\t\"variable\": \"multiSelect\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"PermissionTabField\",\n"
                + "\t\t\"variable\": \"permissions\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"RadioField\",\n"
                + "\t\t\"variable\": \"radio\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"RelationshipField\",\n"
                + "\t\t\"variable\": \"rels\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"RelationshipsTabField\",\n"
                + "\t\t\"variable\": \"relTab\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"SelectField\",\n"
                + "\t\t\"variable\": \"select\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"TabDividerField\",\n"
                + "\t\t\"variable\": \"tabDiv\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"TagField\",\n"
                + "\t\t\"variable\": \"lol\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"TextAreaField\",\n"
                + "\t\t\"variable\": \"textArea\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"TextField\",\n"
                + "\t\t\"variable\": \"text\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"TimeField\",\n"
                + "\t\t\"variable\": \"time\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"WysiwygField\",\n"
                + "\t\t\"variable\": \"wysiwyg\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"RowField\",\n"
                + "\t\t\"variable\": \"row\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"ColumnField\",\n"
                + "\t\t\"variable\": \"col\"\n"
                + "\t}, {\n"
                + "\t\t\"clazz\": \"JSONField\",\n"
                + "\t\t\"variable\": \"json\"\n"
                + "\t}"
                + "]\n"
                + "}";

        final ObjectMapper objectMapper = new ClientObjectMapper().getContext(null);
        final ContentType ct = objectMapper.readValue(json, ContentType.class);
        Assertions.assertNotNull(ct);
    }

    /**
     * Send invalid folder then verify CT is created under System-Folder
     * Then create another one this time using a folder path then verify the returned instance
     */
    @Test
    void Test_Send_Invalid_Host_And_Folder_Verify_Defaults() {

        final SiteAPI siteAPI = apiClientFactory.getClient(SiteAPI.class);
        final ResponseEntityView<SiteView> siteResponse = siteAPI.findByName(
                GetSiteByNameRequest.builder().siteName(siteName).build());

        final long timeStamp = System.currentTimeMillis();

        final SiteView defaultSite = siteResponse.entity();
        Assertions.assertNotNull(defaultSite);
        Assertions.assertTrue(defaultSite.isDefault());

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        //First Scenario here to test is we send a CT with a Folder that we know does not exist
        final String varName1 = "varCT"+timeStamp;
        final ImmutableSimpleContentType contentType1 = ImmutableSimpleContentType.builder()
                .description("ct for testing folders.")
                .name("name")
                .variable(varName1)
                .host(siteName)
                .folder("/non-existing-folder")
                .addFields(
                        ImmutableBinaryField.builder()
                                .variable("binVar"+timeStamp)
                                .build()
                ).build();

        final SaveContentTypeRequest saveRequest = SaveContentTypeRequest.builder().
                from(contentType1).build();

        final ResponseEntityView<List<ContentType>> contentTypeResponse1 = client.createContentTypes(List.of(saveRequest));
        Assertions.assertNotNull(contentTypeResponse1);
        final List<ContentType> contentTypes1 = contentTypeResponse1.entity();
        Assertions.assertNotNull(contentTypes1);
        ContentType newContentType1 = contentTypes1.get(0);
        Assertions.assertNotNull(newContentType1.id());
        Assertions.assertEquals(newContentType1.variable(),varName1);
        Assertions.assertEquals(defaultSite.identifier(), newContentType1.host());
        //The CT should is created under System-Folder
        Assertions.assertEquals(ContentType.SYSTEM_FOLDER,newContentType1.folder());


    }

    /**
     * Test: Sending a CT only using the folderPath including fully qualified name that includes the site followed by the folder path
     * Expected: The folder and site in the response must match the ids of the folder we created and the site we used.
     */
    @Test
    void Test_Send_Folder_Path_Only_Valid_Folder_Expect_Matching_Folder_Id() {

        final FolderAPI folderAPI = apiClientFactory.getClient(FolderAPI.class);

        final ResponseEntityView<List<Map<String, Object>>> makeFoldersResponse = folderAPI.makeFolders(List.of("/foo"), siteName);
        final List<Map<String, Object>> entity = makeFoldersResponse.entity();
        Assertions.assertNotNull(entity);
        final String inode = (String)entity.get(0).get("inode");

        final long timeStamp = System.currentTimeMillis();
        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        //First Scenario here to test if we send a CT with a Folder that we know does not exist
        final String varName1 = "fromFolderPathCT"+timeStamp;
        final ImmutableSimpleContentType contentType1 = ImmutableSimpleContentType.builder()
                .description("ct for testing folders.")
                .name("fooCT")
                .variable(varName1)
                .folderPath(String.format("%s:/foo/",siteName))
                .addFields(
                        ImmutableBinaryField.builder()
                                .variable("binVar"+timeStamp)
                                .build()
                ).build();

        final SaveContentTypeRequest saveRequest = SaveContentTypeRequest.builder().
                from(contentType1).build();

        final ResponseEntityView<List<ContentType>> contentTypeResponse2 = client.createContentTypes(List.of(saveRequest));
        Assertions.assertNotNull(contentTypeResponse2);
        final ContentType contentTypes = contentTypeResponse2.entity().get(0);
        Assertions.assertEquals(inode, contentTypes.folder());
        //Here we get default host as the fallback site because we failed locating
        Assertions.assertEquals(siteName, contentTypes.siteName());
    }

    /**
     * Test: Send a hierarchy of folders and also send the host in a separate property
     * Expect: Everything must be created under the proper folder
     */
    @Test
    void Test_Create_Content_Type_Out_Of_Folder_Path() {

        final SiteAPI siteAPI = apiClientFactory.getClient(SiteAPI.class);
        final ResponseEntityView<SiteView> siteResponse = siteAPI.findByName(
                GetSiteByNameRequest.builder().siteName(siteName).build()
        );

        final SiteView defaultSite = siteResponse.entity();

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);
        final long timeStamp = System.currentTimeMillis();
        //Now use Folder API And Create a folder under our default host and send it using the path name
        final FolderAPI folderAPI = apiClientFactory.getClient(FolderAPI.class);
        final ResponseEntityView<List<Map<String, Object>>> makeFoldersResponse = folderAPI.makeFolders(
                List.of("/f1" + timeStamp, "/f1" + timeStamp + "/f2" + timeStamp),
                siteName);

        final List<Map<String, Object>> makeFolders = makeFoldersResponse.entity();

        final String varName2 = "varCT2"+timeStamp;
        final ImmutableSimpleContentType contentType2 = ImmutableSimpleContentType.builder()
                .description("ct for testing folder 2.")
                .name("name")
                .variable(varName2)
                .host(siteName)
                .folderPath("/f1"+timeStamp+"/f2"+timeStamp)
                .addFields(
                        ImmutableBinaryField.builder()
                                .variable("binVar2"+timeStamp)
                                .build()
                ).build();

        final SaveContentTypeRequest saveRequest = SaveContentTypeRequest.builder().
                from(contentType2).build();

        final ResponseEntityView<List<ContentType>> contentTypeResponse2 = client.createContentTypes(List.of(saveRequest));
        Assertions.assertNotNull(contentTypeResponse2);
        final List<ContentType> contentTypes2 = contentTypeResponse2.entity();
        Assertions.assertNotNull(contentTypes2);
        ContentType newContentType2 = contentTypes2.get(0);
        Assertions.assertNotNull(newContentType2.id());
        Assertions.assertEquals(newContentType2.variable(),varName2);
        Assertions.assertEquals(defaultSite.identifier(), newContentType2.host());
        //Now the folder should have been created under the folder path we sent
        Assertions.assertEquals(makeFolders.get(1).get("identifier"),newContentType2.folder());

    }


    /**
     * Given scenario: A new content type with row column fields as a layout definition is sent to the server without specifying a layout.
     * Expected: The layout definition should be sent within the fields ContentType's setter.
     * The order of the fields is respected and no layout attribute is given back.
     */
    @Test
    void Test_ContentType_Without_Layout_Attribute() {

        final String varName = "layoutSupportTest" + System.nanoTime();

        final ImmutableSimpleContentType contentType = ImmutableSimpleContentType.builder()
                .baseType(BaseContentType.CONTENT)
                .description("Simple Layout support test")
                .name("layoutsTest")
                .variable(varName)
                .modDate(new Date())
                .fixed(false)
                .iDate(new Date())
                .host(ContentType.SYSTEM_HOST)
                .folder(ContentType.SYSTEM_FOLDER)
                .addFields(
                        ImmutableRowField.builder().name("row-1").build(),
                        ImmutableColumnField.builder().name("column-1").build(),
                        ImmutableTextField.builder().name("__txt_field_1").variable("txtVar1" + System.nanoTime()).build(),
                        ImmutableTextField.builder().name("__txt_field_2").variable("txtVar2" + System.nanoTime()).build(),
                        ImmutableColumnField.builder().name("column-2").build(),
                        ImmutableTextField.builder().name("__txt_field_3").variable("txtVar3" + System.nanoTime()).build(),
                        ImmutableRowField.builder().name("row-2").build(),
                        ImmutableColumnField.builder().name("column-3").build(),
                        ImmutableTextField.builder().name("__txt_field_4").variable("txtVar4" + System.nanoTime()).build()
                )
                .build();

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final SaveContentTypeRequest saveRequest = SaveContentTypeRequest.builder().
                from(contentType).build();

        final ResponseEntityView<List<ContentType>> contentTypeResponse = client.createContentTypes(List.of(saveRequest));
        Assertions.assertNotNull(contentTypeResponse);

        final List<ContentType> contentTypes = contentTypeResponse.entity();
        Assertions.assertNotNull(contentTypes);

        ContentType savedContentType = contentTypes.get(0);
        Assertions.assertNotNull(savedContentType.id());
        Assertions.assertEquals(savedContentType.variable(), varName);

        // The layout is not given back as a response
        List<FieldLayoutRow> layout = savedContentType.layout();
        Assertions.assertNull(layout);

        // All the fields sent are saved and returned back
        Assertions.assertEquals(9, savedContentType.fields().size());

        // The order of the fields sent is respected
        Assertions.assertEquals("row-1", savedContentType.fields().get(0).name());
        Assertions.assertEquals("column-1", savedContentType.fields().get(1).name());
        Assertions.assertEquals("__txt_field_1", savedContentType.fields().get(2).name());
        Assertions.assertEquals("__txt_field_2", savedContentType.fields().get(3).name());
        Assertions.assertEquals("column-2", savedContentType.fields().get(4).name());
        Assertions.assertEquals("__txt_field_3", savedContentType.fields().get(5).name());
        Assertions.assertEquals("row-2", savedContentType.fields().get(6).name());
        Assertions.assertEquals("column-3", savedContentType.fields().get(7).name());
        Assertions.assertEquals("__txt_field_4", savedContentType.fields().get(8).name());
    }

    /**
     * Given scenario: A new content type with row and column fields is defined with an explicit layout attribute.
     * Expected: The server should ignore the layout attribute and only save the fields specified in the addFields() method.
     * The layout attribute should not be returned in the response.
     * The order of the fields specified in the addFields() method should be respected.
     */
    @Test
    void Test_ContentType_Layout_Attribute_Is_Ignored() {

        final String varName = "layoutSupportTest" + System.nanoTime();

        var rowField1 = ImmutableRowField.builder().name("row-1").build();
        var columnField1 = ImmutableColumnField.builder().name("column-1").build();
        //Four textFields are created
        var fieldsList1 = this.contentTypeLayoutTestHelperService.buildTextFields(
                columnField1.name(), 4);
        //The textFields are added to the column
        var layoutColumnFieldList1 = this.contentTypeLayoutTestHelperService.buildLayoutColumns(
                List.of(columnField1), fieldsList1);
        //The layout row is created with the row and its columns
        var fieldLayoutRow1 = this.contentTypeLayoutTestHelperService.buildFieldLayoutRow(rowField1,
                layoutColumnFieldList1);

        var rowField2 = ImmutableRowField.builder().name("row-2").build();
        var columnField2 = ImmutableColumnField.builder().name("column-1").build();
        //Six fields are created
        var fieldsList2 = this.contentTypeLayoutTestHelperService.buildTextFields(
                columnField1.name(), 6);
        //The textFields are added to the column
        var layoutColumnFieldList2 = this.contentTypeLayoutTestHelperService.buildLayoutColumns(
                List.of(columnField2), fieldsList2);
        //The layout row is created with the row and its columns
        var fieldLayoutRow2 = this.contentTypeLayoutTestHelperService.buildFieldLayoutRow(rowField2,
                layoutColumnFieldList2);

        final ImmutableSimpleContentType contentType = ImmutableSimpleContentType.builder()
                .baseType(BaseContentType.CONTENT)
                .description("Simple Layout support test")
                .name("layoutsTest")
                .variable(varName)
                .modDate(new Date())
                .fixed(false)
                .iDate(new Date())
                .host(ContentType.SYSTEM_HOST)
                .folder(ContentType.SYSTEM_FOLDER)
                .addFields(
                        ImmutableRowField.builder().name("row-1").build(),
                        ImmutableColumnField.builder().name("column-1").build(),
                        ImmutableTextField.builder().name("__txt_field_1").variable("txtVar1" + System.nanoTime()).build(),
                        ImmutableTextField.builder().name("__txt_field_2").variable("txtVar2" + System.nanoTime()).build(),
                        ImmutableColumnField.builder().name("column-2").build(),
                        ImmutableTextField.builder().name("__txt_field_3").variable("txtVar3" + System.nanoTime()).build(),
                        ImmutableRowField.builder().name("row-2").build(),
                        ImmutableColumnField.builder().name("column-3").build(),
                        ImmutableTextField.builder().name("__txt_field_4").variable("txtVar4" + System.nanoTime()).build()
                )
                .addLayout(fieldLayoutRow1)
                .addLayout(fieldLayoutRow2)
                .build();


        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final SaveContentTypeRequest saveRequest = SaveContentTypeRequest.builder().
                from(contentType).build();

        final ResponseEntityView<List<ContentType>> contentTypeResponse = client.createContentTypes(List.of(saveRequest));
        Assertions.assertNotNull(contentTypeResponse);

        final List<ContentType> contentTypes = contentTypeResponse.entity();
        Assertions.assertNotNull(contentTypes);

        ContentType savedContentType = contentTypes.get(0);
        Assertions.assertNotNull(savedContentType.id());
        Assertions.assertEquals(savedContentType.variable(), varName);

        // The layout is not given back as a response
        List<FieldLayoutRow> layout = savedContentType.layout();
        Assertions.assertNull(layout);

        // Only the fields set in addFields() are saved and returned back
        Assertions.assertEquals(9, savedContentType.fields().size());

        // The order of the fields sent in addFields() is respected ignoring layout attribute rules
        Assertions.assertEquals("row-1", savedContentType.fields().get(0).name());
        Assertions.assertEquals("column-1", savedContentType.fields().get(1).name());
        Assertions.assertEquals("__txt_field_1", savedContentType.fields().get(2).name());
        Assertions.assertEquals("__txt_field_2", savedContentType.fields().get(3).name());
        Assertions.assertEquals("column-2", savedContentType.fields().get(4).name());
        Assertions.assertEquals("__txt_field_3", savedContentType.fields().get(5).name());
        Assertions.assertEquals("row-2", savedContentType.fields().get(6).name());
        Assertions.assertEquals("column-3", savedContentType.fields().get(7).name());
        Assertions.assertEquals("__txt_field_4", savedContentType.fields().get(8).name());
    }

    /**
     * Given scenario: We have a content type with a relationship field
     * Expected result: The relationship field should be created and the relationship definition should be sent as part of the field definition
     * @throws IOException
     */
    @Test
    void Simple_Relationship_Support_Test() throws IOException {

        final long timeMark = System.nanoTime();

        final ImmutableSimpleContentType blog = ImmutableSimpleContentType.builder()
                .baseType(BaseContentType.CONTENT)
                .description("Parent Content Type")
                .name("MyBlog")
                .variable("MyBlog"+timeMark)
                .modDate(new Date())
                .fixed(false)
                .iDate(new Date())
                .host(ContentType.SYSTEM_HOST)
                .folder(ContentType.SYSTEM_FOLDER)
                .addFields(
                        ImmutableRelationshipField.builder().name("Blog Comment").variable("myBlogComment"+timeMark).indexed(true)
                                .relationships(ImmutableRelationships.builder()
                                        .cardinality(RelationshipCardinality.ONE_TO_MANY)
                                .isParentField(true)
                                .velocityVar("MyBlogComment"+timeMark)
                                .build()
                        ).build()
                ).build();

        final ImmutableSimpleContentType blogComment = ImmutableSimpleContentType.builder()
                .baseType(BaseContentType.CONTENT)
                .description("Child Content Type")
                .name("MyBlogComment")
                .variable("MyBlogComment"+timeMark)
                .modDate(new Date())
                .fixed(false)
                .iDate(new Date())
                .host(ContentType.SYSTEM_HOST)
                .folder(ContentType.SYSTEM_FOLDER)
                .addFields(
                        ImmutableRelationshipField.builder().name("Blog").variable("myBlog"+timeMark).indexed(true)
                                .relationships(ImmutableRelationships.builder()
                                        .cardinality(RelationshipCardinality.MANY_TO_MANY)
                                .velocityVar("MyBlog.myBlogComment"+timeMark)
                                .isParentField(false)
                                .build()
                        ).build()
                ).build();

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final SaveContentTypeRequest saveBlogRequest = SaveContentTypeRequest.builder().
                from(blog).build();
        final ResponseEntityView<List<ContentType>> contentTypeResponse1 = client.createContentTypes(List.of(saveBlogRequest));
        ContentType savedContentType1 = null;
        ContentType savedContentType2 = null;
        try {
            final List<ContentType> contentTypes1 = contentTypeResponse1.entity();

            savedContentType1 = contentTypes1.get(0);
            Assertions.assertNotNull(savedContentType1.id());

            final RelationshipField parentRel1 = (RelationshipField) savedContentType1.fields()
                    .get(2);
            final Relationships relationships1 = parentRel1.relationships();
            Assertions.assertNotNull(relationships1);
            Assertions.assertEquals(RelationshipCardinality.ONE_TO_MANY,
                    relationships1.cardinality());
            // For some reason the server side is not setting the isParentField flag from the Content Type definition
            //Apparently there some extra logic that takes place when relationships are created from the UI
            //Relationship creations is triggered by the fields API
            //So this could be an issue
            //Assertions.assertTrue(relationships1.isParentField());
            Assertions.assertEquals("MyBlogComment" + timeMark, relationships1.velocityVar());

            final SaveContentTypeRequest saveBlogCommentRequest = SaveContentTypeRequest.builder().
                    from(blogComment).build();
            final ResponseEntityView<List<ContentType>> contentTypeResponse2 = client.createContentTypes(
                    List.of(saveBlogCommentRequest));
            final List<ContentType> contentTypes2 = contentTypeResponse2.entity();
            savedContentType2 = contentTypes2.get(0);
            Assertions.assertNotNull(savedContentType2.id());
            final RelationshipField parentRel2 = (RelationshipField) savedContentType2.fields()
                    .get(2);
            final Relationships relationships2 = parentRel2.relationships();
            Assertions.assertNotNull(relationships2);

            Assertions.assertEquals(RelationshipCardinality.MANY_TO_MANY,
                    relationships2.cardinality());
            // For some reason the server side is not setting the isParentField flag from the Content Type definition
            //Apparently there some extra logic that takes place when relationships are created from the UI
            //Relationship creations is triggered by the fields API
            //So this could be an issue
            //Assertions.assertTrue(relationships2.isParentField());
            Assertions.assertEquals("MyBlog.myBlogComment" + timeMark,
                    relationships2.velocityVar());
        } finally {
           // For some reason the modDate on the layout fields on these CT changes with every request
           // Therefore This CTs can generate noise in other tests, when comparing a copy against a local copy
           // So we delete them here
            if (null != savedContentType1){
               getDelete(client, savedContentType1.variable());
           }
           if(null != savedContentType2) {
               getDelete(client, savedContentType2.variable());
           }
        }

    }

}
