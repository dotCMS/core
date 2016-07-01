package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.Test;

import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.model.type.BaseContentTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.db.DbConnectionFactory;

public class ContentTypeFactoryTest {


	@Test
	public void runTest() throws Exception {

		DbConnectionFactory.overrideDefaultDatasource(new TestDataSource().getDataSource());
		ContentTypeFactory factory = new ContentTypeFactoryImpl();
		
		
		// Test all the types
		assertThat("ContentType is type Content", factory.find(Constants.CONTENT).baseType() == BaseContentTypes.CONTENT);
		assertThat("ContentType is type CONTENT", factory.find(Constants.NEWS).baseType() == BaseContentTypes.CONTENT);
		assertThat("News is not simple content", !factory.find(Constants.NEWS).equals(factory.find(Constants.CONTENT)));
		assertThat("ContentType is type FILEASSET", factory.find(Constants.FILEASSET).baseType() == BaseContentTypes.FILEASSET);
		assertThat("ContentType is type WIDGET", factory.find(Constants.WIDGET).baseType() == BaseContentTypes.WIDGET);
		assertThat("ContentType is type FORM", factory.find(Constants.FORM).baseType() == BaseContentTypes.FORM);
		assertThat("ContentType is type PERSONA", factory.find(Constants.PERSONA).baseType() == BaseContentTypes.PERSONA);
		assertThat("ContentType is type HTMLPAGE", factory.find(Constants.HTMLPAGE).baseType() == BaseContentTypes.HTMLPAGE);

		List<ContentType> types = factory.findAll(); 
		
		for (ContentType type : types) {
			ContentType contentType = factory.find(type.inode());
			ContentType contentType2 = factory.findByVar(type.velocityVarName());
			assertThat("ContentType == ContentType2", contentType.equals(contentType2) && contentType.equals(type));
			
		}
		assertThat("findAll sort by Name has same size as find all", factory.findAll("name").size()  ==types.size());
		
	}
	
	
	
	
	
	
}
