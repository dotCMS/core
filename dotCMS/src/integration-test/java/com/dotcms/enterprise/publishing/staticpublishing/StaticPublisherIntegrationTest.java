package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.LicenseTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import org.junit.BeforeClass;
import org.junit.Test;

public class StaticPublisherIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void publish(){

    }
}
