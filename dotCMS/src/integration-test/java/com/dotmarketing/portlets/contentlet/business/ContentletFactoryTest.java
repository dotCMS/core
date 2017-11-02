package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import java.util.ArrayList;
import java.util.Date;
import org.junit.Assert;
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

    @Test
    public void testGetRelatedIdentifier() throws DotSecurityException, DotDataException {

        Contentlet childContentlet = null;
        Contentlet parentContentlet = null;
        List<Contentlet> contentRelationships = null;
        Relationship testRelationship = null;
        Structure testStructure = null;

        try {
            //First lets create a test structure
            testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

            //Now a new test contentlets
            parentContentlet = createContentlet( testStructure, null, false );
            childContentlet = createContentlet( testStructure, null, false );

            //Create the relationship
            testRelationship = createRelationShip( testStructure, false );

            //Create the contentlet relationships
            contentRelationships = new ArrayList<>();
            contentRelationships.add( childContentlet );
            ContentletRelationships contentletRelationships = createContentletRelationships( testRelationship, parentContentlet, testStructure, contentRelationships );

            //Relate contents to our test contentlet
            for ( ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords : contentletRelationships.getRelationshipsRecords() ) {
                //Testing the relate content...
                contentletAPI.relateContent( parentContentlet, contentletRelationshipRecords, user, false );
            }

            Identifier identifier = contentletFactory.getRelatedIdentifier(parentContentlet, "parent-child");

            Assert.assertEquals(childContentlet.getIdentifier(), identifier.getId());
        } finally {
            if (parentContentlet != null && testRelationship != null && contentRelationships != null
                    && !contentRelationships.isEmpty()) {
                contentletAPI
                        .deleteRelatedContent(childContentlet, testRelationship, true, user, false);
            }

            if (parentContentlet != null) {
                contentletAPI.delete(parentContentlet, user, false);
            }

            if (childContentlet != null) {
                contentletAPI.delete(childContentlet, user, false);
            }

            if (testStructure != null) {
                StructureFactory.deleteStructure(testStructure);
            }
        }
    }
}