package com.dotcms.saml.service.impl;

import com.dotcms.saml.MessageObserver;
import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.codec.HTMLEncoder;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.net.HttpServletSupport;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.messaging.encoder.servlet.HttpServletResponseMessageEncoder;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.binding.BindingException;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.binding.encoding.SAMLMessageEncoder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.RequestAbstractType;
import org.opensaml.saml.saml2.core.StatusResponseType;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Http Post Deflate Encoder
 * This implementation removes the velocity dependency in order to use only a single text file.
 * Mostly to avoid some issues with osgi dependencies
 * @author jsanca
 */
public class DotHTTPPOSTRawDeflateEncoder  implements HttpServletResponseMessageEncoder<SAMLObject>, SAMLMessageEncoder {

    // this file is in src/main/resources/templates/
    private static final String TEMPLATE_AUTH_POST_RAW = "/templates/auth-post-raw.txt";
    private final RawTemplateProcessor rawTemplateProcessor;
    private HttpServletResponse response;
    private MessageContext<SAMLObject> messageContext;
    private boolean isDestroyed;
    private boolean isInitialized;
    private final MessageObserver messageObserver;

    public DotHTTPPOSTRawDeflateEncoder(final MessageObserver messageObserver) {

        this.messageObserver      = messageObserver;
        this.rawTemplateProcessor = RawTemplateProcessor.getInstance();
    }

    @Override
    public String getBindingURI() {
        return SAMLConstants.SAML2_REDIRECT_BINDING_URI;
    }

    @Override
    public void prepareContext() throws MessageEncodingException {

    }

    @Override
    public void encode() throws MessageEncodingException {

        this.messageObserver.updateDebug(this.getClass().getName(), "Beginning encode of message of type: " +
                this.messageContext.getMessage().getClass().getName());

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        doEncode();

        logEncodedMessage();

        this.messageObserver.updateDebug(this.getClass().getName(), "Successfully encoded message.");
    }

    protected void logEncodedMessage() {

        final XMLObject message = this.messageContext.getMessage();
        if (message == null) {
            this.messageObserver.updateWarning(this.getClass().getName(),
                    "Encoded message was null, nothing to log");
            return;
        }

        try {

            final Element dom = XMLObjectSupport.marshall(message);
            this.messageObserver.updateDebug(this.getClass().getName(),
                    SerializeSupport.prettyPrintXML(dom));
        } catch (MarshallingException e) {
            this.messageObserver.updateError(this.getClass().getName(),
                    "Unable to marshall message for logging purposes", e);
        }
    }

    protected void doEncode() throws MessageEncodingException {

        final SAMLObject outboundMessage = messageContext.getMessage();
        if (outboundMessage == null) {
            throw new MessageEncodingException("No outbound SAML message contained in message context");
        } else {

            final String endpointURL = this.getEndpointURL(messageContext).toString();
            this.postEncode(messageContext, endpointURL);
        }
    }

    protected void postEncode(final MessageContext<SAMLObject> messageContext, final String endpointURL) throws MessageEncodingException {

        this.messageObserver.updateDebug(this.getClass().getName(),
                "Invoking Raw templating to create POST body");

        try {

            final Map<String, String> context =  new HashMap<>();
            this.populateContext(context, messageContext, endpointURL);
            final HttpServletResponse response = this.getHttpServletResponse();
            HttpServletSupport.addNoCacheHeaders(response);
            HttpServletSupport.setUTF8Encoding(response);
            HttpServletSupport.setContentType(response, "text/html");
            final Writer out = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
            this.rawTemplateProcessor.renderTemplateFile(out, context, TEMPLATE_AUTH_POST_RAW);
            out.flush();
        } catch (Exception e) {

            this.messageObserver.updateError(this.getClass().getName(),
                    "Error invoking Velocity template: " + e.getMessage(), e);
            throw new MessageEncodingException("Error creating output document", e);
        }
    }


    protected void populateContext(final Map<String, String> context,
                                   final MessageContext<SAMLObject> messageContext,
                                   final String endpointURL) throws MessageEncodingException {

        final String encodedEndpointURL = HTMLEncoder.encodeForHTMLAttribute(endpointURL);
        this.messageObserver.updateDebug(this.getClass().getName(),
                "Encoding action url of '" + endpointURL + "' with encoded value '" +  encodedEndpointURL+ "' ");
        context.put("action", encodedEndpointURL);
        context.put("binding", this.getBindingURI());
        context.put("RelayState", "");   //  init as an empty in case it is not needed
        context.put("SAMLRequest", "");
        context.put("SAMLResponse", "");

        final SAMLObject outboundMessage = messageContext.getMessage();
        this.messageObserver.updateDebug(this.getClass().getName(),
                "Marshalling and Base64 encoding SAML message");
        final Element domMessage = this.marshallMessage(outboundMessage);

        String relayState;
        String messageXML;
        String encodedMessage;
        String encodedRelayState;

        try {
            messageXML        = SerializeSupport.nodeToString(domMessage);
            encodedMessage    = Base64Support.encode(messageXML.getBytes("UTF-8"), Base64Support.UNCHUNKED);
            if (outboundMessage instanceof RequestAbstractType) {
                context.put("SAMLRequest", "<input type=\"hidden\" name=\"SAMLRequest\" value=\""+encodedMessage+"\"/>");
            } else {
                if (!(outboundMessage instanceof StatusResponseType)) {
                    throw new MessageEncodingException("SAML message is neither a SAML RequestAbstractType or StatusResponseType");
                }

                context.put("SAMLResponse", "<input type=\"hidden\" name=\"SAMLResponse\" value=\""+encodedMessage+"\"/>");
            }
        } catch (UnsupportedEncodingException var9) {
            this.messageObserver.updateError(this.getClass().getName(),"UTF-8 encoding is not supported, this VM is not Java compliant.");
            throw new MessageEncodingException("Unable to encode message, UTF-8 encoding is not supported");
        }

        relayState = SAMLBindingSupport.getRelayState(messageContext);
        if (SAMLBindingSupport.checkRelayState(relayState)) {
            encodedRelayState = HTMLEncoder.encodeForHTMLAttribute(relayState);
            this.messageObserver.updateDebug(this.getClass().getName(),
            "Setting RelayState parameter to: '" + relayState + "' , encoded as '" +  encodedRelayState+ "' ");
            context.put("RelayState", "<input type=\"hidden\" name=\"RelayState\" value=\""+encodedRelayState+"\"/>");
        }
    }

    protected Element marshallMessage(final XMLObject message) throws MessageEncodingException {

        this.messageObserver.updateDebug(this.getClass().getName(),"Marshalling message");

        try {

            return XMLObjectSupport.marshall(message);
        } catch (MarshallingException e) {

            this.messageObserver.updateError(this.getClass().getName(),"Error marshalling message", e);
            throw new MessageEncodingException("Error marshalling message", e);
        }
    }

    protected URI getEndpointURL(final MessageContext<SAMLObject> messageContext) throws MessageEncodingException {
        try {
            return SAMLBindingSupport.getEndpointURL(messageContext);
        } catch (BindingException e) {
            throw new MessageEncodingException("Could not obtain message endpoint URL", e);
        }
    }

    @Override
    public void setMessageContext(final MessageContext<SAMLObject> messageContext) {

        this.messageContext = messageContext;
    }

    @Override
    public boolean isDestroyed() {
        return this.isDestroyed;
    }

    @Override
    public void destroy() {
        if (!this.isDestroyed) {
            response = null;
            messageContext = null;
            this.isDestroyed = true;
        }
    }

    @Override
    public boolean isInitialized() {

        return this.isInitialized;
    }

    @Override
    public void initialize() throws ComponentInitializationException {

        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        if (!this.isInitialized()) {
            if (this.response == null) {
                throw new ComponentInitializationException("HTTP servlet response cannot be null");
            }

            if (messageContext == null) {
                throw new ComponentInitializationException("Message context cannot be null");
            }
            this.isInitialized = true;
        }
    }

    @Override
    public HttpServletResponse getHttpServletResponse() {

        return this.response;
    }

    @Override
    public void setHttpServletResponse(final HttpServletResponse response) {

        this.response = response;
    }
}
