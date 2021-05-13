package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResult;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ThreadUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Jonathan Gamba
 *         Date: 4/18/13
 */
public class ContentletIndexAPIImplTest extends IntegrationTestBase {

    private static String stemmerText;
    private static User user;
    private static Host defaultHost;

    private static Language defaultLanguage;

    private static ContentletAPI contentletAPI;
    private static ContentletIndexAPI indexAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static ESMappingAPIImpl esMappingAPI;
    private static FieldAPI fieldAPI;
    private static HostAPI hostAPI;
    private static LanguageAPI languageAPI;
    private static RelationshipAPI relationshipAPI;


    @BeforeClass
    public static void prepare () throws Exception {
    	//Setting web app environment
        IntegrationTestInitService.getInstance().init();

        hostAPI = APILocator.getHostAPI();
        languageAPI = APILocator.getLanguageAPI();
        fieldAPI  = APILocator.getContentTypeFieldAPI();

        //Setting the test user
        user = APILocator.getUserAPI().getSystemUser();
        defaultHost = hostAPI.findDefaultHost( user, false );
      
        //Getting the default language
        defaultLanguage = languageAPI.getDefaultLanguage();

        contentletAPI = APILocator.getContentletAPI();
        esMappingAPI = new ESMappingAPIImpl();
        indexAPI = APILocator.getContentletIndexAPI();
        relationshipAPI = APILocator.getRelationshipAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user);

        /*
        ORIGINAL TEXT
        A stemmer for English, for example, should identify the string 'cats' (and possibly 'catlike', 'catty' etc.) as based on the root 'cat',
        and 'stemmer', 'stemming', 'stemmed' as based on 'stem'.
        A stemming algorithm reduces the words 'fishing', 'fished', 'fish', and 'fisher' to the
        root word, 'fish'.
        On the other hand, 'argue', 'argued', 'argues', 'arguing', and 'argus' reduce to the stem 'argu' (illustrating the case where the stem is
        not itself a word or root) but 'argument' and 'arguments' reduce to the stem 'argument'.
         */
        //REMOVED ROOTS WORDS FOR TESTING
        stemmerText = "A stemmer for English, for example, should identify cat " +
                "and stemmer, stemming, stemmed. A stemming algorithm " +
                "reduces the words fishing, fished, and fisher to the " +
                "root word... On the other hand, argue, argued, argues, arguing, " +
                "and argus reduce to the stem ... (illustrating the case where the stem is " +
                "not itself a word or root) but arguments reduce to the stem .....";
    }

    @Test
    public void test_indexContentList_ContentTypeWithPageDetailNotExists() throws Exception{
        long time = System.currentTimeMillis();
        ContentType type = null;
        try {
            type = ContentTypeBuilder
                    .builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.getType()))
                    .description("description" + time).folder(FolderAPI.SYSTEM_FOLDER)
                    .host(Host.SYSTEM_HOST)
                    .name("ContentTypeTestingWithPageDetail" + time).owner("owner")
                    .variable("velocityVarNameTesting" + time).detailPage("pageNotExists")
                    .build();

            type = contentTypeAPI.save(type);

            final Field titleField =
                    FieldBuilder.builder(TextField.class).name("testTitle").variable("testTitle")
                            .unique(true)
                            .contentTypeId(type.id()).dataType(
                            DataTypes.TEXT).build();
            fieldAPI.save(titleField, user);

            final Contentlet contentlet = new ContentletDataGen(type.id())
                    .setProperty("testTitle", "TestContent").nextPersisted();

            final List<Contentlet> contentlets = new ArrayList<>();
            contentlets.add(contentlet);

            indexAPI.addContentToIndex(contentlets);

            assertTrue(contentletAPI
                    .indexCount("+identifier:" + contentlet.getIdentifier(), user, false) > 0);
        }finally {
            if(type!=null){
                contentTypeAPI.delete(type);
            }
        }

    }

    private void generateTestContentlets(final int count) throws Exception {
        final long languageId = languageAPI.getDefaultLanguage().getId();

        final ContentType webPageContentContentType = contentTypeAPI.find("webPageContent");
        final Host host = hostAPI.findDefaultHost(user, false);

        for (int i = 0; i <= count; i++) {
            new ContentletDataGen(
                    webPageContentContentType.id())
                    .languageId(languageId)
                    .host(host)
                    .setProperty("title", "genericContent")
                    .setProperty("author", "systemUser")
                    .setProperty("body", "Generic Content Body").nextPersisted();
        }
    }

    @Test
    public void test_indexContentList_with_diff_refresh_strategies () throws Exception {

        generateTestContentlets(50);

        final List<Contentlet>   contentlets = contentletAPI.findAllContent(0, 100)
                .stream().filter(Objects::nonNull).collect(Collectors.toList());

        assertNotNull(contentlets);
        assertTrue("The number of contentlet returned is: " + contentlets.size(),contentlets.size() >= 50);

        final List<Contentlet>   contentletsDefaultRefresh    = contentlets.subList(0, 15);
        final List<Contentlet>   contentletsImmediateRefresh  = contentlets.subList(15, 30);
        final List<Contentlet>   contentletsWaitForRefresh    = contentlets.subList(30, 50);

        assertNotNull(contentletsDefaultRefresh);
        assertTrue(contentletsDefaultRefresh.size() > 0);

        assertNotNull(contentletsImmediateRefresh);
        assertTrue(contentletsImmediateRefresh.size() > 0);

        assertNotNull(contentletsWaitForRefresh);
        assertTrue(contentletsWaitForRefresh.size() > 0);

        contentletsImmediateRefresh.stream().forEach(contentlet -> contentlet.setIndexPolicy(IndexPolicy.FORCE));
        contentletsWaitForRefresh.stream().forEach(contentlet -> contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR));


        indexAPI.addContentToIndex(contentlets);

        for (final Contentlet contentlet : contentletsDefaultRefresh) {

            if (null != contentlet.getInode()) {
                final boolean exists = contentletAPI.indexCount("+identifier:" + contentlet.getIdentifier(), user, false) > 0;
                System.out.println(contentlet.getIdentifier() + " with default strategy was indexed: " + exists);
            }
        }

        for (final Contentlet contentlet : contentletsImmediateRefresh) {

            if (null != contentlet.getInode()) {
                final boolean exists = contentletAPI.indexCount("+identifier:" + contentlet.getIdentifier(), user, false) > 0;
                System.out.println(contentlet.getIdentifier() + " with immediate strategy was indexed: " + exists);
                assertTrue(contentlet.getIdentifier() + " with immediate strategy was indexed: " + exists, exists);
            }
        }

        for (final Contentlet contentlet : contentletsWaitForRefresh) {

            if (null != contentlet.getInode()) {
                final boolean exists = contentletAPI.indexCount("+identifier:" + contentlet.getIdentifier(), user, false) > 0;
                System.out.println(contentlet.getIdentifier() + " with wait for strategy was indexed: " + exists);
                assertTrue(contentlet.getIdentifier() + " with wait for strategy was indexed: " + exists, exists);
            }
        }
    }


    /**
     * Testing the {@link ContentletIndexAPI#createContentIndex(String)}, {@link ContentletIndexAPI#delete(String)} and
     * {@link ContentletIndexAPI#listDotCMSIndices()} methods
     *
     * @throws Exception
     * @see ContentletIndexAPI
     * @see ContentletIndexAPIImpl
     */
    @Test
    public void createContentIndexAndDelete () throws Exception {

        //Build the index names
        String timeStamp = String.valueOf( new Date().getTime() );
        String workingIndex = IndexType.WORKING.getPrefix() + "_" + timeStamp;
        String liveIndex = IndexType.LIVE.getPrefix() + "_" + timeStamp;

        //Get all the indices
        List<String> indices = indexAPI.listDotCMSIndices();
        //Validate
        assertNotNull( indices );
        assertTrue( !indices.isEmpty() );
        int oldIndices = indices.size();

        //Creates the working index
        Boolean result = indexAPI.createContentIndex( workingIndex );
        //Validate
        assertTrue( result );

        //Creates the live index
        result = indexAPI.createContentIndex( liveIndex );
        //Validate
        assertTrue( result );

        //***************************************************
        //Get all the indices
        indices = indexAPI.listDotCMSIndices();
        //Validate
        assertNotNull( indices );
        assertTrue( !indices.isEmpty() );
        int newIndices = indices.size();

        //Search for the just saved indices
        Boolean foundWorking = false;
        Boolean foundLive = false;
        for ( String index : indices ) {
            if ( index.equals( liveIndex ) ) {
                foundLive = true;
            } else if ( index.equals( workingIndex ) ) {
                foundWorking = true;
            }
        }
        //Validate
        assertTrue( foundWorking );
        assertTrue( foundLive );

        //Verify we just added two more indices
        assertEquals( oldIndices + 2, newIndices );

        //***************************************************
        //Now lets delete the created indices
        Boolean deleted = indexAPI.delete( workingIndex );
        assertTrue( deleted );
        deleted = indexAPI.delete( liveIndex );
        assertTrue( deleted );

        //***************************************************
        //Get all the indices again....
        indices = indexAPI.listDotCMSIndices();
        //Validate
        assertNotNull( indices );
        assertTrue( !indices.isEmpty() );
        newIndices = indices.size();

        //Verify if we still find the deleted indices
        foundWorking = false;
        foundLive = false;
        for ( String index : indices ) {
            if ( index.equals( liveIndex ) ) {
                foundLive = true;
            } else if ( index.equals( workingIndex ) ) {
                foundWorking = true;
            }
        }
        //Validate
        assertFalse( foundWorking );
        assertFalse( foundLive );

        //Verify we just added two more indices
        assertEquals( oldIndices, newIndices );
    }

    /**
     * Testing the {@link ContentletIndexAPI#activateIndex(String)}, {@link ContentletIndexAPI#deactivateIndex(String)}
     * and {@link ContentletIndexAPI#getCurrentIndex()} methods
     *
     * @throws Exception
     * @see ContentletIndexAPI
     * @see ContentletIndexAPIImpl
     */
    @Test
    public void activateDeactivateIndex () throws Exception {

        //Build the index names
        String timeStamp = String.valueOf( new Date().getTime() );
        String workingIndex = IndexType.WORKING.getPrefix() + "_" + timeStamp;
        String liveIndex = IndexType.LIVE.getPrefix() + "_" + timeStamp;

        String oldActiveLive = indexAPI.getActiveIndexName(IndexType.LIVE.getPrefix());
        String oldActiveWorking = indexAPI.getActiveIndexName(IndexType.WORKING.getPrefix());

        //Creates the working index
        Boolean result = indexAPI.createContentIndex( workingIndex );
        assertTrue( result );
        //Activate this working index
        indexAPI.activateIndex( workingIndex );

        //Creates the live index
        result = indexAPI.createContentIndex( liveIndex );
        assertTrue( result );
        //Activate this live index
        indexAPI.activateIndex( liveIndex );

        //***************************************************
        //Get the current indices
        String liveActiveIndex = indexAPI.getActiveIndexName( IndexType.LIVE.getPrefix() );

        //Validate
        assertNotNull( liveActiveIndex );
        assertEquals( liveActiveIndex, liveIndex );

        //***************************************************
        //Now lets deactivate the indices
        //Deactivate this working index
        indexAPI.deactivateIndex( workingIndex );
        //Deactivate this live index
        indexAPI.deactivateIndex( liveIndex );

        // restore old active index
        indexAPI.activateIndex(oldActiveWorking);
        indexAPI.activateIndex(oldActiveLive);
    }

    /**
     * Testing {@link ContentletIndexAPI#isDotCMSIndexName(String)}
     *
     * @throws Exception
     * @see ContentletIndexAPI
     * @see ContentletIndexAPIImpl
     */
    @Test
    public void isDotCMSIndexName () throws Exception {

        //Build the index names
        String timeStamp = String.valueOf( new Date().getTime() );
        String workingIndex = IndexType.WORKING.getPrefix() + "_" + timeStamp;

        //Verify with a proper name
        boolean isIndexName = indexAPI.isDotCMSIndexName( workingIndex );
        assertTrue( isIndexName );

        //Verify a non proper name
        workingIndex = "TEST" + "_" + timeStamp;
        isIndexName = indexAPI.isDotCMSIndexName( workingIndex );
        assertFalse( isIndexName );
    }

    /**
     * Testing {@link ContentletIndexAPI#optimize(java.util.List)}
     *
     * @throws Exception
     * @see ContentletIndexAPI
     * @see ContentletIndexAPIImpl
     */
    @Test
    public void optimize () throws Exception {

        //Build the index names
        String timeStamp = String.valueOf( new Date().getTime() );
        String workingIndex = IndexType.WORKING.getPrefix() + "_" + timeStamp;
        String liveIndex = IndexType.LIVE.getPrefix() + "_" + timeStamp;

        //Creates the working index
        Boolean result = indexAPI.createContentIndex( workingIndex );
        //Validate
        assertTrue( result );

        //Creates the live index
        result = indexAPI.createContentIndex( liveIndex );
        //Validate
        assertTrue( result );

        //Test the optimize method
        List<String> indices = new ArrayList<String>();
        indices.add( workingIndex );
        indices.add( liveIndex );
        Boolean optimized = indexAPI.optimize( indices );
        //Validate
        assertTrue( optimized );
    }

    /**
     * Testing the {@link ContentletIndexAPI#addContentToIndex(com.dotmarketing.portlets.contentlet.model.Contentlet)},
     * {@link ContentletIndexAPI#removeContentFromIndex(com.dotmarketing.portlets.contentlet.model.Contentlet)} methods
     *
     * @throws Exception
     * @see ContentletAPI
     * @see ContentletIndexAPI
     * @see ContentletIndexAPIImpl
     */
    @Test
    public void addRemoveContentToIndex () throws Exception {

        //Creating a test structure
        Structure testStructure = loadTestStructure();
        //Creating a test contentlet
        Contentlet testContentlet = loadTestContentlet( testStructure );

        try {

            //And add it to the index
            testContentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
            indexAPI.addContentToIndex( testContentlet );

            //We are just making time in order to let it apply the index
            contentletAPI.isInodeIndexed( testContentlet.getInode(), true );

            //Verify if it was added to the index
            String query = "+structureName:" + testStructure.getVelocityVarName() + " +deleted:false +live:true";
            List<Contentlet> result = contentletAPI.search( query, 0, -1, "modDate desc", user, true );

            //Validations
            assertNotNull( result );
            assertTrue( !result.isEmpty() );

            //Remove the contentlet from the index
            indexAPI.removeContentFromIndex( testContentlet );

            //We are just making time in order to let it apply the index
            wasContentRemoved( query );

            //Verify if it was removed to the index
            result = contentletAPI.search( query, 0, -1, "modDate desc", user, true );

            //Validations
            assertTrue( result == null || result.isEmpty() );
        } finally {
            try {
                contentletAPI.destroy(testContentlet, user, false );
            } catch (Exception e) {
                e.printStackTrace();
            }
            contentTypeAPI.delete(new StructureTransformer(testStructure).from());
        }
    }

    /**
     * Testing {@link ContentletIndexAPI#removeContentFromIndexByStructureInode(String)}
     *
     * @throws Exception
     * @see ContentletAPI
     * @see ContentletIndexAPI
     * @see ContentletIndexAPIImpl
     */
    @Test
    public void removeContentFromIndexByStructureInode () throws Exception {

        //Creating a test structure
        Structure testStructure = loadTestStructure();
        //Creating a test contentlet
        Contentlet testContentlet = loadTestContentlet( testStructure );

        try {
            //And add it to the index
            testContentlet.setIndexPolicy(IndexPolicy.FORCE);
            indexAPI.addContentToIndex( testContentlet );

            //Verify if it was added to the index
            String query = "+structureName:" + testStructure.getVelocityVarName() + " +deleted:false +live:true";
            List<Contentlet> result = contentletAPI.search( query, 0, -1, "modDate desc", user, true );

            //Validations
            assertNotNull( result );
            assertFalse( result.isEmpty() );

            //Remove the contentlet from the index
            indexAPI.removeContentFromIndexByStructureInode( testStructure.getInode() );

            int x=0;
            do {
                Thread.sleep(5000);
                //Verify if it was removed to the index
                result = contentletAPI.search( query, 0, -1, "modDate desc", user, true );
                x++;
            } while(!result.isEmpty() && x<100);

            //Validations after removing content from index, the result must be empty
            assertNotNull(result);
            assertTrue(result.isEmpty());

        } finally {
            try {
                contentletAPI.destroy(testContentlet, user, false );
            } catch (Exception e) {
                e.printStackTrace();
            }
            contentTypeAPI.delete(new StructureTransformer(testStructure).from());
        }
    }

    /**
     * Testing the {@link SiteSearchAPI#putToIndex(String, com.dotcms.enterprise.publishing.sitesearch.SiteSearchResult, String)},
     * {@link SiteSearchAPI#search(String, String, int, int)} and {@link SiteSearchAPI#deleteFromIndex(String, String)} methods
     *
     * @throws Exception
     * @see ContentletAPI
     * @see ContentletIndexAPI
     * @see ContentletIndexAPIImpl
     */
    @Test
    public void testSearch () throws Exception {

        SiteSearchAPI siteSearchAPI = APILocator.getSiteSearchAPI();

        //*****************************************************************
        //Verify if we already have and site search index, if not lets create one...
        IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();
        String currentSiteSearchIndex = indiciesInfo.getSiteSearch();
        String indexName = currentSiteSearchIndex;
        if ( currentSiteSearchIndex == null ) {
            indexName = SiteSearchAPI.ES_SITE_SEARCH_NAME + "_" + ContentletIndexAPIImpl.timestampFormatter.format( new Date() );
            APILocator.getSiteSearchAPI().createSiteSearchIndex( indexName, null, 1 );
            APILocator.getSiteSearchAPI().activateIndex( indexName );
        }

        //*****************************************************************
        //Creating a test structure
        Structure testStructure = loadTestStructure();
        //Creating a test contentlet
        Contentlet testContentlet = loadTestContentlet( testStructure );
        //Creating a test html page
        HTMLPageAsset testHtmlPage = loadHtmlPage( testContentlet );

        //*****************************************************************
        //Build a site search result in order to add it to the index
        Optional<ContentletVersionInfo> versionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(testHtmlPage.getIdentifier(), testHtmlPage.getLanguageId());

        assertTrue(versionInfo.isPresent());

        String docId = testHtmlPage.getIdentifier() + "_" + defaultLanguage.getId();

        SiteSearchResult res = new SiteSearchResult( testHtmlPage.getMap() );
        res.setLanguage( defaultLanguage.getId() );
        res.setFileName( testHtmlPage.getFriendlyName() );
        res.setModified( versionInfo.get().getVersionTs() );
        res.setHost( defaultHost.getIdentifier() );
        res.setMimeType( "text/html" );
        res.setContentLength( 1 );//Just sending something different than 0
        res.setContent( stemmerText );
        res.setUri( testHtmlPage.getURI() );
        res.setUrl( testHtmlPage.getPageUrl() );
        res.setId( docId );

        //Adding it to the index
        siteSearchAPI.putToIndex( indexName, res, "HTMLPage");
        isDocIndexed( docId );

        try {

            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //+++++++++++++++++++++++++STEMMERS+++++++++++++++++++++++++
            /*
            NOTE: THE CONTENT TEXT DOES NOT CONTAIN THE ROOT WORDS, THIS IS JUST THE REFERENCE TEXT SHOWING HOW SHOULD WORKS!!

            A stemmer for English, for example, should identify the string 'cats' (and possibly 'catlike', 'catty' etc.) as based on the root 'cat',
            and 'stemmer', 'stemming', 'stemmed' as based on 'stem'.
            A stemming algorithm reduces the words 'fishing', 'fished', 'fish', and 'fisher' to the
            root word, 'fish'.
            On the other hand, 'argue', 'argued', 'argues', 'arguing', and 'argus' reduce to the stem 'argu' (illustrating the case where the stem is
            not itself a word or root) but 'argument' and 'arguments' reduce to the stem 'argument'.

            NOTE: THE CONTENT TEXT DOES NOT CONTAIN THE ROOT WORDS, THIS IS JUST THE REFERENCE TEXT SHOWING HOW SHOULD WORKS!!
             */

            //Testing the stemer
            SiteSearchResults siteSearchResults = siteSearchAPI.search( indexName, "argu", 0, 100 );
            //Validations
            assertTrue( siteSearchResults.getError(), siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertTrue( siteSearchResults.getTotalResults() > 0 );
            String highLights = siteSearchResults.getResults().get( 0 ).getHighLights()[0];
            assertTrue( highLights.contains( "<em>argue</em>" ) );
            assertTrue( highLights.contains( "<em>argued</em>" ) );
            assertTrue( highLights.contains( "<em>argues</em>" ) );
            assertTrue( highLights.contains( "<em>arguing</em>" ) );
            assertTrue( highLights.contains( "<em>argus</em>" ) );

            //Testing the stemer
            siteSearchResults = siteSearchAPI.search( indexName, "cats", 0, 100 );
            //Validations
            assertTrue( siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertTrue( siteSearchResults.getTotalResults() > 0 );
            highLights = siteSearchResults.getResults().get( 0 ).getHighLights()[0];
            assertTrue( highLights.contains( "<em>cat</em>" ) );

            //Testing the stemer
            siteSearchResults = siteSearchAPI.search( indexName, "stem", 0, 100 );
            //Validations
            assertTrue( siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertTrue( siteSearchResults.getTotalResults() > 0 );
            highLights = siteSearchResults.getResults().get( 0 ).getHighLights()[0];
            //assertTrue( highLights.contains( "<em>stemmer</em>" ) );//Not found..., verify this....
            assertTrue( highLights.contains( "<em>stemming</em>" ) );
            assertTrue( highLights.contains( "<em>stemmed</em>" ) );

            //Testing the stemer
            siteSearchResults = siteSearchAPI.search( indexName, "argument", 0, 100 );
            //Validations
            assertTrue( siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertTrue( siteSearchResults.getTotalResults() > 0 );
            highLights = siteSearchResults.getResults().get( 0 ).getHighLights()[0];
            assertTrue( highLights.contains( "<em>arguments</em>" ) );

            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //++++++++++++++++++++++++++NGrams++++++++++++++++++++++++++
            //Testing the search with words that are in each extreme of the text
            siteSearchResults = siteSearchAPI.search( indexName, "english illustrating", 0, 100 );
            //Validations
            assertTrue( siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertTrue( siteSearchResults.getTotalResults() > 0 );

            //Testing the search with words that are not in find order
            siteSearchResults = siteSearchAPI.search( indexName, "arguments algorithm", 0, 100 );
            //Validations
            assertTrue( siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertTrue( siteSearchResults.getTotalResults() > 0 );

            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //++++++++++++++++++++++++OTHER TESTS+++++++++++++++++++++++
            //Testing the search by existing word with wildcard
            siteSearchResults = siteSearchAPI.search( indexName, "engli*", 0, 100 );
            //Validations
            assertTrue( siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertTrue( siteSearchResults.getTotalResults() > 0 );

            //Testing the search by existing word with wildcard
            siteSearchResults = siteSearchAPI.search( indexName, "*engli", 0, 100 );
            //Validations
            assertTrue( siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertEquals(1, siteSearchResults.getTotalResults());


            //Testing the search with a non existing word
            siteSearchResults = siteSearchAPI.search( indexName, "weird", 0, 100 );
            //Validations
            assertTrue( siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertEquals(0, siteSearchResults.getTotalResults());
        } finally {
            //And finally remove the index
            contentTypeAPI.delete(new StructureTransformer(testStructure).from());
            siteSearchAPI.deleteFromIndex( indexName, docId );
        }
    }

    @Test
    public void testSearchIndexByDate () throws Exception {

    	Folder testFolder = null;
    	Structure testStructure = null;
    	Contentlet testContent = null;

    	try {
	    	//Creating a test structure
	    	PermissionAPI permissionAPI = APILocator.getPermissionAPI();

	    	//Set up a test folder
	    	testFolder = APILocator.getFolderAPI().createFolders( "testSearchIndexByDateFolder", defaultHost, user, false );
	    	permissionAPI.permissionIndividually( permissionAPI.findParentPermissionable( testFolder ), testFolder, user);

	    	//Set up a test structure
	    	String structureName = "testSearchIndexByDateStructure";
	    	testStructure = new Structure();
	    	testStructure.setHost( defaultHost.getIdentifier() );
	    	testStructure.setFolder( testFolder.getInode() );
	    	testStructure.setName( structureName );
	    	testStructure.setStructureType( Structure.STRUCTURE_TYPE_CONTENT );
	    	testStructure.setOwner( user.getUserId() );
	    	testStructure.setVelocityVarName( structureName );
	    	StructureFactory.saveStructure( testStructure );
	    	CacheLocator.getContentTypeCache().add( testStructure );
	    	//Adding test field

            Field field =
                    FieldBuilder.builder(DateTimeField.class).name("testSearchIndexByDateField").variable("testSearchIndexByDateField")
                            .required(true).indexed(true).listed(true)
                            .contentTypeId(testStructure.id()).dataType(
                            DataTypes.DATE).build();
            field = fieldAPI.save(field,user);

	    	//Creating a test contentlet
	    	testContent = new Contentlet();
	    	testContent.setReviewInterval( "1m" );
	    	testContent.setStructureInode( testStructure.getInode() );
	    	testContent.setHost( defaultHost.getIdentifier() );
	    	testContent.setLanguageId(1);
	    	testContent.setIndexPolicy(IndexPolicy.FORCE);
	    	testContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
	    	testContent.setProperty(field.variable(),"03/05/2014");

	    	testContent = contentletAPI.checkin( testContent, null, permissionAPI.getPermissions( testStructure ), user, false );

            testContent.setIndexPolicy(IndexPolicy.FORCE);
            testContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
	    	APILocator.getVersionableAPI().setLive(testContent);

			//And add it to the index
			indexAPI.addContentToIndex( testContent );

			//We are just making time in order to let it apply the index
			contentletAPI.isInodeIndexed( testContent.getInode(), true );

			//Verify if it was added to the index
			String query = "+structureName:" + testStructure.getVelocityVarName() + " +testSearchIndexByDateStructure.testSearchIndexByDateField:03/05/2014 +deleted:false +live:true";
			List<Contentlet> result = contentletAPI.search( query, 0, -1, "modDate desc", user, true );

			assertTrue(UtilMethods.isSet(result) && !result.isEmpty());

    	} catch(Exception e) {
    		Logger.error("Error executing testSearchIndexByDate", e.getMessage(), e);
    	} finally {
    		if(UtilMethods.isSet(testStructure)) {
    			APILocator.getStructureAPI().delete(testStructure, user);
    		}
    		if(UtilMethods.isSet(testFolder)) {
    			APILocator.getFolderAPI().delete(testFolder, user, false);
    		}
    	}

    }

  @Test
  public void test_that_live_and_working_content_makes_it_into_the_index() throws Exception {
    ContentType type = new ContentTypeDataGen()
        .fields(ImmutableList.of(ImmutableTextField.builder().name("Title").variable("title").searchable(true).listed(true).build()))
        .nextPersisted();
    Contentlet content = new ContentletDataGen(type.id()).setProperty("title", "contentTest " + System.currentTimeMillis()).next();

    String title = "version1";
    content.setStringProperty("title", title);

    content.setIndexPolicy(IndexPolicy.FORCE);

    // check in the content
    content = contentletAPI.checkin(content, user, false);

    assertTrue(content.getIdentifier() != null);
    assertTrue(content.isWorking());
    assertFalse(content.isLive());
    assertEquals(content.getTitle(), title);
    // publish the content
    content.setIndexPolicy(IndexPolicy.FORCE);
    content.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
    contentletAPI.publish(content, user, false);
    String liveInode = content.getInode();
    assertTrue(content.isLive());
    content.setInode(null);
    title = "version2";
    content.setStringProperty("title", title);
    content.setIndexPolicy(IndexPolicy.FORCE);
    content = contentletAPI.checkin(content, user, false);
    assertTrue(content.getIdentifier() != null);
    assertTrue(content.isWorking());
    assertFalse(content.isLive());
    assertTrue(content.hasLiveVersion());
    String workingInode = content.getInode();
    List<ContentletSearch> liveSearch = contentletAPI.searchIndex("+identifier:" + content.getIdentifier() + " +live:true", 1, 0, "modDate", user, false);
    List<ContentletSearch> workingSearch = contentletAPI.searchIndex("+identifier:" + content.getIdentifier() + " +live:false", 1, 0, "modDate", user, false);
    assert(liveSearch.size()>0);
    assert(workingSearch.size()>0);
    assert(liveSearch.get(0).getInode().equals(liveInode));
    assert(workingSearch.get(0).getInode().equals(workingInode));
    assert(!workingSearch.get(0).getInode().equals(liveInode));
    
    indexAPI.removeContentFromIndex(content);
    ReindexThread.startThread();
    APILocator.getReindexQueueAPI().addContentletReindex(content);
    ThreadUtils.sleep(10000);
    liveSearch = contentletAPI.searchIndex("+identifier:" + content.getIdentifier() + " +live:true", 1, 0, "modDate", user, false);
    workingSearch = contentletAPI.searchIndex("+identifier:" + content.getIdentifier() + " +live:false", 1, 0, "modDate", user, false);
    assert(liveSearch.size()>0);
    assert(workingSearch.size()>0);
    assert(liveSearch.get(0).getInode().equals(liveInode));
    assert(workingSearch.get(0).getInode().equals(workingInode));
    assert(!workingSearch.get(0).getInode().equals(liveInode));
    
  }
    
    /**
     * Creates and returns a test html page
     *
     * @param contentlet
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private HTMLPageAsset loadHtmlPage ( Contentlet contentlet ) throws DotSecurityException, DotDataException {

        Structure structure = contentlet.getStructure();

        //Create a container for the given contentlet
        Container container = new Container();
        container.setCode( "this is the code" );
        container.setFriendlyName( "test container" );
        container.setTitle( "his is the title" );
        container.setMaxContentlets( 5 );
        container.setPreLoop( "preloop code" );
        container.setPostLoop( "postloop code" );
        //Save it

        List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(structure.getInode());
        cs.setCode("this is the code");
        csList.add(cs);

        container = APILocator.getContainerAPI().save( container, csList, defaultHost, user, false );

        //Create a template
        String body = "<html><body> #parseContainer('" + container.getIdentifier() + "') </body></html>";
        String title = "empty test template " + UUIDGenerator.generateUuid();

        Template template = new Template();
        template.setTitle( title );
        template.setBody( body );

        template = APILocator.getTemplateAPI().saveTemplate( template, defaultHost, user, false );

        //Create the html page
        Folder homeFolder = APILocator.getFolderAPI().createFolders( "/home/", defaultHost, user, false );
        HTMLPageAsset htmlPage = new HTMLPageDataGen(homeFolder, template).languageId(1)
				.nextPersisted();

        MultiTree multiTree = new MultiTree();
        multiTree.setParent1( htmlPage.getIdentifier() );
        multiTree.setParent2( container.getIdentifier() );
        multiTree.setChild( contentlet.getIdentifier() );
        multiTree.setTreeOrder( 1 );
        APILocator.getMultiTreeAPI().saveMultiTree( multiTree );

        return htmlPage;
    }

    /**
     * Creates and returns a test Structure
     *
     * @return
     * @throws Exception
     */
    private Structure loadTestStructure () throws Exception {

        PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        //Set up a test folder
        Folder testFolder = APILocator.getFolderAPI().createFolders( "/" + new Date().getTime() + "/", defaultHost, user, false );
        permissionAPI.permissionIndividually( permissionAPI.findParentPermissionable( testFolder ), testFolder, user);

        //Set up a test structure
        String structureName = "ESContentletIndexAPITest_" + new Date().getTime();
        Structure testStructure = new Structure();
        testStructure.setHost( defaultHost.getIdentifier() );
        testStructure.setFolder( testFolder.getInode() );
        testStructure.setName( structureName );
        testStructure.setStructureType( Structure.STRUCTURE_TYPE_CONTENT );
        testStructure.setOwner( user.getUserId() );
        testStructure.setVelocityVarName( structureName );
        StructureFactory.saveStructure( testStructure );
        CacheLocator.getContentTypeCache().add( testStructure );
        //Adding test field
        final Field field =
                FieldBuilder.builder(WysiwygField.class).name("Wysiwyg").variable("wysiwyg").required(true).indexed(true).listed(true)
                        .contentTypeId(testStructure.id()).dataType(
                        DataTypes.LONG_TEXT).build();
        fieldAPI.save(field,user);

        return testStructure;
    }

    /**
     * Creates and returns a test Contentlet
     *
     * @return
     * @throws Exception
     */
    private Contentlet loadTestContentlet ( Structure testStructure ) throws Exception {

        //Set up a test contentlet
        Contentlet testContentlet = new Contentlet();
        testContentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
        testContentlet.setStructureInode( testStructure.getInode() );
        testContentlet.setHost( defaultHost.getIdentifier() );
        testContentlet.setLanguageId( defaultLanguage.getId() );
        testContentlet.setStringProperty( "wysiwyg", stemmerText );
        testContentlet = contentletAPI.checkin( testContentlet, user, false );

        //Make it live
        APILocator.getVersionableAPI().setLive( testContentlet );

        return testContentlet;
    }

    private boolean isDocIndexed ( String id ) {

        if ( !UtilMethods.isSet( id ) ) {
            Logger.warn( this, "Requested Inode is not indexed because Inode is not set" );
        }
        SearchHits lc;
        boolean found = false;
        int counter = 0;
        while ( counter < 300 ) {
            try {
                lc = indexSearch( "+id:" + id, "modDate" );
            } catch ( Exception e ) {
                Logger.error( this.getClass(), e.getMessage(), e );
                return false;
            }
            if ( lc.getTotalHits().value > 0 ) {
                return true;
            }
            try {
                Thread.sleep( 100 );
            } catch ( Exception e ) {
                Logger.debug( this, "Cannot sleep : ", e );
            }
            counter++;
        }
        return found;
    }

    private SearchHits indexSearch ( String query, String sortBy ) throws Exception {

        String qq = ESContentFactoryImpl.translateQuery( query, sortBy ).getQuery();

        // we check the query to figure out wich indexes to hit
        String indexToHit;
        IndiciesInfo info;
        try {
            info = APILocator.getIndiciesAPI().loadIndicies();
        } catch ( DotDataException ee ) {
            Logger.fatal( this, "Can't get indicies information", ee );
            return null;
        }
        indexToHit = info.getSiteSearch();
        String indexName = indexToHit;
        if ( indexName == null ) {
            indexName = SiteSearchAPI.ES_SITE_SEARCH_NAME + "_" + ContentletIndexAPIImpl.timestampFormatter.format( new Date() );
            APILocator.getSiteSearchAPI().createSiteSearchIndex( indexName, null, 1 );
            APILocator.getSiteSearchAPI().activateIndex( indexName );
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.queryStringQuery( qq ));
        searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        searchSourceBuilder.fetchSource(new String[] {"inode"}, null);
        searchSourceBuilder.storedField("id");
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(indexName);
        searchRequest.source(searchSourceBuilder);

        final SearchResponse response = Sneaky.sneak(()->
                RestHighLevelClientProvider
                        .getInstance().getClient().search(searchRequest, RequestOptions.DEFAULT));
        return response.getHits();
    }

    public boolean wasContentRemoved ( String query ) {

        boolean removed = false;
        int counter = 0;
        while ( counter < 300 ) {
            try {
                //Verify if it was removed to the index
                List<Contentlet> result = contentletAPI.search( query, 0, -1, "modDate desc", user, true );

                //Validations
                if ( result == null || result.isEmpty() ) {
                    return true;
                }
            } catch ( Exception e ) {
                Logger.error( this.getClass(), e.getMessage(), e );
                return false;
            }

            try {
                Thread.sleep( 100 );
            } catch ( Exception e ) {
                Logger.debug( this, "Cannot sleep : ", e );
            }
            counter++;
        }

        return removed;
    }

    @Test
    public void testRemoveContentFromIndex() throws DotDataException, DotSecurityException {

        final ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title")
                                .searchable(true).listed(true).build()))
                .nextPersisted();

        final List<Contentlet> contents = new ArrayList<>();

        // create contentlet
        final String conTitle = "contentTest " + System.currentTimeMillis();
        // check in the content
        Contentlet baseCon = new ContentletDataGen(type.id()).setProperty("title", conTitle).next();
        baseCon.setIndexPolicy(IndexPolicy.FORCE);
        baseCon = contentletAPI.checkin(baseCon, user, false);
        contents.add(baseCon);

        List<Language> languages = languageAPI.getLanguages();
        if (languages.size() < 5) {
            // create 3 languages
            for (int i = 0; i < 3; i++) {
                new LanguageDataGen().country("x" + i).languageName("dummyLanguage" + i)
                        .languageCode("en").countryCode("x" + i).nextPersisted();
            }
        }
        languages = new ArrayList<>();
        languages.addAll(languageAPI.getLanguages());

        languages.removeIf(
                l -> l.getId() == languageAPI.getDefaultLanguage().getId());
        languages = languages.subList(0, (languages.size() > 5) ? 5 : languages.size());

        // building contentents in multiple languages
        for (Language lang : languages) {
            final Contentlet newCon = new ContentletDataGen(type.id())
                    .setProperty("title", conTitle).next();
            newCon.setLanguageId(lang.getId());
            newCon.setInode(null);
            newCon.setIdentifier(baseCon.getIdentifier());
            newCon.setIndexPolicy(IndexPolicy.WAIT_FOR);
            contents.add(contentletAPI.checkin(newCon, user, false));
        }

        // content is in the index
        for (Contentlet content : contents) {
            assertTrue(content.getIdentifier() != null);
            assertTrue(content.isWorking());
            assertFalse(content.isLive());
            assertTrue(contentletAPI.indexCount(
                    "+live:false +identifier:" + content.getIdentifier() + " +inode:" + content
                            .getInode() + " +languageId:" + content.getLanguageId(), user,
                    false) > 0);
            assertTrue(contentletAPI.indexCount(
                    "+live:true +identifier:" + content.getIdentifier() + " +inode:" + content
                            .getInode() + " +languageId:" + content.getLanguageId(), user,
                    false) == 0);

        }

        // testing publish
        for (Contentlet content : contents) {
            content.setIndexPolicy(IndexPolicy.WAIT_FOR);
            content.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
            contentletAPI.publish(content, user, false);
            assertTrue("the contentlet: " + content.getIdentifier() + " must be published",content.isLive());
            assertTrue(contentletAPI.indexCount(
                    "+live:true +identifier:" + content.getIdentifier() + " +inode:" + content
                            .getInode() + " +languageId:" + content.getLanguageId(), user,
                    false) > 0);
        }
        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        APILocator.getReindexQueueAPI().addIdentifierDelete(baseCon.getIdentifier());

        Map<String, ReindexEntry> entries = APILocator.getReindexQueueAPI().findContentToReindex();

        final BulkRequest bulk = indexAPI.createBulkRequest();
        bulk.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
        indexAPI.appendBulkRequest(bulk, entries.values());
        indexAPI.putToIndex(bulk);

        assertTrue(contentletAPI
                .indexCount("+live:false +identifier:" + baseCon.getIdentifier(), user, false)
                == 0);

        for (ContentletSearch indexentry : contentletAPI
                .searchIndex("+live:true +identifier:" + baseCon.getIdentifier(), 0, 0, "moddate",
                        user, false)) {
            System.err.println(indexentry);
        }
        assertTrue(contentletAPI
                .indexCount("+live:true +identifier:" + baseCon.getIdentifier(), user, false) == 0);

        assertTrue(contentletAPI.indexCount("+identifier:" + baseCon.getIdentifier(), user, false)
                == 0);

    }

    /**
     * Method to test: {@link ContentletIndexAPI#getIndexDocumentCount(String)}
     * Test Case: Tries to get the document count of an active index
     * Expected Results: Value should be more than 0
     */
    @Test
    public void testGetIndexDocumentCountSuccess() throws  DotDataException {
        assertTrue(indexAPI.getIndexDocumentCount(indexAPI.getCurrentIndex().get(0)) > 0);
    }

    /**
     * Method to test: {@link ContentletIndexAPI#getIndexDocumentCount(String)}
     * Test Case: Tries to get the document count of an index that does not exist
     * Expected Results: Throws ElasticsearchStatusException
     */
    @Test(expected= ElasticsearchStatusException.class)
    public void testGetIndexDocumentCountWithInvalidIndexNameFails(){
        indexAPI.getIndexDocumentCount("invalidIndexName");
    }
}
