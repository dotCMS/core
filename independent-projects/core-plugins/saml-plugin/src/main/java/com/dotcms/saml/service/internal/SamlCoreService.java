package com.dotcms.saml.service.internal;

import com.dotcms.saml.IdentityProviderConfiguration;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.security.credential.Credential;

import javax.servlet.http.HttpServletRequest;

/**
 * This Service encapsulates all Core the Saml Stuff
 *
 * @author jsanca
 */
public interface SamlCoreService {

    String SINGLE_LOGOUT_REASON  = "urn:oasis:names:tc:SAML:2.0:logout:user";

    /**
     * Build a SAML Object
     *
     * @param clazz
     * @param <T>
     * @return T
     */
    @SuppressWarnings("unchecked")
    <T> T buildSAMLObject(Class<T> clazz);

    /**
     * Build the logout request based on the name id and session index.
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration} idp configuration for the logout request
     * @param nameID {@link NameID} id
     * @param sessionIndexValue {@link String} the session index value was saved when the user gets login
     * @return LogoutRequest
     */
    LogoutRequest buildLogoutRequest(IdentityProviderConfiguration identityProviderConfiguration,
                                     NameID nameID,
                                     String sessionIndexValue);

    /**
     * Return the value of the /AuthnStatement@SessionIndex element in an
     * assertion
     *
     * @return The value. <code>null</code>, if the assertion does not contain
     *         the element.
     */
    String getSessionIndex(Assertion assertion);

    /**
     * Return the value of the /AuthnStatement@SessionIndex element in an
     * assertion
     *
     * @return The value. <code>null</code>, if the assertion does not contain
     *         the element.
     */
    AuthnRequest buildAuthnRequest(HttpServletRequest request,
                                   IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Return the value of the /AuthnStatement@SessionIndex element in an
     * assertion
     *
     * @return The value. <code>null</code>, if the assertion does not contain
     *         the element.
     */
    AuthnRequest buildAuthnRequest(final HttpServletRequest request,
                                          final IdentityProviderConfiguration identityProviderConfiguration,
                                          final String protocolBinding);

    /**
     * Gets from the destination sso url from the configuration.
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return String
     */
    String getIPDSSODestination(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Gets from the destination slo url from the configuration
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return String
     */
    String getIPDSLODestination(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Get the assertion consumer endpoint from the configuration.
     * @param request {@link HttpServletRequest}
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return String
     */
    String getAssertionConsumerEndpoint(HttpServletRequest request,
                                        IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Build the Id for the sender.
     *
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return Issuer
     */
    Issuer buildIssuer(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Get the id for the Issuer, it is the SP identifier on the IdP
     *
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return String
     */
    String getSPIssuerValue(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Return the policy for the Name ID (which is the IdP identifier for the
     * user)
     *
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return NameIDPolicy
     */
    NameIDPolicy buildNameIdPolicy(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Build the Authentication context, with the login and password strategies
     *
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return RequestedAuthnContext
     */
    RequestedAuthnContext buildRequestedAuthnContext(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Based on the configuration properties get the desire comparison type
     *
     * @param identityProviderConfiguration  {@link IdentityProviderConfiguration}
     * @return AuthnContextComparisonTypeEnumeration
     */
    AuthnContextComparisonTypeEnumeration getAuthnContextComparisonTypeEnumeration(
            IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Get the Logout Identity Provider Destination
     *
     * @param identityProviderConfiguration  {@link IdentityProviderConfiguration}
     * @return Endpoint
     */
    Endpoint getIdentityProviderSLODestinationEndpoint(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Get the Identity Provider Destination
     *
     * @param identityProviderConfiguration  {@link IdentityProviderConfiguration}
     * @return Endpoint
     */
    Endpoint getIdentityProviderDestinationEndpoint(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Get the Assertion decrypted
     *
     * @param artifactResponse {@link ArtifactResponse}
     * @param identityProviderConfiguration  {@link IdentityProviderConfiguration}
     * @return Assertion
     */
    Assertion getAssertion(ArtifactResponse artifactResponse,
                           IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Get the Assertion decrypted
     *
     * @param response {@link Response}
     * @param identityProviderConfiguration  {@link IdentityProviderConfiguration}
     *
     * @return Assertion
     */
    Assertion getAssertion(Response response,
                           IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Just get the Encrypted assertion from the {@link ArtifactResponse}
     *
     * @param artifactResponse {@link ArtifactResponse}
     * @return EncryptedAssertion
     */
    EncryptedAssertion getEncryptedAssertion(ArtifactResponse artifactResponse);

    /**
     * Decrypt an {@link EncryptedAssertion}
     *
     * @param encryptedAssertion {@link EncryptedAssertion}
     * @param identityProviderConfiguration  {@link IdentityProviderConfiguration}
     * @return Assertion
     */
    Assertion decryptAssertion(EncryptedAssertion encryptedAssertion,
                               IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Does the verification of the assertion
     *
     * @param assertion {@link Assertion}
     * @param identityProviderConfiguration  {@link IdentityProviderConfiguration}
     */
    void verifyAssertionSignature(Assertion assertion,
                                  IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Does the verification of the assertion
     *
     * @param response {@link Assertion}
     * @param identityProviderConfiguration  {@link IdentityProviderConfiguration}
     */
    void verifyResponseSignature(Response response, IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Creates the credential based on the configuration
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return Credential
     */
    Credential createCredential(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Get the SP credential
     *
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return Credential
     */
    Credential getCredential(IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Get the Identity provider credentials
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return Credential
     */
    Credential getIdPCredentials(IdentityProviderConfiguration identityProviderConfiguration);
}
