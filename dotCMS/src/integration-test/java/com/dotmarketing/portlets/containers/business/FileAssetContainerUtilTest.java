package com.dotmarketing.portlets.containers.business;

import com.dotcms.datagen.ContainerAsFileDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.HostUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class FileAssetContainerUtilTest extends ContentletBaseTest {

    /**
     * Method to Test: {@link FileAssetContainerUtil#getFullPath(FileAssetContainer)}
     * When: Create a new {@link FileAssetContainer}
     * should: return the full path
     *
     * @throws Exception
     */
    @Test
    public void testGetFullPathMethod() throws Exception {

        final Host host = new SiteDataGen().nextPersisted();

        final FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen()
                .host(host)
                .nextPersisted();

        final FileAssetContainer container = (FileAssetContainer) APILocator.getContainerAPI()
                .find(fileAssetContainer.getInode(), APILocator.systemUser(), true);

        final String fullPath = FileAssetContainerUtil.getInstance().getFullPath(container);

        final String expected = "//" + host.getHostname() + (container).getPath();

        Assert.assertEquals(expected, fullPath);
    }

    @Test
    public void test_getPathFromFullPath_wrong_host_on_full_path() throws Exception {

        final String relativePath = FileAssetContainerUtil.getInstance().getPathFromFullPath("demo.dotcms.com","//global.dotcms.com/application/container/test-container");

        Assert.assertNotNull(relativePath);
        Assert.assertEquals(relativePath, "//global.dotcms.com/application/container/test-container");
    }

    @Test
    public void test_getPathFromFullPath_right_host_on_full_path() throws Exception {

        final String relativePath = FileAssetContainerUtil.getInstance().getPathFromFullPath("demo.dotcms.com","//demo.dotcms.com/application/container/test-container");

        Assert.assertNotNull(relativePath);
        Assert.assertEquals(relativePath, "/application/container/test-container");
    }

    @Test
    public void test_getPathFromFullPath_any_host_on_relative_path() throws Exception {

        final String relativePath = FileAssetContainerUtil.getInstance().getPathFromFullPath("demo.dotcms.com","/application/container/test-container");

        Assert.assertNotNull(relativePath);
        Assert.assertEquals(relativePath, "/application/container/test-container");
    }

    @Test
    public void test_getHost_null_default_expected() throws Exception {

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Host host        = FileAssetContainerUtil.getInstance().getHost(null);

        Assert.assertNotNull(host);
        Assert.assertEquals(defaultHost.getHostname(), host.getHostname());
    }

    @Test
    public void test_getHost_blank_default_expected() throws Exception {

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Host host        = FileAssetContainerUtil.getInstance().getHost("");

        Assert.assertNotNull(host);
        Assert.assertEquals(defaultHost.getHostname(), host.getHostname());
    }

    @Test
    public void test_getHost_ramdom_default_expected() throws Exception {

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Host host        = FileAssetContainerUtil.getInstance().getHost("/xxxxxx");

        Assert.assertNotNull(host);
        Assert.assertEquals(defaultHost.getHostname(), host.getHostname());
    }

    @Test
    public void test_getHost_demo_default_demo() throws Exception {

        //Getting the current default host
        final Host currentDefaultHost = APILocator.getHostAPI()
                .findDefaultHost(APILocator.systemUser(), false);

        try {
            //Create test site and make it default
            final Host site = new SiteDataGen().nextPersisted();
            APILocator.getHostAPI().makeDefault(site, APILocator.systemUser(), false);

            final Host host = FileAssetContainerUtil.getInstance()
                    .getHost("//" + site.getHostname() + "/application/containers/test/");

            Assert.assertNotNull(host);
            Assert.assertEquals(site.getHostname(), host.getHostname());
        } finally {
            APILocator.getHostAPI().makeDefault(currentDefaultHost, APILocator.systemUser(), false);
        }
    }



}
