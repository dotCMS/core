package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableHiddenField;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.contenttype.model.field.TextField;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.test.DataSourceForTesting;

public class FieldBuilderTest {

	FieldFactory factory = new FieldFactoryImpl();
	
	@BeforeClass
	public static void initDb() throws FileNotFoundException, Exception{
		new DataSourceForTesting().setup();
	}
	
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
				.fixed(true)
				.searchable(true)
				.build();
		
		
		Field test2 = FieldBuilder.builder(test).build();
		assertThat("fieldbuilder works ",test.equals(test2));
		
	}
	
}