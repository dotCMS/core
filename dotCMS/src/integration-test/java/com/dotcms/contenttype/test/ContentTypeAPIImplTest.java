package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.OnePerContentType;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.Expireable;
import com.dotcms.contenttype.model.type.UrlMapable;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;

public class ContentTypeAPIImplTest extends ContentTypeBaseTest {

	@Test
	public void testFindMethodEquals() throws Exception {

		List<ContentType> types = contentTypeApi.findAll();
		for (ContentType type : types) {
			ContentType contentType = contentTypeApi.find(type.id());
			ContentType contentType2 = contentTypeApi.find(type.variable());
			try {
				assertThat("ContentType == ContentType2", contentType.equals(contentType2) && contentType.equals(type));
			} catch (Throwable t) {

				throw t;
			}
		}
	}

	@Test
	public void testFindAll() throws Exception {
		List<ContentType> types = contentTypeApi.findAll();
		assertThat("findAll sort by Name has same size as find all",
				contentTypeApi.search("0=0", "name desc", -1, 0).size() == types.size());
	}

	@Test
	public void testFieldsMethod() throws Exception {

		ContentType type = contentTypeApi.find(Constants.NEWS);

		// System.out.println(type);
		ContentType otherType = contentTypeApi.find(Constants.NEWS);

		List<Field> fields = otherType.fields();
		// System.out.println(type);
		List<Field> fields2 = type.fields();
		assertThat("We have fields!", fields.size() > 0 && fields.size() == fields2.size());
		for (int j = 0; j < fields.size(); j++) {
			Field field = fields.get(j);
			Field testField = fields2.get(j);
			assertThat("fields are correct:", field.equals(testField));
		}

		fields = type.fields();
		fields = type.fields();
		fields = type.fields();

	}

	@Test
	public void testSerialization() throws Exception {

		File temp = File.createTempFile("test1", "obj");
		File temp2 = File.createTempFile("test2", "obj");
		ContentType origType = contentTypeApi.find(Constants.NEWS);


		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(temp))) {
			oos.writeObject(origType);
			oos.close();
		}

		temp.renameTo(temp2);
		ContentType fromDisk = null;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(temp2))) {
			fromDisk = (ContentType) ois.readObject();
			ois.close();
		}


		try {
			assertThat("fields are correct:", origType.equals(fromDisk));
		} catch (Throwable e) {
			System.out.println("origType" + origType);
			System.out.println("fromDisk" + fromDisk);
			throw e;
		}

		List<Field> fields = origType.fields();
		List<Field> fields2 = fromDisk.fields();

		assertThat("We have fields!", fields.size() > 0 && fields.size() == fields2.size());
		for (int j = 0; j < fields.size(); j++) {
			Field field = fields.get(j);
			Field testField = fields2.get(j);
			try {
				assertThat("fields are correct:", field.equals(testField));
			} catch (Throwable t) {
				System.out.println("Old and New fields are NOT the same");
				System.out.println(field);
				System.out.println(testField);
				throw t;
			}
		}

	}

	@Test
	public void testAddingContentTypes() throws Exception {
		int count = contentTypeApi.count();
		int runs = 20;

		for (int i = 0; i < runs; i++) {
			long time = System.currentTimeMillis() + i;
			int base = (i % 5) + 1;
			Thread.sleep(1);
			ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(base))
					.description("description" + time).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
					.name("ContentTypeTestingWithFields" + time).owner("owner").variable("velocityVarNameTesting" + time).build();
			type = contentTypeApi.save(type);
			addFields(type);
		}
		int count2 = contentTypeApi.count();
		assertThat("contenttypes are added", count == count2 - runs);
	}

	@Test
	public void testUpdatingContentTypes() throws Exception {
		List<ContentType> types = contentTypeApi.findUrlMapped();
		assertThat("findUrlMapped only returns urlmapped content", types.size() > 0);
		for (ContentType type : types) {
			assertThat("findUrlMapped only returns urlmapped content", type.urlMapPattern() != null);
		}

	}

	@Test
	public void testDefaultType() throws DotDataException, DotSecurityException {

		long time = System.currentTimeMillis();
		ContentType initialDefaultType = contentTypeApi.save(ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
				.description("description" + time).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
				.name("ContentTypeDefault1" + time).owner("owner").variable("velocityVarNameDefault1" + time).build());
		contentTypeApi.setAsDefault(initialDefaultType);
		assertThat("we have a default content type", initialDefaultType != null && contentTypeApi.findDefault().defaultType());

		ContentType newDefaultType = contentTypeApi.save(ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
				.description("description" + time).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
				.name("ContentTypeDefault2" + time).owner("owner").variable("velocityVarNameDefault2" + time).build());
		contentTypeApi.setAsDefault(newDefaultType);
		newDefaultType = contentTypeApi.findDefault();
		assertThat("there is a new default content type", newDefaultType.inode().equals(contentTypeApi.findDefault().inode()));

		assertThat("existing content type is not default anymore", initialDefaultType != null && !contentTypeApi.find(initialDefaultType.inode()).defaultType());
	}

	@Test
	public void testSearch() throws Exception {
		String[] searchTerms =
			{Constants.NEWS, "structuretype = 2", " And structure.inode='" + Constants.NEWS + "'"};

		int totalCount = contentTypeApi.count();

		List<ContentType> types = contentTypeApi.search(null, BaseContentType.ANY, "name", -1, 0);
		assertThat("we have at least 40 content types", types.size() > 20);
		types = contentTypeApi.search(null, BaseContentType.ANY, "name", 5, 0);
		assertThat("limit works and we have max five content types", types.size() < 6);
		for (int x = 0; x < totalCount; x = x + 5) {
			types = contentTypeApi.search(null, BaseContentType.ANY, "name asc", 5, 0);
			assertThat("we have max five content types", types.size() < 6);
		}

		for (int i = 0; i < BaseContentType.values().length; i++) {
			types = contentTypeApi.search(null, BaseContentType.getBaseContentType(i), "name", -1, 0);
			if (!types.isEmpty()) {
				assertThat("we have content types of " + BaseContentType.getBaseContentType(i),
					types.size() > 0);
				int count = contentTypeApi.count(null, BaseContentType.getBaseContentType(i));
				assertThat("Count works as well", types.size() == count);
			} else {
				System.out.println("No data found for BaseContentType: " + BaseContentType.getBaseContentType(i));
			}
		}

		for (int i = 0; i < searchTerms.length; i++) {
			types = contentTypeApi.search(searchTerms[i], BaseContentType.ANY, "mod_date desc", -1, 0);
			if (!types.isEmpty()) {
				assertThat("we can search content types:" + searchTerms[i], types.size() > 0);
				int count = contentTypeApi.count(searchTerms[i], BaseContentType.ANY);
				assertThat("Count works as well", types.size() == count);
			} else {
				System.out.println("No data found for BaseContentType: " + BaseContentType.getBaseContentType(i));
			}
		}

	}

	@Test
	public void testAddingUpdatingDeleting() throws Exception {

		for (BaseContentType baseType : BaseContentType.values()) {
			if (baseType == BaseContentType.ANY)
				continue;

			int countAll = contentTypeApi.count();
			int runs = 10;
			int countBaseType = contentTypeApi.count(null, baseType);

			for (int i = 0; i < runs; i++) {
				insert(baseType);
				Thread.sleep(1);
			}

			int countAll2 = contentTypeApi.count();
			int countBaseType2 = contentTypeApi.count(null, baseType);
			assertThat("counts are working", countAll == countAll2 - runs);
			assertThat("counts are working", countAll2 > countBaseType2);
			assertThat("counts are working", countBaseType == countBaseType2 - runs);

			for (int i = 0; i < runs; i++) {
				insert(baseType);
				Thread.sleep(1);
			}
			int countAll3 = contentTypeApi.count();
			int countBaseType3 = contentTypeApi.count(null, baseType);
			assertThat("counts are working", countAll2 == countAll3 - runs);
			assertThat("counts are working", countAll3 > countBaseType3);
			assertThat("counts are working", countBaseType2 == countBaseType3 - runs);

		}

		testUpdating();

		testDeleting();
	}

	@Test
	public void count() throws Exception {
		String query = " velocity_var_name like '%content%'";
		List<ContentType> types = contentTypeApi.search(query);

		int count = contentTypeApi.count(query, BaseContentType.ANY);
		assertThat("we have the right content types:", types.size() == count);
	}

	@Test
	public void suggestVelocityVar() throws DotDataException {
		String tryVar = "Content" + System.currentTimeMillis();
		String newVar = contentTypeApi.suggestVelocityVar(tryVar);

		assertThat("random velocity var works", newVar != null);
		assertThat("random velocity var works : " + newVar + " == " + tryVar, newVar.equals(tryVar));

		tryVar = "News";
		newVar = contentTypeApi.suggestVelocityVar(tryVar);
		assertThat("existing velocity var will not work", !newVar.equals(tryVar));
	}

	private void testDeleting() throws Exception {
		List<ContentType> types =
				contentTypeApi.search("velocity_var_name like 'velocityVarNameTesting%'", BaseContentType.ANY, "mod_date", -1, 0);
		assertThat(types + " search is working", types.size() > 0);
		for (ContentType type : types) {
			delete(type);
		}

	}

	private void testUpdating() throws Exception {
		List<ContentType> types =
				contentTypeApi.search("velocity_var_name like 'velocityVarNameTesting%'", BaseContentType.ANY, "mod_date", -1, 0);
		assertThat(types + " search is working", types.size() > 0);
		for (ContentType type : types) {
			ContentType testing = contentTypeApi.find(type.id());
			assertThat("contenttype is in db", testing.equals(type));
			ContentTypeBuilder builder = ContentTypeBuilder.builder(type);

			builder.host(Constants.DEFAULT_HOST);
			builder.folder(Constants.ABOUT_US_FOLDER);

			if (type instanceof UrlMapable) {
				builder.urlMapPattern("/asdsadsadsad/");
				builder.detailPage("asdadsad");

			}
			if (type instanceof Expireable) {
				builder.publishDateVar("/asdsadsadsad/");
			}
			builder.description("new description");
			builder.variable(type.variable() + "plus");

			type = contentTypeApi.save(builder.build());

			try {
				testing = contentTypeApi.find(type.id());
				assertThat("Type is updated", testing.equals(type));
			} catch (Throwable t) {
				System.out.println("Old and New Contentyypes are NOT the same");
				System.out.println(type);
				System.out.println(testing);
				throw t;
			}
		}
	}

	private void delete(ContentType type) throws Exception {

		ContentType test1 = contentTypeApi.find(type.id());
		assertThat("factory find works", test1.equals(type));
		Exception e = null;
		try {
			contentTypeApi.delete(type);
			test1 = contentTypeApi.find(type.id());
		} catch (Exception e2) {
			e = e2;
			if (!(e instanceof NotFoundInDbException))
				throw e;
		}
		assertThat("Type is not found after delete", e instanceof NotFoundInDbException);
	}

	private void addFields(ContentType type) throws Exception {

		long time = System.currentTimeMillis();
		String TEST_VAR_PREFIX = "testField";

		int numFields = 0;
		for (Class clazz : APILocator.getContentTypeFieldAPI().fieldTypes()) {
			Field fakeField = FieldBuilder.builder(clazz).name("fake").variable("fake").contentTypeId(type.id()).build();
			boolean save = true;
			if (fakeField instanceof OnePerContentType) {
				for (Field field : type.fields()) {
					if (field.getClass().equals(fakeField.getClass())) {
						save = false;
						break;
					}
				}
			}
			if (!save)
				continue;
			for (DataTypes dt : fakeField.acceptedDataTypes()) {
				if(fakeField instanceof OnePerContentType){
				Field savedField = FieldBuilder.builder(clazz).name("test field" + numFields)
						.variable(TEST_VAR_PREFIX + "textField" + numFields).contentTypeId(type.id()).dataType(dt).build();
				APILocator.getContentTypeFieldAPI().save(savedField, APILocator.systemUser());
				numFields++;
				break;
				}
			}
		}
	}
	
	/**
	 * This test create a Content type with fixed fields, update some fields and delete the content type
	 * @throws Exception
	 */
	@Test
	public void testAddingUpdatingDeletingContentTypeWithFixedFields() throws Exception{
		
		int count = contentTypeApi.count();
		String TEST_VAR_PREFIX = "myTestField";
		
		long time = System.currentTimeMillis();
		int base = BaseContentType.WIDGET.ordinal();
		Thread.sleep(1);
		ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(base))
					.description("description" + time).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
					.name("ContentTypeTestingWithFixedFields" + time).owner("owner").variable("velocityVarNameTesting" + time).build();
		type = contentTypeApi.save(type, null, null);
		
		int count2 = contentTypeApi.count();
		assertThat("contenttypes are added", count == count2 - 1);
		type = contentTypeApi.find(type.id());
		assertThat("Content type found", type != null && StringUtils.isNotEmpty(type.id()) );
		
		//Add Field
		List<Field> fields = type.fields();
		int fieldsCount = fields.size();
		Field savedField = FieldBuilder.builder(WysiwygField.class).name("my test field")
				.variable(TEST_VAR_PREFIX + "textField").contentTypeId(type.id()).dataType(DataTypes.LONG_TEXT).build();
		APILocator.getContentTypeFieldAPI().save(savedField, APILocator.systemUser());
		type = contentTypeApi.find(type.id());
		List<Field> newFields = type.fields();
		
		int fieldsCount2 = newFields.size();
		assertThat("contenttypes field added", fieldsCount < fieldsCount2);
		
		//remove field
		contentTypeApi.save(type, fields);
		type = contentTypeApi.find(type.id());
		fieldsCount2 = type.fields().size();
		assertThat("contenttypes field removed", fieldsCount == fieldsCount2);
		
		//deleting content type
		delete(type);
	}

    /**
     * Test the updateModDate method of the contenttypeapi
     * to help detect the changes on fields and field variables
     * @throws Exception
     */
    @Test
    public void testUpdateContentTypeModDate() throws Exception{
        long time = System.currentTimeMillis();
        String TEST_VAR_PREFIX = "myTestField";
        String TEST_FIELD_VAR_PREFIX = "myTestFieldVar";
        String TEST_FIELD_VAR_VALUE_PREFIX = "myTestFieldVar";
        int base = BaseContentType.CONTENT.ordinal();

        Thread.sleep(1);
        ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(base))
                .description("description" + time).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                .name("ContentTypeTestingUpdateModDate" + time).owner("owner").variable("velocityVarNameTesting" + time).build();
        type = contentTypeApi.save(type, null, null);

        int fieldsCount = type.fields().size();
        Date creationModDate = type.modDate();
        assertThat("contenttypes mod_date is not null", creationModDate != null);
        //calling updatemod_date method
        Thread.sleep(1000);
        contentTypeApi.updateModDate(type);
        //getting new mod_date
        type = contentTypeApi.find(type.id());
        Date currentModDate = type.modDate();
        assertThat("contenttypes current mod_date is not null", currentModDate != null);
        assertThat("contenttypes mod_date is updated", creationModDate != currentModDate);
        assertThat("contenttypes mod_date is updated", currentModDate.compareTo(creationModDate) > 0);

        //Test Content Type mod_date changes after adding a Field
        Thread.sleep(1000);
        Field savedField = FieldBuilder.builder(WysiwygField.class).name("my test field")
                .variable(TEST_VAR_PREFIX + "textField").contentTypeId(type.id()).dataType(DataTypes.LONG_TEXT).build();
        savedField = APILocator.getContentTypeFieldAPI().save(savedField, APILocator.systemUser());
        type = contentTypeApi.find(type.id());
        int updatedFieldsCount = type.fields().size();
        Date addFieldDate = type.modDate();
        assertThat("contenttypes current mod_date is not null", addFieldDate != null);
        assertThat("contenttypes mod_date is updated", addFieldDate != currentModDate);
        assertThat("contenttypes mod_date is updated after add Field", addFieldDate.compareTo(currentModDate) > 0);
        assertThat("contenttypes fields incremented", updatedFieldsCount > fieldsCount);

        //Test Content Type mod_date changes after  edit Field
        Thread.sleep(1000);
        savedField = FieldBuilder.builder(savedField).indexed(true).build();
        savedField = APILocator.getContentTypeFieldAPI().save(savedField, APILocator.systemUser());
        type = contentTypeApi.find(type.id());
        Date editFieldDate = type.modDate();
        int updatedFieldsCount2 = type.fields().size();
        assertThat("contenttypes current mod_date is not null", editFieldDate != null);
        assertThat("contenttypes mod_date is updated", editFieldDate != addFieldDate);
        assertThat("contenttypes mod_date is updated after edit Field", editFieldDate.compareTo(addFieldDate) > 0);
        assertThat("contenttypes fields are the same", updatedFieldsCount == updatedFieldsCount2);

        //Test Content Type mod_date changes after adding a Field Variable
        Thread.sleep(1000);
        FieldVariable savedFieldVar = ImmutableFieldVariable.builder().id(null)
                .fieldId(savedField.id()).name(TEST_FIELD_VAR_PREFIX+time)
                .key(TEST_FIELD_VAR_PREFIX+time).value(TEST_FIELD_VAR_VALUE_PREFIX+time)
                .userId(APILocator.systemUser().getUserId()).modDate(new Date()).build();
        savedFieldVar = APILocator.getContentTypeFieldAPI().save(savedFieldVar, APILocator.systemUser());
        type = contentTypeApi.find(type.id());
        Date addFieldVariableDate = type.modDate();
        assertThat("contenttypes current mod_date is not null", addFieldVariableDate != null);
        assertThat("contenttypes mod_date is updated", addFieldVariableDate != editFieldDate);
        assertThat("contenttypes mod_date is updated after add Field Variable", addFieldVariableDate.compareTo(editFieldDate) > 0);
        assertThat("Field Variable is added ",APILocator.getContentTypeFieldAPI().find(savedField.id()).fieldVariables().size() == 1);

        //Test Content Type mod_date changes after editing a Field Variable
        Thread.sleep(1000);
        savedFieldVar = ImmutableFieldVariable.builder().id(savedFieldVar.id())
                .fieldId(savedField.id()).name(TEST_FIELD_VAR_PREFIX+time)
                .key(TEST_FIELD_VAR_PREFIX+time).value(TEST_FIELD_VAR_VALUE_PREFIX+(time+1))
                .userId(APILocator.systemUser().getUserId()).modDate(new Date()).build();
        savedFieldVar = APILocator.getContentTypeFieldAPI().save(savedFieldVar, APILocator.systemUser());
        type = contentTypeApi.find(type.id());
        Date editFieldVariableDate = type.modDate();
        assertThat("contenttypes current mod_date is not null", editFieldVariableDate != null);
        assertThat("contenttypes mod_date is updated", editFieldVariableDate != addFieldVariableDate);
        assertThat("contenttypes mod_date is updated", editFieldVariableDate.compareTo(addFieldVariableDate) > 0);
        assertThat("Field Variable is updated ",APILocator.getContentTypeFieldAPI().find(savedField.id()).fieldVariables().size() == 1);
        assertThat("Field Variable was updated properly",APILocator.getContentTypeFieldAPI().find(savedField.id()).fieldVariables().get(0).value().equals(TEST_FIELD_VAR_VALUE_PREFIX+(time+1)));

        //Test Content Type mod_date changes after deleting a Field Variable
        Thread.sleep(1000);
        APILocator.getContentTypeFieldAPI().delete(savedFieldVar);
        type = contentTypeApi.find(type.id());
        Date deleteFieldVarDate = type.modDate();
        updatedFieldsCount = type.fields().size();
        assertThat("contenttypes current mod_date is not null", deleteFieldVarDate != null);
        assertThat("contenttypes mod_date is updated", deleteFieldVarDate != editFieldVariableDate);
        assertThat("contenttypes mod_date is updated after delete Field Variable", deleteFieldVarDate.compareTo(editFieldVariableDate) > 0);
        assertThat("Field Variable is removed ",APILocator.getContentTypeFieldAPI().find(savedField.id()).fieldVariables().size() == 0);

        //Test Content Type mod_date changes after deleting a Field
        Thread.sleep(1000);
        APILocator.getContentTypeFieldAPI().delete(savedField);
        type = contentTypeApi.find(type.id());
        Date deleteFieldDate = type.modDate();
        updatedFieldsCount = type.fields().size();
        assertThat("contenttypes current mod_date is not null", deleteFieldDate != null);
        assertThat("contenttypes mod_date is updated", deleteFieldDate != deleteFieldVarDate);
        assertThat("contenttypes mod_date is updated after delete Field", deleteFieldDate.compareTo(deleteFieldVarDate) > 0);
        assertThat("contenttypes field removed", updatedFieldsCount == fieldsCount);
        //deleting content type
        delete(type);
    }

	/**
	 * Creates a Content Type with a fixed field and then tries to update it with a field with same VarName and DBColumn but different ID.
	 * @throws Exception
	 */
	@Test
	public void testUpdatingContentTypeWithFixedFieldsDifferentFieldID() throws Exception{

        int base = BaseContentType.WIDGET.ordinal();
        long time = System.currentTimeMillis();

        final String FIRST_UUID = UUID.randomUUID().toString();
        final String FIRST_NAME = "My Fixed Field";
        final String SECOND_UUID = UUID.randomUUID().toString();
        final String SECOND_NAME = "My Fixed Field Updated";

	    ContentType contentType = ContentTypeBuilder
                .builder(BaseContentType.getContentTypeClass(base))
                .description("Description" + time)
                .folder(FolderAPI.SYSTEM_FOLDER)
                .host(Host.SYSTEM_HOST)
                .name("ContentTypeWithFixedFieldsDifferentFieldID" + time)
                .owner("Me")
                .variable("CTVariable" + time)
                .build();
        contentType = contentTypeApi.save(contentType);

        assertThat("ContentType exists", contentTypeApi.find( contentType.inode() ) != null);

        //Add Field.
        List<Field> fields = new ArrayList<>( contentType.fields() );
        List<Field> originalFields = new ArrayList<>( fields );

        int originalFieldSize = fields.size();

        final String TEST_VAR_NAME = "myFixedVarName";

        Field fieldToSave = FieldBuilder.builder( TextField.class )
                .name( FIRST_NAME )
                .variable( TEST_VAR_NAME )
                .contentTypeId( contentType.id() )
                .dataType( DataTypes.TEXT )
                .fixed( true )
                .dbColumn( "text15" )
                .id( FIRST_UUID )
                .build();

        fields.add( fieldToSave );

        contentType = contentTypeApi.save( contentType, fields );

        //Lets check that the Field was added.
        Field fieldFound = null;
        for ( Field field : contentType.fields() ) {
            if ( field.id().equals( FIRST_UUID ) ){
                fieldFound = field;
            }
        }
        Assert.assertNotNull( fieldFound );
        Assert.assertEquals( FIRST_NAME, fieldFound.name() );

        Field fieldToSaveDifferentID = FieldBuilder.builder( TextField.class )
                .name( SECOND_NAME )
                .variable( TEST_VAR_NAME )
                .contentTypeId( contentType.id() )
                .dataType( DataTypes.TEXT )
                .fixed( true )
                .dbColumn( "text15" )
                .id( SECOND_UUID )
                .build();

        originalFields.add( fieldToSaveDifferentID );

        contentType = contentTypeApi.save( contentType, originalFields );

        //Lets check that the Field was updated.
        fieldFound = null;
        for ( Field field : contentType.fields() ) {
            if ( field.id().equals( FIRST_UUID ) ){
                fieldFound = field;
            }
        }
        Assert.assertNotNull( fieldFound );
        Assert.assertEquals( SECOND_NAME, fieldFound.name() );

		//Deleting content type.
		delete(contentType);
	}
	
	/*
	 * Github: https://github.com/dotCMS/core/issues/11861
	 * 
	 * Creates a Widget and a couple of DateTimeFields (Publish and Expire) and set it as Publish and Expire properties in the Content Type.
	 */
	@Test
	public void testWidgetContentTypeWithPublishExpireFields() throws Exception{
		int base = BaseContentType.WIDGET.ordinal();
		createContentTypeWithPublishExpireFields(base);
        
	}
	
	/*
	 * Github: https://github.com/dotCMS/core/issues/11861
	 * 
	 * Creates a Page and a couple of DateTimeFields (Publish and Expire) and set it as Publish and Expire properties in the Content Type.
	 */
	@Test
	public void testPageContentTypeWithPublishExpireFields() throws Exception{
		int base = BaseContentType.HTMLPAGE.ordinal();
		createContentTypeWithPublishExpireFields(base);
	}
	
	/*
	 * Github: https://github.com/dotCMS/core/issues/11861
	 * 
	 * Creates a File and a couple of DateTimeFields (Publish and Expire) and set it as Publish and Expire properties in the Content Type.
	 */
	@Test
	public void testFileContentTypeWithPublishExpireFields() throws Exception{
		int base = BaseContentType.FILEASSET.ordinal();
		createContentTypeWithPublishExpireFields(base);
        
	}
	
	/*
	 * Github: https://github.com/dotCMS/core/issues/11861
	 * 
	 * Creates a Form and a couple of DateTimeFields (Publish and Expire) and set it as Publish and Expire properties in the Content Type.
	 */
	@Test
	public void testFormContentTypeWithPublishExpireFields() throws Exception{
		int base = BaseContentType.FORM.ordinal();
		createContentTypeWithPublishExpireFields(base);
        
	}
	
	/*
	 * Github: https://github.com/dotCMS/core/issues/11861
	 * 
	 * Creates a Persona and a couple of DateTimeFields (Publish and Expire) and set it as Publish and Expire properties in the Content Type.
	 */
	@Test
	public void testPersonaContentTypeWithPublishExpireFields() throws Exception{
		int base = BaseContentType.PERSONA.ordinal();
        createContentTypeWithPublishExpireFields(base);
	}
	
	private void createContentTypeWithPublishExpireFields(int base) throws Exception{
		long time = System.currentTimeMillis();

		ContentType contentType = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(base))
				.description("ContentTypeWithPublishExpireFields " + time).folder(FolderAPI.SYSTEM_FOLDER)
				.host(Host.SYSTEM_HOST).name("ContentTypeWithPublishExpireFields " + time)
				.owner(APILocator.systemUser().toString()).variable("CTVariable").publishDateVar("publishDate")
				.expireDateVar("expireDate").build();
		contentType = contentTypeApi.save(contentType);

		assertThat("ContentType exists", contentTypeApi.find(contentType.inode()) != null);

		List<Field> fields = new ArrayList<>(contentType.fields());

		Field fieldToSave = FieldBuilder.builder(DateTimeField.class).name("Publish Date").variable("publishDate")
				.contentTypeId(contentType.id()).dataType(DataTypes.DATE).indexed(true).build();
		fields.add(fieldToSave);

		fieldToSave = FieldBuilder.builder(DateTimeField.class).name("Expire Date").variable("expireDate")
				.contentTypeId(contentType.id()).dataType(DataTypes.DATE).indexed(true).build();
		fields.add(fieldToSave);

		contentType = contentTypeApi.save(contentType, fields);

		// Deleting content type.
		delete(contentType);
	}
}
