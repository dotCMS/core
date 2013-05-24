package com.dotcms.content.elasticsearch.business;

import com.dotcms.TestBase;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResult;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.internal.InternalSearchHits;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Jonathan Gamba
 *         Date: 4/18/13
 */
public class ESContentletIndexAPITest extends TestBase {

    private static String stemmerText;
    private static User user;
    private static Host defaultHost;
    private static String pageExt;
    private static Language defaultLanguage;

    @BeforeClass
    public static void prepare () throws DotSecurityException, DotDataException {

        HostAPI hostAPI = APILocator.getHostAPI();
        LanguageAPI languageAPI = APILocator.getLanguageAPI();

        //Setting the test user
        user = APILocator.getUserAPI().getSystemUser();
        defaultHost = hostAPI.findDefaultHost( user, false );
        pageExt = Config.getStringProperty( "VELOCITY_PAGE_EXTENSION" );
        //Getting the default language
        defaultLanguage = languageAPI.getDefaultLanguage();

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

    /**
     * Testing the {@link ContentletIndexAPI#createContentIndex(String)}, {@link ContentletIndexAPI#delete(String)} and
     * {@link ContentletIndexAPI#listDotCMSIndices()} methods
     *
     * @throws Exception
     * @see ContentletIndexAPI
     * @see ESContentletIndexAPI
     */
    @Test
    public void createContentIndexAndDelete () throws Exception {

        ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();

        //Build the index names
        String timeStamp = String.valueOf( new Date().getTime() );
        String workingIndex = ESContentletIndexAPI.ES_WORKING_INDEX_NAME + "_" + timeStamp;
        String liveIndex = ESContentletIndexAPI.ES_LIVE_INDEX_NAME + "_" + timeStamp;

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
        assertTrue( oldIndices + 2 == newIndices );

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
        assertTrue( oldIndices == newIndices );
    }

    /**
     * Testing the {@link ContentletIndexAPI#activateIndex(String)}, {@link ContentletIndexAPI#deactivateIndex(String)}
     * and {@link ContentletIndexAPI#getCurrentIndex()} methods
     *
     * @throws Exception
     * @see ContentletIndexAPI
     * @see ESContentletIndexAPI
     */
    @Test
    @Ignore
    public void activateDeactivateIndex () throws Exception {

        ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();

        //Build the index names
        String timeStamp = String.valueOf( new Date().getTime() );
        String workingIndex = ESContentletIndexAPI.ES_WORKING_INDEX_NAME + "_" + timeStamp;
        String liveIndex = ESContentletIndexAPI.ES_LIVE_INDEX_NAME + "_" + timeStamp;

        String oldActiveLive = indexAPI.getActiveIndexName(ContentletIndexAPI.ES_LIVE_INDEX_NAME);
        String oldActiveWorking = indexAPI.getActiveIndexName(ContentletIndexAPI.ES_WORKING_INDEX_NAME);

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
        String liveActiveIndex = indexAPI.getActiveIndexName( ContentletIndexAPI.ES_LIVE_INDEX_NAME );

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
     * @see ESContentletIndexAPI
     */
    @Test
    public void isDotCMSIndexName () throws Exception {

        ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();

        //Build the index names
        String timeStamp = String.valueOf( new Date().getTime() );
        String workingIndex = ESContentletIndexAPI.ES_WORKING_INDEX_NAME + "_" + timeStamp;

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
     * @see ESContentletIndexAPI
     */
    @Test
    public void optimize () throws Exception {

        ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();

        //Build the index names
        String timeStamp = String.valueOf( new Date().getTime() );
        String workingIndex = ESContentletIndexAPI.ES_WORKING_INDEX_NAME + "_" + timeStamp;
        String liveIndex = ESContentletIndexAPI.ES_LIVE_INDEX_NAME + "_" + timeStamp;

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
     * Testing {@link ContentletIndexAPI#getRidOfOldIndex()}
     *
     * @throws Exception
     * @see ContentletIndexAPI
     * @see ESContentletIndexAPI
     */
    @Test
    public void getRidOfOldIndex () throws Exception {
        /*
        ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();

        List<String> oldIndices = indexAPI.getCurrentIndex();
        //Validate
        assertNotNull( oldIndices );
        assertTrue( !oldIndices.isEmpty() );

        indexAPI.getRidOfOldIndex();

        List<String> indices = indexAPI.getCurrentIndex();
        //Validate
        assertNotNull( indices );
        assertTrue( !indices.isEmpty() );

        assertNotSame( oldIndices, indices );

        indices = indexAPI.getNewIndex();
        //Validate
        assertNotNull( indices );
        assertTrue( indices.isEmpty() );
        */

        // this blows up everything. do not try at home
    }

    /**
     * Testing the {@link ContentletIndexAPI#addContentToIndex(com.dotmarketing.portlets.contentlet.model.Contentlet)},
     * {@link ContentletIndexAPI#removeContentFromIndex(com.dotmarketing.portlets.contentlet.model.Contentlet)} methods
     *
     * @throws Exception
     * @see ContentletAPI
     * @see ContentletIndexAPI
     * @see ESContentletIndexAPI
     */
    @Test
    public void addRemoveContentToIndex () throws Exception {

        ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
        ContentletAPI contentletAPI = APILocator.getContentletAPI();

        //Creating a test structure
        Structure testStructure = loadTestStructure();
        //Creating a test contentlet
        Contentlet testContentlet = loadTestContentlet( testStructure );

        try {

            //And add it to the index
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
            APILocator.getContentletAPI().delete( testContentlet, user, false );
        }
    }

    /**
     * Testing {@link ContentletIndexAPI#removeContentFromIndexByStructureInode(String)}
     *
     * @throws Exception
     * @see ContentletAPI
     * @see ContentletIndexAPI
     * @see ESContentletIndexAPI
     */
    @Test
    public void removeContentFromIndexByStructureInode () throws Exception {

        ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
        ContentletAPI contentletAPI = APILocator.getContentletAPI();

        //Creating a test structure
        Structure testStructure = loadTestStructure();
        //Creating a test contentlet
        Contentlet testContentlet = loadTestContentlet( testStructure );

        try {
            //And add it to the index
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
            indexAPI.removeContentFromIndexByStructureInode( testStructure.getInode() );

            int x=0;
            do {
                Thread.sleep(200);
                //Verify if it was removed to the index
                result = contentletAPI.search( query, 0, -1, "modDate desc", user, true );
                x++;
            } while((result == null || result.isEmpty()) && x<100);

        } finally {
            APILocator.getContentletAPI().delete( testContentlet, user, false );
        }
    }

    /**
     * Testing the {@link SiteSearchAPI#putToIndex(String, com.dotcms.enterprise.publishing.sitesearch.SiteSearchResult)},
     * {@link SiteSearchAPI#search(String, String, int, int)} and {@link SiteSearchAPI#deleteFromIndex(String, String)} methods
     *
     * @throws Exception
     * @see ContentletAPI
     * @see ContentletIndexAPI
     * @see ESContentletIndexAPI
     */
    @Test
    public void testSearch () throws Exception {

        SiteSearchAPI siteSearchAPI = APILocator.getSiteSearchAPI();

        //*****************************************************************
        //Verify if we already have and site search index, if not lets create one...
        IndiciesAPI.IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();
        String currentSiteSearchIndex = indiciesInfo.site_search;
        String indexName = currentSiteSearchIndex;
        if ( currentSiteSearchIndex == null ) {
            indexName = SiteSearchAPI.ES_SITE_SEARCH_NAME + "_" + ESContentletIndexAPI.timestampFormatter.format( new Date() );
            APILocator.getSiteSearchAPI().createSiteSearchIndex( indexName, null, 1 );
            APILocator.getSiteSearchAPI().activateIndex( indexName );
        }

        //*****************************************************************
        //Creating a test structure
        Structure testStructure = loadTestStructure();
        //Creating a test contentlet
        Contentlet testContentlet = loadTestContentlet( testStructure );
        //Creating a test html page
        HTMLPage testHtmlPage = loadHtmlPage( testContentlet );

        //*****************************************************************
        //Build a site search result in order to add it to the index
        VersionInfo versionInfo = APILocator.getVersionableAPI().getVersionInfo( testHtmlPage.getIdentifier() );
        String docId = testHtmlPage.getIdentifier() + "_" + defaultLanguage.getId();

        SiteSearchResult res = new SiteSearchResult( testHtmlPage.getMap() );
        res.setLanguage( defaultLanguage.getId() );
        res.setFileName( testHtmlPage.getFriendlyName() );
        res.setModified( versionInfo.getVersionTs() );
        res.setHost( defaultHost.getIdentifier() );
        res.setMimeType( "text/html" );
        res.setContentLength( 1 );//Just sending something different than 0
        res.setContent( stemmerText );
        res.setUri( testHtmlPage.getURI() );
        res.setUrl( testHtmlPage.getPageUrl() );
        res.setId( docId );

        //Adding it to the index
        siteSearchAPI.putToIndex( indexName, res );
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
            assertTrue( siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertTrue( siteSearchResults.getTotalResults() > 0 );
            String highLights = siteSearchResults.getResults().get( 0 ).getHighLights()[0];
            assertTrue( highLights.contains( "<em>argue</em>" ) );
            assertTrue( highLights.contains( "<em>argued</em>" ) );
            assertTrue( highLights.contains( "<em>argues</em>" ) );
            assertTrue( highLights.contains( "<em>arguing</em>" ) );
            //assertTrue( highLights.contains( "<em>argus</em>" ) );//Not found..., verify this....

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
            //Testing the search
            siteSearchResults = siteSearchAPI.search( indexName, "engli*", 0, 100 );
            //Validations
            assertTrue( siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertTrue( siteSearchResults.getTotalResults() > 0 );

            //Testing the search
            siteSearchResults = siteSearchAPI.search( indexName, "*engli", 0, 100 );
            //Validations
            assertTrue( siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertEquals( siteSearchResults.getTotalResults(), 0 );

            //Testing the search
            siteSearchResults = siteSearchAPI.search( indexName, "e", 0, 100 );
            //Validations
            assertTrue( siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertEquals( siteSearchResults.getTotalResults(), 0 );

            //Testing the search with a non existing word
            siteSearchResults = siteSearchAPI.search( indexName, "weird", 0, 100 );
            //Validations
            assertTrue( siteSearchResults.getError() == null || siteSearchResults.getError().isEmpty() );
            assertEquals( siteSearchResults.getTotalResults(), 0 );
        } finally {
            //And finally remove the index
            siteSearchAPI.deleteFromIndex( indexName, docId );
        }
    }

    /**
     * Creates and returns a test html page
     *
     * @param contentlet
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private HTMLPage loadHtmlPage ( Contentlet contentlet ) throws DotSecurityException, DotDataException {

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
        String pageUrl = "testpage_" + new Date().getTime() + "." + pageExt;
        Folder homeFolder = APILocator.getFolderAPI().createFolders( "/home/", defaultHost, user, false );
        HTMLPage htmlPage = new HTMLPage();
        htmlPage.setPageUrl( pageUrl );
        htmlPage.setFriendlyName( pageUrl );
        htmlPage.setTitle( pageUrl );
        htmlPage = APILocator.getHTMLPageAPI().saveHTMLPage( htmlPage, template, homeFolder, user, false );
        //Make it live
        APILocator.getVersionableAPI().setLive( htmlPage );

        MultiTree multiTree = new MultiTree();
        multiTree.setParent1( htmlPage.getIdentifier() );
        multiTree.setParent2( container.getIdentifier() );
        multiTree.setChild( contentlet.getIdentifier() );
        multiTree.setTreeOrder( 1 );
        MultiTreeFactory.saveMultiTree( multiTree );

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
        permissionAPI.permissionIndividually( permissionAPI.findParentPermissionable( testFolder ), testFolder, user, false );

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
        StructureCache.addStructure( testStructure );
        //Adding test field
        Field field = new Field( "Wysiwyg", Field.FieldType.WYSIWYG, Field.DataType.LONG_TEXT, testStructure, true, true, true, 1, false, false, false );
        field.setVelocityVarName( "wysiwyg" );
        field.setListed( true );
        FieldFactory.saveField( field );
        FieldsCache.addField( field );

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
        testContentlet.setStructureInode( testStructure.getInode() );
        testContentlet.setHost( defaultHost.getIdentifier() );
        testContentlet.setLanguageId( defaultLanguage.getId() );
        testContentlet.setStringProperty( "wysiwyg", stemmerText );
        testContentlet = APILocator.getContentletAPI().checkin( testContentlet, user, false );

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
            if ( lc.getTotalHits() > 0 ) {
                found = true;
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

    private SearchHits indexSearch ( String query, String sortBy ) {

        String qq = ESContentFactoryImpl.translateQuery( query, sortBy ).getQuery();

        // we check the query to figure out wich indexes to hit
        String indexToHit;
        IndiciesAPI.IndiciesInfo info;
        try {
            info = APILocator.getIndiciesAPI().loadIndicies();
        } catch ( DotDataException ee ) {
            Logger.fatal( this, "Can't get indicies information", ee );
            return null;
        }
        indexToHit = info.site_search;

        Client client = new ESClient().getClient();
        SearchResponse resp;
        try {
            QueryStringQueryBuilder qb = QueryBuilders.queryString( qq );
            SearchRequestBuilder srb = client.prepareSearch();
            srb.setQuery( qb );

            srb.setIndices( indexToHit );
            srb.addFields( "id" );

            try {
                resp = srb.execute().actionGet();
            } catch ( SearchPhaseExecutionException e ) {
                if ( e.getMessage().contains( "dotraw] in order to sort on" ) ) {
                    return new InternalSearchHits( InternalSearchHits.EMPTY, 0, 0 );
                } else {
                    throw e;
                }
            }
        } catch ( Exception e ) {
            Logger.error( ESContentFactoryImpl.class, e.getMessage(), e );
            throw new RuntimeException( e );
        }
        return resp.getHits();
    }

    public boolean wasContentRemoved ( String query ) {

        ContentletAPI contentletAPI = APILocator.getContentletAPI();

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

}