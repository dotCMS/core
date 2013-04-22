package com.dotcms.content.elasticsearch.business;

import com.dotcms.TestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
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

    private static User user;
    private static Host defaultHost;

    @BeforeClass
    public static void prepare () throws DotSecurityException, DotDataException {

        HostAPI hostAPI = APILocator.getHostAPI();

        //Setting the test user
        user = APILocator.getUserAPI().getSystemUser();
        defaultHost = hostAPI.findDefaultHost( user, false );
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
    public void activateDeactivateIndex () throws Exception {

        ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();

        //Build the index names
        String timeStamp = String.valueOf( new Date().getTime() );
        String workingIndex = ESContentletIndexAPI.ES_WORKING_INDEX_NAME + "_" + timeStamp;
        String liveIndex = ESContentletIndexAPI.ES_LIVE_INDEX_NAME + "_" + timeStamp;

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
        List<String> indices = indexAPI.getCurrentIndex();

        //Validate
        assertNotNull( indices );
        assertTrue( !indices.isEmpty() );
        assertEquals( indices.size(), 2 );

        //The returned indices must match with the one we activated
        String index1 = indices.get( 0 );
        String index2 = indices.get( 1 );
        assertTrue( index1.equals( workingIndex ) || index1.equals( liveIndex ) );
        assertTrue( index2.equals( workingIndex ) || index2.equals( liveIndex ) );

        //***************************************************
        //Now lets deactivate the indices
        //Deactivate this working index
        indexAPI.deactivateIndex( workingIndex );
        //Deactivate this live index
        indexAPI.deactivateIndex( liveIndex );

        //***************************************************
        //Get the current indices
        indices = indexAPI.getCurrentIndex();

        //Validate
        assertNotNull( indices );
        assertTrue( !indices.isEmpty() );
        assertEquals( indices.size(), 2 );

        //The returned indices must NOT match with the one we activated
        index1 = indices.get( 0 );
        index2 = indices.get( 1 );
        assertTrue( !index1.equals( workingIndex ) && !index1.equals( liveIndex ) );
        assertTrue( !index2.equals( workingIndex ) && !index2.equals( liveIndex ) );
    }

    /**
     * Testing the {@link ContentletIndexAPI#setUpFullReindex()}, {@link ContentletIndexAPI#isInFullReindex()}
     * and {@link ContentletIndexAPI#getNewIndex()} methods
     *
     * @throws Exception
     * @see ContentletIndexAPI
     * @see ESContentletIndexAPI
     */
    @Test
    @Ignore ("Working on this test")
    public void getNewIndex () throws Exception {

        ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();

        //Full reindex
        String timeStamp = indexAPI.setUpFullReindex();

        //Should say it is running....
        Boolean isInFullReindex = indexAPI.isInFullReindex();
        assertTrue( isInFullReindex );

        //***************************************************

        //Get the indices that are running
        List<String> indices = indexAPI.getNewIndex();

        //Validate
        assertNotNull( indices );
        assertTrue( !indices.isEmpty() );
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
    }

    /**
     * Testing {@link ContentletIndexAPI#getActiveIndexName(String)}
     *
     * @throws Exception
     * @see ContentletIndexAPI
     * @see ESContentletIndexAPI
     */
    @Test
    public void getActiveIndexName () throws Exception {

        ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();

        //Build the index names
        String timeStamp = String.valueOf( new Date().getTime() );
        String workingIndex = ESContentletIndexAPI.ES_WORKING_INDEX_NAME + "_" + timeStamp;

        //Creates the working index
        Boolean result = indexAPI.createContentIndex( workingIndex );
        assertTrue( result );
        //Activate this working index
        indexAPI.activateIndex( workingIndex );

        //Get the active working index
        String index = indexAPI.getActiveIndexName( ESContentletIndexAPI.ES_WORKING_INDEX_NAME );

        //Validate
        assertNotNull( index );
        assertTrue( index.contains( ESContentletIndexAPI.ES_WORKING_INDEX_NAME ) );
        assertEquals( index, workingIndex );
    }

    /**
     * Testing the {@link ContentletIndexAPI#addContentToIndex(com.dotmarketing.portlets.contentlet.model.Contentlet)},
     * {@link ContentletIndexAPI#removeContentFromIndex(com.dotmarketing.portlets.contentlet.model.Contentlet)} methods
     *
     * @throws Exception
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

        //And add it to the index
        Date initDate = new Date();
        indexAPI.addContentToIndex( testContentlet );

        //We are just making time in order to let it apply the index
        wait( initDate, 2 );

        //Verify if it was added to the index
        String query = "+structureName:" + testStructure.getVelocityVarName() + " +deleted:false +live:true";
        List<Contentlet> result = contentletAPI.search( query, 0, -1, "modDate desc", user, true );

        //Validations
        assertNotNull( result );
        assertTrue( !result.isEmpty() );

        //Remove the contentlet from the index
        initDate = new Date();
        indexAPI.removeContentFromIndex( testContentlet );

        //We are just making time in order to let it apply the index
        wait( initDate, 2 );

        result = contentletAPI.search( query, 0, -1, "modDate desc", user, true );

        //Validations
        assertTrue( result == null || result.isEmpty() );
    }

    /**
     * Testing {@link ContentletIndexAPI#removeContentFromIndexByStructureInode(String)}
     *
     * @throws Exception
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

        //And add it to the index
        Date initDate = new Date();
        indexAPI.addContentToIndex( testContentlet );

        //We are just making time in order to let it apply the index
        wait( initDate, 2 );

        //Verify if it was added to the index
        String query = "+structureName:" + testStructure.getVelocityVarName() + " +deleted:false +live:true";
        List<Contentlet> result = contentletAPI.search( query, 0, -1, "modDate desc", user, true );

        //Validations
        assertNotNull( result );
        assertTrue( !result.isEmpty() );

        //Remove the contentlet from the index
        initDate = new Date();
        indexAPI.removeContentFromIndexByStructureInode( testStructure.getInode() );

        //We are just making time in order to let it apply the index
        wait( initDate, 2 );

        result = contentletAPI.search( query, 0, -1, "modDate desc", user, true );

        //Validations
        assertTrue( result == null || result.isEmpty() );
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
        Field field = new Field( "testtext", Field.FieldType.TEXT, Field.DataType.TEXT, testStructure, true, true, true, 1, "", "", "", true, false, true );
        field.setVelocityVarName( "testtext" );
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

        LanguageAPI languageAPI = APILocator.getLanguageAPI();

        //Getting the default language
        Language language = languageAPI.getDefaultLanguage();

        //Set up a test contentlet
        Contentlet testContentlet = new Contentlet();
        testContentlet.setStructureInode( testStructure.getInode() );
        testContentlet.setHost( defaultHost.getIdentifier() );
        testContentlet.setLanguageId( language.getId() );
        testContentlet.setStringProperty( "testtext", "A test value" );
        testContentlet = APILocator.getContentletAPI().checkin( testContentlet, user, false );
        //Boolean indexed = APILocator.getContentletAPI().isInodeIndexed( testContentlet.getInode() );
        //Make it live
        APILocator.getVersionableAPI().setLive( testContentlet );

        return testContentlet;
    }

    private void wait ( Date from, int seconds ) {
        //We are just making time
        while ( compareInSeconds( from, new Date() ) < seconds ) {
            //Waiting.....
        }
    }

    private Long compareInSeconds ( Date from, Date to ) {
        return (to.getTime() - from.getTime()) / 1000;
    }

}