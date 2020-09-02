package com.dotcms.saml;

/**
 * This factory is in charge of provide the configuration associated to the identifier in the parameters.
 * @author jsanca
 */
public interface IdentityProviderConfigurationFactory {

    /**
     * Returns the configuration for the identifier passed as a parameters
     * @param id String
     * @return IdentityProviderConfiguration
     */
    IdentityProviderConfiguration findIdentityProviderConfigurationById(String id);
}
