package com.dotcms.contenttype.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import org.junit.Test;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.TestDataUtils;
import io.vavr.control.Try;

public class ContentTypeTest extends ContentTypeBaseTest {

    /**
     * This tests that calling type.fields() returns valid fields and that calling type.fields(Class
     * clazz) returns a subset of those fields
     *
     * @throws Exception
     */

    @Test
    public void test_content_type_living_in_system_host() throws Exception {

        final String name = "test"+ System.currentTimeMillis();
        ContentType type = new ContentTypeDataGen()
                .baseContentType(BaseContentType.FORM)
                .name(name)
                .velocityVarName(name)
                .host(APILocator.systemHost())
                .nextPersisted();

       try { type.folderPath(); } catch (Exception e) {
           fail("This should not throw exception");
       }
    }

    /**
     * This tests that calling type.fields() returns valid fields and that calling type.fields(Class
     * clazz) returns a subset of those fields
     * 
     * @throws Exception
     */

    @Test
    public void test_that_fields_of_type_are_returned() throws Exception {
        ContentType type = TestDataUtils.getFormLikeContentType();

        assert (!type.fields().isEmpty());
        assert (type.fields().size() == type.fieldMap().size());
        assert (!type.fields(ConstantField.class).isEmpty());
        assert (type.fields(ConstantField.class).size() == 2);

    }

    /**
     * This tests that calling type.fields(TextField.class) returns the same subset of fields as does
     * calling type.fields(ImmutableTextField.class)
     * 
     * @throws Exception
     */

    @Test
    public void test_that_immutable_fields_work_the_same_as_normal_fields_returned() throws Exception {

        ContentType type = TestDataUtils.getFormLikeContentType();

        assert (!type.fields().isEmpty());
        assert (type.fields().size() == type.fieldMap().size());

        List<Field> constants = type.fields(ConstantField.class);
        List<Field> immutableConstants = type.fields(ImmutableConstantField.class);
        assert (!constants.isEmpty());
        assert (!immutableConstants.isEmpty());



        // make sure that getting the field by Type or by its immutable type works the same
        assert (constants.size() == immutableConstants.size());
        assertEquals(constants, immutableConstants);

    }



    /**
     * This tests that the changing of the code in the ContentType to return fields works the same as
     * the old way we used to do it
     * 
     * @throws Exception
     */

    @Test
    public void test_that_it_works_like_the_old_way() throws Exception {
        ContentType type = TestDataUtils.getWidgetLikeContentType();

        assert (!type.fields().isEmpty());
        assert (type.fields().size() == type.fieldMap().size());
        assert (!type.fields(ConstantField.class).isEmpty());


        List<Field> newWayfields = type.fields(ConstantField.class);

        List<Field> oldWayFields = oldWayOfGettingFields(type, ConstantField.class);


        // make sure that getting the field by Type or by its immutable type works the same
        assert (oldWayFields.size() > 0);
        assert (newWayfields.size() > 0);
        assertEquals(oldWayFields, newWayfields);


        newWayfields = type.fields(TextField.class);

        oldWayFields = oldWayOfGettingFields(type, TextField.class);


        // make sure that getting the field by Type or by its immutable type works the same
        assert (oldWayFields.size() > 0);
        assert (newWayfields.size() > 0);
        assertEquals(newWayfields, oldWayFields);
        
        
        // test that Immutables work the same way
        oldWayFields = oldWayOfGettingFields(type, ImmutableTextField.class);
        assertEquals(newWayfields, oldWayFields);
        
        

    }



    private List<Field> oldWayOfGettingFields(ContentType type, final Class<? extends Field> clazz) {
        return type.fields()
                .stream()
                .filter(field -> Try.of(() -> field.getClass().asSubclass(clazz) != null).getOrElse(false))
                .collect(Collectors.toList());
    }
}
