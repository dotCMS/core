package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.field.layout.FieldLayoutColumn;
import com.dotcms.contenttype.model.field.layout.FieldLayoutRow;
import com.dotcms.contenttype.model.field.layout.FieldLayoutValidationException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import javax.ws.rs.core.Response;

import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.util.JsonArrayToLinkedSetConverter;
import com.dotmarketing.util.json.JSONArray;
import org.glassfish.jersey.internal.util.Base64;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FieldResourceTest {

    private String field;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void shouldFixContentTypesFieldsBeforeReturn () throws DotSecurityException, DotDataException {
        final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        Field field = FieldBuilder.builder(TextField.class).name("text").contentTypeId(type.id()).build();
        field = APILocator.getContentTypeFieldAPI().save(field,APILocator.systemUser());

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.getContentTypeFields(type.id(), getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getColumns().size());
        assertEquals(field.id(), rows.get(0).getColumns().get(0).getFields().get(0).id());
    }

    @Test
    public void shouldDeleteFields () throws DotSecurityException, DotDataException {
        final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);

        final DeleteFieldsForm form =
                new DeleteFieldsForm.Builder().fieldsID(list(fields.get(2).id(), fields.get(3).id())).build();
        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.deleteFields(type.id(), form, getHttpRequest());

        final Map<String, Object> responseMap = (Map<String, Object>)
                ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();
        final List<FieldLayoutRow> rows = (List<FieldLayoutRow>) responseMap.get("fields");

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getColumns().size());
        assertEquals(0, rows.get(0).getColumns().get(0).getFields().size());

        final List<String> deletedIds = (List<String>) responseMap.get("deletedIds");

        assertEquals(2, deletedIds.size());
        assertTrue(deletedIds.contains(fields.get(2).id()));
        assertTrue(deletedIds.contains(fields.get(3).id()));

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        final List<Field> listExpected = list(fields.get(0), fields.get(1));
        assertEquals(
                listExpected.stream().map(field -> field.id()).collect(Collectors.toList()),
                contentTypeFromDB.fields().stream().map(field -> field.id()).collect(Collectors.toList())
        );
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowExceptionAndNotDeleteAny () throws DotSecurityException, DotDataException {
        final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);

        final DeleteFieldsForm form =
                new DeleteFieldsForm.Builder().fieldsID(list(fields.get(0).id())).build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.deleteFields(type.id(), form, getHttpRequest());
    }

    @Test
    public void shouldUpdateFields () throws DotSecurityException, DotDataException, JSONException {
        final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);

        final Field fieldToUpdate = fields.get(3);
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldToUpdate);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();
        fieldToUpdatetMap.put("sortOrder", 2);

        final UpdateFieldsForm form =
                new UpdateFieldsForm.Builder().fields(list(fieldToUpdatetMap))
                        .build();
        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.updateFields(type.id(), form, getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getColumns().size());

        final List<Field> columnFields = rows.get(0).getColumns().get(0).getFields();
        assertEquals(2, columnFields.size());
        assertEquals(fields.get(3).id(), columnFields.get(0).id());
        assertEquals(fields.get(2).id(), columnFields.get(1).id());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        final List<Field> listExpected = list(fields.get(0), fields.get(1), fields.get(3), fields.get(2));
        assertEquals(
                listExpected.stream().map(field -> field.id()).collect(Collectors.toList()),
                contentTypeFromDB.fields().stream().map(field -> field.id()).collect(Collectors.toList())
        );
    }

    @Test
    public void shouldCreateField () throws DotSecurityException, DotDataException, JSONException {
        final String typeName="fieldResourceTest" + System.currentTimeMillis();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).variable(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        createFields(type);

        Field newField = FieldBuilder.builder(TextField.class)
                .name("new field")
                .sortOrder(4)
                .contentTypeId(type.id())
                .build();

        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(newField);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();

        final UpdateFieldsForm form =
                new UpdateFieldsForm.Builder().fields(list(fieldToUpdatetMap))
                        .build();

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.updateFields(type.id(), form, getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getColumns().size());

        final List<Field> columnFields = rows.get(0).getColumns().get(0).getFields();
        assertEquals(3, columnFields.size());

        assertNotNull(columnFields.get(0).id());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());

        assertEquals(
                columnFields.get(2).id(),
                contentTypeFromDB.fields().get(4).id()
        );
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldUpdateFieldsANdThrowException () throws DotSecurityException, DotDataException {
        final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);

        final Field fieldToUpdate = fields.get(0);
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldToUpdate);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();
        fieldToUpdatetMap.put("sortOrder", 4);

        final UpdateFieldsForm form =
                new UpdateFieldsForm.Builder().fields(list(fieldToUpdatetMap))
                        .build();
        final FieldResource fieldResource = new FieldResource();
        fieldResource.updateFields(type.id(), form, getHttpRequest());
    }

    @Test
    public void shouldUpdateField () throws DotSecurityException, DotDataException {
        final String updateFieldName = "field Updated";
        final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);

        final Field fieldToUpdate = fields.get(3);
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldToUpdate);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();
        fieldToUpdatetMap.put("name", updateFieldName);

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(fieldToUpdatetMap)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.updateField(type.id(), fieldToUpdate.id(), form, getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getColumns().size());

        final List<Field> columnFields = rows.get(0).getColumns().get(0).getFields();
        assertEquals(2, columnFields.size());
        assertEquals(fields.get(2).id(), columnFields.get(0).id());
        assertEquals(fields.get(3).id(), columnFields.get(1).id());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());

        Optional<Field> optionalField = contentTypeFromDB.fields()
                .stream()
                .filter(field -> field.id().equals(fieldToUpdate.id()))
                .findFirst();

        optionalField.ifPresent(field -> assertEquals(field.name(), updateFieldName));

        if (!optionalField.isPresent()) {
            throw new RuntimeException("The field should exists");
        }
    }

    @Test
    public void shouldUpdateFieldAndUpdateSortOrder () throws DotSecurityException, DotDataException {
        final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);

        final Field fieldToUpdate = fields.get(3);
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldToUpdate);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();
        fieldToUpdatetMap.put("sortOrder", 2);

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(fieldToUpdatetMap)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.updateField(type.id(), fieldToUpdate.id(), form, getHttpRequest());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());

        List<Field> fieldsFromDB = contentTypeFromDB.fields();
        assertEquals(fieldsFromDB.size(), fieldsFromDB.size());

        for (int i = 0; i < fieldsFromDB.size(); i++) {
            assertEquals(fieldsFromDB.get(i).sortOrder(), i);
            assertEquals(fieldsFromDB.get(i).name(), fieldsFromDB.get(i).name());
        }
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowErrorWhenUpdateTurnIntoWronhLayout () throws DotSecurityException, DotDataException {
        final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);

        final Field fieldToUpdate = fields.get(3);
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldToUpdate);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();
        fieldToUpdatetMap.put("sortOrder", 1);

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(fieldToUpdatetMap)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.updateField(type.id(), fieldToUpdate.id(), form, getHttpRequest());
    }

    @Test(expected = NotFoundInDbException.class)
    public void shouldThrowErrorWhenContentTypeDoesNotExists () throws DotSecurityException, DotDataException {

        Field field = FieldBuilder.builder(TextField.class)
                .name("text 2")
                .sortOrder(3)
                .build();

        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(field);

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(jsonFieldTransformer.mapObject())
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.updateField("notExists", "notExists", form, getHttpRequest());
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowErrorWhenFieldDoesNotExists () throws DotSecurityException, DotDataException {

        final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);

        Field field = FieldBuilder.builder(TextField.class)
                .name("text 2")
                .sortOrder(3)
                .build();

        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(field);

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(jsonFieldTransformer.mapObject())
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.updateField(type.id(), "notExists", form, getHttpRequest());
    }


    @Test
    public void shouldCreateFieldWithPost () throws DotSecurityException, DotDataException {
        final String typeName="fieldResourceTest" + System.currentTimeMillis();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).variable(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        createFields(type);

        Field newField = FieldBuilder.builder(TextField.class)
                .name("new field")
                .sortOrder(4)
                .contentTypeId(type.id())
                .build();

        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(newField);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(fieldToUpdatetMap)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.createField(type.id(), form, getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getColumns().size());

        final List<Field> columnFields = rows.get(0).getColumns().get(0).getFields();
        assertEquals(3, columnFields.size());

        assertNotNull(columnFields.get(0).id());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());

        assertEquals(
                columnFields.get(2).id(),
                contentTypeFromDB.fields().get(4).id()
        );
    }

    @Test
    public void shouldCreateFieldAndUpdateSortOrder () throws DotSecurityException, DotDataException {
        final String typeName="fieldResourceTest" + System.currentTimeMillis();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).variable(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);

        final Field newField = FieldBuilder.builder(TextField.class)
                .name("new field")
                .sortOrder(2)
                .contentTypeId(type.id())
                .build();

        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(newField);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(fieldToUpdatetMap)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.createField(type.id(), form, getHttpRequest());

        final List<Field> fieldsExpected = list(fields.get(0), fields.get(1), newField, fields.get(2), fields.get(3));
        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());

        List<Field> fieldsFromDB = contentTypeFromDB.fields();
        assertEquals(fieldsFromDB.size(), fieldsFromDB.size());

        for (int i = 0; i < fieldsFromDB.size(); i++) {
            assertEquals(fieldsFromDB.get(i).sortOrder(), i);
            assertEquals(fieldsFromDB.get(i).name(), fieldsFromDB.get(i).name());
        }
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowExceptionWhenNewFieldTurnIntoWrongLayout () throws DotSecurityException, DotDataException {
        final String typeName="fieldResourceTest" + System.currentTimeMillis();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).variable(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        createFields(type);

        Field newField = FieldBuilder.builder(TextField.class)
                .name("new field")
                .sortOrder(1)
                .contentTypeId(type.id())
                .build();

        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(newField);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(fieldToUpdatetMap)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.createField(type.id(), form, getHttpRequest());
    }

    @Test(expected = NotFoundInDbException.class)
    public void shouldThrowErrorWhenContentTypeDoesNotExistsInCreateField () throws DotSecurityException, DotDataException {

        Field field = FieldBuilder.builder(TextField.class)
                .name("text 2")
                .sortOrder(3)
                .build();

        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(field);

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(jsonFieldTransformer.mapObject())
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.createField("notExists", form, getHttpRequest());
    }

    @Test
    public void shouldMoveFields () throws DotSecurityException, DotDataException {
        /*final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);
        final List<FieldLayoutRow> rows =  this.getFieldLayoutRow(fields.get(0), fields.get(1),
            new Field[]{fields.get(3), fields.get(2)});

        final MoveFieldsForm form =
                new MoveFieldsForm.Builder().layout(rows)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.moveFields(type.id(), form, getHttpRequest());

        final List<FieldLayoutRow> responseRows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        assertEquals(1, responseRows.size());
        assertEquals(1, responseRows.get(0).getColumns().size());

        final List<Field> columnFields = responseRows.get(0).getColumns().get(0).getFields();
        assertEquals(2, columnFields.size());
        assertEquals(fields.get(3).id(), columnFields.get(0).id());
        assertEquals(fields.get(2).id(), columnFields.get(1).id());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        List<Field> fieldsFromDB = contentTypeFromDB.fields();

        assertEquals(fieldsFromDB.size(), 4);
        assertEquals(((Field) rows.get(0).getDivider()).id(), fieldsFromDB.get(0).id());
        assertEquals(rows.get(0).getColumns().get(0).getColumn().id(), fieldsFromDB.get(1).id());
        assertEquals(rows.get(0).getColumns().get(0).getFields().get(0).id(), fieldsFromDB.get(2).id());
        assertEquals(rows.get(0).getColumns().get(0).getFields().get(1).id(), fieldsFromDB.get(3).id());*/
    }

    private List<FieldLayoutRow>  getFieldLayoutRow(final Field row, final Field column, final Field[] fields)  {
        final FieldLayoutColumn fieldLayoutColumn = new FieldLayoutColumn((ColumnField) column, Arrays.asList(fields));
        return list(new FieldLayoutRow((FieldDivider) row, list(fieldLayoutColumn)));
    }

    @Test(expected = NotFoundInDbException.class)
    public void shouldThrowExceptionWhenContentTypeDoesNotExists () throws DotSecurityException, DotDataException {
        /*final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);
        final List<FieldLayoutRow> rows =  this.getFieldLayoutRow(fields.get(0), fields.get(1),
                new Field[]{fields.get(3), fields.get(2)});

        final MoveFieldsForm form =
                new MoveFieldsForm.Builder().layout(rows)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.moveFields("NotExists", form, getHttpRequest());*/
    }

    private List<Field> createFields(final ContentType type) throws DotDataException, DotSecurityException {
        Field rowField = FieldBuilder.builder(RowField.class)
                .name("row field")
                .sortOrder(0)
                .contentTypeId(type.id())
                .build();
        rowField = APILocator.getContentTypeFieldAPI().save(rowField,APILocator.systemUser());

        Field columnField = FieldBuilder.builder(ColumnField.class)
                .name("column field")
                .sortOrder(1)
                .contentTypeId(type.id())
                .build();
        columnField = APILocator.getContentTypeFieldAPI().save(columnField,APILocator.systemUser());

        Field field_1 = FieldBuilder.builder(TextField.class)
                .name("text 1")
                .sortOrder(2)
                .contentTypeId(type.id())
                .build();
        field_1 = APILocator.getContentTypeFieldAPI().save(field_1,APILocator.systemUser());

        Field field_2 = FieldBuilder.builder(TextField.class)
                .name("text 2")
                .sortOrder(3)
                .contentTypeId(type.id())
                .build();
        field_2 = APILocator.getContentTypeFieldAPI().save(field_2,APILocator.systemUser());

        return list(rowField, columnField, field_1, field_2);
    }

    private static HttpServletRequest getHttpRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                (
                        new MockSessionRequest(new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                ).request()
        );

        request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

        return request;
    }
}
