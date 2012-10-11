package com.dotmarketing.viewtools;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.velocity.tools.generic.ValueParser;
import org.apache.velocity.tools.view.tools.ViewTool;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import com.dotmarketing.util.Logger;
import com.dotmarketing.viewtools.cache.XmlToolCache;
import com.dotmarketing.viewtools.util.ConversionUtils;
import com.dotmarketing.viewtools.bean.XmlToolDoc;

/**
 * <p>
 * Tool for reading/navigating XML files. This uses dom4j under the covers to provide complete XPath support for
 * traversing XML files.
 * </p>
 * <p>
 * Here's a short example:
 * 
 * <pre>
 * XML file:
 *   &lt;foo&gt;&lt;bar&gt;woogie&lt;/bar&gt;&lt;a name=&quot;test&quot;/&gt;&lt;/foo&gt;
 * 
 * Template:
 *   $foo.bar.text
 *   $foo.find('a')
 *   $foo.a.name
 * 
 * Output:
 *   woogie
 *   &lt;a name=&quot;test&quot;/&gt;
 *   test
 * 
 * Configuration:
 * &lt;tools&gt;
 *   &lt;toolbox scope=&quot;application&quot;&gt;
 *     &lt;tool class=&quot;org.apache.velocity.tools.generic.XmlTool&quot;
 *              key=&quot;foo&quot; file=&quot;doc.xml&quot;/&gt;
 *   &lt;/toolbox&gt;
 * &lt;/tools&gt;
 * </pre>
 * 
 * </p>
 * <p>
 * Note that this tool is included in the default GenericTools configuration under the key "xml", but unless you set
 * safeMode="false" for it, you will only be able to parse XML strings. Safe mode is on by default and blocks access to
 * the {@link #read(Object)} method.
 * </p>
 * 
 * @author Nathan Bubna
 * @version $Revision$ $Date: 2006-11-27 10:49:37 -0800 (Mon, 27 Nov 2006) $
 * @since VelocityTools 2.0
 */

public class XmlTool implements ViewTool {
	public static final String FILE_KEY = "file";

	private List<Node> nodes;
	private static long ttl = 30;

	public XmlTool() {
	}

	public XmlTool(Node node) {
		this(Collections.singletonList(node));
	}

	public XmlTool(List<Node> nodes) {
		this.nodes = nodes;
	}

	/**
	 * Looks for the "file" parameter and automatically uses {@link #read(String)} to parse the file and set the
	 * resulting {@link Document} as the root node for this instance.
	 */
	protected void configure(ValueParser parser) {
		String file = parser.getString(FILE_KEY);
		if (file != null) {
			try {
				read(file);
			} catch (IllegalArgumentException iae) {
				throw iae;
			} catch (Exception e) {
				throw new RuntimeException("Could not read XML file at: " + file, e);
			}
		}
	}

	/**
	 * Sets a singular root {@link Node} for this instance.
	 */
	protected void setRoot(Node node) {
		if (node instanceof Document) {
			node = ((Document) node).getRootElement();
		}
		this.nodes = new ArrayList<Node>(1);
		this.nodes.add(node);
	}

	/**
	 * Creates a {@link URL} from the string and passes it to {@link #read(URL)}.
	 */
	protected void read(String file) throws Exception {
		URL url = ConversionUtils.toURL(file, this);

		if (url == null) {
			throw new IllegalArgumentException("Could not find file, classpath resource or standard URL for '" + file
					+ "'.");
		}
		read(url);
	}

	/**
	 * Reads, parses and creates a {@link Document} from the given {@link URL} and uses it as the root {@link Node} for
	 * this instance.
	 */
	protected void read(URL url) throws Exception {
		SAXReader reader = new SAXReader();
		setRoot(reader.read(url));
	}

	/**
	 * Parses the given XML string and uses the resulting {@link Document} as the root {@link Node}.
	 */
	protected void parse(String xml) throws Exception {
		setRoot(DocumentHelper.parseText(xml));
	}

	/**
	 * Set the cache Time To Live
	 * 
	 * @param ttl
	 *            time in minutes
	 * @author Oswaldo Gallango
	 */
	public static void setTTL(long ttl_time) {
		ttl = ttl_time;
	}

	/**
	 * Return the XmlTool Timte to Live in minutes
	 * 
	 * @return ttl
	 * @author Oswaldo Gallango
	 */
	public static long getTTL() {
		return ttl;
	}

	/**
	 * If safe mode is explicitly turned off for this tool, then this will accept either a {@link URL} or the string
	 * representation thereof. If valid, it will return a new {@link XmlTool} instance with that document as the root
	 * {@link Node}. If reading the URL or parsing its content fails or if safe mode is on (the default), this will
	 * return {@code null}. This methos use cache. if the TTL is not modifyed by dedefault the cache is refresh every
	 * 30 minutes
	 * 
	 * @throws Exception
	 */
	public XmlTool read(Object o) throws Exception {
		if (o == null) {
			return null;
		}

		XmlTool xml = new XmlTool();

		XmlToolDoc doc = XmlToolCache.getXmlToolDoc(String.valueOf(o));
		if (doc == null) {
			String xmlPath = "";
			if (o instanceof URL) {
				xml.read((URL) o);
			} else {
				String file = String.valueOf(o);
				xml.read(file);
			}

			doc = new XmlToolDoc();
			doc.setXmlPath(String.valueOf(o));
			doc.setXmlTool(xml);
			doc.setTtl(new Date().getTime() + (ttl * 60000));
			XmlToolCache.addXmlToolDoc(doc);

		} else {
			xml = doc.getXmlTool();
		}
		return xml;

	}

	/**
	 * This accepts XML in form. If the XML is valid, it will return a new {@link XmlTool} instance with the resulting
	 * XML document as the root {@link Node}. If parsing the content fails, this will return {@code null}.
	 * 
	 * @throws Exception
	 */
	public XmlTool parse(Object o) throws Exception {
		if (o == null) {
			return null;
		}
		String s = String.valueOf(o);

		XmlTool xml = new XmlTool();
		xml.parse(s);
		return xml;

	}

	/**
	 * This will first attempt to find an attribute with the specified name and return its value. If no such attribute
	 * exists or its value is {@code null}, this will attempt to convert the given value to a {@link Number} and get
	 * the result of {@link #get(Number)}. If the number conversion fails, then this will convert the object to a
	 * string. If that string does not contain a '/', it appends the result of {@link #getPath()} and a '/' to the front
	 * of it. Finally, it delegates the string to the {@link #find(String)} method and returns the result of that.
	 */
	public Object get(Object o) {
		if (isEmpty() || o == null) {
			return null;
		}
		String attr = attr(o);
		if (attr != null) {
			return attr;
		}
		Number i = ConversionUtils.toNumber(o);
		if (i != null) {
			return get(i);
		}
		String s = String.valueOf(o);
		if (s.length() == 0) {
			return null;
		}
		if (s.indexOf('/') < 0) {
			s = getPath() + '/' + s;
		}
		return find(s);
	}

	/**
	 * Asks {@link #get(Object)} for a "name" result. If none, this will return the result of {@link #getNodeName()}.
	 */
	public Object getName() {
		// give attributes and child elements priority
		Object name = get("name");
		if (name != null) {
			return name;
		}
		return getNodeName();
	}

	/**
	 * Returns the name of the root node. If the internal {@link Node} list has more than one {@link Node}, it will
	 * only return the name of the first node in the list.
	 */
	public String getNodeName() {
		if (isEmpty()) {
			return null;
		}
		return node().getName();
	}

	/**
	 * Returns the XPath that identifies the first/sole {@link Node} represented by this instance.
	 */
	public String getPath() {
		if (isEmpty()) {
			return null;
		}
		return node().getPath();
	}

	/**
	 * Returns the value of the specified attribute for the first/sole {@link Node} in the internal Node list for this
	 * instance, if that Node is an {@link Element}. If it is a non-Element node type or there is no value for that
	 * attribute in this element, then this will return {@code null}.
	 */
	public String attr(Object o) {
		if (o == null) {
			return null;
		}
		String key = String.valueOf(o);
		Node node = node();
		if (node instanceof Element) {
			return ((Element) node).attributeValue(key);
		}
		return null;
	}

	/**
	 * Returns a {@link Map} of all attributes for the first/sole {@link Node} held internally by this instance. If that
	 * Node is not an {@link Element}, this will return null.
	 */
	public Map<String, String> attributes() {
		Node node = node();
		if (node instanceof Element) {
			Map<String, String> attrs = new HashMap<String, String>();
			for (Iterator i = ((Element) node).attributeIterator(); i.hasNext();) {
				Attribute a = (Attribute) i.next();
				attrs.put(a.getName(), a.getValue());
			}
			return attrs;
		}
		return null;
	}

	/**
	 * Returns {@code true} if there are no {@link Node}s internally held by this instance.
	 */
	public boolean isEmpty() {
		return (nodes == null || nodes.isEmpty());
	}

	/**
	 * Returns the number of {@link Node}s internally held by this instance.
	 */
	public int size() {
		if (isEmpty()) {
			return 0;
		}
		return nodes.size();
	}

	/**
	 * Returns an {@link Iterator} that returns new {@link XmlTool} instances for each {@link Node} held internally by
	 * this instance.
	 */
	public Iterator<XmlTool> iterator() {
		if (isEmpty()) {
			return null;
		}
		return new NodeIterator(nodes.iterator());
	}

	/**
	 * Returns an {@link XmlTool} that wraps only the first {@link Node} from this instance's internal Node list.
	 */
	public XmlTool getFirst() {
		if (size() == 1) {
			return this;
		}
		return new XmlTool(node());
	}

	/**
	 * Returns an {@link XmlTool} that wraps only the last {@link Node} from this instance's internal Node list.
	 */
	public XmlTool getLast() {
		if (size() == 1) {
			return this;
		}
		return new XmlTool(nodes.get(size() - 1));
	}

	/**
	 * Returns an {@link XmlTool} that wraps the specified {@link Node} from this instance's internal Node list.
	 */
	public XmlTool get(Number n) {
		if (n == null) {
			return null;
		}
		int i = n.intValue();
		if (i < 0 || i > size() - 1) {
			return null;
		}
		return new XmlTool(nodes.get(i));
	}

	/**
	 * Returns the first/sole {@link Node} from this instance's internal Node list, if any.
	 */
	public Node node() {
		if (isEmpty()) {
			return null;
		}
		return nodes.get(0);
	}

	/**
	 * Converts the specified object to a String and calls {@link #find(String)} with that.
	 */
	public XmlTool find(Object o) {
		if (o == null || isEmpty()) {
			return null;
		}
		return find(String.valueOf(o));
	}

	/**
	 * Performs an XPath selection on the current set of {@link Node}s held by this instance and returns a new
	 * {@link XmlTool} instance that wraps those results. If the specified value is null or this instance does not
	 * currently hold any nodes, then this will return {@code null}. If the specified value, when converted to a
	 * string, does not contain a '/' character, then it has "//" prepended to it. This means that a call to
	 * {@code $xml.find("a")} is equivalent to calling {@code $xml.find("//a")}. The full range of XPath selectors is
	 * supported here.
	 */
	public XmlTool find(String xpath) {
		if (xpath == null || xpath.length() == 0) {
			return null;
		}
		if (xpath.indexOf('/') < 0) {
			xpath = "//" + xpath;
		}
		List<Node> found = new ArrayList<Node>();
		for (Node n : nodes) {
			found.addAll((List<Node>) n.selectNodes(xpath));
		}
		if (found.isEmpty()) {
			return null;
		}
		return new XmlTool(found);
	}

	/**
	 * Returns a new {@link XmlTool} instance that wraps the parent {@link Element} of the first/sole {@link Node} being
	 * wrapped by this instance.
	 */
	public XmlTool getParent() {
		if (isEmpty()) {
			return null;
		}
		Element parent = node().getParent();
		if (parent == null) {
			return null;
		}
		return new XmlTool(parent);
	}

	/**
	 * Returns a new {@link XmlTool} instance that wraps the parent {@link Element}s of each of the {@link Node}s
	 * being wrapped by this instance. This does not return all ancestors, just the immediate parents.
	 */
	public XmlTool parents() {
		if (isEmpty()) {
			return null;
		}
		if (size() == 1) {
			return getParent();
		}
		List<Node> parents = new ArrayList<Node>(size());
		for (Node n : nodes) {
			Element parent = n.getParent();
			if (parent != null && !parents.contains(parent)) {
				parents.add(parent);
			}
		}
		if (parents.isEmpty()) {
			return null;
		}
		return new XmlTool(parents);
	}

	/**
	 * Returns a new {@link XmlTool} instance that wraps all the child {@link Element}s of all the current internally
	 * held nodes that are {@link Element}s themselves.
	 */
	public XmlTool children() {
		if (isEmpty()) {
			return null;
		}
		List<Node> kids = new ArrayList<Node>();
		for (Node n : nodes) {
			if (n instanceof Element) {
				kids.addAll((List<Node>) ((Element) n).elements());
			}
		}
		return new XmlTool(kids);
	}

	/**
	 * Returns the concatenated text content of all the internally held nodes. Obviously, this is most useful when only
	 * one node is held.
	 */
	public String getText() {
		if (isEmpty()) {
			return null;
		}
		StringBuilder out = new StringBuilder();
		for (Node n : nodes) {
			String text = n.getText();
			if (text != null) {
				out.append(text);
			}
		}
		String result = out.toString().trim();
		if (result.length() > 0) {
			return result;
		}
		return null;
	}

	/**
	 * If this instance has no XML {@link Node}s, then this returns the result of {@code super.toString()}. Otherwise,
	 * it returns the XML (as a string) of all the internally held nodes that are not {@link Attribute}s. For
	 * attributes, only the value is used.
	 */
	public String toString() {
		if (isEmpty()) {
			return super.toString();
		}
		StringBuilder out = new StringBuilder();
		for (Node n : nodes) {
			if (n instanceof Attribute) {
				out.append(n.getText().trim());
			} else {
				out.append(n.asXML());
			}
		}
		return out.toString();
	}

	/**
	 * Iterator implementation that wraps a Node list iterator to return new XmlTool instances for each item in the
	 * wrapped iterator.s
	 */
	public static class NodeIterator implements Iterator<XmlTool> {
		private Iterator<Node> i;

		public NodeIterator(Iterator<Node> i) {
			this.i = i;
		}

		public boolean hasNext() {
			return i.hasNext();
		}

		public XmlTool next() {
			return new XmlTool(i.next());
		}

		public void remove() {
			i.remove();
		}
	}

	public void init(Object arg0) {

	}
}