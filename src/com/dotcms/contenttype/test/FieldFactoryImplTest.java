package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.exception.OverFieldLimitException;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableDateTimeField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.test.DataSourceForTesting;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Config;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.ClassPath;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FieldFactoryImplTest {

	FieldFactoryImpl factory = new FieldFactoryImpl();
	final static String TEST_VAR_PREFIX = "testField";

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
	public void testFieldVariables() throws Exception {
		
		
	}
	
	
	
	@Test
	public void testSaveAndFindReturnSameObject() throws Exception {

		String uu = UUID.randomUUID().toString();

		TextField textField = ImmutableTextField.builder().name("test field" + uu).variable(TEST_VAR_PREFIX + uu).contentTypeId(Constants.NEWS)
				.hint("my hint").dataType(DataTypes.TEXT).inode(uu).build();

		Field savedField = factory.save(textField);
		String inode = savedField.inode();
		Field field2 = factory.byId(inode);

		assertThat("savedField == field2", savedField.equals(field2));

	}

	@Test
	public void testDataTypeLimit() throws Exception {

		long time = System.currentTimeMillis();
		ContentType struct = ImmutableSimpleContentType.builder().description("description" + time).folder(FolderAPI.SYSTEM_FOLDER)
				.host(Host.SYSTEM_HOST).name("ContentTypeTesting" + time).owner("owner")
				.velocityVarName("velocityVarNameTesting" + time).build();
		struct = new ContentTypeFactoryImpl().save(struct);

		for (DataTypes dt : DataTypes.values()) {
			int numFields = 0;
			List<Field> fields = factory.byContentTypeId(struct.inode());
			for (Field f : fields) {
				if (f.dataType() == dt)
					numFields++;
			}
			while (numFields < 30) {
				String uu = UUID.randomUUID().toString();

				Field savedField = null;

				if (dt == DataTypes.FLOAT || dt == DataTypes.TEXT || dt == DataTypes.INTEGER) {
					savedField = ImmutableTextField.builder().name("test field" + uu).variable(TEST_VAR_PREFIX + "textField" + uu)
							.contentTypeId(struct.inode()).dataType(dt).build();

				} else if (dt == DataTypes.DATE) {
					savedField = ImmutableDateTimeField.builder().name("date field " + uu).variable(TEST_VAR_PREFIX +"dateField" + uu)
							.contentTypeId(struct.inode()).dataType(dt).build();

				} else if (dt == DataTypes.LONG_TEXT) {
					savedField = ImmutableTextAreaField.builder().name("long text field " + uu).variable(TEST_VAR_PREFIX +"longTextField" + uu)
							.contentTypeId(struct.inode()).dataType(dt).build();

				}
				if (savedField != null) {
					try {
						factory.save(savedField);
						
					} catch (Exception e) {

						assertThat("Over Field Limit", e instanceof OverFieldLimitException);
						assertThat("Over Field Limit",
								numFields >= Config.getIntProperty("db.number.of.contentlet.columns.per.datatype", 25));
						break;
					}
				}
				numFields++;
			}

		}
	}
	@Test
	public void testDeleteingFields() throws Exception {
		for(ContentType type : APILocator.getContentTypeAPI2().findAll(APILocator.getUserAPI().getSystemUser(), true)){
			for(Field field : factory.byContentType(type)){
				if(field.variable().startsWith(TEST_VAR_PREFIX)){
					deleteFields(ImmutableList.of(field));
				}
			}
		}
	}
	
	
	@Test
	public void testLegacyFieldBuilder() throws Exception {

		List<Field> newFields = this.factory.byContentTypeId(Constants.NEWS);
		List<com.dotmarketing.portlets.structure.model.Field> oldFields = APILocator.getStructureAPI()
				.find(Constants.NEWS, APILocator.getUserAPI().getSystemUser()).getFieldsBySortOrder();

		List<Field> newOldFields = new LegacyFieldTransformer(oldFields).asList();

		assertThat("newFields == oldFields", newFields.size() == oldFields.size());

		for (int i = 0; i < newFields.size(); i++) {
			Field f1 = newFields.get(i);
			Field f2 = newOldFields.get(i);
			try {
				assertThat("New Field and old field are equal", f1.equals(f2));
				buildObject(ImmutableList.of(f1, f2));
			} catch (Throwable t) {
				System.out.println("these are not equal!:" + i);

				// these are not equal!
				System.out.println(f1);
				System.out.println(f2);
				throw t;

			}
		}

	}

	@Test
	public void testFieldImplClasses() throws Exception {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		Set<ClassPath.ClassInfo> infos = ClassPath.from(classLoader).getTopLevelClassesRecursive("com.dotcms.contenttype.model.field");
		for (ClassPath.ClassInfo info : infos) {

			Class clazz = Class.forName(info.getPackageName() + "." + info.getSimpleName());

			if (Field.class.isAssignableFrom(clazz)) {
				if (clazz.equals(Field.class) ) {
					continue;
				}
				Field newField = FieldBuilder.builder(clazz).name("test" + clazz.getSimpleName()).variable(TEST_VAR_PREFIX + clazz.getSimpleName()).build();
				buildObject(ImmutableList.of(newField));
			}
		}

	}

	private void buildObject(List<Field> fields) throws Exception {

		for (Field f : fields) {
			try {
				assertThat("we are getting the right class back from type()", f.type().isAssignableFrom(f.getClass()));

				// build with the different datatypes
				for(DataTypes dt : f.acceptedDataTypes()){
					FieldBuilder.builder(f).dataType(dt).build();
				}
			} catch (Throwable t) {
				System.out.println(f);
				System.out.println(f.type());
				System.out.println(f.getClass());

				throw t;
			}
		}
	}
	
	
	private void deleteFields(List<Field> fields) throws Exception {
		
		for(Field field : fields){
			assertThat("deleteing works", field.equals(factory.byId(field.inode())));
			factory.delete(field);
			try{
				Field nope = factory.byId(field.inode());
			}
			catch(Exception nodb){
				assertThat("deleteing works", nodb instanceof NotFoundInDbException);
			}
			assertThat("deleteing fieldVars works", factory.loadVariables(field).size()==0);

		}
	}
}
