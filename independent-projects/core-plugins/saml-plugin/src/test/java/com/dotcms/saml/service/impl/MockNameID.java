package com.dotcms.saml.service.impl;

import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.impl.AbstractNameIDType;

public class MockNameID extends AbstractNameIDType implements NameID  {

    /**
     * Constructor.
     *
     * @param namespaceURI     the namespace the element is in
     * @param elementLocalName the local name of the XML element this Object represents
     * @param namespacePrefix  the prefix for the given namespace
     */
    protected MockNameID(String namespaceURI, String elementLocalName, String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
    }
}
