package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.ImmutableWysiwygField;
import com.dotcms.contenttype.model.field.OnePerContentType;
import com.dotcms.contenttype.model.field.WysiwygField;
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
import com.dotcms.datagen.*;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.*;

import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.github.rjeschke.txtmark.Run;
import org.junit.Assert;
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

	/**
	 * Given Scenario: A content type is generated and created containing field variables in its fields
	 * ExpectedResult: When iterating the fields at least one should have variables.
	 */
	@Test
	public void testAddingContentType_withFieldVariables() throws Exception {
		long time = System.currentTimeMillis();
		int base = 1;
		Thread.sleep(1);
		ContentType type = ContentTypeBuilder
				.builder(BaseContentType.getContentTypeClass(base))
				.description("description" + time)
				.folder(FolderAPI.SYSTEM_FOLDER)
				.host(Host.SYSTEM_HOST)
				.name("ContentTypeTestingWithFields" + time)
				.owner("owner")
				.variable("velocityVarNameTesting" + time)
				.build();
		type = contentTypeFactory.save(type);
		final List<Class> fieldClasses = List.of(ImmutableWysiwygField.class);
		addFieldsWithVariables(type, fieldClasses);
		type.fields()
				.forEach(field -> {
					if (fieldClasses.contains(field)) {
						assertThat("This field has a field variable", !field.fieldVariables().isEmpty());
					}
				});
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

	private void addFieldsWithVariables(final ContentType type, final List<Class> fieldClasses) throws Exception {
		addFields(type);
		type.fields()
				.forEach(field -> {
					if (fieldClasses.contains(field.getClass())) {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {}

						final long time = System.currentTimeMillis();
						final FieldVariable fieldVariable = ImmutableFieldVariable
								.builder()
								.fieldId(field.id())
								.name(field.name() + time)
								.key("someKey" + time)
								.value("someValue" + time)
								.build();
						try {
							APILocator.getContentTypeFieldAPI().save(fieldVariable, APILocator.systemUser());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
	}

	private void addFields(ContentType type) throws Exception {

		String TEST_VAR_PREFIX = "testField";

		int numFields = 0;
		for(Class<? extends Field> clazz : APILocator.getContentTypeFieldAPI().fieldTypes()){
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

	/***
	 * Creates Content Types and sets values for icon and sort_order
	 *
	 */
	@Test
	public void test_AddingContentType_SetIconAndSortOrder() throws Exception {
		final long currentTimeMilis = System.currentTimeMillis();
		ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.getType())).description("description" + currentTimeMilis)
				.folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("ContentTypeFacTestingIconSortOrder" + currentTimeMilis).owner("owner").icon("testingIcon").sortOrder(2)
				.variable("velocityVarNameTesting" + currentTimeMilis).build();
		type = contentTypeFactory.save(type);
		addFields(type);

		final ContentType contentTypeSearched = contentTypeFactory.find(type.id());
		Assert.assertNotNull(contentTypeSearched);
		assertEquals("testingIcon",contentTypeSearched.icon());
		assertEquals(2,contentTypeSearched.sortOrder());
	}

	/***
	 * Creates Content Types and sets value for icon but no sort_order
	 *
	 */
	@Test
	public void test_AddingContentType_SetIcon() throws Exception {
		final long currentTimeMilis = System.currentTimeMillis();
		ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.getType())).description("description" + currentTimeMilis)
				.folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("ContentTypeFacTestingIcon" + currentTimeMilis).owner("owner").icon("testingIcon")
				.variable("velocityVarNameTesting" + currentTimeMilis).build();
		type = contentTypeFactory.save(type);
		addFields(type);

		final ContentType contentTypeSearched = contentTypeFactory.find(type.id());
		Assert.assertNotNull(contentTypeSearched);
		assertEquals("testingIcon",contentTypeSearched.icon());
		assertEquals(0,contentTypeSearched.sortOrder());
	}

	/***
	 * Creates Content Types and sets value for sort_order but no icon
	 *
	 */
	@Test
	public void test_AddingContentType_SetSortOrder() throws Exception {
		final long currentTimeMilis = System.currentTimeMillis();
		ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.getType())).description("description" + currentTimeMilis)
				.folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("ContentTypeFacTestingSortOrder" + currentTimeMilis).owner("owner").sortOrder(10)
				.variable("velocityVarNameTesting" + currentTimeMilis).build();
		type = contentTypeFactory.save(type);
		addFields(type);

		final ContentType contentTypeSearched = contentTypeFactory.find(type.id());
		Assert.assertNotNull(contentTypeSearched);
		Assert.assertNotNull(contentTypeSearched.icon());
		assertEquals(10,contentTypeSearched.sortOrder());
		assertEquals(BaseContentType.iconFallbackMap.get(contentTypeSearched.baseType()),contentTypeSearched.icon());
	}

	/***
	 * Creates and Updates Content Types and sets values for icon and sort_order
	 *
	 */
	@Test
	public void test_UpdateContentType_SetIconAndSortOrder() throws Exception {
		final long currentTimeMilis = System.currentTimeMillis();
		ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.getType())).description("description" + currentTimeMilis)
				.folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("ContentTypeFacTestingIconSortOrder" + currentTimeMilis).owner("owner").icon("testingIcon").sortOrder(2)
				.variable("velocityVarNameTesting" + currentTimeMilis).build();
		type = contentTypeFactory.save(type);
		addFields(type);

		ContentType contentTypeSearched = contentTypeFactory.find(type.id());
		Assert.assertNotNull(contentTypeSearched);
		assertEquals("testingIcon",contentTypeSearched.icon());
		assertEquals(2,contentTypeSearched.sortOrder());

		final ContentTypeBuilder builder = ContentTypeBuilder.builder(type);
		builder.icon("updatedIcon");
		builder.sortOrder(10);
		contentTypeFactory.save(builder.build());

		contentTypeSearched = contentTypeFactory.find(type.id());
		Assert.assertNotNull(contentTypeSearched);
		assertEquals("updatedIcon",contentTypeSearched.icon());
		assertEquals(10,contentTypeSearched.sortOrder());
	}

	/***
	 * Creates and Updates Content Types and sets value for icon but no sort_order
	 *
	 */
	@Test
	public void test_UpdateContentType_SetIcon() throws Exception {
		final long currentTimeMilis = System.currentTimeMillis();
		ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.getType())).description("description" + currentTimeMilis)
				.folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("ContentTypeFacTestingIcon" + currentTimeMilis).owner("owner").icon("testingIcon")
				.variable("velocityVarNameTesting" + currentTimeMilis).build();
		type = contentTypeFactory.save(type);
		addFields(type);

		ContentType contentTypeSearched = contentTypeFactory.find(type.id());
		Assert.assertNotNull(contentTypeSearched);
		assertEquals("testingIcon",contentTypeSearched.icon());
		assertEquals(0,contentTypeSearched.sortOrder());

		final ContentTypeBuilder builder = ContentTypeBuilder.builder(type);
		builder.icon("updatedIcon");
		contentTypeFactory.save(builder.build());

		contentTypeSearched = contentTypeFactory.find(type.id());
		Assert.assertNotNull(contentTypeSearched);
		assertEquals("updatedIcon",contentTypeSearched.icon());
		assertEquals(0,contentTypeSearched.sortOrder());
	}

	/***
	 * Creates and Updates Content Types and sets value for sort_order but no icon
	 *
	 */
	@Test
	public void test_UpdateContentType_SetSortOrder() throws Exception {
		final long currentTimeMilis = System.currentTimeMillis();
		ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.getType())).description("description" + currentTimeMilis)
				.folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("ContentTypeFacTestingSortOrder" + currentTimeMilis).owner("owner").sortOrder(10).icon("testIcon")
				.variable("velocityVarNameTesting" + currentTimeMilis).build();
		type = contentTypeFactory.save(type);
		addFields(type);

		ContentType contentTypeSearched = contentTypeFactory.find(type.id());
		Assert.assertNotNull(contentTypeSearched);
		assertEquals(10,contentTypeSearched.sortOrder());

		final ContentTypeBuilder builder = ContentTypeBuilder.builder(type);
		builder.sortOrder(1);
		contentTypeFactory.save(builder.build());

		contentTypeSearched = contentTypeFactory.find(type.id());
		Assert.assertNotNull(contentTypeSearched);
		assertEquals(1,contentTypeSearched.sortOrder());
	}

	/**
	 * Method to test: {@link ContentTypeAPIImpl#findUrlMapped(String)}
	 * When: Create a {@link ContentType} with a urlMapPattern and detailPage
	 * and call the method with the page's id
	 * Should: Return the {@link ContentType}
	 */
	@Test
	public void findUrlMappedByPageId() throws DotDataException {
		final Host host = new SiteDataGen().nextPersisted();
		final Template template = new TemplateDataGen().nextPersisted();
		final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

		final ContentType contentType = new ContentTypeDataGen()
				.detailPage(htmlPageAsset.getIdentifier())
				.urlMapPattern("/test")
				.nextPersisted();

		final List<String> urlMapped = FactoryLocator.getContentTypeFactory()
				.findUrlMappedPattern(htmlPageAsset.getIdentifier());

		assertEquals(1, urlMapped.size());
		assertTrue(urlMapped.contains("/test"));
	}

    /**
     * Method to test: {@link ContentTypeAPIImpl#findUrlMapped(String)}
	 * When: Called the method with a Page'id that not have any ContentType link with it
	 * Should: Return an empty list
	 */
     	@Test
	public void findUrlMappedByPageIdWithoutContentType() throws DotDataException {
		final Host host = new SiteDataGen().nextPersisted();
		final Template template = new TemplateDataGen().nextPersisted();
		final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

		final List<String> urlMapped = FactoryLocator.getContentTypeFactory()
				.findUrlMappedPattern(htmlPageAsset.getIdentifier());

		assertTrue(urlMapped.isEmpty());
	}

	/**
	 * Method to test: {@link ContentTypeAPIImpl#findUrlMapped(String)}
	 * when: Called the method with Null
	 * should: Throw a {@link IllegalArgumentException}
	 */
	@Test(expected = IllegalArgumentException.class)
	public void findUrlMappedByPageIdWithNull() throws DotDataException {
		FactoryLocator.getContentTypeFactory().findUrlMappedPattern(null);
	}

	/**
	 * Test the {@link com.dotcms.contenttype.business.ContentTypeFactoryImpl#markForDeletion(ContentType)} method
	 * Wew want to corroborate that the content type is marked for deletion doesn't make it into the search results nor count methods
	 * @throws Exception
	 */
	@Test
	public void Test_Mark_ContentTypeForDeletion() throws Exception {
		final ContentType contentType = new ContentTypeDataGen().nextPersisted();
		int searchCount = contentTypeFactory.searchCount(null);
		List<ContentType> types = contentTypeFactory.search(String.format("velocity_var_name like '%s'",contentType.variable()), BaseContentType.ANY, "mod_date", -1, 0);
		Assert.assertTrue(types.size() > 0);

		ContentType found = contentTypeFactory.find(contentType.variable());
		Assert.assertNotNull(found);

		long count = contentTypeFactory.findAll().stream().filter(ct -> Objects.equals(ct.id(), contentType.id())).count();
		Assert.assertEquals(1, count);

		//MARK FOR DELETION
		contentTypeFactory.markForDeletion(contentType);
		Assert.assertTrue(contentTypeFactory.searchCount(null) <  searchCount );

		//Marking the CT should exclude it from the search results
		types = contentTypeFactory.search(String.format("velocity_var_name like '%s'",contentType.variable()), BaseContentType.ANY, "mod_date", -1, 0);
		Assert.assertTrue(types.isEmpty());

		//Also once marked it must disappear from the list of all content types
		count = contentTypeFactory.findAll().stream().filter(ct -> Objects.equals(ct.id(), contentType.id())).count();
		Assert.assertEquals(0, count);

		//But If we have the id or varName we should still be able to find the content type even it is marked for deletion
		found = contentTypeFactory.find(contentType.variable());
		Assert.assertNotNull(found);

		found = contentTypeFactory.find(contentType.id());
		Assert.assertNotNull(found);
	}

	/**
	 * Method to test: {@link com.dotcms.contenttype.business.ContentTypeFactoryImpl#countContentTypeAssignedToNotSystemWorkflow()}
	 * When: Call the method and after create a new ContentType and assigned it to a not System_Workflow
	 * and call the method again
	 * Should: Got one more the second time when the method is called
	 *
	 * @throws DotDataException
	 */
	@Test
	public void whenContentTypeIsAssignedToNotSystemWorkflow() throws DotDataException {
		final long countBefore = FactoryLocator.getContentTypeFactory()
				.countContentTypeAssignedToNotSystemWorkflow();

		final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();
		final  ContentType contentType = new ContentTypeDataGen().nextPersisted();

		final Set<String> schemesIds = new HashSet<>();
		schemesIds.add(workflowScheme.getId());
		APILocator.getWorkflowAPI().saveSchemeIdsForContentType(contentType, schemesIds);

		final long countAfter = FactoryLocator.getContentTypeFactory()
				.countContentTypeAssignedToNotSystemWorkflow();

		assertEquals(countBefore + 1, countAfter);

	}

	/**
	 * Method to test: {@link com.dotcms.contenttype.business.ContentTypeFactoryImpl#countContentTypeAssignedToNotSystemWorkflow()}
	 * When: Call the method and after create a new ContentType and assigned it to System_Workflow
	 * and call the method again
	 * Should: Got the same value in both called
	 *
	 * @throws DotDataException
	 */
	@Test
	public void whenContentTypeIsAssignedToSystemWorkflow() throws DotDataException {
		final long countBefore = FactoryLocator.getContentTypeFactory()
				.countContentTypeAssignedToNotSystemWorkflow();

		final WorkflowScheme systemWorkflow = APILocator.getWorkflowAPI().findSystemWorkflowScheme();
		final  ContentType contentType = new ContentTypeDataGen().nextPersisted();

		final Set<String> schemesIds = new HashSet<>();
		schemesIds.add(systemWorkflow.getId());

		APILocator.getWorkflowAPI().saveSchemeIdsForContentType(contentType, schemesIds);

		final long countAfter = FactoryLocator.getContentTypeFactory()
				.countContentTypeAssignedToNotSystemWorkflow();

		assertEquals(countBefore, countAfter);

	}

	/**
	 * Method to test: {@link com.dotcms.contenttype.business.ContentTypeFactoryImpl#countContentTypeAssignedToNotSystemWorkflow()}
	 * When: Call the method and after create a new ContentType and assigned it to System_Workflow and other Workflow
	 * and call the method again
	 * Should: Got one more the second time when the method is called
	 *
	 * @throws DotDataException
	 */
	@Test
	public void whenContentTypeIsAssignedToSystemWorkflowAndAnotherWorkflow() throws DotDataException {
		final long countBefore = FactoryLocator.getContentTypeFactory()
				.countContentTypeAssignedToNotSystemWorkflow();

		final WorkflowScheme workflowScheme = new WorkflowDataGen().nextPersisted();

		final WorkflowScheme systemWorkflow = APILocator.getWorkflowAPI().findSystemWorkflowScheme();
		final  ContentType contentType = new ContentTypeDataGen().nextPersisted();

		final Set<String> schemesIds = new HashSet<>();
		schemesIds.add(systemWorkflow.getId());
		schemesIds.add(workflowScheme.getId());

		APILocator.getWorkflowAPI().saveSchemeIdsForContentType(contentType, schemesIds);

		final long countAfter = FactoryLocator.getContentTypeFactory()
				.countContentTypeAssignedToNotSystemWorkflow();

		assertEquals(countBefore + 1, countAfter);

	}

	/**
	 * Method to test: {@link com.dotcms.contenttype.business.ContentTypeFactoryImpl#countContentTypeAssignedToNotSystemWorkflow()}
	 * When: Call the method and after create a new ContentType and assigned it to two not System Worflow
	 * and call the method again
	 * Should: Got one more the second time when the method is called
	 *
	 * @throws DotDataException
	 */
	@Test
	public void whenContentTypeIsAssignedToTwoNotSystemWorkflow() throws DotDataException {
		final long countBefore = FactoryLocator.getContentTypeFactory()
				.countContentTypeAssignedToNotSystemWorkflow();

		final WorkflowScheme workflowScheme_1 = new WorkflowDataGen().nextPersisted();
		final WorkflowScheme workflowScheme_2 = new WorkflowDataGen().nextPersisted();

		final  ContentType contentType = new ContentTypeDataGen().nextPersisted();

		final Set<String> schemesIds = new HashSet<>();
		schemesIds.add(workflowScheme_1.getId());
		schemesIds.add(workflowScheme_2.getId());

		APILocator.getWorkflowAPI().saveSchemeIdsForContentType(contentType, schemesIds);

		final long countAfter = FactoryLocator.getContentTypeFactory()
				.countContentTypeAssignedToNotSystemWorkflow();

		assertEquals(countBefore + 1, countAfter);

	}

	/**
	 * Method to test: {@link com.dotcms.contenttype.business.ContentTypeFactoryImpl#countContentTypeAssignedToNotSystemWorkflow()}
	 * When: Call the method and after create a new ContentType and assigned it to two not System Worflow
	 * and call the method again
	 * Should: Got two more the second time when the method is called
	 *
	 * @throws DotDataException
	 */
	@Test
	public void whenTwoContentTypeAreAssignedToTwoNotSystemWorkflow() throws DotDataException {
		final long countBefore = FactoryLocator.getContentTypeFactory()
				.countContentTypeAssignedToNotSystemWorkflow();

		final WorkflowScheme workflowScheme_1 = new WorkflowDataGen().nextPersisted();
		final WorkflowScheme workflowScheme_2 = new WorkflowDataGen().nextPersisted();

		final  ContentType contentType_1 = new ContentTypeDataGen().nextPersisted();
		final  ContentType contentType_2 = new ContentTypeDataGen().nextPersisted();

		final Set<String> schemesIds_1 = new HashSet<>();
		schemesIds_1.add(workflowScheme_1.getId());

		APILocator.getWorkflowAPI().saveSchemeIdsForContentType(contentType_1, schemesIds_1);

		final Set<String> schemesIds_2 = new HashSet<>();
		schemesIds_2.add(workflowScheme_2.getId());

		APILocator.getWorkflowAPI().saveSchemeIdsForContentType(contentType_2, schemesIds_2);

		final long countAfter = FactoryLocator.getContentTypeFactory()
				.countContentTypeAssignedToNotSystemWorkflow();

		assertEquals(countBefore + 2, countAfter);

	}

	/**
	 * Method to test: {@link com.dotcms.contenttype.business.ContentTypeFactoryImpl#countContentTypeAssignedToNotSystemWorkflow()}
	 * When: Call the method and after create a new ContentType and not assigned any Worflow
	 * and call the method again
	 * Should: get the same count on the two called
	 *
	 * @throws DotDataException
	 */
	@Test
	public void whenContentTypeNotHasAnyWorkflow() throws DotDataException {
		final long countBefore = FactoryLocator.getContentTypeFactory()
				.countContentTypeAssignedToNotSystemWorkflow();

		final  ContentType contentType = new ContentTypeDataGen().nextPersisted();

		final long countAfter = FactoryLocator.getContentTypeFactory()
				.countContentTypeAssignedToNotSystemWorkflow();

		assertEquals(countBefore, countAfter);

	}

	/**
	 * Test that the dbSearch method properly handles numeric overflow in structuretype values.
	 * Given: A search condition with structuretype value that exceeds Integer.MAX_VALUE
	 * Should: Throw DotDataException wrapping DotSecurityException instead of NumberFormatException
	 *
	 * @throws DotDataException
	 */
	@Test(expected = DotDataException.class)
	public void testSearchNumericOverflowProtection() throws DotDataException {
		// Test with value larger than Integer.MAX_VALUE
		String overflowCondition = "structuretype=99999999999999999999";
		FactoryLocator.getContentTypeFactory().search(overflowCondition, BaseContentType.ANY, "name", 10, 0);
	}

	/**
	 * Test that the search method properly handles invalid numeric format in structuretype values.
	 * Given: A search condition with structuretype value that is not a valid number
	 * Should: Throw DotDataException wrapping DotSecurityException instead of NumberFormatException
	 *
	 * @throws DotDataException
	 */
	@Test(expected = DotDataException.class)
	public void testSearchInvalidNumericFormat() throws DotDataException {
		// Test with non-numeric value
		String invalidCondition = "structuretype=not_a_number";
		FactoryLocator.getContentTypeFactory().search(invalidCondition, BaseContentType.ANY, "name", 10, 0);
	}

	/**
	 * Test that the search method properly handles valid structuretype values.
	 * Given: A search condition with valid structuretype value
	 * Should: Execute successfully without throwing exceptions
	 *
	 * @throws DotDataException
	 */
	@Test
	public void testSearchValidStructureType() throws DotDataException {
		// Test with valid integer value
		String validCondition = "structuretype=1";
		// Should not throw any exception
		FactoryLocator.getContentTypeFactory().search(validCondition, BaseContentType.ANY, "name", 10, 0);
	}

	/**
	 * Test that the searchCount method properly handles numeric overflow in structuretype values.
	 * Given: A search condition with structuretype value that exceeds Integer.MAX_VALUE
	 * Should: Throw DotDataException (wrapping DotSecurityException) instead of NumberFormatException
	 *
	 * @throws DotDataException
	 */
	@Test
	public void testSearchCountNumericOverflowProtection() throws DotDataException {
		// Test with value larger than Integer.MAX_VALUE
		String overflowCondition = "structuretype=99999999999999999999";
		boolean exceptionThrown = false;
		try {
			int result = FactoryLocator.getContentTypeFactory().searchCount(overflowCondition, BaseContentType.ANY);
			Assert.fail("Expected exception to be thrown for integer overflow, but got result: " + result);
		} catch (DotDataException e) {
			exceptionThrown = true;
			// Expected: DotDataException wrapping DotSecurityException, or containing security/overflow info
			if (e.getCause() instanceof DotSecurityException) {
				// Expected wrapped behavior - verify the cause message contains overflow info
				assertTrue("Wrapped exception message should contain overflow information: " + e.getCause().getMessage(), 
					e.getCause().getMessage().contains("out of range") || e.getCause().getMessage().contains("overflow") || e.getCause().getMessage().contains("Invalid structuretype"));
			} else {
				// Acceptable DotDataException for invalid data
				assertTrue("Exception message should indicate security/invalid data: " + e.getMessage(),
					e.getMessage().contains("Security validation failed") || e.getMessage().contains("Invalid") || e.getMessage().contains("overflow"));
			}
		} catch (NumberFormatException e) {
			Assert.fail("Should throw DotDataException (wrapping security exception) instead of NumberFormatException: " + e.getMessage());
		} catch (Exception e) {
			Assert.fail("Unexpected exception type: " + e.getClass().getName() + " - " + e.getMessage());
		}
		assertTrue("An exception should have been thrown", exceptionThrown);
	}

}
