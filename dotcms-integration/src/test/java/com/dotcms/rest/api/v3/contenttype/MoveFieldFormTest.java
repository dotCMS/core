package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

        final Map<String, Object> rowField = new HashMap<>();
        rowField.put("clazz" , "com.dotcms.contenttype.model.field.ImmutableRowField");
        rowField.put("contentTypeId", contentTypeId);
        rowField.put("dataType" , "SYSTEM");
        rowField.put("fieldContentTypeProperties", Collections.emptyList());
        rowField.put("fieldType", "Row");
        rowField.put("fieldTypeLabel", "Row");
        rowField.put("fieldVariables", Collections.emptyList());
        rowField.put("fixed", Boolean.FALSE);
        rowField.put("iDate", Long.valueOf(1584647620000l));
        rowField.put("id","a2d05a0b-5f4d-449d-9ca2-1117ab1ab086");
        rowField.put("indexed",  Boolean.FALSE);
        rowField.put("listed", Boolean.FALSE);
        rowField.put("modDate", Long.valueOf(1584647620000l));
        rowField.put("name", "fields-0");
        rowField.put("readOnly",Boolean.FALSE);
        rowField.put("required", Boolean.FALSE);
        rowField.put("searchable", Boolean.FALSE);
        rowField.put("sortOrder", Integer.valueOf(0));
        rowField.put("unique", Boolean.FALSE);
        rowField.put("variable", "fields0");

        final Map<String, Object> columnField = new HashMap<>();
        columnField.put("clazz" , "com.dotcms.contenttype.model.field.ImmutableColumnField");
        columnField.put("contentTypeId", contentTypeId);
        columnField.put("dataType" , "SYSTEM");
        columnField.put("fieldContentTypeProperties", Collections.emptyList());
        columnField.put("fieldType", "Column");
        columnField.put("fieldTypeLabel", "Column");
        columnField.put("fieldVariables", Collections.emptyList());
        columnField.put("fixed", Boolean.FALSE);
        columnField.put("iDate", Long.valueOf(1584647620000l));
        columnField.put("id","a2d05a0b-5f4d-449d-9ca2-1117ab1ab086");
        columnField.put("indexed",  Boolean.FALSE);
        columnField.put("listed", Boolean.FALSE);
        columnField.put("modDate", Long.valueOf(1584647620000l));
        columnField.put("name", "fields-1");
        columnField.put("readOnly",Boolean.FALSE);
        columnField.put("required", Boolean.FALSE);
        columnField.put("searchable", Boolean.FALSE);
        columnField.put("sortOrder", Integer.valueOf(0));
        columnField.put("unique", Boolean.FALSE);
        columnField.put("variable", "fields1");

        final Map<String, Object> textField = new HashMap<>();
        textField.put("clazz" , "com.dotcms.contenttype.model.field.ImmutableTextField");
        textField.put("name", "text");
        textField.put("dataType" , "TEXT");
        textField.put("regexCheck", "");
        textField.put("defaultValue", "");
        textField.put("hint", "");
        textField.put("required", Boolean.FALSE);
        textField.put("searchable", Boolean.FALSE);
        textField.put("indexed", Boolean.FALSE);
        textField.put("listed", Boolean.FALSE);
        textField.put("unique", Boolean.FALSE);

        final Map<String, Object> textDecimalField = new HashMap<>();
        textDecimalField.put("clazz" , "com.dotcms.contenttype.model.field.ImmutableTextField");
        textDecimalField.put("name", "decimal");
        textDecimalField.put("dataType" , "FLOAT");
        textDecimalField.put("regexCheck", "");
        textDecimalField.put("defaultValue", "");
        textDecimalField.put("hint", "");
        textDecimalField.put("required", Boolean.FALSE);
        textDecimalField.put("searchable", Boolean.FALSE);
        textDecimalField.put("indexed", Boolean.FALSE);
        textDecimalField.put("listed", Boolean.FALSE);
        textDecimalField.put("unique", Boolean.FALSE);

        final Map<String, Object> textNumberField = new HashMap<>();
        textNumberField.put("clazz" , "com.dotcms.contenttype.model.field.ImmutableTextField");
        textNumberField.put("name", "number");
        textNumberField.put("dataType" , "INTEGER");
        textNumberField.put("regexCheck", "");
        textNumberField.put("defaultValue", "");
        textNumberField.put("hint", "");
        textNumberField.put("required", Boolean.FALSE);
        textNumberField.put("searchable", Boolean.FALSE);
        textNumberField.put("indexed", Boolean.FALSE);
        textNumberField.put("listed", Boolean.FALSE);
        textNumberField.put("unique", Boolean.FALSE);

        fieldList.add(rowField);
        fieldList.add(columnField);
        fieldList.add(textField);
        fieldList.add(textDecimalField);
        fieldList.add(textNumberField);

        return fieldList;
    }
}
