package com.dotmarketing.portlets.containers.business;

import com.dotcms.datagen.ContainerAsFileDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import org.junit.Assert;
import org.junit.Test;

public class FileAssetContainerUtilTest extends ContentletBaseTest {

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
