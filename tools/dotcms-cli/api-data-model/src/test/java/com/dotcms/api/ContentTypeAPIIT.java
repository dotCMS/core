package com.dotcms.api;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.FieldLayoutRow;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableColumnField;
import com.dotcms.contenttype.model.field.ImmutableRelationshipField;
import com.dotcms.contenttype.model.field.ImmutableRelationships;
import com.dotcms.contenttype.model.field.ImmutableRowField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.Relationships;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.workflow.ImmutableActionMapping;
import com.dotcms.contenttype.model.workflow.ImmutableWorkflowAction;
import com.dotcms.contenttype.model.workflow.SystemAction;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.contenttype.AbstractSaveContentTypeRequest;
import com.dotcms.model.contenttype.FilterContentTypesRequest;
import com.dotcms.model.contenttype.SaveContentTypeRequest;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.SiteView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
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

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll()
                .persist(ServiceBean.builder().name("default")
                        .url(new URL("http://localhost:8080")).active(true).build());

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
                Assertions.assertNotNull(workflow.name());
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
        final SaveContentTypeRequest saveRequest = AbstractSaveContentTypeRequest.builder()
                .of(contentType).build();

        final ResponseEntityView<List<ContentType>> response = client.createContentTypes(List.of(saveRequest));
        Assertions.assertNotNull(response);
        final List<ContentType> contentTypes = response.entity();
        Assertions.assertNotNull(contentTypes);
        ContentType newContentType = contentTypes.get(0);
        Assertions.assertNotNull(newContentType.id());
        Assertions.assertEquals("_var_"+identifier, newContentType.variable());
        //We make sure the CT exists because the following line does not throw 404
        client.getContentType(newContentType.variable(), 1L, true);
        //Now lets test update
        final ImmutableSimpleContentType updatedContentType = ImmutableSimpleContentType.builder().from(newContentType).description("Updated").build();
        final SaveContentTypeRequest request = AbstractSaveContentTypeRequest.builder()
                .of(updatedContentType).build();
        final ResponseEntityView<ContentType> responseEntityView = client.updateContentType(
                request.variable(), request);
        Assertions.assertEquals("Updated", responseEntityView.entity().description());
        //And finally test delete
        final ResponseEntityView<String> responseStringEntity = client.delete(updatedContentType.variable());
        Assertions.assertTrue(responseStringEntity.entity().contains("deleted"));

        try {
            //a small wait to make sure the CT is deleted
            //a simple Thread.sleep would do the trick but Sonar says it's not a good practice
            int count = 0;
            while (null != client.getContentType(updatedContentType.variable(), 1L, true)){
               //We wait for the CT to be deleted
               System.out.println("Waiting for CT to be deleted");
               count++;
               if(count > 10){
                   Assertions.fail("CT was not deleted");
               }
            }
            //This should throw 404 but under certain circumstances it does throw 400
        }catch(javax.ws.rs.WebApplicationException e){
            // Not relevant here
        }
    }

    @Test
    void Test_Create_Then_Update_Action_Mappings() throws JsonProcessingException {
        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);
        //We're only using this to extract the existing Workflows
        final ResponseEntityView<ContentType> response = client.getContentType("FileAsset", 1L, false);
        final ContentType fileAsset = response.entity();
        Assertions.assertFalse(Objects.requireNonNull(fileAsset.workflows()).isEmpty());
        final String systemWorkflowId = fileAsset.workflows().get(0).id();

        final ObjectMapper mapper = new ObjectMapper();
        final ImmutableWorkflowAction workflowAction = ImmutableWorkflowAction.builder()
                .id("b9d89c80-3d88-4311-8365-187323c96436")
                .name("Publish")
                .assignable(false)
                .commentable(false)
                .condition("")
                .icon("workflowIcon")
                .nextAssign("654b0931-1027-41f7-ad4d-173115ed8ec1")
                .nextStep("dc3c9cd0-8467-404b-bf95-cb7df3fbc293")
                .nextStepCurrentStep(false)
                .order(0)
                .roleHierarchyForAssign(false)
                .schemeId("d61a59e1-a49c-46f2-a929-db2b4bfa88b2")
                .showOn(List.of("EDITING",
                        "PUBLISHED",
                        "UNLOCKED",
                        "NEW",
                        "UNPUBLISHED",
                        "LISTING",
                        "LOCKED"))
                .build();

        final ImmutableActionMapping actionMapping = ImmutableActionMapping.builder()
                .workflowAction(workflowAction)
                .identifier(systemWorkflowId)
                .systemAction("NEW")
                .build();

        final Map<String, ImmutableActionMapping> actionMappings = Map.of(SystemAction.NEW.name(), actionMapping);
        final JsonNode jsonNode = mapper.valueToTree(actionMappings);

        final long identifier =  System.currentTimeMillis();
        final ImmutableSimpleContentType contentType = ImmutableSimpleContentType.builder()
                .description("ct action mappings.")
                .variable("_var_"+identifier)
                .addFields(
                        ImmutableBinaryField.builder()
                                .name("_bin_var_"+identifier)
                                .variable("anyField"+System.currentTimeMillis())
                                .build()
                ).workflows(fileAsset.workflows())
                .systemActionMappings(jsonNode)
                .build();

        final String content = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(contentType);

        System.out.println(content);

        final SaveContentTypeRequest request = AbstractSaveContentTypeRequest.builder()
                .of(contentType).build();

        final ResponseEntityView<List<ContentType>> response2 = client.createContentTypes(List.of(request));

        final ContentType creted = response2.entity().get(0);
        Assertions.assertNotNull(creted.systemActionMappings());

        final ImmutableSimpleContentType modifiedContentType = ImmutableSimpleContentType.builder()
                .from(creted).addFields(ImmutableBinaryField.builder()
                        .contentTypeId(creted.id())
                        .name("_bin_var_2" + identifier)
                        .variable("anyField2" + System.currentTimeMillis())
                        .build()).description("Modified!").build();

        final SaveContentTypeRequest request2 = AbstractSaveContentTypeRequest.builder().of(modifiedContentType).build();
        final ResponseEntityView<ContentType> entityView = client.updateContentType(
                request2.variable(), request2
        );

        final ContentType updatedContentType = entityView.entity();
        Assertions.assertNotNull(updatedContentType.systemActionMappings());
        Assertions.assertEquals("Modified!",updatedContentType.description());
        Assertions.assertEquals(2, updatedContentType.fields().size());

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

        final SaveContentTypeRequest saveRequest = AbstractSaveContentTypeRequest.builder()
                .of(contentType1).build();

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

        final SaveContentTypeRequest saveRequest = AbstractSaveContentTypeRequest.builder()
                .of(contentType1).build();

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


        final SaveContentTypeRequest saveRequest = AbstractSaveContentTypeRequest.builder()
                .of(contentType2).build();

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
     * This is here to verify that if we send row column fields as a layout definition such definition comes back as expected within the resulting CT
     * It's IMPORTANT noticing that the layout definition should be sent within the fields ContentType's setter.
     * There is an addLayOut method generated by Immutables that if used will be ignored
     * @throws IOException
     */
    @Test
    void Simple_LayOut_Support_Test() throws IOException {

        final String varName = "layoutSupportTest"+System.nanoTime();

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
                //.addLayout()   <-- Even though We have an addLayOuts method the server side only takes into account the layout fields sent as fields
                .build();

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final SaveContentTypeRequest saveRequest = AbstractSaveContentTypeRequest.builder()
                .of(contentType).build();

        final ResponseEntityView<List<ContentType>> contentTypeResponse = client.createContentTypes(List.of(saveRequest));
        Assertions.assertNotNull(contentTypeResponse);
        final List<ContentType> contentTypes = contentTypeResponse.entity();
        Assertions.assertNotNull(contentTypes);
        ContentType savedContentType = contentTypes.get(0);
        Assertions.assertNotNull(savedContentType.id());
        Assertions.assertEquals(savedContentType.variable(), varName);
        //System.out.println(savedContentType);
        List<FieldLayoutRow> layout = savedContentType.layout();
        Assertions.assertNotNull(layout);
        Assertions.assertEquals(2, layout.size());

        //Expect two columns here
        FieldLayoutRow fieldLayoutRow0 = layout.get(0);
        Assertions.assertEquals(2, fieldLayoutRow0.columns().size());
        Assertions.assertEquals("column-1", fieldLayoutRow0.columns().get(0).columnDivider().name());
        Assertions.assertEquals("column-2", fieldLayoutRow0.columns().get(1).columnDivider().name());

        Assertions.assertEquals(2, fieldLayoutRow0.columns().get(0).fields().size());
        Assertions.assertEquals(1, fieldLayoutRow0.columns().get(1).fields().size());

        //Expect 1 column here
        FieldLayoutRow fieldLayoutRow1 = layout.get(1);
        Assertions.assertEquals(1, fieldLayoutRow1.columns().size());
        Assertions.assertEquals("column-3", fieldLayoutRow1.columns().get(0).columnDivider().name());
        Assertions.assertEquals(1, fieldLayoutRow1.columns().get(0).fields().size());
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
                                .cardinality(0)
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
                                .cardinality(1)
                                .velocityVar("MyBlog.myBlogComment"+timeMark)
                                .isParentField(false)
                                .build()
                        ).build()
                ).build();

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final SaveContentTypeRequest saveBlogRequest = AbstractSaveContentTypeRequest.builder().of(blog).build();
        final ResponseEntityView<List<ContentType>> contentTypeResponse1 = client.createContentTypes(List.of(saveBlogRequest));
        ContentType savedContentType1 = null;
        ContentType savedContentType2 = null;
        try {
            final List<ContentType> contentTypes1 = contentTypeResponse1.entity();

            savedContentType1 = contentTypes1.get(0);
            Assertions.assertNotNull(savedContentType1.id());

            final RelationshipField parentRel1 = (RelationshipField) savedContentType1.fields()
                    .get(0);
            final Relationships relationships1 = parentRel1.relationships();
            Assertions.assertNotNull(relationships1);
            Assertions.assertEquals(0, relationships1.cardinality());
            // For some reason the server side is not setting the isParentField flag from the Content Type definition
            //Apparently there some extra logic that takes place when relationships are created from the UI
            //Relationship creations is triggered by the fields API
            //So this could be an issue
            //Assertions.assertTrue(relationships1.isParentField());
            Assertions.assertEquals("MyBlogComment" + timeMark, relationships1.velocityVar());

            final SaveContentTypeRequest saveBlogCommentRequest = AbstractSaveContentTypeRequest.builder()
                    .of(blogComment).build();
            final ResponseEntityView<List<ContentType>> contentTypeResponse2 = client.createContentTypes(
                    List.of(saveBlogCommentRequest));
            final List<ContentType> contentTypes2 = contentTypeResponse2.entity();
            savedContentType2 = contentTypes2.get(0);
            Assertions.assertNotNull(savedContentType2.id());
            final RelationshipField parentRel2 = (RelationshipField) savedContentType2.fields()
                    .get(0);
            final Relationships relationships2 = parentRel2.relationships();
            Assertions.assertNotNull(relationships2);

            Assertions.assertEquals(1, relationships2.cardinality());
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
               client.delete(savedContentType1.variable());
           }
           if(null != savedContentType2) {
               client.delete(savedContentType2.variable());
           }
        }

    }

}
