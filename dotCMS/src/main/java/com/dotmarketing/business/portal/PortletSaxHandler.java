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

    /**
     * Collects element text. The parser hands it over already decoded, so it is escaped again
     * before it goes into the rebuilt {@code <portlet>} element: a display name such as
     * {@code Roles & Tools} would otherwise put a raw {@code &} in that element, which then
     * fails to parse and the portlet is dropped.
     *
     * @param ch     the characters from the XML document
     * @param start  the start position in the array
     * @param length the number of characters to read from the array
     */
    @Override
    public void characters(char[] ch, int start, int length) {
        currentValue.append(ch, start, length);
        if (isPortlet) {
            appendEscaped(portletElement, ch, start, length, false);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (isPortlet) {
            portletElement.append("</").append(qName).append(">");
        }
        handleEndElement(qName);
    }

    /**
     * Writes a start tag into the rebuilt {@code <portlet>} element. Attribute values arrive
     * decoded, so they are escaped again, quotes included since the value sits inside double
     * quotes.
     *
     * @param qName      the element name
     * @param attributes the element's attributes
     */
    private void appendStartElement(String qName, Attributes attributes) {
        portletElement.append("<").append(qName);
        for (int i = 0; i < attributes.getLength(); i++) {
            final String value = attributes.getValue(i);
            portletElement.append(" ").append(attributes.getQName(i)).append("=\"");
            appendEscaped(portletElement, value.toCharArray(), 0, value.length(), true);
            portletElement.append("\"");
        }
        portletElement.append(">");
    }

    /**
     * Appends characters with the XML special characters replaced by their entities:
     * {@code &}, {@code <} and {@code >} always, and {@code "} when the text is an attribute value.
     *
     * @param target    the builder to append to
     * @param ch        the characters to append
     * @param start     the start position in the array
     * @param length    the number of characters to append
     * @param attribute {@code true} when the characters are a double-quoted attribute value
     */
    private static void appendEscaped(final StringBuilder target, final char[] ch, final int start,
            final int length, final boolean attribute) {
        for (int i = start; i < start + length; i++) {
            final char c = ch[i];
            switch (c) {
                case '&':
                    target.append("&amp;");
                    break;
                case '<':
                    target.append("&lt;");
                    break;
                case '>':
                    target.append("&gt;");
                    break;
                case '"':
                    target.append(attribute ? "&quot;" : "\"");
                    break;
                default:
                    target.append(c);
            }
        }
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