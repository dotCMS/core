package com.dotcms.saml.service.handler;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.MessageObserver;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.impl.DotHTTPRedirectDeflateEncoder;
import com.dotcms.saml.service.internal.SamlCoreService;
import com.dotcms.saml.utils.AuthnRequestStateCookie;
import com.dotcms.saml.utils.SamlUtils;
import com.dotcms.saml.utils.SignatureUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import org.apache.velocity.context.Context;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPRedirectDeflateEncoder;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Implements the authentication handler by redirect
 * @author jsanca
 */
public class HttpRedirectAuthenticationHandler implements AuthenticationHandler {

    private final SamlCoreService samlCoreService;
    private final MessageObserver messageObserver;
    private final SamlConfigurationService samlConfigurationService;


    public HttpRedirectAuthenticationHandler(final SamlCoreService samlCoreService,
                                             final MessageObserver messageObserver,
                                             final SamlConfigurationService samlConfigurationService) {

        this.samlCoreService = samlCoreService;
        this.messageObserver = messageObserver;
        this.samlConfigurationService = samlConfigurationService;
    }

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response,
                       final IdentityProviderConfiguration identityProviderConfiguration,
                       final String relayState) {

        final MessageContext context    = new MessageContext(); // main context
        final AuthnRequest authnRequest = this.samlCoreService.buildAuthnRequest(request, identityProviderConfiguration);
        // lets the SAML Response be bound to this request (InResponseTo) when the IdP posts it back
        AuthnRequestStateCookie.remember(request, response, identityProviderConfiguration, authnRequest.getID());

        // in case sign is need on request
        final boolean needSign = identityProviderConfiguration.containsOptionalProperty("auth.sign.request")?
                Boolean.parseBoolean(identityProviderConfiguration.getOptionalProperty("auth.sign.request").toString()): false;

        if (needSign) {

            try {

                final Signature signature = this.createSignature(identityProviderConfiguration);

                this.messageObserver.updateInfo(this.getClass().getName(), "signature: " + signature);
                authnRequest.setSignature(signature);

                // Marshall and Sign
                final String digestAlgorithm = identityProviderConfiguration.containsOptionalProperty("auth.signature.reference.digestmethod.algorithm") ?
                        (String) identityProviderConfiguration.getOptionalProperty("auth.signature.reference.digestmethod.algorithm") :
                        SignatureConstants.ALGO_ID_DIGEST_SHA256;
                SignatureUtils.marshalAndSing(authnRequest, this.messageObserver, signature, digestAlgorithm);
            } catch (Exception e) {

                this.messageObserver.updateError(this.getClass().getName(), e.getMessage(), e);
            }
        }

        context.setMessage(authnRequest);

        // peer entity (Idp to SP and viceversa)
        final SAMLPeerEntityContext peerEntityContext = context.getSubcontext(SAMLPeerEntityContext.class, true);
        // info about the endpoint of the peer entity
        final SAMLEndpointContext endpointContext     = peerEntityContext.getSubcontext(SAMLEndpointContext.class, true);

        endpointContext.setEndpoint(this.samlCoreService.getIdentityProviderDestinationEndpoint(identityProviderConfiguration));

        // this was made in other to have some kinda of tracking on the session, but not sure if it is useful
        final HttpSession session = request.getSession();
        session.setAttribute("dotsaml.login", true);
        final boolean needSignatureSigningParams = identityProviderConfiguration.containsOptionalProperty("auth.sign.params")?
                Boolean.parseBoolean(identityProviderConfiguration.getOptionalProperty("auth.sign.params").toString()): true;
        if (needSignatureSigningParams) {
            SignatureUtils.setSignatureSigningParams(this.samlCoreService.getCredential(identityProviderConfiguration), context);
        }

        if (UtilMethods.isSet(relayState)) {

            this.messageObserver.updateDebug(this.getClass().getName(), "Setting the relay state: " + relayState);
            SAMLBindingSupport.setRelayState(context, relayState);
        }
        this.doRedirect(context, response, authnRequest, identityProviderConfiguration);
    }

    private Signature createSignature(final IdentityProviderConfiguration identityProviderConfiguration) {

        final X509Credential credential     = (X509Credential) this.samlCoreService.getCredential(identityProviderConfiguration);
        final String signatureAlgorithm = identityProviderConfiguration.containsOptionalProperty("auth.signature.algorithm")?
                (String)identityProviderConfiguration.getOptionalProperty("auth.signature.algorithm"):SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256;

        return SignatureUtils.createSignature(this.samlCoreService, this.messageObserver, credential, signatureAlgorithm);
    }

    // this makes the redirect to the IdP
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void doRedirect(final MessageContext context, final HttpServletResponse response,
                              final XMLObject xmlObject,
                              final IdentityProviderConfiguration identityProviderConfiguration) {

        final boolean clearQueryParams = samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration,
                SamlName.DOTCMS_SAML_CLEAR_LOCATION_QUERY_PARAMS);

        try {

            final HTTPRedirectDeflateEncoder encoder = new DotHTTPRedirectDeflateEncoder(
                    clearQueryParams, this.messageObserver);

            encoder.setMessageContext(context);
            encoder.setHttpServletResponse(response);

            encoder.initialize();

            this.messageObserver.updateDebug(this.getClass().getName(), "Printing XMLObject:");
            this.messageObserver.updateDebug(this.getClass().getName(), "\n\n" + SamlUtils.toXMLObjectString(xmlObject));
            this.messageObserver.updateDebug(this.getClass().getName(), "Redirecting to IdP '" + identityProviderConfiguration.getIdpName() + "'");

            encoder.encode();
        } catch (ComponentInitializationException | MessageEncodingException e) {

            final String errorMsg = "An error occurred when executing redirect to IdP '" +
                    identityProviderConfiguration.getIdpName() + "': " + e.getMessage();
            this.messageObserver.updateError(this.getClass().getName(), errorMsg, e);
            throw new SamlException(errorMsg, e);
        }
    }
}
