package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.ContentletBaseTest;
import org.junit.Assert;
import org.junit.Test;

public class FileAssetContainerUtilTest extends ContentletBaseTest {


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

        final Host host        = FileAssetContainerUtil.getInstance().getHost("//demo.dotcms.com/application/containers/test/");

        Assert.assertNotNull(host);
        Assert.assertEquals("demo.dotcms.com", host.getHostname());
    }



}
