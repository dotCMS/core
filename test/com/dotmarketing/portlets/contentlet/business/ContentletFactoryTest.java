package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Jonathan Gamba.
 * Date: 3/19/12
 * Time: 11:32 AM
 */
public class ContentletFactoryTest extends ContentletBaseTest {

    /**
     * Testing {@link com.dotmarketing.portlets.contentlet.business.ContentletFactory#findAllCurrent()}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletFactory
     */
    @Test ( expected = DotDataException.class )
    public void findAllCurrent () throws DotDataException, DotSecurityException {

        //Getting all contentlets live/working contentlets
        List<Contentlet> contentlets = contentletFactory.findAllCurrent();

        //Validations
        assertTrue( contentlets != null && !contentlets.isEmpty() );

        //Validate the integrity of the array
        Contentlet contentlet = contentletFactory.find( contentlets.iterator().next().getInode() );

        //Validations
        assertTrue( contentlet != null && ( contentlet.getInode() != null && !contentlet.getInode().isEmpty() ) );
    }

    /**
     * Testing {@link com.dotmarketing.portlets.contentlet.business.ContentletFactory#findAllCurrent(int, int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletFactory
     */
    @Test
    public void findAllCurrentOffsetLimit () throws DotDataException, DotSecurityException {

        //Getting all contentlets live/working contentlets
        List<Contentlet> contentlets = contentletFactory.findAllCurrent( 0, 5 );

        //Validations
        assertTrue( contentlets != null && !contentlets.isEmpty() );
        assertEquals( contentlets.size(), 5 );

        //Validate the integrity of the array
        Contentlet foundContentlet = null;
        for ( Contentlet contentlet : contentlets ) {

            //TODO: We need to verify for null because the findAllCurrent CAN return null objects, this could happen because the index can return inodes that are not into the db....
            if ( contentlet != null ) {
                foundContentlet = contentlet;
                break;
            }
        }

        //Validations
        assertNotNull( foundContentlet );

        //Search for one of the objects we found
        String inode = foundContentlet.getInode();
        Contentlet contentlet = contentletFactory.find(inode);

        //Validations
        assertTrue( contentlet != null && ( contentlet.getInode() != null && !contentlet.getInode().isEmpty() ) );
    }

    /**
     * Testing {@link com.dotmarketing.portlets.contentlet.business.ContentletFactory#find(String)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletFactory
     */
    @Test
    public void find () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Search the contentlet
        Contentlet foundContentlet = contentletFactory.find( contentlet.getInode() );

        //Validations
        assertNotNull( foundContentlet );
        assertEquals( foundContentlet.getInode(), contentlet.getInode() );
    }

}