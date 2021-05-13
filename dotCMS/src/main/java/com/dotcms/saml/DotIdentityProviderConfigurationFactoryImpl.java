package com.dotcms.saml;

import com.dotcms.security.apps.AppsAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import io.vavr.control.Try;

import java.util.Arrays;
import java.util.List;

/**
 * DotCMS implementation for the {@link IdentityProviderConfigurationFactory}
 * @author jsanca
 */
public class DotIdentityProviderConfigurationFactoryImpl implements IdentityProviderConfigurationFactory {

    private final AppsAPI appsAPI;
    private final HostAPI hostAPI;

    public DotIdentityProviderConfigurationFactoryImpl(final AppsAPI appsAPI, final HostAPI hostAPI) {

        this.appsAPI = appsAPI;
        this.hostAPI = hostAPI;
    }

    @Override
    public IdentityProviderConfiguration findIdentityProviderConfigurationById(
            final String identityProviderIdentifier) {

        return this.existsConfiguration(identityProviderIdentifier)?
                this.createIdentityProviderConfigurationFor(identityProviderIdentifier): null;
    }

    private boolean existsConfiguration(final String identityProviderIdentifier) {

        final List hosts = Host.SYSTEM_HOST.equals(identityProviderIdentifier)?
                Arrays.asList(identityProviderIdentifier):  Arrays.asList(Host.SYSTEM_HOST, identityProviderIdentifier);

        return Try.of(()->!this.appsAPI.filterSitesForAppKey(DotSamlProxyFactory.SAML_APP_CONFIG_KEY,
                hosts, APILocator.systemUser()).isEmpty()).getOrElse(false);
    }

    private IdentityProviderConfiguration createIdentityProviderConfigurationFor(final String identityProviderIdentifier) {

        final Host host = Try.of(()->
                hostAPI.find(identityProviderIdentifier, APILocator.systemUser(), false)).getOrNull();

        return null != host?Try.of(()->new DotIdentityProviderConfigurationImpl(this.appsAPI, host)).getOrNull():null;
    }
}
