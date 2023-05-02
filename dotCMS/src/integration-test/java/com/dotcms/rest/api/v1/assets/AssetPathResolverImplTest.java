package com.dotcms.rest.api.v1.assets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.rest.api.v1.asset.AssetPathResolver;
import com.dotcms.rest.api.v1.asset.ResolvedAssetAndPath;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
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
        new FolderDataGen().parent(foo).name("bar").nextPersisted();
    }

    /**
     * This is special case and needs to be solved separately Cuz FolderAPI always return system
     * folder when path is "/" regardless of host
     *
     * @throws DotDataException
     */
    @Test
    public void Test_Parse_Host_Followed_By_Root() throws DotDataException {
        final String url = String.format("http://%s/", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.getInstance()
                .resolve(url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/", parse.path());
        assertNull(parse.asset());
    }

    @Test
    public void Test_Parse_Path() throws DotDataException {

        final String url = String.format("http://%s/foo/bar/1234", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.getInstance().resolve(
                url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/bar/1234", parse.path());
        assertEquals("1234", parse.asset());
    }

    @Test
    public void Test_Parse_Path_With_Resource_And_Extension() throws DotDataException {
        final String url = String.format("http://%s/foo/bar/1234.webp", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.getInstance().resolve(
                url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/bar/1234.webp", parse.path());
        assertEquals("1234.webp", parse.asset());
    }

    @Test
    public void Test_Parse_Path_Shorten_No_Protocol_Host_Name() throws DotDataException {
        final String url = String.format("//%s/foo/bar/1234", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.getInstance().resolve(
                url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/bar/1234", parse.path());
        assertEquals("1234", parse.asset());
    }

    @Test
    public void Test_Parse_Path_Ending_In_Slash() throws DotDataException {
        final String url = String.format("//%s/foo/bar/1234/", host.getHostname());
        final ResolvedAssetAndPath parse = AssetPathResolver.getInstance().resolve(
                url, APILocator.systemUser());
        assertEquals(host.getHostname(), parse.host());
        assertEquals("/foo/bar/1234/", parse.path());
        assertNull(parse.asset());
    }

    @Test
    public void Test_Parse_Path_Simple_Root() throws DotDataException {
        final ResolvedAssetAndPath parse = AssetPathResolver.getInstance()
                .resolve("//", APILocator.systemUser());
        assertEquals(Host.SYSTEM_HOST, parse.host());
        assertNull(parse.path());
        assertNull(parse.asset());
    }

}