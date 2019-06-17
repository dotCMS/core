package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.field.layout.*;
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
import com.dotmarketing.util.FileUtil;
import org.glassfish.jersey.internal.util.Base64;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.json.JSONException;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.*;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FieldResourceTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void shouldFixContentTypesFieldsBeforeReturn () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();

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
    public void shouldFixMaxColumnsPerRowRules () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();

        final List<Field> fields = createLayoutWithManyColumns(type);

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.getContentTypeFields(type.id(), getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        assertEquals(1, rows.size());
        assertEquals(4, rows.get(0).getColumns().size());
        assertEquals(0, rows.get(0).getColumns().get(0).getFields().size());
        assertEquals(1, rows.get(0).getColumns().get(1).getFields().size());
        assertEquals(fields.get(3).id(), rows.get(0).getColumns().get(1).getFields().get(0).id());
        assertEquals(1, rows.get(0).getColumns().get(2).getFields().size());
        assertEquals(fields.get(5).id(), rows.get(0).getColumns().get(2).getFields().get(0).id());
        assertEquals(2, rows.get(0).getColumns().get(3).getFields().size());
        assertEquals(fields.get(8).id(), rows.get(0).getColumns().get(3).getFields().get(0).id());
        assertEquals(fields.get(10).id(), rows.get(0).getColumns().get(3).getFields().get(1).id());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        assertEquals(12, contentTypeFromDB.fields().size());
    }

    @Test
    public void shouldDeleteFields () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();

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
        final ContentType type = createContentType();

        final List<Field> fields = createFields(type);

        final DeleteFieldsForm form =
                new DeleteFieldsForm.Builder().fieldsID(list(fields.get(0).id())).build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.deleteFields(type.id(), form, getHttpRequest());
    }

    @Test
    public void shouldUpdateFields () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();

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
    public void shouldUpdateFieldsInLegacyContentTypeWhenSortOrderIsNotSend () throws DotSecurityException, DotDataException {
        final String fieldNewName = "Updated";
        final ContentType type = createContentType();

        final List<Field> fields = createLegacyLayout(type);

        final Field fieldToUpdate = fields.get(1);
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldToUpdate);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();
        fieldToUpdatetMap.put("name", fieldNewName);

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(fieldToUpdatetMap)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.updateField(type.id(), fieldToUpdatetMap.get("id").toString(), form, getHttpRequest());


        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        assertEquals(fieldNewName, contentTypeFromDB.fields().get(1).name());
    }

    @Test
    public void shouldUpdateFieldsInLegacyContentTypeWhenSortOrderWasNotSent () throws DotSecurityException, DotDataException {
        final String fieldNewName = "Updated";
        final ContentType type = createContentType();

        final List<Field> fields = createLegacyLayout(type);

        final Field fieldToUpdate = fields.get(1);
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldToUpdate);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();
        fieldToUpdatetMap.put("name", fieldNewName);
        fieldToUpdatetMap.remove("sortOrder");

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(fieldToUpdatetMap)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.updateField(type.id(), fieldToUpdatetMap.get("id").toString(), form, getHttpRequest());


        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        assertEquals(fieldNewName, contentTypeFromDB.fields().get(1).name());
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowExceptionWhenTryUpdateSortOrderInWrongLayoutContentType () throws DotSecurityException, DotDataException {
        final String fieldNewName = "Updated";
        final ContentType type = createContentType();

        final List<Field> fields = createLegacyLayout(type);

        final Field fieldToUpdate = fields.get(1);
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldToUpdate);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();
        fieldToUpdatetMap.put("name", fieldNewName);
        fieldToUpdatetMap.put("sortOrder", 0);

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(fieldToUpdatetMap)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.updateField(type.id(), fieldToUpdatetMap.get("id").toString(), form, getHttpRequest());
    }

    @Test
    public void shouldCreateField () throws DotSecurityException, DotDataException, JSONException {
        final  ContentType type = createContentType();

        createFields(type);

        Field newField = createTextField(type, "new field", 4);

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
    public void shouldUpdateFieldsAndThrowException () throws DotSecurityException, DotDataException {
        final  ContentType type = createContentType();

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
        final  ContentType type = createContentType();

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

        final Optional<Field> optionalField = contentTypeFromDB.fields()
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
        final ContentType type = createContentType();

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
        final ContentType type = createContentType();

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

        ContentType type = createContentType();

        createFields(type);

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
        final ContentType type = createContentType();

        createFields(type);

        Field newField = createTextField(type, "new field", 4);

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
        final ContentType type = createContentType();

        final List<Field> fields = createFields(type);

        final Field newField = createTextField(type, "new field", 2);

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
            assertEquals(fieldsExpected.get(i).name(), fieldsFromDB.get(i).name());
        }
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowExceptionWhenNewFieldTurnIntoWrongLayout () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();

        createFields(type);

        Field newField = createTextField(type, "new field", 1);

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
        final ContentType type = createContentType();

        final List<Field> fields = createFields(type);
        final List<Map<String, Object>> layout = getToLayoutMap(list(fields.get(0), fields.get(1), fields.get(3), fields.get(2)));

        final MoveFieldsForm form =
                new MoveFieldsForm.Builder().layout(layout)
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
        assertEquals(fields.get(0).id(), fieldsFromDB.get(0).id());
        assertEquals(fields.get(1).id(), fieldsFromDB.get(1).id());
        assertEquals(fields.get(3).id(), fieldsFromDB.get(2).id());
        assertEquals(fields.get(2).id(), fieldsFromDB.get(3).id());
    }

    @Test
    public void shouldFixMaxColumnsPerRowRulesWhenMovesAWrongLayoutContentType () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();

        createLayoutWithManyColumns(type);

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.getContentTypeFields(type.id(), getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        final List<Map<String, Object>> layout = getToLayoutMap(getFields(rows));

        final MoveFieldsForm form =
                new MoveFieldsForm.Builder().layout(layout)
                        .build();

        fieldResource.moveFields(type.id(), form, getHttpRequest());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        assertEquals(9, contentTypeFromDB.fields().size());
    }

    private List<Field> getFields(final List<FieldLayoutRow> rows) {
        final List<Field> fields = new ArrayList();

        for (final FieldLayoutRow row : rows) {
            fields.add(row.getDivider());

            for (final FieldLayoutColumn column : row.getColumns()) {
                fields.add(column.getColumn());
                fields.addAll(column.getFields());
            }
        }
        return fields;
    }

    @NotNull
    private List<Map<String, Object>> getToLayoutMap(final List<Field> fields) {
        final List<Map<String, Object>> result = new ArrayList<>();

        final List<FieldUtil.FieldsFragment> fieldsFragments = FieldUtil.splitByFieldDivider(fields);

        for (final FieldUtil.FieldsFragment fieldsFragment : fieldsFragments) {
            final Map rowMap = new HashMap();
            rowMap.put("divider", getMap(fieldsFragment.getFieldDivider()));

            final List<List<Field>> columns = FieldUtil.splitByColumnField(fieldsFragment.getOthersFields());

            final List<Map<String, Object>> columnsMap = new ArrayList<>();

            for (final List<Field> columnFields : columns) {
                final Map columnMap = new HashMap();
                columnMap.put("columnDivider", getMap(columnFields.get(0)));
                columnMap.put("fields", columnFields.subList(1, columnFields.size())
                        .stream().map(field -> getMap(field)).collect(toImmutableList())
                );

                columnsMap.add(columnMap);
            }

            rowMap.put("columns", columnsMap);
            result.add(rowMap);
        }

        return result;
    }

    private Map getMap(final Field field) {
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(field);
        return jsonFieldTransformer.mapObject();
    }

    @Test(expected = NotFoundInDbException.class)
    public void shouldThrowExceptionWhenContentTypeDoesNotExists () throws DotSecurityException, DotDataException {

        final List<Map<String, Object>> layoput =  new ArrayList<>();

        final MoveFieldsForm form =
                new MoveFieldsForm.Builder().layout(layoput)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.moveFields("NotExists", form, getHttpRequest());
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowExceptionWhenUpdateFieldWithMaxColumnsRule () throws DotSecurityException, DotDataException, JSONException {
        final ContentType type = createContentType();

        final List<Field> fields = createLayoutWithMultiRow(type);

        final Field fieldToUpdate = fields.get(12);
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldToUpdate);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();
        fieldToUpdatetMap.put("sortOrder", 7);

        final UpdateFieldsForm form =
                new UpdateFieldsForm.Builder().fields(list(fieldToUpdatetMap))
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.updateFields(type.id(), form, getHttpRequest());
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowExceptionWhenDeleteFieldWithMaxColumnsRule () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();

        final List<Field> fields = createLayoutWithMultiRow(type);

        final DeleteFieldsForm form =
                new DeleteFieldsForm.Builder().fieldsID(list(fields.get(7).id())).build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.deleteFields(type.id(), form, getHttpRequest());
    }

    private ContentType createContentType() throws DotDataException, DotSecurityException {
        final String typeName = "fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);
        return type;
    }

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowExceptionWhenMoveFieldWithMaxColumnsRule () throws DotSecurityException, DotDataException, JSONException {
        final ContentType type = createContentType();

        final List<Field> fields = createLayoutWithMultiRow(type);
        final List<Map<String, Object>> layout = list(
                map("divider", getMap(fields.get(0)), "columns", list(
                        map("columnDivider", getMap(fields.get(1)), "fields", list()),
                        map("columnDivider", getMap(fields.get(2)), "fields", list(getMap(fields.get(3)))),
                        map("columnDivider", getMap(fields.get(4)), "fields", list(getMap(fields.get(5)))),
                        map("columnDivider", getMap(fields.get(6)), "fields", list()),
                        map("columnDivider", getMap(fields.get(6)), "fields", list())
                )),
                map("divider", getMap(fields.get(7)), "columns", list(
                        map("columnDivider", getMap(fields.get(8)), "fields", list(getMap(fields.get(9)))),
                        map("columnDivider", getMap(fields.get(10)), "fields", list(getMap(fields.get(11))))
                ))
        );

        final MoveFieldsForm form =
                new MoveFieldsForm.Builder().layout(layout)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.moveFields(type.id(), form, getHttpRequest());
    }

    @Test
    public void shouldRemoveEmptyRows () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();

        final List<Field> fields = createLayoutWithEmptyRows(type);

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.getContentTypeFields(type.id(), getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getColumns().size());
        assertEquals(1, rows.get(0).getColumns().get(0).getFields().size());

        assertEquals(fields.get(3).id(), rows.get(0).getDivider().id());
        assertEquals(fields.get(4).id(), rows.get(0).getColumns().get(0).getColumn().id());
        assertEquals(fields.get(5).id(), rows.get(0).getColumns().get(0).getFields().get(0).id());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        assertEquals(6, contentTypeFromDB.fields().size());
    }

    private List<Field> createFields(final ContentType type) throws DotDataException, DotSecurityException {
        return list(
            createRowField(type, "row field", 0),
            createColumnField(type, "column field", 1),
            createTextField(type, "text 1", 2),
            createTextField(type, "text 2", 3)
        );
    }

    private List<Field> createLegacyLayout(final ContentType type) throws DotDataException, DotSecurityException {
        return list(
            createTextField(type, "text 1", 0),
            createTextField(type, "text 2", 1)
        );
    }

    private List<Field> createLayoutWithMultiRow(final ContentType type) throws DotDataException, DotSecurityException {

        return list(
            createRowField(type, "row field", 0),
            createColumnField(type, "column field", 1),
            createColumnField(type, "column field2", 2),
            createTextField(type, "text 1", 3),
            createColumnField(type, "column field3", 4),
            createTextField(type, "text 2", 5),
            createColumnField(type, "column field4", 6),

            createRowField(type, "row field 2", 7),
            createColumnField(type, "column field5", 8),
            createTextField(type, "text 3", 9),
            createColumnField(type, "column field6", 10),
            createTextField(type, "text 4", 11),
            createColumnField(type, "column field7", 12)
        );
    }

    private List<Field> createLayoutWithEmptyRows(final ContentType type) throws DotDataException, DotSecurityException {

        return list(
                createRowField(type, "row field 1", 0),
                createRowField(type, "row field 2", 1),
                createRowField(type, "row field 3", 2),
                createRowField(type, "row field 4", 3),
                createColumnField(type, "column field 1", 4),
                createTextField(type, "text 1", 5)
        );
    }

    private List<Field> createLayoutWithManyColumns(final ContentType type) throws DotDataException, DotSecurityException {
        return list(
            createRowField(type, "row field", 0),
            createColumnField(type, "column field", 1),
            createColumnField(type, "column field2", 2),
            createTextField(type, "text 1", 3),
            createColumnField(type, "column field3", 4),
            createTextField(type, "text 2", 5),
            createColumnField(type, "column field4", 6),
            createColumnField(type, "column field5", 7),
            createTextField(type, "text 3", 8),
            createColumnField(type, "column field6", 9),
            createTextField(type, "text 4", 10),
            createColumnField(type, "column field7", 11)
        );
    }

    private Field createRowField(ContentType type, String s, int i) {
        final Field field = FieldBuilder.builder(RowField.class)
                .name(s)
                .sortOrder(i)
                .contentTypeId(type.id())
                .build();

        try{
            return APILocator.getContentTypeFieldAPI().save(field, APILocator.systemUser());
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private Field createColumnField(final ContentType type, final String name, final int sortOrder) {
        final Field field = FieldBuilder.builder(ColumnField.class)
                .name(name)
                .sortOrder(sortOrder)
                .contentTypeId(type.id())
                .build();

        try {
            return APILocator.getContentTypeFieldAPI().save(field, APILocator.systemUser());
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private Field createTextField(final ContentType type, final String name, final int sortOrder) {
        final Field field =  FieldBuilder.builder(TextField.class)
                .name(name)
                .sortOrder(sortOrder)
                .contentTypeId(type.id())
                .build();

        try {
            return APILocator.getContentTypeFieldAPI().save(field, APILocator.systemUser());
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
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
