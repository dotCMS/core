package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.component.SiteAndFolderParams;
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
import java.util.Optional;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class SiteAndFolderResolverImplTest extends IntegrationTestBase {

    private static final String FULL_PATH = "%s:/%s/";

    final FolderAPI folderAPI = APILocator.getFolderAPI();

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

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

        //These two provide default value therefore the expected is they'll be not null
        Assert.assertNotNull(contentType.folderPath());
        Assert.assertNotNull(contentType.siteName());

        final SiteAndFolderParams siteAndFolder1 = contentType.siteAndFolderParams();
        Assert.assertNotNull(siteAndFolder1.folder());
        Assert.assertNotNull(siteAndFolder1.host());
        //However, these two should return null because no value has been used to set them in reality
        Assert.assertNull(siteAndFolder1.folderPath());
        Assert.assertNull(siteAndFolder1.siteName());

        final ImmutableFileAssetContentType copy = ImmutableFileAssetContentType.copyOf(
                (FileAssetContentType) contentType);
        final SiteAndFolderParams siteAndFolder = copy.siteAndFolderParams();
        Assert.assertNotNull(siteAndFolder.folder());
        Assert.assertNotNull(siteAndFolder.host());
        Assert.assertNull(siteAndFolder.folderPath());
        Assert.assertNull(siteAndFolder.siteName());

        // TODO: Fix me This builder copy thing is not right!!!
        //  It'll initialize the default methods

        final ContentType copyContentType = ContentTypeBuilder.builder(contentType).build();

        final SiteAndFolderParams siteAndFolder2 = copyContentType.siteAndFolderParams();
        Assert.assertNotNull(siteAndFolder2.folder());
        Assert.assertNotNull(siteAndFolder2.host());
        //However, these two should return null because no value has been used to set them in reality
        Assert.assertNull(siteAndFolder2.folderPath());
        Assert.assertNull(siteAndFolder2.siteName());

    }

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
