package com.dotmarketing.business;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by Nollymar Longa on 10/25/17.
 */
public class IdentifierFactoryTest {

    private static IdentifierFactory factory;
    private static Host defaultHost;
    private static Host systemHost;
    private static IdentifierCache ic;
    private static User systemUser;
    private static FolderAPI folderAPI;
    private static HostAPI hostAPI;
    private static ContentTypeAPI contentTypeAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        factory        = new IdentifierFactoryImpl();
        systemUser     = APILocator.systemUser();
        contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        folderAPI      = APILocator.getFolderAPI();
        hostAPI        = APILocator.getHostAPI();

        defaultHost = hostAPI.findDefaultHost(systemUser, false);
        systemHost  = hostAPI.findSystemHost();

        ic = CacheLocator.getIdentifierCache();
    }

    @Test
    public void testFindByURIPatternSuccessWhenInclude()
            throws DotDataException, DotSecurityException {
        final List<Identifier> identifiers = factory
                .findByURIPattern(Identifier.ASSET_TYPE_FOLDER, "/products", true,
                        defaultHost);

        Assert.assertNotNull(identifiers);
        Assert.assertFalse(identifiers.isEmpty());

        final Identifier identifier = identifiers.get(0);
        Assert.assertTrue(identifier.getId() != null && !identifier.getId().isEmpty());
        Assert.assertEquals("products", identifier.getAssetName());
        Assert.assertEquals(Identifier.ASSET_TYPE_FOLDER, identifier.getAssetType());
    }

    @Test
    public void testFindByURIPatternSuccessWhenNotInclude()
            throws DotDataException, DotSecurityException {
        final List<Identifier> identifiers = factory
                .findByURIPattern(Identifier.ASSET_TYPE_FOLDER, "/products", false,
                        defaultHost);

        Assert.assertNotNull(identifiers);
        Assert.assertFalse(identifiers.isEmpty());

        Assert.assertTrue(identifiers.size() > 1);
    }

    @Test
    public void testFindByURIFound() throws DotDataException {

        //Flush cache for this contentlet to force look up in database
        ic.removeFromCacheByURI(defaultHost.getIdentifier(), "/products");
        final Identifier identifier = factory.findByURI(defaultHost.getIdentifier(), "/products");

        Assert.assertTrue(identifier.getId() != null && !identifier.getId().isEmpty());
        Assert.assertEquals("products", identifier.getAssetName());
        Assert.assertEquals(Identifier.ASSET_TYPE_FOLDER, identifier.getAssetType());
    }

    @Test
    public void testfindByURINotFound() throws DotDataException {

        //Flush cache for this contentlet to force look up in database
        ic.removeFromCacheByURI(defaultHost.getIdentifier(), "/products");

        final Identifier identifier = factory.findByURI(systemHost.getIdentifier(), "/products");

        Assert.assertTrue(identifier.getId() != null && identifier.getId().isEmpty());
        Assert.assertNull(identifier.getAssetName());
        Assert.assertNull(identifier.getAssetType());
    }

    @Test
    public void testFindByParentPathFound() throws DotDataException {

        final List<Identifier> identifiers = factory
                .findByParentPath(defaultHost.getIdentifier(), "/blogs");

        Assert.assertTrue(identifiers != null && !identifiers.isEmpty());
        Assert.assertEquals("/blogs/", identifiers.get(0).getParentPath());
    }

    @Test
    public void testFindByParentPathNotFound() throws DotDataException {

        final List<Identifier> identifiers = factory
                .findByParentPath(systemHost.getIdentifier(), "/blogs");

        Assert.assertTrue(identifiers == null || identifiers.isEmpty());
    }

    @Test
    public void testLoadFromDbFound() throws DotDataException {
        final Identifier identifier = factory.loadFromDb(defaultHost.getIdentifier());

        Assert.assertNotNull(identifier);
        Assert.assertEquals(defaultHost.getIdentifier(), identifier.getId());

    }

    @Test(expected = DotStateException.class)
    public void testLoadFromDbWithNullID() throws DotDataException {
        factory.loadFromDb((String) null);
    }

    @Test
    public void testLoadFromDbVersionableFound() throws DotDataException {
        final Identifier identifier = factory.loadFromDb(defaultHost);

        Assert.assertNotNull(identifier);
        Assert.assertEquals(defaultHost.getIdentifier(), identifier.getId());

    }

    @Test(expected = DotStateException.class)
    public void testLoadFromDbWithNullVersionable() throws DotDataException {
        factory.loadFromDb((Versionable) null);
    }

    @Test
    public void testCreateNewFolderIdentifierForFolder()
            throws DotSecurityException, DotDataException {

        Folder newFolder, parentFolder;
        Identifier identifier;

        parentFolder = folderAPI.findFolderByPath("/products", defaultHost, systemUser, false);
        newFolder = new Folder();
        identifier = null;

        newFolder.setName("TestingFolder" + System.currentTimeMillis());

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newFolder, parentFolder);

            Assert.assertNotNull(identifier.getId());
            Assert.assertFalse(identifier.getId().isEmpty());
            Assert.assertNotNull(newFolder.getVersionId());
            Assert.assertFalse(newFolder.getVersionId().isEmpty());
            Assert.assertEquals(parentFolder.getPath(), identifier.getParentPath());
            Assert.assertEquals(Identifier.ASSET_TYPE_FOLDER, identifier.getAssetType());
            Assert.assertEquals(newFolder.getName().toLowerCase(), identifier.getAssetName());
        } finally {
            if (identifier != null) {
                //Deletes the created identifier
                deleteIdentifier(identifier);
            }
        }
    }

    @Test
    public void testCreateNewFileAssetIdentifierForFolder()
            throws DotSecurityException, DotDataException, IOException {

        Contentlet newContentlet;
        Identifier identifier;
        File tempFile;
        Folder parentFolder;
        String fileName;

        identifier = null;
        newContentlet = new FileAsset();
        fileName = "tempFile" + System.currentTimeMillis();
        tempFile = File.createTempFile(fileName, "txt");
        parentFolder = folderAPI.findFolderByPath("/products", defaultHost, systemUser, false);

        newContentlet.setContentTypeId(contentTypeAPI.find(FileAssetAPI.BINARY_FIELD).id());
        newContentlet.setBinary(FileAssetAPI.BINARY_FIELD, tempFile);
        newContentlet.setStringProperty(FileAssetAPI.HOST_FOLDER_FIELD, parentFolder.getInode());
        newContentlet.setStringProperty(FileAssetAPI.TITLE_FIELD, fileName + ".txt");
        newContentlet.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileName + ".txt");

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newContentlet, parentFolder);

            Assert.assertNotNull(identifier.getId());
            Assert.assertFalse(identifier.getId().isEmpty());
            Assert.assertNotNull(newContentlet.getVersionId());
            Assert.assertFalse(newContentlet.getVersionId().isEmpty());
            Assert.assertEquals(parentFolder.getPath(), identifier.getParentPath());
            Assert.assertEquals(Identifier.ASSET_TYPE_CONTENTLET, identifier.getAssetType());
            Assert.assertEquals(
                    newContentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD).toLowerCase(),
                    identifier.getAssetName());
        } finally {
            //Deletes the created identifier
            if (identifier != null) {
                deleteIdentifier(identifier);
            }
        }
    }

    @Test
    public void testCreateNewHtmlPageIdentifierForFolder()
            throws DotSecurityException, DotDataException {

        Contentlet newContentlet;
        Identifier identifier;
        Folder parentFolder;
        String pageName;

        identifier = null;
        newContentlet = new HTMLPageAsset();
        pageName = "tempPage" + System.currentTimeMillis();
        parentFolder = folderAPI.findFolderByPath("/products", defaultHost, systemUser, false);

        newContentlet.setContentTypeId(contentTypeAPI.find(
                        HTMLPageAssetAPI.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_VARNAME).inode());
        newContentlet.setStringProperty(HTMLPageAssetAPI.URL_FIELD, pageName);

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newContentlet, parentFolder);

            Assert.assertNotNull(identifier.getId());
            Assert.assertFalse(identifier.getId().isEmpty());
            Assert.assertNotNull(newContentlet.getVersionId());
            Assert.assertFalse(newContentlet.getVersionId().isEmpty());
            Assert.assertEquals(parentFolder.getPath(), identifier.getParentPath());
            Assert.assertEquals(Identifier.ASSET_TYPE_CONTENTLET, identifier.getAssetType());
            Assert.assertEquals(pageName.toLowerCase(), identifier.getAssetName());
        } finally {
            //Deletes the created identifier
            if (identifier != null) {
                deleteIdentifier(identifier);
            }
        }
    }

    @Test
    public void testCreateNewWebAssetIdentifierForFolder()
            throws DotSecurityException, DotDataException {

        Folder parentFolder;
        Identifier identifier;
        WebAsset newWebAsset;

        identifier = null;
        newWebAsset = new Template();
        parentFolder = folderAPI.findFolderByPath("/resources", defaultHost, systemUser, false);

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newWebAsset, parentFolder);

            Assert.assertNotNull(identifier.getId());
            Assert.assertFalse(identifier.getId().isEmpty());
            Assert.assertNotNull(newWebAsset.getVersionId());
            Assert.assertFalse(newWebAsset.getVersionId().isEmpty());
            Assert.assertEquals(parentFolder.getPath(), identifier.getParentPath());
            Assert.assertEquals("template", identifier.getAssetType());
            Assert.assertNotNull(identifier.getURI());
            Assert.assertNotNull(identifier.getAssetType());

        } finally {
            //Deletes the created identifier
            if (identifier != null) {
                deleteIdentifier(identifier);
            }
        }
    }

    @Test
    public void testCreateNewLinkIdentifierForFolder()
            throws DotSecurityException, DotDataException {

        Folder parentFolder;
        Identifier identifier;
        WebAsset newWebAsset;

        identifier = null;
        newWebAsset = new Link();
        parentFolder = folderAPI.findFolderByPath("/resources", defaultHost, systemUser, false);
        newWebAsset.setInode(UUIDGenerator.generateUuid());

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newWebAsset, parentFolder);

            Assert.assertNotNull(identifier.getId());
            Assert.assertFalse(identifier.getId().isEmpty());
            Assert.assertNotNull(newWebAsset.getVersionId());
            Assert.assertFalse(newWebAsset.getVersionId().isEmpty());
            Assert.assertEquals(parentFolder.getPath(), identifier.getParentPath());
            Assert.assertEquals("links", identifier.getAssetType());
            Assert.assertNotNull(identifier.getURI());
            Assert.assertNotNull(identifier.getAssetType());

        } finally {
            //Deletes the created identifier
            if (identifier != null) {
                deleteIdentifier(identifier);
            }
        }
    }

    @Test
    public void testCreateNewFolderIdentifierForHost()
            throws DotSecurityException, DotDataException {

        Folder newFolder;
        Identifier identifier;

        newFolder = new Folder();
        identifier = null;

        newFolder.setName("TestingFolder" + System.currentTimeMillis());

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newFolder, defaultHost);

            Assert.assertNotNull(identifier.getId());
            Assert.assertFalse(identifier.getId().isEmpty());
            Assert.assertNotNull(newFolder.getVersionId());
            Assert.assertFalse(newFolder.getVersionId().isEmpty());
            Assert.assertEquals(Identifier.ASSET_TYPE_FOLDER, identifier.getAssetType());
            Assert.assertEquals(newFolder.getName().toLowerCase(), identifier.getAssetName());
        } finally {
            if (identifier != null) {
                //Deletes the created identifier
                deleteIdentifier(identifier);
            }
        }
    }

    @Test
    public void testCreateNewFileAssetIdentifierForHost()
            throws DotSecurityException, DotDataException, IOException {

        Contentlet newContentlet;
        Identifier identifier;
        File tempFile;
        String fileName;

        identifier = null;
        newContentlet = new FileAsset();
        fileName = "tempFile" + System.currentTimeMillis();
        tempFile = File.createTempFile(fileName, "txt");

        newContentlet.setContentTypeId(contentTypeAPI.find(FileAssetAPI.BINARY_FIELD).inode());
        newContentlet.setBinary(FileAssetAPI.BINARY_FIELD, tempFile);
        newContentlet.setStringProperty(FileAssetAPI.TITLE_FIELD, fileName + ".txt");
        newContentlet.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileName + ".txt");

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newContentlet, defaultHost);

            Assert.assertNotNull(identifier.getId());
            Assert.assertFalse(identifier.getId().isEmpty());
            Assert.assertNotNull(newContentlet.getVersionId());
            Assert.assertFalse(newContentlet.getVersionId().isEmpty());
            Assert.assertEquals(Identifier.ASSET_TYPE_CONTENTLET, identifier.getAssetType());
            Assert.assertEquals(
                    newContentlet.getBinary(FileAssetAPI.BINARY_FIELD).getName().toLowerCase(),
                    identifier.getAssetName());
        } finally {
            //Deletes the created identifier
            if (identifier != null) {
                deleteIdentifier(identifier);
            }
        }
    }

    @Test
    public void testCreateNewHtmlPageIdentifierForHost()
            throws DotSecurityException, DotDataException {

        Contentlet newContentlet;
        Identifier identifier;
        String pageName;

        identifier = null;
        newContentlet = new HTMLPageAsset();
        pageName = "tempPage" + System.currentTimeMillis();

        newContentlet.setContentTypeId(contentTypeAPI.find(
                        HTMLPageAssetAPI.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_VARNAME).inode());
        newContentlet.setStringProperty(HTMLPageAssetAPI.URL_FIELD, pageName);

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newContentlet, defaultHost);

            Assert.assertNotNull(identifier.getId());
            Assert.assertFalse(identifier.getId().isEmpty());
            Assert.assertNotNull(newContentlet.getVersionId());
            Assert.assertFalse(newContentlet.getVersionId().isEmpty());
            Assert.assertEquals(Identifier.ASSET_TYPE_CONTENTLET, identifier.getAssetType());
            Assert.assertEquals(pageName.toLowerCase(), identifier.getAssetName());
        } finally {
            //Deletes the created identifier
            if (identifier != null) {
                deleteIdentifier(identifier);
            }
        }
    }

    @Test
    public void testCreateNewHostIdentifierForHost()
            throws DotSecurityException, DotDataException {

        Contentlet newContentlet;
        Identifier identifier;

        identifier = null;
        newContentlet = new Host();
        newContentlet.setInode(UUIDGenerator.generateUuid());

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newContentlet, defaultHost);

            Assert.assertNotNull(identifier.getId());
            Assert.assertFalse(identifier.getId().isEmpty());
            Assert.assertNotNull(newContentlet.getVersionId());
            Assert.assertFalse(newContentlet.getVersionId().isEmpty());
            Assert.assertEquals(Identifier.ASSET_TYPE_CONTENTLET, identifier.getAssetType());

        } finally {
            //Deletes the created identifier
            if (identifier != null) {
                deleteIdentifier(identifier);
            }
        }
    }


    @Test
    public void testLoadAllIdentifiers() throws DotDataException {
        final List<Identifier> identifiers = factory.loadAllIdentifiers();

        Assert.assertNotNull(identifiers);
        Assert.assertTrue(identifiers.size() > 0);
    }

    @Test
    public void testIsIdentifierFound() {
        final boolean found = factory.isIdentifier(defaultHost.getIdentifier());

        Assert.assertTrue(found);
    }

    @Test
    public void testIsIdentifierWhenNull() {
        final boolean found = factory.isIdentifier(null);

        Assert.assertFalse(found);
    }

    @Test
    public void testGetAssetTypeFromDB() throws DotDataException {
       final String assetType = factory.getAssetTypeFromDB(defaultHost.getIdentifier());

        Assert.assertNotNull(assetType);
        Assert.assertEquals("contentlet", assetType);

    }

    @Test
    public void testGetAssetTypeFromDBWhenNull() throws DotDataException {
        final String assetType = factory.getAssetTypeFromDB(null);

        Assert.assertNull(assetType);

    }

    private void deleteIdentifier(Identifier identifier) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL("delete from identifier where id = ?");
        db.addParam(identifier.getId());
        db.loadResult();
    }

}
