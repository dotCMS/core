/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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
