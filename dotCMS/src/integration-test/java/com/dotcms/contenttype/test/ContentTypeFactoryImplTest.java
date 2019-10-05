package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

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
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ContentTypeFactoryImplTest extends ContentTypeBaseTest {

	@Test
	public void testDifferentContentTypes() throws Exception {

		ContentType content 	= contentTypeFactory.find(Constants.CONTENT);
		ContentType news = contentTypeFactory.find(newsLikeContentType.id());
		ContentType widget = TestDataUtils.getWidgetLikeContentType();
		ContentType form = TestDataUtils.getFormLikeContentType();
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
			ContentType contentType1 = contentTypeFactory.find(type.id());
			ContentType contentType2 = contentTypeFactory.find(type.variable());
			try {
				assertThat("testing equals:\ncontentType1:" + contentType1 +"\ncontentType2" + contentType2 + "\ntype in list " + type, contentType1.equals(contentType2) && contentType1.equals(type));
				
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

		ContentType type = contentTypeFactory.find(newsLikeContentType.id());

		//System.out.println(type);
		ContentType otherType = contentTypeFactory.find(newsLikeContentType.id());

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

		try(ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(temp.toPath()))){
			oos.writeObject(newsLikeContentType);
		}

		temp.renameTo(temp2);
		ContentType fromDisk = null;
		try(ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(temp2.toPath()))){
			fromDisk = (ContentType) ois.readObject();
			ois.close();
		}


		try {
			assertThat("fields are correct:", newsLikeContentType.equals(fromDisk));
		} catch (Throwable e) {
			System.out.println("origType" + newsLikeContentType);
			System.out.println("fromDisk" + fromDisk);
			throw e;
		}

		List<Field> fields = newsLikeContentType.fields();
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
		String[] searchTerms = {newsLikeContentType.id(), "structuretype = 2",
				" And structure.inode='" + newsLikeContentType.id() + "'"};

		int totalCount = contentTypeFactory.searchCount(null);
		final List<ContentType> typesCreated = new ArrayList<>();

		for(int i=0; i<5; i++) {
			typesCreated.add(new ContentTypeDataGen().nextPersisted());
		}

		try {

			List<ContentType> types = contentTypeFactory
					.search(null, BaseContentType.ANY, "name", -1, 0);
			assertThat("we have at least 5 content types", types.size() >= 5);
			types = contentTypeFactory.search(null, BaseContentType.ANY, "name", 5, 0);
			assertThat("limit works and we have max five content types", types.size() < 6);
			for (int x = 0; x < totalCount; x = x + 5) {
				types = contentTypeFactory.search(null, BaseContentType.ANY, "name", 5, x);
				assertThat("we have max five content types", types.size() < 6);
			}

			for (int i = 0; i < BaseContentType.values().length; i++) {
				types = contentTypeFactory
						.search(null, BaseContentType.getBaseContentType(i), "name", -1, 0);
				if (!types.isEmpty()) {
					assertThat("we have content types of " + BaseContentType.getBaseContentType(i),
							types.size() > 0);
					int count = contentTypeFactory
							.searchCount(null, BaseContentType.getBaseContentType(i));
					assertThat("Count works as well", types.size() == count);
				} else {
					System.out.println("No data found for BaseContentType: " + BaseContentType
							.getBaseContentType(i));
				}
			}

			for (int i = 0; i < searchTerms.length; i++) {
				types = contentTypeFactory
						.search(searchTerms[i], BaseContentType.ANY, "mod_date desc", -1, 0);
				if (!types.isEmpty()) {
					assertThat("we can search content types:" + searchTerms[i], types.size() > 0);
					int count = contentTypeFactory.searchCount(searchTerms[i], BaseContentType.ANY);
					assertThat("Count works as well", types.size() == count);
				} else {
					System.out.println("No data found for BaseContentType: " + BaseContentType
							.getBaseContentType(i));
				}
			}

		} finally {
			typesCreated.forEach(ContentTypeDataGen::remove);
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
				insert(baseType);
				Thread.sleep(1);
			}

			int countAll2 = contentTypeFactory.searchCount(null);
			int countBaseType2 = contentTypeFactory.searchCount(null,baseType);
			assertThat("counts are working", countAll == countAll2 - runs);
			assertThat("counts are working", countAll2 > countBaseType2);
			assertThat("counts are working", countBaseType == countBaseType2 - runs);


			for (int i = 0; i < runs; i++) {
				insert(baseType);
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

        assertThat("random velocity var works", newVar != null);
        assertThat("random velocity var works : " + newVar + " == " + tryVar,
                newVar.equals(tryVar));

        //Create a test content type
        final ContentType newsLikeContentType = TestDataUtils.getNewsLikeContentType();
        tryVar = newsLikeContentType.variable();
        newVar = contentTypeFactory.suggestVelocityVar(tryVar);
        assertThat("existing velocity var will not work", !newVar.equalsIgnoreCase(tryVar));

        //Velocity var should be case insensitive
        newVar = contentTypeFactory.suggestVelocityVar(tryVar.toUpperCase());
        assertThat("existing velocity var will not work", !newVar.equalsIgnoreCase(tryVar));
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

		Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
		Folder testFolder = new FolderDataGen().site(defaultHost).nextPersisted();

		for(ContentType type : types){
			ContentType testing = contentTypeFactory.find(type.id());
			assertThat("contenttype is in db", testing.equals(type) );
			ContentTypeBuilder builder = ContentTypeBuilder.builder(type);

			builder.host(defaultHost.getIdentifier());
			builder.folder(testFolder.getInode());

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
