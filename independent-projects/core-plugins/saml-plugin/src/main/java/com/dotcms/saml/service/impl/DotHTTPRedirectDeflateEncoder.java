package com.dotcms.saml.service.impl;

import com.dotcms.saml.MessageObserver;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.net.HttpServletSupport;
import net.shibboleth.utilities.java.support.net.URLBuilder;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.messaging.SAMLMessageSecuritySupport;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPRedirectDeflateEncoder;
import org.opensaml.saml.saml2.core.RequestAbstractType;
import org.opensaml.saml.saml2.core.StatusResponseType;
import org.opensaml.xmlsec.SignatureSigningParameters;

import javax.servlet.http.HttpServletResponse;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.List;

/**
 * This class is in charge of creating a html form to do the redirect for the auth request login to the IDP
 * @author jsanca
 */
public class DotHTTPRedirectDeflateEncoder extends HTTPRedirectDeflateEncoder {

    private final boolean clearQueryParams;
    private final MessageObserver messageObserver;

    public DotHTTPRedirectDeflateEncoder(final MessageObserver messageObserver) {
        this(true, messageObserver);
    }

    public DotHTTPRedirectDeflateEncoder(final boolean clearQueryParams, final MessageObserver messageObserver) {

        this.clearQueryParams = clearQueryParams;
        this.messageObserver  = messageObserver;
    }

    @Override
    protected String buildRedirectURL(final MessageContext<SAMLObject> messageContext,
                                      final String endpoint,
                                      final String message) throws MessageEncodingException {

        this.messageObserver.updateDebug(this.getClass().getName(),
                "Building URL to redirect client to: " + endpoint);
        URLBuilder urlBuilder = null;

        try {

            urlBuilder = new URLBuilder(endpoint);
        } catch (MalformedURLException e) {

            throw new MessageEncodingException("Endpoint URL " + endpoint + " is not a valid URL", e);
        }

        final List<Pair<String, String>> queryParams = urlBuilder.getQueryParams();
        if (this.clearQueryParams) {
            queryParams.clear();
        }

        final SAMLObject outboundMessage = (SAMLObject)messageContext.getMessage();
        if (outboundMessage instanceof RequestAbstractType) {

            queryParams.add(new Pair("SAMLRequest", message));
        } else {

            if (!(outboundMessage instanceof StatusResponseType)) {

                throw new MessageEncodingException("SAML message is neither a SAML RequestAbstractType nor StatusResponseType");
            }

            queryParams.add(new Pair("SAMLResponse", message));
        }

        final String relayState = SAMLBindingSupport.getRelayState(messageContext);
        if (SAMLBindingSupport.checkRelayState(relayState)) {

            queryParams.add(new Pair("RelayState", relayState));
        }

        final SignatureSigningParameters signingParameters = SAMLMessageSecuritySupport.getContextSigningParameters(messageContext);
        if (signingParameters != null && signingParameters.getSigningCredential() != null) {

            final String sigAlgURI            = this.getSignatureAlgorithmURI(signingParameters);
            final Pair<String, String> sigAlg = new Pair("SigAlg", sigAlgURI);
            final String sigMaterial          = urlBuilder.buildQueryString();

            queryParams.add(sigAlg);
            queryParams.add(new Pair("Signature", this.generateSignature(signingParameters.getSigningCredential(), sigAlgURI, sigMaterial)));
        } else {

            this.messageObserver.updateDebug(this.getClass().getName(),
                    "No signing credential was supplied, skipping HTTP-Redirect DEFLATE signing");
        }

        return urlBuilder.buildURL();
    }

    /** {@inheritDoc} */
    @Override
    protected void doEncode() throws MessageEncodingException {
        MessageContext<SAMLObject> messageContext = getMessageContext();
        SAMLObject outboundMessage = messageContext.getMessage();

        String endpointURL = getEndpointURL(messageContext).toString();

        // removeSignature(outboundMessage);

        String encodedMessage = deflateAndBase64Encode(outboundMessage);

        String redirectURL = buildRedirectURL(messageContext, endpointURL, encodedMessage);

        HttpServletResponse response = getHttpServletResponse();
        HttpServletSupport.addNoCacheHeaders(response);

        sendRedirectHTML(response, redirectURL);
        this.messageObserver.updateInfo(this.getClass().getName(), "Doing a Redirect deflate");

    }
    
    
    final static String redirectTemplate =
                    new StringWriter()
                    .append("<html>")
                    .append("<head>")
                    .append("<meta http-equiv=\"refresh\" content=\"0;URL='REDIRECT_ME'\"/>")
                    .append("<style>p {font-family: Arial;font-size: 16px;color: #666;margin: 50px;text-align:center;opacity: 1;animation: fadeIn ease 5s;animation-iteration-count: 0;-webkit-animation: fadeIn ease 5s;}@keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}}@-moz-keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}}@-webkit-keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}}@-o-keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}@-ms-keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}}</style>")
                    .append("</head>")
                    .append("<body><p>If your browser does not refresh, click <a href=\"REDIRECT_ME\">Here</a>.</p></body>")
                    .append("</html>")
                    .toString();
    
    
    
    public void sendRedirectHTML(HttpServletResponse response, final String redirectUrl) {
        
        final String finalTemplate = UtilMethods.replace(redirectTemplate,"REDIRECT_ME", redirectUrl);
        
        response.setContentType("text/html");
        Try.run(() -> {
            response.getWriter().write(finalTemplate);
            response.getWriter().flush();
        });
    }
    
}
