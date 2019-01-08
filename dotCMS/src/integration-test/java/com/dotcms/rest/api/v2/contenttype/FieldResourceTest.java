package com.dotcms.rest.api.v2.contenttype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.internal.util.Base64;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FieldResourceTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String typeName="fieldResourceTest" + UUIDUtil.uuid();

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
    public void testCreateRelationshipFieldWithDash_Return400() throws Exception {
        final FieldResource resource = new FieldResource();

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
                contentType.id(), jsonField.replace("CONTENT_TYPE_ID", contentType.id()), getHttpRequest());

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertTrue("Error: " + response.getEntity().toString(), response.getEntity().toString().contains("contains characters not allowed"));
    }

    @Test
    public void testUpdateFieldVariable_Return400() throws Exception {
        final FieldResource resource = new FieldResource();

        final ContentType contentType = getContentType();

        final String jsonField = "{"+
                // IDENTITY VALUES
                "	\"clazz\" : \"" + ImmutableRelationshipField.class.getCanonicalName() +"\","+
                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                "	\"name\" : \"YouTube Videos\","+
                "   \"values\" :  1," +
                "   \"variable\": \"youtubeVideos\","+
                "   \"relationType\": \"Youtube\""+
                "	}";

        Response response = resource.createContentTypeField(
                contentType.id(), jsonField.replace("CONTENT_TYPE_ID", contentType.id()), getHttpRequest());

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        final Map<String, Object> fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity();

        final String jsonFieldUpdate = "{"+
                // IDENTITY VALUES
                "	\"clazz\" : \"" + ImmutableRelationshipField.class.getCanonicalName() +"\","+
                "	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
                "	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
                "	\"name\" : \"YouTube Videos\","+
                "   \"values\" :  1," +
                "   \"variable\": \"youtube-Videos\","+
                "   \"relationType\": \"Youtube\""+
                "	}";

        response = resource.updateContentTypeFieldById(
                (String) fieldMap.get("id"),
                jsonFieldUpdate.replace("CONTENT_TYPE_ID", contentType.id()).replace("CONTENT_TYPE_FIELD_ID", (String) fieldMap.get("id")),
                getHttpRequest());

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertTrue("Error: " + response.getEntity().toString(), response.getEntity().toString().contains("Field variable can not be modified"));
    }


    @Test
    public void testFieldsList() throws Exception {
        final FieldResource resource = new FieldResource();

        ContentType contentType = getContentType();
       //Test using ContentType Id
        Response response = resource.getContentTypeFields(contentType.id(), getHttpRequest());

        assertResponse_OK(response);

        List fields = (List) ((ResponseEntityView) response.getEntity()).getEntity();

        assertFalse(fields.isEmpty());

        for(Object fieldMap : fields){
            Field field = convertMapToField((Map<String, Object>) fieldMap);

            assertNotNull(field);

            assertTrue(field.getClass().getSimpleName().startsWith("Immutable"));
        }
        //Now test using variable name
        response = resource.getContentTypeFields(contentType.variable(), getHttpRequest());
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

            final FieldResource resource = new FieldResource();

            final ContentType contentType = getContentType();

            Response response;
            Map<String, Object> fieldMap;

            // Test Field Creation
            assertResponse_OK(
                    response = resource.createContentTypeField(
                            contentType.id(), getJsonFieldCreate().replace("CONTENT_TYPE_ID", contentType.id()), getHttpRequest()
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
                        response = resource.getContentTypeFieldById((String) fieldMap.get("id"), getHttpRequest())
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
                                getHttpRequest()
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
                        response = resource.getContentTypeFieldByVar(contentType.id(), (String) fieldMap.get("variable"), getHttpRequest())
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
                        response = resource.deleteContentTypeFieldById((String) fieldMap.get("id"), getHttpRequest())
                );

                assertResponse_NOT_FOUND(
                        response = resource.getContentTypeFieldById((String) fieldMap.get("id"), getHttpRequest())
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
                        new MockSessionRequest(new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                ).request()
        );

        request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

        return request;
    }
}
