package com.dotcms.rest.api.v1.assets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.rest.api.v1.asset.AssetPathResolver;
import com.dotcms.rest.api.v1.asset.ResolvedAssetAndPath;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import org.junit.BeforeClass;
import org.junit.Test;

public class AssetPathResolverImplTest {

    static Host host;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        host = new SiteDataGen().nextPersisted(true);
        Folder foo = new FolderDataGen().site(host).name("foo").nextPersisted();
        final Folder bar = new FolderDataGen().parent(foo).name("bar").nextPersisted();
    }

    /**
     * This is special case and needs to be solved separately Cuz FolderAPI always return system
     * folder when path is "/" regardless of host
     *
     * @throws DotDataException
     */
    @Test
    public void Test_Parse_Host_Followed_By_Root() throws DotDataException, DotSecurityException {
        final String url = String.format("http://%s/", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.newInstance()
                .resolve(url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/", parse.path());
        assertNull(parse.asset());
    }

    /**
     * Given scenario: host followed by path the last portion should be considered an asset only if a folder with that name exists
     * Expected: host is resolved and path is empty
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Parse_Path_With_Asset() throws DotDataException, DotSecurityException {

        final String url = String.format("http://%s/foo/bar/1234", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.newInstance()
                .resolve(url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/bar/1234", parse.path());
        assertEquals("1234", parse.asset());
    }

    /**
     * Given scenario: host followed by path and resource followed by extension
     * Expected: host is resolved and so it is path and resource
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Parse_Path_With_Resource_And_Extension()
            throws DotDataException, DotSecurityException {
        final String url = String.format("http://%s/foo/bar/1234.webp", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.newInstance()
                .resolve(url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/bar/1234.webp", parse.path());
        assertEquals("1234.webp", parse.asset());
    }

    /**
     * Given scenario: host followed by path
     * Expected: host is resolved and so it is path and resource
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Parse_Path_Shorten_No_Protocol_Host_Name()
            throws DotDataException, DotSecurityException {
        final String url = String.format("//%s/foo/bar/1234", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.newInstance()
                .resolve(url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/bar/1234", parse.path());
        assertEquals("1234", parse.asset());
    }

    /**
     * Given scenario: Try resolving a folder path that does not exist
     * Expected: NotFoundInDbException is the expected exception
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = NotFoundInDbException.class)
    public void Test_Parse_None_Existing_Folder() throws DotDataException, DotSecurityException {
        final String url = String.format("//%s/foo/bar/1234/", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.newInstance()
                .resolve(url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/bar/1234/", parse.path());
        assertNull(parse.asset());
    }

    /**
     * Given scenario: Try resolving a folder ending with forward slash
     * Expected: Should resolve the folder
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Parse_Folder_Path_Ends_With_Forward_Slash() throws DotDataException, DotSecurityException {
        final String url = String.format("http://%s/foo/bar/", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.newInstance().resolve(url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/bar/", parse.path());
        assertNull( parse.asset());
    }

    /**
     * Given scenario: Try resolving a folder ending with NO forward slash
     * Expected: Should resolve the folder
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Parse_Folder_Path_No_Ending_Forward_Slash() throws DotDataException, DotSecurityException {
        final String url = String.format("http://%s/foo/bar", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.newInstance().resolve(url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/bar", parse.path());
        assertNull( parse.asset());
    }

    /**
     * Given scenario: Try resolving a resource under a folder with the same name
     * Expected: Should resolve the resource
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Parse_Folder_Path_With_Asset() throws DotDataException, DotSecurityException {
        final String url = String.format("http://%s/foo/bar/bar", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.newInstance()
                .resolve(url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/bar/bar", parse.path());
        assertEquals("bar", parse.asset());
    }

    @Test
    public void Test_Parse_Asset_Under_Root() throws DotDataException, DotSecurityException {
        final String url = String.format("http://%s/bar.txt", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.newInstance()
                .resolve(url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/bar.txt", parse.path());
        assertEquals("bar.txt", parse.asset());
    }


    /**
     * Given scenario: We request a ur with a non-existing folder
     * Expected: Should resolve the resource and create the missing folder
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Parse_Asset_Path_Create_Missing_Folder() throws DotDataException, DotSecurityException {
        final String newFolder = String.format("foo%s",System.currentTimeMillis());

        final Folder folderByPath = APILocator.getFolderAPI()
                .findFolderByPath(newFolder, host, APILocator.systemUser(), false);
        //Test Folder we intend to create does not exist

        assertNull(folderByPath.getIdentifier());

        final String url = String.format("http://%s/%s/bar.txt", host.getHostname(), newFolder);
        final ResolvedAssetAndPath parse = AssetPathResolver.newInstance()
                .resolve(url, APILocator.systemUser(), true);

        assertNotNull(parse.path());
        assertNotNull(parse.asset());

        final String expectedFolderPath = String.format("/%s", newFolder);

        assertEquals(host.getHostname(), parse.host());
        assertEquals(expectedFolderPath, parse.path().replaceAll("/bar.txt",""));
        assertEquals("bar.txt", parse.asset());

        final Folder folderByPathAfter = APILocator.getFolderAPI()
                .findFolderByPath(expectedFolderPath, host, APILocator.systemUser(), false);
        //Test Folder we intend to create does not exist
        assertNotNull(folderByPathAfter);
        assertNotNull(folderByPathAfter.getIdentifier());
    }


}