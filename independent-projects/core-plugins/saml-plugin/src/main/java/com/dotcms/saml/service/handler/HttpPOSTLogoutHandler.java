package com.dotcms.saml.service.handler;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.SamlNameID;
import com.dotcms.saml.MessageObserver;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.impl.DotHTTPPOSTDeflateEncoder;
import com.dotcms.saml.service.internal.SamlCoreService;
import com.dotcms.saml.utils.IdpConfigCredentialResolver;
import com.dotcms.saml.utils.SamlUtils;
import com.dotcms.saml.utils.SignatureUtils;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import org.apache.velocity.app.VelocityEngine;
import org.apache.xml.security.c14n.Canonicalizer;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.ConfigurableContentReference;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.provider.ApacheSantuarioSignerProviderImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Implements the Logout handler by POST
 * @author jsanca
 */
public class HttpPOSTLogoutHandler implements LogoutHandler {

    private final SamlCoreService samlCoreService;
    private final VelocityEngine  velocityEngine;
    private final MessageObserver messageObserver;

    public HttpPOSTLogoutHandler(final SamlCoreService samlCoreService,
                                 final VelocityEngine velocityEngine,
                                 final MessageObserver messageObserver) {

        this.samlCoreService = samlCoreService;
        this.velocityEngine  = velocityEngine;
        this.messageObserver = messageObserver;
    }

    @Override
    public void handle(final HttpServletRequest  request,
                       final HttpServletResponse response,
                       final Object nameID,
                       final String sessionIndexValue,
                       final IdentityProviderConfiguration identityProviderConfiguration) {

        final MessageContext context      = new MessageContext(); // main context
        final LogoutRequest logoutRequest = this.samlCoreService.buildLogoutRequest(
                identityProviderConfiguration, SamlUtils.toNameID((SamlNameID) nameID), sessionIndexValue);

        // Similar to the AuthRequest, logout request can be sign
        final boolean needSign = identityProviderConfiguration.containsOptionalProperty("logout.sign.request")?
                Boolean.parseBoolean(identityProviderConfiguration.getOptionalProperty("logout.sign.request").toString()): false;

        if (needSign) {

            try {

                final Signature signature = this.createSignature(identityProviderConfiguration);

                this.messageObserver.updateInfo(this.getClass().getName(), "signature: " + signature);
                logoutRequest.setSignature(signature);

                // Marshall and Sign
                final String digestAlgorithm = identityProviderConfiguration.containsOptionalProperty("logout.signature.reference.digestmethod.algorithm") ?
                        (String) identityProviderConfiguration.getOptionalProperty("logout.signature.reference.digestmethod.algorithm") :
                        SignatureConstants.ALGO_ID_DIGEST_SHA256;

                SignatureUtils.marshalAndSing(logoutRequest, this.messageObserver, signature, digestAlgorithm);
            } catch (Exception e) {

                this.messageObserver.updateError(this.getClass().getName(), e.getMessage(), e);
            }
        }

        // peer entity (Idp to SP and viceversa)
        final SAMLPeerEntityContext peerEntityContext = context.getSubcontext(SAMLPeerEntityContext.class, true);
        // info about the endpoint of the peer entity
        final SAMLEndpointContext endpointContext = peerEntityContext.getSubcontext(SAMLEndpointContext.class, true);

        endpointContext.setEndpoint(this.samlCoreService
                .getIdentityProviderSLODestinationEndpoint(identityProviderConfiguration));

        context.setMessage(logoutRequest);

        final boolean needSignatureSigningParams = identityProviderConfiguration.containsOptionalProperty("logout.sign.params")?
                Boolean.parseBoolean(identityProviderConfiguration.getOptionalProperty("logout.sign.params").toString()): true;
        if (needSignatureSigningParams) {
            SignatureUtils.setSignatureSigningParams(this.samlCoreService.getCredential(identityProviderConfiguration), context);
        }

        this.doPost(context, response, logoutRequest, identityProviderConfiguration);
    }

    private Signature createSignature(final IdentityProviderConfiguration identityProviderConfiguration) {

        final X509Credential credential     = (X509Credential) this.samlCoreService.getCredential(identityProviderConfiguration);
        final String signatureAlgorithm = identityProviderConfiguration.containsOptionalProperty("logout.signature.algorithm")?
                (String)identityProviderConfiguration.getOptionalProperty("logout.signature.algorithm"):SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256;

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
