package com.dotcms.saml;

/**
 * This service builds the service to be reference builder on the bundle context of OSGI
 * The SAML OSGI Bundle has to provide both implementations
 * @author jsanca
 */
public interface SamlServiceBuilder {

    /**
     * Builds the SAML Configuration service
     * @return
     */
    SamlConfigurationService buildSamlConfigurationService();

    /**
     * Creates the Saml Authentication Facade for the SAML integration
     * @param identityProviderConfigurationFactory {@link IdentityProviderConfigurationFactory}
     * @param messageObserver {@link MessageObserver}
     * @param samlConfigurationService {@link SamlConfigurationService}
     * @return SamlAuthenticationService
     */
    SamlAuthenticationService buildAuthenticationService(
            IdentityProviderConfigurationFactory identityProviderConfigurationFactory,
            MessageObserver messageObserver,
            SamlConfigurationService samlConfigurationService);
}
