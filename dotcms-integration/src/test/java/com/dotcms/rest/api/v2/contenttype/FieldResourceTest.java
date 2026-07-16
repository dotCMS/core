package com.dotcms.rest.api.v2.contenttype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.HiddenField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.ImmutableRelationshipField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.PermissionTabField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Base64;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FieldResourceTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String typeName="fieldResourceTest" + System.currentTimeMillis();

    @BeforeClass
    public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).variable(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);
        Field field = FieldBuilder.builder(TextField.class).name("text").contentTypeId(type.id()).build();
        APILocator.getContentTypeFieldAPI().save(field,APILocator.systemUser());
    }

    @AfterClass
    public static void cleanUpData() throws DotDataException, DotSecurityException {
        ContentType contentType = getContentType();
        APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentType);
    }

    @Test
    public void testCreateRelationshipFieldWithoutContentTypeId_Return400() throws Exception {

        final WebResource webResourceThatReturnsAdminUser = mock(WebResource.class);
        final InitDataObject dataObject1 = mock(InitDataObject.class);
        when(dataObject1.getUser()).thenReturn(APILocator.systemUser());
        when(webResourceThatReturnsAdminUser
                .init(nullable(String.class), any(HttpServletRequest.class),any(HttpServletResponse.class), anyBoolean(),
                        nullable(String.class))).thenReturn(dataObject1);

        final FieldResource resource = new FieldResource(webResourceThatReturnsAdminUser, APILocator.getContentTypeFieldAPI());

        final ContentType contentType = getContentType();

        final String jsonField = "{"+
                // IDENTITY VALUES
                "	\"clazz\" : \"" + ImmutableRelationshipField.class.getCanonicalName() +"\","+
                "	\"name\" : \"YouTube Videos\","+
                "   \"values\" :  1," +
                "   \"variable\": \"youtubeVideos\","+
                "   \"relationType\": \"Youtube\""+
                "	}";

        final Response response = resource.createContentTypeField(null,jsonField,getHttpRequest(),  new EmptyHttpResponse());

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertTrue("Error: " + response.getEntity().toString(), response.getEntity().toString().contains("ContentTypeId needs to be set to save the Field"));
    }

    @Test
    public void testCreateRelationshipFieldWithoutRelationType_Return400() throws Exception {
        final WebResource webResourceThatReturnsAdminUser = mock(WebResource.class);
        final InitDataObject dataObject1 = mock(InitDataObject.class);
        when(dataObject1.getUser()).thenReturn(APILocator.systemUser());
        when(webResourceThatReturnsAdminUser
                .init(nullable(String.class), any(HttpServletRequest.class),any(HttpServletResponse.class), anyBoolean(),
                        nullable(String.class))).thenReturn(dataObject1);

        final FieldResource resource = new FieldResource(webResourceThatReturnsAdminUser, APILocator.getContentTypeFieldAPI());

        final ContentType contentType = getContentType();

        final String jsonField = "{"+
                // IDENTITY VALUES
                "	\"clazz\" : \"" + ImmutableRelationshipField.class.getCanonicalName() +"\","+
                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                "	\"name\" : \"YouTube Videos\","+
                "   \"values\" :  1," +
                "   \"variable\": \"youtubeVideos\","+
                "	}";

        final Response response = resource.createContentTypeField(
                contentType.id(), jsonField.replace("CONTENT_TYPE_ID", contentType.id()), getHttpRequest(),  new EmptyHttpResponse());

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertTrue("Error: " + response.getEntity().toString(), response.getEntity().toString().contains("Relation Type needs to be set"));
    }

    @Test
    public void testCreateRelationshipFieldWithDash_Return400() throws Exception {
        final WebResource webResourceThatReturnsAdminUser = mock(WebResource.class);
        final InitDataObject dataObject1 = mock(InitDataObject.class);
        when(dataObject1.getUser()).thenReturn(APILocator.systemUser());
        when(webResourceThatReturnsAdminUser
                .init(nullable(String.class), any(HttpServletRequest.class),any(HttpServletResponse.class), anyBoolean(),
                        nullable(String.class))).thenReturn(dataObject1);

        final FieldResource resource = new FieldResource(webResourceThatReturnsAdminUser, APILocator.getContentTypeFieldAPI());

        final ContentType contentType = getContentType();

        final String jsonField = "{"+
                // IDENTITY VALUES
                "	\"clazz\" : \"" + ImmutableRelationshipField.class.getCanonicalName() +"\","+
                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                "	\"name\" : \"YouTube Videos\","+
                "   \"values\" :  1," +
                "   \"variable\": \"youtube-videos\","+
                "   \"relationType\": \"Youtube\""+
                "	}";

        final Response response = resource.createContentTypeField(
                contentType.id(), jsonField.replace("CONTENT_TYPE_ID", contentType.id()), getHttpRequest(),  new EmptyHttpResponse());

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertTrue("Error: " + response.getEntity().toString(), response.getEntity().toString().contains("contains characters not allowed"));
    }

    /**
     * Method to test: Testing the
     * {@link com.dotcms.contenttype.business.FieldAPI#save(Field, User)} method
     * <p>
     * Given Scenario: Creating a new field of type {@link ImmutableRelationshipField} and saving it
     * to later update it modifying the variable name
     * <p>
     * ExpectedResult: The process should be successful and the variable name should not be updated
     */
    @Test
    public void testUpdateFieldVariable() throws Exception {
        final WebResource webResourceThatReturnsAdminUser = mock(WebResource.class);
        final InitDataObject dataObject1 = mock(InitDataObject.class);
        when(dataObject1.getUser()).thenReturn(APILocator.systemUser());
        when(webResourceThatReturnsAdminUser
                .init(nullable(String.class), any(HttpServletRequest.class),any(HttpServletResponse.class), anyBoolean(),
                        nullable(String.class))).thenReturn(dataObject1);

        final FieldResource resource = new FieldResource(webResourceThatReturnsAdminUser, APILocator.getContentTypeFieldAPI());

        final ContentType contentType = getContentType();

        final String jsonField = "{"+
                // IDENTITY VALUES
                "	\"clazz\" : \"" + ImmutableRelationshipField.class.getCanonicalName() +"\","+
                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                "	\"name\" : \"YouTube Videos\","+
                "   \"values\" :  1," +
                "   \"variable\": \"youtubeVideos\","+
                "   \"relationType\": \"" + contentType.variable() + "\""+
                "	}";

        Response response = resource.createContentTypeField(
                contentType.id(), jsonField.replace("CONTENT_TYPE_ID", contentType.id()), getHttpRequest(),  new EmptyHttpResponse());

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        Map<String, Object> fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity();

        final String jsonFieldUpdate = "{"+
                // IDENTITY VALUES
                "	\"clazz\" : \"" + ImmutableRelationshipField.class.getCanonicalName() +"\","+
                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                "	\"name\" : \"YouTube Videos\","+
                "   \"values\" :  1," +
                "   \"variable\": \"youtube-Videos\","+
                "   \"relationType\": \"" + contentType.variable() + "\""+
                "	}";

        response = resource.updateContentTypeFieldById(
                (String) fieldMap.get("id"),
                jsonFieldUpdate.replace("CONTENT_TYPE_ID", contentType.id()).replace("CONTENT_TYPE_FIELD_ID", (String) fieldMap.get("id")),
                getHttpRequest(),  new EmptyHttpResponse());

        // The process should not fail
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        // Make sure we don't the variable didn't change
        fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity();
        assertEquals("youtubeVideos", fieldMap.get("variable"));
    }


    @Test
    public void testFieldsList() throws Exception {
        final WebResource webResourceThatReturnsAdminUser = mock(WebResource.class);
        final InitDataObject dataObject1 = mock(InitDataObject.class);
        when(dataObject1.getUser()).thenReturn(APILocator.systemUser());
        when(webResourceThatReturnsAdminUser
                .init(nullable(String.class), any(HttpServletRequest.class),any(HttpServletResponse.class), anyBoolean(),
                        nullable(String.class))).thenReturn(dataObject1);

        final FieldResource resource = new FieldResource(webResourceThatReturnsAdminUser, APILocator.getContentTypeFieldAPI());

        ContentType contentType = getContentType();
       //Test using ContentType Id
        Response response = resource.getContentTypeFields(contentType.id(), getHttpRequest(),  new EmptyHttpResponse());

        assertResponse_OK(response);

        List fields = (List) ((ResponseEntityView) response.getEntity()).getEntity();

        assertFalse(fields.isEmpty());

        for(Object fieldMap : fields){
            Field field = convertMapToField((Map<String, Object>) fieldMap);

            assertNotNull(field);

            assertTrue(field.getClass().getSimpleName().startsWith("Immutable"));
        }
        //Now test using variable name
        response = resource.getContentTypeFields(contentType.variable(), getHttpRequest(),  new EmptyHttpResponse());
        assertResponse_OK(response);

        fields = (List) ((ResponseEntityView) response.getEntity()).getEntity();

        assertFalse(fields.isEmpty());

        for(Object fieldMap : fields){
            Field field = convertMapToField((Map<String, Object>) fieldMap);

            assertNotNull(field);

            assertTrue(field.getClass().getSimpleName().startsWith("Immutable"));
        }

    }

    @Test
    public void testFieldBinary() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableBinaryField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof BinaryField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableBinaryField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof BinaryField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldCategory() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableCategoryField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"values\" : \"3297fcca-d88a-45a7-aef4-7960bc6964aa\","+
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+
                                "	\"indexed\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof CategoryField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("3297fcca-d88a-45a7-aef4-7960bc6964aa", field.values());
                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());
                        assertTrue(field.searchable());
                        assertTrue(field.indexed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableCategoryField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"values\" : \"3d5d641b-e5fd-409e-a283-b6fe7ab780d1\","+
                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\","+
                                "	\"indexed\" : \"true\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof CategoryField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("3d5d641b-e5fd-409e-a283-b6fe7ab780d1", field.values());
                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());
                        assertFalse(field.searchable());
                        assertTrue(field.indexed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldCheckbox() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableCheckboxField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"TEXT\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"values\" : \"Canada|CA\\r\\nMexico|MX\\r\\nUSA|US\","+
                                "	\"defaultValue\" : \"THE DEFAULT VALUE\","+
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+
                                "	\"indexed\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof CheckboxField);
                        assertEquals(DataTypes.TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("Canada|CA\r\nMexico|MX\r\nUSA|US", field.values());
                        assertEquals("THE DEFAULT VALUE", field.defaultValue());
                        assertEquals("THE HINT", field.hint());
                        assertEquals(3, field.selectableValues().size());

                        assertTrue(field.required());
                        assertTrue(field.searchable());
                        assertTrue(field.indexed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableCheckboxField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"TEXT\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"values\" : \"CostaRica|CR\\r\\nVenezuela|VN\","+
                                "	\"defaultValue\" : \"THE DEFAULT VALUE 2\","+
                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\","+
                                "	\"indexed\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof CheckboxField);
                        assertEquals(DataTypes.TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("CostaRica|CR\r\nVenezuela|VN", field.values());
                        assertEquals("THE DEFAULT VALUE 2", field.defaultValue());
                        assertEquals("THE HINT 2", field.hint());
                        assertEquals(2, field.selectableValues().size());

                        assertFalse(field.required());
                        assertFalse(field.searchable());
                        assertFalse(field.indexed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldConstant() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableConstantField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"values\" : \"THE VALUE\","+
                                "	\"hint\" : \"THE HINT\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof ConstantField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE VALUE", field.values());
                        assertEquals("THE HINT", field.hint());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableConstantField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"values\" : \"THE VALUE 2\","+
                                "	\"hint\" : \"THE HINT 2\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof ConstantField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE VALUE 2", field.values());
                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldCustom() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableCustomField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"LONG_TEXT\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"values\" : \"THE VALUE\","+
                                "	\"defaultValue\" : \"THE DEFAULT VALUE\","+
                                "	\"regexCheck\" : \"THE VALIDATION REGEX\","+
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+
                                "	\"indexed\" : \"true\","+
                                "	\"listed\" : \"true\","+
                                "	\"unique\" : \"false\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof CustomField);
                        assertEquals(DataTypes.LONG_TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE VALUE", field.values());
                        assertEquals("THE DEFAULT VALUE", field.defaultValue());
                        assertEquals("THE VALIDATION REGEX", field.regexCheck());
                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());
                        assertTrue(field.searchable());
                        assertTrue(field.indexed());
                        assertTrue(field.listed());
                        assertFalse(field.unique());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableCustomField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"LONG_TEXT\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"values\" : \"THE VALUE 2\","+
                                "	\"defaultValue\" : \"THE DEFAULT VALUE 2\","+
                                "	\"regexCheck\" : \"THE VALIDATION REGEX 2\","+
                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\","+
                                "	\"indexed\" : \"false\","+
                                "	\"listed\" : \"false\","+
                                "	\"unique\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof CustomField);
                        assertEquals(DataTypes.LONG_TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE VALUE 2", field.values());
                        assertEquals("THE DEFAULT VALUE 2", field.defaultValue());
                        assertEquals("THE VALIDATION REGEX 2", field.regexCheck());
                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());
                        assertFalse(field.searchable());
                        assertFalse(field.indexed());
                        assertFalse(field.listed());
                        assertFalse(field.unique());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldDate() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableDateField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"DATE\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"defaultValue\" : \"1995-12-05\","+
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+
                                "	\"indexed\" : \"true\","+
                                "	\"listed\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof DateField);
                        assertEquals(DataTypes.DATE, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("1995-12-05", field.defaultValue());
                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());
                        assertTrue(field.searchable());
                        assertTrue(field.indexed());
                        assertTrue(field.listed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableDateField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"DATE\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"defaultValue\" : \"1980-03-31\","+
                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\","+
                                "	\"indexed\" : \"false\","+
                                "	\"listed\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof DateField);
                        assertEquals(DataTypes.DATE, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("1980-03-31", field.defaultValue());
                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());
                        assertFalse(field.searchable());
                        assertFalse(field.indexed());
                        assertFalse(field.listed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldDateTime() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableDateTimeField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"DATE\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"defaultValue\" : \"2005-12-01 15:22:00\","+
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+
                                "	\"indexed\" : \"true\","+
                                "	\"listed\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof DateTimeField);
                        assertEquals(DataTypes.DATE, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("2005-12-01 15:22:00", field.defaultValue());
                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());
                        assertTrue(field.searchable());
                        assertTrue(field.indexed());
                        assertTrue(field.listed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableDateTimeField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"DATE\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"defaultValue\" : \"1980-03-31 09:15:00\","+
                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\","+
                                "	\"indexed\" : \"false\","+
                                "	\"listed\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof DateTimeField);
                        assertEquals(DataTypes.DATE, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("1980-03-31 09:15:00", field.defaultValue());
                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());
                        assertFalse(field.searchable());
                        assertFalse(field.indexed());
                        assertFalse(field.listed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldFile() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableFileField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"TEXT\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof FileField);
                        assertEquals(DataTypes.TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableFileField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"TEXT\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof FileField);
                        assertEquals(DataTypes.TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldHidden() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableHiddenField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"values\" : \"THE VALUE\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof HiddenField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE VALUE", field.values());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableHiddenField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"values\" : \"THE VALUE 2\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof HiddenField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE VALUE 2", field.values());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldHostFolder() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableHostFolderField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Host Field 1\","+

                                // MANDATORY VALUES
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof HostFolderField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Host Field 1", field.name());
                        assertEquals("theHostField1", field.variable());

                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());
                        assertTrue(field.searchable());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableHostFolderField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Host Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theHostField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof HostFolderField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Host Field 2", field.name());
                        assertEquals("theHostField1", field.variable());

                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());
                        assertFalse(field.searchable());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldImage() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableImageField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"TEXT\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof ImageField);
                        assertEquals(DataTypes.TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableImageField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"TEXT\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof ImageField);
                        assertEquals(DataTypes.TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldKeyValue() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableKeyValueField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"LONG_TEXT\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof KeyValueField);
                        assertEquals(DataTypes.LONG_TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());
                        assertTrue(field.searchable());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableKeyValueField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"LONG_TEXT\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof KeyValueField);
                        assertEquals(DataTypes.LONG_TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());
                        assertFalse(field.searchable());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldLineDivider() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableLineDividerField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The LineDivider Field 1\","+

                                // MANDATORY VALUES

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof LineDividerField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The LineDivider Field 1", field.name());
                        assertEquals("theLinedividerField1", field.variable());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableLineDividerField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theLinedividerField1\","+
                                "	\"sortOrder\":\"12\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof LineDividerField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theLinedividerField1", field.variable());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldMultiSelect() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableMultiSelectField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"LONG_TEXT\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"values\" : \"Canada|CA\\r\\nMexico|MX\\r\\nUSA|US\","+
                                "	\"defaultValue\" : \"THE DEFAULT VALUE\","+
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+
                                "	\"indexed\" : \"true\","+
                                "	\"unique\" : \"false\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof MultiSelectField);
                        assertEquals(DataTypes.LONG_TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("Canada|CA\r\nMexico|MX\r\nUSA|US", field.values());
                        assertEquals("THE DEFAULT VALUE", field.defaultValue());
                        assertEquals("THE HINT", field.hint());
                        assertEquals(3, field.selectableValues().size());

                        assertTrue(field.required());
                        assertTrue(field.searchable());
                        assertTrue(field.indexed());
                        assertFalse(field.unique());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableMultiSelectField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"LONG_TEXT\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"values\" : \"CostaRica|CR\\r\\nVenezuela|VN\","+
                                "	\"defaultValue\" : \"THE DEFAULT VALUE 2\","+
                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\","+
                                "	\"indexed\" : \"false\","+
                                "	\"unique\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof MultiSelectField);
                        assertEquals(DataTypes.LONG_TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("CostaRica|CR\r\nVenezuela|VN", field.values());
                        assertEquals("THE DEFAULT VALUE 2", field.defaultValue());
                        assertEquals("THE HINT 2", field.hint());
                        assertEquals(2, field.selectableValues().size());

                        assertFalse(field.required());
                        assertFalse(field.searchable());
                        assertFalse(field.indexed());
                        assertFalse(field.unique());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldPermissionTab() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutablePermissionTabField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof PermissionTabField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutablePermissionTabField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof PermissionTabField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldRadio() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableRadioField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"TEXT\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"values\" : \"Canada|CA\\r\\nMexico|MX\\r\\nUSA|US\","+
                                "	\"defaultValue\" : \"THE DEFAULT VALUE\","+
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+
                                "	\"indexed\" : \"true\","+
                                "	\"listed\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof RadioField);
                        assertEquals(DataTypes.TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("Canada|CA\r\nMexico|MX\r\nUSA|US", field.values());
                        assertEquals("THE DEFAULT VALUE", field.defaultValue());
                        assertEquals("THE HINT", field.hint());
                        assertEquals(3, field.selectableValues().size());

                        assertTrue(field.required());
                        assertTrue(field.searchable());
                        assertTrue(field.indexed());
                        assertTrue(field.listed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableRadioField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"TEXT\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"values\" : \"CostaRica|CR\\r\\nVenezuela|VN\","+
                                "	\"defaultValue\" : \"THE DEFAULT VALUE 2\","+
                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\","+
                                "	\"indexed\" : \"false\","+
                                "	\"listed\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof RadioField);
                        assertEquals(DataTypes.TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("CostaRica|CR\r\nVenezuela|VN", field.values());
                        assertEquals("THE DEFAULT VALUE 2", field.defaultValue());
                        assertEquals("THE HINT 2", field.hint());
                        assertEquals(2, field.selectableValues().size());

                        assertFalse(field.required());
                        assertFalse(field.searchable());
                        assertFalse(field.indexed());
                        assertFalse(field.listed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldRelationshipsTab() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableRelationshipsTabField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Relationship Field 1\","+

                                // MANDATORY VALUES

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof RelationshipsTabField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Relationship Field 1", field.name());
                        assertEquals("theRelationshipField1", field.variable());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableRelationshipsTabField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theRelationshipField1\","+
                                "	\"sortOrder\":\"12\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof RelationshipsTabField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theRelationshipField1", field.variable());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldSelect() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableSelectField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"TEXT\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"values\" : \"Canada|CA\\r\\nMexico|MX\\r\\nUSA|US\","+
                                "	\"defaultValue\" : \"THE DEFAULT VALUE\","+
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+
                                "	\"indexed\" : \"true\","+
                                "	\"listed\" : \"true\","+
                                "	\"unique\" : \"false\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof SelectField);
                        assertEquals(DataTypes.TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("Canada|CA\r\nMexico|MX\r\nUSA|US", field.values());
                        assertEquals("THE DEFAULT VALUE", field.defaultValue());
                        assertEquals("THE HINT", field.hint());
                        assertEquals(3, field.selectableValues().size());

                        assertTrue(field.required());
                        assertTrue(field.searchable());
                        assertTrue(field.indexed());
                        assertTrue(field.listed());
                        assertFalse(field.unique());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableSelectField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"TEXT\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"values\" : \"CostaRica|CR\\r\\nVenezuela|VN\","+
                                "	\"defaultValue\" : \"THE DEFAULT VALUE 2\","+
                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\","+
                                "	\"indexed\" : \"false\","+
                                "	\"listed\" : \"false\","+
                                "	\"unique\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof SelectField);
                        assertEquals(DataTypes.TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("CostaRica|CR\r\nVenezuela|VN", field.values());
                        assertEquals("THE DEFAULT VALUE 2", field.defaultValue());
                        assertEquals("THE HINT 2", field.hint());
                        assertEquals(2, field.selectableValues().size());

                        assertFalse(field.required());
                        assertFalse(field.searchable());
                        assertFalse(field.indexed());
                        assertFalse(field.listed());
                        assertFalse(field.unique());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldTabDividerField() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableTabDividerField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof TabDividerField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableTabDividerField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof TabDividerField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldTag() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableTagField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"defaultValue\" : \"THE DEFAULT VALUE\","+
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+
                                "	\"indexed\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof TagField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE DEFAULT VALUE", field.defaultValue());
                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());
                        assertTrue(field.searchable());
                        assertTrue(field.indexed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableTagField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"SYSTEM\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"defaultValue\" : \"THE DEFAULT VALUE 2\","+
                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\","+
                                "	\"indexed\" : \"true\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof TagField);
                        assertEquals(DataTypes.SYSTEM, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE DEFAULT VALUE 2", field.defaultValue());
                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());
                        assertFalse(field.searchable());
                        assertTrue(field.indexed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldText() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableTextField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"TEXT\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"defaultValue\" : \"THE DEFAULT VALUE\","+
                                "	\"regexCheck\" : \"THE VALIDATION REGEX\","+
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+
                                "	\"indexed\" : \"true\","+
                                "	\"listed\" : \"true\","+
                                "	\"unique\" : \"false\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof TextField);
                        assertEquals(DataTypes.TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE DEFAULT VALUE", field.defaultValue());
                        assertEquals("THE VALIDATION REGEX", field.regexCheck());
                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());
                        assertTrue(field.searchable());
                        assertTrue(field.indexed());
                        assertTrue(field.listed());
                        assertFalse(field.unique());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableTextField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"TEXT\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"defaultValue\" : \"THE DEFAULT VALUE 2\","+
                                "	\"regexCheck\" : \"THE VALIDATION REGEX 2\","+
                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\","+
                                "	\"indexed\" : \"false\","+
                                "	\"listed\" : \"false\","+
                                "	\"unique\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof TextField);
                        assertEquals(DataTypes.TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE DEFAULT VALUE 2", field.defaultValue());
                        assertEquals("THE VALIDATION REGEX 2", field.regexCheck());
                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());
                        assertFalse(field.searchable());
                        assertFalse(field.indexed());
                        assertFalse(field.listed());
                        assertFalse(field.unique());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldTextArea() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableTextAreaField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"LONG_TEXT\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"defaultValue\" : \"THE DEFAULT VALUE\","+
                                "	\"regexCheck\" : \"THE VALIDATION REGEX\","+
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+
                                "	\"indexed\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof TextAreaField);
                        assertEquals(DataTypes.LONG_TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE DEFAULT VALUE", field.defaultValue());
                        assertEquals("THE VALIDATION REGEX", field.regexCheck());
                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());
                        assertTrue(field.searchable());
                        assertTrue(field.indexed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableTextAreaField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"LONG_TEXT\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"defaultValue\" : \"THE DEFAULT VALUE 2\","+
                                "	\"regexCheck\" : \"THE VALIDATION REGEX 2\","+
                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\","+
                                "	\"indexed\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof TextAreaField);
                        assertEquals(DataTypes.LONG_TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE DEFAULT VALUE 2", field.defaultValue());
                        assertEquals("THE VALIDATION REGEX 2", field.regexCheck());
                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());
                        assertFalse(field.searchable());
                        assertFalse(field.indexed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldTime() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableTimeField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"DATE\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"defaultValue\" : \"20:11:02\","+
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+
                                "	\"indexed\" : \"true\","+
                                "	\"listed\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof TimeField);
                        assertEquals(DataTypes.DATE, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("20:11:02", field.defaultValue());
                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());
                        assertTrue(field.searchable());
                        assertTrue(field.indexed());
                        assertTrue(field.listed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableTimeField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"DATE\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"defaultValue\" : \"09:15:00\","+
                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\","+
                                "	\"indexed\" : \"false\","+
                                "	\"listed\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof TimeField);
                        assertEquals(DataTypes.DATE, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("09:15:00", field.defaultValue());
                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());
                        assertFalse(field.searchable());
                        assertFalse(field.indexed());
                        assertFalse(field.listed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    @Test
    public void testFieldWysiwyg() throws Exception {
        testField(
                new AbstractFieldTester(){
                    @Override
                    protected String getJsonFieldCreate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableWysiwygField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"dataType\" : \"LONG_TEXT\","+
                                "	\"name\" : \"The Field 1\","+

                                // MANDATORY VALUES
                                "	\"defaultValue\" : \"THE DEFAULT VALUE\","+
                                "	\"regexCheck\" : \"THE VALIDATION REGEX\","+
                                "	\"hint\" : \"THE HINT\","+

                                "	\"required\" : \"true\","+
                                "	\"searchable\" : \"true\","+
                                "	\"indexed\" : \"true\","+

                                // OPTIONAL VALUES
                                "	\"readOnly\" : \"false\","+
                                "	\"fixed\" : \"false\","+
                                "	\"sortOrder\" : 11"+
                                "	}";
                    }

                    @Override
                    protected void assertFieldCreate(Field field) {
                        assertTrue(field instanceof WysiwygField);
                        assertEquals(DataTypes.LONG_TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 1", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE DEFAULT VALUE", field.defaultValue());
                        assertEquals("THE VALIDATION REGEX", field.regexCheck());
                        assertEquals("THE HINT", field.hint());

                        assertTrue(field.required());
                        assertTrue(field.searchable());
                        assertTrue(field.indexed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(11, field.sortOrder());
                    }

                    @Override
                    protected String getJsonFieldUpdate() {
                        return "{"+
                                // IDENTITY VALUES
                                "	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableWysiwygField\","+
                                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                                "	\"dataType\" : \"LONG_TEXT\","+
                                "	\"name\" : \"The Field 2\","+

                                // MANDATORY VALUES
                                "	\"variable\" : \"theField1\","+
                                "	\"sortOrder\":\"12\","+

                                "	\"defaultValue\" : \"THE DEFAULT VALUE 2\","+
                                "	\"regexCheck\" : \"THE VALIDATION REGEX 2\","+
                                "	\"hint\" : \"THE HINT 2\","+

                                "	\"required\" : \"false\","+
                                "	\"searchable\" : \"false\","+
                                "	\"indexed\" : \"false\""+
                                "	}";
                    }

                    @Override
                    protected void assertFieldUpdate(Field field) {
                        assertTrue(field instanceof WysiwygField);
                        assertEquals(DataTypes.LONG_TEXT, field.dataType());
                        assertNotNull(field.id());
                        assertEquals("The Field 2", field.name());
                        assertEquals("theField1", field.variable());

                        assertEquals("THE DEFAULT VALUE 2", field.defaultValue());
                        assertEquals("THE VALIDATION REGEX 2", field.regexCheck());
                        assertEquals("THE HINT 2", field.hint());

                        assertFalse(field.required());
                        assertFalse(field.searchable());
                        assertFalse(field.indexed());

                        assertFalse(field.readOnly());
                        assertFalse(field.fixed());
                        assertEquals(12, field.sortOrder());
                    }
                }
        );
    }

    private static abstract class AbstractFieldTester {

        protected abstract String getJsonFieldCreate();
        protected abstract String getJsonFieldUpdate();

        protected abstract void assertFieldCreate(Field field);
        protected abstract void assertFieldUpdate(Field field);

        public void run() throws Exception {

            final WebResource webResourceThatReturnsAdminUser = mock(WebResource.class);
            final InitDataObject dataObject1 = mock(InitDataObject.class);
            when(dataObject1.getUser()).thenReturn(APILocator.systemUser());
            when(webResourceThatReturnsAdminUser
                    .init(nullable(String.class), any(HttpServletRequest.class),any(HttpServletResponse.class), anyBoolean(),
                            nullable(String.class))).thenReturn(dataObject1);

            final FieldResource resource = new FieldResource(webResourceThatReturnsAdminUser, APILocator.getContentTypeFieldAPI());

            final ContentType contentType = getContentType();

            Response response;
            Map<String, Object> fieldMap;

            // Test Field Creation
            assertResponse_OK(
                    response = resource.createContentTypeField(
                            contentType.id(), getJsonFieldCreate().replace("CONTENT_TYPE_ID", contentType.id()), getHttpRequest(),  new EmptyHttpResponse()
                    )
            );

            assertNotNull(
                    fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
            );

            try {
                assertFieldCreate(
                        convertMapToField((Map<String, Object>) fieldMap)
                );

                // Test Field Retrieval by Id
                assertResponse_OK(
                        response = resource.getContentTypeFieldById((String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse())
                );

                assertNotNull(
                        fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
                );

                assertFieldCreate(
                        convertMapToField((Map<String, Object>) fieldMap)
                );

                // Test Field Update
                assertResponse_OK(
                        response = resource.updateContentTypeFieldById(
                                (String) fieldMap.get("id"),
                                getJsonFieldUpdate().replace("CONTENT_TYPE_ID", contentType.id()).replace("CONTENT_TYPE_FIELD_ID", (String) fieldMap.get("id")),
                                getHttpRequest(),  new EmptyHttpResponse()
                        )
                );

                assertNotNull(
                        fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
                );

                assertFieldUpdate(
                        convertMapToField((Map<String, Object>) fieldMap)
                );

                // Test Field Retrieval by Var
                assertResponse_OK(
                        response = resource.getContentTypeFieldByVar(contentType.id(), (String) fieldMap.get("variable"), getHttpRequest(),  new EmptyHttpResponse())
                );

                assertNotNull(
                        fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
                );

                assertFieldUpdate(
                        convertMapToField((Map<String, Object>) fieldMap)
                );

            } finally {

                // Test Field Deletion
                assertResponse_OK(
                        response = resource.deleteContentTypeFieldById((String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse())
                );

                assertResponse_NOT_FOUND(
                        response = resource.getContentTypeFieldById((String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse())
                );
            }
        }
    }

    private void testField(AbstractFieldTester fieldTester) throws Exception {
        fieldTester.run();
    }


    private static void assertResponse_OK(Response response){
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
        assertTrue(response.getEntity() instanceof ResponseEntityView);
        assertTrue(
                (ResponseEntityView.class.cast(response.getEntity()).getErrors() == null) ||
                        ResponseEntityView.class.cast(response.getEntity()).getErrors().isEmpty()
        );
    }

    private static void assertResponse_NOT_FOUND(Response response){
        assertNotNull(response);
        assertEquals(404, response.getStatus());
        assertNotNull(response.getEntity());
    }


    private static Field convertMapToField(Map<String, Object> fieldMap) {
        try {
            fieldMap.remove("fieldVariables");
            fieldMap.remove("fieldTypeLabel");
            fieldMap.remove("fieldType");

            return mapper.readValue(
                    mapper.writeValueAsString(fieldMap),
                    Field.class
            );
        } catch (IOException e) {
            return null;
        }
    }

    private static ContentType getContentType() throws DotDataException, DotSecurityException {
        User user = APILocator.getUserAPI().getSystemUser();
        return APILocator.getContentTypeAPI(user).find(typeName);
    }

    private static HttpServletRequest getHttpRequest() {
        MockHeaderRequest request = new MockHeaderRequest(
                (
                        new MockSessionRequest(new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                ).request()
        );

        request.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));

        return request;
    }
}
