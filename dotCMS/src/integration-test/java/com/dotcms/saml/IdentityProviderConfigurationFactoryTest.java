package com.dotcms.saml;

import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import org.apache.felix.framework.OSGIUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.UnrecoverableKeyException;

import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IdentityProviderConfigurationFactoryTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        if (!OSGIUtil.getInstance().isInitialized()) {
            OSGIUtil.getInstance().initializeFramework(Config.CONTEXT);
        }
    }

    @Test
    public void test_UnrecoverableKeyException() {

        final AppsAPI appsAPI = mock(AppsAPI.class);
        final HostAPI hostAPI = mock(HostAPI.class);

        when(appsAPI.filterSitesForAppKey(anyString(),
                anyCollection(), anyObject())).thenThrow(new RuntimeException(new UnrecoverableKeyException()));
        final IdentityProviderConfigurationFactory configurationFactory   = new DotIdentityProviderConfigurationFactoryImpl(appsAPI, hostAPI);
        final IdentityProviderConfiguration identityProviderConfiguration = configurationFactory.findIdentityProviderConfigurationById("xxx");

        Assert.assertNull(identityProviderConfiguration);
    }
}
