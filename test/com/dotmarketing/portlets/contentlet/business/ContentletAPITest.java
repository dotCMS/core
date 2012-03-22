package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import org.apache.lucene.queryParser.ParseException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Jonathan Gamba.
 * Date: 3/20/12
 * Time: 12:12 PM
 */
public class ContentletAPITest extends ContentletBaseTest {

    /**
     * Testing {@link ContentletAPI#findAllContent(int, int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findAllContent () throws DotDataException, DotSecurityException {

        //Getting all contentlets live/working contentlets
        List<Contentlet> contentlets = contentletAPI.findAllContent( 0, 5 );

        //Validations
        assertTrue( contentlets != null && !contentlets.isEmpty() );
        assertEquals( contentlets.size(), 5 );

        //Validate the integrity of the array
        Contentlet contentlet = contentletAPI.find( contentlets.iterator().next().getInode(), user, false );

        //Validations
        assertTrue( contentlet != null && ( contentlet.getInode() != null && !contentlet.getInode().isEmpty() ) );
    }

    /**
     * Testing {@link ContentletAPI#find(String, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void find () throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Search the contentlet
        Contentlet foundContentlet = contentletAPI.find( contentlet.getInode(), user, false );

        //Validations
        assertNotNull( foundContentlet );
        assertEquals( foundContentlet.getInode(), contentlet.getInode() );
    }

    /**
     * Testing {@link ContentletAPI#findContentletForLanguage(long, com.dotmarketing.beans.Identifier)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletForLanguage () throws DotDataException, DotSecurityException {

        //Getting our test contentlet
        Contentlet contentletWithLanguage = null;
        for ( Contentlet contentlet : contentlets ) {

            //Verify if we have a contentlet with a language set
            if ( contentlet.getLanguageId() != 0 ) {
                contentletWithLanguage = contentlet;
                break;
            }
        }

        Identifier contentletIdentifier = APILocator.getIdentifierAPI().find( contentletWithLanguage );

        //Search the contentlet
        assertNotNull( contentletWithLanguage );
        Contentlet foundContentlet = contentletAPI.findContentletForLanguage( contentletWithLanguage.getLanguageId(), contentletIdentifier );

        //Validations
        assertNotNull( foundContentlet );
        assertEquals( foundContentlet.getInode(), contentletWithLanguage.getInode() );
    }

    /**
     * Testing {@link ContentletAPI#findByStructure(com.dotmarketing.portlets.structure.model.Structure, com.liferay.portal.model.User, boolean, int, int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findByStructure () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Search the contentlet
        List<Contentlet> foundContentlets = contentletAPI.findByStructure( contentlet.getStructure(), user, false, 0, 0 );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#findByStructure(String, com.liferay.portal.model.User, boolean, int, int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findByStructureInode () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.findByStructure( contentlet.getStructureInode(), user, false, 0, 0 );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#findContentletByIdentifier(String, boolean, long, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletByIdentifier () throws DotSecurityException, DotDataException {

        //Getting our test contentlet
        Contentlet contentletWithLanguage = null;
        for ( Contentlet contentlet : contentlets ) {

            //Verify if we have a contentlet with a language set
            if ( contentlet.getLanguageId() != 0 ) {
                contentletWithLanguage = contentlet;
                break;
            }
        }

        //Search the contentlet
        assertNotNull( contentletWithLanguage );
        Contentlet foundContentlet = contentletAPI.findContentletByIdentifier( contentletWithLanguage.getIdentifier(), false, contentletWithLanguage.getLanguageId(), user, false );

        //Validations
        assertNotNull( foundContentlet );
        assertEquals( foundContentlet.getInode(), contentletWithLanguage.getInode() );
    }

    /**
     * Testing {@link ContentletAPI#findContentletsByIdentifiers(String[], boolean, long, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletByIdentifiers () throws DotSecurityException, DotDataException {

        //Getting our test contentlet
        Contentlet contentletWithLanguage = null;
        for ( Contentlet contentlet : contentlets ) {

            //Verify if we have a contentlet with a language set
            if ( contentlet.getLanguageId() != 0 ) {
                contentletWithLanguage = contentlet;
                break;
            }
        }

        //Search the contentlet
        assertNotNull( contentletWithLanguage );
        List<Contentlet> foundContentlets = contentletAPI.findContentletsByIdentifiers( new String[]{ contentletWithLanguage.getIdentifier() }, false, contentletWithLanguage.getLanguageId(), user, false );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#findContentlets(java.util.List)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentlets () throws DotSecurityException, DotDataException {

        //Getting our test inodes
        List<String> inodes = new ArrayList<String>();
        for ( Contentlet contentlet : contentlets ) {
            inodes.add( contentlet.getInode() );
        }

        //Search for the contentlets
        List<Contentlet> foundContentlets = contentletAPI.findContentlets( inodes );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
        assertEquals( foundContentlets.size(), contentlets.size() );
    }

    /**
     * Testing {@link ContentletAPI#findContentletsByFolder(com.dotmarketing.portlets.folders.model.Folder, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletsByFolder () throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Getting the folder of the test contentlet
        Folder folder = APILocator.getFolderAPI().find( contentlet.getFolder(), user, false );

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.findContentletsByFolder( folder, user, false );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#findContentletsByHost(com.dotmarketing.beans.Host, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletsByHost () throws DotDataException, DotSecurityException {

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.findContentletsByHost( defaultHost, user, false );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#copyContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void copyContentlet () throws DotSecurityException, DotDataException {

        Contentlet copyContentlet = null;
        try {
            //Getting a known contentlet
            Contentlet contentlet = contentlets.iterator().next();

            //Copy the test contentlet
            copyContentlet = contentletAPI.copyContentlet( contentlet, user, false );

            //validations
            assertTrue( copyContentlet != null && !copyContentlet.getInode().isEmpty() );
            assertEquals( copyContentlet.getStructureInode(), contentlet.getStructureInode() );
            assertEquals( copyContentlet.getIdentifier(), contentlet.getIdentifier() );
            assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
            assertEquals( copyContentlet.getHost(), contentlet.getHost() );
        } finally {
            contentletAPI.delete( copyContentlet, user, false );
        }
    }

    /**
     * Testing {@link ContentletAPI#copyContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.folders.model.Folder, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void copyContentletWithFolder () throws DotSecurityException, DotDataException {

        Contentlet copyContentlet = null;
        try {
            //Getting a known contentlet
            Contentlet contentlet = contentlets.iterator().next();

            //Getting the folder of the test contentlet
            Folder folder = APILocator.getFolderAPI().find( contentlet.getFolder(), user, false );

            //Copy the test contentlet
            copyContentlet = contentletAPI.copyContentlet( contentlet, folder, user, false );

            //validations
            assertTrue( copyContentlet != null && !copyContentlet.getInode().isEmpty() );
            assertEquals( copyContentlet.getStructureInode(), contentlet.getStructureInode() );
            assertEquals( copyContentlet.getIdentifier(), contentlet.getIdentifier() );
            assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
            assertEquals( copyContentlet.getHost(), contentlet.getHost() );
        } finally {
            contentletAPI.delete( copyContentlet, user, false );
        }
    }

    /**
     * Testing {@link ContentletAPI#copyContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.beans.Host, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void copyContentletWithHost () throws DotSecurityException, DotDataException {

        Contentlet copyContentlet = null;
        try {
            //Getting a known contentlet
            Contentlet contentlet = contentlets.iterator().next();

            //Copy the test contentlet
            copyContentlet = contentletAPI.copyContentlet( contentlet, defaultHost, user, false );

            //validations
            assertTrue( copyContentlet != null && !copyContentlet.getInode().isEmpty() );
            assertEquals( copyContentlet.getStructureInode(), contentlet.getStructureInode() );
            assertEquals( copyContentlet.getIdentifier(), contentlet.getIdentifier() );
            assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
            assertEquals( copyContentlet.getHost(), contentlet.getHost() );
        } finally {
            contentletAPI.delete( copyContentlet, user, false );
        }
    }

    /**
     * Testing {@link ContentletAPI#copyContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.folders.model.Folder, com.liferay.portal.model.User, boolean, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void copyContentletWithFolderAppendCopy () throws DotSecurityException, DotDataException {

        Contentlet copyContentlet = null;
        try {
            //Getting a known contentlet
            Contentlet contentlet = contentlets.iterator().next();

            //Getting the folder of the test contentlet
            Folder folder = APILocator.getFolderAPI().find( contentlet.getFolder(), user, false );

            //Copy the test contentlet
            copyContentlet = contentletAPI.copyContentlet( contentlet, folder, user, true, false );

            //validations
            assertTrue( copyContentlet != null && !copyContentlet.getInode().isEmpty() );
            assertEquals( copyContentlet.getStructureInode(), contentlet.getStructureInode() );
            assertEquals( copyContentlet.getIdentifier(), contentlet.getIdentifier() );
            assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
            assertEquals( copyContentlet.getHost(), contentlet.getHost() );
            assertTrue( copyContentlet.getStringProperty( FileAssetAPI.FILE_NAME_FIELD ).contains( "COPY" ) );
        } finally {
            contentletAPI.delete( copyContentlet, user, false );
        }
    }

    /**
     * Testing {@link ContentletAPI#search(String, int, int, String, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void search () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Create the lucene query
        String luceneQuery = "+structureinode:" + contentlet.getStructureInode() + " +deleted:false";

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.search( luceneQuery, 1000, -1, "inode", user, false );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#search(String, int, int, String, com.liferay.portal.model.User, boolean, int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @throws org.apache.lucene.queryParser.ParseException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void searchWithPermissions () throws DotSecurityException, DotDataException, ParseException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Create the lucene query
        String luceneQuery = "+structureinode:" + contentlet.getStructureInode() + " +deleted:false";

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.search( luceneQuery, 1000, -1, "inode", user, false, PermissionAPI.PERMISSION_READ );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#searchIndex(String, int, int, String, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @throws org.apache.lucene.queryParser.ParseException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void searchIndex () throws DotDataException, DotSecurityException, ParseException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Create the lucene query
        String luceneQuery = "+structureinode:" + contentlet.getStructureInode() + " +deleted:false";

        //Search the contentlets
        List<ContentletSearch> foundContentlets = contentletAPI.searchIndex( luceneQuery, 1000, -1, "inode", user, false );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

}