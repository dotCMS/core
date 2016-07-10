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
import com.dotcms.contenttype.model.type.BaseContentTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.test.DataSourceForTesting;

public class ContentTypeBuilderTest {

	FieldFactory factory = new FieldFactoryImpl();
	
	@BeforeClass
	public static void initDb() throws FileNotFoundException, Exception{
		DbConnectionFactory.overrideDefaultDatasource(new DataSourceForTesting().getDataSource());
	}
	
	@Test
	public void testEquals() throws Exception {
		ContentType f1 = ContentTypeBuilder.instanceOf(SimpleContentType.class);
		ContentType f2 = ContentTypeBuilder.instanceOf(SimpleContentType.class);
		assertThat("ContentTypeBuilderUtilTest works ",f1.equals(f2));
	}
	
	@Test
	public void testAllContentTypeBuilders() throws Exception {
		for(BaseContentTypes  type : BaseContentTypes.values()){
			if(type==BaseContentTypes.NONE)continue;
			ContentTypeBuilder.instanceOf(type.implClass());
			ContentTypeBuilder.builder(type.implClass()).inode("asd");
			
		}
	}
	
	@Test
	public void testCopy() throws Exception {
		
		ContentType test = ImmutableWidgetContentType.builder()
				.name("TEST Title")
				.velocityVarName("formTitle")

				.fixed(true)

				.build();
		
		
		ContentType test2 = ContentTypeBuilder.builder(test).build();
		assertThat("ContentTypeBuilder works ",test.equals(test2));
		
	}
}