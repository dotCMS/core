package com.dotmarketing.portlets.contentlet.ajax;

import static com.dotcms.integrationtestutil.content.ContentUtils.createTestKeyValueContent;
import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
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
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

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
		List<String> fieldsValues = new ArrayList<String>();
		fieldsValues.add("conHost");
		fieldsValues.add(host.getIdentifier());
		fieldsValues.add("webPageContent.title");
		fieldsValues.add(title);
		fieldsValues.add("languageId");
		fieldsValues.add(String.valueOf(defaultLang.getId()));
		List<String> categories = new ArrayList<String>();
		
		List<Object> results=new ContentletAjax().searchContentletsByUser(structure.getInode(), fieldsValues, categories, false, false, false, false,1, "modDate Desc", 10,systemUser, null, null, null);
		Map<String,Object> result = (Map<String,Object>)results.get(0);
		Assert.assertEquals((Long)result.get("total"), new Long(1));
		result = (Map<String,Object>)results.get(3);
		Assert.assertTrue(Long.parseLong(String.valueOf(result.get("languageId")))==defaultLang.getId());
		contentlet = contentletAPI.find(String.valueOf(result.get("inode")),systemUser,false);
		contentletAPI.archive(contentlet,systemUser,false);
		contentletAPI.delete(contentlet,systemUser,false);

		/*
		 * Get italian version
		 */
		fieldsValues = new ArrayList<String>();
		fieldsValues.add("conHost");
		fieldsValues.add(host.getIdentifier());
		fieldsValues.add("webPageContent.title");
		fieldsValues.add(title);
		fieldsValues.add("languageId");
		fieldsValues.add(String.valueOf(language.getId()));

		results=new ContentletAjax().searchContentletsByUser(structure.getInode(), fieldsValues, categories, false, false, false, false,1, "modDate Desc", 10,systemUser, null, null, null);
		result = (Map<String,Object>)results.get(0);
		Assert.assertEquals(new Long(1L), (Long)result.get("total"));
		result = (Map<String,Object>)results.get(3);
		Assert.assertTrue(Long.parseLong(String.valueOf(result.get("languageId")))==language.getId());
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
					.find(LanguageVariableAPI.LANGUAGEVARIABLE);
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
                        CollectionsUtils
                                .map(relationship, CollectionsUtils.list(childContentlet)),
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

}
