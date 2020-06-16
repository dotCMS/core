package com.dotcms.saml;

import com.dotcms.saml.service.external.IdentityProviderConfigurationFactory;
import com.dotcms.saml.service.external.MessageObserver;
import com.dotcms.saml.service.external.SamlAuthenticationService;
import com.dotcms.saml.service.external.SamlConfigurationService;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.external.SamlServiceBuilder;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppsAPI;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DotSamlFactory {

    public static final String SAML_APP_CONFIG_KEY = "dotsaml-config";

    private final Map<Class, Object> instanceMap        = new ConcurrentHashMap<>();
    private final SamlServiceBuilder samlServiceBuilder = null; // todo: get this from osgi reference
    private final MessageObserver    messageObserver    = new DotLoggerMessageObserver();
    private final AppsAPI            appsAPI            = APILocator.getAppsAPI();


    private static class SingletonHolder {
        private static final DotSamlFactory INSTANCE = new DotSamlFactory();
    }
    /**
     * Get the instance.
     * @return DotSamlFactory
     */
    public static DotSamlFactory getInstance() {

        return DotSamlFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    public IdentityProviderConfigurationFactory IdentityProviderConfigurationFactory() {

        return null;
    }

    public MessageObserver messageObserver() {

        return this.messageObserver;
    }

    public SamlConfigurationService samlConfigurationService() {

        return null;
    }

    public SamlAuthenticationService samlAuthenticationService() {

        if (this.isAnyHostConfiguredAsSAML()) {

            if (!this.instanceMap.containsKey(SamlAuthenticationService.class)) {

                this.instanceMap.put(SamlAuthenticationService.class,
                        samlServiceBuilder.buildAuthenticationService(this.IdentityProviderConfigurationFactory(),
                                this.messageObserver(), this.samlConfigurationService()));
            }

            return (SamlAuthenticationService) this.instanceMap.get(SamlAuthenticationService.class);
        }

        throw new SamlException("Not any host has been configured as a SAML");
    }

    /**
     * Returns true is any host is configured as a SAML
     * @return boolean
     */
    public boolean isAnyHostConfiguredAsSAML () {

        boolean isAnyConfigured = false;
        final User user         = APILocator.systemUser();

        final Optional<AppDescriptor> appDescriptorOptional = Try.of(
                ()-> this.appsAPI
                        .getAppDescriptor(SAML_APP_CONFIG_KEY, user)).getOrElseGet(e-> Optional.empty());
        if (appDescriptorOptional.isPresent()) {

            final AppDescriptor appDescriptor = appDescriptorOptional.get();

            final Map<String, Set<String>>  appKeysByHost = Try.of(()-> this.appsAPI.appKeysByHost())
                    .getOrElseGet(e -> Collections.emptyMap());
            final Set<String> sitesWithConfigurations     = this.appsAPI
                    .filterSitesForAppKey(appDescriptor.getKey(), appKeysByHost.keySet(), user);

            isAnyConfigured                   = !sitesWithConfigurations.isEmpty();
        }

        return isAnyConfigured;
    }

}
