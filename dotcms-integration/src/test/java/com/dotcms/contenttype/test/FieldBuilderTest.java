package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;
import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableCheckboxField;
import com.dotcms.contenttype.model.field.ImmutableDateField;
import com.dotcms.contenttype.model.field.ImmutableDateTimeField;
import com.dotcms.contenttype.model.field.ImmutableHiddenField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableMultiSelectField;
import com.dotcms.contenttype.model.field.ImmutableRadioField;
import com.dotcms.contenttype.model.field.ImmutableSelectField;
import com.dotcms.contenttype.model.field.ImmutableTimeField;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotRuntimeException;

public class FieldBuilderTest extends ContentTypeBaseTest {

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
			FieldBuilder.builder(types.implClass()).id("asd");

		}
	}



	@Test
	public void testImplTypeBuilder() throws Exception {
		
		
		final String value = "testest";
		
		
		for(LegacyFieldTypes types : LegacyFieldTypes.values()){
			Class clazz = types.implClass();
			Field test = FieldBuilder.builder(clazz)				
			.name(value)
			.variable(value)
			.required(true)
			.listed(true)
			.indexed(true)
			.sortOrder(1)
			.contentTypeId(value)
			.fixed(true)
			.searchable(true)
			.values(value)
			.build();
			
			
			assertThat("value is set", value.equals(test.values()));
			assertThat("variable is set", value.equals(test.variable()));
			assertThat("contentTypeId is set", value.equals(test.contentTypeId()));
			assertThat("name is set", value.equals(test.name()));
			Field test2 = FieldBuilder.builder(test).build();
			assertThat("fieldbuilder works ",test.equals(test2));

		}
	}


	
	
	
	
	
	@Test
	public void testCopy() throws Exception {

		Field test = ImmutableHiddenField.builder()
				.name("Form Title")
				.variable("formTitle")
				.required(true)
				.listed(true)
				.indexed(true)
				.sortOrder(1)
				.contentTypeId("test")
				.fixed(true)
				.searchable(true)
				.values("testest")
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
	@Rule
	public ExpectedException thrown= ExpectedException.none();
	/**
	 * tests if the list of values provided for a user to select
	 * is valid for the datatype
	 * @throws Exception
	 */
	@Test
	public void testDateFieldException()  {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("invalid default Value");

		DateField field = ImmutableDateField.builder().contentTypeId("test").variable("teat").name("test").defaultValue("1/1/2016").build();

	}

	@Test
	public void testDateField()  {

		String[] valids = new String[]{"now","2011-01-01","2034-01-01" };
		for(String x: valids){
			DateField field = ImmutableDateField.builder().contentTypeId("test").variable("teat").name("test").defaultValue(x).build();
			assertThat("now is a valid default for date fields works ",field.defaultValue().equals(x));
		}
		DateField field = ImmutableDateField.builder().contentTypeId("test").variable("teat").name("test").defaultValue(null).build();
		assertThat("now is a valid default for date fields works ",field.defaultValue()==null);
	}
	/**
	 * tests if the list of values provided for a user to select
	 * is valid for the datatype
	 * @throws Exception
	 */
	@Test
	public void testDateTimeFieldException()  {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("invalid default Value");

		DateTimeField field = ImmutableDateTimeField.builder().contentTypeId("test").variable("teat").name("test").defaultValue("1/1/2016 10:11:12").build();

	}

	@Test
	public void testDateTimeField()  {

		String[] valids = new String[]{"now","2011-01-01 10:11:12","2034-01-01 14:17:18" };
		for(String x: valids){
			DateTimeField field = ImmutableDateTimeField.builder().contentTypeId("test").variable("teat").name("test").defaultValue(x).build();
			assertThat("now is a valid default for date fields works ",field.defaultValue().equals(x));
		}
		DateTimeField field = ImmutableDateTimeField.builder().contentTypeId("test").variable("teat").name("test").defaultValue(null).build();
		assertThat("now is a valid default for date fields works ",field.defaultValue()==null);
	}


	/**
	 * tests if the list of values provided for a user to select
	 * is valid for the datatype
	 * @throws Exception
	 */
	@Test
	public void testTimeFieldException()  {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("invalid default Value");

		TimeField field = ImmutableTimeField.builder().contentTypeId("test").variable("teat").name("test").defaultValue("10-11-12 pm").build();

	}

	@Test
	public void testTimeField()  {

		String[] valids = new String[]{"now","10:11:12","4:17:18" , "4:17:18 pm"};
		for(String x: valids){
			TimeField field = ImmutableTimeField.builder().contentTypeId("test").variable("teat").name("test").defaultValue(x).build();
			assertThat("now is a valid default for date fields works ",field.defaultValue().equals(x));
		}
		TimeField field = ImmutableTimeField.builder().contentTypeId("test").variable("teat").name("test").defaultValue(null).build();
		assertThat("now is a valid default for date fields works ",field.defaultValue()==null);
	}

}
