package com.dotcms.saml;

import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import org.apache.felix.framework.OSGIUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.UnrecoverableKeyException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IdentityProviderConfigurationFactoryTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        if (!OSGIUtil.getInstance().isInitialized()) {
            OSGIUtil.getInstance().initializeFramework();
        }
    }

    @Test
    public void test_UnrecoverableKeyException() {

        final AppsAPI appsAPI = mock(AppsAPI.class);
        final HostAPI hostAPI = mock(HostAPI.class);

        when(appsAPI.filterSitesForAppKey(anyString(),
                anyCollection(), any())).thenThrow(new RuntimeException(new UnrecoverableKeyException()));
        final IdentityProviderConfigurationFactory configurationFactory   = new DotIdentityProviderConfigurationFactoryImpl(appsAPI, hostAPI);
        final IdentityProviderConfiguration identityProviderConfiguration = configurationFactory.findIdentityProviderConfigurationById("xxx");

        Assert.assertNull(identityProviderConfiguration);
    }

    @Test
    public void test_findIdentityProviderConfigurationByConfigId() throws Exception {

        final AppsAPI appsAPI = mock(AppsAPI.class);
        final HostAPI hostAPI = mock(HostAPI.class);

        Config.setProperty("dotcms.saml.use.idp.config.id", true);

        final String testConfigId = "792c699b-5025-4869-9ae8-2ff78f245fe9";

        when(appsAPI.filterSitesForAppKey(anyString(),
                anyCollection(), any())).thenReturn(Set.of());

        final String testHostId = "5b54cc87-eab8-466b-814a-a621adf695d8";
        final Map<String, Set<String>> keysByHost = Map.of(testHostId,
                Set.of(DotSamlProxyFactory.SAML_APP_CONFIG_KEY));
        when(appsAPI.appKeysByHost()).thenReturn(keysByHost);

        final Host testHost = mock(Host.class);
        when(testHost.getIdentifier()).thenReturn(testHostId);
        when(hostAPI.find(anyString(), any(), anyBoolean())).thenReturn(testHost);

        final AppSecrets appSecrets = mock(AppSecrets.class);
        when(appSecrets.getSecrets()).thenReturn(
                Map.of(
                        "idp.config.identifier",
                        Secret.builder().withValue(testConfigId).withHidden(false).withType(Type.STRING).build()));
        when(appsAPI.getSecrets(anyString(), any(), any())).thenReturn(Optional.of(appSecrets));
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(), any())).thenReturn(Optional.of(appSecrets));

        final IdentityProviderConfigurationFactory configurationFactory =
                new DotIdentityProviderConfigurationFactoryImpl(appsAPI, hostAPI);
        final IdentityProviderConfiguration identityProviderConfiguration =
                configurationFactory.findIdentityProviderConfigurationById(testConfigId);

        Assert.assertNotNull(identityProviderConfiguration);
    }
}
