package com.dotcms.saml.service.impl;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.util.AttributeMap;
import org.opensaml.core.xml.util.XMLObjectChildrenList;
import org.opensaml.saml.common.AbstractSAMLObject;
import org.opensaml.saml.saml2.core.Attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MockAttribute extends AbstractSAMLObject implements Attribute {

    /** Name of the attribute. */
    private String name;

    /** Format of the name of the attribute. */
    private String nameFormat;

    /** Human readable name of the attribute. */
    private String friendlyName;

    /** "anyAttribute" attributes. */
    private AttributeMap unknownAttributes;

    /** List of attribute values for this attribute. */
    private final XMLObjectChildrenList<XMLObject> attributeValues;

    /**
     * Constructor.
     *
     * @param namespaceURI the namespace the element is in
     * @param elementLocalName the local name of the XML element this Object represents
     * @param namespacePrefix the prefix for the given namespace
     */
    protected MockAttribute(String namespaceURI, String elementLocalName, String namespacePrefix,
                            final XMLObjectChildrenList<XMLObject> attributeValues) {
        super(namespaceURI, elementLocalName, namespacePrefix);
        this.unknownAttributes = new AttributeMap(this);
        this.attributeValues   = attributeValues;
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public void setName(String n) {
        name = prepareForAssignment(name, n);
    }

    /** {@inheritDoc} */
    public String getNameFormat() {
        return nameFormat;
    }

    /** {@inheritDoc} */
    public void setNameFormat(String format) {
        nameFormat = prepareForAssignment(nameFormat, format);
    }

    /** {@inheritDoc} */
    public String getFriendlyName() {
        return friendlyName;
    }

    /** {@inheritDoc} */
    public void setFriendlyName(String fname) {
        friendlyName = prepareForAssignment(friendlyName, fname);
    }

    /**
     * {@inheritDoc}
     */
    public AttributeMap getUnknownAttributes() {
        return unknownAttributes;
    }

    /** {@inheritDoc} */
    public List<XMLObject> getAttributeValues() {
        return attributeValues;
    }

    /** {@inheritDoc} */
    public List<XMLObject> getOrderedChildren() {
        ArrayList<XMLObject> children = new ArrayList<>();

        children.addAll(attributeValues);

        return Collections.unmodifiableList(children);
    }
}
