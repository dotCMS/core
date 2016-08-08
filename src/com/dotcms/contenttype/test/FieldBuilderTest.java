package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableCheckboxField;
import com.dotcms.contenttype.model.field.ImmutableHiddenField;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.contenttype.model.field.TextField;
import com.dotmarketing.business.DotStateException;

public class FieldBuilderTest {

	FieldFactory factory = new FieldFactoryImpl();
	

	@Test
	public void testEquals() throws Exception {
		Field f1 = FieldBuilder.instanceOf(TextField.class);
		Field f2 = FieldBuilder.instanceOf(TextField.class);
		assertThat("fieldbuilder works ",f1.equals(f2));
	}
	
	@Test
	public void testAllFieldBuilders() throws Exception {
		for(LegacyFieldTypes types : LegacyFieldTypes.values()){
			FieldBuilder.instanceOf(types.implClass());
			FieldBuilder.builder(types.implClass()).inode("asd");
			
		}
	}
	
	
	
	@Test
	public void testCopy() throws Exception {
		
		Field test = ImmutableHiddenField.builder()
				.name("Form Title")
				.dataType(DataTypes.CONSTANT)
				.variable("formTitle")
				.required(true)
				.listed(true)
				.indexed(true)
				.sortOrder(1)
				.contentTypeId("test")
				.fixed(true)
				.searchable(true)
				.build();
		
		
		Field test2 = FieldBuilder.builder(test).build();
		assertThat("fieldbuilder works ",test.equals(test2));
		
	}
	
	/**
	 * tests if the list of values provided for a user to select
	 * is valid for the datatype
	 * @throws Exception
	 */
	@Test
	public void testValuesCheck() throws Exception {

		try{
			Field test = ImmutableCheckboxField.builder()
				.name("checkbox")
				.dataType(DataTypes.INTEGER)
				.variable("checkbox")
				.required(true)
				.listed(true)
				.indexed(true)
				.sortOrder(1)
				.fixed(true)
				.values("asdsa|asdasddas\r\nxxxxx|xx")

				.contentTypeId("test")
				.build();
			throw new Exception("field value check not working");
		}
		catch(DotStateException t){
			// we should be here. the field above is in a invalid state
		}
		try{
			Field test = ImmutableCheckboxField.builder()
				.name("checkbox")
				.dataType(DataTypes.FLOAT)
				.variable("checkbox")
				.required(true)
				.listed(true)
				.indexed(true)
				.sortOrder(1)
				.fixed(true)
				.values("asdsa|asdasddas\r\nxxxxx|xx")

				.contentTypeId("test")
				.build();
			throw new Exception("float field value check not working");
		}
		catch(DotStateException t){
			// we should be here. the field above is in a invalid state
		}
		
		try{
			Field test = ImmutableCheckboxField.builder()
				.name("checkbox")
				.dataType(DataTypes.BOOL)
				.variable("checkbox")
				.required(true)
				.listed(true)
				.indexed(true)
				.sortOrder(1)
				.fixed(true)
				.values("asdsa|asdasddas\r\nxxxxx|xx")

				.contentTypeId("test")
				.build();
			throw new Exception("boolean field value check not working");
		}
		catch(DotStateException t){
			// we should be here. the field above is in a invalid state
		}
	}
	
	
	
	
}