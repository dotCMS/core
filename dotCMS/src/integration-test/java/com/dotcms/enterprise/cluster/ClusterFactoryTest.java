package com.dotcms.enterprise.cluster;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClusterFactoryTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testClusterReady_ShouldReturnTrue() {
        Assert.assertTrue(ClusterFactory.clusterReady());
    }

}
