package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Date;

import org.junit.Test;

import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableCheckboxField;
import com.dotcms.contenttype.model.field.ImmutableHiddenField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableMultiSelectField;
import com.dotcms.contenttype.model.field.ImmutableRadioField;
import com.dotcms.contenttype.model.field.ImmutableSelectField;
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
	public void testFieldChecks() throws Exception {
		Field f1 = null;
		try{
			 f1 = ImmutableHostFolderField.builder()
					.indexed(false)
					.dataType(DataTypes.BINARY)
					.name("asdsad").variable("asdasd").build();

		}
		catch(IllegalArgumentException e){
			assertThat("fieldChecks work:" + e.getMessage() ,true);
		}
		catch(Throwable e){
			assertThat("fieldChecks do not work:" + e.getMessage() ,false);
		}
		
		try{
			f1 = ImmutableHostFolderField.builder()
					.iDate(new Date(0L))
					.indexed(false)
					.dataType(DataTypes.BINARY)
					.name("asdsad").variable("asdasd").build();
			assertThat("fieldChecks should not be called for old fields" ,f1!=null);
		}
		catch(IllegalArgumentException e){
			assertThat("fieldChecks should not be called for old fields:" + e.getMessage() ,false);
		}
		catch(Throwable e){
			assertThat("fieldChecks do not work:" + e.getMessage() ,false);
		}
		
		
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

		String testVal = "asdsa|asdasddas\r\nxxxxx|xx";
		try{
			Field test = ImmutableSelectField.builder()
				.name("select")
				.dataType(DataTypes.INTEGER)
				.variable("select")
				.required(true)
				.listed(true)
				.indexed(true)
				.sortOrder(1)
				.fixed(true)
				.values(testVal)
				.contentTypeId("test")
				.build();
			throw new Exception("field value check not working");
		}
		catch(DotStateException t){
			// we should be here. the field above is in a invalid state
		}
		try{
			Field test = ImmutableRadioField.builder()
				.name("select")
				.dataType(DataTypes.FLOAT)
				.variable("select")
				.required(true)
				.listed(true)
				.indexed(true)
				.sortOrder(1)
				.fixed(true)
				.values(testVal)


				.contentTypeId("test")
				.build();
			throw new Exception("float field value check not working");
		}
		catch(DotStateException t){
			// we should be here. the field above is in a invalid state
		}
		
		try{
			Field test = ImmutableRadioField.builder()
				.name("checkbox")
				.dataType(DataTypes.BOOL)
				.variable("checkbox")
				.required(true)
				.listed(true)
				.indexed(true)
				.sortOrder(1)
				.values(testVal)

				.contentTypeId("test")
				.build();
			throw new Exception("boolean field value check not working");
		}
		catch(DotStateException t){
			// we should be here. the field above is in a invalid state
		}
	}
	
	
	
	
}