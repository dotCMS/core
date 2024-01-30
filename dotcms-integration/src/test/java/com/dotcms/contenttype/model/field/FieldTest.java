package com.dotcms.contenttype.model.field;

import static org.junit.Assert.assertEquals;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FieldVariableDataGen;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;

public class FieldTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@Link Field#field}
     * When: Create a {@link Field} with two {@link FieldVariable}
     * Should: Return the {@link FieldVariable}'s value when the method is called with the {@link FieldVariable}'s key
     */
    @Test
    public void fieldVariableValue(){
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Field field = new FieldDataGen().contentTypeId(contentType.id()).nextPersisted();

        new FieldVariableDataGen()
                .field(field)
                .key("field-variable-1")
                .value("field-value-1")
                .nextPersisted();

        new FieldVariableDataGen()
                .field(field)
                .key("field-variable-2")
                .value("field-value-2")
                .nextPersisted();

        assertEquals("field-value-1", field.fieldVariableValue("field-variable-1").get());
        assertEquals("field-value-2", field.fieldVariableValue("field-variable-2").get());
    }

}
