package com.dotcms.saml.service.impl;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.AbstractSAMLObject;
import org.opensaml.saml.ext.saml2mdquery.ActionNamespace;

import java.util.List;

public class MockActionNamespace  extends AbstractSAMLObject implements ActionNamespace {

    /** Action namespace value. */
    private String value;

    /**
     * Constructor.
     *
     * @param namespaceURI the namespace the element is in
     * @param elementLocalName the local name of the XML element this Object represents
     * @param namespacePrefix the prefix for the given namespace
     */
    protected MockActionNamespace(String namespaceURI, String elementLocalName, String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
    }

    /** {@inheritDoc} */
    @Override
    public String getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(String newValue) {
        value = prepareForAssignment(value, newValue);
    }

    /** {@inheritDoc} */
    @Override
    public List<XMLObject> getOrderedChildren() {
        // no children
        return null;
    }
}
