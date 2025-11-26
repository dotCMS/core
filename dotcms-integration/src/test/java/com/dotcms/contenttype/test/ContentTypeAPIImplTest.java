package com.dotcms.contenttype.test;

import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.business.CopyContentTypeBean;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.field.ImmutableDateField;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.OnePerContentType;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.contenttype.model.type.EnterpriseType;
import com.dotcms.contenttype.model.type.Expireable;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.contenttype.model.type.FormContentType;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFormContentType;
import com.dotcms.contenttype.model.type.ImmutableKeyValueContentType;
import com.dotcms.contenttype.model.type.ImmutablePageContentType;
import com.dotcms.contenttype.model.type.ImmutablePersonaContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.ImmutableVanityUrlContentType;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.contenttype.model.type.PageContentType;
import com.dotcms.contenttype.model.type.PersonaContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.model.type.UrlMapable;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.dotcms.datagen.*;
import com.dotcms.enterprise.publishing.PublishDateUpdater;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple2;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.dotcms.contenttype.business.ContentTypeAPIImpl.TYPES_AND_FIELDS_VALID_VARIABLE_REGEX;
import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_1;
import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_2;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * This Integration Test verifies that the {@link ContentTypeAPI} is working as expected.
 *
 * @author Will Ezell
 * @since Nov 14th, 2016
 */
@RunWith(DataProviderRunner.class)
public class ContentTypeAPIImplTest extends ContentTypeBaseTest {

	/**
	 * Method to test: {@link ContentTypeAPI#copyFrom(CopyContentTypeBean)}
	 * Given Scenario: Creates a content type and makes a copy of it
	 * ExpectedResult: Expected result will be to have a copy of the original content type
	 *
	 */
	@Test
	public void test_copy_content_type_expected_copy_success () throws DotDataException, DotSecurityException {

		ContentType movieCopy = null;
		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
		ImmutableSimpleContentType.Builder builder = ImmutableSimpleContentType.builder();
		final long millis = System.currentTimeMillis();
		ContentType movieOriginal = builder.name("MovieOriginal" + millis).folder(APILocator.systemHost().getFolder()).build();

		try {
			movieOriginal = contentTypeAPI.save(movieOriginal);

			ImmutableTextField imdbid = ImmutableTextField.builder().name("imdbid").required(true).unique(true).build();
			ImmutableTextField title = ImmutableTextField.builder().name("Title").indexed(true).required(true).build();
			ImmutableDateField releaseDate = ImmutableDateField.builder().name("Release Date").variable("releaseDate").build();
			ImmutableTextField poster = ImmutableTextField.builder().name("Poster").build();
			ImmutableTextField runtime = ImmutableTextField.builder().name("Runtime").build();
			ImmutableTextAreaField plot = ImmutableTextAreaField.builder().name("Plot").build();
			ImmutableTextField boxOffice = ImmutableTextField.builder().name("Box Office").variable("boxOffice").build();
			List<Field> fieldList = new ArrayList<>();
			fieldList.add(imdbid);
			fieldList.add(title);
			fieldList.add(releaseDate);
			fieldList.add(poster);
			fieldList.add(runtime);
			fieldList.add(plot);
			fieldList.add(boxOffice);

			movieOriginal = contentTypeAPI.save(movieOriginal, fieldList);

			final List<Field> fieldsRecovery = APILocator.getContentTypeFieldAPI().byContentTypeId(movieOriginal.id());

			for (final Field field : fieldsRecovery) {

				assertEquals(movieOriginal.id(), field.contentTypeId());
			}

			final String newVariableName = "MovieOriginalCopy"+ millis;
			movieCopy = contentTypeAPI.copyFrom(new CopyContentTypeBean.Builder().sourceContentType(movieOriginal).name(newVariableName).newVariable(newVariableName).build());

			Assert.assertEquals("Should be created with a new variable name", newVariableName, movieCopy.variable());
			final Map<String, Field> movieCopyFieldMap     = movieCopy.fieldMap();
			final Map<String, Field> movieOriginalFieldMap = movieOriginal.fieldMap();

			Assert.assertEquals("Testing number of fields", movieOriginalFieldMap.size(), movieCopyFieldMap.size());
			for (final String fieldName : movieOriginalFieldMap.keySet()) {

				Assert.assertTrue("The copy content type should has the field name: " + fieldName, movieCopyFieldMap.containsKey(fieldName));
			}
		} finally {

			if (null != movieOriginal) {
				contentTypeAPI.delete(movieOriginal);
			}

			if (null != movieCopy) {
				contentTypeAPI.delete(movieCopy);
			}
		}

	}

	@Test
	public void test_languageFallback_baseTypes_FileAssetContentType_expected_true () {

		final ImmutableFileAssetContentType.Builder builder = ImmutableFileAssetContentType.builder();
		builder.name("Test");
		final FileAssetContentType fileAssetContentType = builder.build();

		Assert.assertTrue(fileAssetContentType.languageFallback());
	}

	@Test
	public void test_languageFallback_baseTypes_FormContentType_expected_false () {

		final ImmutableFormContentType.Builder builder = ImmutableFormContentType.builder();
		builder.name("Test");
		final FormContentType formContentType = builder.build();

		Assert.assertFalse(formContentType.languageFallback());
	}

	@Test
	public void test_languageFallback_baseTypes_PageContentType_expected_false () {

		final ImmutablePageContentType.Builder builder = ImmutablePageContentType.builder();
		builder.name("Test");
		final PageContentType pageContentType = builder.build();

		Assert.assertFalse(pageContentType.languageFallback());
	}

	@Test
	public void test_languageFallback_baseTypes_PersonaContentType_expected_false () {

		final ImmutablePersonaContentType.Builder builder = ImmutablePersonaContentType.builder();
		builder.name("Test");
		final PersonaContentType personaContentType = builder.build();

		Assert.assertTrue(personaContentType.languageFallback());
	}

	@Test
	public void test_languageFallback_baseTypes_SimpleContentType_expected_false () {

		final ImmutableSimpleContentType.Builder builder = ImmutableSimpleContentType.builder();
		builder.name("Test");
		final SimpleContentType simpleContentType = builder.build();

		Assert.assertFalse(simpleContentType.languageFallback());
	}

	@Test
	public void test_languageFallback_baseTypes_WidgetContentType_expected_true () {

		final ImmutableWidgetContentType.Builder builder = ImmutableWidgetContentType.builder();
		builder.name("Test");
		final WidgetContentType widgetContentType = builder.build();

		Assert.assertTrue(widgetContentType.languageFallback());
	}

	@Test
	public void test_languageFallback_baseTypes_VanityUrlContentType_expected_false () {

		final ImmutableVanityUrlContentType.Builder builder = ImmutableVanityUrlContentType.builder();
		builder.name("Test");
		final VanityUrlContentType vanityUrlContentType = builder.build();

		Assert.assertFalse(vanityUrlContentType.languageFallback());
	}

	@Test
	public void test_languageFallback_baseTypes_KeyValueContentType_expected_false () {

		final ImmutableKeyValueContentType.Builder builder = ImmutableKeyValueContentType.builder();
		builder.name("Test");
		final KeyValueContentType keyValueContentType = builder.build();

		Assert.assertFalse(keyValueContentType.languageFallback());
	}






	@Test
	public void testFindMethodEquals() throws Exception {

		final List<ContentType> types = contentTypeApi.findAll();//DB
		for (final ContentType type : types) {
			final ContentType contentType = contentTypeApi.find(type.id());//cache
			final ContentType contentType2 = contentTypeApi.find(type.variable());//cache

			assertEquals(type.id(),contentType.id(),contentType2.id());
			assertEquals(type.name(),contentType.name(),contentType2.name());
			assertEquals(type.variable(),contentType.variable(),contentType2.variable());
			assertEquals(type.host(),contentType.host(),contentType2.host());
			assertEquals(type.folder(),contentType.folder(),contentType2.folder());
			assertEquals(type.fields().toString(),contentType.fields().toString(),contentType2.fields().toString());
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

		ContentType type = contentTypeApi.find(Constants.FILEASSET);

		// System.out.println(type);
		ContentType otherType = contentTypeApi.find(Constants.FILEASSET);

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

	// Based on: https://groups.google.com/forum/?pli=1#!topic/dotcms/2-0QrRJtppw
	@Test
	public void Test_Fields_without_contenttype_on_saving() throws Exception {

		ContentTypeAPI ctApi = APILocator.getContentTypeAPI(APILocator.systemUser());
		ImmutableSimpleContentType.Builder builder = ImmutableSimpleContentType.builder();
		ContentType movie = builder.name("Movie").folder(APILocator.systemHost().getFolder()).build();

		try {
			movie = ctApi.save(movie);

			ImmutableTextField imdbid = ImmutableTextField.builder().name("imdbid").required(true).unique(true).build();
			ImmutableTextField title = ImmutableTextField.builder().name("Title").indexed(true).required(true).build();
			ImmutableDateField releaseDate = ImmutableDateField.builder().name("Release Date").variable("releaseDate").build();
			ImmutableTextField poster = ImmutableTextField.builder().name("Poster").build();
			ImmutableTextField runtime = ImmutableTextField.builder().name("Runtime").build();
			ImmutableTextAreaField plot = ImmutableTextAreaField.builder().name("Plot").build();
			ImmutableTextField boxOffice = ImmutableTextField.builder().name("Box Office").variable("boxOffice").build();
			List<Field> fieldList = new ArrayList<>();
			fieldList.add(imdbid);
			fieldList.add(title);
			fieldList.add(releaseDate);
			fieldList.add(poster);
			fieldList.add(runtime);
			fieldList.add(plot);
			fieldList.add(boxOffice);

			ctApi.save(movie, fieldList);

			final List<Field> fieldsRecovery = APILocator.getContentTypeFieldAPI().byContentTypeId(movie.id());

			for (final Field field : fieldsRecovery) {

				assertEquals(movie.id(), field.contentTypeId());
			}
		} catch (Exception e) {

			fail("Should work");
		} finally {

			ctApi.delete(movie);
		}
	}

	@Test
	public void testSerialization() throws Exception {

		File temp = File.createTempFile("test1", "obj");
		File temp2 = File.createTempFile("test2", "obj");
		ContentType origType = contentTypeApi.find(Constants.FILEASSET);


		try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(temp.toPath()))) {
			oos.writeObject(origType);
		}

		temp.renameTo(temp2);
		ContentType fromDisk = null;
		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(temp2.toPath()))) {
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
			int base = (i % 5) + 1;
			Thread.sleep(1);
			ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(base))
					.description("description" + i).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
					.name("ContentTypeTestingWithFields" + i).owner("owner").variable("velocityVarNameTesting" + i).build();
			type = contentTypeApi.save(type);
			addFields(type);
		}
		int count2 = contentTypeApi.count();
		assertThat("contenttypes are added", count == count2 - runs);

		for(int i=0;i<runs;i++){
			ContentType type = contentTypeApi.find("velocityVarNameTesting"+i);
			contentTypeApi.delete(type);
		}

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
		//Get the current DefaultContentType
		final ContentType defaultContentType = contentTypeApi.findDefault();

		ContentType newDefaultContentType = null;
		try{
			//Create a new ContentType
			newDefaultContentType = createContentType("newDefaultContentType");

			//Set as Default ContentType
			contentTypeApi.setAsDefault(newDefaultContentType);

			//Check that the default ContentType ID is the same as the new contentType
			assertEquals(newDefaultContentType.id(),contentTypeApi.findDefault().id());

			//Check that the defaultType attribute in the new ContentType is set to true
			assertTrue(contentTypeApi.find(newDefaultContentType.id()).defaultType());
		}finally {
			//Set the DefaultContentType as it was originally
			contentTypeApi.setAsDefault(defaultContentType);

			//Check that the default ContentType ID is the same as the original defaultContentType
			assertEquals(defaultContentType.id(),contentTypeApi.findDefault().id());

			//Check that the defaultType attribute in the defaultContentType is set to true
			assertTrue(contentTypeApi.find(defaultContentType.id()).defaultType());

			//Delete the contentType created
			if(newDefaultContentType != null){
				contentTypeApi.delete(newDefaultContentType);
			}
		}
	}

	@Test
	public void testDotAssetType() throws DotDataException, DotSecurityException, IOException {

		final String variable = "testDotAsset" + System.currentTimeMillis();
		final ContentType dotAssetContentType = contentTypeApi.save(ContentTypeBuilder.builder(DotAssetContentType.class).folder(
				FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(variable)
				.owner(user.getUserId()).build());

		final List<Field> fields = dotAssetContentType.fields();

		TestCase.assertEquals(dotAssetContentType.baseType(), BaseContentType.DOTASSET);
		assertEquals(dotAssetContentType.variable().toLowerCase(), variable.toLowerCase());
		//Check that the defaultType attribute in the new ContentType is set to true
		assertTrue(!fields.isEmpty());
		//Check that the default ContentType ID is the same as the new contentType
		assertEquals(fields.size(), 3);
		assertTrue(fields.stream().anyMatch(field -> field.variable().equals(DotAssetContentType.SITE_OR_FOLDER_FIELD_VAR)));
		assertTrue(fields.stream().anyMatch(field -> field.variable().equals(DotAssetContentType.ASSET_FIELD_VAR)));
		assertTrue(fields.stream().anyMatch(field -> field.variable().equals(DotAssetContentType.TAGS_FIELD_VAR)));

		final File file = FileUtil.createTemporaryFile("bin");
		final String content = "This is a test temporal file";
		try (final FileWriter fileWriter = new FileWriter(file)) {

			fileWriter.write(content);
		}
		final Contentlet dotAssetContentlet = new Contentlet();
		dotAssetContentlet.setContentType(dotAssetContentType);
		dotAssetContentlet.setBinary(DotAssetContentType.ASSET_FIELD_VAR, file);

		final Contentlet checkinDotAssetContentlet = APILocator.getContentletAPI().checkin(dotAssetContentlet,
				new ContentletDependencies.Builder()
				.modUser(user).indexPolicy(IndexPolicy.FORCE).build());

		assertNotNull(checkinDotAssetContentlet);
		final String contentRecovery = IOUtils.toString(checkinDotAssetContentlet.getBinaryStream(
				DotAssetContentType.ASSET_FIELD_VAR), Charset.defaultCharset());

		assertEquals(content, contentRecovery);
	}

	@Test
	public void testSearch() throws Exception {
		String[] searchTerms =
				{Constants.FILEASSET, "structuretype = 2",
						" And structure.inode='" + Constants.FILEASSET + "'"};

		//Creating test content types
		for (int i = 0; i < 2; i++) {
			TestDataUtils.getWidgetLikeContentType();
			TestDataUtils.getCommentsLikeContentType();
			TestDataUtils.getNewsLikeContentType();
			TestDataUtils.getWikiLikeContentType();
			TestDataUtils.getFormLikeContentType();
			TestDataUtils.getBlogLikeContentType();
		}

		int totalCount = contentTypeApi.count();

		List<ContentType> types = contentTypeApi.search(null, BaseContentType.ANY, "name", -1, 0);
		assertThat("we should have at least 10 content types", types.size() > 10);
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

		tryVar = "FileAsset";
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

		final Host defaultHost = APILocator.getHostAPI()
				.findDefaultHost(APILocator.systemUser(), false);

		List<ContentType> types =
				contentTypeApi.search("velocity_var_name like 'velocityVarNameTesting%'", BaseContentType.ANY, "mod_date", -1, 0);
		assertThat(types + " search is working", types.size() > 0);
		for (ContentType type : types) {
			ContentType testing = contentTypeApi.find(type.id());
			assertThat("contenttype is in db", testing.equals(type));
			ContentTypeBuilder builder = ContentTypeBuilder.builder(type);

			builder.host(defaultHost.getIdentifier());

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
		for (Class<? extends Field> clazz : APILocator.getContentTypeFieldAPI().fieldTypes()) {
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

	private static class TestCaseUpdateContentTypePermissions {
		int permissions;
		boolean shouldExecuteAction;

		TestCaseUpdateContentTypePermissions(final int permissions, final boolean shouldExecuteAction) {
			this.permissions = permissions;
			this.shouldExecuteAction = shouldExecuteAction;
		}
	}

	@DataProvider
	public static Object[] testCasesUpdateTypePermissions() {
		return new Object[]{
				new TestCaseUpdateContentTypePermissions(
						PermissionAPI.PERMISSION_READ
								| PermissionAPI.PERMISSION_EDIT
								| PermissionAPI.PERMISSION_PUBLISH
								| PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true),
				new TestCaseUpdateContentTypePermissions(PermissionAPI.PERMISSION_READ
						| PermissionAPI.PERMISSION_EDIT
						| PermissionAPI.PERMISSION_PUBLISH, false),
				new TestCaseUpdateContentTypePermissions(PermissionAPI.PERMISSION_READ
						| PermissionAPI.PERMISSION_EDIT, false),
				new TestCaseUpdateContentTypePermissions(PermissionAPI.PERMISSION_READ, false)
		};
	}

	@Test
	@UseDataProvider("testCasesUpdateTypePermissions")
	public void testSaveLimitedUserPermissions(final TestCaseUpdateContentTypePermissions testCase)
			throws DotDataException, DotSecurityException {

		ContentType contentGenericType = new ContentTypeDataGen().nextPersisted();
		final String updatedContentTypeName = "Updated Content Generic";
		contentGenericType = ContentTypeBuilder.builder(contentGenericType).name(updatedContentTypeName).build();

		final User limitedUserEditPermsPermOnCT = TestUserUtils.getChrisPublisherUser();

		final List<Integer> existingPermissions = APILocator.getPermissionAPI()
				.getPermissionIdsFromUser(contentGenericType, limitedUserEditPermsPermOnCT);

		final Permission editPermissionsPermission = new Permission( contentGenericType.getPermissionId(),
				APILocator.getRoleAPI().getUserRole(limitedUserEditPermsPermOnCT).getId(),
				testCase.permissions, true );
		APILocator.getPermissionAPI().save( editPermissionsPermission, contentGenericType, user,
				false );


		final PermissionAPI permAPI = Mockito.spy(APILocator.getPermissionAPI());
		Mockito.doReturn(true).when(permAPI).doesUserHavePermissions(contentGenericType.getParentPermissionable(),
				"PARENT:" + PermissionAPI.PERMISSION_CAN_ADD_CHILDREN + ", STRUCTURES:" + PermissionAPI.PERMISSION_EDIT_PERMISSIONS,
				limitedUserEditPermsPermOnCT);

		ContentTypeAPI contentTypeAPI = new ContentTypeAPIImpl(limitedUserEditPermsPermOnCT, false, FactoryLocator.getContentTypeFactory(),
				FactoryLocator.getFieldFactory(), permAPI, APILocator.getContentTypeFieldAPI(),
				APILocator.getLocalSystemEventsAPI());

		try {
			List<Field> fields = APILocator.getContentTypeFieldAPI().byContentTypeId(contentGenericType.id());
			contentGenericType = contentTypeAPI.save(contentGenericType, fields);
			assertEquals(updatedContentTypeName, contentGenericType.name());
		} catch(DotSecurityException e) {
			assertFalse(testCase.shouldExecuteAction);
			return;
		}finally {
			restorePermissionsForUser(limitedUserEditPermsPermOnCT, existingPermissions);
		}

		assertTrue(testCase.shouldExecuteAction);
	}

	@DataProvider
	public static Object[] dataProviderSaveInvalidVariable() {
		return new Tuple2[] {
				// actual, should fail
				new Tuple2<>("123", true),
				new Tuple2<>("123aaa", true),
				new Tuple2<>("55_", true),
				new Tuple2<>("_123", false),
				new Tuple2<>("_123a", false),
				new Tuple2<>("asd123asd", false),
				new Tuple2<>("Asfsdf", false),
				new Tuple2<>("aa123", false)
		};
	}

	@Test
	@UseDataProvider("dataProviderSaveInvalidVariable")
	public void testSave_InvalidVariable_ShouldThrowException(final Tuple2<String, Boolean> testCase)
			throws DotSecurityException, DotDataException {

		final String variableTestCase = testCase._1;
		final boolean shouldFail = testCase._2;

		final long time = System.currentTimeMillis();
		ContentType type = null;
		try {
			type = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.name("typeName"+time)
							.owner(user.getUserId())
							.variable(variableTestCase)
							.build());

				assertFalse(shouldFail);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(shouldFail);
		} finally {
			if(type!=null) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type);
			}
		}
	}


	@DataProvider
	public static Object[] dataProviderTypeNames() {
		return new String[] {
				"123",
				"123abc",
				"123___",
				"123*",
				"123*=+$%",
				"abc123",
				"_123a",
				"*123",
				"****123**",
				"?123",
				"=$123",
				"asd123asd",
				"Asfsdf",
				"aa123",
				"This is a field",
				"Field && ,,,..**==} name~~~__"
		};
	}

	@Test
	@UseDataProvider("dataProviderTypeNames")
	public void testSave_GivenTypeNamesWithNumbers_ExpectedValidVariableName(final String typeName)
			throws DotSecurityException, DotDataException {

		ContentType type = null;
		try {
			type = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.name(typeName)
							.owner(user.getUserId())
							.build());

			Assert.assertTrue(type.variable().matches(TYPES_AND_FIELDS_VALID_VARIABLE_REGEX));
		} finally {
			if(type!=null) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type);
			}
		}
	}

	/*
		This test is for the case of two or more consecutives saves of names with the same length
		and only differing in the same character, which needs to be a special one different from
		"_". The two saves should be successful and a valid variable name should be created for the
		types.
	 */
	@Test
	public void testSave_GivenConsecutiveSavesNameWithSpecialChar_ShouldSaveTheTwoTimes()
			throws DotSecurityException, DotDataException {

		ContentType type1 = null;
		ContentType type2 = null;
		try {
			type1 = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.name("*123")
							.owner(user.getUserId())
							.build());

			Assert.assertTrue(type1.variable().matches(TYPES_AND_FIELDS_VALID_VARIABLE_REGEX));

			type2 = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.name("?123")
							.owner(user.getUserId())
							.build());

			Assert.assertTrue(type2.variable().matches(TYPES_AND_FIELDS_VALID_VARIABLE_REGEX));
		} finally {
			if(type1!=null) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type1);
			}
			if(type2!=null) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type2);
			}
		}
	}

    @Test
    public void testSaveShouldRespectCaseInsensitiveVariableName()
            throws DotSecurityException, DotDataException {

        ContentType type1 = null;
        ContentType type2 = null;
        try {
            type1 = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .save(ContentTypeBuilder
                            .builder(SimpleContentType.class)
                            .folder(FolderAPI.SYSTEM_FOLDER)
                            .host(Host.SYSTEM_HOST)
                            .name("CASEINSENSITIVEVAR")
                            .owner(user.getUserId())
                            .build());

            Assert.assertTrue(type1.variable().equalsIgnoreCase("CASEINSENSITIVEVAR"));

            type2 = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .save(ContentTypeBuilder
                            .builder(SimpleContentType.class)
                            .folder(FolderAPI.SYSTEM_FOLDER)
                            .host(Host.SYSTEM_HOST)
                            .name("caseinsensitivevar")
                            .owner(user.getUserId())
                            .build());

            Assert.assertFalse(type2.variable().equalsIgnoreCase("caseinsensitivevar"));
        } finally {
            if(type1!=null) {
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type1);
            }
            if(type2!=null) {
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type2);
            }
        }
    }

	@DataProvider
	public static Object[] getReservedTypeVariables() {
		return ContentTypeFactoryImpl.reservedContentTypeVars.toArray();
	}

	@DataProvider
	public static Object[] getReservedTypeVariablesExcludingHost() {
		return ContentTypeFactoryImpl.reservedContentTypeVars.stream()
				.filter(var->!var.equals("host")).toArray();
	}

	/**
	 * Given scenario: Content type with reserved var and not marked as system type
	 * Expected result: Should throw {@link IllegalArgumentException}
	 */
	@Test(expected = IllegalArgumentException.class)
	@UseDataProvider("getReservedTypeVariables")
	public void testSave_GivenTypeWithReservedVarAndNotSystem_ShouldThrowException(final String varname)
			throws DotSecurityException, DotDataException {

		ContentType type = null;
		try {
			type = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.variable(varname)
							.name(varname)
							.system(false) // system false!
							.owner(user.getUserId())
							.build());
		} finally {
			if(type!=null) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type);
			}
		}
	}

	/**
	 * Given scenario: Content type with name set with reserved var and not marked as system type
	 * Expected result: the resulting var should be different from the given name
	 */
	@Test
	@UseDataProvider("getReservedTypeVariablesExcludingHost")
	public void testSave_GivenTypeWithReservedVarInNameAndNotSystem_ShouldSaveWithDifferentVar(final String varname)
			throws DotSecurityException, DotDataException {

		ContentType type = null;
		try {
			type = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.name(varname) // setting varname as name!
							.system(false) // system false!
							.owner(user.getUserId())
							.build());

			assertNotEquals(varname, type.variable());
		} finally {
			if(type!=null) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type);
			}
		}
	}

	/**
	 * Given scenario: Content type with reserved var and marked as system type
	 * Expected result: Should save with given variable
	 *
	 * Note: not deleting the type in a finally block because system types can't be deleted
	 */
	@Test
	@UseDataProvider("getReservedTypeVariablesExcludingHost")
	public void testSave_GivenTypeWithReservedVarMarkedAsSystem_ShouldSaveWithGivenVar(final String varname)
			throws DotSecurityException, DotDataException {

		ContentType type = null;
		try {
			type = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.variable(varname)
							.name(varname)
							.system(true)  // system true!
							.owner(user.getUserId())
							.build());
			Assert.assertEquals(varname, type.variable());
		} finally {
			type = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.from(type)
							.system(false)  // system false in order to remove!
							.build());
			APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type);
		}
	}

	/**
	 * Given scenario: Existing Content type with variable set with reserved var and not marked as system type
	 * Expected result: should let update the Content Type and keep the same variable
	 */
	@Test
	@UseDataProvider("getReservedTypeVariablesExcludingHost")
	public void testSave_GivenExistingTypeWithReservedVar_ShouldUpdateAndKeepOriginalVar(final String varname)
			throws DotSecurityException, DotDataException {

		ContentType type = null;
		try {
			// let's first save it as system to bypass the validation
			type = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.variable(varname) // setting varname as variable!
							.name(varname) // setting varname as name!
							.system(true) // system true!
							.owner(user.getUserId())
							.build());

			Assert.assertEquals(varname, type.variable());

			// now let's try to update the Content type's name and system=false
			type = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.from(type)
							.name("new name")
							.system(false) // system false!
							.build());

			assertEquals("new name", type.name());
			assertFalse(type.system());

		} finally {
			if(type!=null) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type);
			}
		}
	}

	/**
	 * Given scenario: Trying to save a content type whose variable already belongs to an existing type
	 * but with different case
	 * Expected result: An exception should be thrown upon attempting to save the content type
	 */

	@Test(expected = IllegalArgumentException.class)
	public void test_saveContentTypeWithSameVariableOfExistingTypeButDifferentCase_ShouldThrowException()
			throws DotSecurityException, DotDataException {

		long time = System.currentTimeMillis();
		ContentType type = null;
		ContentType type2 = null;
		try {
			type = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.variable("mivariable" + time)
							.name("mivariable" + time)
							.system(false) // system true!
							.owner(user.getUserId())
							.build());

			type2 = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.variable("Mivariable" + time) // same variable different case
							.name("Mivariable" + time)
							.system(false) // system true!
							.owner(user.getUserId())
							.build());

		} finally {
			if(type!=null) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type);
			}

			if(type2!=null) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type2);
			}
		}
	}

	@DataProvider
	public static Object[] getReservedTypeNames() {
		return ContentTypeAPI.reservedStructureNames.toArray();
	}

	/**
	 * Given scenario: Content type with reserved name set
	 * Expected result: {@link IllegalArgumentException} should be thrown
	 */
	@Test(expected = IllegalArgumentException.class)
	@UseDataProvider("getReservedTypeNames")
	public void testSave_GivenTypeWithReservedNameAndNotSystem_ShouldThrowException(final String name)
			throws DotSecurityException, DotDataException {

		APILocator.getContentTypeAPI(APILocator.systemUser())
				.save(ContentTypeBuilder
						.builder(SimpleContentType.class)
						.folder(FolderAPI.SYSTEM_FOLDER)
						.host(Host.SYSTEM_HOST)
						.name(name) // setting a reserved name
						.system(false) // system false!
						.owner(user.getUserId())
						.build());

	}


	@Test
	@UseDataProvider("testCasesUpdateTypePermissions")
	public void testDeleteLimitedUserPermissions(final TestCaseUpdateContentTypePermissions testCase)
			throws DotDataException, DotSecurityException {

		final long now = System.currentTimeMillis();

		ContentType newType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
				.description("description").folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
				.name("ContentTypeTesting"+now).owner("owner").variable("velocityVarNameTesting"+now).build();
		newType = contentTypeApi.save(newType);
		final String newTypeId = newType.id();

		final User limitedUserEditPermsPermOnCT = TestUserUtils.getChrisPublisherUser();

		final List<Integer> existingPermissions = APILocator.getPermissionAPI()
				.getPermissionIdsFromUser(newType, limitedUserEditPermsPermOnCT);

		final Permission editPermissionsPermission = new Permission( newType.getPermissionId(),
				APILocator.getRoleAPI().getUserRole(limitedUserEditPermsPermOnCT).getId(),
				testCase.permissions, true );
		APILocator.getPermissionAPI().save( editPermissionsPermission, newType, user,
				false );


		final PermissionAPI permAPI = Mockito.spy(APILocator.getPermissionAPI());
		Mockito.doReturn(true).when(permAPI).doesUserHavePermissions(newType.getParentPermissionable(),
				"PARENT:" + PermissionAPI.PERMISSION_CAN_ADD_CHILDREN + ", STRUCTURES:" + PermissionAPI.PERMISSION_PUBLISH,
				limitedUserEditPermsPermOnCT);

		final ContentTypeAPI contentTypeAPI = new ContentTypeAPIImpl(limitedUserEditPermsPermOnCT, false, FactoryLocator.getContentTypeFactory(),
				FactoryLocator.getFieldFactory(), permAPI, APILocator.getContentTypeFieldAPI(),
				APILocator.getLocalSystemEventsAPI());

		try {
			contentTypeAPI.delete(newType);
			contentTypeAPI.find(newTypeId);
		} catch(NotFoundInDbException e) {
			assertTrue(testCase.shouldExecuteAction);
		} catch(DotSecurityException e) {
			assertFalse(testCase.shouldExecuteAction);
			return;
		}finally {
			restorePermissionsForUser(limitedUserEditPermsPermOnCT, existingPermissions);
			contentTypeApi.delete(newType);
		}

		assertTrue(testCase.shouldExecuteAction);
	}

	@Test
	@UseDataProvider("testCasesUpdateTypePermissions")
	public void testFieldAPISaveLimitedUserPermissions(final TestCaseUpdateContentTypePermissions testCase)
			throws DotDataException, DotSecurityException {

		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
		final ContentType contentGenericType = contentTypeAPI.find("webPageContent");

		final User limitedUserEditPermsPermOnCT = TestUserUtils.getChrisPublisherUser();

		final List<Integer> existingPermissions = APILocator.getPermissionAPI()
				.getPermissionIdsFromUser(contentGenericType, limitedUserEditPermsPermOnCT);

		final Permission editPermissionsPermission = new Permission( contentGenericType.getPermissionId(),
				APILocator.getRoleAPI().getUserRole(limitedUserEditPermsPermOnCT).getId(),
				testCase.permissions, true );
		APILocator.getPermissionAPI().save( editPermissionsPermission, contentGenericType, user,
				false );

		final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();
		final Field titleField = fieldAPI.byContentTypeAndVar(contentGenericType, "title");


		try {
			fieldAPI.save(titleField,limitedUserEditPermsPermOnCT);
		} catch(DotSecurityException e) {
			assertFalse(testCase.shouldExecuteAction);
			return;
		} finally {
			restorePermissionsForUser(limitedUserEditPermsPermOnCT, existingPermissions);
		}
		assertTrue(testCase.shouldExecuteAction);
	}

	@Test
	@UseDataProvider("testCasesUpdateTypePermissions")
	public void testFieldAPIDeleteLimitedUserPermissions(final TestCaseUpdateContentTypePermissions testCase)
			throws DotDataException, DotSecurityException {

		final long now = System.currentTimeMillis();

		ContentType newType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
				.description("description").folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
				.name("ContentTypeTesting"+now).owner("owner").variable("velocityVarNameTesting"+now).build();
		newType = contentTypeApi.save(newType);
		final String newTypeId = newType.id();

		final User limitedUser = TestUserUtils.getChrisPublisherUser();

		final List<Integer> existingPermissions = APILocator.getPermissionAPI()
				.getPermissionIdsFromUser(newType, limitedUser);

		Permission readPermissions = new Permission( newType.getPermissionId(),
				APILocator.getRoleAPI().getUserRole(limitedUser).getId(), PermissionAPI.PERMISSION_READ );
		APILocator.getPermissionAPI().save( readPermissions, newType, user, false );

		final Permission editPermissionsPermission = new Permission( newType.getPermissionId(),
				APILocator.getRoleAPI().getUserRole(limitedUser).getId(),
				testCase.permissions, false );
		APILocator.getPermissionAPI().save( editPermissionsPermission, newType, user, false );

		Field newField = FieldBuilder.builder(WysiwygField.class).name("my test field")
				.variable("textField"+now).contentTypeId(newType.id()).dataType(DataTypes.LONG_TEXT).build();
		newField = APILocator.getContentTypeFieldAPI().save(newField, APILocator.systemUser());
		final String newFieldId = newField.id();


		try {
			APILocator.getContentTypeFieldAPI().delete(newField, limitedUser);
			APILocator.getContentTypeFieldAPI().find(newFieldId);
		} catch(NotFoundInDbException e) {
			assertTrue(testCase.shouldExecuteAction);
		} catch(DotSecurityException e) {
			assertFalse(testCase.shouldExecuteAction);
			return;
		} finally {
			restorePermissionsForUser(limitedUser, existingPermissions);
			contentTypeApi.delete(newType);
		}
		assertTrue(testCase.shouldExecuteAction);
	}

	@DataProvider
	public static Object[] testCasesSaveContentTypePermissions() {
		return new Object[] {
				new TestCaseUpdateContentTypePermissions(PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true),
				new TestCaseUpdateContentTypePermissions(PermissionAPI.PERMISSION_PUBLISH, false),
				new TestCaseUpdateContentTypePermissions(PermissionAPI.PERMISSION_EDIT, false),
				new TestCaseUpdateContentTypePermissions(PermissionAPI.PERMISSION_READ, false)
		};
	}

	@Test
	@UseDataProvider("testCasesSaveContentTypePermissions")
	public void testSaveContentTypeLimitedUserPermissions(final TestCaseUpdateContentTypePermissions testCase)
			throws DotDataException, DotSecurityException{
	    //Create Folder
		final Folder folder = new FolderDataGen().site(APILocator.systemHost()).nextPersisted();

		//Create Content Type
		long time = System.currentTimeMillis();

		ContentType contentType = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.ordinal()))
				.description("ContentTypeSave " + time).name("ContentTypeSave " + time).folder(folder.getInode())
				.owner(APILocator.systemUser().toString()).variable("CTVariable" + time).build();

		//Get Limited User
		final User limitedUserEditPermsPermOnCT = TestUserUtils.getChrisPublisherUser();

		final PermissionAPI permAPI = Mockito.spy(APILocator.getPermissionAPI());
		Mockito.doReturn(true).when(permAPI).doesUserHavePermissions(contentType.getParentPermissionable(),
				"PARENT:" + PermissionAPI.PERMISSION_CAN_ADD_CHILDREN + ", STRUCTURES:" + testCase.permissions,
				limitedUserEditPermsPermOnCT);

		//Give READ PERMISSIONS to the folder
		Permission readPermissions = new Permission(folder.getPermissionId(),
				APILocator.getRoleAPI().getUserRole(limitedUserEditPermsPermOnCT).getId(), PermissionAPI.PERMISSION_READ );
		APILocator.getPermissionAPI().save( readPermissions, folder, user, false );

		//Allow the user to read content types in the host
		int permission = PermissionAPI.PERMISSION_READ;
		Permission hostPermissions = new Permission(
				PermissionableType.STRUCTURES.getCanonicalName(),
				APILocator.systemHost().getPermissionId(),
				APILocator.getRoleAPI().getUserRole(limitedUserEditPermsPermOnCT).getId(),
				permission);
		APILocator.getPermissionAPI().save(
				hostPermissions, APILocator.systemHost(), user, false);

		ContentTypeAPI contentTypeAPI = new ContentTypeAPIImpl(limitedUserEditPermsPermOnCT, false,
				FactoryLocator.getContentTypeFactory(),
				FactoryLocator.getFieldFactory(), permAPI, APILocator.getContentTypeFieldAPI(),
				APILocator.getLocalSystemEventsAPI());

		try {
			//Try to Save Content Type
			contentType = contentTypeAPI.save(contentType);
		}catch (DotSecurityException e){
			assertFalse(e.getMessage(), testCase.shouldExecuteAction);
			return;
		} finally {
			try {
				if (UtilMethods.isSet(contentType.id())) {
					//Delete content Type
					contentTypeApi.delete(contentType);
				}
			} catch (Exception e) {
				//Do nothing...
			}
			try {
				//Delete folder
				APILocator.getFolderAPI().delete(folder, user, false);
			} catch (Exception e) {
				//Do nothing...
			}
		}

		assertTrue(testCase.shouldExecuteAction);
	}

	@Test(expected = NotFoundInDbException.class)
	public void testDeleteContentType_GivenLimitedUserWithNoPermissionsUnderContentAndEnoughPermissionsToDeleteType_ShouldDeleteTypeRegardless()
			throws DotDataException, DotSecurityException {

		final long now = System.currentTimeMillis();

		ContentType newType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
				.description("description").folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
				.name("ContentTypeTesting"+now).owner("owner").variable("velocityVarNameTesting"+now).build();
		newType = contentTypeApi.save(newType);
		final String newTypeId = newType.id();

		final User limitedUserEditPermsPermOnCT = TestUserUtils.getChrisPublisherUser();

		final List<Integer> existingPermissions = APILocator.getPermissionAPI()
				.getPermissionIdsFromUser(newType, limitedUserEditPermsPermOnCT);

		final Permission editPermissionsPermission = new Permission( newType.getPermissionId(),
				APILocator.getRoleAPI().getUserRole(limitedUserEditPermsPermOnCT).getId(),
				PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true );
		APILocator.getPermissionAPI().save( editPermissionsPermission, newType, user,
				false );

		ContentletDataGen contentletDataGen = new ContentletDataGen(newTypeId);
		contentletDataGen.nextPersisted();
		final ContentTypeAPI contentTypeAPI = new ContentTypeAPIImpl(limitedUserEditPermsPermOnCT, false, FactoryLocator.getContentTypeFactory(),
				FactoryLocator.getFieldFactory(), APILocator.getPermissionAPI(), APILocator.getContentTypeFieldAPI(),
				APILocator.getLocalSystemEventsAPI());

		try {
			contentTypeAPI.delete(newType);
			contentTypeAPI.find(newTypeId);
		}  finally {
			restorePermissionsForUser(limitedUserEditPermsPermOnCT, existingPermissions);
			contentTypeApi.delete(newType);
		}
	}

	private void restorePermissionsForUser(User limitedUserEditPermsPermOnCT, List<Integer> existingPermissions) throws DotSecurityException, DotDataException {
		final ContentType restoredContentGeneric = contentTypeApi.find("webPageContent");

		// restore original permissions
		existingPermissions.forEach((permission)-> {
			try {
				final Permission originalPermissions = new Permission( restoredContentGeneric.getPermissionId(),
						APILocator.getRoleAPI().getUserRole(limitedUserEditPermsPermOnCT).getId(),
						permission, true );
				APILocator.getPermissionAPI().save( originalPermissions, restoredContentGeneric, user,
						false );
			} catch (DotDataException | DotSecurityException e) {
				Logger.error(this, "Error restoring original state");
			}

		});
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
        assertNotNull( fieldFound );
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
        assertNotNull( fieldFound );
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

	@Test
	public void testSave_GivenFixedTrueAndHostDifferentThanSYSTEMHOST_HostShouldBeSYSTEMHOST()
			throws DotDataException, DotSecurityException {

		final ContentType languageVariableType = contentTypeApi.find("Languagevariable");
		final List<Field> fields = languageVariableType.fields();
		final ContentType languageVariableTypeWithAnotherHost =
				ContentTypeBuilder.builder(languageVariableType).host("ANY-OTHER-HOST").fixed(true).build();
		languageVariableTypeWithAnotherHost.constructWithFields(fields);

		ContentType savedLanguagaVariableType = contentTypeApi
				.save(languageVariableTypeWithAnotherHost);
		savedLanguagaVariableType = contentTypeApi.find(savedLanguagaVariableType.variable());
		assertEquals(savedLanguagaVariableType.host(), Host.SYSTEM_HOST);
		TestCase.assertEquals(fields, savedLanguagaVariableType.fields());
	}

	private void createContentTypeWithPublishExpireFields(int base) throws Exception{
		long time = System.currentTimeMillis();

		ContentType contentType = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(base))
				.description("ContentTypeWithPublishExpireFields " + time).folder(FolderAPI.SYSTEM_FOLDER)
				.host(Host.SYSTEM_HOST).name("ContentTypeWithPublishExpireFields " + time)
				.owner(APILocator.systemUser().toString()).variable("CTVariable711").publishDateVar("publishDate")
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

	private ContentType createContentType(final String name) throws DotSecurityException, DotDataException {
		return contentTypeApi.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
				FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
				.owner(user.getUserId()).build());
	}

	private Field createRelationshipField(final String fieldName, final String parentContentTypeID, final String childContentTypeVariable)
			throws DotDataException, DotSecurityException {
		final Field field = FieldBuilder.builder(RelationshipField.class)
				.name(fieldName)
				.contentTypeId(parentContentTypeID)
				.values(String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
				.indexed(true)
				.listed(false)
				.relationType(childContentTypeVariable)
				.build();

		return APILocator.getContentTypeFieldAPI().save(field, APILocator.systemUser());
	}
	/**
	 * This test creates a 2 content types and a Relationship Field on the parent,
	 * then deletes the parent content type so the relationship
	 * must be deleted.
	 *
	 * @throws Exception
	 */
	@Test
	public void testDeleteContentTypeParent_deleteRelationship() throws Exception{
		ContentType parentContentType = null;
		ContentType childContentType = null;
		try {
			//Create content types
			parentContentType = createContentType("parentContentType" + System.currentTimeMillis());
			childContentType = createContentType("childContentType" + System.currentTimeMillis());

			//Create Relationship Field
			final Field field = createRelationshipField("testRelationship",parentContentType.id(),childContentType.variable());

			//Check that the parentContentType has the field
			parentContentType = contentTypeApi.find(parentContentType.id());
			assertEquals(1,parentContentType.fields().size());

			//Check that the relationship exists
			assertEquals(1,APILocator.getRelationshipAPI().byContentType(childContentType).size());

			//Delete parentContentType
			contentTypeApi.delete(parentContentType);

			//Check that the relationship is deleted
			assertTrue("Relationship Still Exists",APILocator.getRelationshipAPI().byContentType(childContentType).isEmpty());

		} finally{
			if(parentContentType != null){
				contentTypeApi.delete(parentContentType);
			}
			if(childContentType != null){
				contentTypeApi.delete(childContentType);
			}
		}
	}

	/**
	 * This test creates a 2 content types and a Relationship Field on the parent,
	 * then deletes the child content type so the Relationship Field on the parent and the relationship
	 * must be deleted.
	 *
	 * @throws Exception
	 */
	@Test
	public void testDeleteContentTypeChild_deleteRelationshipFieldOnParent_deleteRelationship() throws Exception{
		ContentType parentContentType = null;
		ContentType childContentType = null;
		try {
			//Create content types
			parentContentType = createContentType("parentContentType" + System.currentTimeMillis());
			childContentType = createContentType("childContentType" + System.currentTimeMillis());

			//Create Relationship Field
			final Field field = createRelationshipField("testRelationship",parentContentType.id(),childContentType.variable());

			//Check that the parentContentType has the field
			parentContentType = contentTypeApi.find(parentContentType.id());
			assertEquals(1,parentContentType.fields().size());

			//Check that the relationship exists
			assertEquals(1,APILocator.getRelationshipAPI().byContentType(childContentType).size());

			//Delete childContentType
			contentTypeApi.delete(childContentType);

			//Check that the field is deleted on the parentContentType
			parentContentType = contentTypeApi.find(parentContentType.id());
			assertEquals(0,parentContentType.fields().size());

			//Check that the relationship is deleted
			assertTrue("Relationship Still Exists",APILocator.getRelationshipAPI().byContentType(parentContentType).isEmpty());

		} finally{
			if(parentContentType != null){
				contentTypeApi.delete(parentContentType);
			}
			if(childContentType != null){
				contentTypeApi.delete(childContentType);
			}
		}
	}

	/**
	 * This test creates a 2 content types and a Relationship Field on both content types(relationship both ways),
	 * then deletes the parent content type so the Relationship Field on the child and the relationship
	 * must be deleted.
	 *
	 * @throws Exception
	 */
	@Test
	public void testDeleteContentTypeParent_deleteRelationshipFieldOnChild_deleteRelationship_bothWays() throws Exception{
		ContentType parentContentType = null;
		ContentType childContentType = null;
		try {
			//Create content types
			parentContentType = createContentType("parentContentType" + System.currentTimeMillis());
			childContentType = createContentType("childContentType" + System.currentTimeMillis());

			//Create Relationship Field on Parent
			final Field parentRelationshipField = createRelationshipField("testRelationship",parentContentType.id(),childContentType.variable());

			//Check that the parentContentType has the field
			parentContentType = contentTypeApi.find(parentContentType.id());
			assertEquals(1,parentContentType.fields().size());

			//Check that the relationship exists
			assertEquals(1,APILocator.getRelationshipAPI().byContentType(childContentType).size());

			//Create Relationship Field on Child
			createRelationshipField("testRelationship",childContentType.id(),parentContentType.variable()+"."+parentRelationshipField.variable());

			//Check that the childContentType has the field
			childContentType = contentTypeApi.find(childContentType.id());
			assertEquals(1,childContentType.fields().size());

			//Check that there is only one relationship regardless the 2 fields
			assertEquals(1,APILocator.getRelationshipAPI().byContentType(childContentType).size());

			//Delete parentContentType
			contentTypeApi.delete(parentContentType);

			//Check that the field is deleted on the childContentType
			childContentType = contentTypeApi.find(childContentType.id());
			assertEquals(0,childContentType.fields().size());

			//Check that the relationship is deleted
			assertTrue("Relationship Still Exists",APILocator.getRelationshipAPI().byContentType(childContentType).isEmpty());

		} finally{
			if(parentContentType != null){
				contentTypeApi.delete(parentContentType);
			}
			if(childContentType != null){
				contentTypeApi.delete(childContentType);
			}
		}
	}

	/**
	 * This test creates a 2 content types and a Relationship Field on both content types(relationship both ways),
	 * then deletes the child content type so the Relationship Field on the parent and the relationship
	 * must be deleted.
	 *
	 * @throws Exception
	 */
	@Test
	public void testDeleteContentTypeChild_deleteRelationshipFieldOnParent_deleteRelationship_bothWays() throws Exception{
		ContentType parentContentType = null;
		ContentType childContentType = null;
		try {
			//Create content types
			parentContentType = createContentType("parentContentType" + System.currentTimeMillis());
			childContentType = createContentType("childContentType" + System.currentTimeMillis());

			//Create Relationship Field on Parent
			final Field parentRelationshipField = createRelationshipField("testRelationship",parentContentType.id(),childContentType.variable());

			//Check that the parentContentType has the field
			parentContentType = contentTypeApi.find(parentContentType.id());
			assertEquals(1,parentContentType.fields().size());

			//Check that the relationship exists
			assertEquals(1,APILocator.getRelationshipAPI().byContentType(childContentType).size());

			//Create Relationship Field on Child
			createRelationshipField("testRelationship",childContentType.id(),parentContentType.variable()+"."+parentRelationshipField.variable());

			//Check that the childContentType has the field
			childContentType = contentTypeApi.find(childContentType.id());
			assertEquals(1,childContentType.fields().size());

			//Check that there is only one relationship regardless the 2 fields
			assertEquals(1,APILocator.getRelationshipAPI().byContentType(childContentType).size());

			//Delete childContentType
			contentTypeApi.delete(childContentType);

			//Check that the field is deleted on the parentContentType
			parentContentType = contentTypeApi.find(parentContentType.id());
			assertEquals(0,parentContentType.fields().size());

			//Check that the relationship is deleted
			assertTrue("Relationship Still Exists",APILocator.getRelationshipAPI().byContentType(parentContentType).isEmpty());

		} finally{
			if(parentContentType != null){
				contentTypeApi.delete(parentContentType);
			}
			if(childContentType != null){
				contentTypeApi.delete(childContentType);
			}
		}
	}


	 @Test
	  public void test_get_fields_filtered_by_class() throws Exception{

	   ContentType newType = ContentTypeBuilder.builder(BaseContentType.FILEASSET.immutableClass())
	        .description("description").folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
	        .name("ContentTypeTesting"+System.currentTimeMillis()).owner("owner").variable("velocityVarNameTesting"+System.currentTimeMillis()).build();
	    newType = contentTypeApi.save(newType);


	    List<Field> fields = newType.fields(BinaryField.class);
		 assertTrue("There must be only one Binary Field in this test File Asset sub-type", fields.size() == 1);

      //Add Field.
      fields = new ArrayList<>( newType.fields() );

      Field fieldToSave = FieldBuilder.builder( TextField.class )
              .name( "test"+System.currentTimeMillis() )
              .variable( "test"+System.currentTimeMillis() )
              .contentTypeId( newType.id() )
              .dataType( DataTypes.TEXT )
              .fixed( true )
              .id( UUIDGenerator.generateUuid() )
              .build();

      fields.add( fieldToSave );

      newType = contentTypeApi.save( newType, fields );


      fields = newType.fields(TextField.class);
	  assertTrue("There must be only two Text Field in this test File Asset sub-type", fields.size() == 2);

      fields = newType.fields(SelectField.class);
      assert(fields.size()==0);
	 }


	 /**
	  * This test is to ensure that a content type is returning the correct
	  * parent permissionable based on where it lives in the hierarchy
	  * If the content type lives on a folder, then that will be the parent
	  * if it lives on a host, then that, else the system host
	  * @throws Exception
	  */
   @Test
   public void test_content_type_parent_permissionable() throws Exception{
     Host site = new SiteDataGen().nextPersisted();
     Folder folder = new FolderDataGen().site(site).nextPersisted();


     ContentType systemHostType = ImmutableSimpleContentType.builder()
         .name("ContentTypeTesting"+System.currentTimeMillis())
         .variable("velocityVarNameTesting"+System.currentTimeMillis())

         .host(APILocator.systemHost().getIdentifier())
         .folder(APILocator.getFolderAPI().SYSTEM_FOLDER)
         .build();
     systemHostType = contentTypeApi.save(systemHostType);

     ContentType hostType = ImmutableSimpleContentType.builder()
         .name("ContentTypeTesting"+System.currentTimeMillis())
         .variable("velocityVarNameTesting"+System.currentTimeMillis())
         .host(site.getIdentifier())
         .folder(APILocator.getFolderAPI().SYSTEM_FOLDER)
         .build();
     hostType = contentTypeApi.save(hostType);

     ContentType folderType = ImmutableSimpleContentType.builder()
         .name("ContentTypeTesting"+System.currentTimeMillis())
         .variable("velocityVarNameTesting"+System.currentTimeMillis())
         .host(site.getIdentifier())
         .folder(folder.getInode())
         .build();

     folderType = contentTypeApi.save(folderType);

     TestCase.assertEquals(systemHostType.getParentPermissionable(), APILocator.systemHost());
     TestCase.assertEquals(hostType.getParentPermissionable(), site);
     TestCase.assertEquals(folderType.getParentPermissionable(), folder);
   }


     /**
      * When dotCMS starts up and there is no persisted data, we need to instanciate
      * the content types before we can save the content.  This means that things like
      * host lookups will fail. So instead of sending the Host as a parent permissionable,
      * we send a PermissionProxy that has the same data which can be used temporarilly to
      * calcuate the correct permission inheratance.
      */
     @Test
     public void test_content_type_parent_permissionable_when_no_data() throws Exception{
     // test inheritance if no data available

     SimpleContentType fakeType = ImmutableSimpleContentType.builder()
         .name("ContentTypeTesting"+System.currentTimeMillis())
         .variable("velocityVarNameTesting"+System.currentTimeMillis())
         .host("fakeHost")
         .folder("fakeFolder")
         .build();

     assert(fakeType.getParentPermissionable() instanceof PermissionableProxy);

     assertEquals(fakeType.getParentPermissionable().getPermissionId(), "fakeFolder" );
     fakeType = ImmutableSimpleContentType.copyOf(fakeType).withFolder(Folder.SYSTEM_FOLDER);
     assertEquals(fakeType.getParentPermissionable().getPermissionId(), "fakeHost" );
     fakeType = ImmutableSimpleContentType.copyOf(fakeType).withHost(Host.SYSTEM_HOST);
     assertEquals(fakeType.getParentPermissionable().getPermissionId(), Host.SYSTEM_HOST );

   }


     @Test
     public void test_get_fields_by_type() throws Exception{

       ContentType testType = new ContentTypeDataGen().baseContentType(BaseContentType.FORM).nextPersisted();

       assertThat("a form has four fields",testType.fields().size()==3);
       assertThat("Filtering fields by immutable type", testType.fields(ImmutableConstantField.class).size()==2);
       assertThat("Filtering fields by field type",testType.fields(ConstantField.class).size()==2);
       assertThat("Filtering fields by immutable type",testType.fields(HostFolderField.class).size()==1);
       assertThat("Filtering fields by field type",testType.fields(ImmutableHostFolderField.class).size()==1);
     }

	/***
	 * If you create a CT with the same name than existing one, the number at the end of the variable should increase
	 * instead of concat the next number.
	 * E.g
	 * Name               |       Variable
	 * newContentType           newContentType
	 * newContentType           newContentType1
	 * newContentType           newContentType2
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	@Test
	public void testSaveContentTypeWithSameName_ShouldIncreaseNumberInsteadConcat()
			throws DotSecurityException, DotDataException {

		ContentType type1 = null;
		ContentType type2 = null;
		ContentType type3 = null;
		final String contentTypeName = "newContentType";
		try {
			type1 = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.name(contentTypeName)
							.owner(user.getUserId())
							.build());

			Assert.assertTrue("Got instead:" + type1.variable(), type1.variable().equalsIgnoreCase(contentTypeName));

			type2 = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.name(contentTypeName)
							.owner(user.getUserId())
							.build());

			Assert.assertTrue("Got instead:" + type2.variable(), type2.variable().equalsIgnoreCase(contentTypeName + 1));

			type3 = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.name(contentTypeName)
							.owner(user.getUserId())
							.build());

			Assert.assertTrue("Got instead:" + type3.variable() ,type3.variable().equalsIgnoreCase(contentTypeName + 2));
		} finally {
			if(type1!=null) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type1);
			}
			if(type2!=null) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type2);
			}
			if(type3!=null) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type3);
			}
		}
	}

	@DataProvider
	public static Object[] getReservedContentTypeVars() {
		return ContentTypeFactoryImpl.reservedContentTypeVars.toArray();
	}

	/***
	 * If you try to create a CT with a reserved name, it should alter the variable to avoid
	 * conflicts. Reserved content type variables: {@link ContentTypeFactoryImpl#reservedContentTypeVars }
	 *
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	@Test
	@UseDataProvider("getReservedContentTypeVars")
	public void testSaveContentTypeWithReservedVar_ShouldUseDifferentVar(final String reservedVar)
			throws DotSecurityException, DotDataException {

		// Skipping "host" case since it is also a forbidden content type name but will throw exception. com.dotcms.contenttype.business.ContentTypeAPI.reservedStructureNames
		if(reservedVar.equalsIgnoreCase("host")) return;

		ContentType type = null;
		try {
			type = APILocator.getContentTypeAPI(APILocator.systemUser())
					.save(ContentTypeBuilder
							.builder(SimpleContentType.class)
							.folder(FolderAPI.SYSTEM_FOLDER)
							.host(Host.SYSTEM_HOST)
							.name(reservedVar)
							.owner(user.getUserId())
							.build());

			assertNotEquals(reservedVar, type.variable());
		} finally {
			if(type!=null) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type);
			}
		}
	}



	/**
	 * Method to test: {@link ContentTypeAPI#findAllRespectingLicense()}
	 * Given scenario: Valid EE license
	 * Expected result: {@link EnterpriseType}s included in returned List
	 */
	@Test
	public void test_findAllRespectingLicense_whenLicense_IncludeEETypes() throws Exception {
		assertFalse(APILocator.getContentTypeAPI(APILocator.systemUser()).findAllRespectingLicense()
				.stream().noneMatch((type) -> type instanceof EnterpriseType));

	}

    /**
     * Method to test: {@link ContentTypeAPI#isContentTypeAllowed(ContentType)}
     * Given scenario: The method is tested for each {@link BaseContentType} using an enterprise license
     * Expected result: The method should return true
     */
	@Test
	public void testIsContentTypeAllowedReturnsTrue(){
        assertTrue(Arrays.asList(BaseContentType.values()).stream()
                .filter(type -> type != BaseContentType.ANY).allMatch(
                        type -> {
                            try {
                                return APILocator.getContentTypeAPI(APILocator.systemUser())
                                        .isContentTypeAllowed(
                                                new ContentTypeDataGen().baseContentType(type)
                                                        .next());
                            } catch (Exception e) {
                                return false;
                            }
                        }));
    }



	/***
	 * Method to test: {@link ContentTypeAPI#save(ContentType)}
	 * Given Scenario: We had a bug where when no site id was passed to the endpoint the CT would still get created but the list of fields would get lost.
	 * Expected Result: If no site is set the CT should be placed under System Host's Umbrella.
 	 * @param testCase
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	@Test
	@UseDataProvider("getSites")
	public void Fields_Should_Not_Get_Removed_When_Host_Is_Empty_Null(final SiteTestCase testCase)
			throws DotSecurityException, DotDataException {

		ContentType contentType = null;
		ContentType savedContentType = null;
		try {

			final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();

			fields.add(
						new FieldDataGen()
								.name("Site or Folder")
								.velocityVarName("hostfolder")
								.sortOrder(1)
								.required(Boolean.TRUE)
								.type(HostFolderField.class)
								.next()
			);

			fields.add(
					new FieldDataGen()
							.name("Title")
							.velocityVarName("title")
							.sortOrder(2)
							.type(TextField.class)
							.next()
			);

			fields.add(
					new FieldDataGen()
							.name(FILE_ASSET_1)
							.velocityVarName(FILE_ASSET_1)
							.sortOrder(3)
							//The only way we can guarantee a field won't be indexed is by setting all these to false
							.indexed(false).searchable(false).listed(false).unique(false)
							.type(BinaryField.class)
							.next()
			);

			fields.add(
					new FieldDataGen()
							.name(FILE_ASSET_2)
							.velocityVarName(FILE_ASSET_2)
							.sortOrder(4)
							.indexed(true)
							.type(BinaryField.class)
							.next()
			);

            final Host site = testCase.site;

            final String contentTypeName = "ContentTypeXYZ" + System.currentTimeMillis();

			contentType = ImmutableSimpleContentType.builder()
					.name(contentTypeName)
					.variable(contentTypeName)
					//There's mocked site that is intended to simulate the missing id
					//Therefore here we always pass it
					.host(site.getIdentifier())
					.build();

            contentType.constructWithFields(fields);
			savedContentType = APILocator.getContentTypeAPI(APILocator.systemUser()).save(contentType);
			//Here we test that the fields are still present even when the site is null
			Assert.assertFalse(savedContentType.fields().isEmpty());
			assertNotNull(savedContentType.host());

			final String expected = DEFAULT.equals(testCase.expected) ? APILocator.getHostAPI()
					.findDefaultHost(APILocator.systemUser(), false).getIdentifier()
					: testCase.expected;

			assertEquals(expected, savedContentType.host());

		} finally {
			if(null != savedContentType) {
				APILocator.getContentTypeAPI(APILocator.systemUser()).delete(savedContentType);
			}
		}
	}

	static final String DEFAULT = "default";

	@DataProvider
	public static Object[] getSites() throws DotSecurityException {

		final Host emptyIdentifierHost = Mockito.mock(Host.class);
		Mockito.when(emptyIdentifierHost.getIdentifier()).thenReturn("");

		final Host systemHost = Mockito.mock(Host.class);
		Mockito.when(systemHost.getIdentifier()).thenReturn(Host.SYSTEM_HOST);
		Mockito.when(systemHost.getHostname()).thenReturn(Host.SYSTEM_HOST_NAME);

		final Host namedHost = Mockito.mock(Host.class);
		//At this moment (When the data provider is getting instantiated)
		//it is troublesome getting the Current default site therefore we're going to set this placeholder
		Mockito.when(namedHost.getIdentifier()).thenReturn("unk");
		Mockito.when(namedHost.getHostname()).thenReturn(DEFAULT);

		return new Object[]{
				new SiteTestCase(emptyIdentifierHost, DEFAULT),
				new SiteTestCase(systemHost, Host.SYSTEM_HOST),
				new SiteTestCase(namedHost, DEFAULT)
		};
	}

	static class SiteTestCase{

    	final Host site;

    	final String expected;

		public SiteTestCase(final Host site, final String expected) {
			this.site = site;
			this.expected = expected;
		}

		@Override
		public String toString() {
			return "SiteTestCase{" +
					"site=" + site +
					", expected='" + expected + '\'' +
					'}';
		}
	}

	/**
	 * Method to test: {@link ContentTypeAPIImpl#findUrlMappedPattern(String)}
	 * When: Create a {@link ContentType} with a urlMapPattern and detailPage
	 * and call the method with the page's id
	 * Should: Return the {@link ContentType}
	 */
	@Test
	public void findUrlMappedByPageId() throws DotDataException {
		final Host host = new SiteDataGen().nextPersisted();
		final Template template = new TemplateDataGen().nextPersisted();
		final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

		final ContentType contentType_1 = new ContentTypeDataGen()
				.detailPage(htmlPageAsset.getIdentifier())
				.urlMapPattern("/test_1")
				.nextPersisted();

		final ContentType contentType_2 = new ContentTypeDataGen()
				.detailPage(htmlPageAsset.getIdentifier())
				.urlMapPattern("/test_2")
				.nextPersisted();

		final List<String> urlMapped = APILocator.getContentTypeAPI(APILocator.systemUser())
				.findUrlMappedPattern(htmlPageAsset.getIdentifier());

		Assert.assertEquals(2, urlMapped.size());
		Assert.assertTrue(urlMapped.contains("/test_1"));
		Assert.assertTrue(urlMapped.contains("/test_2"));
	}

	/**
	 * Method to test: {@link ContentTypeAPIImpl#findUrlMappedPattern(String)}
	 * When: Called the method with a Page'id that not have any ContentType link with it
	 * Should: Return an empty list
	 */
	@Test
	public void findUrlMappedByPageIdWithoutContentType() throws DotDataException {
		final Host host = new SiteDataGen().nextPersisted();
		final Template template = new TemplateDataGen().nextPersisted();
		final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

		final List<String> urlMapped = APILocator.getContentTypeAPI(APILocator.systemUser())
				.findUrlMappedPattern(htmlPageAsset.getIdentifier());

		Assert.assertTrue(urlMapped.isEmpty());
	}

	/**
	 * Method to test: {@link ContentTypeAPIImpl#findUrlMappedPattern(String)}
	 * when: Called the method with Null
	 * should: Throw a {@link IllegalArgumentException}
	 */
	@Test(expected = IllegalArgumentException.class)
	public void findUrlMappedByPageIdWithNull() throws DotDataException {
		APILocator.getContentTypeAPI(APILocator.systemUser())
				.findUrlMappedPattern(null);
	}


	/**
	 * Method to test: {@link com.dotcms.enterprise.publishing.PublishDateUpdater#getContentTypeVariableWithPublishField()}  }  }}
	 * Given Scenario: Add empty validation on publish and Expire date of the content type
	 * ExpectedResult: return only content types with publish date != of null o empty
	 *
	 * @throws DotDataException
	 * @throws DotSecurityException
	 *
	 */

	@Test
	public void test_getContentTypeVariableWithPublishField_ShouldReturnNonNullOrEmpty() throws Exception {
		long time = System.currentTimeMillis();
		int base = BaseContentType.CONTENT.ordinal();
		//create a new content type with publish date
		ContentType contentType = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(base))
				.description("ContentTypeWithPublishField " + time).folder(FolderAPI.SYSTEM_FOLDER)
				.host(Host.SYSTEM_HOST).name("ContentTypeWithPublishField " + time)
				.owner(APILocator.systemUser().toString()).variable("testContentVar"+time).publishDateVar("publishDate")
				.build();

		contentType = contentTypeApi.save(contentType);

		//validate if the content exist
		assertThat("Content was not created correctly", contentTypeApi.find(contentType.inode()) != null);

		//create the publishing date field
		List<Field> newFieldsList = new ArrayList<>();
		Field fieldToSave = FieldBuilder.builder(DateTimeField.class).name("Publish Date").variable("publishDate")
				.contentTypeId(contentType.id()).dataType(DataTypes.DATE).indexed(true).build();

		newFieldsList.add(fieldToSave);

		contentType = contentTypeApi.save(contentType, newFieldsList);

		//get all the objects in the structure != null or empty
		List<String> structureList = PublishDateUpdater.getContentTypeVariableWithPublishField();

		//validate if the content type name appears in the retrieved data
		assertTrue("The publish date of the content is empty or null", structureList.contains(contentType.variable()));

		//remove the published dates
		contentType = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(base))
				.description("ContentTypeWithPublishExpireFields " + time).folder(FolderAPI.SYSTEM_FOLDER)
				.host(Host.SYSTEM_HOST).name("ContentTypeWithPublishExpireFields " + time)
				.id(contentType.inode())
				.owner(APILocator.systemUser().toString()).variable(contentType.variable()).publishDateVar("")
				.expireDateVar("").build();

		contentType = contentTypeApi.save(contentType);

		structureList = PublishDateUpdater.getContentTypeVariableWithPublishField();

		//validate that the content type name didn't appear in the retrieved data
		assertFalse("Publish date wasn't deleted", structureList.contains(contentType.variable()));

	}

	/**
	 * Method to test: {@link ContentTypeAPIImpl#countContentTypeAssignedToNotSystemWorkflow()}
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

		final long countAfter = APILocator.getContentTypeAPI(APILocator.systemUser())
				.countContentTypeAssignedToNotSystemWorkflow();

		Assert.assertEquals(countBefore + 1, countAfter);

	}

	/**
	 * <ul>
	 *     <li><b>Method to test: </b>{@link ContentTypeAPI#deleteSync(ContentType)} (ContentType)}</li>
	 *     <li><b>Given Scenario: </b>Creates a test Content Type and deletes it.</li>
	 *     <li><b>Expected Result: </b>This method will delete the specified Content Type BUT it
	 *     will be done in the same transaction. So, requesting it to the API immediately will throw
	 *     a {@link NotFoundInDbException}.</li>
	 * </ul>
	 */
	@Test(expected = NotFoundInDbException.class)
	public void deleteContentTypeSync() throws DotDataException, DotSecurityException {
		// ╔══════════════════╗
		// ║  Initialization  ║
		// ╚══════════════════╝
		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
		final String testTypeName = "My Test Content Type-" + System.currentTimeMillis();

		// ╔════════════════════════╗
		// ║  Generating Test data  ║
		// ╚════════════════════════╝
		final ContentType testType = createContentType(testTypeName);
		contentTypeAPI.deleteSync(testType);

		// ╔══════════════╗
		// ║  Assertions  ║
		// ╚══════════════╝
		// This call must throw the expected Exception
		contentTypeAPI.find(testType.variable());
	}

	/**
	 * <ul>
	 *     <li><b>Method to test:
	 *     </b>{@link ContentTypeAPI#copyFromAndDependencies(CopyContentTypeBean)}</li>
	 *     <li><b>Given Scenario: </b>Creates a test Content Type and a test Workflow Schemes. Then,
	 *     assigns it to the test Content Type. Finally, copies the source Content Type and its
	 *     dependencies.</li>
	 *     <li><b>Expected Result: </b>The copied Content Type must point to the same Workflow
	 *     Scheme as it was copied with dependencies.</li>
	 * </ul>
	 */
	@Test
	public void testCopyFromAndDependencies() throws DotDataException, DotSecurityException {
		// ╔══════════════════╗
		// ║  Initialization  ║
		// ╚══════════════════╝
		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
		final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
		final long millis = System.currentTimeMillis();
		ContentType sourceContentType = null;
		ContentType copiedContentType = null;
		WorkflowScheme workflowScheme = null;
		try {
			// ╔════════════════════════╗
			// ║  Generating Test data  ║
			// ╚════════════════════════╝
			final List<Field> fields = new ArrayList<>();
			fields.add(new FieldDataGen().name("Title").velocityVarName("title").next());
			fields.add(new FieldDataGen().name("Author").velocityVarName("author").next());
			fields.add(new FieldDataGen().type(WysiwygField.class).name("Description").velocityVarName("description").next());
			sourceContentType = new ContentTypeDataGen()
					.host(APILocator.systemHost()).description("This is a description of my test Content Type")
					.fields(fields)
					.nextPersisted();
			sourceContentType = contentTypeAPI.save(sourceContentType);
			workflowScheme = new WorkflowDataGen().name("Test Workflow_" + millis).nextPersisted();
			final Set<String> schemesIds = new HashSet<>();
			schemesIds.add(workflowScheme.getId());
			workflowAPI.saveSchemeIdsForContentType(sourceContentType, schemesIds);
			final String newVariableName = sourceContentType.variable() + millis;
			copiedContentType =
					contentTypeAPI.copyFromAndDependencies(new CopyContentTypeBean.Builder().sourceContentType(sourceContentType).name(newVariableName).newVariable(newVariableName).build());

			// ╔══════════════╗
			// ║  Assertions  ║
			// ╚══════════════╝
			assertEquals("The copied Content Type MUST have a different Velocity Variable Name", newVariableName, copiedContentType.variable());
			final Map<String, Field> sourceTypeFieldMap = sourceContentType.fieldMap();
			final Map<String, Field> copiedTypeFieldMap = copiedContentType.fieldMap();
			assertEquals("Both Content Types MUST have the same number of fields", sourceTypeFieldMap.size(), copiedTypeFieldMap.size());
			for (final String fieldName : sourceTypeFieldMap.keySet()) {
				assertTrue(String.format("The copied Content Type must have a field with name '%s'", fieldName), copiedTypeFieldMap.containsKey(fieldName));
			}
			final List<WorkflowScheme> schemesForSourceContentType = workflowAPI.findSchemesForContentType(sourceContentType);
			final List<WorkflowScheme> schemesForCopiedContentType = workflowAPI.findSchemesForContentType(copiedContentType);
			assertEquals("", schemesForSourceContentType.size(), schemesForCopiedContentType.size());
			if (!schemesForSourceContentType.containsAll(schemesForCopiedContentType)) {
				fail("The Workflow Schemes from both Content Types MUST be the same");
			}
		} finally {
			// ╔═══════════╗
			// ║  Cleanup  ║
			// ╚═══════════╝
			if (null != sourceContentType) {
				contentTypeAPI.delete(sourceContentType);
			}
			if (null != copiedContentType) {
				contentTypeAPI.delete(copiedContentType);
			}
			if (null != workflowScheme) {
                try {
					workflowAPI.archive(workflowScheme, APILocator.systemUser());
                    workflowAPI.deleteScheme(workflowScheme, APILocator.systemUser());
                } catch (final AlreadyExistException e) {
                    // Failed to delete the scheme. Just move on
                }
            }
		}
	}

	/**
	 * <ul>
	 *     <li><b>Method to test:
	 *     </b>{@link ContentTypeAPI#copyFromAndDependencies(CopyContentTypeBean, Host)}</li>
	 *     <li><b>Given Scenario: </b>Creates a test Content Type and a test Workflow Schemes. Then,
	 *     assigns it to the test Content Type. Create a new test Site as well. Finally, copies the
	 *     source Content Type and its dependencies to the new Site.</li>
	 *     <li><b>Expected Result: </b>The copied Content Type must point to the same Workflow
	 *     Scheme as it was copied with dependencies, and also point to the new Site.</li>
	 * </ul>
	 */
	@Test
	public void testCopyFromAndDependenciesToAnotherSite() throws DotDataException, DotSecurityException {
		// ╔══════════════════╗
		// ║  Initialization  ║
		// ╚══════════════════╝
		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
		final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
		final long millis = System.currentTimeMillis();
		final String siteTwoName = "sitetwo" + millis +".com";
		ContentType sourceContentType = null;
		ContentType copiedContentType = null;
		WorkflowScheme workflowScheme = null;
		try {
			// ╔════════════════════════╗
			// ║  Generating Test data  ║
			// ╚════════════════════════╝
			final List<Field> fields = new ArrayList<>();
			fields.add(new FieldDataGen().name("Title").velocityVarName("title").next());
			fields.add(new FieldDataGen().name("Author").velocityVarName("author").next());
			fields.add(new FieldDataGen().type(WysiwygField.class).name("Description").velocityVarName("description").next());
			sourceContentType = new ContentTypeDataGen()
					.host(APILocator.systemHost()).description("This is a description of my test Content Type")
					.fields(fields)
					.nextPersisted();
			sourceContentType = contentTypeAPI.save(sourceContentType);
			final Host siteTwo = new SiteDataGen().name(siteTwoName).nextPersisted();
			workflowScheme = new WorkflowDataGen().name("Test Workflow_" + millis).nextPersisted();
			final Set<String> schemesIds = new HashSet<>();
			schemesIds.add(workflowScheme.getId());
			workflowAPI.saveSchemeIdsForContentType(sourceContentType, schemesIds);
			final String newVariableName = sourceContentType.variable() + millis;
			copiedContentType =
					contentTypeAPI.copyFromAndDependencies(new CopyContentTypeBean.Builder().sourceContentType(sourceContentType).name(newVariableName).newVariable(newVariableName).build(), siteTwo);

			// ╔══════════════╗
			// ║  Assertions  ║
			// ╚══════════════╝
			assertNotEquals("Both Content Types MUST belong to different Sites", sourceContentType.siteName(), copiedContentType.siteName());
			assertEquals("The copied Content Type MUST have a different Velocity Variable Name", newVariableName, copiedContentType.variable());
			final Map<String, Field> sourceTypeFieldMap = sourceContentType.fieldMap();
			final Map<String, Field> copiedTypeFieldMap = copiedContentType.fieldMap();
			assertEquals("Both Content Types MUST have the same number of fields", sourceTypeFieldMap.size(), copiedTypeFieldMap.size());
			for (final String fieldName : sourceTypeFieldMap.keySet()) {
				assertTrue(String.format("The copied Content Type must have a field with name '%s'", fieldName), copiedTypeFieldMap.containsKey(fieldName));
			}
			final List<WorkflowScheme> schemesForSourceContentType = workflowAPI.findSchemesForContentType(sourceContentType);
			final List<WorkflowScheme> schemesForCopiedContentType = workflowAPI.findSchemesForContentType(copiedContentType);
			assertEquals("", schemesForSourceContentType.size(), schemesForCopiedContentType.size());
			if (!schemesForSourceContentType.containsAll(schemesForCopiedContentType)) {
				fail("The Workflow Schemes from both Content Types MUST be the same");
			}
		} finally {
			// ╔═══════════╗
			// ║  Cleanup  ║
			// ╚═══════════╝
			if (null != sourceContentType) {
				contentTypeAPI.delete(sourceContentType);
			}
			if (null != copiedContentType) {
				contentTypeAPI.delete(copiedContentType);
			}
			if (null != workflowScheme) {
				try {
					workflowAPI.archive(workflowScheme, APILocator.systemUser());
					workflowAPI.deleteScheme(workflowScheme, APILocator.systemUser());
				} catch (final AlreadyExistException e) {
					// Failed to delete the scheme. Just move on
				}
			}
		}
	}

	/**
	 * <ul>
	 *     <li><b>Method to test:
	 *     </b>{@link ContentTypeAPI#copyFromAndDependencies(CopyContentTypeBean, Host, boolean)}</li>
	 *     <li><b>Given Scenario: </b>Creates a test Content Type and a test Workflow Schemes. Then,
	 *     assigns it to the test Content Type. Create a new test Site as well. Finally, copies the
	 *     source Content Type and its dependencies to the new Site WITHOUT copying Relationship
	 *     Fields.</li>
	 *     <li><b>Expected Result: </b>The copied Content Type must point to the same Workflow
	 *     Scheme as it was copied with dependencies, and also point to the new Site. Moreover, NO
	 *     Relationship Fields must be present.</li>
	 * </ul>
	 */
	@Test
	public void testCopyFromAndDependenciesToAnotherSiteWithNoRelationshipFields() throws DotDataException, DotSecurityException {
		// ╔══════════════════╗
		// ║  Initialization  ║
		// ╚══════════════════╝
		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
		final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
		final long millis = System.currentTimeMillis();
		final String siteTwoName = "sitetwo" + millis +".com";
		ContentType sourceContentType = null;
		ContentType copiedContentType = null;
		WorkflowScheme workflowScheme = null;
		try {
			// ╔════════════════════════╗
			// ║  Generating Test data  ║
			// ╚════════════════════════╝
			final List<Field> fields = new ArrayList<>();
			fields.add(new FieldDataGen().name("Title").velocityVarName("title").next());
			fields.add(new FieldDataGen().type(RelationshipField.class)
					.name("First Related Content").velocityVarName("firstRelatedContent").relationType("webPageContent").values("0").next());
			fields.add(new FieldDataGen().name("Author").velocityVarName("author").next());
			fields.add(new FieldDataGen().type(WysiwygField.class).name("Description").velocityVarName("description").next());
			fields.add(new FieldDataGen().type(RelationshipField.class)
					.name("Second Related Content").velocityVarName("secondRelatedContent").relationType("webPageContent").values("0").next());
			sourceContentType = new ContentTypeDataGen()
					.host(APILocator.systemHost()).description("This is a description of my test Content Type")
					.fields(fields)
					.nextPersisted();
			sourceContentType = contentTypeAPI.save(sourceContentType);
			final Host siteTwo = new SiteDataGen().name(siteTwoName).nextPersisted();
			workflowScheme = new WorkflowDataGen().name("Test Workflow_" + millis).nextPersisted();
			final Set<String> schemesIds = new HashSet<>();
			schemesIds.add(workflowScheme.getId());
			workflowAPI.saveSchemeIdsForContentType(sourceContentType, schemesIds);
			final String newVariableName = sourceContentType.variable() + millis;
			// Pass down the flag to skip copying Relationship fields
			copiedContentType =
					contentTypeAPI.copyFromAndDependencies(
							new CopyContentTypeBean.Builder().sourceContentType(sourceContentType).name(newVariableName).newVariable(newVariableName).build(),
							siteTwo,
							false);

			// ╔══════════════╗
			// ║  Assertions  ║
			// ╚══════════════╝
			assertEquals("Source Content Types MUST belong to System Host", "systemHost", sourceContentType.siteName());
			assertEquals("Copied Content Types MUST belong to the second test Site", siteTwoName, copiedContentType.siteName());
			assertEquals("The copied Content Type MUST have a different Velocity Variable Name",
					newVariableName, copiedContentType.variable());
			final Map<String, Field> copiedTypeFieldMap = copiedContentType.fieldMap();
			assertEquals("The copied Content Type must have ONLY 3 fields, as Relationship Fields were not copied",
					3,  copiedTypeFieldMap.size());
			copiedTypeFieldMap.forEach((fieldVarName, field)
					-> assertNotEquals("The copied Content Type must NOT have any Relationship Field",
                    RelationshipField.class, field.getClass()));
			final List<WorkflowScheme> schemesForSourceContentType = workflowAPI.findSchemesForContentType(sourceContentType);
			final List<WorkflowScheme> schemesForCopiedContentType = workflowAPI.findSchemesForContentType(copiedContentType);
			if (!schemesForSourceContentType.containsAll(schemesForCopiedContentType)) {
				fail("The Workflow Schemes from both Content Types MUST be the same");
			}
		} finally {
			// ╔═══════════╗
			// ║  Cleanup  ║
			// ╚═══════════╝
			if (null != sourceContentType) {
				contentTypeAPI.delete(sourceContentType);
			}
			if (null != copiedContentType) {
				contentTypeAPI.delete(copiedContentType);
			}
			if (null != workflowScheme) {
				try {
					workflowAPI.archive(workflowScheme, APILocator.systemUser());
					workflowAPI.deleteScheme(workflowScheme, APILocator.systemUser());
				} catch (final AlreadyExistException e) {
					// Failed to delete the scheme. Just move on
				}
			}
		}
	}

}
