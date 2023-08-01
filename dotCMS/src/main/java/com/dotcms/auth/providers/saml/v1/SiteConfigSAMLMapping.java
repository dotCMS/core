package com.dotcms.auth.providers.saml.v1;

import com.dotcms.saml.IdentityProviderConfigurationFactory;

/**
 * Took another config id and returns a new one based on site id
 * @param otherConfigId String
 * @return String
 */
@FunctionalInterface
public interface SiteConfigSAMLMapping {

    String apply (final String otherConfigId, final IdentityProviderConfigurationFactory identityProviderConfigurationFactory);
}
