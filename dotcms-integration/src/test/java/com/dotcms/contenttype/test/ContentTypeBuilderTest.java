package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotmarketing.db.test.DataSourcesForTesting;

public class ContentTypeBuilderTest extends ContentTypeBaseTest {

	@Test
	public void testEquals() throws Exception {
		ContentType f1 = ContentTypeBuilder.instanceOf(SimpleContentType.class);
		ContentType f2 = ContentTypeBuilder.instanceOf(SimpleContentType.class);
		assertThat("ContentTypeBuilderUtilTest works ",f1.equals(f2));
	}

	@Test
	public void testAllContentTypeBuilders() throws Exception {
		for(BaseContentType  type : BaseContentType.values()){
			if(type==BaseContentType.ANY)continue;
			ContentTypeBuilder.instanceOf(type.immutableClass());
			ContentTypeBuilder.builder(type.immutableClass()).id("asd");
		}
	}

	@Test
	public void testCopy() throws Exception {

		ContentType test = ImmutableWidgetContentType.builder()
				.name("TEST Title")
				.variable("formTitle")
				.fixed(true)
				.build();

		ContentType test2 = ContentTypeBuilder.builder(test).build();
		assertThat("ContentTypeBuilder works ",test.equals(test2));

	}
}