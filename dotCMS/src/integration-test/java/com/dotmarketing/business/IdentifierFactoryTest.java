package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by Nollymar Longa on 10/25/17.
 */
public class IdentifierFactoryTest {

    private static final String TEMP_FILE = "tempFile";
    public static final String TXT = "txt";
    public static final String DOT_TXT = ".txt";
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

        Contentlet fileAsset = TestDataUtils.getFileAssetContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());
        Folder assetFolder = APILocator.getFolderAPI()
                .find(fileAsset.getFolder(), APILocator.systemUser(), false);
        String folderPath = assetFolder.getPath();
        if (folderPath.endsWith("/")) {//Removing trailing /
            folderPath = folderPath.substring(0, folderPath.length() - 1);
        }

        final List<Identifier> identifiers = factory
                .findByURIPattern(Identifier.ASSET_TYPE_FOLDER, folderPath, true,
                        defaultHost);

        assertNotNull(identifiers);
        assertFalse(identifiers.isEmpty());

        final Identifier identifier = identifiers.get(0);
        assertTrue(identifier.getId() != null && !identifier.getId().isEmpty());
        assertEquals(assetFolder.getName(), identifier.getAssetName());
        assertEquals(Identifier.ASSET_TYPE_FOLDER, identifier.getAssetType());
    }

    @Test
    public void testFindByURIPatternSuccessWhenNotInclude()
            throws DotDataException, DotSecurityException {

        Contentlet fileAsset = TestDataUtils.getFileAssetContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());
        Folder assetFolder = APILocator.getFolderAPI()
                .find(fileAsset.getFolder(), APILocator.systemUser(), false);
        String folderPath = assetFolder.getPath();
        if (folderPath.endsWith("/")) {//Removing trailing /
            folderPath = folderPath.substring(0, folderPath.length() - 1);
        }

        final List<Identifier> identifiers = factory
                .findByURIPattern(Identifier.ASSET_TYPE_FOLDER, folderPath, false,
                        defaultHost);

        assertNotNull(identifiers);
        assertFalse(identifiers.isEmpty());
    }

    @Test
    public void testFindByURIFound() throws DotDataException, DotSecurityException {

        Contentlet fileAsset = TestDataUtils.getFileAssetContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());
        Folder assetFolder = APILocator.getFolderAPI()
                .find(fileAsset.getFolder(), APILocator.systemUser(), false);
        String folderPath = assetFolder.getPath();
        if (folderPath.endsWith("/")) {//Removing trailing /
            folderPath = folderPath.substring(0, folderPath.length() - 1);
        }

        //Flush cache for this contentlet to force look up in database
        ic.removeFromCacheByURI(defaultHost.getIdentifier(), folderPath);
        final Identifier identifier = factory.findByURI(defaultHost.getIdentifier(), folderPath);

        assertTrue(identifier.getId() != null && !identifier.getId().isEmpty());
        assertEquals(assetFolder.getName(), identifier.getAssetName());
        assertEquals(Identifier.ASSET_TYPE_FOLDER, identifier.getAssetType());
    }

    @Test
    public void testfindByURINotFound() throws DotDataException {

        //Flush cache for this contentlet to force look up in database
        ic.removeFromCacheByURI(defaultHost.getIdentifier(), "/products");

        final Identifier identifier = factory.findByURI(systemHost.getIdentifier(), "/products");

        assertTrue(identifier.getId() != null && identifier.getId().isEmpty());
        assertNull(identifier.getAssetName());
        assertNull(identifier.getAssetType());
    }

    @Test
    public void testFindByParentPathFound() throws DotDataException, DotSecurityException {

        Contentlet fileAsset = TestDataUtils.getFileAssetContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());
        Folder assetFolder = APILocator.getFolderAPI()
                .find(fileAsset.getFolder(), APILocator.systemUser(), false);
        String folderPath = assetFolder.getPath();
        if (folderPath.endsWith("/")) {//Removing trailing /
            folderPath = folderPath.substring(0, folderPath.length() - 1);
        }

        final List<Identifier> identifiers = factory
                .findByParentPath(defaultHost.getIdentifier(), folderPath);

        assertTrue(identifiers != null && !identifiers.isEmpty());
        assertEquals(assetFolder.getPath(), identifiers.get(0).getParentPath());
    }

    @Test
    public void testFindByParentPathVsFindByURIEquivalency()
            throws DotDataException, DotSecurityException {

        Contentlet fileAsset = TestDataUtils.getFileAssetContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());
        Folder assetFolder = APILocator.getFolderAPI()
                .find(fileAsset.getFolder(), APILocator.systemUser(), false);
        String folderPath = assetFolder.getPath();
        if (folderPath.endsWith("/")) {//Removing trailing /
            folderPath = folderPath.substring(0, folderPath.length() - 1);
        }

        final List<Identifier> identifiers = factory
                .findByParentPath(defaultHost.getIdentifier(), folderPath);

        assertTrue(identifiers != null && !identifiers.isEmpty());
        assertEquals(assetFolder.getPath(), identifiers.get(0).getParentPath());

        final Identifier identifier = factory
                .findByURI(defaultHost.getIdentifier(), folderPath);

        assertNotNull(identifier);
        assertEquals(assetFolder.getPath(), identifier.getURI() + "/");

    }

    @Test
    public void test_Find_By_Parent_Path_Same_URI_Different_Host() throws IOException, DotDataException, DotSecurityException{
        final String binaryFieldContentTypeId = contentTypeAPI.find(FileAssetAPI.BINARY_FIELD).id();
        final String commonPath = "child-1/child-2/child-3/child-4";
        final String anyContent = "LOL!";
        final long defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final User user = APILocator.systemUser();
        // Find a default host
        final Host host1 = APILocator.getHostAPI().findDefaultHost(user, false);
        Host host2 = null;
        Folder root1 = null;
        Folder root2 = null;
        try {
        // Create a test host
            host2 = new Host();
            host2.setHostname("my.testsite-" + System.currentTimeMillis() + ".com");
            host2.setDefault(false);
            host2.setLanguageId(defaultLanguage);
            host2.setIndexPolicy(IndexPolicy.FORCE);
            host2 = APILocator.getHostAPI().save(host2, user, false);

            // Create a Folder path that will be used in both host, Same path added in both hosts
            final String rootFolderName = String
                    .format("lolFolder-%d", System.currentTimeMillis());
            root1 = folderAPI.createFolders(rootFolderName, host1, user, false);
            root2 = folderAPI.createFolders(rootFolderName, host2, user, false);

            final Folder subFolders1 = folderAPI
                    .createFolders(root1.getPath() + commonPath,
                            host1, user, false);

            // Create a different binary for each one them
            Contentlet newContentlet1 = new FileAsset();
            final String fileName1 = TEMP_FILE + System.currentTimeMillis();
            final File tempFile1 = File.createTempFile(fileName1, TXT);
            FileUtil.write(tempFile1, anyContent);
            final String fileNameField1 = fileName1 + DOT_TXT;
            final String title1 = "Contentlet-1";

            newContentlet1.setFolder(subFolders1.getInode());
            newContentlet1.setContentTypeId(binaryFieldContentTypeId);
            newContentlet1.setBinary(FileAssetAPI.BINARY_FIELD, tempFile1);
            newContentlet1.setStringProperty(FileAssetAPI.HOST_FOLDER_FIELD, subFolders1.getInode());
            newContentlet1.setStringProperty(FileAssetAPI.TITLE_FIELD, title1);
            newContentlet1.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileNameField1);
            newContentlet1.setIndexPolicy(IndexPolicy.FORCE);

            // Create a piece of content for the default host
            newContentlet1 = APILocator.getContentletAPI().checkin(newContentlet1, user, false);
            Logger.info(getClass(),newContentlet1.getIdentifier());

            final Folder subFolders2 = folderAPI
                    .createFolders(root2.getPath() + commonPath,
                            host2, user, false);

            Contentlet newContentlet2 = new FileAsset();
            final String fileName2 = TEMP_FILE + System.currentTimeMillis();
            final File tempFile2 = File.createTempFile(fileName2, "txt");
            FileUtil.write(tempFile2, anyContent);
            final String fileNameField2 = fileName2 + DOT_TXT;
            final String title2 = "Contentlet-2";

            newContentlet2.setFolder(subFolders2.getInode());
            newContentlet2.setContentTypeId(binaryFieldContentTypeId);
            newContentlet2.setBinary(FileAssetAPI.BINARY_FIELD, tempFile2);
            newContentlet2.setStringProperty(FileAssetAPI.HOST_FOLDER_FIELD, subFolders2.getInode());
            newContentlet2.setStringProperty(FileAssetAPI.TITLE_FIELD, title2);
            newContentlet2.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileNameField2);
            newContentlet2.setIndexPolicy(IndexPolicy.FORCE);

            // Create a piece of content for the new host
            newContentlet2 = APILocator.getContentletAPI().checkin(newContentlet2, user, false);
            Logger.info(getClass(),newContentlet2.getIdentifier());

            final String sharedPath1 = root1.getPath() + commonPath;
            final String sharedPath2 = root2.getPath() + commonPath;

            Logger.info(getClass(), () -> " SharedPath is: " + sharedPath1);

            assertEquals("shared paths must match", sharedPath1, sharedPath2);

            // The actual test. Begins down here...

            // find by parent should bring back each contentlet for the given path and hostId
            final List<Identifier> identifiers1 = factory
                    .findByParentPath(host1.getIdentifier(), sharedPath1);
            assertFalse(identifiers1.isEmpty());
            assertEquals("assetName does not match", fileNameField1, identifiers1.get(0).getAssetName());

            final List<Identifier> identifiers2 = factory
                    .findByParentPath(host2.getIdentifier(), sharedPath2);
            assertFalse(identifiers2.isEmpty());
            assertEquals("assetName does not match", fileNameField2, identifiers2.get(0).getAssetName());

            final Contentlet contentlet1 = APILocator.getContentletAPI()
                    .findContentletByIdentifier(newContentlet1.getIdentifier(), false, defaultLanguage, user,
                            false);
            final String actualTitle1 = contentlet1.getStringProperty(FileAssetAPI.TITLE_FIELD);
            assertEquals("Title does not match.", title1, actualTitle1);

            final Contentlet contentlet2 = APILocator.getContentletAPI()
                    .findContentletByIdentifier(newContentlet2.getIdentifier(), false, defaultLanguage, user,
                            false);
            final String actualTitle2 = contentlet2.getStringProperty(FileAssetAPI.TITLE_FIELD);
            assertEquals("Title does not match.", title2, actualTitle2);

            //Now Let's test findByURIPattern
            String assetType = "contentlet";
            final List<String> list = Stream.of(sharedPath1.split("/")).filter(s->!s.isEmpty()).collect(Collectors.toList());
            for(int i = list.size(); i >=0; i--){

                 final List<String> sublist = list.subList(0,i);
                 final String uri =  "/" + sublist.stream().collect(Collectors.joining("/"));

                 if("/".equals(uri)){
                    break;
                 }

                 String uri1, uri2;
                 uri1 = uri2 = uri;
                 if(i < list.size()){
                   assetType = "folder";
                 }

                 if("contentlet".equals(assetType) && !uri.endsWith("/")){
                     uri1 = uri + "/" + fileNameField1;
                     uri2 = uri + "/" + fileNameField2;
                 }

                 Logger.info(getClass(), "URI1 : " +  uri1);
                 Logger.info(getClass(), "URI2 : " +  uri2);
                 final List<Identifier> host1Identifiers = factory.findByURIPattern(assetType, uri1 ,true, host1);
                 assertFalse("at least 1 identifier is expected",host1Identifiers.isEmpty());
                 for(final Identifier id:host1Identifiers){
                     assertEquals("AssetType not matching expected.", assetType, id.getAssetType());
                 }

                 final List<Identifier> host2Identifiers = factory.findByURIPattern(assetType, uri2,true, host2);
                 assertFalse("at least 1 identifier is expected",host2Identifiers.isEmpty());
                 for(final Identifier id:host2Identifiers){
                    assertEquals("AssetType not matching expected.", assetType, id.getAssetType());
                 }
            }

        }finally {

            try {
                if (null != root1) {
                    APILocator.getFolderAPI().delete(root1, user, false);
                }

                if (null != root2) {
                    APILocator.getFolderAPI().delete(root2, user, false);
                }

                if (null != host2) {
                    APILocator.getHostAPI().archive(host2, user, false);
                    APILocator.getHostAPI().delete(host2, user, false);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void testFindByParentPathNotFound() throws DotDataException {

        final List<Identifier> identifiers = factory
                .findByParentPath(systemHost.getIdentifier(), "/blogs");

        assertTrue(identifiers == null || identifiers.isEmpty());
    }

    @Test
    public void testLoadFromDbFound() throws DotDataException {
        final Identifier identifier = factory.loadFromDb(defaultHost.getIdentifier());

        assertNotNull(identifier);
        assertEquals(defaultHost.getIdentifier(), identifier.getId());

    }

    @Test(expected = DotStateException.class)
    public void testLoadFromDbWithNullID() throws DotDataException {
        factory.loadFromDb((String) null);
    }

    @Test
    public void testLoadFromDbVersionableFound() throws DotDataException {
        final Identifier identifier = factory.loadFromDb(defaultHost);

        assertNotNull(identifier);
        assertEquals(defaultHost.getIdentifier(), identifier.getId());

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

        parentFolder = new FolderDataGen().nextPersisted();
        newFolder = new Folder();
        identifier = null;

        newFolder.setName("TestingFolder" + System.currentTimeMillis());

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newFolder, parentFolder);

            assertNotNull(identifier.getId());
            assertFalse(identifier.getId().isEmpty());
            assertNotNull(newFolder.getVersionId());
            assertFalse(newFolder.getVersionId().isEmpty());
            assertEquals(parentFolder.getPath(), identifier.getParentPath());
            assertEquals(Identifier.ASSET_TYPE_FOLDER, identifier.getAssetType());
            assertEquals(newFolder.getName(), identifier.getAssetName());
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
        fileName = TEMP_FILE + System.currentTimeMillis();
        tempFile = File.createTempFile(fileName, TXT);
        parentFolder = new FolderDataGen().nextPersisted();

        newContentlet.setContentTypeId(contentTypeAPI.find(FileAssetAPI.BINARY_FIELD).id());
        newContentlet.setBinary(FileAssetAPI.BINARY_FIELD, tempFile);
        newContentlet.setStringProperty(FileAssetAPI.HOST_FOLDER_FIELD, parentFolder.getInode());
        newContentlet.setStringProperty(FileAssetAPI.TITLE_FIELD, fileName + DOT_TXT);
        newContentlet.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileName + DOT_TXT);

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newContentlet, parentFolder);

            assertNotNull(identifier.getId());
            assertFalse(identifier.getId().isEmpty());
            assertNotNull(newContentlet.getVersionId());
            assertFalse(newContentlet.getVersionId().isEmpty());
            assertEquals(parentFolder.getPath(), identifier.getParentPath());
            assertEquals(Identifier.ASSET_TYPE_CONTENTLET, identifier.getAssetType());
            assertEquals(
                    newContentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD),
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
        parentFolder = new FolderDataGen().nextPersisted();

        newContentlet.setContentTypeId(contentTypeAPI.find(
                        HTMLPageAssetAPI.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_VARNAME).inode());
        newContentlet.setStringProperty(HTMLPageAssetAPI.URL_FIELD, pageName);

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newContentlet, parentFolder);

            assertNotNull(identifier.getId());
            assertFalse(identifier.getId().isEmpty());
            assertNotNull(newContentlet.getVersionId());
            assertFalse(newContentlet.getVersionId().isEmpty());
            assertEquals(parentFolder.getPath(), identifier.getParentPath());
            assertEquals(Identifier.ASSET_TYPE_CONTENTLET, identifier.getAssetType());
            assertEquals(pageName, identifier.getAssetName());
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
        parentFolder = new FolderDataGen().nextPersisted();

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newWebAsset, parentFolder);

            assertNotNull(identifier.getId());
            assertFalse(identifier.getId().isEmpty());
            assertNotNull(newWebAsset.getVersionId());
            assertFalse(newWebAsset.getVersionId().isEmpty());
            assertEquals(parentFolder.getPath(), identifier.getParentPath());
            assertEquals("template", identifier.getAssetType());
            assertNotNull(identifier.getURI());
            assertNotNull(identifier.getAssetType());

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
        parentFolder = new FolderDataGen().nextPersisted();
        newWebAsset.setInode(UUIDGenerator.generateUuid());

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newWebAsset, parentFolder);

            assertNotNull(identifier.getId());
            assertFalse(identifier.getId().isEmpty());
            assertNotNull(newWebAsset.getVersionId());
            assertFalse(newWebAsset.getVersionId().isEmpty());
            assertEquals(parentFolder.getPath(), identifier.getParentPath());
            assertEquals("links", identifier.getAssetType());
            assertNotNull(identifier.getURI());
            assertNotNull(identifier.getAssetType());

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

            assertNotNull(identifier.getId());
            assertFalse(identifier.getId().isEmpty());
            assertNotNull(newFolder.getVersionId());
            assertFalse(newFolder.getVersionId().isEmpty());
            assertEquals(Identifier.ASSET_TYPE_FOLDER, identifier.getAssetType());
            assertEquals(newFolder.getName(), identifier.getAssetName());
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
        fileName = TEMP_FILE + System.currentTimeMillis();
        tempFile = File.createTempFile(fileName, TXT);

        newContentlet.setContentTypeId(contentTypeAPI.find(FileAssetAPI.BINARY_FIELD).inode());
        newContentlet.setBinary(FileAssetAPI.BINARY_FIELD, tempFile);
        newContentlet.setStringProperty(FileAssetAPI.TITLE_FIELD, fileName + DOT_TXT);
        newContentlet.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileName + DOT_TXT);

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newContentlet, defaultHost);

            assertNotNull(identifier.getId());
            assertFalse(identifier.getId().isEmpty());
            assertNotNull(newContentlet.getVersionId());
            assertFalse(newContentlet.getVersionId().isEmpty());
            assertEquals(Identifier.ASSET_TYPE_CONTENTLET, identifier.getAssetType());
            assertEquals(
                    newContentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD),
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
        pageName = TEMP_FILE + System.currentTimeMillis();

        newContentlet.setContentTypeId(contentTypeAPI.find(
                        HTMLPageAssetAPI.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_VARNAME).inode());
        newContentlet.setStringProperty(HTMLPageAssetAPI.URL_FIELD, pageName);

        try {
            //Creates new identifier
            identifier = factory.createNewIdentifier(newContentlet, defaultHost);

            assertNotNull(identifier.getId());
            assertFalse(identifier.getId().isEmpty());
            assertNotNull(newContentlet.getVersionId());
            assertFalse(newContentlet.getVersionId().isEmpty());
            assertEquals(Identifier.ASSET_TYPE_CONTENTLET, identifier.getAssetType());
            assertEquals(pageName, identifier.getAssetName());
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

            assertNotNull(identifier.getId());
            assertFalse(identifier.getId().isEmpty());
            assertNotNull(newContentlet.getVersionId());
            assertFalse(newContentlet.getVersionId().isEmpty());
            assertEquals(Identifier.ASSET_TYPE_CONTENTLET, identifier.getAssetType());

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

        assertNotNull(identifiers);
        assertTrue(identifiers.size() > 0);
    }

    @Test
    public void testIsIdentifierFound() {
        final boolean found = factory.isIdentifier(defaultHost.getIdentifier());

        assertTrue(found);
    }

    @Test
    public void testIsIdentifierWhenNull() {
        final boolean found = factory.isIdentifier(null);

        assertFalse(found);
    }

    @Test
    public void testGetAssetTypeFromDB() throws DotDataException {
       final String assetType = factory.getAssetTypeFromDB(defaultHost.getIdentifier());

        assertNotNull(assetType);
        assertEquals("contentlet", assetType);

    }

    @Test
    public void testGetAssetTypeFromDBWhenNull() throws DotDataException {
        final String assetType = factory.getAssetTypeFromDB(null);

        assertNull(assetType);

    }

    private void deleteIdentifier(final Identifier identifier) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL("delete from identifier where id = ?");
        db.addParam(identifier.getId());
        db.loadResult();
    }

}
