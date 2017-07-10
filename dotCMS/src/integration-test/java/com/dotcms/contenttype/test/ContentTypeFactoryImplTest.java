package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.OnePerContentType;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.Expireable;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFormContentType;
import com.dotcms.contenttype.model.type.ImmutablePageContentType;
import com.dotcms.contenttype.model.type.ImmutablePersonaContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.dotcms.contenttype.model.type.UrlMapable;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Structure;

public class ContentTypeFactoryImplTest extends ContentTypeBaseTest {

	@Test
	public void testDifferentContentTypes() throws Exception {

		ContentType content 	= contentTypeFactory.find(Constants.CONTENT);
		ContentType news 		= contentTypeFactory.find(Constants.NEWS);
		ContentType widget 		= contentTypeFactory.find(Constants.WIDGET);
		ContentType form 		= contentTypeFactory.find(Constants.FORM);
		ContentType fileAsset 	= contentTypeFactory.find(Constants.FILEASSET);
		ContentType htmlPage 	= contentTypeFactory.find(Constants.HTMLPAGE);
		ContentType persona 	= contentTypeFactory.find(Constants.PERSONA);

		// Test all the types
		assertThat("ContentType is type Content", content.baseType() == BaseContentType.CONTENT);
		assertThat("ContentType is type Content", content instanceof ImmutableSimpleContentType);
		assertThat("News is not simple content", !news.equals(content));

		assertThat("ContentType is type FILEASSET", fileAsset.baseType() == BaseContentType.FILEASSET);
		assertThat("ContentType is type FILEASSET", fileAsset instanceof ImmutableFileAssetContentType);

		assertThat("ContentType is type WIDGET", widget.baseType() == BaseContentType.WIDGET);
		assertThat("ContentType is type WIDGET", widget instanceof ImmutableWidgetContentType);

		assertThat("ContentType is type FORM", form.baseType() == BaseContentType.FORM);
		assertThat("ContentType is type FORM", form instanceof ImmutableFormContentType);

		assertThat("ContentType is type PERSONA", persona.baseType() == BaseContentType.PERSONA);
		assertThat("ContentType is type PERSONA", persona instanceof ImmutablePersonaContentType);

		assertThat("ContentType is type HTMLPAGE", htmlPage.baseType() == BaseContentType.HTMLPAGE);
		assertThat("ContentType is type HTMLPAGE", htmlPage instanceof ImmutablePageContentType);

	}

	@Test
	public void testFindMethodEquals() throws Exception {
		List<ContentType> types = contentTypeFactory.findAll();
		for (ContentType type : types) {
			ContentType contentType = contentTypeFactory.find(type.id());
			ContentType contentType2 = contentTypeFactory.find(type.variable());
			try {
				assertThat("ContentType == ContentType2", contentType.equals(contentType2) && contentType.equals(type));
			} catch (Throwable t) {

				throw t;
			}
		}
	}
	@Test
	public void testFindUrlMapped() throws Exception {
		List<ContentType> types = contentTypeFactory.findUrlMapped();
		assertThat("findUrlMapped only returns urlmapped content", types.size()>0);
		for(ContentType type : types){
			assertThat("findUrlMapped only returns urlmapped content", type.urlMapPattern()!=null);
		}

	}


	@Test
	public void testFindAll() throws Exception {
		List<ContentType> types = contentTypeFactory.findAll();
		assertThat("findAll sort by Name has same size as find all", contentTypeFactory.findAll("name").size() == types.size());
	}

	@Test
	public void testFieldsMethod() throws Exception {

		ContentType type = contentTypeFactory.find(Constants.NEWS);

		//System.out.println(type);
		ContentType otherType = contentTypeFactory.find(Constants.NEWS);

		List<Field> fields = otherType.fields();
		//System.out.println(type);
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
		ContentType origType = contentTypeFactory.find(Constants.NEWS);

		try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(temp))){
			oos.writeObject(origType);
			oos.close();
		}

		temp.renameTo(temp2);
		ContentType fromDisk = null;
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(temp2))){
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
			try{
				assertThat("fields are correct:", field.equals(testField));
			} catch (Throwable t) {
				System.out.println("Old and New fields are NOT the same");
				System.out.println(field);
				System.out.println(testField);
				throw t;
			}
		}

	}

	/*
	@Test
	public void testLegacyTransform() throws Exception {

		Structure st = new Structure();
		List<ContentType> types = contentTypeFactory.findAll("name");
		List<ContentType> oldTypes = new StructureTransformer(getCrappyStructures()).asList();

		assertThat("findAll and legacy return same quantity", types.size() == oldTypes.size());

		for (int i = 0; i < types.size(); i++) {
			try {
				assertThat("Old and New Contentyypes are the same", types.get(i).equals(oldTypes.get(i)));
			} catch (Throwable t) {
				System.out.println("Old and New Contentyypes are NOT the same");
				System.out.println(types.get(i));
				System.out.println(oldTypes.get(i));
				throw t;
			}
		}

		assertThat("findAll sort by Name has same size as find all", contentTypeFactory.findAll("name").size() == types.size());
	}
	*/

	@Test
	public void testAddingContentTypes() throws Exception {
		int count = contentTypeFactory.searchCount(null);
		int runs = 20;

		for (int i = 0; i < runs; i++) {
			long time = System.currentTimeMillis() + i;
			int base = (i % 5) + 1;
			Thread.sleep(1);
			ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(base)).description("description" + time)
					.folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("ContentTypeTestingWithFields" + time).owner("owner")
					.variable("velocityVarNameTesting" + time).build();
			type = contentTypeFactory.save(type);
			addFields(type);
		}
		int count2 = contentTypeFactory.searchCount(null);
		assertThat("contenttypes are added", count == count2 - runs);
	}

	@Test
	public void testUpdatingContentTypes() throws Exception {
		List<ContentType> types = contentTypeFactory.findUrlMapped();
		assertThat("findUrlMapped only returns urlmapped content", types.size()>0);
		for(ContentType type : types){
			assertThat("findUrlMapped only returns urlmapped content", type.urlMapPattern()!=null);
		}

	}

	@Test 
	public void testDefaultType() throws DotDataException{
		ContentType type = contentTypeFactory.findDefaultType();
		assertThat("we have a default content type", type !=null);

	}

	@Test
	public void testSearch() throws Exception {
		String[] searchTerms = { Constants.NEWS, "structuretype = 2", " And structure.inode='" + Constants.NEWS + "'" };

		int totalCount = contentTypeFactory.searchCount(null);

		List<ContentType> types=contentTypeFactory.search(null, BaseContentType.ANY, "name", -1,0);
		assertThat("we have at least 40 content types", types.size() > 20);
		types = contentTypeFactory.search(null, BaseContentType.ANY, "name", 5, 0);
		assertThat("limit works and we have max five content types", types.size() < 6);
		for (int x = 0; x < totalCount; x = x + 5) {
			types = contentTypeFactory.search(null, BaseContentType.ANY, "name",  5, x);
			assertThat("we have max five content types", types.size() < 6);
		}

		for (int i = 0; i < BaseContentType.values().length; i++) {
			types = contentTypeFactory.search(null, BaseContentType.getBaseContentType(i), "name", -1, 0);
			assertThat("we have content types of" + BaseContentType.getBaseContentType(i), types.size() > 0);
			int count = contentTypeFactory.searchCount(null,  BaseContentType.getBaseContentType(i));
			assertThat("Count works as well", types.size() == count);
		}

		for (int i = 0; i < searchTerms.length; i++) {
			types = contentTypeFactory.search(searchTerms[i], BaseContentType.ANY, "mod_date desc", -1, 0);
			assertThat("we can search content types:" + searchTerms[i], types.size() > 0);
			int count = contentTypeFactory.searchCount(searchTerms[i],  BaseContentType.ANY);
			assertThat("Count works as well", types.size() == count);
		}

	}

	@Test
	public void testAddingUpdatingDeleteing() throws Exception {

		for(BaseContentType baseType: BaseContentType.values()){
			if(baseType == BaseContentType.ANY)continue;
			int countAll = contentTypeFactory.searchCount(null);
			int runs = 10;
			int countBaseType = contentTypeFactory.searchCount(null, baseType);

			for (int i = 0; i < runs; i++) {
				insert(baseType,null);
				Thread.sleep(1);
			}

			int countAll2 = contentTypeFactory.searchCount(null);
			int countBaseType2 = contentTypeFactory.searchCount(null,baseType);
			assertThat("counts are working", countAll == countAll2 - runs);
			assertThat("counts are working", countAll2 > countBaseType2);
			assertThat("counts are working", countBaseType == countBaseType2 - runs);


			for (int i = 0; i < runs; i++) {
				insert(baseType,UUID.randomUUID().toString());
				Thread.sleep(1);
			}
			int countAll3 = contentTypeFactory.searchCount(null);
			int countBaseType3 = contentTypeFactory.searchCount(null,baseType);
			assertThat("counts are working", countAll2 == countAll3 - runs);
			assertThat("counts are working", countAll3 > countBaseType3);
			assertThat("counts are working", countBaseType2 == countBaseType3 - runs);

		}

		testUpdating();

		testDeleting() ;
	}

	@Test
	public void searchCount() throws DotDataException {
		String query = " velocity_var_name like '%content%'";
		List<ContentType> types = contentTypeFactory.search(query, -1);

		int count= contentTypeFactory.searchCount(query,BaseContentType.ANY);
		assertThat("we have the right content types:", types.size() == count);

	}

	@Test
	public void suggestVelocityVar() throws DotDataException {
		String tryVar = "Content" + System.currentTimeMillis();
		String newVar = contentTypeFactory.suggestVelocityVar(tryVar);

		assertThat("random velocity var works", newVar!=null);
		assertThat("random velocity var works : " + newVar + " == " + tryVar, newVar.equals(tryVar));

		tryVar = "News" ;
		newVar = contentTypeFactory.suggestVelocityVar(tryVar);
		assertThat("existing velocity var will not work", !newVar.equals(tryVar));
	}

	private void testDeleting() throws Exception{
		List<ContentType> types = contentTypeFactory.search("velocity_var_name like 'velocityVarNameTesting%'", BaseContentType.ANY, "mod_date", -1, 0);
		assertThat(types +" search is working", types.size() > 0);
		for(ContentType type : types){
			delete(type);
		}

	}

	private void testUpdating() throws Exception {
		List<ContentType> types = contentTypeFactory.search("velocity_var_name like 'velocityVarNameTesting%'", BaseContentType.ANY, "mod_date", -1, 0);
		assertThat(types +" search is working", types.size() > 0);
		for(ContentType type : types){
			ContentType testing = contentTypeFactory.find(type.id());
			assertThat("contenttype is in db", testing.equals(type) );
			ContentTypeBuilder builder = ContentTypeBuilder.builder(type);

			builder.host(Constants.DEFAULT_HOST);
			builder.folder(Constants.ABOUT_US_FOLDER);

			if(type instanceof UrlMapable){
				builder.urlMapPattern("/asdsadsadsad/");
				builder.detailPage("asdadsad");

			}
			if(type instanceof Expireable){
				builder.publishDateVar("/asdsadsadsad/");
			}
			builder.description("new description");
			builder.variable(type.variable() + "plus");

			type=contentTypeFactory.save(builder.build());

			try{
				testing = contentTypeFactory.find(type.id());
				assertThat("Type is updated", testing.equals(type));
			}
			catch(Throwable t){
				System.out.println("Old and New Contentyypes are NOT the same");
				System.out.println(type);
				System.out.println(testing);
				throw t;
			}
		}
	}

	private void delete(ContentType type) throws Exception {

		ContentType test1 = contentTypeFactory.find(type.id());
		assertThat("factory find works", test1.equals(type) );
		Exception e=null;
		try{
			contentTypeFactory.delete(type);
			test1 = contentTypeFactory.find(type.id());
		}
		catch(Exception e2){
			e=e2;
			if(!(e instanceof NotFoundInDbException)) throw e;
		}
		assertThat("Type is not found after delete", e instanceof NotFoundInDbException);
	}

	private void insert(BaseContentType baseType, String inode) throws DotDataException {

		long i = System.currentTimeMillis();


		ContentTypeBuilder builder =ContentTypeBuilder.builder(baseType.immutableClass())
				.description("description" + i)
				.expireDateVar(null)
				.folder(FolderAPI.SYSTEM_FOLDER)
				.host(Host.SYSTEM_HOST)
				.name(baseType.name() + "Testing" + i)
				.owner("owner")
				.variable("velocityVarNameTesting" + i);


		ContentType type = builder.build(); 
		type = contentTypeFactory.save(type);

		ContentType type2 = contentTypeFactory.find(type.id());
		try{
			assertThat("Type saved correctly", type2.equals(type));
		}
		catch(Throwable t){
			System.out.println("Old and New Contentyypes are NOT the same");
			System.out.println(type);
			System.out.println(type2);
			throw t;
		}
		List<Field> fields = new FieldFactoryImpl().byContentTypeId(type.id());
		List<Field> baseTypeFields = ContentTypeBuilder.builder(baseType.immutableClass()).name("test").variable("rewarwa").build().requiredFields();
		try {
			assertThat("fields are all added:\n" + fields + "\n" + baseTypeFields, fields.size() == baseTypeFields.size());
		}
		catch(Throwable e){
			System.out.println(e.getMessage());
			System.out.println("Saved  db: " + fields);
			System.out.println("not saved: " + baseTypeFields);
			System.out.println("\n");
			throw e;

		}
		for (int j = 0; j < fields.size(); j++) {
			Field field = fields.get(j);
			Field baseField = null;
			try{
				baseField = baseTypeFields.get(j);
				assertThat("field datatypes are not correct:", field.dataType().equals(baseField.dataType()));
				assertThat("fields variable is not correct:", field.variable().equals(baseField.variable()));
				assertThat("field class is not correct:", field.getClass().equals(baseField.getClass()));
				assertThat("field name is  not correct:", field.name().equals(baseField.name()));
				assertThat("field sort order is not correct", field.sortOrder() == baseField.sortOrder());
			} 
			catch(Throwable e){
				System.out.println(e.getMessage());
				System.out.println("Saved  db: " + field);
				System.out.println("not saved: " + baseField);
				System.out.println("\n");
				throw e;

			}
		}
	}

	/*
	private static List<Structure> getCrappyStructures(){
		return InodeFactory.getInodesOfClass(Structure.class,"name");
	}
	*/

	private void addFields(ContentType type) throws Exception {

		long time = System.currentTimeMillis();
		String TEST_VAR_PREFIX = "testField";

		int numFields = 0;
		for(Class clazz : APILocator.getContentTypeFieldAPI().fieldTypes()){
			Field fakeField = FieldBuilder.builder(clazz).name("fake").variable("fake").contentTypeId(type.id()).build();
			boolean save = true;
			if(fakeField instanceof OnePerContentType){
				for(Field field : type.fields()){
					if(field.getClass().equals(fakeField.getClass())){
						save = false;
						break;
					}
				}
			}
			if(!save) continue;
			for (DataTypes dt : fakeField.acceptedDataTypes()) {
				if(fakeField instanceof OnePerContentType){
				Field savedField = FieldBuilder.builder(clazz)
						.name("test field" + numFields)
						.variable(TEST_VAR_PREFIX + "textField" + numFields)
						.contentTypeId(type.id())
						.dataType(dt)
						.build();
				APILocator.getContentTypeFieldAPI().save(savedField, APILocator.systemUser());
				numFields++;
				break;
				}
			}
		}
	}
}
