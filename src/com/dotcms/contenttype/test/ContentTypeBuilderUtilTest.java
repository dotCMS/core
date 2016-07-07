package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.type.BaseContentTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.test.DataSourceForTesting;

public class ContentTypeBuilderUtilTest {

	FieldFactory factory = new FieldFactoryImpl();
	
	@BeforeClass
	public static void initDb() throws FileNotFoundException, Exception{
		DbConnectionFactory.overrideDefaultDatasource(new DataSourceForTesting().getDataSource());
	}
	
	@Test
	public void testEquals() throws Exception {
		ContentType f1 = ContentTypeBuilder.instanceOf(SimpleContentType.class);
		ContentType f2 = ContentTypeBuilder.instanceOf(SimpleContentType.class);
		System.out.println(f1.name());
		System.out.println(f2.name());
		assertThat("ContentTypeBuilderUtilTest works ",f1.equals(f2));
	}
	
	@Test
	public void testAllContentTypeBuilders() throws Exception {
		for(BaseContentTypes  types : BaseContentTypes.values()){
			ContentTypeBuilder.instanceOf(types.implClass());
			ContentTypeBuilder.builder(types.implClass()).inode("asd");
			
		}
	}
	
	
}