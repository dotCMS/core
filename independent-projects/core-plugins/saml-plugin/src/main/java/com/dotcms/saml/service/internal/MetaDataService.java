package com.dotcms.saml.service.internal;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.service.external.MetaData;
import org.opensaml.security.credential.Credential;

import java.util.Collection;

/**
 * Service to interact with the Metadata
 * @author jsanca
 */
public interface MetaDataService {

    /**
     * Returns the metadata for the actual IDP
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return MetaData
     */
    MetaData getMetaData(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Returns the MetaData service to parse and recover the actual metadata associated to the actual IDP
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return MetaDescriptorService
     */
    MetaDescriptorService getMetaDescriptorService(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Gets the sigining credentials from the IDP metadata
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return Collection of {@link Credential}
     */
    Collection<Credential> getSigningCredentials(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Returns the SSO destination URL (to do the authentication) from the IDP metadada
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return String
     */
    String getIdentityProviderDestinationSSOURL(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Returns the SLO destination URL (to do the logout) from the IDP metadata
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return String
     */
    String getIdentityProviderDestinationSLOURL(IdentityProviderConfiguration identityProviderConfiguration);
}
