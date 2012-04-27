package com.dotcms.achecker.parsing;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;

import com.dotcms.achecker.parsing.DoctypeBean;

public 	class ACheckerDocument implements Document {

	private Document document;

	private DoctypeBean doctypeBean;

	public ACheckerDocument(Document document) {
		this.document = document;
	}

	public void setDoctypeBean(DoctypeBean doctypeBean) {
		this.doctypeBean = doctypeBean;
	}

	public DoctypeBean getDoctypeBean() {
		return doctypeBean;
	}

	public DocumentType getDoctype() {
		return document.getDoctype();
	}

	public DOMImplementation getImplementation() {
		return document.getImplementation();
	}

	public Element getDocumentElement() {
		return document.getDocumentElement();
	}

	public Element createElement(String tagName) throws DOMException {
		return document.createElement(tagName);
	}

	public DocumentFragment createDocumentFragment() {
		return document.createDocumentFragment();
	}

	public Text createTextNode(String data) {
		return document.createTextNode(data);
	}

	public Comment createComment(String data) {
		return document.createComment(data);
	}

	public CDATASection createCDATASection(String data) throws DOMException {
		return document.createCDATASection(data);
	}

	public ProcessingInstruction createProcessingInstruction(String target,
			String data) throws DOMException {
		return document.createProcessingInstruction(target, data);
	}

	public Attr createAttribute(String name) throws DOMException {
		return document.createAttribute(name);
	}

	public String getNodeName() {
		return document.getNodeName();
	}

	public String getNodeValue() throws DOMException {
		return document.getNodeValue();
	}

	public EntityReference createEntityReference(String name)
	throws DOMException {
		return document.createEntityReference(name);
	}

	public void setNodeValue(String nodeValue) throws DOMException {
		document.setNodeValue(nodeValue);
	}

	public short getNodeType() {
		return document.getNodeType();
	}

	public Node getParentNode() {
		return document.getParentNode();
	}

	public NodeList getChildNodes() {
		return document.getChildNodes();
	}

	public Node getFirstChild() {
		return document.getFirstChild();
	}

	public NodeList getElementsByTagName(String tagname) {
		return document.getElementsByTagName(tagname);
	}

	public Node getLastChild() {
		return document.getLastChild();
	}

	public Node getPreviousSibling() {
		return document.getPreviousSibling();
	}

	public Node getNextSibling() {
		return document.getNextSibling();
	}

	public Node importNode(Node importedNode, boolean deep)
	throws DOMException {
		return document.importNode(importedNode, deep);
	}

	public NamedNodeMap getAttributes() {
		return document.getAttributes();
	}

	public Document getOwnerDocument() {
		return document.getOwnerDocument();
	}

	public Node insertBefore(Node newChild, Node refChild)
	throws DOMException {
		return document.insertBefore(newChild, refChild);
	}

	public Node replaceChild(Node newChild, Node oldChild)
	throws DOMException {
		return document.replaceChild(newChild, oldChild);
	}

	public Node removeChild(Node oldChild) throws DOMException {
		return document.removeChild(oldChild);
	}

	public Node appendChild(Node newChild) throws DOMException {
		return document.appendChild(newChild);
	}

	public Element createElementNS(String namespaceURI, String qualifiedName)
	throws DOMException {
		return document.createElementNS(namespaceURI, qualifiedName);
	}

	public boolean hasChildNodes() {
		return document.hasChildNodes();
	}

	public Node cloneNode(boolean deep) {
		return document.cloneNode(deep);
	}

	public void normalize() {
		document.normalize();
	}

	public Attr createAttributeNS(String namespaceURI, String qualifiedName)
	throws DOMException {
		return document.createAttributeNS(namespaceURI, qualifiedName);
	}

	public boolean isSupported(String feature, String version) {
		return document.isSupported(feature, version);
	}

	public String getNamespaceURI() {
		return document.getNamespaceURI();
	}

	public String getPrefix() {
		return document.getPrefix();
	}

	public NodeList getElementsByTagNameNS(String namespaceURI,
			String localName) {
		return document.getElementsByTagNameNS(namespaceURI, localName);
	}

	public void setPrefix(String prefix) throws DOMException {
		document.setPrefix(prefix);
	}

	public Element getElementById(String elementId) {
		return document.getElementById(elementId);
	}

	public String getInputEncoding() {
		return document.getInputEncoding();
	}

	public String getXmlEncoding() {
		return document.getXmlEncoding();
	}

	public boolean getXmlStandalone() {
		return document.getXmlStandalone();
	}

	public String getLocalName() {
		return document.getLocalName();
	}

	public void setXmlStandalone(boolean xmlStandalone) throws DOMException {
		document.setXmlStandalone(xmlStandalone);
	}

	public boolean hasAttributes() {
		return document.hasAttributes();
	}

	public String getBaseURI() {
		return document.getBaseURI();
	}

	public String getXmlVersion() {
		return document.getXmlVersion();
	}

	public short compareDocumentPosition(Node other) throws DOMException {
		return document.compareDocumentPosition(other);
	}

	public void setXmlVersion(String xmlVersion) throws DOMException {
		document.setXmlVersion(xmlVersion);
	}

	public String getTextContent() throws DOMException {
		return document.getTextContent();
	}

	public boolean getStrictErrorChecking() {
		return document.getStrictErrorChecking();
	}

	public void setStrictErrorChecking(boolean strictErrorChecking) {
		document.setStrictErrorChecking(strictErrorChecking);
	}

	public void setTextContent(String textContent) throws DOMException {
		document.setTextContent(textContent);
	}

	public String getDocumentURI() {
		return document.getDocumentURI();
	}

	public void setDocumentURI(String documentURI) {
		document.setDocumentURI(documentURI);
	}

	public Node adoptNode(Node source) throws DOMException {
		return document.adoptNode(source);
	}

	public boolean isSameNode(Node other) {
		return document.isSameNode(other);
	}

	public String lookupPrefix(String namespaceURI) {
		return document.lookupPrefix(namespaceURI);
	}

	public boolean isDefaultNamespace(String namespaceURI) {
		return document.isDefaultNamespace(namespaceURI);
	}

	public String lookupNamespaceURI(String prefix) {
		return document.lookupNamespaceURI(prefix);
	}

	public boolean isEqualNode(Node arg) {
		return document.isEqualNode(arg);
	}

	public DOMConfiguration getDomConfig() {
		return document.getDomConfig();
	}

	public void normalizeDocument() {
		document.normalizeDocument();
	}

	public Node renameNode(Node n, String namespaceURI, String qualifiedName)
	throws DOMException {
		return document.renameNode(n, namespaceURI, qualifiedName);
	}

	public Object getFeature(String feature, String version) {
		return document.getFeature(feature, version);
	}

	public Object setUserData(String key, Object data,
			UserDataHandler handler) {
		return document.setUserData(key, data, handler);
	}

	public Object getUserData(String key) {
		return document.getUserData(key);
	}

}