package com.dotmarketing.business.portal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class PortletSaxHandler<T> extends DefaultHandler {

    private final StringBuilder currentValue = new StringBuilder();
    private final Function<String, Optional<T>> valueMapper;
    private final Map<String, T> valueMap;
    private String portletName;
    private String portletClass;
    private final StringBuilder portletElement = new StringBuilder();
    private boolean isPortlet = false;

    PortletSaxHandler(Function<String, Optional<T>> valueMapper) {
        this.valueMap = new HashMap<>();
        this.valueMapper = valueMapper;
    }

    public Map<String, T> getValueMap() {
        return valueMap;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        currentValue.setLength(0);
        if (qName.equalsIgnoreCase("portlet")) {
            portletElement.setLength(0);
            isPortlet = true;
        }
        if (isPortlet) {
            appendStartElement(qName, attributes);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        currentValue.append(ch, start, length);
        if (isPortlet) {
            portletElement.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (isPortlet) {
            portletElement.append("</").append(qName).append(">");
        }
        handleEndElement(qName);
    }

    private void appendStartElement(String qName, Attributes attributes) {
        portletElement.append("<").append(qName);
        for (int i = 0; i < attributes.getLength(); i++) {
            portletElement.append(" ").append(attributes.getQName(i)).append("=\"").append(attributes.getValue(i)).append("\"");
        }
        portletElement.append(">");
    }

    private void handleEndElement(String qName) {
        if (qName.equalsIgnoreCase("portlet-name")) {
            portletName = currentValue.toString().trim();
        } else if (qName.equalsIgnoreCase("portlet-class")) {
            portletClass = currentValue.toString().trim();
        } else if (qName.equalsIgnoreCase("portlet")) {
            processPortletEndElement();
        }
    }

    private void processPortletEndElement() {
        if (portletName != null && !portletName.isEmpty() && portletClass != null && !portletClass.isEmpty()) {
            String portletXML = portletElement.toString();
            valueMapper.apply(portletXML).ifPresent(value -> valueMap.put(portletName, value));
        }
        portletName = null;
        portletClass = null;
        isPortlet = false;
    }
}