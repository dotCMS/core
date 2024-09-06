package com.dotmarketing.portlets.contentlet.ajax;

import static com.dotcms.integrationtestutil.content.ContentUtils.createTestKeyValueContent;
import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.StringPool;
import com.liferay.util.servlet.SessionMessages;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dotcms.integrationtestutil.content.ContentUtils.createTestKeyValueContent;
import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 *
 * @author oswaldogallango
 *
 */
@RunWith(DataProviderRunner.class)
public class ContentletAjaxTest {

	private Language language;
	private Contentlet contentlet;
	private static User systemUser;
	private static Language defaultLang;
	private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static ContentletAPI contentletAPI;
    private static RelationshipAPI relationshipAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        systemUser = APILocator.systemUser();
        contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        fieldAPI = APILocator.getContentTypeFieldAPI();
        contentletAPI = APILocator.getContentletAPI();
        relationshipAPI = APILocator.getRelationshipAPI();
    }

    @DataProvider
    public static Object[] cardinalities() {
        return RELATIONSHIP_CARDINALITY.values();
    }

	/**
	 * Test problem on "Content Search" when switching on one language
	 * show all the contentlets
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Test
	public void issue5330() throws DotDataException, DotSecurityException{
		/*
		 * Creating language
		 */
		language = new LanguageDataGen().nextPersisted();

		/*
		 * Creating multilanguage contententlet
		 */
		Host host = APILocator.getHostAPI().findDefaultHost(systemUser, false);

		Structure structure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("webPageContent");
		contentlet = new Contentlet();
		contentlet.setContentTypeId(structure.getInode());
		contentlet.setHost(host.getIdentifier());
		contentlet.setLanguageId(defaultLang.getId());
		String title = "testIssue5330"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		contentlet.setStringProperty("title", title);
		contentlet.setStringProperty("body", "testIssue5330");
		contentlet.setHost(host.getIdentifier());
		contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);

		contentlet = contentletAPI.checkin(contentlet, systemUser,false);
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
		APILocator.getVersionableAPI().setLive(contentlet);
		contentletAPI.isInodeIndexed(contentlet.getInode(),true);

		String ident = contentlet.getIdentifier();
		contentlet = contentletAPI.findContentletByIdentifier(ident, true, defaultLang.getId(), systemUser, false);
		contentlet = contentletAPI.checkout(contentlet.getInode(), systemUser, false);
		contentlet.setLanguageId(language.getId());
		contentlet.setStringProperty("body", "italianTestIssue5330");
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);

		contentlet = contentletAPI.checkin(contentlet, systemUser,false);
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
		APILocator.getVersionableAPI().setLive(contentlet);
		contentletAPI.isInodeIndexed(contentlet.getInode(),true);
		/*
		 * Validate that there are two contentlets associated to the same identifier wit different languages
		 */
		List<Contentlet> contList = contentletAPI.getSiblings(ident);
		Assert.assertEquals(2, contList.size());

		/*
		 * Get english version
		 */
		List<String> fieldsValues = new ArrayList<>();
		fieldsValues.add("conHost");
		fieldsValues.add(host.getIdentifier());
		fieldsValues.add("webPageContent.title");
		fieldsValues.add(title);
		fieldsValues.add("languageId");
		fieldsValues.add(String.valueOf(defaultLang.getId()));
		List<String> categories = new ArrayList<>();

		List<Object> results=new ContentletAjax().searchContentletsByUser(structure.getInode(), fieldsValues, categories, false, false, false, false,1, "modDate Desc", 10,systemUser, null, null, null);
		Map<String,Object> result = (Map<String,Object>)results.get(0);
		Assert.assertEquals((Long)result.get("total"), Long.valueOf(1));
		result = (Map<String,Object>)results.get(3);
		assertTrue(Long.parseLong(String.valueOf(result.get("languageId")))==defaultLang.getId());
		contentlet = contentletAPI.find(String.valueOf(result.get("inode")),systemUser,false);
		contentletAPI.archive(contentlet,systemUser,false);
		contentletAPI.delete(contentlet,systemUser,false);

		/*
		 * Get italian version
		 */
		fieldsValues = new ArrayList<>();
		fieldsValues.add("conHost");
		fieldsValues.add(host.getIdentifier());
		fieldsValues.add("webPageContent.title");
		fieldsValues.add(title);
		fieldsValues.add("languageId");
		fieldsValues.add(String.valueOf(language.getId()));

		results=new ContentletAjax().searchContentletsByUser(structure.getInode(), fieldsValues, categories, false, false, false, false,1, "modDate Desc", 10,systemUser, null, null, null);
		result = (Map<String,Object>)results.get(0);
		Assert.assertEquals(Long.valueOf(1L), (Long)result.get("total"));
		result = (Map<String,Object>)results.get(3);
		assertTrue(Long.parseLong(String.valueOf(result.get("languageId")))==language.getId());
		contentlet = contentletAPI.find(String.valueOf(result.get("inode")),systemUser,false);
		contentletAPI.destroy(contentlet, systemUser, false);
	}

	@Test
	public void test_doSearchGlossaryTerm_ReturnsListLanguageVariables()
			throws Exception {
		language = new LanguageDataGen().nextPersisted();
		Contentlet languageVariable1 = null;
		Contentlet languageVariable2 = null;
		Contentlet languageVariable3 = null;
		try {

			long time = System.currentTimeMillis();

			final ContentType languageVariableContentType = contentTypeAPI
					.find(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);
			languageVariable1 = createTestKeyValueContent(
					"brought.you.by.IT"+time, "hello world", language.getId(),
					languageVariableContentType, systemUser);
			languageVariable2 = createTestKeyValueContent(
					"brought.you.by.Java"+time, "hello world", language.getId(),
					languageVariableContentType, systemUser);

			languageVariable3 = createTestKeyValueContent(
					"brought.you.by.Jay"+time, "hello world", language.getId(),
					languageVariableContentType, systemUser);

			final ContentletAjax contentletAjax = new ContentletAjax();
			List<String[]> languageVariablesList =
					contentletAjax.doSearchGlossaryTerm("brought.you.by.IT",String.valueOf(language.getId()));
			Assert.assertEquals("languageVariablesList: "
					+ languageVariablesList.toString(),1,languageVariablesList.size());
			languageVariablesList =
					contentletAjax.doSearchGlossaryTerm("brought.you.by.J",String.valueOf(language.getId()));
			Assert.assertEquals("languageVariablesList: "
					+ languageVariablesList.toString(),2,languageVariablesList.size());
			languageVariablesList =
					contentletAjax.doSearchGlossaryTerm("brought.you.by",String.valueOf(language.getId()));
			Assert.assertEquals("languageVariablesList: "
					+ languageVariablesList.toString(),3,languageVariablesList.size());

		}finally {
			if(languageVariable1 != null){
				deleteContentlets(systemUser,languageVariable1);
			}
			if(languageVariable2 != null){
				deleteContentlets(systemUser,languageVariable2);
			}
			if(languageVariable3 != null){
				deleteContentlets(systemUser,languageVariable3);
			}
		}

	}

    /**
     * <b>Method to Test:</b> {@link ContentletAjax#searchContentletsByUser(List, String, List, List,
     * boolean, boolean, boolean, boolean, int, String, int, User, HttpSession, String, String)}<p>
     * <b>When:</b> filtering by related content, results are returned correctly regardless of the
     * relationship's cardinality <p>
     * <b>Should:</b> Return results
     */
    @Test
    @UseDataProvider("cardinalities")
    public void test_searchContentletsByUser_filteringByRelatedContent_returns_validResults(
            RELATIONSHIP_CARDINALITY cardinality)
            throws DotSecurityException, DotDataException {

        final ContentletAjax contentletAjax = new ContentletAjax();

        final ContentType parentContentType = contentTypeAPI.save(
                ContentTypeBuilder.builder(SimpleContentType.class).folder(
                        FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("parentContentType")
                        .owner(systemUser.getUserId()).build());

        final ContentType childContentType = contentTypeAPI.save(
                ContentTypeBuilder.builder(SimpleContentType.class).folder(
                        FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("childContentType")
                        .owner(systemUser.getUserId()).build());

        Field fieldInChild = FieldBuilder.builder(RelationshipField.class).name("myNewRel")
                .contentTypeId(childContentType.id()).values(String.valueOf(cardinality.ordinal()))
                .relationType(parentContentType.variable()).build();

        //One side of the relationship is set childContentType --> parentContentType
        fieldInChild = fieldAPI.save(fieldInChild, systemUser);

        final String fullFieldVarInChild =
                childContentType.variable() + StringPool.PERIOD + fieldInChild.variable();

        Field fieldInParent = FieldBuilder.builder(RelationshipField.class).name("otherSide")
                .contentTypeId(parentContentType.id())
                .values(String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_ONE.ordinal()))
                .relationType(fullFieldVarInChild).build();

        //One side of the relationship is set parentContentType --> childContentType
        fieldInParent = fieldAPI.save(fieldInParent, systemUser);

        final String fullFieldVarInParent =
                parentContentType.variable() + StringPool.PERIOD + fieldInParent.variable();

        final Relationship relationship = relationshipAPI
                .getRelationshipFromField(fieldInChild, systemUser);

        //creates child contentlet
        final Contentlet childContentlet = new ContentletDataGen(childContentType.id())
                .nextPersisted();

        //creates parent contentlet
        final Contentlet parentContentlet = contentletAPI
                .checkin(new ContentletDataGen(parentContentType.id()).next(),
						Map.of(relationship, CollectionsUtils.list(childContentlet)),
                        null, systemUser, false);

        //Searching child related content
        List results = contentletAjax.searchContentletsByUser(ImmutableList.of(BaseContentType.ANY),
                childContentType.inode(),
                CollectionsUtils.list(fullFieldVarInChild, parentContentlet.getIdentifier()),
                Collections.emptyList(), false, false, false,
                false, 0, "moddate", 0, systemUser, null, null, null);

        assertNotNull(results);
        assertEquals(1, Integer.parseInt(((Map) results.get(0)).get("total").toString()));
        assertEquals(childContentlet.getIdentifier(), ((Map) results.get(3)).get("identifier"));

        //Searching parent related content
        results = contentletAjax.searchContentletsByUser(ImmutableList.of(BaseContentType.ANY),
                parentContentType.inode(),
                CollectionsUtils.list(fullFieldVarInParent, childContentlet.getIdentifier()),
                Collections.emptyList(), false, false, false,
                false, 0, "moddate", 0, systemUser, null, null, null);

        assertNotNull(results);
        assertEquals(1, Integer.parseInt(((Map) results.get(0)).get("total").toString()));
        assertEquals(parentContentlet.getIdentifier(), ((Map) results.get(3)).get("identifier"));
    }

	/**
	 * <b>Method to Test:</b> {@link ContentletAjax#searchContentletsByUser(List, String, List, List,
	 * boolean, boolean, boolean, boolean, int, String, int, User, HttpSession, String, String)}<p>
	 * <b>When:</b> filtering by specific dates or a date range<p>
	 * <b>Should:</b> Return results
	 */
	@Test
	public void test_searchContentletsByUser_filteringByDates_returns_validResults()
			throws DotDataException, DotSecurityException {

		final ContentType currentCalendarEventType = Try.of(()->contentTypeAPI.find("calendarEvent")).getOrNull();
		if (null != currentCalendarEventType) {

			contentTypeAPI.delete(currentCalendarEventType);
		}

		final ContentType eventContentType = new ContentTypeDataGen().velocityVarName("calendarEvent").nextPersisted();

		try {
			ContentTypeDataGen.addField(new FieldDataGen()
					.velocityVarName("title")
					.contentTypeId(eventContentType.id())
					.type(TextField.class)
					.nextPersisted());

			ContentTypeDataGen.addField(new FieldDataGen()
					.velocityVarName("startDate")
					.contentTypeId(eventContentType.id())
					.type(DateTimeField.class)
					.defaultValue("")
					.nextPersisted());

			ContentTypeDataGen.addField(new FieldDataGen()
					.velocityVarName("endDate")
					.contentTypeId(eventContentType.id())
					.type(DateTimeField.class)
					.defaultValue("")
					.nextPersisted());

			final ContentletAjax contentletAjax = new ContentletAjax();
			final Date currentDate = new Date();
			final String formattedDate = DateUtil.format(currentDate, "MM/dd/yyyy");
			final Contentlet event = new ContentletDataGen(eventContentType.id())
					.setProperty("title", "MyEvent" + System.currentTimeMillis())
					.setProperty("startDate", currentDate).setProperty("endDate", currentDate)
					.nextPersisted();

			//Filtering by specific dates
			List results = contentletAjax.searchContentletsByUser(
					ImmutableList.of(BaseContentType.ANY),
					eventContentType.inode(),
					CollectionsUtils.list("identifier", event.getIdentifier(),
							"calendarEvent.startDate",
							formattedDate),
					Collections.emptyList(), false, false, false,
					false, 0, "moddate", 0, systemUser, null, null, null);

			assertTrue(UtilMethods.isSet(results));
			assertEquals(1L, ((HashMap) results.get(0)).get("total"));
			assertEquals(event.getInode(), ((HashMap) results.get(3)).get("inode"));

			//Filtering by date range
			final StringBuilder dateRange = new StringBuilder();
			dateRange.append(StringPool.OPEN_BRACKET).append(formattedDate).append(" TO ")
					.append(formattedDate).append(StringPool.CLOSE_BRACKET);
			results = contentletAjax.searchContentletsByUser(ImmutableList.of(BaseContentType.ANY),
					eventContentType.inode(),
					CollectionsUtils.list("identifier", event.getIdentifier(),
							"calendarEvent.startDate",
							dateRange.toString()),
					Collections.emptyList(), false, false, false,
					false, 0, "moddate", 0, systemUser, null, null, null);

			assertTrue(UtilMethods.isSet(results));
			assertEquals(1L, ((HashMap) results.get(0)).get("total"));
			assertEquals(event.getInode(), ((HashMap) results.get(3)).get("inode"));
		}finally {
			contentTypeAPI.delete(eventContentType);
		}
	}

	private static void setUpContext() {
		final HttpSession session = mock(HttpSession.class);
		Mockito.when(session.getAttribute(SessionMessages.KEY)).thenReturn(new LinkedHashMap());

		final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
		Mockito.when(httpServletRequest.getSession()).thenReturn(session);
		Mockito.when(httpServletRequest.getAttribute(WebKeys.USER)).thenReturn(APILocator.systemUser());

		final WebContext webContext = mock(WebContext.class);
		Mockito.when(webContext.getHttpServletRequest()).thenReturn(httpServletRequest);

		final WebContextFactory.WebContextBuilder webContextBuilderMock =
				mock(WebContextFactory.WebContextBuilder.class);
		Mockito.when(webContextBuilderMock.get()).thenReturn(webContext);

		final com.dotcms.repackage.org.directwebremoting.Container containerMock =
				mock(com.dotcms.repackage.org.directwebremoting.Container.class);
		Mockito.when(containerMock.getBean(WebContextFactory.WebContextBuilder.class)).thenReturn(webContextBuilderMock);

		WebContextFactory.attach(containerMock);
	}


	/**
	 * <b>Method to Test:</b> {@link ContentletAjax#getContentletData(String)}<p>
	 * <b>When:</b> getting the data of the contentlet <p>
	 * <b>Should:</b> Return if the field contains an image field
	 */
	@Test
	public void test_getContentletData_addingHasImageField(){
		setUpContext();

		final ContentType contentType = new ContentTypeDataGen().nextPersisted();
		ContentTypeDataGen.addField(new FieldDataGen()
				.velocityVarName("title")
				.contentTypeId(contentType.id())
				.type(TextField.class)
				.nextPersisted());
		ContentTypeDataGen.addField(new FieldDataGen()
				.velocityVarName("constImage")
				.contentTypeId(contentType.id())
				.type(ImageField.class)
				.nextPersisted());
		final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();
		final ContentletAjax contentletAjax = new ContentletAjax();
		final Map<String, Object> contentletData = contentletAjax.getContentletData(contentlet.getInode());
		assertNotNull(contentletData);
		assertTrue(contentletData.get("hasImageFields").equals("true"));
	}

	/**
	 * <b>Method to Test:</b> {@link ContentletAjax#searchContentletsByUser(List, String, List, List, boolean, boolean, boolean, boolean, int, String, int, User, HttpSession, String, String, String)} <p>
	 * <b>When:</b> you create multilingual content and select All Languages, should show all content in all diff languages <p>
	 * <b>Should:</b> Content should be shown in every possible language
	 */
	@Test
	public void test_searchContentletsByUser_shouldReturnContentWithMultiLang() throws DotDataException, DotSecurityException {

		// create new language
		language = new LanguageDataGen().nextPersisted();
		Host host = APILocator.getHostAPI().findDefaultHost(systemUser, false);

		//create new content type
		Structure structure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("webPageContent");
		
		// create multilingual contentlets
		contentlet = new Contentlet();
		contentlet.setContentTypeId(structure.getInode());
		contentlet.setHost(host.getIdentifier());
		contentlet.setLanguageId(defaultLang.getId());
		String title = "testIssue"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		contentlet.setStringProperty("title", title);
		contentlet.setStringProperty("body", "testIssueEnglish");
		contentlet.setHost(host.getIdentifier());
		contentlet.setIndexPolicy(IndexPolicy.FORCE);
		contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);

		contentlet = contentletAPI.checkin(contentlet, systemUser,false);
		contentlet.setIndexPolicy(IndexPolicy.FORCE);
		contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
		APILocator.getVersionableAPI().setLive(contentlet);
		contentletAPI.isInodeIndexed(contentlet.getInode(),true);

		String ident = contentlet.getIdentifier();
		contentlet = contentletAPI.findContentletByIdentifier(ident, true, defaultLang.getId(), systemUser, false);
		contentlet = contentletAPI.checkout(contentlet.getInode(), systemUser, false);
		contentlet.setLanguageId(language.getId());
		contentlet.setStringProperty("body", "testIssueItalian");
		contentlet.setIndexPolicy(IndexPolicy.FORCE);
		contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);

		contentlet = contentletAPI.checkin(contentlet, systemUser,false);
		contentlet.setIndexPolicy(IndexPolicy.FORCE);
		contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
		APILocator.getVersionableAPI().setLive(contentlet);
		contentletAPI.isInodeIndexed(contentlet.getInode(),true);
		//  Validate that there are two contentlets associated to the same identifier wit different languages
		List<Contentlet> contList = contentletAPI.getSiblings(ident);
		Assert.assertEquals(2, contList.size());

		// Gets all versions of the contentlet
		List<String> fieldsValues = new ArrayList<>();
		fieldsValues.add("conHost");
		fieldsValues.add(host.getIdentifier());
		fieldsValues.add("webPageContent.title");
		fieldsValues.add(title);
		List<String> categories = new ArrayList<>();

		// The result should contain the two contentlets
		List<Object> results=new ContentletAjax().searchContentletsByUser(structure.getInode(), fieldsValues, categories, false, false, false, false,1, "modDate Desc", 10,systemUser, null, null, null);
		Map<String,Object> result = (Map<String,Object>)results.get(0);
		Assert.assertEquals((Long)result.get("total"), Long.valueOf(2));
		// Validate the two different languages
		final long contentletOne_Language = Long.parseLong(((Map<String,String>)results.get(3)).get("languageId"));
		final long contentletTwo_Language = Long.parseLong(((Map<String,String>)results.get(4)).get("languageId"));
		assertTrue(contentletTwo_Language!=contentletOne_Language);

	}

}
