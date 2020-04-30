package com.dotcms.contenttype.test;

import static org.junit.Assert.assertEquals;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.TestDataUtils;
import io.vavr.control.Try;

public class ContentTypeTest extends ContentTypeBaseTest {


    
    
	@Test
	public void test_that_fields_of_type_are_returned() throws Exception {
	    ContentType type = TestDataUtils.getFormLikeContentType();
	    
	    assert(!type.fields().isEmpty());
	    assert(type.fields().size()==type.fieldMap().size());
	    assert(!type.fields(ConstantField.class).isEmpty());
	    assert(type.fields(ConstantField.class).size()==2);

	}

	
    @Test
    public void test_that_immutable_fields_work_the_same_as_normal_fields_returned() throws Exception {
        
        ContentType type = TestDataUtils.getFormLikeContentType();
        
        assert(!type.fields().isEmpty());
        assert(type.fields().size()==type.fieldMap().size());
        
        List<Field> constants = type.fields(ConstantField.class);
        List<Field> immutableConstants = type.fields(ImmutableConstantField.class);
        assert(!constants.isEmpty());
        assert(!immutableConstants.isEmpty());

        
        
        // make sure that getting the field by Type or by its immutable type works the same
        assert(constants.size() == immutableConstants.size());
        assertEquals(constants,immutableConstants);
        
    }
	
    
    @Test
    public void test_that_it_works_like_the_old_way() throws Exception {
        ContentType type = TestDataUtils.getFormLikeContentType();
        
        assert(!type.fields().isEmpty());
        assert(type.fields().size()==type.fieldMap().size());
        assert(!type.fields(ConstantField.class).isEmpty());
        
        
        List<Field> constantsNewWay = type.fields(ConstantField.class);
        
        List<Field> constantsOldWay = oldWayOfGettingFields(type, ConstantField.class);
        
        
        // make sure that getting the field by Type or by its immutable type works the same
        assert(constantsNewWay.size() >0);
        assert(constantsOldWay.size() >0);
        assertEquals(constantsNewWay,constantsOldWay);
        
        
        
    }
	
	
	
	

	  private List<Field> oldWayOfGettingFields(ContentType type, final Class<? extends Field> clazz) {
	    return type.fields()
	    .stream()
	    .filter(field -> Try.of(()->field.getClass().asSubclass(clazz)!=null).getOrElse(false))
	    .collect(Collectors.toList());
	  }
}