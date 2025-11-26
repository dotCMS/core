package com.dotcms.rest.api.v1.asset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class AssetPathResolverImplIntegrationTest {

    static Host host;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        host = new SiteDataGen().nextPersisted(true);
        Folder foo = new FolderDataGen().site(host).name("foo").nextPersisted();
        new FolderDataGen().parent(foo).name("bar").nextPersisted();
        new FolderDataGen().parent(foo).name("test withSpace").nextPersisted();
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
     * Given scenario: host followed by path with an encoded asset name. The asset name contains
     * spaces that have been encoded.
     * <p>
     * Expected: host is resolved, path contains the original asset name with spaces, and asset name
     * is the original decoded value.
     *
     * @throws DotDataException     if there is an error accessing data
     * @throws DotSecurityException if there is a security violation
     */
    @Test
    public void Test_Parse_Path_With_Encoded_Asset() throws DotDataException, DotSecurityException {

        String url = String.format("http://%s/foo/bar/qwe rty.png", host.getHostname());
        url = url.replaceAll(" ", URLEncoder.encode(" ", StandardCharsets.UTF_8));

        final ResolvedAssetAndPath parse = AssetPathResolver.newInstance()
                .resolve(url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/bar/qwe rty.png", parse.path());
        assertEquals("qwe rty.png", parse.asset());
    }

    /**
     * Given scenario: host followed by path with an asset name with spaces that has not been
     * encoded.
     * <p>
     * Expected: an exception is thrown due to the space character in the asset name, and the cause
     * of the exception is URISyntaxException.
     */
    @Test
    public void Test_Parse_Path_With_Asset_Without_Encoding() {

        final String url = String.format("http://%s/foo/bar/qwe rty.png", host.getHostname());

        try {
            AssetPathResolver.newInstance().resolve(url, APILocator.systemUser());
            Assert.fail("Should have thrown an URISyntaxException");
        } catch (Exception e) {
            Assert.assertTrue(e.getCause() instanceof URISyntaxException);
        }
    }

    /**
     * Given scenario: host followed by path with an asset name that has a folder with spaces that
     * has been encoded.
     * <p>
     * Expected: the URL is properly resolved, the host, path, and asset are correctly extracted.
     * the folder with spaces is properly decoded to its original form. No exceptions are expected
     * to be thrown.
     *
     * @throws DotDataException     If there is an error resolving the asset path
     * @throws DotSecurityException If the user does not have the necessary permissions
     */
    @Test
    public void Test_Parse_Path_With_Encoded_Folder()
            throws DotDataException, DotSecurityException {

        String url = String.format("http://%s/foo/test withSpace/qwerty.png", host.getHostname());
        url = url.replaceAll(" ", URLEncoder.encode(" ", StandardCharsets.UTF_8));

        final ResolvedAssetAndPath parse = AssetPathResolver.newInstance()
                .resolve(url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/test withSpace/qwerty.png", parse.path());
        assertEquals("qwerty.png", parse.asset());
    }

    /**
     * Given scenario: host followed by path with an asset name that has a folder with spaces that
     * has not been encoded.
     * <p>
     * Expected: the URL is not properly resolved and an URISyntaxException is thrown.
     * <p>
     * This method tests the case where the URL contains a path with spaces that has not been
     * encoded. It ensures that if the path contains spaces without proper encoding, an
     * URISyntaxException is thrown.
     *
     * @throws URISyntaxException if the URL contains an improperly encoded path with spaces
     */
    @Test
    public void Test_Parse_Path_Without_Encoding() {

        String url = String.format("http://%s/foo/test withSpace/qwerty.png", host.getHostname());

        try {
            AssetPathResolver.newInstance().resolve(url, APILocator.systemUser());
            Assert.fail("Should have thrown an URISyntaxException");
        } catch (Exception e) {
            Assert.assertTrue(e.getCause() instanceof URISyntaxException);
        }
    }

    /**
     * Given scenario: host followed by path with an asset name with spaces that also has a folder
     * with spaces that has been properly encoded.
     * <p>
     * Expected: the URL is properly resolved, the host, path, and asset are correctly extracted.
     * the folder and asset with spaces is properly decoded to its original form. No exceptions are
     * expected to be thrown.
     * <p>
     * This method tests the case where the URL contains a path with spaces that has been properly
     * encoded. It ensures that the path is correctly resolved and the parsed asset and path match
     * the input URL.
     *
     * @throws DotDataException     if there is an error in the asset data
     * @throws DotSecurityException if there is a security exception
     */
    @Test
    public void Test_Parse_Path_With_Encoded_Folder_And_Asset()
            throws DotDataException, DotSecurityException {

        String url = String.format("http://%s/foo/test withSpace/qwe rty.png", host.getHostname());
        url = url.replaceAll(" ", URLEncoder.encode(" ", StandardCharsets.UTF_8));

        final ResolvedAssetAndPath parse = AssetPathResolver.newInstance()
                .resolve(url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/test withSpace/qwe rty.png", parse.path());
        assertEquals("qwe rty.png", parse.asset());
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
     * Given scenario: We request an url with a non-existing folder
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

    /**
     * Given scenario: A folder with parentheses in the name
     * Expected: Should resolve the resource without any exceptions and return the folder as folder and not as an asset
     *
     * @throws DotDataException
     */
    @Test
    public void Test_Resolve_Folder_With_Parenthesis() throws DotDataException, DotSecurityException {
        Folder folder = null;
        try {
            folder = new FolderDataGen().site(host).name("(testFolder)").nextPersisted();
            final String url = String.format("http://%s/(testFolder)/", host.getHostname());

            final ResolvedAssetAndPath parse = AssetPathResolver.newInstance()
                    .resolve(url, APILocator.systemUser());
            assertEquals("/(testFolder)/", parse.path());
            assertNull(parse.asset());
        } finally {
            FolderDataGen.remove(folder);
        }


    }

    /**
     * Given scenario: A folder with parentheses in the name and an asset
     * Expected: Should resolve the resource without any exceptions and return the folder as folder and the asset as an asset
     *
     * @throws DotDataException
     */
    @Test
    public void Test_Resolve_Asset_In_Folder_With_Parenthesis() throws DotDataException, DotSecurityException {
        Folder folder = null;
        try {
            folder = new FolderDataGen().site(host).name("(testFolder)").nextPersisted();
            final String url = String.format("http://%s/(testFolder)/example.txt", host.getHostname());

            final ResolvedAssetAndPath parse = AssetPathResolver.newInstance()
                    .resolve(url, APILocator.systemUser());
            assertEquals("/(testFolder)/example.txt", parse.path());
            assertNotNull(parse.asset());
            assertEquals("example.txt", parse.asset());
        } finally {
            FolderDataGen.remove(folder);
        }
    }


}