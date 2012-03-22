/*
 * Copyright 2004 Guy Van den Broeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dotmarketing.util.diff.helper;

import java.io.IOException;

import org.cyberneko.html.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Parses HTML files using the Neko HTML parser. Puts all elements and attribute
 * names to lowercase, removes all namespaces, produces well-formed XML.
 */
public class NekoHtmlParser {

    public SaxBuffer parse(InputSource is) throws IOException, SAXException {
        SaxBuffer buffer = new SaxBuffer();
        parse(is, buffer);

        return buffer;
    }

    public void parse(InputSource is, ContentHandler consumer)
            throws IOException, SAXException {
        if (is == null)
            throw new NullPointerException("is argument is required.");

        SAXParser parser = new SAXParser();
        parser.setFeature("http://xml.org/sax/features/namespaces", true);
        parser
                .setFeature(
                        "http://cyberneko.org/html/features/override-namespaces",
                        false);
        parser.setFeature(
                "http://cyberneko.org/html/features/insert-namespaces", false);
        parser
                .setFeature(
                        "http://cyberneko.org/html/features/scanner/ignore-specified-charset",
                        true);
        parser.setProperty(
                "http://cyberneko.org/html/properties/default-encoding",
                "UTF-8");
        parser.setProperty("http://cyberneko.org/html/properties/names/elems",
                "lower");
        parser.setProperty("http://cyberneko.org/html/properties/names/attrs",
                "lower");

        parser.setContentHandler(new RemoveNamespacesHandler(
                new MergeCharacterEventsHandler(consumer)));
        parser.parse(is);
    }

    /**
     * A ContentHandler that drops all namespace information.
     */
    static class RemoveNamespacesHandler implements ContentHandler {
        private ContentHandler consumer;

        public RemoveNamespacesHandler(ContentHandler consumer) {
            this.consumer = consumer;
        }

        public void endDocument() throws SAXException {
            consumer.endDocument();
        }

        public void startDocument() throws SAXException {
            consumer.startDocument();
        }

        public void characters(char ch[], int start, int length)
                throws SAXException {
            consumer.characters(ch, start, length);
        }

        public void ignorableWhitespace(char ch[], int start, int length)
                throws SAXException {
            consumer.ignorableWhitespace(ch, start, length);
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            // dropped on purpose
        }

        public void skippedEntity(String name) throws SAXException {
            // dropped on purpose
        }

        public void setDocumentLocator(Locator locator) {
            consumer.setDocumentLocator(locator);
        }

        public void processingInstruction(String target, String data)
                throws SAXException {
            // dropped on purpose
        }

        public void startPrefixMapping(String prefix, String uri)
                throws SAXException {
            // dropped on purpose
        }

        public void endElement(String namespaceURI, String localName,
                String qName) throws SAXException {
            consumer.endElement("", localName, localName);
        }

        public void startElement(String namespaceURI, String localName,
                String qName, Attributes atts) throws SAXException {
            AttributesImpl newAtts = new AttributesImpl(atts);
            for (int i = 0; i < atts.getLength(); i++) {
                newAtts.setURI(i, "");
                newAtts.setQName(i, newAtts.getLocalName(i));
            }
            consumer.startElement("", localName, localName, atts);
        }
    }

}
