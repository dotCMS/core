/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
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

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;

class MergeCharacterEventsHandler implements ContentHandler {
    private ContentHandler consumer;

    private char[] ch;

    private int start = 0;

    private int length = 0;

    public MergeCharacterEventsHandler(ContentHandler consumer) {
        this.consumer = consumer;
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
        char[] newCh = new char[this.length + length];
        if (this.ch != null)
            System.arraycopy(this.ch, this.start, newCh, 0, this.length);
        System.arraycopy(ch, start, newCh, this.length, length);
        this.start = 0;
        this.length = newCh.length;
        this.ch = newCh;
    }

    private void flushCharacters() throws SAXException {
        if (ch != null) {
            consumer.characters(ch, start, length);
            ch = null;
            start = 0;
            length = 0;
        }
    }

    public void endDocument() throws SAXException {
        flushCharacters();
        consumer.endDocument();
    }

    public void startDocument() throws SAXException {
        flushCharacters();
        consumer.startDocument();
    }

    public void ignorableWhitespace(char ch[], int start, int length)
            throws SAXException {
        flushCharacters();
        consumer.ignorableWhitespace(ch, start, length);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        flushCharacters();
        consumer.endPrefixMapping(prefix);
    }

    public void skippedEntity(String name) throws SAXException {
        flushCharacters();
        consumer.skippedEntity(name);
    }

    public void setDocumentLocator(Locator locator) {
        consumer.setDocumentLocator(locator);
    }

    public void processingInstruction(String target, String data)
            throws SAXException {
        flushCharacters();
        consumer.processingInstruction(target, data);
    }

    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        flushCharacters();
        consumer.startPrefixMapping(prefix, uri);
    }

    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        flushCharacters();
        consumer.endElement(namespaceURI, localName, qName);
    }

    public void startElement(String namespaceURI, String localName,
            String qName, Attributes atts) throws SAXException {
        flushCharacters();
        consumer.startElement(namespaceURI, localName, qName, atts);
    }
}