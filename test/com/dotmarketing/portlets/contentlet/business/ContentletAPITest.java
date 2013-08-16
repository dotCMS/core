package com.dotmarketing.portlets.contentlet.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.junit.Ignore;
import org.junit.Test;

import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cmis.proxy.DotInvocationHandler;
import com.dotmarketing.cmis.proxy.DotRequestProxy;
import com.dotmarketing.cmis.proxy.DotResponseProxy;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;

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
    @Ignore ( "Not Ready to Run." )
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
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void searchWithPermissions () throws DotSecurityException, DotDataException {

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
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void searchIndex () throws DotDataException, DotSecurityException {

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
    @Ignore ( "Not Ready to Run." )
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

        menuLinkAPI.delete( menuLink, user, false );

    }

    /**
     * Testing {@link ContentletAPI#addFileToContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, String, String, com.liferay.portal.model.User, boolean)}
     *
     * @throws Exception
     * @see ContentletAPI
     * @see Contentlet
     */
    @Ignore ( "Not Ready to Run." )
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
    @Ignore ( "Not Ready to Run." )
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

        //Getting a known contentlet
//        Contentlet contentlet = contentlets.iterator().next();

        //Find all the relationships for this contentlet
        ContentletRelationships contentletRelationships = contentletAPI.getAllRelationships( parentContentlet );

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

        Structure st=new Structure();
        st.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
        st.setName("JUNIT-test-getAllLanguages"+System.currentTimeMillis());
        st.setVelocityVarName("testAllLanguages"+System.currentTimeMillis());
        st.setHost(defaultHost.getIdentifier());
        StructureFactory.saveStructure(st);

        Field ff=new Field("title",Field.FieldType.TEXT,Field.DataType.TEXT,st,true,true,true,1,false,false,true);
        FieldFactory.saveField(ff);

        String identifier=null;
        List<Language> list=APILocator.getLanguageAPI().getLanguages();
        Contentlet last=null;
        for(Language ll : list) {
            Contentlet con=new Contentlet();
            con.setStructureInode(st.getInode());
            if(identifier!=null) con.setIdentifier(identifier);
            con.setStringProperty(ff.getVelocityVarName(), "test text "+System.currentTimeMillis());
            con.setLanguageId(ll.getId());
            con=contentletAPI.checkin(con, user, false);
            if(identifier==null) identifier=con.getIdentifier();
            contentletAPI.isInodeIndexed(con.getInode());
            APILocator.getVersionableAPI().setLive(con);
            last=con;
        }

        //Get all the contentles siblings for this contentlet (contentlet for all the languages)
        List<Contentlet> forAllLanguages = contentletAPI.getAllLanguages( last, true, user, false );

        //Validations
        assertNotNull( forAllLanguages );
        assertTrue( !forAllLanguages.isEmpty() );
        assertEquals(list.size(), forAllLanguages.size());
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
    public void delete () throws Exception {

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

        // make sure the db is totally clean up

        AssetUtil.assertDeleted(newContentlet.getInode(), newContentlet.getIdentifier(), "contentlet");
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

        //Verify if it is published
        Boolean isLive = APILocator.getVersionableAPI().isLive( contentlet );
        if ( !isLive ) {
            //Publish the test contentlet
            contentletAPI.publish( contentlet, user, false );

            //Verify if it was published
            isLive = APILocator.getVersionableAPI().isLive( contentlet );

            //Validations
            assertNotNull( isLive );
            assertTrue( isLive );
        }

        //Unpublish the test contentlet
        contentletAPI.unpublish( contentlet, user, false );

        //Verify if it was unpublished
        isLive = APILocator.getVersionableAPI().isLive( contentlet );

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
    @Ignore ( "Not Ready to Run." )
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
    @Ignore ( "Not Ready to Run." )
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

    /**
     * Now we introduce the case when we wanna add content with
     * the inode & identifier we set. The content should not exists
     * for that inode nor the identifier.
     *
     * @throws Exception if test fails
     */
    @Test
    public void saveContentWithExistingIdentifier() throws Exception {
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ) + "zzz", "junit_test_structure_" + String.valueOf( new Date().getTime() ) + "zzz" );

        Field field = new Field( "JUnit Test Text", Field.FieldType.TEXT, Field.DataType.TEXT, testStructure, false, true, false, 1, false, false, false );
        FieldFactory.saveField( field );

        Contentlet cont=new Contentlet();
        cont.setStructureInode(testStructure.getInode());
        cont.setStringProperty(field.getVelocityVarName(), "a value");
        cont.setReviewInterval( "1m" );
        cont.setStructureInode( testStructure.getInode() );
        cont.setHost( defaultHost.getIdentifier() );

        // here comes the existing inode and identifier
        // for this test we generate them using the normal
        // generator but the use case for this is when
        // the content comes from another dotCMS instance
        String inode=UUIDGenerator.generateUuid();
        String identifier=UUIDGenerator.generateUuid();
        cont.setInode(inode);
        cont.setIdentifier(identifier);

        Contentlet saved = contentletAPI.checkin(cont, user, false);
        //contentlets.add(saved);

        assertEquals(saved.getInode(), inode);
        assertEquals(saved.getIdentifier(), identifier);

        // the inode should hit the index
        contentletAPI.isInodeIndexed(inode, 2);

        CacheLocator.getContentletCache().clearCache();

        // now lets test with existing content
        Contentlet existing=contentletAPI.find(inode, user, false);
        assertEquals(inode, existing.getInode());
        assertEquals(identifier, existing.getIdentifier());

        // new inode to create a new version
        String newInode=UUIDGenerator.generateUuid();
        existing.setInode(newInode);

        saved=contentletAPI.checkin(existing, user, false);
        contentlets.add(saved);

        assertEquals(newInode, saved.getInode());
        assertEquals(identifier, saved.getIdentifier());

        contentletAPI.isInodeIndexed(newInode);
    }

    /**
     * Making sure we set pub/exp dates on identifier when saving content
     * and we set them back to the content when reading.
     *
     * https://github.com/dotCMS/dotCMS/issues/1763
     */
    @Test
    public void testPubExpDatesFromIdentifier() throws Exception {
        // set up a structure with pub/exp variables
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ) + "zzzvv", "junit_test_structure_" + String.valueOf( new Date().getTime() ) + "zzzvv" );
        Field field = new Field( "JUnit Test Text", Field.FieldType.TEXT, Field.DataType.TEXT, testStructure, false, true, true, 1, false, false, false );
        FieldFactory.saveField( field );
        Field fieldPubDate = new Field( "Pub Date", Field.FieldType.DATE_TIME, Field.DataType.DATE, testStructure, false, true, true, 2, false, false, false );
        FieldFactory.saveField( fieldPubDate );
        Field fieldExpDate = new Field( "Exp Date", Field.FieldType.DATE_TIME, Field.DataType.DATE, testStructure, false, true, true, 3, false, false, false );
        FieldFactory.saveField( fieldExpDate );
        testStructure.setPublishDateVar(fieldPubDate.getVelocityVarName());
        testStructure.setExpireDateVar(fieldExpDate.getVelocityVarName());
        StructureFactory.saveStructure(testStructure);

        // some dates to play with
        Date d1=new Date();
        Date d2=new Date(d1.getTime()+60000L);
        Date d3=new Date(d2.getTime()+60000L);
        Date d4=new Date(d3.getTime()+60000L);

        // get default lang and one alternate to play with sibblings
        long deflang=APILocator.getLanguageAPI().getDefaultLanguage().getId();
        long altlang=-1;
        for(Language ll : APILocator.getLanguageAPI().getLanguages())
            if(ll.getId()!=deflang)
                altlang=ll.getId();

        // if we save using d1 & d1 then the identifier should
        // have those values after save
        Contentlet c1=new Contentlet();
        c1.setStructureInode(testStructure.getInode());
        c1.setStringProperty(field.getVelocityVarName(), "c1");
        c1.setDateProperty(fieldPubDate.getVelocityVarName(), d1);
        c1.setDateProperty(fieldExpDate.getVelocityVarName(), d2);
        c1.setLanguageId(deflang);
        c1=APILocator.getContentletAPI().checkin(c1, user, false);
        APILocator.getContentletAPI().isInodeIndexed(c1.getInode());

        Identifier ident=APILocator.getIdentifierAPI().find(c1);
        assertEquals(d1,ident.getSysPublishDate());
        assertEquals(d2,ident.getSysExpireDate());

        // if we save another language version for the same identifier
        // then the identifier should be updated with those dates d3&d4
        Contentlet c2=new Contentlet();
        c2.setStructureInode(testStructure.getInode());
        c2.setStringProperty(field.getVelocityVarName(), "c2");
        c2.setIdentifier(c1.getIdentifier());
        c2.setDateProperty(fieldPubDate.getVelocityVarName(), d3);
        c2.setDateProperty(fieldExpDate.getVelocityVarName(), d4);
        c2.setLanguageId(altlang);
        c2=APILocator.getContentletAPI().checkin(c2, user, false);
        APILocator.getContentletAPI().isInodeIndexed(c2.getInode());

        Identifier ident2=APILocator.getIdentifierAPI().find(c2);
        assertEquals(d3,ident2.getSysPublishDate());
        assertEquals(d4,ident2.getSysExpireDate());

        // the other contentlet should have the same dates if we read it again
        Contentlet c11=APILocator.getContentletAPI().find(c1.getInode(), user, false);
        assertEquals(d3,c11.getDateProperty(fieldPubDate.getVelocityVarName()));
        assertEquals(d4,c11.getDateProperty(fieldExpDate.getVelocityVarName()));

        // also it should be in the index update with the new dates
        FastDateFormat datetimeFormat = ESMappingAPIImpl.datetimeFormat;
        String q="+structureName:"+testStructure.getVelocityVarName()+
                " +inode:"+c11.getInode()+
                " +"+testStructure.getVelocityVarName()+"."+fieldPubDate.getVelocityVarName()+":"+datetimeFormat.format(d3)+
                " +"+testStructure.getVelocityVarName()+"."+fieldExpDate.getVelocityVarName()+":"+datetimeFormat.format(d4);
        assertEquals(1,APILocator.getContentletAPI().indexCount(q, user, false));
    }


    @Test
    public void rangeQuery() throws Exception {
        // https://github.com/dotCMS/dotCMS/issues/2630
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ) + "zzzvv", "junit_test_structure_" + String.valueOf( new Date().getTime() ) + "zzzvv" );
        Field field = new Field( "JUnit Test Text", Field.FieldType.TEXT, Field.DataType.TEXT, testStructure, false, true, true, 1, false, false, false );
        FieldFactory.saveField( field );

        List<Contentlet> list=new ArrayList<Contentlet>();
        String[] letters={"a","b","c","d","e","f","g"};
        for(String letter : letters) {
            Contentlet conn=new Contentlet();
            conn.setStructureInode(testStructure.getInode());
            conn.setStringProperty(field.getVelocityVarName(), letter);
            conn = contentletAPI.checkin(conn, user, false);
            contentletAPI.isInodeIndexed(conn.getInode());
            list.add(conn);
        }
        String query = "+structurename:"+testStructure.getVelocityVarName()+
                " +"+testStructure.getVelocityVarName()+"."+field.getVelocityVarName()+":[b   TO f ]";
        String sort = testStructure.getVelocityVarName()+"."+field.getVelocityVarName()+" asc";
        List<Contentlet> search = contentletAPI.search(query, 100, 0, sort, user, false);
        assertEquals(5,search.size());
        assertEquals("b",search.get(0).getStringProperty(field.getVelocityVarName()));
        assertEquals("c",search.get(1).getStringProperty(field.getVelocityVarName()));
        assertEquals("d",search.get(2).getStringProperty(field.getVelocityVarName()));
        assertEquals("e",search.get(3).getStringProperty(field.getVelocityVarName()));
        assertEquals("f",search.get(4).getStringProperty(field.getVelocityVarName()));

        contentletAPI.delete(list, user, false);
        FieldFactory.deleteField(field);
        StructureFactory.deleteStructure(testStructure);
    }

    @Test
    public void widgetInvalidateAllLang() throws Exception {

        Structure sw=StructureCache.getStructureByVelocityVarName("SimpleWidget");
        Language def=APILocator.getLanguageAPI().getDefaultLanguage();
        Contentlet w = new Contentlet();
        w.setStructureInode(sw.getInode());
        w.setStringProperty("widgetTitle", "A testing widget "+UUIDGenerator.generateUuid());
        w.setStringProperty("code", "Initial code");
        w.setLanguageId(def.getId());
        w = contentletAPI.checkin(w, user, false);
        APILocator.getVersionableAPI().setLive(w);
        APILocator.getContentletIndexAPI().addContentToIndex(w,false,true);
        contentletAPI.isInodeIndexed(w.getInode(),true);


        /*
         * For every language we should get the same content and contentMap template code
         */
        String contentEXT=Config.getStringProperty("VELOCITY_CONTENT_EXTENSION");
        VelocityEngine engine = VelocityUtil.getEngine();
        SimpleNode contentTester = engine.getRuntimeServices().parse(new StringReader("code:$code"), "tester1");

        contentTester.init(null, null);

        InvocationHandler dotInvocationHandler = new DotInvocationHandler(new HashMap());

        DotRequestProxy requestProxy = (DotRequestProxy) Proxy
                .newProxyInstance(DotRequestProxy.class.getClassLoader(),
                        new Class[] { DotRequestProxy.class },
                        dotInvocationHandler);

        DotResponseProxy responseProxy = (DotResponseProxy) Proxy
                .newProxyInstance(DotResponseProxy.class.getClassLoader(),
                        new Class[] { DotResponseProxy.class },
                        dotInvocationHandler);

        requestProxy.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        requestProxy.setAttribute(com.liferay.portal.util.WebKeys.USER,APILocator.getUserAPI().getSystemUser());

        Template teng1 = engine.getTemplate("/live/"+w.getIdentifier()+"_1."+contentEXT);
        Template tesp1 = engine.getTemplate("/live/"+w.getIdentifier()+"_2."+contentEXT);

        Context ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
        StringWriter writer=new StringWriter();
        teng1.merge(ctx, writer);
        contentTester.render(new InternalContextAdapterImpl(ctx), writer);
        assertEquals("code:Initial code",writer.toString());
        ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
        writer=new StringWriter();
        tesp1.merge(ctx, writer);
        contentTester.render(new InternalContextAdapterImpl(ctx), writer);
        assertEquals("code:Initial code",writer.toString());

        Contentlet w2=contentletAPI.checkout(w.getInode(), user, false);
        w2.setStringProperty("code", "Modified Code to make templates different");
        w2 = contentletAPI.checkin(w2, user, false);
        contentletAPI.publish(w2, user, false);
        contentletAPI.isInodeIndexed(w2.getInode(),true);

        // now if everything have been cleared correctly those should match again
        Template teng3 = engine.getTemplate("/live/"+w.getIdentifier()+"_1."+contentEXT);
        Template tesp3 = engine.getTemplate("/live/"+w.getIdentifier()+"_2."+contentEXT);
        ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
        writer=new StringWriter();
        teng3.merge(ctx, writer);
        contentTester.render(new InternalContextAdapterImpl(ctx), writer);
        assertEquals("code:Modified Code to make templates different",writer.toString());
        ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
        writer=new StringWriter();
        tesp3.merge(ctx, writer);
        contentTester.render(new InternalContextAdapterImpl(ctx), writer);
        assertEquals("code:Modified Code to make templates different",writer.toString());

        // clean up
        APILocator.getVersionableAPI().removeLive(w2.getIdentifier(), w2.getLanguageId());
        contentletAPI.archive(w2, user, false);
        contentletAPI.delete(w2, user, false);
    }

    @Test
    public void testFileCopyOnSecondLanguageVersion() throws DotDataException, DotSecurityException {

    	// Structure
        Structure testStructure = new Structure();

        testStructure.setDefaultStructure( false );
        testStructure.setDescription( "structure2709" );
        testStructure.setFixed( false );
        testStructure.setIDate( new Date() );
        testStructure.setName( "structure2709" );
        testStructure.setOwner( user.getUserId() );
        testStructure.setDetailPage( "" );
        testStructure.setStructureType( Structure.STRUCTURE_TYPE_CONTENT );
        testStructure.setType( "structure" );
        testStructure.setVelocityVarName( "structure2709" );

        StructureFactory.saveStructure( testStructure );

        Permission permissionRead = new Permission( testStructure.getInode(), APILocator.getRoleAPI().loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ );
        Permission permissionEdit = new Permission( testStructure.getInode(), APILocator.getRoleAPI().loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_EDIT );
        Permission permissionWrite = new Permission( testStructure.getInode(), APILocator.getRoleAPI().loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_WRITE );

        APILocator.getPermissionAPI().save( permissionRead, testStructure, user, false );
        APILocator.getPermissionAPI().save( permissionEdit, testStructure, user, false );
        APILocator.getPermissionAPI().save( permissionWrite, testStructure, user, false );


        // Fields

        // title
        Field title = new Field();
        title.setFieldName("testTitle2709");
        title.setFieldType(FieldType.TEXT.toString());
        title.setListed(true);
        title.setRequired(true);
        title.setSearchable(true);
        title.setStructureInode(testStructure.getInode());
        title.setType("field");
        title.setValues("");
        title.setVelocityVarName("testTitle2709");
        title.setIndexed(true);
        title.setFieldContentlet("text4");
        FieldFactory.saveField( title );

        // file
        Field file = new Field();
        file.setFieldName("testFile2709");
        file.setFieldType(FieldType.FILE.toString());
        file.setListed(true);
        file.setRequired(true);
        file.setSearchable(true);
        file.setStructureInode(testStructure.getInode());
        file.setType("field");
        file.setValues("");
        file.setVelocityVarName("testFile2709");
        file.setIndexed(true);
        file.setFieldContentlet("text1");
        FieldFactory.saveField( file );

        // ENGLISH CONTENT
        Contentlet englishContent = new Contentlet();
        englishContent.setReviewInterval( "1m" );
        englishContent.setStructureInode( testStructure.getInode() );
        englishContent.setLanguageId(1);

        List<Contentlet> files =  APILocator.getContentletAPI().search("+structureName:FileAsset", 10, -1, null, user, false);
        Contentlet fileA = files.get(0);

        contentletAPI.setContentletProperty( englishContent, title, "englishTitle2709" );
        contentletAPI.setContentletProperty( englishContent, file, fileA.getInode() );

        englishContent = contentletAPI.checkin( englishContent, null, APILocator.getPermissionAPI().getPermissions( testStructure ), user, false );

        // SPANISH CONTENT
		Contentlet spanishContent = new Contentlet();
		spanishContent.setReviewInterval("1m");
		spanishContent.setStructureInode(testStructure.getInode());
		spanishContent.setLanguageId(2);
		spanishContent.setIdentifier(englishContent.getIdentifier());

		contentletAPI.setContentletProperty( spanishContent, title, "spanishTitle2709" );
		contentletAPI.setContentletProperty( spanishContent, file, fileA.getInode() );

		spanishContent = contentletAPI.checkin( spanishContent, null, APILocator.getPermissionAPI().getPermissions( testStructure ), user, false );
		Object retrivedFile = spanishContent.get("testFile2709");
		assertTrue(retrivedFile!=null);

		APILocator.getStructureAPI().delete(testStructure, user);

    }

}