package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MoveFieldFormTest {

    private ContentType createContentType() throws DotDataException, DotSecurityException {
        final String typeName = "fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);
        return type;
    }

    /**
     * This test creates a collection of fields, 2 system ones and 3 text fields: one text data type, one float and one integer
     * The idea is to make sure the data type is deserialized in the right way
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testGetRows_givenFieldsinJSON_ShouldDeserializeRespectingOrderAndDatatypes() throws DotSecurityException, DotDataException {

        final ContentType type = createContentType();
        final List<Map<String, Object>> fieldList = createTestFields (type.id());
        final MoveFieldsForm form     = new MoveFieldsForm(fieldList);

        final FieldLayout fieldLayout = form.getRows(type);

        assertNotNull("Field layour must be not null", fieldLayout);
        assertNotNull("Get field must be not null", fieldLayout.getFields());
        assertEquals("Field Layout must retrieve 5 fieldds",5, fieldLayout.getFields().size());
        assertTrue("The first field on the list must be a RowField", fieldLayout.getFields().get(0) instanceof RowField);
        assertEquals("The first field data type must be a DataTypes.SYSTEM", RowField.class.cast(fieldLayout.getFields().get(0)).dataType(), DataTypes.SYSTEM);
        assertEquals("The first field name must be fields-0", "fields-0", RowField.class.cast(fieldLayout.getFields().get(0)).name());

        assertTrue("The second field on the list must be a ColumnField", fieldLayout.getFields().get(1) instanceof ColumnField);
        assertEquals("The second field data type must be a DataTypes.SYSTEM", ColumnField.class.cast(fieldLayout.getFields().get(1)).dataType(), DataTypes.SYSTEM);
        assertEquals("The second field name must be fields-1", "fields-1", ColumnField.class.cast(fieldLayout.getFields().get(1)).name());

        assertTrue("The third field on the list must be a TextField", fieldLayout.getFields().get(2) instanceof TextField);
        assertEquals("The third field data type must be a DataTypes.TEXT", TextField.class.cast(fieldLayout.getFields().get(2)).dataType(), DataTypes.TEXT);
        assertEquals("The third field name must be text","text", TextField.class.cast(fieldLayout.getFields().get(2)).name());

        assertTrue("The four field on the list must be a TextField", fieldLayout.getFields().get(3) instanceof TextField);
        assertEquals("The four field data type must be a DataTypes.FLOAT", TextField.class.cast(fieldLayout.getFields().get(3)).dataType(), DataTypes.FLOAT);
        assertEquals("The four field name must be text", "decimal", TextField.class.cast(fieldLayout.getFields().get(3)).name());

        assertTrue("The five field on the list must be a TextField", fieldLayout.getFields().get(4) instanceof TextField);
        assertEquals("The five field data type must be a DataTypes.INTEGER",TextField.class.cast(fieldLayout.getFields().get(4)).dataType(), DataTypes.INTEGER);
        assertEquals("The four field name must be number", "number", TextField.class.cast(fieldLayout.getFields().get(4)).name());

    }

    final List<Map<String, Object>> createTestFields (final String contentTypeId) {

        final List<Map<String, Object>> fieldList = new ArrayList<>();
        final Map<String, Object> rowField = CollectionsUtils.map("clazz" , "com.dotcms.contenttype.model.field.ImmutableRowField",
                "contentTypeId", contentTypeId,
                "dataType" , "SYSTEM",
                "fieldContentTypeProperties", Collections.emptyList(),
                "fieldType", "Row",
                "fieldTypeLabel", "Row",
                "fieldVariables", Collections.emptyList(),
                "fixed", Boolean.FALSE,
                "iDate", new Long(1584647620000l),
                "id","a2d05a0b-5f4d-449d-9ca2-1117ab1ab086",
                "indexed",  Boolean.FALSE,
                "listed", Boolean.FALSE,
                "modDate", new Long(1584647620000l),
                "name", "fields-0",
                "readOnly",Boolean.FALSE,
                "required", Boolean.FALSE,
                "searchable", Boolean.FALSE,
                "sortOrder", new Integer(0),
                "unique", Boolean.FALSE,
                "variable", "fields0");

        final Map<String, Object> columnField = CollectionsUtils.map("clazz" , "com.dotcms.contenttype.model.field.ImmutableColumnField",
                "contentTypeId", contentTypeId,
                "dataType" , "SYSTEM",
                "fieldContentTypeProperties", Collections.emptyList(),
                "fieldType", "Column",
                "fieldTypeLabel", "Column",
                "fieldVariables", Collections.emptyList(),
                "fixed", Boolean.FALSE,
                "iDate", new Long(1584647620000l),
                "id","a2d05a0b-5f4d-449d-9ca2-1117ab1ab086",
                "indexed",  Boolean.FALSE,
                "listed", Boolean.FALSE,
                "modDate", new Long(1584647620000l),
                "name", "fields-1",
                "readOnly",Boolean.FALSE,
                "required", Boolean.FALSE,
                "searchable", Boolean.FALSE,
                "sortOrder", new Integer(0),
                "unique", Boolean.FALSE,
                "variable", "fields1");

        final Map<String, Object> textField = CollectionsUtils.map("clazz" , "com.dotcms.contenttype.model.field.ImmutableTextField",
                "name", "text",
                "dataType" , "TEXT",
                "regexCheck", "",
                "defaultValue", "",
                "hint", "",
                "required", Boolean.FALSE,
                "searchable", Boolean.FALSE,
                "indexed", Boolean.FALSE,
                "listed", Boolean.FALSE,
                "unique", Boolean.FALSE);

        final Map<String, Object> textDecimalField = CollectionsUtils.map("clazz" , "com.dotcms.contenttype.model.field.ImmutableTextField",
                "name", "decimal",
                "dataType" , "FLOAT",
                "regexCheck", "",
                "defaultValue", "",
                "hint", "",
                "required", Boolean.FALSE,
                "searchable", Boolean.FALSE,
                "indexed", Boolean.FALSE,
                "listed", Boolean.FALSE,
                "unique", Boolean.FALSE);

        final Map<String, Object> textNumberField = CollectionsUtils.map("clazz" , "com.dotcms.contenttype.model.field.ImmutableTextField",
                "name", "number",
                "dataType" , "INTEGER",
                "regexCheck", "",
                "defaultValue", "",
                "hint", "",
                "required", Boolean.FALSE,
                "searchable", Boolean.FALSE,
                "indexed", Boolean.FALSE,
                "listed", Boolean.FALSE,
                "unique", Boolean.FALSE);

        fieldList.add(rowField);
        fieldList.add(columnField);
        fieldList.add(textField);
        fieldList.add(textDecimalField);
        fieldList.add(textNumberField);

        return fieldList;
    }
}
