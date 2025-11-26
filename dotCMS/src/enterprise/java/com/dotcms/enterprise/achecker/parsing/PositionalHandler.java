/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.parsing;

import com.dotcms.enterprise.achecker.utility.ParserConst;
import java.util.Stack;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

public class PositionalHandler extends DefaultHandler implements LexicalHandler {

	private Locator locator;
	private Stack<Element> elementStack =  new Stack<>();
	private StringBuilder textBuffer = new StringBuilder(); 
	private Document doc  = null;
	private DoctypeBean doctypeBean;
	
	public DoctypeBean getDoctypeBean() {
		return doctypeBean;
	}
	
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
		doctypeBean = new DoctypeBean(name, publicId, systemId);
	}
	
	public PositionalHandler(Document doc) {
		super();		
		this.doc = doc;
	}
	
	@Override
	public void setDocumentLocator(final Locator locator) {
		this.locator = locator; 
	}
	
	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
	throws SAXException {
		addTextIfNeeded();
		final Element el = doc.createElement(qName);
		for (int i = 0; i < attributes.getLength(); i++) {
			el.setAttribute(attributes.getQName(i), attributes.getValue(i));
		}
		el.setUserData(ParserConst.LINE_NUMBER_KEY_NAME, String.valueOf(this.locator.getLineNumber() + 1), null);
		el.setUserData(ParserConst.COL_NUMBER_KEY_NAME , String.valueOf(this.locator.getColumnNumber()), null);
		elementStack.push(el);
	}

	@Override
	public void endElement(final String uri, final String localName, final String qName) {
		addTextIfNeeded();
		final Element closedEl = elementStack.pop();
		if (elementStack.isEmpty()) { // Is this the root element?
				doc.appendChild(closedEl);
		} else {
			final Element parentEl = elementStack.peek();
			parentEl.appendChild(closedEl);
		}
	}

	@Override
	public void characters(final char ch[], final int start, final int length) throws SAXException {
		textBuffer.append(ch, start, length);
	}

	private void addTextIfNeeded() {
		if (textBuffer.length() > 0) {
			final Element el = elementStack.peek();
			final Node textNode = doc.createTextNode(textBuffer.toString());
			el.appendChild(textNode);
			textBuffer.delete(0, textBuffer.length());
		}
	}

	public void startEntity(String name) throws SAXException {}
	public void startCDATA() throws SAXException {}
	public void endEntity(String name) throws SAXException {}
	public void endDTD() throws SAXException {}
	public void endCDATA() throws SAXException {}
	public void comment(char[] ch, int start, int length) throws SAXException {}

}
