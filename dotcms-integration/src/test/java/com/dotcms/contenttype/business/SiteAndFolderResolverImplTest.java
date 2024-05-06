package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.component.SiteAndFolder;
import com.dotcms.contenttype.model.type.*;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.ImportUtil;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
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

    /**
     * Test that if we explicitly set SYSTEM_HOST as the desired site hosting the CT we will get that back
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Explicitly_Set_System_Host_And_System_Folder_Expect_Same_Values() throws DotDataException, DotSecurityException{

        final User user = APILocator.systemUser();
        final SiteAndFolderResolver siteAndFolderResolver = SiteAndFolderResolver.newInstance(user);

        final String variable = "testDotAsset" + System.currentTimeMillis();
        ContentType contentType = ContentTypeBuilder.builder(DotAssetContentType.class)
                .folder(FolderAPI.SYSTEM_FOLDER)
                .host(Host.SYSTEM_HOST).name(variable)
                .owner(user.getUserId()).build();

        ContentType resolved = siteAndFolderResolver.resolveSiteAndFolder(contentType);
        Assert.assertEquals("System-Host was Explicitly set", Host.SYSTEM_HOST, resolved.host());
        Assert.assertEquals("System-Folder was Explicitly set", Folder.SYSTEM_FOLDER, resolved.folder());

        //Test idempotency
        resolved = siteAndFolderResolver.resolveSiteAndFolder(contentType);
        Assert.assertEquals("System-Host should still be here. ", Host.SYSTEM_HOST, resolved.host());
        Assert.assertEquals("System-Folder should still be here. ", Folder.SYSTEM_FOLDER, resolved.folder());

    }

    /**
     * Here we're testing that the SiteAndFolder property is aware of default values being returned or not
     * on the contentType  properties host and folder
     * Basically if something has been explicitly set on host
     *   contentType.siteAndFolder().host() should be not null
     * Similar if something has been set explicitly set on folder
     *   contentType.siteAndFolder().folder() should be not null
     * If on the contrary if something gets explicitly set on those two fields
     * we should continue to see the new values that were manually set
     */
    @Test
    public void Test_Get_SiteAndFolderParams_When_No_Host_Value_Is_Expect_Null(){
        final String path = "path";
        final String host = "host";
        final String folder = "folder";
        final String variable = "dotTest" + System.currentTimeMillis();
        ContentType contentType = ContentTypeBuilder.builder(FileAssetContentType.class)
                // We're not setting any values on host nor folder
                .name(variable)
                .folderPath(path)
                .build();
        //These are expected to return the default value
        Assert.assertEquals(Host.SYSTEM_HOST, contentType.host());
        Assert.assertEquals(Folder.SYSTEM_FOLDER, contentType.folder());
        Assert.assertEquals(path,contentType.folderPath());

        SiteAndFolder siteAndFolder = contentType.siteAndFolder();

        Assert.assertNull(siteAndFolder.host());
        Assert.assertNull(siteAndFolder.folder());
        Assert.assertEquals(path,siteAndFolder.folderPath());

        //Now test the values on siteAndFolder after setting explicit values on host and folder
        contentType = ContentTypeBuilder.builder(FileAssetContentType.class)
                //.loadedOrResolved(true)
                // here We're explicitly setting values on folder and host
                .name(variable)
                .host(path)
                .folder(folder)
                .host(host)
                .folderPath(folder + path)
                .build();

        siteAndFolder = contentType.siteAndFolder();

        Assert.assertEquals(host,siteAndFolder.host());
        Assert.assertEquals(folder,siteAndFolder.folder());
        Assert.assertEquals(folder + path, siteAndFolder.folderPath());
    }


    @Test
    public void TestCaller(){
        Assert.assertTrue(SiteAndFolderResolver.isCalledByClass(SiteAndFolderResolverImplTest.class.getName()));
        Assert.assertFalse(SiteAndFolderResolver.isCalledByClass(ImportUtil.class.getName()));
    }

}
