package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.exception.OverFieldLimitException;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableDateTimeField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.test.DataSourceForTesting;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Config;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FieldFactoryTest {

	FieldFactory factory = new FieldFactoryImpl();

	
	@BeforeClass
	public static void initDb() throws FileNotFoundException, Exception {
		DataSource ds  =new DataSourceForTesting().getDataSource();
		Connection c = ds.getConnection();
		DbConnectionFactory.overrideDefaultDatasource(ds);
		ServletContext context =  Mockito.mock(ServletContext.class);
		Config.CONTEXT = context;
		

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
		for (int i = 0; i < fields1.size(); i++) {
			Field field1 = fields1.get(i);
			Field field2 = fields2.get(i);

			assertThat("Field1 == Field2", field1.equals(field2));
		}
	}

	@Test
	public void testFindByTypeMethods() throws Exception {

		List<Field> fields1 = factory.byContentTypeId(Constants.NEWS);
		List<Field> fields2 = factory.byContentTypeVar("news");
		for (int i = 0; i < fields1.size(); i++) {

			Field field1 = fields1.get(i);
			Field field2 = fields2.get(i);

			assertThat("Field1 == Field2", field1.equals(field2));
		}
	}

	@Test
	public void testSaveAndFindReturnSameObject() throws Exception {

		String uu = UUID.randomUUID().toString();
		
		TextField textField = ImmutableTextField.builder().name("test field" + uu).variable("testField" + uu).contentTypeId(Constants.NEWS)
				.hint("my hint").dataType(DataTypes.TEXT).inode(uu).build();

		Field savedField = factory.save(textField);
		String inode = savedField.inode();
		Field field2 = factory.byId(inode);

		assertThat("savedField == field2", savedField.equals(field2));

	}

	@Test
	public void testDataTypeLimit() throws Exception {
		long time = System.currentTimeMillis();
		ContentType struct = ImmutableSimpleContentType.builder()
				.description("description" + time)
				.folder(FolderAPI.SYSTEM_FOLDER)
				.host(Constants.SYSTEM_HOST)
				.name("ContentTypeTesting" + time)
				.owner("owner")
				.velocityVarName("velocityVarNameTesting" + time)
				.build();
		struct = new ContentTypeFactoryImpl().save(struct);
		
		for(DataTypes dt : DataTypes.values()){
			int numFields = 0;
			List<Field> fields = factory.byContentTypeId(struct.inode());
			for(Field f : fields){
				if(f.dataType()==dt)numFields++;
			}
			while(numFields<30){
				String uu = UUID.randomUUID().toString();
			
				Field savedField = null;

				if(dt==DataTypes.FLOAT || dt==DataTypes.TEXT || dt==DataTypes.INTEGER  ){
					savedField = ImmutableTextField.builder()
							.name("test field" + uu)
							.variable("textField" + uu)
							.contentTypeId(struct.inode())
							.dataType(dt).build();
					
				}
				else if(dt==DataTypes.DATE  ){
					savedField =  ImmutableDateTimeField.builder().name("date field " + uu).variable("dateField" + uu).contentTypeId(struct.inode())
							.dataType(dt).build();
					
				}
				else if(dt==DataTypes.LONG_TEXT  ){
					savedField =  ImmutableTextAreaField.builder().name("long text field " + uu).variable("longTextField" + uu).contentTypeId(struct.inode())
							.dataType(dt).build();
					
				}
				if(savedField!=null){
					try{
						factory.save(savedField);
					}
					catch(Exception e){

						assertThat("Over Field Limit", e instanceof OverFieldLimitException );
						assertThat("Over Field Limit", numFields>=Config.getIntProperty("db.number.of.contentlet.columns.per.datatype", 25));
						break;
					}
				}
				numFields++;
			}
			


		}
	}

	@Test
	public void testLegacyFieldBuilder() throws Exception {

		List<Field> newFields = this.factory.byContentTypeId(Constants.NEWS);
		List<com.dotmarketing.portlets.structure.model.Field> oldFields = APILocator.getStructureAPI().find(Constants.NEWS, APILocator.getUserAPI().getSystemUser()).getFieldsBySortOrder();
		
		
		List<Field> newOldFields = new LegacyFieldTransformer(oldFields).asList();

		assertThat("newFields == oldFields", newFields.size() == oldFields.size());

		for (int i = 0; i < newFields.size(); i++) {
			Field f1 = newFields.get(i);
			Field f2 = newOldFields.get(i);
			try {
				assertThat("New Field and old field are equal", f1.equals(f2));
			} catch (Throwable t) {
				System.out.println("these are not equal!:" + i);

				// these are not equal!
				System.out.println(f1);
				System.out.println(f2);
				throw t;

			}

		}


	}

}
