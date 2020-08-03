package com.dotcms.saml;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import org.apache.felix.framework.OSGIUtil;
import org.junit.BeforeClass;
import org.junit.Test;

public class SamlConfigurationServiceTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        if (!OSGIUtil.getInstance().isInitialized()) {
            OSGIUtil.getInstance().initializeFramework(Config.CONTEXT);
        }
    }

    @Test
    public void test_getConfigAsString() {

        /*final IdentityProviderConfigurationFactory configurationFactory = new MockIdentityProviderConfigurationFactory();
        final SamlConfigurationService samlConfigurationService = DotSamlProxyFactory.getInstance().samlConfigurationService();
        final String clockSkew = samlConfigurationService.getConfigAsString(
                configurationFactory.findIdentityProviderConfigurationById("demo.dotcms.com"), SamlName.DOT_SAML_CLOCK_SKEW);

        Assert.assertEquals("10000", clockSkew);*/
    }



}
