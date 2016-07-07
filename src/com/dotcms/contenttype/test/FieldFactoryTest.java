package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sun.reflect.misc.FieldUtil;

import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.test.DataSourceForTesting;

public class FieldFactoryTest {

	FieldFactory factory = new FieldFactoryImpl();
	
	@BeforeClass
	public static void initDb() throws FileNotFoundException, Exception{
		DbConnectionFactory.overrideDefaultDatasource(new DataSourceForTesting().getDataSource());
	}
	
	@Test
	public void testEquals() throws Exception {

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

			assertThat("Field1 == Field2", field1.equals(field2));
		}
	}
	
	
	@Test
	public void testFindByTypeMethods() throws Exception {

		List<Field> fields1 = factory.byContentTypeId(Constants.NEWS);
		List<Field> fields2 = factory.byContentTypeVar("news");
		for(int i=0;i<fields1.size();i++){
			Field field1 = fields1.get(i);
			Field field2 = fields2.get(i);

			assertThat("Field1 == Field2", field1.equals(field2));
		}
	}
	
	
	@Test
	public void testSaveAndFindReturnSameObject() throws Exception {

		TextField textField = ImmutableTextField.builder()
				.name("test field")
				.variable("testField")
				.contentTypeId(Constants.NEWS)
				.hint("my hint")
				.dataType(DataTypes.TEXT)
				.inode("fieldtesting")
				.build();	
	
		Field savedField = factory.save(textField);
		String inode = savedField.inode();
		Field field2 = factory.byId(inode);

		assertThat("savedField == field2", savedField.equals(field2));
		
	}
	
	
	@Test
	public void test25FieldLimit() throws Exception {

		TextField textField = ImmutableTextField.builder()
				.name("test field")
				.variable("testField")
				.contentTypeId(Constants.NEWS)
				.hint("my hint")
				.dataType(DataTypes.TEXT)
				.inode("fieldtesting")
				.build();	
	
		Field savedField = factory.save(textField);
		String inode = savedField.inode();
		Field field2 = factory.byId(inode);

		assertThat("savedField == field2", savedField.equals(field2));
		
	}
	
	
	//@Test
	public void testBuilder() throws Exception {
		Field field = new BinaryField() {
			
			@Override
			public String variable() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String values() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String relationType() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String regexCheck() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String owner() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String name() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String inode() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String hint() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String defaultValue() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String dbColumn() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public DataTypes dataType() {
				// TODO Auto-generated method stub
				return DataTypes.BINARY;
			}
			
			@Override
			public String contentTypeId() {
				// TODO Auto-generated method stub
				return null;
			}
		};

	}

}
