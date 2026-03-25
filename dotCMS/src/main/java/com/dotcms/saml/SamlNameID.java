package com.dotcms.saml;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Objects;

/**
 * A serializable holder for a SAML NameID's XML representation.
 *
 * <p>OpenSAML's concrete NameID implementation does not implement {@link Serializable},
 * which prevents it from being stored in Redis-managed sessions. This class lives in the
 * webapp classpath (core) so that Tomcat's session deserializer can always find it —
 * unlike plugin bundle classes, which are invisible to the webapp classloader.
 *
 * <p>The full XML string is stored so that the plugin can reconstruct a live
 * NameID on demand via {@code SamlUtils.toNameID(SamlNameID)}.
 *
 * <p>Usage in the plugin:
 * <pre>
 *     // Store:
 *     attrBuilder.nameID(new SamlNameID(SamlUtils.toXMLObjectString(nameID)));
 *
 *     // Reconstruct:
 *     NameID nameID = SamlUtils.toNameID((SamlNameID) attributes.getNameID());
 * </pre>
 *
 * @author jsanca
 */
public final class SamlNameID implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String xmlString;

    /**
     * Constructs a {@code SamlNameID} from the XML representation of a NameID.
     *
     * @param xmlString the full XML string of the NameID; must not be {@code null}
     */
    public SamlNameID(final String xmlString) {
        this.xmlString = Objects.requireNonNull(xmlString, "xmlString must not be null");
    }

    /**
     * Returns the XML string representation of the NameID.
     *
     * @return the XML string; never {@code null}
     */
    public String getXmlString() {
        return xmlString;
    }

    /**
     * Validates invariants after Java deserialization.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (xmlString.isBlank()) {
            throw new InvalidObjectException("SamlNameID: xmlString must not be blank");
        }
    }

    @Override
    public String toString() {
        return "SamlNameID{xmlString='" + xmlString + "'}";
    }
}
