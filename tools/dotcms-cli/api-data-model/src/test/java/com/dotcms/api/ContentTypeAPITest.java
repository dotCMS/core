package com.dotcms.api;

import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.contenttype.FilterContentTypesRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

@QuarkusTest
public class ContentTypeAPITest {

    private static final Set<String> CONTENT_TYPE_VARS = ImmutableSet.of(
            "HtmlPageAsset", "FileAsset", "dotFavoritePage", "Host",
            "forms", "VanityUrl", "htmlpageasset", "webPageContent",
            "dotAsset", "Languagevariable", "persona", "calendarEvent"
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
                .description("LOL")
                .id("1").name("name")
                .inode("123456")
                .variable("lol").modDate(new Date())
                .fixed(true)
                .iDate(new Date())
                .host("host")
                .folder("lol")
                .addFields(ImmutableBinaryField.builder().name("lol").id("1").searchable(true)
                        .unique(false).indexed(true).readOnly(false).forceIncludeInApi(false)
                        .modDate(new Date()).required(false).variable("lol").sortOrder(1)
                        .fixed(false).listed(true).dataType(DataTypes.SYSTEM).build()).build();

        final String ctAsString = objectMapper.writeValueAsString(contentType);
        //System.out.println(ctAsString);

        final ContentType ct = objectMapper.readValue(ctAsString, ContentType.class);
        Assert.assertNotNull(ct);
        Assert.assertTrue(
                ct.fields().stream().anyMatch(field -> field instanceof BinaryField));

        final ResponseEntityView<ImmutableSimpleContentType> responseEntityView = ResponseEntityView.<ImmutableSimpleContentType>builder()
                .entity(contentType).build();
        final String viewAsString = objectMapper.writeValueAsString(responseEntityView);
        System.out.println(viewAsString);

        /*
         The following bits won't work as the generated json lacks of the class attribute within entity
         ResponseEntityView takes the entity as a Parametrized type
         Therefore the annotations on the entity we're passing are not present when ObjectMapper serialize EntityView
         If we want to be able to rebuild the CT from within a generated json
         We would need a concrete immutable class geerated from AbstractResponseEntityView making the type info available explicitly like this:
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
                        "calendarEvent,VanityUrl,webPageContent,htmlpageasset,FileAsset"
                        )
                ).page(1)
                .perPage(3).build());
        Assertions.assertNotNull(response);
        Assertions.assertEquals(3, response.entity().size());
    }

    @Test
    public void Test_Get_Single_Content_Type() {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        for (final String var : CONTENT_TYPE_VARS) {
            //System.out.println("Testing with var " + var);
            final ResponseEntityView<ContentType> response =
                    client.getContentType(var, 1L, true);
            Assertions.assertNotNull(response);
        }
    }


    @Test
    public void Test_Create_Then_Update_Then_Delete_Content_Type() {

         long identifier =  System.currentTimeMillis();
        final ImmutableSimpleContentType contentType = ImmutableSimpleContentType.builder()
                .baseType(BaseContentType.CONTENT)
                .description("ct for testing.")
                .name("name")
                .variable("_var_"+identifier)
                .modDate(new Date())
                .fixed(true)
                .iDate(new Date())
                .host("SYSTEM_HOST")
                .folder("SYSTEM_FOLDER")
                .addFields(
                        ImmutableBinaryField.builder()
                                .name("_bin_var_"+identifier)
                                .fixed(false)
                                .listed(true)
                                .searchable(true)
                                .unique(false)
                                .indexed(true)
                                .readOnly(false)
                                .forceIncludeInApi(false)
                                .modDate(new Date())
                                .required(false)
                                .variable("lol")
                                .sortOrder(1)
                                .dataType(DataTypes.SYSTEM).build()
                ).build();

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);
        final ResponseEntityView<List<ContentType>> response = client.createContentTypes(ImmutableList.of(contentType));
        Assertions.assertNotNull(response);
        final List<ContentType> contentTypes = response.entity();
        Assertions.assertNotNull(contentTypes);
        ContentType newContentType = contentTypes.get(0);
        Assertions.assertNotNull(newContentType.id());
        Assertions.assertEquals(newContentType.variable(),"_var_"+identifier);
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


    public void Test_Load_Minimalistic_File() throws JsonProcessingException {
       final String in = "{\n"
                + "  \"clazz\" : \"SimpleContentType\",\n"
                + "  \"name\" : \"name\",\n"
                + "  \"variable\" : \"__var__1666727954809\",\n"
                + "  \"modDate\" : \"2022-10-25T19:59:14.818+00:00\",\n"
                + "  \"fixed\" : true,\n"
                + "  \"iDate\" : \"2022-10-25T19:59:14.818+00:00\",\n"
                + "  \"host\" : \"SYSTEM_HOST\",\n"
                + "  \"folder\" : \"SYSTEM_FOLDER\",\n"
                + "  \"sortOrder\" : 1,\n"
                + "  \"description\" : \"ct for testing.\",\n"
                + "  \"baseType\" : \"CONTENT\",\n"
                + "  \"fields\" : [ {\n"
                + "    \"clazz\" : \"BinaryField\",\n"
                + "    \"searchable\" : true,\n"
                + "    \"unique\" : false,\n"
                + "    \"indexed\" : true,\n"
                + "    \"listed\" : true,\n"
                + "    \"readOnly\" : false,\n"
                + "    \"forceIncludeInApi\" : false,\n"
                + "    \"modDate\" : \"2022-10-25T19:59:14.820+00:00\",\n"
                + "    \"name\" : \"__bin_var__1666727954809\",\n"
                + "    \"required\" : false,\n"
                + "    \"variable\" : \"lol\",\n"
                + "    \"sortOrder\" : 1,\n"
                + "    \"fixed\" : false,\n"
                + "    \"dataType\" : \"SYSTEM\"\n"
                + "  } ]\n"
                + "}";
        final ObjectMapper objectMapper = new ClientObjectMapper().getContext(null);
        final ContentType contentType = objectMapper.readValue(in, ContentType.class);
        Assertions.assertNotNull(contentType);

    }

}
