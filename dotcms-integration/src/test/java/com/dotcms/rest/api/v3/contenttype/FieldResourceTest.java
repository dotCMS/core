package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableColumnField;
import com.dotcms.contenttype.model.field.ImmutableRowField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.layout.FieldLayoutColumn;
import com.dotcms.contenttype.model.field.layout.FieldLayoutRow;
import com.dotcms.contenttype.model.field.layout.FieldLayoutValidationException;
import com.dotcms.contenttype.model.field.layout.FieldUtil;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.dotmarketing.util.json.JSONException;
import org.glassfish.jersey.internal.util.Base64;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.toImmutableList;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class FieldResourceTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * When try to update a field in a Content Tye with a right Layout
     * Should Update successfully
     */
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

        final List<Field> fieldsFromResponse = this.getFields(rows);
        checkAllFieldsIds(fields, fieldsFromResponse);

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        checkAllFieldsIds(fields, contentTypeFromDB.fields());

        assertEquals(updateFieldName, contentTypeFromDB.fields().get(3).name());
    }

    /**
     * When try to update a field in a Content Tye with a wrong Layout
     * Should Update successfully and fix the content type layout
     */
    @Test
    public void shouldUpdateFieldsInLegacyContentType () throws DotSecurityException, DotDataException {
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
        final Response response = fieldResource.updateField(type.id(), fieldToUpdatetMap.get("id").toString(), form, getHttpRequest());
        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) response.getEntity()).getEntity();

        final List<Field> fieldsFromResponse = this.getFields(rows);
        assertEquals(4, fieldsFromResponse.size());
        assertEquals(ImmutableRowField.class, fieldsFromResponse.get(0).getClass());
        assertEquals(ImmutableColumnField.class, fieldsFromResponse.get(1).getClass());
        assertEquals(fields.get(0).id(), fieldsFromResponse.get(2).id());
        assertEquals(fields.get(1).id(), fieldsFromResponse.get(3).id());
        assertEquals(fieldNewName, fieldsFromResponse.get(3).name());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        assertEquals(4, contentTypeFromDB.fields().size());
        assertEquals(ImmutableRowField.class, contentTypeFromDB.fields().get(0).getClass());
        assertEquals(ImmutableColumnField.class, contentTypeFromDB.fields().get(1).getClass());
        assertEquals(fields.get(0).id(), contentTypeFromDB.fields().get(2).id());
        assertEquals(fields.get(1).id(), contentTypeFromDB.fields().get(3).id());
        assertEquals(fieldNewName, contentTypeFromDB.fields().get(3).name());
    }

    /**
     * When try to update a field and the sort order is not sent
     * Should Update the other fields's properties and ignore the sort order
     */
    @Test
    public void shouldUpdateFieldAndIgnoreSortOrder () throws DotSecurityException, DotDataException {
        final String updateFieldName = "field Updated";
        final  ContentType type = createContentType();
        final List<Field> fields = createFields(type);

        final Field fieldToUpdate = fields.get(3);
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldToUpdate);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();
        fieldToUpdatetMap.put("name", updateFieldName);
        fieldToUpdatetMap.remove("sortOrder");

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(fieldToUpdatetMap)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.updateField(type.id(), fieldToUpdate.id(), form, getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        final List<Field> fieldsFromResponse = this.getFields(rows);
        checkAllFieldsIds(fields, fieldsFromResponse);

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        checkAllFieldsIds(fields, contentTypeFromDB.fields());

        assertEquals(updateFieldName, contentTypeFromDB.fields().get(3).name());
    }

    /**
     * When try to update a field in a Content Tye with a wrong Layout and not sent the sort order
     * Should Update successfully, fix the content type layout and ignore the sort order
     */
    @Test
    public void shouldUpdateFieldAndIgnoreSortOrderWhenContentTypeHasWrongLayout () throws DotSecurityException, DotDataException {
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
        final Response response = fieldResource.updateField(type.id(), fieldToUpdatetMap.get("id").toString(), form, getHttpRequest());
        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) response.getEntity()).getEntity();

        final List<Field> fieldsFromResponse = this.getFields(rows);
        assertEquals(4, fieldsFromResponse.size());
        assertEquals(ImmutableRowField.class, fieldsFromResponse.get(0).getClass());
        assertEquals(ImmutableColumnField.class, fieldsFromResponse.get(1).getClass());
        assertEquals(fields.get(0).id(), fieldsFromResponse.get(2).id());
        assertEquals(fields.get(1).id(), fieldsFromResponse.get(3).id());
        assertEquals(fieldNewName, fieldsFromResponse.get(3).name());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        assertEquals(4, contentTypeFromDB.fields().size());
        assertEquals(ImmutableRowField.class, contentTypeFromDB.fields().get(0).getClass());
        assertEquals(ImmutableColumnField.class, contentTypeFromDB.fields().get(1).getClass());
        assertEquals(fields.get(0).id(), contentTypeFromDB.fields().get(2).id());
        assertEquals(fields.get(1).id(), contentTypeFromDB.fields().get(3).id());
        assertEquals(fieldNewName, contentTypeFromDB.fields().get(3).name());
    }

    /**
     * When try to update a field and the sort order is sent with a new value
     * Should Update the other fields's properties and ignore the sort order
     */
    @Test
    public void shouldUpdateFieldAndIgnoreSortOrderEvenWhenItSent () throws DotSecurityException, DotDataException {
        final String updateFieldName = "field Updated";
        final  ContentType type = createContentType();
        final List<Field> fields = createFields(type);

        final Field fieldToUpdate = fields.get(3);
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldToUpdate);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();
        fieldToUpdatetMap.put("name", updateFieldName);
        fieldToUpdatetMap.put("sortOrder", "0");

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().field(fieldToUpdatetMap)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.updateField(type.id(), fieldToUpdate.id(), form, getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        final List<Field> fieldsFromResponse = this.getFields(rows);
        checkAllFieldsIds(fields, fieldsFromResponse);

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        checkAllFieldsIds(fields, contentTypeFromDB.fields());

        assertEquals(updateFieldName, contentTypeFromDB.fields().get(3).name());
    }

    /**
     * When try to update a field in a Content Tye with a wrong Layout and sent the sort order
     * Should Update successfully, fix the content type layout and ignore the sort order
     */
    @Test
    public void shouldUpdateFieldAndIgnoreSortOrderEvenWhenItIsSentWhenContentTypeHasWrongLayout ()
            throws DotSecurityException, DotDataException {
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
        final Response response = fieldResource.updateField(type.id(), fieldToUpdatetMap.get("id").toString(), form, getHttpRequest());
        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) response.getEntity()).getEntity();

        final List<Field> fieldsFromResponse = this.getFields(rows);
        assertEquals(4, fieldsFromResponse.size());
        assertEquals(ImmutableRowField.class, fieldsFromResponse.get(0).getClass());
        assertEquals(ImmutableColumnField.class, fieldsFromResponse.get(1).getClass());
        assertEquals(fields.get(0).id(), fieldsFromResponse.get(2).id());
        assertEquals(fields.get(1).id(), fieldsFromResponse.get(3).id());
        assertEquals(fieldNewName, fieldsFromResponse.get(3).name());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        assertEquals(4, contentTypeFromDB.fields().size());
        assertEquals(ImmutableRowField.class, contentTypeFromDB.fields().get(0).getClass());
        assertEquals(ImmutableColumnField.class, contentTypeFromDB.fields().get(1).getClass());
        assertEquals(fields.get(0).id(), contentTypeFromDB.fields().get(2).id());
        assertEquals(fields.get(1).id(), contentTypeFromDB.fields().get(3).id());
        assertEquals(fieldNewName, contentTypeFromDB.fields().get(3).name());
    }

    /**
     * When try to update a field in a Content Tye does exists
     * Should throw a NotFoundInDbException
     */
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

        final ContentType type = createContentType();

        createFields(type);

        final Field field = FieldBuilder.builder(TextField.class)
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

    /**
     * When Get the fields of a Content Type with a right FieldLayout
     * Should Return exactly the curent content type layout
     */
    @Test
    public void shouldGetAllContentTypesFields () throws DotSecurityException, DotDataException {
        final  ContentType type = createContentType();
        final List<Field> fields = createFields(type);

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.getContentTypeFields(type.id(), getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        final List<Field> fieldsFromResponse = this.getFields(rows);
        checkAllFieldsIds(fields, fieldsFromResponse);

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        checkAllFieldsIds(fields, contentTypeFromDB.fields());
    }

    /**
     * When Get the fields of a Content Type with a wrong FieldLayout
     * Should Return fix the layout in the response
     */
    @Test
    public void shouldFixLayout () throws DotSecurityException, DotDataException {
        final  ContentType type = createContentType();
        final List<Field> fields = createLegacyLayout(type);

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.getContentTypeFields(type.id(), getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        final List<Field> fieldsFromResponse = this.getFields(rows);
        assertEquals(4, fieldsFromResponse.size());
        assertEquals(ImmutableRowField.class, fieldsFromResponse.get(0).getClass());
        assertEquals(ImmutableColumnField.class, fieldsFromResponse.get(1).getClass());
        assertEquals(fields.get(0).id(), fieldsFromResponse.get(2).id());
        assertEquals(fields.get(1).id(), fieldsFromResponse.get(3).id());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        assertEquals(2, contentTypeFromDB.fields().size());
        assertEquals(fields.get(0).id(), contentTypeFromDB.fields().get(0).id());
        assertEquals(fields.get(1).id(), contentTypeFromDB.fields().get(1).id());
    }

    /**
     * When Get the fields of a Content Type with a FieldLayout with Many columns in any row
     * Should Return a layout with Max of 4 columns by row
     */
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

    /**
     * When Get the fields of a Content Type with a FieldLayout with Empty rows
     * Should Return a fix layout
     */
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

    /**
     * When try to move fields in a Content Type with a right layout
     * Should update the field with the new sortOrder
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldMoveFields () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();

        final List<Field> fields = createFields(type);
        final List<Field> listExpected = list(fields.get(0), fields.get(1), fields.get(3), fields.get(2));
        final List<Map<String, Object>> layout = getToLayoutMap(listExpected);

        final MoveFieldsForm form =
                new MoveFieldsForm.Builder().layout(layout)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.moveFields(type.id(), form, getHttpRequest());

        final List<FieldLayoutRow> responseRows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        final List<Field> responseFields = this.getFields(responseRows);
        checkAllFieldsIds(listExpected, responseFields);

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        checkAllFieldsIds(listExpected, contentTypeFromDB.fields());
    }



    /**
     * When try to create a field in a Content Type with a right layout
     * Should create the field in the right sortOrder
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws JSONException
     */
    @Test
    public void shouldCreateFieldWithMoveEndPoint () throws DotSecurityException, DotDataException, JSONException {
        final  ContentType type = createContentType();

        final List<Field> fields = createFields(type);
        final Field newField = FieldBuilder.builder(TextField.class)
                .name("new field")
                .sortOrder(0)
                .contentTypeId(type.id())
                .build();
        fields.add(2, newField);

        final List<Map<String, Object>> layout = getToLayoutMap(fields);

        final MoveFieldsForm form =
                new MoveFieldsForm.Builder().layout(layout)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        final Response response = fieldResource.moveFields(type.id(), form, getHttpRequest());

        final List<FieldLayoutRow> responseRows =
                (List<FieldLayoutRow>) ((ResponseEntityView) response.getEntity()).getEntity();

        final List<Field> responseFields = this.getFields(responseRows);
        checkAllFieldsIds(fields, responseFields);

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        checkAllFieldsIds(fields, contentTypeFromDB.fields());

        assertNotNull(responseFields.get(2).id());
    }

    /**
     * When try to create a field in a Content Type with a right layout
     * Should create the field and the relationship.
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws JSONException
     */
    @Test
    public void shouldCreateFieldAndRelationshipWithMoveEndPoint () throws DotSecurityException, DotDataException, JSONException {
        final  ContentType type = createContentType();

        final List<Field> fields = createFields(type);
        final Field relationshipField = FieldBuilder.builder(RelationshipField.class)
                .name("relationshipField")
                .contentTypeId(type.id())
                .values(String.valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()))
                .relationType(type.variable()).build();
        fields.add(2, relationshipField);

        final List<Map<String, Object>> layout = getToLayoutMap(fields);

        final MoveFieldsForm form =
                new MoveFieldsForm.Builder().layout(layout)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        final Response response = fieldResource.moveFields(type.id(), form, getHttpRequest());

        final List<FieldLayoutRow> responseRows =
                (List<FieldLayoutRow>) ((ResponseEntityView) response.getEntity()).getEntity();

        final List<Field> responseFields = this.getFields(responseRows);
        checkAllFieldsIds(fields, responseFields);

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        checkAllFieldsIds(fields, contentTypeFromDB.fields());

        assertNotNull(responseFields.get(2).id());

        final List<Relationship> relationshipList =  APILocator.getRelationshipAPI().byContentType(type);
        assertNotNull(relationshipList);
        assertFalse(relationshipList.isEmpty());

    }

    /**
     * When try to move fields in a Content Type with a wrong layout
     * Should move the fields and fix the layout
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws JSONException
     */
    @Test
    public void shouldMoveAndFixLegacyLayout () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();
        final List<Field> fields = createLegacyLayout(type);

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.getContentTypeFields(type.id(), getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        final List<Map<String, Object>> layout = getToLayoutMap(getFields(rows));
        final Map<String, Object> column = ((List<Map<String, Object>>) layout.get(0).get("columns")).get(0);
        final List<Map<String, Object>> fieldsMap = (List<Map<String, Object>>) column.get("fields");
        column.put("fields", list(fieldsMap.get(1), fieldsMap.get(0)));

        final MoveFieldsForm form =
                new MoveFieldsForm.Builder().layout(layout)
                        .build();

        final Response response = fieldResource.moveFields(type.id(), form, getHttpRequest());
        final List<FieldLayoutRow> responseRows = (List<FieldLayoutRow>) ((ResponseEntityView) response.getEntity()).getEntity();

        final List<Field> responseFields = this.getFields(responseRows);
        assertEquals(4, responseFields.size());
        assertEquals(responseFields.get(0).getClass(), com.dotcms.contenttype.model.field.ImmutableRowField.class);
        assertEquals(responseFields.get(1).getClass(), com.dotcms.contenttype.model.field.ImmutableColumnField.class);
        assertEquals(responseFields.get(2).id(), fields.get(1).id());
        assertEquals(responseFields.get(3).id(), fields.get(0).id());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        assertEquals(4, contentTypeFromDB.fields().size());
        assertEquals(contentTypeFromDB.fields().get(0).getClass(), com.dotcms.contenttype.model.field.ImmutableRowField.class);
        assertEquals(contentTypeFromDB.fields().get(1).getClass(), com.dotcms.contenttype.model.field.ImmutableColumnField.class);
        assertEquals(contentTypeFromDB.fields().get(2).id(), fields.get(1).id());
        assertEquals(contentTypeFromDB.fields().get(3).id(), fields.get(0).id());
    }

    /**
     * When try to move fields in a Content Type with a wrong layout
     * Should move the fields and fix the layout
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws JSONException
     */
    @Test
    public void shouldMoveAndFixMaxColumnsInRowRules () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();
        final List<Field> fields = createLayoutWithManyColumns(type);

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.getContentTypeFields(type.id(), getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        final List<Map<String, Object>> layout = getToLayoutMap(getFields(rows));

        final Map<String, Object> column = ((List<Map<String, Object>>) layout.get(0).get("columns")).get(3);
        final List<Map<String, Object>> fieldsMap = (List<Map<String, Object>>) column.get("fields");
        column.put("fields", list(fieldsMap.get(1), fieldsMap.get(0)));

        final MoveFieldsForm form =
                new MoveFieldsForm.Builder().layout(layout)
                        .build();

        final Response response = fieldResource.moveFields(type.id(), form, getHttpRequest());
        final List<FieldLayoutRow> responseRows = (List<FieldLayoutRow>) ((ResponseEntityView) response.getEntity()).getEntity();

        final List<Field> responseFields = this.getFields(responseRows);
        assertEquals(9, responseFields.size());
        assertEquals(responseFields.get(0).getClass(), ImmutableRowField.class);
        assertEquals(responseFields.get(1).getClass(), ImmutableColumnField.class);
        assertEquals(responseFields.get(2).getClass(), ImmutableColumnField.class);
        assertEquals(responseFields.get(3).id(), fields.get(3).id());
        assertEquals(responseFields.get(4).getClass(), ImmutableColumnField.class);
        assertEquals(responseFields.get(5).id(), fields.get(5).id());
        assertEquals(responseFields.get(6).getClass(), ImmutableColumnField.class);
        assertEquals(responseFields.get(7).id(), fields.get(10).id());
        assertEquals(responseFields.get(8).id(), fields.get(8).id());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        assertEquals(9, responseFields.size());
        assertEquals(contentTypeFromDB.fields().get(0).getClass(), ImmutableRowField.class);
        assertEquals(contentTypeFromDB.fields().get(1).getClass(), ImmutableColumnField.class);
        assertEquals(contentTypeFromDB.fields().get(2).getClass(), ImmutableColumnField.class);
        assertEquals(contentTypeFromDB.fields().get(3).id(), fields.get(3).id());
        assertEquals(contentTypeFromDB.fields().get(4).getClass(), ImmutableColumnField.class);
        assertEquals(contentTypeFromDB.fields().get(5).id(), fields.get(5).id());
        assertEquals(contentTypeFromDB.fields().get(6).getClass(), ImmutableColumnField.class);
        assertEquals(contentTypeFromDB.fields().get(7).id(), fields.get(10).id());
        assertEquals(contentTypeFromDB.fields() .get(8).id(), fields.get(8).id());
    }

    /**
     * When try to create a new tab divider with the move end point in a Content Type with a right layout
     * Should create it
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws JSONException
     */
    @Test
    public void shouldCreateTabDividerWithMoveEndPoint () throws DotSecurityException, DotDataException, JSONException {
        final  ContentType type = createContentType();

        final List<Field> fields = createFields(type);
        final Field newField = createTabField(type, "tab_1", 0);
        fields.add(newField);

        final List<Map<String, Object>> layout = getToLayoutMap(fields);

        final MoveFieldsForm form =
                new MoveFieldsForm.Builder().layout(layout)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.moveFields(type.id(), form, getHttpRequest());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        final int nFields = contentTypeFromDB.fields().size();

        assertEquals(contentTypeFromDB.fields().get(nFields - 1).name(), newField.name());
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
    public void shouldThrowExceptionWhenMoveFieldWithMaxColumnsRule () throws DotSecurityException, DotDataException, JSONException {
        final ContentType type = createContentType();

        final List<Field> fields = createLayoutWithMultiRow(type);
        final List<Map<String, Object>> layout = list(
                Map.of("divider", getMap(fields.get(0)), "columns", list(
                        Map.of("columnDivider", getMap(fields.get(1)), "fields", list()),
                        Map.of("columnDivider", getMap(fields.get(2)), "fields", list(getMap(fields.get(3)))),
                        Map.of("columnDivider", getMap(fields.get(4)), "fields", list(getMap(fields.get(5)))),
                        Map.of("columnDivider", getMap(fields.get(6)), "fields", list()),
                        Map.of("columnDivider", getMap(fields.get(12)), "fields", list())
                )),
                Map.of("divider", getMap(fields.get(7)), "columns", list(
                        Map.of("columnDivider", getMap(fields.get(8)), "fields", list(getMap(fields.get(9)))),
                        Map.of("columnDivider", getMap(fields.get(10)), "fields", list(getMap(fields.get(11))))
                ))
        );

        final MoveFieldsForm form =
                new MoveFieldsForm.Builder().layout(layout)
                        .build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.moveFields(type.id(), form, getHttpRequest());
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

    @Test
    public void shouldDeleteFieldsInContentTypesWithWrongLayout () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();

        final List<Field> fields = createLegacyLayout(type);

        final DeleteFieldsForm form =
                new DeleteFieldsForm.Builder().fieldsID(list(fields.get(1).id())).build();
        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.deleteFields(type.id(), form, getHttpRequest());

        final Map<String, Object> responseMap = (Map<String, Object>)
                ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();
        final List<FieldLayoutRow> rows = (List<FieldLayoutRow>) responseMap.get("fields");

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getColumns().size());
        assertEquals(1, rows.get(0).getColumns().get(0).getFields().size());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        assertEquals(contentTypeFromDB.fields().size(), 3);
        assertEquals(ImmutableRowField.class, contentTypeFromDB.fields().get(0).getClass());
        assertEquals(ImmutableColumnField.class, contentTypeFromDB.fields().get(1).getClass());
        assertEquals(fields.get(0).id(), contentTypeFromDB.fields().get(2).id());
    }

    @Test
    public void shouldDeleteFieldsAndFixMaxColumnsPerRowRules () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();

        final List<Field> fields = createLayoutWithManyColumns(type);

        final DeleteFieldsForm form =
                new DeleteFieldsForm.Builder().fieldsID(list(fields.get(3).id())).build();
        final FieldResource fieldResource = new FieldResource();
        fieldResource.deleteFields(type.id(), form, getHttpRequest());

        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(APILocator.systemUser()).find(type.id());
        assertEquals(contentTypeFromDB.fields().size(), 8);
        assertEquals(contentTypeFromDB.fields().get(0).getClass(), ImmutableRowField.class);
        assertEquals(contentTypeFromDB.fields().get(1).getClass(), ImmutableColumnField.class);
        assertEquals(contentTypeFromDB.fields().get(2).getClass(), ImmutableColumnField.class);
        assertEquals(contentTypeFromDB.fields().get(3).getClass(), ImmutableColumnField.class);

        assertEquals(contentTypeFromDB.fields().get(4).id(), fields.get(5).id());
        assertEquals(contentTypeFromDB.fields().get(5).getClass(), ImmutableColumnField.class);
        assertEquals(contentTypeFromDB.fields().get(6).id(), fields.get(8).id());
        assertEquals(contentTypeFromDB.fields().get(7).id(), fields.get(10).id());
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




    @Test(expected = FieldLayoutValidationException.class)
    public void shouldThrowExceptionWhenDeleteFieldWithMaxColumnsRule () throws DotSecurityException, DotDataException {
        final ContentType type = createContentType();

        final List<Field> fields = createLayoutWithMultiRow(type);

        final DeleteFieldsForm form =
                new DeleteFieldsForm.Builder().fieldsID(list(fields.get(7).id())).build();

        final FieldResource fieldResource = new FieldResource();
        fieldResource.deleteFields(type.id(), form, getHttpRequest());
    }



    private void checkAllFieldsIds(final List<Field> fields, final List<Field> otherFields) {
        assertEquals(fields.size(), otherFields.size());

        for(int i =0; i < fields.size(); i++) {
            final Field field = fields.get(i);

            if (field.id() != null) {
                assertEquals(field.id(), otherFields.get(i).id());
            } else {
                assertEquals(field.name(), otherFields.get(i).name());
            }
        }
    }

    private ContentType createContentType() throws DotDataException, DotSecurityException {
        final String typeName = "fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);
        return type;
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

            rowMap.put("columns", columnsMap.isEmpty() ? null : columnsMap);
            result.add(rowMap);
        }

        return result;
    }

    private Map getMap(final Field field) {
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(field);
        return jsonFieldTransformer.mapObject();
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

    private Field createTabField(final ContentType type, final String name, final int sortOrder) {
        final Field field =  FieldBuilder.builder(TabDividerField.class)
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
                        new MockSessionRequest(new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                ).request()
        );

        request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

        return request;
    }
}
