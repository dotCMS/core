package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.content.business.DotMappingException;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import org.apache.lucene.queryParser.ParseException;
import org.junit.Test;

import java.util.*;

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
            assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
            assertEquals( copyContentlet.get( "junitTestWysiwyg" ), contentlet.get( "junitTestWysiwyg" ) );
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
            assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
            assertEquals( copyContentlet.get( "junitTestWysiwyg" ), contentlet.get( "junitTestWysiwyg" ) );
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
            assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
            assertEquals( copyContentlet.get( "junitTestWysiwyg" ), contentlet.get( "junitTestWysiwyg" ) );
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

    /**
     * Testing {@link ContentletAPI#publishRelatedHtmlPages(com.dotmarketing.portlets.contentlet.model.Contentlet)}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotCacheException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void publishRelatedHtmlPages () throws DotDataException, DotSecurityException, DotCacheException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Making it live
        APILocator.getVersionableAPI().setLive( contentlet );

        //Publish html pages for this contentlet
        contentletAPI.publishRelatedHtmlPages( contentlet );

        //TODO: How to validate this???, good question, basically checking that the html page is not in cache basically the method publishRelatedHtmlPages(...) will just remove the htmlPage from cache

        //Get the contentlet Identifier to gather the related pages
        Identifier identifier = APILocator.getIdentifierAPI().find( contentlet );
        //Get the identifier's number of the related pages
        List<MultiTree> multiTrees = MultiTreeFactory.getMultiTreeByChild( identifier.getInode() );
        for ( MultiTree multitree : multiTrees ) {
            //Get the Identifiers of the related pages
            Identifier htmlPageIdentifier = APILocator.getIdentifierAPI().find( multitree.getParent1() );
            //Get the pages
            HTMLPage htmlPage = ( HTMLPage ) APILocator.getVersionableAPI().findLiveVersion( htmlPageIdentifier, APILocator.getUserAPI().getSystemUser(), false );

            //OK..., lets try to find this page in the cache...
            HTMLPage foundPage = ( HTMLPage ) CacheLocator.getCacheAdministrator().get( "HTMLPageCache" + htmlPage.getIdentifier(), "HTMLPageCache" );

            //Validations
            assertTrue( foundPage == null || ( foundPage.getInode() == null || foundPage.getInode().equals( "" ) ) );
        }
    }

    /**
     * Testing {@link ContentletAPI#cleanField(com.dotmarketing.portlets.structure.model.Structure, com.dotmarketing.portlets.structure.model.Field, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void cleanField () throws DotDataException, DotSecurityException {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Search the contentlet for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );
        Contentlet contentlet = contentletList.iterator().next();

        //Getting a know field for this structure
        //TODO: The definition of the method getFieldByName receive a parameter named "String:structureType", some examples I saw send the Inode, but actually what it needs is the structure name....
        Field foundWysiwygField = FieldFactory.getFieldByName( structure.getName(), "JUnit Test Wysiwyg" );

        //Getting the current value for this field
        Object value = contentletAPI.getFieldValue( contentlet, foundWysiwygField );

        //Validations
        assertNotNull( value );
        assertTrue( !( ( String ) value ).isEmpty() );

        //Set to the default value
        contentletAPI.cleanField( structure, foundWysiwygField, user, false );

        //Search for the value again
        Object newValue = contentletAPI.getFieldValue( contentlet, foundWysiwygField );

        //Validations
        assertNotSame( value, newValue );
    }

    /**
     * Testing {@link ContentletAPI#cleanHostField(com.dotmarketing.portlets.structure.model.Structure, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotMappingException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void cleanHostField () throws DotDataException, DotSecurityException, DotMappingException {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Search the contentlet for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );

        //Check the current identifies
        Identifier contentletIdentifier = APILocator.getIdentifierAPI().find( contentletList.iterator().next() );

        //Cleaning the host field for the identifier
        contentletAPI.cleanHostField( structure, user, false );

        //Now get again the identifier to see if the change was made
        Identifier changedContentletIdentifier = APILocator.getIdentifierAPI().find( contentletList.iterator().next() );

        //Validations
        assertNotNull( changedContentletIdentifier );
        assertNotSame( contentletIdentifier, changedContentletIdentifier );
        assertNotSame( contentletIdentifier.getHostId(), changedContentletIdentifier.getHostId() );
    }

    /**
     * Testing {@link ContentletAPI#getNextReview(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getNextReview () throws DotSecurityException, DotDataException {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Search the contentlet for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );

        //Getting the next review date
        Date nextReview = contentletAPI.getNextReview( contentletList.iterator().next(), user, false );

        //Validations
        assertNotNull( nextReview );
    }

    /**
     * Testing {@link ContentletAPI#getContentletReferences(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getContentletReferences () throws DotSecurityException, DotDataException {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Search the contentlet for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );

        //Retrieve all the references for this Contentlet.
        List<Map<String, Object>> references = contentletAPI.getContentletReferences( contentletList.iterator().next(), user, false );

        //Validations
        assertNotNull( references );
        assertTrue( !references.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#getFieldValue(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Field)}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getFieldValue () throws DotSecurityException, DotDataException {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting a know field for this structure
        //TODO: The definition of the method getFieldByName receive a parameter named "String:structureType", some examples I saw send the Inode, but actually what it needs is the structure name....
        Field foundWysiwygField = FieldFactory.getFieldByName( structure.getName(), "JUnit Test Wysiwyg" );

        //Search the contentlets for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );

        //Getting the current value for this field
        Object value = contentletAPI.getFieldValue( contentletList.iterator().next(), foundWysiwygField );

        //Validations
        assertNotNull( value );
        assertTrue( !( ( String ) value ).isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#addLinkToContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, String, String, com.liferay.portal.model.User, boolean)}
     *
     * @throws Exception
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void addLinkToContentlet () throws Exception {

        Link menuLink = null;
        try {

            String RELATION_TYPE = new Link().getType();

            //Getting a known structure
            Structure structure = structures.iterator().next();

            //Create a menu link
            menuLink = createMenuLink();

            //Search the contentlets for this structure
            List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );
            Contentlet contentlet = contentletList.iterator().next();

            //Add to this contentlet a link
            contentletAPI.addLinkToContentlet( contentlet, menuLink.getInode(), RELATION_TYPE, user, false );

            //Verify if the link was associated
            //List<Link> relatedLinks = contentletAPI.getRelatedLinks( contentlet, user, false );//TODO: This method is not working but we are not testing it on this call....

            //Get the contentlet Identifier to gather the menu links
            Identifier menuLinkIdentifier = APILocator.getIdentifierAPI().find( menuLink );

            //Verify if the relation was created
            Tree tree = TreeFactory.getTree( contentlet.getInode(), menuLinkIdentifier.getInode(), RELATION_TYPE );

            //Validations
            assertNotNull( tree );
            assertNotNull( tree.getParent() );
            assertNotNull( tree.getChild() );
            assertEquals( tree.getParent(), contentlet.getInode() );
            assertEquals( tree.getChild(), menuLinkIdentifier.getInode() );
            assertEquals( tree.getRelationType(), RELATION_TYPE );
        } finally {
            menuLinkAPI.delete( menuLink, user, false );
        }
    }

    /**
     * Testing {@link ContentletAPI#addFileToContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, String, String, com.liferay.portal.model.User, boolean)}
     *
     * @throws Exception
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void addFileToContentlet () throws Exception {

        File testFile = null;
        try {

            String RELATION_TYPE = new File().getType();

            //Getting a known structure
            Structure structure = structures.iterator().next();

            //Search the contentlets for this structure
            List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );
            Contentlet contentlet = contentletList.iterator().next();

            //Creating the test file
            testFile = createFile( "test.txt" );

            /*//Gettting the related files to this contentlet
            List<File> files = contentletAPI.getRelatedFiles( contentlet, user, false );//TODO: This method is not working but we are not testing it on this call....
            if ( files == null ) {
                files = new ArrayList<File>();
            }
            int initialSize = files.size();*/

            //Adding the file to the contentlet
            contentletAPI.addFileToContentlet( contentlet, testFile.getInode(), RELATION_TYPE, user, false );

            //Gettting the related files to this contentlet
            //List<File> finalFiles = contentletAPI.getRelatedFiles( contentlet, user, false );//TODO: This method is not working but we are not testing it on this call....

            //Get the contentlet Identifier to gather the menu links
            Identifier fileIdentifier = APILocator.getIdentifierAPI().find( testFile );

            //Verify if the relation was created
            Tree tree = TreeFactory.getTree( contentlet.getInode(), fileIdentifier.getInode(), RELATION_TYPE );

            //Validations
            assertNotNull( tree );
            assertNotNull( tree.getParent() );
            assertNotNull( tree.getChild() );
            assertEquals( tree.getParent(), contentlet.getInode() );
            assertEquals( tree.getChild(), fileIdentifier.getInode() );
            assertEquals( tree.getRelationType(), RELATION_TYPE );

            //Validations
            /*assertNotNull( finalFiles );
            assertTrue( !finalFiles.isEmpty() );
            assertNotSame( initialSize, finalFiles.size() );*/
        } finally {
            fileAPI.delete( testFile, user, false );
        }
    }

    /**
     * Testing {@link ContentletAPI#addImageToContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, String, String, com.liferay.portal.model.User, boolean)}
     *
     * @throws Exception
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void addImageToContentlet () throws Exception {

        File testFile = null;
        try {

            String RELATION_TYPE = new File().getType();

            //Getting a known structure
            Structure structure = structures.iterator().next();

            //Search the contentlets for this structure
            List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );
            Contentlet contentlet = contentletList.iterator().next();

            //Creating the test file
            testFile = createFile( "test.gif" );

            /*//Gettting the related files to this contentlet
            List<File> files = contentletAPI.getRelatedFiles( contentlet, user, false );//TODO: This method is not working but we are not testing it on this call....
            if ( files == null ) {
                files = new ArrayList<File>();
            }
            int initialSize = files.size();*/

            //Adding the file to the contentlet
            contentletAPI.addImageToContentlet( contentlet, testFile.getInode(), RELATION_TYPE, user, false );

            //Gettting the related files to this contentlet
            //List<File> finalFiles = contentletAPI.getRelatedFiles( contentlet, user, false );//TODO: This method is not working but we are not testing it on this call....

            //Get the contentlet Identifier to gather the menu links
            Identifier fileIdentifier = APILocator.getIdentifierAPI().find( testFile );

            //Verify if the relation was created
            Tree tree = TreeFactory.getTree( contentlet.getInode(), fileIdentifier.getInode(), RELATION_TYPE );

            //Validations
            assertNotNull( tree );
            assertNotNull( tree.getParent() );
            assertNotNull( tree.getChild() );
            assertEquals( tree.getParent(), contentlet.getInode() );
            assertEquals( tree.getChild(), fileIdentifier.getInode() );
            assertEquals( tree.getRelationType(), RELATION_TYPE );

            /*//Validations
            assertNotNull( finalFiles );
            assertTrue( !finalFiles.isEmpty() );
            assertNotSame( initialSize, finalFiles.size() );*/
        } finally {
            fileAPI.delete( testFile, user, false );
        }
    }

    /**
     * Testing {@link ContentletAPI#findPageContentlets(String, String, String, boolean, long, com.liferay.portal.model.User, boolean)}
     *
     * @throws Exception
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findPageContentlets () throws DotDataException, DotSecurityException {

        //Iterate throw the test contentles
        for ( Contentlet contentlet : contentlets ) {

            //Get the identifier for this contentlet
            Identifier identifier = APILocator.getIdentifierAPI().find( contentlet );

            //Search for related html pages and containers
            List<MultiTree> multiTrees = MultiTreeFactory.getMultiTreeByChild( identifier.getInode() );
            if ( multiTrees != null && !multiTrees.isEmpty() ) {

                for ( MultiTree multiTree : multiTrees ) {

                    //Getting the identifiers
                    Identifier htmlPageIdentifier = APILocator.getIdentifierAPI().find( multiTree.getParent1() );
                    Identifier containerPageIdentifier = APILocator.getIdentifierAPI().find( multiTree.getParent2() );

                    //Find the related contentlets, at this point should return something....
                    List<Contentlet> pageContentlets = contentletAPI.findPageContentlets( htmlPageIdentifier.getInode(), containerPageIdentifier.getInode(), null, true, -1, user, false );

                    //Validations
                    assertTrue( pageContentlets != null && !pageContentlets.isEmpty() );
                }

                break;
            }
        }
    }

    /**
     * Testing {@link ContentletAPI#getAllRelationships(com.dotmarketing.portlets.contentlet.model.Contentlet)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getAllRelationships () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Create the test relationship
        createRelationShip( contentlet.getStructure(), false );

        //Find all the relationships for this contentlet
        ContentletRelationships contentletRelationships = contentletAPI.getAllRelationships( contentlet.getInode(), user, false );

        //Validations
        assertNotNull( contentletRelationships );
        assertTrue( contentletRelationships.getRelationshipsRecords() != null && !contentletRelationships.getRelationshipsRecords().isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#getAllRelationships(com.dotmarketing.portlets.contentlet.model.Contentlet)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getAllRelationshipsByContentlet () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Find all the relationships for this contentlet
        ContentletRelationships contentletRelationships = contentletAPI.getAllRelationships( contentlet );

        //Validations
        assertNotNull( contentletRelationships );
        assertTrue( contentletRelationships.getRelationshipsRecords() != null && !contentletRelationships.getRelationshipsRecords().isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#getAllLanguages(com.dotmarketing.portlets.contentlet.model.Contentlet, Boolean, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getAllLanguages () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Get all the contentles siblings for this contentlet (contentlet for all the languages)
        List<Contentlet> forAllLanguages = contentletAPI.getAllLanguages( contentlet, true, user, false );

        //Validations
        assertNotNull( forAllLanguages );
        assertTrue( !forAllLanguages.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#isContentEqual(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void isContentEqual () throws DotDataException, DotSecurityException {

        Iterator<Contentlet> contentletIterator = contentlets.iterator();

        //Getting test contentlets
        Contentlet contentlet1 = contentletIterator.next();
        Contentlet contentlet2 = contentletIterator.next();

        //Compare if the contentlets are equal
        Boolean areEqual = contentletAPI.isContentEqual( contentlet1, contentlet2, user, false );

        //Validations
        assertNotNull( areEqual );
        assertFalse( areEqual );
    }

    /**
     * Testing {@link ContentletAPI#archive(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void archive () throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        try {
            //Archive this given contentlet (means it will be mark it as deleted)
            contentletAPI.archive( contentlet, user, false );

            //Verify if it was deleted
            Boolean isDeleted = APILocator.getVersionableAPI().isDeleted( contentlet );

            //Validations
            assertNotNull( isDeleted );
            assertTrue( isDeleted );
        } finally {
            contentletAPI.unarchive( contentlet, user, false );
        }
    }

    /**
     * Testing {@link ContentletAPI#delete(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void delete () throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new contentlet
        Contentlet newContentlet = createContentlet( testStructure, null, false );

        //Now we need to delete it
        contentletAPI.delete( newContentlet, user, false );

        //Try to find the deleted Contentlet
        Contentlet foundContentlet = contentletAPI.find( newContentlet.getInode(), user, false );

        //Validations
        assertTrue( foundContentlet == null || foundContentlet.getInode() == null || foundContentlet.getInode().isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#delete(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteForAllVersions () throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new contentlet
        Contentlet newContentlet = createContentlet( testStructure, null, false );

        //Now we need to delete it
        contentletAPI.delete( newContentlet, user, false, true );

        //Try to find the deleted Contentlet
        Contentlet foundContentlet = contentletAPI.find( newContentlet.getInode(), user, false );

        //Validations
        assertTrue( foundContentlet == null || foundContentlet.getInode() == null || foundContentlet.getInode().isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#publish(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void publish () throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Publish the test contentlet
        contentletAPI.publish( contentlet, user, false );

        //Verify if it was published
        Boolean isLive = APILocator.getVersionableAPI().isLive( contentlet );

        //Validations
        assertNotNull( isLive );
        assertTrue( isLive );
    }

    /**
     * Testing {@link ContentletAPI#publish(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void publishCollection () throws DotDataException, DotSecurityException {

        //Publish all the test contentlets
        contentletAPI.publish( contentlets, user, false );

        for ( Contentlet contentlet : contentlets ) {

            //Verify if it was published
            Boolean isLive = APILocator.getVersionableAPI().isLive( contentlet );

            //Validations
            assertNotNull( isLive );
            assertTrue( isLive );
        }
    }

    /**
     * Testing {@link ContentletAPI#unpublish(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void unpublish () throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Unpublish the test contentlet
        contentletAPI.unpublish( contentlet, user, false );

        //Verify if it was unpublished
        Boolean isLive = APILocator.getVersionableAPI().isLive( contentlet );

        //Validations
        assertNotNull( isLive );
        assertFalse( isLive );
    }

    /**
     * Testing {@link ContentletAPI#unpublish(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void unpublishCollection () throws DotDataException, DotSecurityException {

        //Unpublish all the test contentlets
        contentletAPI.unpublish( contentlets, user, false );

        for ( Contentlet contentlet : contentlets ) {

            //Verify if it was unpublished
            Boolean isLive = APILocator.getVersionableAPI().isLive( contentlet );

            //Validations
            assertNotNull( isLive );
            assertFalse( isLive );
        }
    }

    /**
     * Testing {@link ContentletAPI#archive(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void archiveCollection () throws DotDataException, DotSecurityException {

        try {
            //Archive this given contentlet collection (means it will be mark them as deleted)
            contentletAPI.archive( contentlets, user, false );

            for ( Contentlet contentlet : contentlets ) {

                //Verify if it was deleted
                Boolean isDeleted = APILocator.getVersionableAPI().isDeleted( contentlet );

                //Validations
                assertNotNull( isDeleted );
                assertTrue( isDeleted );
            }
        } finally {
            contentletAPI.unarchive( contentlets, user, false );
        }
    }

    /**
     * Testing {@link ContentletAPI#unarchive(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void unarchiveCollection () throws DotDataException, DotSecurityException {

        //First lets archive this given contentlet collection (means it will be mark them as deleted)
        contentletAPI.archive( contentlets, user, false );

        //Now lets test the unarchive
        contentletAPI.unarchive( contentlets, user, false );

        for ( Contentlet contentlet : contentlets ) {

            //Verify if it continues as deleted
            Boolean isDeleted = APILocator.getVersionableAPI().isDeleted( contentlet );

            //Validations
            assertNotNull( isDeleted );
            assertFalse( isDeleted );
        }
    }

    /**
     * Testing {@link ContentletAPI#unarchive(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void unarchive () throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //First lets archive this given contentlet (means it will be mark it as deleted)
        contentletAPI.archive( contentlet, user, false );

        //Now lets test the unarchive
        contentletAPI.unarchive( contentlet, user, false );

        //Verify if it continues as deleted
        Boolean isDeleted = APILocator.getVersionableAPI().isDeleted( contentlet );

        //Validations
        assertNotNull( isDeleted );
        assertFalse( isDeleted );
    }

    /**
     * Testing {@link ContentletAPI#deleteAllVersionsandBackup(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteAllVersionsAndBackup () throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new contentlet
        Contentlet newContentlet = createContentlet( testStructure, null, false );
        Identifier contentletIdentifier = APILocator.getIdentifierAPI().find( newContentlet.getIdentifier() );

        //Now test this delete
        List<Contentlet> testContentlets = new ArrayList<Contentlet>();
        testContentlets.add( newContentlet );
        contentletAPI.deleteAllVersionsandBackup( testContentlets, user, false );

        //Try to find the versions for this Contentlet (Must be only one version)
        List<Contentlet> versions = contentletAPI.findAllVersions( contentletIdentifier, user, false );

        //Validations
        assertNotNull( versions );
        assertEquals( versions.size(), 1 );
    }

    /**
     * Testing {@link ContentletAPI#delete(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteCollection () throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new contentlet
        Contentlet newContentlet = createContentlet( testStructure, null, false );

        //Now test this delete
        List<Contentlet> testContentlets = new ArrayList<Contentlet>();
        testContentlets.add( newContentlet );
        contentletAPI.delete( testContentlets, user, false );

        //Try to find the deleted Contentlet
        Contentlet foundContentlet = contentletAPI.find( newContentlet.getInode(), user, false );

        //Validations
        assertTrue( foundContentlet == null || foundContentlet.getInode() == null || foundContentlet.getInode().isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#delete(java.util.List, com.liferay.portal.model.User, boolean, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteCollectionAllVersions () throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new contentlet
        Contentlet newContentlet = createContentlet( testStructure, null, false );

        //Now test this delete
        List<Contentlet> testContentlets = new ArrayList<Contentlet>();
        testContentlets.add( newContentlet );
        contentletAPI.delete( testContentlets, user, false, true );

        //Try to find the deleted Contentlet
        Contentlet foundContentlet = contentletAPI.find( newContentlet.getInode(), user, false );

        //Validations
        assertTrue( foundContentlet == null || foundContentlet.getInode() == null || foundContentlet.getInode().isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#deleteRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteRelatedContent () throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new test contentlets
        Contentlet parentContentlet = createContentlet( testStructure, null, false );
        Contentlet childContentlet = createContentlet( testStructure, null, false );

        //Create the relationship
        Relationship testRelationship = createRelationShip( testStructure, false );

        //Create the contentlet relationships
        List<Contentlet> contentRelationships = new ArrayList<Contentlet>();
        contentRelationships.add( childContentlet );
        ContentletRelationships contentletRelationships = createContentletRelationships( testRelationship, parentContentlet, testStructure, contentRelationships );

        //Relate contents to our test contentlet
        for ( ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords : contentletRelationships.getRelationshipsRecords() ) {
            contentletAPI.relateContent( parentContentlet, contentletRelationshipRecords, user, false );
        }

        //Now test this delete
        contentletAPI.deleteRelatedContent( parentContentlet, testRelationship, user, false );

        //Try to find the deleted Contentlet
        List<Contentlet> foundContentlets = contentletAPI.getRelatedContent( parentContentlet, testRelationship, user, false );

        //Validations
        assertTrue( foundContentlets == null || foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#deleteRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, boolean, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteRelatedContentWithParent () throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new test contentlets
        Contentlet parentContentlet = createContentlet( testStructure, null, false );
        Contentlet childContentlet = createContentlet( testStructure, null, false );

        //Create the relationship
        Relationship testRelationship = createRelationShip( testStructure, false );

        //Create the contentlet relationships
        List<Contentlet> contentRelationships = new ArrayList<Contentlet>();
        contentRelationships.add( childContentlet );
        ContentletRelationships contentletRelationships = createContentletRelationships( testRelationship, parentContentlet, testStructure, contentRelationships );

        //Relate contents to our test contentlet
        for ( ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords : contentletRelationships.getRelationshipsRecords() ) {
            contentletAPI.relateContent( parentContentlet, contentletRelationshipRecords, user, false );
        }

        Boolean hasParent = RelationshipFactory.isParentOfTheRelationship( testRelationship, parentContentlet.getStructure() );

        //Now test this delete
        contentletAPI.deleteRelatedContent( parentContentlet, testRelationship, hasParent, user, false );

        //Try to find the deleted Contentlet
        List<Contentlet> foundContentlets = contentletAPI.getRelatedContent( parentContentlet, testRelationship, user, false );

        //Validations
        assertTrue( foundContentlets == null || foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#relateContent(Contentlet, ContentletRelationships.ContentletRelationshipRecords, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void relateContent () throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new test contentlets
        Contentlet parentContentlet = createContentlet( testStructure, null, false );
        Contentlet childContentlet = createContentlet( testStructure, null, false );

        //Create the relationship
        Relationship testRelationship = createRelationShip( testStructure, false );

        //Create the contentlet relationships
        List<Contentlet> contentRelationships = new ArrayList<Contentlet>();
        contentRelationships.add( childContentlet );
        ContentletRelationships contentletRelationships = createContentletRelationships( testRelationship, parentContentlet, testStructure, contentRelationships );

        //Relate contents to our test contentlet
        for ( ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords : contentletRelationships.getRelationshipsRecords() ) {
            //Testing the relate content...
            contentletAPI.relateContent( parentContentlet, contentletRelationshipRecords, user, false );
        }

        //Try to find the related Contentlet
        //List<Contentlet> foundContentlets = contentletAPI.getRelatedContent( parentContentlet, testRelationship, user, false );//TODO: This is not the correct method to test the relateContent?? (relateContent and getRelatedContent..., is should, some how it does work for me....)

        /*//Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );*/

        //Verify if the content was related
        Tree tree = TreeFactory.getTree( parentContentlet.getIdentifier(), childContentlet.getIdentifier(), testRelationship.getRelationTypeValue() );

        //Validations
        assertNotNull( tree );
        assertNotNull( tree.getParent() );
        assertNotNull( tree.getChild() );
        assertEquals( tree.getParent(), parentContentlet.getIdentifier() );
        assertEquals( tree.getChild(), childContentlet.getIdentifier() );
        assertEquals( tree.getRelationType(), testRelationship.getRelationTypeValue() );
    }

    /**
     * Testing {@link ContentletAPI#relateContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void relateContentDirect () throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new test contentlets
        Contentlet parentContentlet = createContentlet( testStructure, null, false );
        Contentlet childContentlet = createContentlet( testStructure, null, false );

        //Create the relationship
        Relationship testRelationship = createRelationShip( testStructure, false );

        //Create the contentlet relationships
        List<Contentlet> contentRelationships = new ArrayList<Contentlet>();
        contentRelationships.add( childContentlet );

        //Relate the content
        contentletAPI.relateContent( parentContentlet, testRelationship, contentRelationships, user, false );

        //Try to find the related Contentlet
        //List<Contentlet> foundContentlets = contentletAPI.getRelatedContent( parentContentlet, testRelationship, user, false );//TODO: This is not the correct method to test the relateContent?? (relateContent and getRelatedContent..., is should, some how it does work for me....)

        /*//Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );*/

        //Verify if the content was related
        Tree tree = TreeFactory.getTree( parentContentlet.getIdentifier(), childContentlet.getIdentifier(), testRelationship.getRelationTypeValue() );

        //Validations
        assertNotNull( tree );
        assertNotNull( tree.getParent() );
        assertNotNull( tree.getChild() );
        assertEquals( tree.getParent(), parentContentlet.getIdentifier() );
        assertEquals( tree.getChild(), childContentlet.getIdentifier() );
        assertEquals( tree.getRelationType(), testRelationship.getRelationTypeValue() );
    }

    /**
     * Testing {@link ContentletAPI#getRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getRelatedContent () throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new test contentlets
        Contentlet parentContentlet = createContentlet( testStructure, null, false );
        Contentlet childContentlet = createContentlet( testStructure, null, false );

        //Create the relationship
        Relationship testRelationship = createRelationShip( testStructure, false );

        //Create the contentlet relationships
        List<Contentlet> contentRelationships = new ArrayList<Contentlet>();
        contentRelationships.add( childContentlet );

        //Relate the content
        contentletAPI.relateContent( parentContentlet, testRelationship, contentRelationships, user, false );

        //Try to find the related Contentlet
        //List<Contentlet> foundContentlets = contentletAPI.getRelatedContent( parentContentlet, testRelationship, user, false );

        List<Relationship> relationships = RelationshipFactory.getAllRelationshipsByStructure( parentContentlet.getStructure() );
        //Validations
        assertTrue( relationships != null && !relationships.isEmpty() );

        List<Contentlet> foundContentlets = null;
        for ( Relationship relationship : relationships ) {
            foundContentlets = contentletAPI.getRelatedContent( parentContentlet, relationship, user, true );
        }

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#getRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, boolean, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getRelatedContentPullByParent () throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new test contentlets
        Contentlet parentContentlet = createContentlet( testStructure, null, false );
        Contentlet childContentlet = createContentlet( testStructure, null, false );

        //Create the relationship
        Relationship testRelationship = createRelationShip( testStructure, false );

        //Create the contentlet relationships
        List<Contentlet> contentRelationships = new ArrayList<Contentlet>();
        contentRelationships.add( childContentlet );

        //Relate the content
        contentletAPI.relateContent( parentContentlet, testRelationship, contentRelationships, user, false );

        Boolean hasParent = RelationshipFactory.isParentOfTheRelationship( testRelationship, parentContentlet.getStructure() );

        List<Relationship> relationships = RelationshipFactory.getAllRelationshipsByStructure( parentContentlet.getStructure() );
        //Validations
        assertTrue( relationships != null && !relationships.isEmpty() );

        List<Contentlet> foundContentlets = null;
        for ( Relationship relationship : relationships ) {
            foundContentlets = contentletAPI.getRelatedContent( parentContentlet, relationship, hasParent, user, true );
        }

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

}