package com.dotcms.content.elasticsearch.business;

import com.dotcms.TestBase;
import com.dotmarketing.business.APILocator;
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
     * and {@link ContentletIndexAPI#getNewIndex()}
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

}