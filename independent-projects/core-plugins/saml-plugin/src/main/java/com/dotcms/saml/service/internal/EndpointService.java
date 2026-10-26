package com.dotcms.saml.service.internal;

import com.dotcms.saml.IdentityProviderConfiguration;

public interface EndpointService {

    /**
     * In case the user wants some specific customer url, otherwise null. This
     * URL is used on the metadata to fill out the AssertionConsumerService
     *
     * @param identityProviderConfiguration IdentityProviderConfiguration
     * @return String
     */
    String getAssertionConsumerEndpoint(IdentityProviderConfiguration identityProviderConfiguration);


    /**
     * In case the user wants some specific logout url, otherwise null. This URL
     * is used on the metadata to fill out the assertion customer service
     *
     * We are assuming that the issuerUrl which is posted by the Idp and
     * SingleLogoutEndpoint which is also posted by the Idp will be on the
     * same domain and port.
     *
     * @param identityProviderConfiguration IdpConfig
     * @return String
     */
    String getSingleLogoutEndpoint(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Get's the access filter array, which are the exceptional cases to avoid
     * to evaluate on the Saml Web Logic For instance if
     * you include a file that shouldn't need any mapping, you can use it.
     *
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return String []
     */
    String[] getAccessFilterArray(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Returns the logout paths
     *
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return String[]
     */
    String[] getLogoutPathArray(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Get's the include urls to be analyzed by the open saml plugin, usually
     * the admin They can be a pattern
     *
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return String []
     */
    String[] getIncludePathArray(IdentityProviderConfiguration identityProviderConfiguration);
}
