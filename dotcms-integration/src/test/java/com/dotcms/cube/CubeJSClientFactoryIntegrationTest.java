package com.dotcms.cube;

import com.dotcms.IntegrationTestBase;
import com.dotcms.analytics.AnalyticsTestUtils;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class CubeJSClientFactoryIntegrationTest extends IntegrationTestBase {

    private CubeJSClientFactoryImpl cubeJSClientFactory;
    private AnalyticsHelper analyticsHelper;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setup() throws Exception {
        cubeJSClientFactory = new CubeJSClientFactoryImpl();
        analyticsHelper = AnalyticsTestUtils.mockAnalyticsHelper();
        CubeJSClientFactoryImpl.setAnalyticsHelper(analyticsHelper);
    }

    @Test
    public void testCreateWithAnalyticsApp() throws Exception {
        final CubeJSClient cubeClient = cubeJSClientFactory.create(
            analyticsHelper.appFromHost(
                WebAPILocator.getHostWebAPI().getCurrentHost()));
        assertNotNull(cubeClient);
    }

    @Test
    public void testCreateWithUser() throws Exception {
        final CubeJSClient cubeClient = cubeJSClientFactory.create(APILocator.systemUser());
        assertNotNull(cubeClient);
    }

}
