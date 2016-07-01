package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;

public class FieldFactoryTest {


	@Test
	public void runTest() throws Exception {

		DbConnectionFactory.overrideDefaultDatasource(new TestDataSource().getDataSource());
		FieldFactory factory = new FieldFactoryImpl();
		//assertThat("ContentType is type Content", factory.find("Asd").baseType() == BaseContentTypes.CONTENT);


		DotConnect db = new DotConnect();
		db.setSQL("select inode from field");
		List<Map<String, Object>> results = db.loadObjectResults();

		for (Map<String, Object> map : results) {
			Field field1 = factory.byId(map.get("inode").toString());
			Field field2 = factory.byId(map.get("inode").toString());
			assertThat("Field1 == Field2", field1.equals(field2));
		}
	
		List<Field> fields1 = factory.byContentTypeId(Constants.NEWS);
		List<Field> fields2 = factory.byContentTypeVar("news");
		for(int i=0;i<fields1.size();i++){
			Field field1 = fields1.get(i);
			Field field2 = fields2.get(i);
			System.out.println(field1);
			System.out.println(field2);
			assertThat("Field1 == Field2", field1.equals(field2));
		}

		TextField textField = ImmutableTextField.builder()
				.name("test field")
				.variable("testField")
				.contentTypeId(Constants.NEWS)
				.hint("my hint")
				.dataType(DataTypes.TEXT)
				.build();	
		Field f = factory.save(textField);
		System.out.println(f.inode());
		
		
	}
}
