package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.contenttype.model.field.TextField;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.test.DataSourceForTesting;

public class FieldBuilderUtilTest {

	FieldFactory factory = new FieldFactoryImpl();
	
	@BeforeClass
	public static void initDb() throws FileNotFoundException, Exception{
		DbConnectionFactory.overrideDefaultDatasource(new DataSourceForTesting().getDataSource());
	}
	
	@Test
	public void testEquals() throws Exception {
		Field f1 = FieldBuilder.instanceOf(TextField.class);
		Field f2 = FieldBuilder.instanceOf(TextField.class);
		System.out.println(f1.typeName());
		System.out.println(f2.typeName());
		assertThat("fieldbuilder works ",f1.equals(f2));
	}
	
	@Test
	public void testAllFieldBuilders() throws Exception {
		for(LegacyFieldTypes types : LegacyFieldTypes.values()){
			FieldBuilder.instanceOf(types.implClass());
			FieldBuilder.builder(types.implClass()).inode("asd");
			
		}
	}
	
	
}