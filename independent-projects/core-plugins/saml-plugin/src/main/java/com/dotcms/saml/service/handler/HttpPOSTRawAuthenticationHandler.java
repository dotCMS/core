package com.dotcms.saml.service.handler;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.MessageObserver;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.impl.DotHTTPPOSTRawDeflateEncoder;
import com.dotcms.saml.service.internal.SamlCoreService;
import com.dotcms.saml.utils.AuthnRequestStateCookie;
import com.dotcms.saml.utils.SamlUtils;
import com.dotmarketing.util.UtilMethods;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.signature.support.SignatureConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Implements the authentication handler by POST
 * Todo: this needs the sign: https://github.com/dotCMS/core/issues/32099
 * @author jsanca
 */
public class HttpPOSTRawAuthenticationHandler implements AuthenticationHandler {

    private final SamlCoreService samlCoreService;
    private final MessageObserver messageObserver;

    public HttpPOSTRawAuthenticationHandler(final SamlCoreService samlCoreService,
                                            final MessageObserver messageObserver) {

        this.samlCoreService = samlCoreService;
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

        context.setMessage(authnRequest);

        // peer entity (Idp to SP and viceversa)
        final SAMLPeerEntityContext peerEntityContext = context.getSubcontext(SAMLPeerEntityContext.class, true);
        // info about the endpoint of the peer entity
        final SAMLEndpointContext endpointContext     = peerEntityContext.getSubcontext(SAMLEndpointContext.class, true);

        endpointContext.setEndpoint(this.samlCoreService.getIdentityProviderDestinationEndpoint(identityProviderConfiguration));
        // TODO: ADD LOGIC FOR needSign
        this.setSignatureSigningParams(context, identityProviderConfiguration);

        if (UtilMethods.isSet(relayState)) {

            this.messageObserver.updateDebug(this.getClass().getName(), "Setting the relay state: " + relayState);
            SAMLBindingSupport.setRelayState(context, relayState);
        }
        this.doPost(context, response, authnRequest, identityProviderConfiguration);
    }

    private void setSignatureSigningParams(final MessageContext context, final IdentityProviderConfiguration idpConfig) {

        final SignatureSigningParameters signatureSigningParameters = new SignatureSigningParameters();

        signatureSigningParameters.setSigningCredential(this.samlCoreService.getCredential(idpConfig));
        signatureSigningParameters.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);

        context.getSubcontext(SecurityParametersContext.class, true)
                .setSignatureSigningParameters(signatureSigningParameters);
    }

    // this makes the post to the IdP
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doPost(final MessageContext context, final HttpServletResponse response,
                        final XMLObject xmlObject, final IdentityProviderConfiguration idpConfig) {
        final DotHTTPPOSTRawDeflateEncoder encoder;

        try {

            this.messageObserver.updateDebug(this.getClass().getName(), "Sending an Authn Post Raw request");

            encoder = new DotHTTPPOSTRawDeflateEncoder(this.messageObserver);

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
