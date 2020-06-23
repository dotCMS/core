package com.dotcms.saml;

import com.dotcms.saml.service.external.IdentityProviderConfiguration;
import com.dotcms.saml.service.external.IdentityProviderConfigurationFactory;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DotCMS implementation for the {@link IdentityProviderConfigurationFactory}
 * @author jsanca
 */
public class DotIdentityProviderConfigurationFactoryImpl implements IdentityProviderConfigurationFactory {

    private final AppsAPI appsAPI;
    private final HostAPI hostAPI;
    // todo: this must be not in this way, must be totally stateless, can not store the secrets on the POJO, create the POJO everything and do not save it.
    private final Map<String, IdentityProviderConfiguration> identityProviderConfigurationMap = new ConcurrentHashMap<>();

    public DotIdentityProviderConfigurationFactoryImpl(final AppsAPI appsAPI, final HostAPI hostAPI) {

        this.appsAPI = appsAPI;
        this.hostAPI = hostAPI;
    }

    @Override
    public IdentityProviderConfiguration findIdentityProviderConfigurationById(
            final String identityProviderIdentifier) {

        IdentityProviderConfiguration identityProviderConfiguration = null;

        if (UtilMethods.isSet(identityProviderIdentifier)) {

            boolean existsIdpConfig = this.identityProviderConfigurationMap.containsKey(identityProviderIdentifier);
            if (!existsIdpConfig) {

                existsIdpConfig = this.createIdentityProviderConfigurationFor(identityProviderIdentifier);
            }

            if (existsIdpConfig) {

                identityProviderConfiguration = this.identityProviderConfigurationMap.get(identityProviderConfiguration);
            }
        } else {

            throw new IllegalArgumentException( "Idp Identifier is required." );
        }

        return identityProviderConfiguration;
    }

    private synchronized boolean createIdentityProviderConfigurationFor(final String identityProviderIdentifier) {

        final Host host = Try.of(()->
                hostAPI.find(identityProviderIdentifier, APILocator.systemUser(), false)).getOrNull();

        if (null != host) {

            final Optional<AppSecrets> appSecretOpt =
                    Try.of(()->this.appsAPI.getSecrets(DotSamlConfigurationServiceImpl.DOT_SAML_DEFAULT_PROPERTIES_CONTEXT_MAP_KEY,
                            true, host, APILocator.systemUser())).getOrElseGet(e -> Optional.empty());

            if (appSecretOpt.isPresent()) {

                this.identityProviderConfigurationMap.put(identityProviderIdentifier,
                        new DotIdentityProviderConfigurationImpl(appSecretOpt.get()));

                return this.identityProviderConfigurationMap.containsKey(identityProviderIdentifier);
            }
        }

        return false;
    }
}
