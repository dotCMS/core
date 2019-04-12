package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.model.field.*;
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
import com.dotcms.repackage.javax.ws.rs.BadRequestException;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.internal.util.Base64;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;

public class FieldResourceTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void shouldFixContentTypesFieldsBeforeReturn () throws DotSecurityException, DotDataException {
        final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).variable(typeName).build();
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

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).variable(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);

        final DeleteFieldsForm form =
                new DeleteFieldsForm.Builder().fieldsID(list(fields.get(2).id(), fields.get(3).id())).build();
        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.deleteFields(type.id(), form, getHttpRequest());

        final List<FieldLayoutRow> rows =
                (List<FieldLayoutRow>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getColumns().size());
        assertEquals(0, rows.get(0).getColumns().get(0).getFields().size());

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

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).variable(typeName).build();
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

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).variable(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);

        final Field fieldToUpdate = fields.get(3);
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldToUpdate);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();
        fieldToUpdatetMap.put("sortOrder", 2);

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().fields(list(fieldToUpdatetMap))
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

    @Test(expected = FieldLayoutValidationException.class)
    public void shouldUpdateFieldsANdThrowException () throws DotSecurityException, DotDataException, JSONException {
        final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).variable(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final List<Field> fields = createFields(type);

        final Field fieldToUpdate = fields.get(0);
        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldToUpdate);
        final Map<String, Object> fieldToUpdatetMap = jsonFieldTransformer.mapObject();
        fieldToUpdatetMap.put("sortOrder", 4);

        final UpdateFieldForm form =
                new UpdateFieldForm.Builder().fields(list(fieldToUpdatetMap))
                        .build();
        final FieldResource fieldResource = new FieldResource();
        fieldResource.updateFields(type.id(), form, getHttpRequest());
    }

    private List<Field> createFields(ContentType type) throws DotDataException, DotSecurityException {
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
        MockHeaderRequest request = new MockHeaderRequest(
                (
                        new MockSessionRequest(new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                ).request()
        );

        request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

        return request;
    }
}
