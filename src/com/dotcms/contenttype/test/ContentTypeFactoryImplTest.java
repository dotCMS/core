package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFormContentType;
import com.dotcms.contenttype.model.type.ImmutablePageContentType;
import com.dotcms.contenttype.model.type.ImmutablePersonaContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.dotcms.contenttype.transform.contenttype.FromStructureTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ContentTypeFactoryImplTest {

	final ContentTypeFactory factory = new ContentTypeFactoryImpl();

	@BeforeClass
	public static void initDb() throws DotDataException, Exception {

		DotConnect dc = new DotConnect();
		String structsToDelete = "(select inode from structure where structure.velocity_var_name like 'velocityVarNameTesting%' )";

		dc.setSQL("delete from field where structure_inode in " + structsToDelete);
		dc.loadResult();

		dc.setSQL("delete from inode where type='field' and inode not in  (select inode from field)");
		dc.loadResult();

		dc.setSQL("delete from contentlet_version_info where identifier in (select identifier from contentlet where structure_inode in "
				+ structsToDelete + ")");
		dc.loadResult();

		dc.setSQL("delete from contentlet where structure_inode in " + structsToDelete);
		dc.loadResult();

		dc.setSQL("delete from inode where type='contentlet' and inode not in  (select inode from contentlet)");
		dc.loadResult();

		dc.setSQL("delete from structure where  structure.velocity_var_name like 'velocityVarNameTesting%' ");
		dc.loadResult();

		dc.loadResult();
		dc.setSQL("delete from inode where type='structure' and inode not in  (select inode from structure)");
		dc.loadResult();

		dc.setSQL("delete from field where structure_inode not in (select inode from structure)");
		dc.loadResult();

		dc.setSQL("delete from inode where type='field' and inode not in  (select inode from field)");
		dc.loadResult();

		dc.setSQL("update structure set structure.url_map_pattern =null, structure.page_detail=null where structuretype =3");
		dc.loadResult();

	}

	@Test
	public void testDifferentContentTypes() throws Exception {

		ContentType content = factory.find(Constants.CONTENT);
		ContentType news = factory.find(Constants.NEWS);
		ContentType widget = factory.find(Constants.WIDGET);
		ContentType form = factory.find(Constants.FORM);
		ContentType fileAsset = factory.find(Constants.FILEASSET);
		ContentType htmlPage = factory.find(Constants.HTMLPAGE);
		ContentType persona = factory.find(Constants.PERSONA);

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
		List<ContentType> types = factory.findAll();
		for (ContentType type : types) {
			ContentType contentType = factory.find(type.inode());
			ContentType contentType2 = factory.findByVar(type.velocityVarName());
			try {
				assertThat("ContentType == ContentType2", contentType.equals(contentType2) && contentType.equals(type));
			} catch (Throwable t) {

				throw t;
			}
		}
	}
	@Test
	public void testFindUrlMapped() throws Exception {
		List<ContentType> types = factory.findUrlMapped();
		assertThat("findUrlMapped only returns urlmapped content", types.size()>0);
		for(ContentType type : types){
			assertThat("findUrlMapped only returns urlmapped content", type.urlMapPattern()!=null);
		}
		
	}
	
	
	@Test
	public void testFindAll() throws Exception {
		List<ContentType> types = factory.findAll();
		assertThat("findAll sort by Name has same size as find all", factory.findAll("name").size() == types.size());
	}

	@Test
	public void testFieldsMethod() throws Exception {

		Cache<String, ContentType> cacheTest = CacheBuilder.newBuilder().maximumSize(100).build();

		ContentType type = factory.find(Constants.NEWS);
		cacheTest.put(type.inode(), type);
		System.out.println(type);

		ContentType otherType = cacheTest.getIfPresent(type.inode());

		List<Field> fields = otherType.fields();
		System.out.println(type);
		List<Field> fields2 = APILocator.getFieldAPI2().byContentType(type);
		assertThat("We have fields!", fields.size() > 0 && fields.size() == fields2.size());
		for (int j = 0; j < fields.size(); j++) {
			Field field = fields.get(j);
			Field testField = fields2.get(j);
			assertThat("fields are correct:", field.equals(testField));
		}

	}

	@Test
	public void testSerialization() throws Exception {

		File temp = File.createTempFile("test1", "obj");
		File temp2 = File.createTempFile("test2", "obj");
		ContentType origType = factory.find(Constants.NEWS);
		
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
			assertThat("fields are correct:", field.equals(testField));
		}

	}

	@Test
	public void testLegacyTransform() throws Exception {
		
		Structure st = new Structure();
		List<ContentType> types = factory.findAll("name");
		List<ContentType> oldTypes = new FromStructureTransformer(getCrappyStructures()).asList();

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

		assertThat("findAll sort by Name has same size as find all", factory.findAll("name").size() == types.size());
	}

	@Test
	public void testAddingContentTypes() throws Exception {
		int count = factory.searchCount(null);
		int runs = 20;

		for (int i = 0; i < runs; i++) {
			long time = System.currentTimeMillis() + i;
			int base = (i % 5) + 1;
			Thread.sleep(1);
			ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(base)).description("description" + time)
					.folder(FolderAPI.SYSTEM_FOLDER).host(Constants.SYSTEM_HOST).name("ContentTypeTestingWithFields" + time).owner("owner")
					.velocityVarName("velocityVarNameTesting" + time).build();
			type = factory.save(type);
			addFields(type);
		}
		int count2 = factory.searchCount(null);
		assertThat("contenttypes are added", count == count2 - runs);
	}

	
	@Test 
	public void testDefaultType() throws DotDataException{
		ContentType type = factory.findDefaultType();
		assertThat("we have a default content type", type !=null);

	}
	
	
	
	@Test
	public void testSearch() throws Exception {
		String[] searchTerms = { Constants.NEWS, "news", "content", "structuretype = 2", " And structure.inode='" + Constants.NEWS + "'" };

		int totalCount = factory.searchCount(null);

		List<ContentType> types;
		assertThat("we have at least 40 content types", factory.search(null, BaseContentType.ANY, "name", 0, 100).size() > 40);
		types = factory.search(null, BaseContentType.ANY, "name", 0, 5);
		assertThat("limit works and we have max five content types", types.size() < 6);
		for (int x = 0; x < totalCount; x = x + 5) {
			types = factory.search(null, BaseContentType.ANY, "name", x, 5);
			assertThat("we have max five content types", types.size() < 6);
		}

		for (int i = 0; i < BaseContentType.values().length; i++) {
			types = factory.search(null, BaseContentType.getBaseContentType(i), "name", 0, 1000);
			assertThat("we have content types of" + BaseContentType.getBaseContentType(i), types.size() > 0);
			int count = factory.searchCount(null,  BaseContentType.getBaseContentType(i));
			assertThat("Count works as well", types.size() == count);
		}

		for (int i = 0; i < searchTerms.length; i++) {
			types = factory.search(searchTerms[i], BaseContentType.ANY, "mod_date desc", 0, 1000);
			assertThat("we can search content types:" + searchTerms[i], types.size() > 0);
			int count = factory.searchCount(searchTerms[i],  BaseContentType.ANY);
			assertThat("Count works as well", types.size() == count);
		}

	}




	@Test
	public void testAdding() throws Exception {

		for(BaseContentType baseType: BaseContentType.values()){
			if(baseType == BaseContentType.ANY)continue;
			int countAll = factory.searchCount(null);
			int runs = 20;
			int countBaseType = factory.searchCount(null, baseType);
	
			for (int i = 0; i < runs; i++) {
				add(baseType);
				Thread.sleep(1);
			}
	
			int countAll2 = factory.searchCount(null);
			int countBaseType2 = factory.searchCount(null,baseType);
			assertThat("counts are working", countAll == countAll2 - runs);
			assertThat("counts are working", countAll2 > countBaseType2);
			assertThat("counts are working", countBaseType == countBaseType2 - runs);
	
			testDeleting(baseType);
		}
	}
	
	
	

	public void testDeleting(BaseContentType baseType) throws Exception {
		

			
			List<ContentType> types = factory.search("velocity_var_name like 'velocityVarNameTesting%'", baseType, "mod_date", 0, 100);
			assertThat(baseType +" search is working", types.size() > 0);
			for(ContentType type : types){
				ContentType test1 = factory.find(type.inode());
				assertThat("factory find works", test1.equals(type) );
				factory.delete(type);
				try{
					test1 = factory.find(type.inode());
				}
				catch(NotFoundInDbException e){
					assertThat("Type is not found after delete", e instanceof NotFoundInDbException);
				}
			}

	}
	

	
	public void add(BaseContentType baseType) throws DotDataException {

		long i = System.currentTimeMillis();
		

		ContentType type = ContentTypeBuilder.builder(baseType.immutableClass())
				.description("description" + i)
				.expireDateVar(null)
				.folder(FolderAPI.SYSTEM_FOLDER)
				.host(Constants.SYSTEM_HOST)
				.name(baseType.name() + "Testing" + i)
				.owner("owner")
				.velocityVarName("velocityVarNameTesting" + i)
				.build();
		
		type = factory.save(type);

		List<Field> fields = new FieldFactoryImpl().byContentTypeId(type.inode());
		List<Field> baseTypeFields = ContentTypeBuilder.builder(baseType.immutableClass()).name("test").velocityVarName("rewarwa").build().requiredFields();
		assertThat("fields are all added", fields.size() == baseTypeFields.size());

		for (int j = 0; j < fields.size(); j++) {
			Field field = fields.get(j);
			Field baseField = baseTypeFields.get(j);
			assertThat("fields are correct:", field.dataType().equals(baseField.dataType()));
			assertThat("fields are correct:", field.variable().equals(baseField.variable()));
			assertThat("fields are correct:", field.getClass().equals(baseField.getClass()));
			assertThat("fields are correct:", field.name().equals(baseField.name()));
			assertThat("fields are correct:", field.sortOrder() == baseField.sortOrder());
		}
	}
	
	
	
	@Test
	public void searchCount() throws DotDataException {
		String query = " velocity_var_name like '%content%'";
		List<ContentType> types = factory.search(query, 10000);
		
		int count= factory.searchCount(query,BaseContentType.ANY);
		assertThat("we have content type:", types.size()>0);
		assertThat("we have the right content types:", types.size() == count);

	}
	@Test
	public void suggestVelocityVar() throws DotDataException {
		String tryVar = "content" + System.currentTimeMillis();
		String newVar = factory.suggestVelocityVar(tryVar);
		
		assertThat("random velocity var works", newVar!=null);
		assertThat("random velocity var works", newVar.equals(tryVar));
		
		
		tryVar = "news" ;
		newVar = factory.suggestVelocityVar(tryVar);
		assertThat("existing velocity var will not work", !newVar.equals(tryVar));

	}
	
	private static List<Structure> getCrappyStructures(){
		return InodeFactory.getInodesOfClass(Structure.class,"name");
	}
	
	
	

	private void addFields(ContentType type) throws Exception {

		long time = System.currentTimeMillis();
		String TEST_VAR_PREFIX = "testField";

		int numFields = 0;
		for(Class clazz : APILocator.getFieldAPI2().fieldTypes()){
			Field fakeField = FieldBuilder.builder(clazz).name("fake").variable("fake").build();
			boolean save = true;
			if(fakeField.onePerContentType()){
				for(Field field : APILocator.getFieldAPI2().byContentType(type)){
					if(field.getClass().equals(fakeField.getClass())){
						save = false;
						break;
					}
				}
			}
			if(!save) continue;
			for (DataTypes dt : fakeField.acceptedDataTypes()) {
				Field savedField = FieldBuilder.builder(clazz)
					.name("test field" + numFields)
					.variable(TEST_VAR_PREFIX + "textField" + numFields)
					.contentTypeId(type.inode())
					.dataType(dt)
					.build();
				APILocator.getFieldAPI2().save(savedField);
				numFields++;
			}
		}
	}
}
