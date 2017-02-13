package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.exception.OverFieldLimitException;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableDateTimeField;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.org.apache.hadoop.mapred.lib.Arrays;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Config;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FieldFactoryImplTest extends ContentTypeBaseTest {

	final static String TEST_VAR_PREFIX = "testField";

	@Test
	public void testEquals() throws Exception {

		DotConnect db = new DotConnect();

		db.setSQL("select inode from field");
		List<Map<String, Object>> results = db.loadObjectResults();

		for (Map<String, Object> map : results) {
			Field field1 = fieldFactory.byId(map.get("inode").toString());
			Field field2 = fieldFactory.byId(map.get("inode").toString());
			assertThat("Field1 == Field2", field1.equals(field2));
		}
	}

	@Test
	public void testFindByTypeMethods() throws Exception {

		List<Field> fields1 = fieldFactory.byContentTypeId(Constants.NEWS);
		List<Field> fields2 = fieldFactory.byContentTypeId(Constants.NEWS);
		for (int i = 0; i < fields1.size(); i++) {

			Field field1 = fields1.get(i);
			Field field2 = fields2.get(i);

			assertThat("Field1 == Field2", field1.equals(field2));
		}
	}



	@Test
	public void testFieldVariables() throws Exception {
		List<Field> fields = fieldFactory.byContentTypeId(Constants.NEWS);
		final int runs = 10;


		for (Field field : fields) {
			List<FieldVariable> vars = FactoryLocator.getFieldFactory2().loadVariables(field);
			for(FieldVariable var : vars){
				fieldApi.delete(var);
			}
			vars = fieldApi.loadVariables(field);

			assertThat("No field vars for field " + field, vars.size() ==0);

			for(int i=0;i<runs;i++){
				String key = "key" + i;
				String val = "val"+i;

				FieldVariable var = ImmutableFieldVariable.builder()
						.key(key)
						.value(val)
						.fieldId(field.inode())
						.build();

				var = fieldApi.save(var, APILocator.systemUser());
				assertThat("field var saved correctly " + var,var.equals(fieldApi.loadVariable(var.id())));

				vars = fieldApi.loadVariables(field);
				assertThat("field var added for field " + field, vars.size() ==i+1);

			}

			int mySize = vars.size();
			assertThat("field vars all saved correctly ", mySize ==runs);

			for(FieldVariable var : vars){
				fieldApi.delete(var);
				assertThat("field deleted correctly " + var,--mySize ==  fieldApi.loadVariables(field).size());
			}
		}
	}

	@Test
	public void testSaveAndFindReturnSameObject() throws Exception {

		String uu = UUID.randomUUID().toString();

		TextField textField = ImmutableTextField.builder().name("test field" + uu)
				.variable(TEST_VAR_PREFIX + uu).contentTypeId(Constants.NEWS).hint("my hint")
				.dataType(DataTypes.TEXT).id(uu).build();

		Field savedField = fieldFactory.save(textField);
		String inode = savedField.inode();
		Field field2 = fieldFactory.byId(inode);

		assertThat("savedField == field2", savedField.equals(field2));

	}

	@Test
	public void testDataTypeLimit() throws Exception {

		long time = System.currentTimeMillis();
		ContentType struct = ImmutableSimpleContentType.builder().description("description" + time)
				.folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
				.name("ContentTypeTesting" + time).owner("owner")
				.variable("velocityVarNameTesting" + time).build();
		struct = new ContentTypeFactoryImpl().save(struct);

		for (DataTypes dt : DataTypes.values()) {
			int numFields = 0;
			List<Field> fields = fieldFactory.byContentTypeId(struct.inode());
			for (Field f : fields) {
				if (f.dataType() == dt)
					numFields++;
			}
			while (numFields < 30) {
				String uu = UUID.randomUUID().toString();

				Field savedField = null;

				if (dt == DataTypes.FLOAT || dt == DataTypes.TEXT || dt == DataTypes.INTEGER) {
					savedField = ImmutableTextField.builder().name("test field" + uu)
							.variable(TEST_VAR_PREFIX + "textField" + uu)
							.contentTypeId(struct.inode()).dataType(dt).build();

				} else if (dt == DataTypes.DATE) {
					savedField = ImmutableDateTimeField.builder().name("date field " + uu)
							.variable(TEST_VAR_PREFIX + "dateField" + uu)
							.contentTypeId(struct.inode()).dataType(dt).build();

				} else if (dt == DataTypes.LONG_TEXT) {
					savedField = ImmutableTextAreaField.builder().name("long text field " + uu)
							.variable(TEST_VAR_PREFIX + "longTextField" + uu)
							.contentTypeId(struct.inode()).dataType(dt).build();

				}
				if (savedField != null) {
					try {
						fieldFactory.save(savedField);

					} catch (Throwable e) {
						try {
							assertThat("Over Field Limit:" + e.getMessage(),
									e instanceof OverFieldLimitException);
							assertThat("Over Field Limit" + e.getMessage(),
									numFields >= Config.getIntProperty(
											"db.number.of.contentlet.columns.per.datatype", 25));
							break;
						} catch (Throwable t) {
							e.printStackTrace();
							t.printStackTrace();
							throw e;
						}
					}
				}
				numFields++;
			}

		}
	}

	@Test
	public void testDeleteingFields() throws Exception {
		for (ContentType type : APILocator.getContentTypeAPI(APILocator.getUserAPI().getSystemUser(), true)
				.findAll()) {
			for (Field field : fieldFactory.byContentType(type)) {
				if (field.variable().startsWith(TEST_VAR_PREFIX)) {
					deleteFields(ImmutableList.of(field));
				}
			}
		}
	}


	@Test
	public void testLegacyFieldBuilder() throws Exception {

		List<Field> newFields = Arrays.asList(fieldFactory.byContentTypeId(Constants.NEWS).toArray(new Field[0]));
		List<com.dotmarketing.portlets.structure.model.Field> oldFields = APILocator
				.getStructureAPI().find(Constants.NEWS, APILocator.getUserAPI().getSystemUser())
				.getFieldsBySortOrder();

		assertThat("newFields == oldFields", newFields.size() == oldFields.size());

		List<Field> newOldFields = Arrays.asList(new LegacyFieldTransformer(oldFields).asList().toArray(new Field[0]));

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

		for (Class clazz : APILocator.getContentTypeFieldAPI().fieldTypes()) {
			Field newField = FieldBuilder.builder(clazz).contentTypeId("test")
					.name("test" + clazz.getSimpleName())
					.variable(TEST_VAR_PREFIX + clazz.getSimpleName()).build();
			buildObject(ImmutableList.of(newField));

		}

	}

	@Test
	public void testSuggestVelocityVar() throws Exception {

		Map<String, String> testingNames =
				ImmutableMap.<String, String>builder().put("This is a Variable", "thisIsAVariable")
				.put("HereIs ONe", "hereisOne")
				.put("A. A. Milne", "aAMilne")
				.put("test", "test")
				.put("teSt", "teSt1")
				.put("TEST", "test2")
				.put("TeST", "test3")
				.put("TeSt", "test4")
				.put("Test", "test5")
				.put("1 x 2/5", "1X25")
				.put("Test5", "test51")
				.put("SKU", "sku")
				.put("Camel Case DOne Wrong", "camelCaseDoneWrong")
				.put("NOOWORK ee", "nooworkEe")
				.put("#@%!$Q#^QAGR", "qQagr")
				.build();



		List<Field> testFields = null;
		for (BaseContentType baseType : BaseContentType.values()) {
			if (baseType == baseType.ANY)
				continue;
			ContentType type = ContentTypeBuilder.instanceOf(baseType.immutableClass());
			testFields = type.requiredFields();
			for (Field field : testFields) {
				// make sure variables work

				String suggestion = fieldFactory.suggestVelocityVar(field.name(), ImmutableList.of());
				String suggestion2 = fieldFactory.suggestVelocityVar(field.variable(), testFields);
				try {
					assertThat("we are not munging up existing vars", field.variable().equals(
							fieldFactory.suggestVelocityVar(field.variable(), ImmutableList.of())));
					assertThat("we are suggesting good names", suggestion != null);
					assertThat("we should not suggest an existing variable name ",
							(!field.variable().equals(suggestion2)));
				} catch (Throwable t) {

					System.out.println(t.getMessage());
					System.out.println(type);
					System.out.println(field);
					System.out.println(field.variable());
					System.out.println("suggestion:" + suggestion);
					System.out.println("suggestion:" + suggestion2);
				}
			}
		}

		testFields = new ArrayList<>();
		for (String key : testingNames.keySet()) {
			String suggest = fieldFactory.suggestVelocityVar(key, testFields);
			testFields.add(ImmutableTextField.builder().name(key).variable(suggest)
					.contentTypeId("fake").build());
			assertThat("variable " + key + " "  + " returned "
					+ suggest + " expected " + testingNames.get(key), suggest.equals(testingNames.get(key)) );


		}
		Set<String> set = new HashSet();
		for (Field field : testFields) {
			assertThat("we have all different var names", !set.contains(field.variable()));
			set.add(field.variable());
		}



	}

	private void buildObject(List<Field> fields) throws Exception {

		for (Field f : fields) {
			try {
				assertThat("we are getting the right class back from type()",
						f.type().isAssignableFrom(f.getClass()));

				// build with the different datatypes
				for (DataTypes dt : f.acceptedDataTypes()) {
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

		for (Field field : fields) {
			assertThat("deleteing works", field.equals(fieldFactory.byId(field.inode())));
			fieldFactory.delete(field);
			try {
				Field nope = fieldFactory.byId(field.inode());
			} catch (Exception nodb) {
				assertThat("deleteing works", nodb instanceof NotFoundInDbException);
			}
			assertThat("deleteing fieldVars works", fieldFactory.loadVariables(field).size() == 0);

		}
	}
}
