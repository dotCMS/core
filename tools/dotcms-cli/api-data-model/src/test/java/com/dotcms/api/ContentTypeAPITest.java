package com.dotcms.api;


import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.contenttype.ContentTypesResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;
import com.google.common.collect.ImmutableList;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.immutables.value.internal.$processor$.meta.$GsonMirrors.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

@QuarkusTest
public class ContentTypeAPITest {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    RestClientFactory apiClientFactory;

    @Inject
    ServiceManager serviceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").active(true).build());
    }

    @Test
    public void Test_Basic_Content_Type_Serialize() throws JsonProcessingException {

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new GuavaModule());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new VersioningModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(ResponseEntityView.class)
                .allowIfBaseType(SimpleContentType.class)
                .allowIfBaseType(FileAssetContentType.class)
                .allowIfBaseType(BinaryField.class)
                .allowIfSubType(Map.class)
                .allowIfSubType(List.class)
                .build();

        //objectMapper.activateDefaultTyping(validator);
        objectMapper.deactivateDefaultTyping();

        final ImmutableSimpleContentType contentType = ImmutableSimpleContentType.builder()
                .baseType(BaseContentType.CONTENT)
                .description("LOL")
                .id("1").name("name")
                .inode("123456")
                .variable("lol").modDate(new Date()).fixed(true).iDate(new Date()).host("host")
                .sortOrder(1).folder("lol")
                .addFields(ImmutableBinaryField.builder().name("lol").id("1").searchable(true)
                        .unique(false).indexed(true).readOnly(false).forceIncludeInApi(false)
                        .modDate(new Date()).required(false).variable("lol").sortOrder(1)
                        .fixed(false).listed(true).build()).build();

        final String ctAsString = objectMapper.writeValueAsString(contentType);
        System.out.println(ctAsString);


        ResponseEntityView<ImmutableSimpleContentType> responseEntityView = ResponseEntityView.<ImmutableSimpleContentType>builder().entity(contentType).build();
        final String viewAsString = objectMapper.writeValueAsString(responseEntityView);
        System.out.println(viewAsString);

        /*
        final TypeReference <ResponseEntityView<ImmutableSimpleContentType>> typeReference = new TypeReference<>() {};
        ResponseEntityView<?> entityView = objectMapper.readValue(viewAsString, typeReference);
        Assert.assertNotNull(entityView);
        ImmutableSimpleContentType ct = (ImmutableSimpleContentType)entityView.entity();
        */

        final ContentType ct = objectMapper.readValue(ctAsString, ContentType.class);
        Assert.assertNotNull(ct);
        Assert.assertTrue(
                ct.fields().stream().anyMatch(field -> field instanceof BinaryField));

    }


    @Test
    public void Test_Content_Types() {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final String user = "admin@dotcms.com";
        final char[] passwd= "admin".toCharArray();
        authenticationContext.login(user, passwd);

        final  ResponseEntityView<List<ContentType>> response = client.getContentTypes(null, null, null, null, null, null, null );
        Assertions.assertNotNull(response);
    }

    @Ignore
    @Test
    public void Test_Content_Type() {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final String user = "admin@dotcms.com";
        final char[] passwd= "admin".toCharArray();
        authenticationContext.login(user, passwd);

        final ResponseEntityView<ContentType> response = client.getContentType("FileAsset", 1L, true);
        Assertions.assertNotNull(response);
    }


    @Ignore
    @Test
    public void Test_Save_Content_Type() {

        final ContentTypeAPI client = apiClientFactory.getClient(ContentTypeAPI.class);

        final String user = "admin@dotcms.com";
        final char[] passwd= "admin".toCharArray();
        authenticationContext.login(user, passwd);

        final ResponseEntityView<ContentType> response = client.getContentType("FileAsset", 1L, true);
        final ContentType ct = response.entity();

        final ResponseEntityView<List<ContentType>> response2 = client.createContentTypes(ImmutableList.of(ct));
        Assertions.assertNotNull(response2);
    }

}
