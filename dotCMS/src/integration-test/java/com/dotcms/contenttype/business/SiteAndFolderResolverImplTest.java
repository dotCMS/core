package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.component.SiteAndFolder;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.Optional;


public class SiteAndFolderResolverImplTest extends IntegrationTestBase {

    private static final String FULL_PATH = "%s:/%s/";

    final FolderAPI folderAPI = APILocator.getFolderAPI();

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Here we're basically testing that
     * When Building a CT that is marked as source DB
     * The values on folderPath and siteName should show a calculated value
     * When Source is different from DB It is considered that the CT is used to capture values
     * e.g. When the class is used in the resource to capture a user input
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_CT_SiteAndFolderParams() throws DotDataException, DotSecurityException {

        final User user = APILocator.systemUser();
        final Host host = new SiteDataGen().nextPersisted();
        final Folder foo = folderAPI
                .createFolders("/foo"+System.currentTimeMillis(), host, user, false);

        final String variable = "variableName" + System.currentTimeMillis();
        ContentType contentType = ContentTypeBuilder.builder(FileAssetContentType.class)
                .folder(foo.getInode())
                .host(host.getIdentifier())
                .name(variable)
                .id(UUIDUtil.uuid())
                .owner(user.getUserId()).build();

        Assert.assertFalse(contentType.loadedOrResolved());
        //If source isn't db These two provide default value therefore the expected is they'll be null
        //These two should only provide a calculated value when the content-type is loaded from the db.
        Assert.assertNull(contentType.folderPath());
        Assert.assertNull(contentType.siteName());

        final SiteAndFolder siteAndFolder1 = contentType.siteAndFolder();
        Assert.assertNotNull(siteAndFolder1.folder());
        Assert.assertNotNull(siteAndFolder1.host());
        //However, these two should return null because no value has been used to set them in reality
        Assert.assertNull(siteAndFolder1.folderPath());
        Assert.assertNull(siteAndFolder1.siteName());

        final ImmutableFileAssetContentType copy = ImmutableFileAssetContentType.copyOf(
                (FileAssetContentType) contentType);
        final SiteAndFolder siteAndFolder = copy.siteAndFolder();
        Assert.assertNotNull(siteAndFolder.folder());
        Assert.assertNotNull(siteAndFolder.host());
        Assert.assertNull(siteAndFolder.folderPath());
        Assert.assertNull(siteAndFolder.siteName());

        // TODO: Fix me This builder copy thing is not right!!!
        //  It'll initialize the default methods

        final ContentType copyContentType = ContentTypeBuilder.builder(contentType).build();

        final SiteAndFolder siteAndFolder2 = copyContentType.siteAndFolder();
        Assert.assertNotNull(siteAndFolder2.folder());
        Assert.assertNotNull(siteAndFolder2.host());
        //However, these two should return null because no value has been used to set them in reality
        Assert.assertNull(siteAndFolder2.folderPath());
        Assert.assertNull(siteAndFolder2.siteName());

        ContentType contentTypeFromDB = ContentTypeBuilder.builder(FileAssetContentType.class)
                .folder(foo.getInode())
                .host(host.getIdentifier())
                .name(variable)
                .id(UUIDUtil.uuid())
                .owner(user.getUserId())
                .loadedOrResolved(true)
                .build();

        Assert.assertTrue(contentTypeFromDB.loadedOrResolved());
        //These two should only provide a calculated value when the content-type is loaded from the db.
        //Therefore, not null is the expected here. All good.
        Assert.assertNotNull(contentTypeFromDB.folderPath());
        Assert.assertNotNull(contentTypeFromDB.siteName());

        final SiteAndFolder fromDB = contentTypeFromDB.siteAndFolder();
        Assert.assertNotNull(fromDB.folder());
        Assert.assertNotNull(fromDB.host());

        //However, these should still be not null as this comes from the db
        Assert.assertNotNull(fromDB.folderPath());
        Assert.assertNotNull(fromDB.siteName());

    }

    /**
     * Purpose of this testis verify that when we build the Resolver using skipping resolve site set to true
     * Whatever comes in is what we should expect to come back
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Skip_Validate_Whatever_Comes_In_Comes_Back_TheSame() throws DotDataException, DotSecurityException {
        final User user = APILocator.systemUser();
        final SiteAndFolderResolverImpl impl = new SiteAndFolderResolverImpl(user, true, true);

        final String variable = "varName" + System.currentTimeMillis();
        ContentType contentType = ContentTypeBuilder.builder(FileAssetContentType.class)
                .folder("fakeValidFolderId")
                .host("fakeValidHostId")
                .name(variable)
                .id(UUIDUtil.uuid())
                .owner(user.getUserId()).build();

        final ContentType resolvedSiteAndFolder = impl.resolveSiteAndFolder(contentType);
        Assert.assertEquals("fakeValidFolderId",resolvedSiteAndFolder.folder());
        Assert.assertEquals("fakeValidHostId",resolvedSiteAndFolder.host());

    }


    /**
     * Fixed content-type should only belong into system-host and therefore also belong into system-folder
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Resolve_Fixed_ContentType() throws DotDataException, DotSecurityException {
        final User user = APILocator.systemUser();

        final String variable = "varName" + System.currentTimeMillis();
        ContentType contentType = ContentTypeBuilder.builder(FileAssetContentType.class)
                        .folder("anyFolder")
                        .host("anyHost")
                        .name(variable)
                        .fixed(true)
                        .id(UUIDUtil.uuid())
                        .owner(user.getUserId()).build();

        final SiteAndFolderResolverImpl skipResolveFallbackToSystem = new SiteAndFolderResolverImpl(user, false, false);
        final ContentType resolved1 = skipResolveFallbackToSystem.resolveSiteAndFolder(contentType);
        Assert.assertEquals(Host.SYSTEM_HOST, resolved1.host());
        Assert.assertEquals(Folder.SYSTEM_FOLDER, resolved1.folder());
        Assert.assertEquals(Folder.SYSTEM_FOLDER_PATH, resolved1.folderPath());
        Assert.assertEquals(Host.SYSTEM_HOST_NAME, resolved1.siteName());
    }

    /**
     * Once a content-type has been resolved it should be able to return siteName and folderPath
     * Scenario: We create an instance that is instructed to fall back to default
     * Expected: the resolved CT should return the "default" site
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Fallback_To_Default() throws DotDataException, DotSecurityException {
        final User user = APILocator.systemUser();

        final String variable = "varName" + System.currentTimeMillis();
        ContentType contentType = ContentTypeBuilder.builder(FileAssetContentType.class)
                .folder("anyFolder")
                .host("anyHost")
                .name(variable)
                .id(UUIDUtil.uuid())
                .owner(user.getUserId()).build();

        final SiteAndFolderResolverImpl skipResolveFallbackToDefault = new SiteAndFolderResolverImpl(user, false, true);
        final ContentType resolved1 = skipResolveFallbackToDefault.resolveSiteAndFolder(contentType);
        Assert.assertEquals("default", resolved1.siteName());
        Assert.assertTrue(UUIDUtil.isUUID(resolved1.host()));
        Assert.assertEquals(Folder.SYSTEM_FOLDER, resolved1.folder());
        Assert.assertEquals(Folder.SYSTEM_FOLDER_PATH, resolved1.folderPath());
    }

    /**
     * Once a content-type has been resolved it should be able to return siteName and folderPath
     * Scenario: We create an instance that is instructed to fall back to system-host
     * Expected: the resolved CT should return the "default" site
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Fallback_To_System_Host() throws DotDataException, DotSecurityException {
        final User user = APILocator.systemUser();

        final String variable = "varName" + System.currentTimeMillis();
        ContentType contentType = ContentTypeBuilder.builder(FileAssetContentType.class)
                .folder("anyFolder"+System.currentTimeMillis())
                .host("any_Host"+System.currentTimeMillis())
                .name(variable)
                .id(UUIDUtil.uuid())
                .owner(user.getUserId()).build();

        final SiteAndFolderResolverImpl skipResolveFallbackToSystem = new SiteAndFolderResolverImpl(user, false, false);
        final ContentType resolved1 = skipResolveFallbackToSystem.resolveSiteAndFolder(contentType);
        Assert.assertEquals(Host.SYSTEM_HOST, resolved1.host());
        Assert.assertEquals(Folder.SYSTEM_FOLDER, resolved1.folder());
        Assert.assertEquals(Folder.SYSTEM_FOLDER_PATH, resolved1.folderPath());
    }

    /**
     * Test Once a valid folder is set the returned folderPath is valid
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Set_Existing_Folder_Expect_Valid_Resolution() throws DotDataException, DotSecurityException {

        final User user = APILocator.systemUser();

        final Host host = new SiteDataGen().nextPersisted();
        final Folder foo = folderAPI
                .createFolders("/foo"+System.currentTimeMillis(), host, user, false);

        final String variable = "varName" + System.currentTimeMillis();
        ContentType contentType = ContentTypeBuilder.builder(FileAssetContentType.class)
                .folder(foo.getInode())
                .host(host.getIdentifier())
                .name(variable)
                .id(UUIDUtil.uuid()) //We must set an id to simulate a saved CT
                .owner(user.getUserId()).build();

        final SiteAndFolderResolverImpl impl = new SiteAndFolderResolverImpl(user, false, true);
        final ContentType resolved1 = impl.resolveSiteAndFolder(contentType);
        Assert.assertEquals(host.getIdentifier(), resolved1.host());
        Assert.assertEquals(foo.getInode(), resolved1.folder());
        Assert.assertEquals(String.format(FULL_PATH,host.getHostname(),foo.getName()), resolved1.folderPath());
    }

    /**
     * Feed a valid folder path. Expect resolved valid folder with matching ids
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Set_Folder_Path_Expect_Valid_Resolution() throws DotDataException, DotSecurityException {

        final User user = APILocator.systemUser();

        final Host host = new SiteDataGen().nextPersisted();
        final Folder foo = folderAPI
                .createFolders("/foo"+System.currentTimeMillis(), host, user, false);

        final String folderPath = String.format(FULL_PATH, host.getHostname(), foo.getName());

        final String variable = "varName" + System.currentTimeMillis();
        ContentType contentType = ContentTypeBuilder.builder(FileAssetContentType.class)
                .folderPath(folderPath)
                .name(variable)
                .id(UUIDUtil.uuid()) //We must set an id to simulate a saved CT
                .owner(user.getUserId()).build();

        final SiteAndFolderResolverImpl impl = new SiteAndFolderResolverImpl(user, false, true);
        final ContentType resolved1 = impl.resolveSiteAndFolder(contentType);
        Assert.assertEquals(host.getIdentifier(), resolved1.host());
        Assert.assertEquals(foo.getInode(), resolved1.folder());
        Assert.assertEquals(folderPath, resolved1.folderPath());
    }

    /**
     * Test internal method fromPath
     * Path is of the form site:/folderName
     * Scenario 1: Invalid path
     * Expected: Empty resolution
     * Scenario 2: Blank string
     * Expected: Empty resolution
     * Scenario 3: site:/
     * Expected: site and system folder
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_fromPath_Method() throws DotDataException, DotSecurityException {

        final Host host = new SiteDataGen().nextPersisted();
        final User user = APILocator.systemUser();
        final String folderPath = String.format("/foo%d/",System.currentTimeMillis());
        final Folder foo = folderAPI.createFolders(folderPath, host, user, false);

        final SiteAndFolderResolverImpl impl = new SiteAndFolderResolverImpl(user, false, true);
        final Optional<Folder> folder1 = impl.fromPath(  String.format(FULL_PATH, host.getHostname(),foo.getName()));
        Assert.assertTrue(folder1.isPresent());
        Assert.assertEquals(foo.getIdentifier(),folder1.get().getIdentifier());

        //Test non-existing folder
        final Optional<Folder> folder2 = impl.fromPath("default:/xyz");
        Assert.assertTrue(folder2.isEmpty());

        final Optional<Folder> folder3 = impl.fromPath("");
        Assert.assertTrue(folder3.isEmpty());

        final Optional<Folder> folder4 = impl.fromPath("default:/");
        Assert.assertTrue(folder4.isPresent());
        Assert.assertTrue(folder4.get().isSystemFolder());

    }

    /**
     * Test another version of the internal method fromPath (the one that takes site id)
     * Path is of the form site:/folderName
     * Scenario 1: Invalid path
     * Expected: Empty resolution
     * Scenario 2: Blank string
     * Expected: Empty resolution
     * Scenario 3: site:/
     * Expected: site and system folder
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_fromPath_And_Site_Method() throws DotDataException, DotSecurityException {

        final Host host = new SiteDataGen().nextPersisted();
        final User user = APILocator.systemUser();
        final String folderPath = String.format("/foo%d/",System.currentTimeMillis());
        final Folder foo = folderAPI.createFolders(folderPath, host, user, false);

        final SiteAndFolderResolverImpl impl = new SiteAndFolderResolverImpl(user, false, true);
        final Optional<Folder> folder1 = impl.fromPath(folderPath, host.getIdentifier());
        Assert.assertTrue(folder1.isPresent());
        Assert.assertEquals(foo.getIdentifier(),folder1.get().getIdentifier());

        final Optional<Folder> folder2 = impl.fromPath("/xyz", host.getIdentifier());
        Assert.assertTrue(folder2.isEmpty());

    }



}
