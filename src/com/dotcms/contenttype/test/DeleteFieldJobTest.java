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

public class DeleteFieldJobTest {

	FieldFactory factory = new FieldFactoryImpl();
	

	@Test
	public void testFieldDelete() throws Exception {
		
	}
	
	
	
}