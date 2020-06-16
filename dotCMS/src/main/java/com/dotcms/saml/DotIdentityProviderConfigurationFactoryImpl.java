package com.dotcms.saml;

import com.dotcms.saml.service.external.IdentityProviderConfiguration;
import com.dotcms.saml.service.external.IdentityProviderConfigurationFactory;
import com.dotcms.security.apps.AppsAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;

/**
 * DotCMS implementation for the {@link IdentityProviderConfigurationFactory}
 * @author jsanca
 */
public class DotIdentityProviderConfigurationFactoryImpl implements IdentityProviderConfigurationFactory {

    private final AppsAPI appsAPI;

    public DotIdentityProviderConfigurationFactoryImpl(final AppsAPI appsAPI) {

        this.appsAPI = appsAPI;
    }

    @Override
    public IdentityProviderConfiguration findIdentityProviderConfigurationById(
            final String identityProviderIdentifier) {

        IdentityProviderConfiguration identityProviderConfiguration = null;

        if (UtilMethods.isSet(identityProviderIdentifier)) {

            this.appsAPI.appKeysByHost()
        } else {

            throw new IllegalArgumentException( "Idp Identifier is required." );
        }

        return identityProviderConfiguration;
    }
}
