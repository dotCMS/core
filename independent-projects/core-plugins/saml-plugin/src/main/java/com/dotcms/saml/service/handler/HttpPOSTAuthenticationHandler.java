package com.dotcms.saml.service.handler;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.MessageObserver;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.impl.DotHTTPPOSTDeflateEncoder;
import com.dotcms.saml.service.internal.SamlCoreService;
import com.dotcms.saml.utils.AuthnRequestStateCookie;
import com.dotcms.saml.utils.SamlUtils;
import com.dotcms.saml.utils.SignatureUtils;
import com.dotmarketing.util.UtilMethods;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Implements the authentication handler by POST
 * This class creates the XML associated the AuthRequest to send the login request to the IDP
 * @author jsanca
 */
public class HttpPOSTAuthenticationHandler implements AuthenticationHandler {

    private final SamlCoreService samlCoreService;
    private final VelocityEngine  velocityEngine;
    private final MessageObserver messageObserver;

    public HttpPOSTAuthenticationHandler(final SamlCoreService samlCoreService,
                                         final VelocityEngine velocityEngine,
                                         final MessageObserver messageObserver) {

        this.samlCoreService = samlCoreService;
        this.velocityEngine  = velocityEngine;
        this.messageObserver = messageObserver;
    }

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response,
                       final IdentityProviderConfiguration identityProviderConfiguration,
                       final String relayState) {

        final MessageContext context    = new MessageContext(); // main context
        final AuthnRequest authnRequest = this.samlCoreService.buildAuthnRequest(request, identityProviderConfiguration, SAMLConstants.SAML2_POST_BINDING_URI);
        // lets the SAML Response be bound to this request (InResponseTo) when the IdP posts it back
        AuthnRequestStateCookie.remember(request, response, identityProviderConfiguration, authnRequest.getID());

        // the client can ask to sign or not the request
        final boolean needSign = identityProviderConfiguration.containsOptionalProperty("auth.sign.request")?
                Boolean.parseBoolean(identityProviderConfiguration.getOptionalProperty("auth.sign.request").toString()): false;

        if (needSign) {
        // in case the sign is needed here is the logic for
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

        // Saml can also send an extra params signed
        final boolean needSignatureSigningParams = identityProviderConfiguration.containsOptionalProperty("auth.sign.params")?
                Boolean.parseBoolean(identityProviderConfiguration.getOptionalProperty("auth.sign.params").toString()): true;
        if (needSignatureSigningParams) {
            SignatureUtils.setSignatureSigningParams(this.samlCoreService.getCredential(identityProviderConfiguration), context);
        }

        // in case the relay state is set
        if (UtilMethods.isSet(relayState)) {

            this.messageObserver.updateDebug(this.getClass().getName(), "Setting the relay state: " + relayState);
            SAMLBindingSupport.setRelayState(context, relayState);
        }
        this.doPost(context, response, authnRequest, identityProviderConfiguration);
    }

    private Signature createSignature(final IdentityProviderConfiguration identityProviderConfiguration) {

        final X509Credential credential     = (X509Credential) this.samlCoreService.getCredential(identityProviderConfiguration);
        final String signatureAlgorithm = identityProviderConfiguration.containsOptionalProperty("auth.signature.algorithm")?
                (String)identityProviderConfiguration.getOptionalProperty("auth.signature.algorithm"):SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256;

        return SignatureUtils.createSignature(this.samlCoreService, this.messageObserver, credential, signatureAlgorithm);
    }

    // this makes the post to the IdP
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doPost(final MessageContext context, final HttpServletResponse response,
                        final XMLObject xmlObject, final IdentityProviderConfiguration idpConfig) {
        final HTTPPostEncoder encoder;

        try {
            encoder = new DotHTTPPOSTDeflateEncoder(this.velocityEngine);

            encoder.setMessageContext(context);
            encoder.setHttpServletResponse(response);

            encoder.initialize();

            this.messageObserver.updateDebug(this.getClass().getName(), "Printing XMLObject:");
            this.messageObserver.updateDebug(this.getClass().getName(), "\n\n" + SamlUtils.toXMLObjectString(xmlObject));
            this.messageObserver.updateDebug(this.getClass().getName(), "Posting to IdP '" + idpConfig.getIdpName() + "'");


            encoder.encode();
        } catch (ComponentInitializationException | MessageEncodingException e) {

            final String errorMsg = "An error occurred when executing Posting to IdP '" +
                    idpConfig.getIdpName() + "': " + e.getMessage();
            this.messageObserver.updateError(this.getClass().getName(), errorMsg, e);
            throw new SamlException(errorMsg, e);
        }
    }
}
