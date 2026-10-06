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

    /** Maximum permitted length of the XML string (guards against heap exhaustion on corrupt sessions). */
    private static final int MAX_XML_LENGTH   = 8192;

    /** Maximum permitted length of the plain NameID value. */
    private static final int MAX_VALUE_LENGTH = 1024;

    private final String xmlString;

    /** The plain NameID value (email, persistent ID, etc.) for cheap retrieval without XML parsing. */
    private final String value;

    /**
     * Constructs a {@code SamlNameID}.
     *
     * @param xmlString the full XML string of the NameID; must not be {@code null}
     * @param value     the plain NameID value (e.g. email or opaque ID); must not be {@code null}
     */
    public SamlNameID(final String xmlString, final String value) {
        this.xmlString = Objects.requireNonNull(xmlString, "xmlString must not be null");
        this.value     = Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * Returns the XML string representation of the NameID, used to reconstruct a live
     * NameID object in the SAML plugin via {@code SamlUtils.toNameID(SamlNameID)}.
     *
     * <p><strong>Security warning:</strong> the XML string contains the user's SAML identity
     * value (email address, persistent ID, etc.) and is therefore PII. It must never be
     * written to logs, stored in plaintext audit trails, or returned in API responses.
     * Use {@link #getValue()} only when the raw identity string is genuinely required
     * (e.g. for hashing before storage), and {@link #toString()} for any diagnostic output.
     *
     * @return the XML string; never {@code null}
     */
    public String getXmlString() {
        return xmlString;
    }

    /**
     * Returns the plain NameID value (e.g. email address or opaque persistent ID).
     *
     * <p><strong>Security warning:</strong> this is raw PII. Pass it only to operations that
     * require the actual identity (e.g. {@code SAMLHelper.hashIt()}) — never write it directly
     * to logs or API responses. If a correlation token is needed for debugging, hash the value
     * first and use only the first few characters of the hash.
     *
     * @return the NameID value; never {@code null}
     */
    public String getValue() {
        return value;
    }

    /**
     * Validates invariants after Java deserialization.
     *
     * <p>Length caps are enforced here in addition to the null/blank checks because
     * {@code defaultReadObject()} can restore arbitrarily large strings from a corrupt or
     * malicious Redis entry, potentially exhausting heap memory before any application code
     * runs.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (xmlString == null || xmlString.isBlank()) {
            throw new InvalidObjectException("SamlNameID: xmlString must not be null or blank");
        }
        if (xmlString.length() > MAX_XML_LENGTH) {
            throw new InvalidObjectException("SamlNameID: xmlString exceeds maximum length of " + MAX_XML_LENGTH);
        }
        if (value == null || value.isBlank()) {
            throw new InvalidObjectException("SamlNameID: value must not be null or blank");
        }
        if (value.length() > MAX_VALUE_LENGTH) {
            throw new InvalidObjectException("SamlNameID: value exceeds maximum length of " + MAX_VALUE_LENGTH);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SamlNameID)) {
            return false;
        }
        final SamlNameID other = (SamlNameID) o;
        return Objects.equals(xmlString, other.xmlString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xmlString);
    }

    /**
     * Returns a redacted representation to prevent accidental PII leakage in logs.
     * The XML string contains the user's SAML identity value and must never appear in logs.
     */
    @Override
    public String toString() {
        return "SamlNameID{xmlString='[REDACTED]'}";
    }
}
