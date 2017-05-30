package com.dotmarketing.portlets.contentlet.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.junit.Ignore;
import org.junit.Test;

import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.StructureDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.mock.request.MockInternalRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.apache.commons.lang.time.FastDateFormat;
import com.dotcms.repackage.twitter4j.IDs;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

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
        List<String> inodes = new ArrayList<>();
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

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Copy the test contentlet
        Contentlet copyContentlet = contentletAPI.copyContentlet( contentlet, user, false );

        //validations
        assertTrue( copyContentlet != null && !copyContentlet.getInode().isEmpty() );
        assertEquals(copyContentlet.getStructureInode(), contentlet.getStructureInode());
        assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
        assertEquals( copyContentlet.getHost(), contentlet.getHost() );

        contentletAPI.delete(copyContentlet, user, false);
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

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Getting the folder of the test contentlet
        Folder folder = APILocator.getFolderAPI().find( contentlet.getFolder(), user, false );

        //Copy the test contentlet
        Contentlet copyContentlet = contentletAPI.copyContentlet( contentlet, folder, user, false );

        //validations
        assertTrue( copyContentlet != null && !copyContentlet.getInode().isEmpty() );
        assertEquals(copyContentlet.getStructureInode(), contentlet.getStructureInode());
        assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
        assertEquals( copyContentlet.get("junitTestWysiwyg"), contentlet.get("junitTestWysiwyg") );

        contentletAPI.delete(copyContentlet, user, false);
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

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Copy the test contentlet
        Contentlet copyContentlet = contentletAPI.copyContentlet( contentlet, defaultHost, user, false );

        //validations
        assertTrue( copyContentlet != null && !copyContentlet.getInode().isEmpty() );
        assertEquals( copyContentlet.getStructureInode(), contentlet.getStructureInode() );
        assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
        assertEquals( copyContentlet.get( "junitTestWysiwyg" ), contentlet.get( "junitTestWysiwyg" ) );
        assertEquals( copyContentlet.getHost(), contentlet.getHost() );

        contentletAPI.delete( copyContentlet, user, false );
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

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Getting the folder of the test contentlet
        Folder folder = APILocator.getFolderAPI().find( contentlet.getFolder(), user, false );

        //Copy the test contentlet
        Contentlet copyContentlet = contentletAPI.copyContentlet( contentlet, folder, user, true, false );

        //validations
        assertTrue( copyContentlet != null && !copyContentlet.getInode().isEmpty() );
        assertEquals( copyContentlet.getStructureInode(), contentlet.getStructureInode() );
        assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
        assertEquals( copyContentlet.get( "junitTestWysiwyg" ), contentlet.get( "junitTestWysiwyg" ) );

        contentletAPI.delete( copyContentlet, user, false );
    }
    
    @Test
    public void copyContentletWithSeveralVersionsOrderIssue() throws Exception {
        long defLang=APILocator.getLanguageAPI().getDefaultLanguage().getId();
        
        Host host1=new Host();
        host1.setHostname("copy.contentlet.t1_"+System.currentTimeMillis());
        host1.setDefault(false);
        host1.setLanguageId(defLang);
        host1 = APILocator.getHostAPI().save(host1, user, false);
        contentletAPI.isInodeIndexed(host1.getInode());
        
        Host host2=new Host();
        host2.setHostname("copy.contentlet.t2_"+System.currentTimeMillis());
        host2.setDefault(false);
        host2.setLanguageId(defLang);
        host2 = APILocator.getHostAPI().save(host2, user, false);
        contentletAPI.isInodeIndexed(host2.getInode());
        
        
        java.io.File bin=new java.io.File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + java.io.File.separator
                + UUIDGenerator.generateUuid() + java.io.File.separator + "hello.txt");
        bin.getParentFile().mkdirs();
        
        bin.createNewFile();
        FileUtils.writeStringToFile(bin, "this is the content of the file");
        
        Contentlet file=new Contentlet();
        file.setHost(host1.getIdentifier());
        file.setFolder("SYSTEM_FOLDER");
        file.setStructureInode(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("FileAsset").getInode());
        file.setLanguageId(defLang);
        file.setStringProperty(FileAssetAPI.TITLE_FIELD,"test copy");
        file.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, "hello.txt");
        file.setBinary(FileAssetAPI.BINARY_FIELD, bin);
        file = contentletAPI.checkin(file, user, false);
        contentletAPI.isInodeIndexed(file.getInode());
        final String ident = file.getIdentifier();
        
        // create 20 versions
        for(int i=1; i<=20; i++) {
            file = contentletAPI.findContentletByIdentifier(ident, false, defLang, user, false);
            APILocator.getFileAssetAPI().renameFile(file, "hello"+i, user, false);
            contentletAPI.isInodeIndexed(APILocator.getVersionableAPI().getContentletVersionInfo(ident, defLang).getWorkingInode());
        }
        
        file = contentletAPI.findContentletByIdentifier(ident, false, defLang, user, false);
        
        // the issue https://github.com/dotCMS/dotCMS/issues/5007 is caused by an order issue
        // when we call copy it saves all the versions in the new location. but it should
        // do it in older-to-newer order. because the last save will be the asset_name in the
        // identifier and the data that will have the "working" (or live) version.
        Contentlet copy = contentletAPI.copyContentlet(file, host2, user, false);
        Identifier copyIdent = APILocator.getIdentifierAPI().find(copy);
        
        copy = contentletAPI.findContentletByIdentifier(copyIdent.getId(), false, defLang, user, false);
        
        assertEquals("hello20.txt",copyIdent.getAssetName());
        assertEquals("hello20.txt",copy.getStringProperty(FileAssetAPI.FILE_NAME_FIELD));
        assertEquals("hello20.txt",copy.getBinary(FileAssetAPI.BINARY_FIELD).getName());
        assertEquals("this is the content of the file", FileUtils.readFileToString(copy.getBinary(FileAssetAPI.BINARY_FIELD)));
        
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
        List<MultiTree> multiTrees = MultiTreeFactory.getMultiTreeByChild( identifier.getId() );
        for ( MultiTree multitree : multiTrees ) {
            //Get the Identifiers of the related pages
            Identifier htmlPageIdentifier = APILocator.getIdentifierAPI().find( multitree.getParent1() );

            //OK..., lets try to find this page in the cache...
            HTMLPageAsset foundPage = ( HTMLPageAsset ) CacheLocator.getCacheAdministrator().get( "HTMLPageCache" + identifier, "HTMLPageCache" );

            //Validations
            assertTrue( foundPage == null || ( foundPage.getInode() == null || foundPage.getInode().equals( "" ) ) );
        }
    }

    /**
     * Testing {@link ContentletAPI#cleanField(com.dotmarketing.portlets.structure.model.Structure, com.dotmarketing.portlets.structure.model.Field, com.liferay.portal.model.User, boolean)}
     * with a binary field
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void cleanBinaryField() throws DotDataException, DotSecurityException {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        Long identifier = uniqueIdentifier.get(structure.getName());

        //Search the contentlet for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );
        Contentlet contentlet = contentletList.iterator().next();

        //Getting a known binary field for this structure
        //TODO: The definition of the method getFieldByName receive a parameter named "String:structureType", some examples I saw send the Inode, but actually what it needs is the structure name....

        Field foundBinaryField = FieldFactory.getFieldByVariableName( structure.getInode(), "junitTestBinary" + identifier );


        //Getting the current value for this field
        Object value = contentletAPI.getFieldValue( contentlet, foundBinaryField );

        //Validations
        assertNotNull( value );
        assertTrue( ((java.io.File) value).exists() );

        //Cleaning the binary field
        contentletAPI.cleanField( structure, foundBinaryField, user, false );

        //Validations
        assertFalse( ((java.io.File) value).exists() );
    }

    /**
     * Testing {@link ContentletAPI#cleanField(com.dotmarketing.portlets.structure.model.Structure, com.dotmarketing.portlets.structure.model.Field, com.liferay.portal.model.User, boolean)}
     * with a tag field
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void cleanTagField() throws DotDataException, DotSecurityException {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        Long identifier = uniqueIdentifier.get(structure.getName());

        //Search the contentlet for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );
        Contentlet contentlet = contentletList.iterator().next();

        //Getting a known tag field for this structure
        //TODO: The definition of the method getFieldByName receive a parameter named "String:structureType", some examples I saw send the Inode, but actually what it needs is the structure name....
        Field foundTagField = FieldFactory.getFieldByVariableName( structure.getInode(), "junitTestTag" + identifier );

        //Getting the current value for this field
        List<Tag> value = tagAPI.getTagsByInodeAndFieldVarName(contentlet.getInode(), foundTagField.getVelocityVarName());

        //Validations
        assertNotNull( value );
        assertFalse( value.isEmpty() );

        //Cleaning the tag field
        contentletAPI.cleanField( structure, foundTagField, user, false );

        //Getting the current value for this field
        List<Tag> value2 = tagAPI.getTagsByInodeAndFieldVarName(contentlet.getInode(), foundTagField.getVelocityVarName());

        //Validations
        assertTrue( value2.isEmpty() );        
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
     * Tests method {@link ContentletAPI#getContentletReferences(Contentlet, User, boolean)}.
     * <p>
     * Checks that expected containers and pages (in the correct language) are returned by the method.
     */

    @Test
    public void getContentletReferences() throws Exception {
        int english = 1;
        int spanish = 2;

        try {
            HibernateUtil.startTransaction();
            Structure structure = new StructureDataGen().nextPersisted();
            Container container = new ContainerDataGen().withStructure(structure, "").nextPersisted();
            Template template = new TemplateDataGen().withContainer(container).nextPersisted();
            Folder folder = new FolderDataGen().nextPersisted();

            HTMLPageDataGen htmlPageDataGen = new HTMLPageDataGen(folder, template);
            HTMLPageAsset englishPage = htmlPageDataGen.languageId(english).nextPersisted();
            HTMLPageAsset spanishPage = htmlPageDataGen.pageURL(englishPage.getPageUrl() + "SP").languageId(spanish)
                .nextPersisted();

            ContentletDataGen contentletDataGen = new ContentletDataGen(structure.getInode());
            Contentlet contentInEnglish = contentletDataGen.languageId(english).nextPersisted();
            Contentlet contentInSpanish = contentletDataGen.languageId(spanish).nextPersisted();

            // let's add the content to the page in english (create the page-container-content relationship)
            MultiTree multiTreeEN = new MultiTree(englishPage.getIdentifier(), container.getIdentifier(),
                contentInEnglish.getIdentifier());
            MultiTreeFactory.saveMultiTree(multiTreeEN, english);

            // let's add the content to the page in spanish (create the page-container-content relationship)
            MultiTree multiTreeSP = new MultiTree(spanishPage.getIdentifier(), container.getIdentifier(),
                contentInSpanish.getIdentifier());
            MultiTreeFactory.saveMultiTree(multiTreeSP, spanish);

            // let's get the references for english content
            List<Map<String, Object>> references = contentletAPI.getContentletReferences(contentInEnglish, user, false);

            assertNotNull(references);
            assertTrue(!references.isEmpty());
            // let's check if the referenced page is in the expected language
            assertEquals(((IHTMLPage) references.get(0).get("page")).getLanguageId(), english);
            // let's check the referenced container is the expected
            assertEquals(((Container) references.get(0).get("container")).getInode(), container.getInode());

            // let's get the references for spanish content
            references = contentletAPI.getContentletReferences(contentInSpanish, user, false);

            assertNotNull(references);
            assertTrue(!references.isEmpty());
            // let's check if the referenced page is in the expected language
            assertEquals(((IHTMLPage) references.get(0).get("page")).getLanguageId(), spanish);
            // let's check the referenced container is the expected
            assertEquals(((Container) references.get(0).get("container")).getInode(), container.getInode());

            ContentletDataGen.remove(contentInEnglish);
            ContentletDataGen.remove(contentInSpanish);
            HTMLPageDataGen.remove(englishPage);
            HTMLPageDataGen.remove(spanishPage);
            TemplateDataGen.remove(template);
            ContainerDataGen.remove(container);
            StructureDataGen.remove(structure);
            FolderDataGen.remove(folder);

            HibernateUtil.commitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            throw e;
        }
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

        Long identifier = uniqueIdentifier.get(structure.getName());

        //Getting a know field for this structure
        //TODO: The definition of the method getFieldByName receive a parameter named "String:structureType", some examples I saw send the Inode, but actually what it needs is the structure name....
        Field foundWysiwygField = FieldFactory.getFieldByVariableName( structure.getInode(), "junitTestWysiwyg" + identifier );

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

        String RELATION_TYPE = new Link().getType();

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Create a menu link
        Link menuLink = createMenuLink();

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
        Tree tree = TreeFactory.getTree( contentlet.getInode(), menuLinkIdentifier.getId(), RELATION_TYPE );

        //Validations
        assertNotNull( tree );
        assertNotNull( tree.getParent() );
        assertNotNull( tree.getChild() );
        assertEquals( tree.getParent(), contentlet.getInode() );
        assertEquals( tree.getChild(), menuLinkIdentifier.getId() );
        assertEquals( tree.getRelationType(), RELATION_TYPE );
        
        try{
        	HibernateUtil.startTransaction();
            menuLinkAPI.delete( menuLink, user, false );
        	HibernateUtil.commitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(ContentletAPITest.class, e.getMessage());
        }


    }

    /**
     * Testing {@link ContentletAPI#findPageContentlets(String, String, String, boolean, long, com.liferay.portal.model.User, boolean)}
     *
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
            List<MultiTree> multiTrees = MultiTreeFactory.getMultiTreeByChild( identifier.getId() );
            if ( multiTrees != null && !multiTrees.isEmpty() ) {

                for ( MultiTree multiTree : multiTrees ) {

                    //Getting the identifiers
                    Identifier htmlPageIdentifier = APILocator.getIdentifierAPI().find( multiTree.getParent1() );
                    Identifier containerPageIdentifier = APILocator.getIdentifierAPI().find( multiTree.getParent2() );

                    //Find the related contentlets, at this point should return something....
                    List<Contentlet> pageContentlets = contentletAPI.findPageContentlets( htmlPageIdentifier.getId(), containerPageIdentifier.getId(), null, true, -1, user, false );

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
        List<Contentlet> contentRelationships = new ArrayList<>();
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
        st.setStructureType(BaseContentType.CONTENT.getType());
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
     * https://github.com/dotCMS/core/issues/11716
     * @throws DotDataException
     * @throws DotSecurityException
     */

    @Test
    public void addRemoveContentFromIndex () throws DotDataException, DotSecurityException {
   // respect CMS Anonymous permissions
      boolean respectFrontendRoles = false;
      int num = 5;
      Host host = APILocator.getHostAPI().findDefaultHost(user, respectFrontendRoles);
      Folder folder = APILocator.getFolderAPI().findSystemFolder();

      Language lang = APILocator.getLanguageAPI().getDefaultLanguage();
      ContentType type = APILocator.getContentTypeAPI(user).find("webPageContent");
      List<Contentlet> origCons = new ArrayList<>();
      
      Map map = new HashMap<>();
      map.put("stInode", type.id());
      map.put("host", host.getIdentifier());
      map.put("folder", folder.getInode()); 
      map.put("languageId", lang.getId());
      map.put("sortOrder", new Long(0));
      map.put("body", "body");

      
      //add 5 contentlets
      for(int i = 0;i<num;i++){
        map.put("title", i+ "my test title");
        
        // create a new piece of content backed by the map created above
        Contentlet content = new Contentlet(map);
    
        // check in the content
        content= contentletAPI.checkin(content,user, respectFrontendRoles);
        
        assertTrue( content.getIdentifier()!=null );
        assertTrue( content.isWorking());
        assertFalse( content.isLive());
        // publish the content
        contentletAPI.publish(content, user, respectFrontendRoles);
        assertTrue( content.isLive());
        origCons.add(content);
      }
      
      
      //commit it index
      HibernateUtil.closeSession();
      for(Contentlet c : origCons){
        assertTrue(contentletAPI.indexCount("+live:true +identifier:" +c.getIdentifier() + " +inode:" + c.getInode() , user, respectFrontendRoles)>0);
      }
      
      
      HibernateUtil.startTransaction();
      try{
        List<Contentlet> checkedOut=contentletAPI.checkout(origCons, user, respectFrontendRoles);
        for(Contentlet c : checkedOut){
          c.setStringProperty("title", c.getStringProperty("title") + " new");
          c = contentletAPI.checkin(c,user, respectFrontendRoles);
          contentletAPI.publish(c, user, respectFrontendRoles);
          assertTrue( c.isLive());
        }
        throw new DotDataException("uh oh, what happened?");
      }
      catch(DotDataException e){
        HibernateUtil.rollbackTransaction();
        
      }
      finally{
        HibernateUtil.closeSession();
      }
      for(Contentlet c : origCons){
        assertTrue(contentletAPI.indexCount("+live:true +identifier:" +c.getIdentifier() + " +inode:" + c.getInode() , user, respectFrontendRoles)>0);
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
        contentletAPI.archive(newContentlet, user, false);
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

        Language language = APILocator.getLanguageAPI().getDefaultLanguage();

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new contentlet
        Contentlet newContentlet = createContentlet( testStructure, language, false );

        // new inode to create a new version
        String newInode=UUIDGenerator.generateUuid();
        newContentlet.setInode(newInode);


        List<Language> languages = APILocator.getLanguageAPI().getLanguages();
        for ( Language localLanguage : languages ) {
            if ( localLanguage.getId() != language.getId() ) {
                language = localLanguage;
                break;
            }
        }

        newContentlet.setLanguageId(language.getId());

        newContentlet = contentletAPI.checkin(newContentlet, user, false);

        //Now we need to delete it
        contentletAPI.archive(newContentlet, user, false);
        contentletAPI.delete( newContentlet, user, false, true );

        //Try to find the deleted Contentlet
        Identifier contentletIdentifier = APILocator.getIdentifierAPI().find( newContentlet.getIdentifier() );
        List<Contentlet> foundContentlets = contentletAPI.findAllVersions(contentletIdentifier, user, false );

        //Validations
        assertTrue( foundContentlets == null || foundContentlets.isEmpty() );
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

        contentletAPI.publish(contentlets, user, false);

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
        List<Contentlet> testContentlets = new ArrayList<>();
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
        contentletAPI.archive(newContentlet, user, false);
        List<Contentlet> testContentlets = new ArrayList<>();
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

        Language language = APILocator.getLanguageAPI().getDefaultLanguage();

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Create a new contentlet with one version
        Contentlet newContentlet1 = createContentlet( testStructure, language, false );

        //Create a new contentlet with two versions
        Contentlet newContentlet2 = createContentlet( testStructure, language, false );

        // new inode to create the second version
        String newInode=UUIDGenerator.generateUuid();
        newContentlet2.setInode(newInode);

        List<Language> languages = APILocator.getLanguageAPI().getLanguages();
        for ( Language localLanguage : languages ) {
            if ( localLanguage.getId() != language.getId() ) {
                language = localLanguage;
                break;
            }
        }

        newContentlet2.setLanguageId(language.getId());

        newContentlet2 = contentletAPI.checkin(newContentlet2, user, false);


        //Now test this delete
        List<Contentlet> testContentlets = new ArrayList<>();
        testContentlets.add( newContentlet1 );
        testContentlets.add( newContentlet2 );
        contentletAPI.delete( testContentlets, user, false, true );

        //Try to find the deleted Contentlets
        Identifier contentletIdentifier = APILocator.getIdentifierAPI().find( newContentlet1.getIdentifier() );
        List<Contentlet> foundContentlets = contentletAPI.findAllVersions(contentletIdentifier, user, false );

        //Validations for newContentlet1
        assertTrue( foundContentlets == null || foundContentlets.isEmpty() );

        contentletIdentifier = APILocator.getIdentifierAPI().find( newContentlet2.getIdentifier() );
        foundContentlets = contentletAPI.findAllVersions(contentletIdentifier, user, false );

        //Validations for newContentlet2
        assertTrue( foundContentlets == null || foundContentlets.isEmpty() );
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
        List<Contentlet> contentRelationships = new ArrayList<>();
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
        List<Contentlet> contentRelationships = new ArrayList<>();
        contentRelationships.add( childContentlet );
        ContentletRelationships contentletRelationships = createContentletRelationships( testRelationship, parentContentlet, testStructure, contentRelationships );

        //Relate contents to our test contentlet
        for ( ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords : contentletRelationships.getRelationshipsRecords() ) {
            contentletAPI.relateContent( parentContentlet, contentletRelationshipRecords, user, false );
        }

        Boolean hasParent = FactoryLocator.getRelationshipFactory().isParent( testRelationship, parentContentlet.getStructure() );

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
        List<Contentlet> contentRelationships = new ArrayList<>();
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
        List<Contentlet> contentRelationships = new ArrayList<>();
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
        List<Contentlet> contentRelationships = new ArrayList<>();
        contentRelationships.add( childContentlet );

        //Relate the content
        contentletAPI.relateContent( parentContentlet, testRelationship, contentRelationships, user, false );

        //Try to find the related Contentlet
        //List<Contentlet> foundContentlets = contentletAPI.getRelatedContent( parentContentlet, testRelationship, user, false );

        List<Relationship> relationships = FactoryLocator.getRelationshipFactory().byContentType( parentContentlet.getStructure() );
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
        List<Contentlet> contentRelationships = new ArrayList<>();
        contentRelationships.add( childContentlet );

        //Relate the content
        contentletAPI.relateContent( parentContentlet, testRelationship, contentRelationships, user, false );

        Boolean hasParent = FactoryLocator.getRelationshipFactory().isParent( testRelationship, parentContentlet.getStructure() );

        List<Relationship> relationships = FactoryLocator.getRelationshipFactory().byContentType( parentContentlet.getStructure() );
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

        Thread.sleep(2000); // wait a bit for the index
        
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
        field = FieldFactory.saveField( field );

        List<Contentlet> list=new ArrayList<>();
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
        APILocator.getStructureAPI().delete(testStructure, user);
    }

    @Test
    public void widgetInvalidateAllLang() throws Exception {

        HttpServletRequest requestProxy = new MockInternalRequest().request();
        HttpServletResponse responseProxy = new BaseResponse().response();

        initMessages();

        Structure sw=CacheLocator.getContentTypeCache().getStructureByVelocityVarName("SimpleWidget");
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
        String contentEXT=Config.getStringProperty("VELOCITY_CONTENT_EXTENSION", "content");
        VelocityEngine engine = VelocityUtil.getEngine();
        SimpleNode contentTester = engine.getRuntimeServices().parse(new StringReader("code:$code"), "tester1");

        contentTester.init(null, null);

        requestProxy.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        requestProxy.setAttribute(com.liferay.portal.util.WebKeys.USER,APILocator.getUserAPI().getSystemUser());

        org.apache.velocity.Template teng1 = engine.getTemplate("/live/"+w.getIdentifier()+"_1."+contentEXT);
        org.apache.velocity.Template tesp1 = engine.getTemplate("/live/"+w.getIdentifier()+"_2."+contentEXT);

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
        org.apache.velocity.Template teng3 = engine.getTemplate("/live/"+w.getIdentifier()+"_1."+contentEXT);
        org.apache.velocity.Template tesp3 = engine.getTemplate("/live/"+w.getIdentifier()+"_2."+contentEXT);
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
        APILocator.getVersionableAPI().removeLive(w2);
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
        testStructure.setStructureType( BaseContentType.CONTENT.getType() );
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
        try{
        	HibernateUtil.startTransaction();
        	APILocator.getStructureAPI().delete(testStructure, user);
        	HibernateUtil.commitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(ContentletAPITest.class, e.getMessage());
        }


    }

    @Test
    public void newFileAssetLanguageDifferentThanDefault() throws DotSecurityException, DotDataException, IOException {
        int spanish = 2;
        Folder folder = APILocator.getFolderAPI().findSystemFolder();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");

        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder, file);
        Contentlet fileInSpanish = fileAssetDataGen.languageId(spanish).nextPersisted();
        Contentlet
            result =
            contentletAPI.findContentletByIdentifier(fileInSpanish.getIdentifier(), false, spanish, user, false);
        assertEquals(fileInSpanish.getInode(), result.getInode());

        fileAssetDataGen.remove(fileInSpanish);
    }
    
    @Test
    public void newVersionFileAssetLanguageDifferentThanDefault() throws DotDataException, IOException, DotSecurityException{
    	int english = 1;
    	int spanish = 2;
    	
    	Folder folder = APILocator.getFolderAPI().findSystemFolder();
    	java.io.File file = java.io.File.createTempFile("file", ".txt");
        FileUtil.write(file, "helloworld");
        
    	FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder,file);
    	Contentlet fileAsset = fileAssetDataGen.languageId(english).nextPersisted();
  	  
    	Contentlet resultEnglish = contentletAPI.findContentletByIdentifier(fileAsset.getIdentifier(), false, english, user, false);
  	  
    	Contentlet contentletSpanish = contentletAPI.findContentletByIdentifier(fileAsset.getIdentifier(), false, english, user, false);
    	contentletSpanish.setInode("");
    	contentletSpanish.setLanguageId(spanish);
    	contentletSpanish = contentletAPI.checkin(contentletSpanish, user, false);
  	  
    	Contentlet resultSpanish = contentletAPI.findContentletByIdentifier(fileAsset.getIdentifier(), false, spanish, user, false);
    	assertNotNull( resultSpanish );
    	
    	fileAssetDataGen.remove(resultSpanish);
    }
    
    /**
     * Deletes a list of contents
     * @throws Exception
     */
    @Test
    public void deleteMultipleContents() throws Exception { // https://github.com/dotCMS/core/issues/7678

    	// languages
    	int english = 1;
    	int spanish = 2;
        //Using System User.
        User user = APILocator.getUserAPI().getSystemUser();
        // new template
        Template template = new TemplateDataGen().nextPersisted();
        // new test folder
		Folder testFolder = new FolderDataGen().nextPersisted();
		// sample pages
		HTMLPageAsset pageEnglish1 = new HTMLPageDataGen(testFolder, template).languageId(english).nextPersisted();
		HTMLPageAsset pageEnglish2 = new HTMLPageDataGen(testFolder, template).languageId(english).nextPersisted();
		contentletAPI.publish(pageEnglish1, user, false);
		contentletAPI.publish(pageEnglish2, user, false);
        // delete counter
        int deleted = 0;
        // Page list
        List<HTMLPageAsset> liveHTMLPages = new ArrayList<HTMLPageAsset>();
        // List of contentlets created for this test.
        List<Contentlet> contentletsCreated = new ArrayList<Contentlet>();
        
        liveHTMLPages.add(pageEnglish1);
        liveHTMLPages.add(pageEnglish2);
               
        //We need to create a new copy of pages for Spanish.
        for(HTMLPageAsset liveHTMLPage : liveHTMLPages){
            Contentlet htmlPageContentlet = APILocator.getContentletAPI().find( liveHTMLPage.getInode(), user, false );

            //As a copy we need to remove this info to do a clean checkin.
            htmlPageContentlet.getMap().remove("modDate");
            htmlPageContentlet.getMap().remove("lastReview");
            htmlPageContentlet.getMap().remove("owner");
            htmlPageContentlet.getMap().remove("modUser");

            htmlPageContentlet.getMap().put("inode", "");
            htmlPageContentlet.getMap().put("languageId", new Long(spanish));

            //Checkin and Publish.
            Contentlet working = APILocator.getContentletAPI().checkin(htmlPageContentlet, user, false);
            APILocator.getContentletAPI().publish(working, user, false);
            APILocator.getContentletAPI().isInodeIndexed(working.getInode(), true);

            contentletsCreated.add(working);
        }

        //Now remove all the pages that we created for this tests.
        APILocator.getContentletAPI().unpublish(contentletsCreated, user, false);
        APILocator.getContentletAPI().archive(contentletsCreated, user, false);
        APILocator.getContentletAPI().delete(contentletsCreated, user, false);
        
        for(Contentlet contentlet: contentletsCreated){
        	if(APILocator.getContentletAPI().find(contentlet.getInode(), user, false) == null){
        		deleted++;
        	}
        }
        // 2 Spanish pages created, 2 should have been deleted
        assertEquals(2, deleted);
        
        List<Contentlet> liveEnglish = new ArrayList<Contentlet>();
        for(IHTMLPage page:liveHTMLPages){
        	liveEnglish.add(APILocator.getContentletAPI().find( page.getInode(), user, false ));
        }
        
        APILocator.getContentletAPI().unpublish(liveEnglish, user, false);
        APILocator.getContentletAPI().archive(liveEnglish, user, false);
        APILocator.getContentletAPI().delete(liveEnglish, user, false);
        
        deleted = 0;
        for(Contentlet contentlet: liveEnglish){
        	if(APILocator.getContentletAPI().find(contentlet.getInode(), user, false) == null){
        		deleted++;
        	}
        }
        
        // 2 English pages created, 2 should have been deleted
        assertEquals(2, deleted);

        // dispose other objects
		FolderDataGen.remove(testFolder);
		TemplateDataGen.remove(template);
		
    }

    /**
     * This JUnit is to check the fix on Issue 10797 (https://github.com/dotCMS/core/issues/10797)
     * It executes the following:
     * 1) create a new structure
     * 2) create a new field
     * 3) create a contentlet
     * 4) set the contentlet property
     * 5) check the contentlet
     * 6) deletes it all in the end
     *
     * @throws Exception Any exception that may happen
     */
    @Test
    public void test_validateContentlet_contentWithTabDividerField() throws Exception {
        Structure testStructure = null;
        Field tabDividerField = null;

        try {
            // Create test structure
            testStructure = createStructure("Tab Divider Test Structure_" + String.valueOf(new Date().getTime()) + "tab_divider", "tab_divider_test_structure_" + String.valueOf(new Date().getTime()) + "tab_divider");

            // Create tab divider field
            tabDividerField = new Field("JUnit Test TabDividerField", FieldType.TAB_DIVIDER, Field.DataType.SECTION_DIVIDER, testStructure, false, true, true, 1, false, false, false);
            tabDividerField = FieldFactory.saveField(tabDividerField);

            // Create the test contentlet
            Contentlet testContentlet = new Contentlet();
            testContentlet.setStructureInode(testStructure.getInode());

            // Set the contentlet property
            contentletAPI.setContentletProperty(testContentlet, tabDividerField, "tabDividerFieldValue");

            // Checking the contentlet
            testContentlet = contentletAPI.checkin(testContentlet, user, false);
            contentletAPI.isInodeIndexed(testContentlet.getInode());
        } catch (Exception ex) {
            Logger.error(this, "An error occurred during test_validateContentlet_contentWithTabDividerField", ex);
            throw ex;
        } finally {
            // Delete field
            FieldFactory.deleteField(tabDividerField);

            // Delete structure
            APILocator.getStructureAPI().delete(testStructure, user);
        }
    }

}