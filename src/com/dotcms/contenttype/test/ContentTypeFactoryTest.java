package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.model.type.BaseContentTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.db.DbConnectionFactory;
public class ContentTypeFactoryTest {
	
	
	
	final static String NEWS = "28039964-5615-4ccf-bb96-ded62adbcc6a";
	final static String CONTENT = "2a3e91e4-fbbf-4876-8c5b-2233c1739b05";
	final static String FILEASSET = "33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d";
	final static String WIDGET = "9d8cba31-0072-4c38-96db-8b619f2b57ab";
	final static String FORM = "cb1f21c7-7d98-4ea3-903f-c5de10e41700";
	final static String PERSONA = "c938b15f-bcb6-49ef-8651-14d455a97045";
	final static String HTMLPAGE = "c541abb1-69b3-4bc5-8430-5e09e5239cc8";
	
	@Test
	public void runTest() throws Exception {


		DbConnectionFactory.overrideDefaultDatasource(new TestDataSource().getDataSource());
		ContentTypeFactoryImpl factory = new ContentTypeFactoryImpl();
		ContentType contentType = factory.find(CONTENT);
		ContentType contentType2 = factory.find(CONTENT);
		assertThat("ContentType is type Content", contentType.baseType() == BaseContentTypes.CONTENT);
		System.out.println("contentType1:" + contentType);
		System.out.println("contentType2:" + contentType2);
		assertThat("ContentType == ContentType2", contentType.equals(contentType2));

		assertThat("ContentType is type CONTENT", factory.find(NEWS).baseType() == BaseContentTypes.CONTENT);
		assertThat("News is not simple content", !factory.find(NEWS).equals(factory.find(CONTENT)));
		assertThat("ContentType is type FILEASSET", factory.find(FILEASSET).baseType() == BaseContentTypes.FILEASSET);
		assertThat("ContentType is type WIDGET", factory.find(WIDGET).baseType() == BaseContentTypes.WIDGET);
		assertThat("ContentType is type FORM", factory.find(FORM).baseType() == BaseContentTypes.FORM);
		assertThat("ContentType is type PERSONA", factory.find(PERSONA).baseType() == BaseContentTypes.PERSONA);
		assertThat("ContentType is type HTMLPAGE", factory.find(HTMLPAGE).baseType() == BaseContentTypes.HTMLPAGE);

	}
}
