package com.dotcms.contenttype.test;

import java.io.FileNotFoundException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.business.FieldApiImpl;
import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.field.FieldType;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.test.DataSourceForTesting;

public class ScratchTest {

	FieldFactory factory = new FieldFactoryImpl();
	
	@BeforeClass
	public static void initDb() throws FileNotFoundException, Exception{
		DbConnectionFactory.overrideDefaultDatasource(new DataSourceForTesting().getDataSource());
	}
	
	@Test
	public void testEquals() throws Exception {
		FieldApiImpl api = new FieldApiImpl();
		for (Class clazz : api.fieldTypes()) {
			if( FieldType.class.isAssignableFrom(clazz)){
				
				
				
				
				
				
			}
			else{
				System.out.println("Nope:" + clazz);
			}
		}
		api.fieldTypes();
	}
}