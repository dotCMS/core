package com.dotcms.saml.service.internal;

import com.dotcms.saml.IdentityProviderConfiguration;

/**
 * Encapsulates all related to Credentials
 * @author jsanca
 */
public interface CredentialService {

    /**
     * In case you need a custom credentials for the ID Provider (dotCMS)
     * overrides the implementation class on the configuration. By default it
     * uses the Idp metadata credentials info, from the XML to figure out this
     * info.
     *
     * @param identityProviderConfiguration IdentityProviderConfiguration
     * @return CredentialProvider
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    CredentialProvider getIdProviderCustomCredentialProvider(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * In case you need custom credentials for the Service Provider (DotCMS)
     * overwrites the implementation class on the configuration. By default it
     * uses a Trust Storage to get the keys and creates the credential.
     *
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return CredentialProvider
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    CredentialProvider getServiceProviderCustomCredentialProvider(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * If the user wants to do a verifyAssertionSignature, by default true.
     * There are some testing or diagnostic scenarios where you want to avoid
     * the validation to identified issues, but in general on production this
     * must be true.
     *
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return boolean
     */
    boolean isVerifyAssertionSignatureNeeded(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * If the user wants to do a verifyResponseSignature, by default true.
     * There are some testing or diagnostic scenarios where you want to avoid
     * the validation to identified issues, but in general on production this
     * must be true.
     *
     * @param identityProviderConfiguration identityProviderConfiguration
     * @return boolean
     */
    boolean isVerifyResponseSignatureNeeded(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * If the user wants to do a verifySignatureCredentials, by default true
     * There are some testing or diagnostic scenarios where you want to avoid
     * the validation to identified issues, but in general on production this
     * must be true. Note: if isVerifyAssertionSignatureNeeded is true, this is
     * also skipped.
     *
     * @param identityProviderConfiguration IdentityProviderConfiguration
     * @return boolean
     */
    boolean isVerifySignatureCredentialsNeeded(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * If the user wants to do a verifySignatureProfile, by default true There
     * are some testing or diagnostic scenarios where you want to avoid the
     * validation to identified issues, but in general on production this must
     * be true. Note: if isVerifyAssertionSignatureNeeded is true, this is also
     * skipped.
     *
     * @param identityProviderConfiguration IdentityProviderConfiguration
     * @return boolean
     */
    boolean isVerifySignatureProfileNeeded(IdentityProviderConfiguration identityProviderConfiguration);
}
