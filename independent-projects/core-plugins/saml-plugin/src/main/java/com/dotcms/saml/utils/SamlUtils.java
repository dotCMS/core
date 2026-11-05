package com.dotcms.saml.utils;

import com.dotcms.saml.SamlNameID;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.impl.SamlCoreServiceImpl;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.security.RandomIdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.QNameSupport;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.messaging.handler.impl.BasicMessageHandlerChain;
import org.opensaml.saml.saml2.core.NameID;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saml Utils
 * @author jsanca
 */
public class SamlUtils {

    private static final UnmarshallerFactory                unmarshallerFactory     = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
    private static final MarshallerFactory                  marshallerFactory       = XMLObjectProviderRegistrySupport.getMarshallerFactory();
    private static final ParserPool                         parserPool              = XMLObjectProviderRegistrySupport.getParserPool();
    private static final RandomIdentifierGenerationStrategy secureRandomIdGenerator = new RandomIdentifierGenerationStrategy();

    /**
     * Generate the Random id
     *
     * @return String
     */
    public static String generateSecureRandomId() {

        return secureRandomIdGenerator.generateIdentifier();
    }

    /**
     * Convert to String an {@link XMLObject}
     *
     * @param object
     *            {@link XMLObject}
     * @return String
     */
    public static String toXMLObjectString(final XMLObject object) {

        // Serialize the existing DOM when there is one. Marshalling again would move the element into a new
        // document and release its parent's DOM, and OpenSAML reads the signature of the parent (for example
        // the Response around an unsigned assertion) from that DOM.
        final Element element = null != object.getDOM()? object.getDOM() : toElement(object);

        return toElementString(element);
    }

    /**
     * Convert to String an {@link Element}
     *
     * @param element
     *            {@link Element}
     * @return String
     */
    public static String toElementString(final Element element) {

        final Transformer transformer;
        final StreamResult result;
        final DOMSource source;
        String xmlString = null;

        try {

            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            result      = new StreamResult(new StringWriter());
            source      = new DOMSource(element);

            transformer.transform(source, result);
            xmlString = result.getWriter().toString();
        } catch (TransformerException e) {

            Logger.getLogger(SamlCoreServiceImpl.class.getName()).log(Level.WARNING, e.getMessage(), e);
        }

        return xmlString;
    }

    /**
     * Parses an XML string back into an {@link XMLObject} using OpenSAML's own infrastructure.
     * This is the inverse of {@link #toXMLObjectString(XMLObject)}.
     *
     * <p>XML parsing is delegated to {@link XMLObjectProviderRegistrySupport#getParserPool()},
     * OpenSAML's shared {@code BasicParserPool}. This pool is:
     * <ul>
     *   <li>Already configured during OpenSAML bootstrap with XXE protection built-in.</li>
     *   <li>Thread-safe by design: it pools {@code DocumentBuilder} instances so concurrent
     *       calls never share a parser.</li>
     *   <li>Namespace-aware and consistent with how OpenSAML processes all other XML internally.
     *   </li>
     * </ul>
     *
     * @param xmlString the XML string representation of the object; must not be {@code null}
     * @return the reconstructed {@link XMLObject}
     * @throws NullPointerException if {@code xmlString} is {@code null}
     * @throws UnmarshallingException if parsing or unmarshalling fails
     */
    public static XMLObject fromXMLString(final String xmlString) throws UnmarshallingException {
        Objects.requireNonNull(xmlString, "xmlString must not be null");
        try {
            final Document document = parserPool.parse(new StringReader(xmlString));
            return toXMLObject(document.getDocumentElement());
        } catch (final UnmarshallingException e) {
            throw e;
        } catch (final Exception e) {
            throw new UnmarshallingException("Failed to parse XML string to XMLObject: " + e.getMessage(), e);
        }
    }

    /**
     * Reconstructs a {@link org.opensaml.saml.saml2.core.NameID} from a {@link SamlNameID}.
     *
     * <p>This is the plugin-side counterpart of wrapping a raw NameID in a {@link SamlNameID}
     * for session storage. Call this whenever plugin code needs to use a live NameID object
     * retrieved from {@link com.dotcms.saml.Attributes#getNameID()}.
     *
     * @param samlNameID the session-safe wrapper; must not be {@code null}
     * @return the reconstructed NameID
     * @throws IllegalStateException if the XML cannot be parsed
     */
    public static NameID toNameID(final SamlNameID samlNameID) {
        Objects.requireNonNull(samlNameID, "samlNameID must not be null");
        try {
            return (NameID) fromXMLString(samlNameID.getXmlString());
        } catch (final UnmarshallingException e) {
            throw new IllegalStateException("Cannot reconstruct NameID from XML: " + samlNameID.getXmlString(), e);
        }
    }

    public static XMLObject toXMLObject (final Element element) throws UnmarshallingException {

        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        if (unmarshaller == null) {

            unmarshaller = unmarshallerFactory.getUnmarshaller(
                    XMLObjectProviderRegistrySupport.getDefaultProviderQName());

            if (unmarshaller == null) {

                final String errorMsg = "No unmarshaller available for " + QNameSupport.getNodeQName(element);
                Logger.getLogger(SamlCoreServiceImpl.class.getName()).log(Level.SEVERE, errorMsg);
                throw new UnmarshallingException(errorMsg);
            } else {

                Logger.getLogger(SamlCoreServiceImpl.class.getName()).log(Level.SEVERE,"No unmarshaller was registered for {}. Using default unmarshaller.",
                        QNameSupport.getNodeQName(element));
            }
        }

        return unmarshaller.unmarshall(element);
    }

    public static Element toElement(final XMLObject object) {

        final Marshaller out = marshallerFactory.getMarshaller(object);

        try {

            out.marshall(object);
        } catch (MarshallingException e) {

            Logger.getLogger(SamlCoreServiceImpl.class.getName()).log(Level.WARNING, e.getMessage(), e);
        }

        return object.getDOM();
    }



    /**
     * Invoke a message handler chain
     *
     * @param handlerChain
     *            {@link BasicMessageHandlerChain}
     * @param context
     *            MessageContext
     */
    public static <T> void invokeMessageHandlerChain(final BasicMessageHandlerChain<T> handlerChain,
                                                     final MessageContext<T> context) {
        try {

            handlerChain.initialize();
            handlerChain.doInvoke(context);
        } catch (ComponentInitializationException | MessageHandlerException e) {

            throw new SamlException(e.getMessage(), e);
        }
    }
}
