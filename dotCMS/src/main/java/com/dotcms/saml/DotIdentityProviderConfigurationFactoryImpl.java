package com.dotcms.saml;

import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import io.vavr.control.Try;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DotCMS implementation for the {@link IdentityProviderConfigurationFactory}
 * @author jsanca
 */
public class DotIdentityProviderConfigurationFactoryImpl implements IdentityProviderConfigurationFactory {

    private static final String DOTCMS_SAML_USE_IDP_CONFIG_ID = "dotcms.saml.use.idp.config.id";
    private static final String IDP_CONFIG_IDENTIFIER = "idp.config.identifier";

    private final AppsAPI appsAPI;
    private final HostAPI hostAPI;

    public DotIdentityProviderConfigurationFactoryImpl(final AppsAPI appsAPI, final HostAPI hostAPI) {

        this.appsAPI = appsAPI;
        this.hostAPI = hostAPI;
    }

    @Override
    public IdentityProviderConfiguration findIdentityProviderConfigurationById(
            final String identityProviderIdentifier) {

        IdentityProviderConfiguration identityProviderConfiguration =
                this.existsConfigurationForSite(identityProviderIdentifier)?
                    this.createIdentityProviderConfigurationForSite(identityProviderIdentifier): null;

        // if the configuration is not found for the site, then try to find it by config id
        // when the flag to find by config id is enabled
        if (null == identityProviderConfiguration &&
                Config.getBooleanProperty(DOTCMS_SAML_USE_IDP_CONFIG_ID, false)) {
            identityProviderConfiguration =
                    this.createIdentityProviderConfigurationForConfigId(identityProviderIdentifier);
        }

        return identityProviderConfiguration;
    }

    private boolean existsConfigurationForSite(final String identityProviderIdentifier) {

        final List<String> hosts = Host.SYSTEM_HOST.equals(identityProviderIdentifier)?
                List.of(identityProviderIdentifier) :  Arrays.asList(Host.SYSTEM_HOST, identityProviderIdentifier);

        return Try.of(()->!this.appsAPI.filterSitesForAppKey(DotSamlProxyFactory.SAML_APP_CONFIG_KEY,
                hosts, APILocator.systemUser()).isEmpty()).getOrElse(false);
    }

    private IdentityProviderConfiguration createIdentityProviderConfigurationForSite(final String identityProviderIdentifier) {

        final Host host = Try.of(()->
                hostAPI.find(identityProviderIdentifier, APILocator.systemUser(), false)).getOrNull();

        return null != host?Try.of(()->new DotIdentityProviderConfigurationImpl(this.appsAPI, host)).getOrNull():null;
    }

    private IdentityProviderConfiguration createIdentityProviderConfigurationForConfigId(
            final String identityProviderIdentifier) {

        final List<Host> sites = Try.of(()-> this.appsAPI.appKeysByHost().entrySet().stream()
                .filter(entry -> entry.getValue().contains(DotSamlProxyFactory.SAML_APP_CONFIG_KEY))
                .map(entry ->
                        Try.of(()-> hostAPI.find(
                                entry.getKey(), APILocator.systemUser(), false)).getOrNull())
                .filter(Objects::nonNull)
                .collect(Collectors.toList())).getOrElse(List.of());

        final Host siteWithConfigId = sites.stream()
                .filter(host -> {
                    final Optional<AppSecrets> appSecretsOptional = Try.of(() ->
                        this.appsAPI.getSecrets(DotSamlProxyFactory.SAML_APP_CONFIG_KEY,
                            host, APILocator.systemUser())).getOrElse(Optional.empty());
                    if (appSecretsOptional.isPresent()) {
                        final AppSecrets appSecrets = appSecretsOptional.get();
                        if (appSecrets.getSecrets().containsKey(IDP_CONFIG_IDENTIFIER)) {
                            final Optional<Secret> secretOptional =  Optional.ofNullable(
                                    appSecrets.getSecrets().get(IDP_CONFIG_IDENTIFIER));
                            return secretOptional.isPresent() &&
                                secretOptional.get().getString().equals(identityProviderIdentifier);
                        }
                    }
                    return false;
                }).findFirst().orElse(null);

        if (null == siteWithConfigId) {
            return null;
        }

        return Try.of(() ->
                new DotIdentityProviderConfigurationImpl(this.appsAPI, siteWithConfigId)).getOrNull();
    }

}
