package com.dotcms.api;

import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.contenttype.FilterContentTypesRequest;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.SiteView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

@QuarkusTest
public class ContentTypeAPITest {

    private static final Set<String> CONTENT_TYPE_VARS = ImmutableSet.of(
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
                .persist(ServiceBean.builder().name("default").active(true).build());

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
    public void Test_Content_Type_Model_Serialization() throws JsonProcessingException {

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
    public void Test_Get_All_ContentTypes() {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final ResponseEntityView<List<ContentType>> response = client.getContentTypes(null, null,
                null, null, null, null, null);
        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.entity().isEmpty());
    }

    @Test
    public void Test_Get_Filtered_Paginated_ContentTypes() {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final ResponseEntityView<List<ContentType>> response = client.getContentTypes("file", 1,
                10, null, null, null, null);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(1,response.entity().size());
    }

    @Test
    public void Test_Post_Filtered_Paginated_ContentTypes() {
        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);
        final ResponseEntityView<List<ContentType>> response = client.filterContentTypes(FilterContentTypesRequest.builder()
                .filter(ImmutableMap.of("types",
                        "VanityUrl,webPageContent,htmlpageasset,FileAsset"
                        )
                ).page(1)
                .perPage(4).build());
        Assertions.assertNotNull(response);
        Assertions.assertEquals(4, response.entity().size());
    }

    @Test
    public void Test_Get_Single_Content_Type() {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        for (final String var : CONTENT_TYPE_VARS) {
            final ResponseEntityView<ContentType> response =
                    client.getContentType(var, 1L, true);
            Assertions.assertNotNull(response);
        }
    }

    /**
     * Test we get 404 when requesting a non-existing CT
     */
    @Test
    public void Test_404_None_Existing_Content_Type() {
        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);
        try {
            client.getContentType("non-existing-content-type-"+System.currentTimeMillis(), null, null);
        }catch (NotFoundException notFoundException){
            return;
        }
        Assertions.fail("If we got here then test has failed");
    }

    @Test
    public void Test_Create_Then_Update_Then_Delete_Content_Type() {

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
        final ResponseEntityView<List<ContentType>> response = client.createContentTypes(ImmutableList.of(contentType));
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
        final ResponseEntityView<ContentType> responseEntityView = client.updateContentTypes(updatedContentType.variable(),updatedContentType);
        Assertions.assertEquals("Updated", responseEntityView.entity().description());
        //And finally test delete
        final ResponseEntityView<String> responseStringEntity = client.delete(updatedContentType.variable());
        Assertions.assertTrue(responseStringEntity.entity().contains("deleted"));

        try {
            client.getContentType(updatedContentType.variable(), 1L, true);
            Assertions.fail("If we got this far then delete-method failed to perform its job.");
        }catch(javax.ws.rs.NotFoundException e){
            // Not relevant here
        }
    }

    /**
     * We're trying to simplify the input file we want to se to the server via CLI so this basically test we are allowing the use of a Shorter name in the clazz field
     * @throws JsonProcessingException
     */
    @Test
    public void Test_Deserialize_Class_Alias_Content_Type() throws JsonProcessingException {
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
                + "\t}]\n"
                + "}";

        final ObjectMapper objectMapper = new ClientObjectMapper().getContext(null);
        final ContentType ct = objectMapper.readValue(json, ContentType.class);
        Assertions.assertNotNull(ct);
    }

    /**
     * Send invalid folder then verify CT is created under System-Folder
     * Then create another this time using a folder path then verify the returned instance
     */
    @Test
    public void Test_Send_Invalid_Host_And_Folder_Should_Default_to_System() {

        final SiteAPI siteAPI = apiClientFactory.getClient(SiteAPI.class);
        final ResponseEntityView<SiteView> siteResponse = siteAPI.findHostByName(
                GetSiteByNameRequest.builder().siteName("default").build());

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
                .host("default")
                .folder("/non-existing-folder")
                .addFields(
                        ImmutableBinaryField.builder()
                                .variable("binVar"+timeStamp)
                                .build()
                ).build();

        final ResponseEntityView<List<ContentType>> contentTypeResponse1 = client.createContentTypes(ImmutableList.of(contentType1));
        Assertions.assertNotNull(contentTypeResponse1);
        final List<ContentType> contentTypes1 = contentTypeResponse1.entity();
        Assertions.assertNotNull(contentTypes1);
        ContentType newContentType1 = contentTypes1.get(0);
        Assertions.assertNotNull(newContentType1.id());
        Assertions.assertEquals(newContentType1.variable(),varName1);
        Assertions.assertEquals(defaultSite.identifier(), newContentType1.host());
        //The CT should is created under System-Folder
        Assertions.assertEquals(ContentType.SYSTEM_FOLDER,newContentType1.folder());

        //Now use Folder API And Create a folder under out default host and send it using the path name
        final FolderAPI folderAPI = apiClientFactory.getClient(FolderAPI.class);
        final ResponseEntityView<List<Map<String, Object>>> makeFoldersResponse = folderAPI.makeFolders(
                ImmutableList.of("/f1" + timeStamp, "/f1" + timeStamp + "/f2" + timeStamp),
                "default");

        final List<Map<String, Object>> makeFolders = makeFoldersResponse.entity();

        final String varName2 = "varCT2"+timeStamp;
        final ImmutableSimpleContentType contentType2 = ImmutableSimpleContentType.builder()
                .description("ct for testing folder 2.")
                .name("name")
                .variable(varName2)
                .host("default")
                .folder("/f1"+timeStamp+"/f2"+timeStamp)
                .addFields(
                        ImmutableBinaryField.builder()
                                .variable("binVar2"+timeStamp)
                                .build()
                ).build();

        final ResponseEntityView<List<ContentType>> contentTypeResponse2 = client.createContentTypes(ImmutableList.of(contentType2));
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

    @Test
    public void Test_Send_Send_Folder_Path_Only() {

        final long timeStamp = System.currentTimeMillis();
        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        //First Scenario here to test is we send a CT with a Folder that we know does not exist
        final String varName1 = "varCT"+timeStamp;
        final ImmutableSimpleContentType contentType1 = ImmutableSimpleContentType.builder()
                .description("ct for testing folders.")
                .name("name")
                .variable(varName1)
                .folderPath("default:/foo/")
                .addFields(
                        ImmutableBinaryField.builder()
                                .variable("binVar"+timeStamp)
                                .build()
                ).build();

        final ResponseEntityView<List<ContentType>> contentTypeResponse2 = client.createContentTypes(ImmutableList.of(contentType1));
        Assertions.assertNotNull(contentTypeResponse2);

    }

}
