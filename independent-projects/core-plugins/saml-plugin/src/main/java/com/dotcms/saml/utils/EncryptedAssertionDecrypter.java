package com.dotcms.saml.utils;

import com.dotcms.saml.MessageObserver;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLRuntimeException;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.xmlsec.encryption.EncryptedData;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import javax.crypto.SecretKey;
import javax.xml.XMLConstants;
import java.io.ByteArrayInputStream;
import java.security.Key;
import java.util.HashMap;

/**
 * This EncryptedAssertion Decrypter is a customization in order to avoid XML Marshalling issues
 * @author jsanca
 */
public class EncryptedAssertionDecrypter {

    private final ParserPool dotParserPool;
    private final MessageObserver messageObserver;

    public EncryptedAssertionDecrypter(final MessageObserver messageObserver) {
        this.dotParserPool   = this.buildParserPool();
        this.messageObserver = messageObserver;
    }

    public Assertion decrypt(final EncryptedAssertion encryptedAssertion, final SecretKey decryptKey) throws DecryptionException {

        final Document document = this.decryptDataToDOM(encryptedAssertion.getEncryptedData(), decryptKey);
        if (null == document) {

            throw new DecryptionException("Could not decrypt the assertion");
        }

        try {

            final Element element     = document.getDocumentElement();
            final XMLObject xmlObject = SamlUtils.toXMLObject(element);

            if (!(xmlObject instanceof Assertion)) {

                throw new DecryptionException("Decrypted SAMLObject was not an instance of Assertion");
            }

            return (Assertion) xmlObject;
        } catch (UnmarshallingException e) {

            this.messageObserver.updateError(this.getClass().getName(),"There was an error during unmarshalling of the decrypted element", e);
            throw new DecryptionException("Unmarshalling error during decryption", e);
        }
    }

    protected Document decryptDataToDOM(@Nonnull final EncryptedData encryptedData,
                                        @Nonnull final Key dataEncKey) throws DecryptionException {

        Constraint.isNotNull(encryptedData, "EncryptedData cannot be null");
        Constraint.isNotNull(dataEncKey, "Data decryption key cannot be null");

        if (!EncryptionConstants.TYPE_ELEMENT.equals(encryptedData.getType())) {
            this.messageObserver.updateError(this.getClass().getName(),
                    "EncryptedData was of unsupported type '" + encryptedData.getType()
                            + "', could not attempt decryption");
            throw new DecryptionException("EncryptedData of unsupported type was encountered");
        }

        try {
            checkAndMarshall(encryptedData);
        } catch (DecryptionException e) {
            this.messageObserver.updateError(this.getClass().getName(),"Error marshalling EncryptedData for decryption", e);
            throw e;
        }

        final Element targetElement = encryptedData.getDOM();

        XMLCipher xmlCipher;
        try {

            xmlCipher = XMLCipher.getInstance();
            xmlCipher.init(XMLCipher.DECRYPT_MODE, dataEncKey);
        } catch (XMLEncryptionException e) {
            this.messageObserver.updateError(this.getClass().getName(),"Error initialzing cipher instance on data decryption", e);
            throw new DecryptionException("Error initialzing cipher instance on data decryption", e);
        }

        byte[] bytes = null;
        try {
            bytes = xmlCipher.decryptToByteArray(targetElement);
        } catch (XMLEncryptionException e) {
            this.messageObserver.updateError(this.getClass().getName(),"Error decrypting the encrypted data element", e);
            throw new DecryptionException("Error decrypting the encrypted data element", e);
        } catch (Exception e) {
            throw new DecryptionException("Probable runtime exception on decryption:" + e.getMessage(), e);
        }

        if (bytes == null) {
            throw new DecryptionException("EncryptedData could not be decrypted");
        }

        this.messageObserver.updateInfo(this.getClass().getName(), "decrypted assertion: " + new String(bytes));

        try {
            return this.dotParserPool.parse(new ByteArrayInputStream(bytes));
        } catch (XMLParserException  e) {
            this.messageObserver.updateError(this.getClass().getName(),"Error parsing decrypted input stream", e);
            throw new DecryptionException("Error parsing input stream", e);
        }
    }

    protected void checkAndMarshall(@Nonnull final XMLObject xmlObject) throws DecryptionException {
        Constraint.isNotNull(xmlObject, "XMLObject cannot be null");

        Element targetElement = xmlObject.getDOM();
        if (targetElement == null) {
            Marshaller marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(xmlObject);
            if (marshaller == null) {
                marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(
                        XMLObjectProviderRegistrySupport.getDefaultProviderQName());
                if (marshaller == null) {
                    String errorMsg = "No marshaller available for " + xmlObject.getElementQName();
                    this.messageObserver.updateError(this.getClass().getName(), errorMsg);
                    throw new DecryptionException(errorMsg);
                }
            }
            try {
                targetElement = marshaller.marshall(xmlObject);
            } catch (MarshallingException e) {
                this.messageObserver.updateError(this.getClass().getName(), "Error marshalling target XMLObject", e);
                throw new DecryptionException("Error marshalling target XMLObject", e);
            }
        }
    }

    protected ParserPool buildParserPool() {
        BasicParserPool pp = new BasicParserPool();
        HashMap<String, Boolean> features = new HashMap<>();

        pp.setNamespaceAware(true);

        // Note: this feature config is necessary due to an unresolved Xerces deferred DOM issue/bug
        features.put("http://apache.org/xml/features/dom/defer-node-expansion", Boolean.FALSE);

        // The following config is to harden the parser pool against known XML security vulnerabilities
        pp.setExpandEntityReferences(false);
        features.put(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        features.put("http://apache.org/xml/features/disallow-doctype-decl", true);

        pp.setBuilderFeatures(features);

        try {
            pp.initialize();
            return pp;
        } catch (ComponentInitializationException e) {
            throw new XMLRuntimeException("Problem initializing Decrypter internal ParserPool", e);
        }
    }
}

