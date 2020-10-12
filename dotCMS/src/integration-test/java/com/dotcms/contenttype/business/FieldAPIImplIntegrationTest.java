package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;

public class FieldAPIImplIntegrationTest {

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * when: A content type has fields with wrong sort_order and save multiple fields to fix the sort order and move a field
     * Should: save all of them with the right sort order value
     * Method to test: {@link FieldAPIImpl#saveFields(List, User)}
     */
    @Test
    public void shouldSaveFields() throws DotDataException, DotSecurityException {

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final long current = System.currentTimeMillis();

        final Field aliases = new FieldDataGen()
                .name("Aliases")
                .sortOrder(1)
                .contentTypeId(contentType.id())
                .velocityVarName("Aliases" + current)
                .nextPersisted();

        final Field row = new FieldDataGen()
                .name("fields-0")
                .type(RowField.class)
                .sortOrder(1)
                .contentTypeId(contentType.id())
                .velocityVarName("fields0" + current)
                .nextPersisted();

        final Field column = new FieldDataGen()
                .name("fields-1").
                        type(ColumnField.class)
                .sortOrder(1)
                .contentTypeId(contentType.id())
                .velocityVarName("fields1" + current)
                .nextPersisted();

        final Field hostName = new FieldDataGen()
                .name("Host Name")
                .sortOrder(1)
                .contentTypeId(contentType.id())
                .velocityVarName("hostname" + current)
                .nextPersisted();

        final Field tagStorage = new FieldDataGen()
                .name("Tag Storage")
                .sortOrder(2)
                .contentTypeId(contentType.id())
                .velocityVarName("tagstorage" + current)
                .nextPersisted();

        final Field isDefault = new FieldDataGen()
                .name("Is Default")
                .sortOrder(3)
                .contentTypeId(contentType.id())
                .velocityVarName("default" + current)
                .nextPersisted();

        //
        final Field newRow_1 = new FieldDataGen()
                .name("fields-2")
                .type(RowField.class)
                .sortOrder(0)
                .contentTypeId(contentType.id())
                .velocityVarName("field2" + current)
                .next();

        final Field newColumn_1 = new FieldDataGen()
                .name("fields-3")
                .type(ColumnField.class)
                .sortOrder(1)
                .contentTypeId(contentType.id())
                .velocityVarName("field3" + current)
                .next();

        final Field newRow_2 = new FieldDataGen()
                .id(row.id())
                .name("fields-0")
                .type(RowField.class)
                .sortOrder(2)
                .contentTypeId(contentType.id())
                .velocityVarName(row.variable())
                .next();

        final Field newColumn_2 = new FieldDataGen()
                .id(column.id())
                .name("fields-1")
                .type(ColumnField.class)
                .sortOrder(3)
                .contentTypeId(contentType.id())
                .velocityVarName(column.variable())
                .next();

        final Field newAliases = new FieldDataGen()
                .id(aliases.id())
                .name("Aliases")
                .sortOrder(4)
                .contentTypeId(contentType.id())
                .velocityVarName(aliases.variable())
                .next();

        final Field newHostName = new FieldDataGen()
                .id(hostName.id())
                .name("Host Name")
                .sortOrder(5)
                .contentTypeId(contentType.id())
                .velocityVarName(hostName.variable())
                .next();

        final Field newTagStorage = new FieldDataGen()
                .id(tagStorage.id())
                .name("Tag Storage")
                .sortOrder(6)
                .contentTypeId(contentType.id())
                .velocityVarName(tagStorage.variable())
                .next();

        final Field newIsDefault = new FieldDataGen()
                .id(isDefault.id())
                .name("Is Default")
                .sortOrder(7)
                .contentTypeId(contentType.id())
                .velocityVarName(isDefault.variable())
                .next();

        final List<Field> newFieldList = list(
                newRow_1, newColumn_1, newRow_2, newColumn_2, newAliases, newHostName, newTagStorage, newIsDefault);
        APILocator.getContentTypeFieldAPI().saveFields(newFieldList, APILocator.systemUser());

        final List<Field> fields = APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentType.id()).fields();

        assertEquals(8, fields.size());

        final List<String> expectedNames = newFieldList.stream().map(field -> field.name()).collect(Collectors.toList());

        for (int i = 0; i < expectedNames.size(); i++) {
            assertEquals(expectedNames.get(i), fields.get(i).name());
            assertEquals(i, fields.get(i).sortOrder());
        }

    }
}