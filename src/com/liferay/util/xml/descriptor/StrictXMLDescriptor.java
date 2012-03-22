/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util.xml.descriptor;

import java.util.Comparator;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;

import com.liferay.util.xml.AttributeComparator;
import com.liferay.util.xml.ElementComparator;

/**
 * <a href="StrictXMLDescriptor.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Jorge Ferrer
 * @version $Revision: 1.3 $
 *
 */
public class StrictXMLDescriptor implements XMLDescriptor {

	public boolean areEqual(Element el1, Element el2) {
		if (_compare(el1, el2) == 0) {
			return true;
		}
		else {
			return false;
		}
	}

	public boolean canHandleType(String doctype, Document root) {
		return false;
	}

	public boolean canJoinChildren(Element element) {
		return false;
	}

	public String[] getRootChildrenOrder() {
		return _ROOT_ORDERED_CHILDREN;
	}

	private int _compare(Object obj1, Object obj2) {
		Element el1 = (Element)obj1;
		Element el2 = (Element)obj2;

		String el1Name = el1.getName();
		String el2Name = el2.getName();

		if (!el1Name.equals(el2Name)) {
			return el1Name.compareTo(el2Name);
		}

		String el1Text = el1.getTextTrim();
		String el2Text = el2.getTextTrim();

		if (!el1Text.equals(el2Text)) {
			return el1Text.compareTo(el2Text);
		}

		int attributeComparison = _compareAttributes(el1, el2);
		if (attributeComparison != 0) {
			return attributeComparison;
		}

		int childrenComparison = _compareChildren(el1, el2);
		if (childrenComparison != 0) {
			return childrenComparison;
		}

		return 0;
	}

	private int _compareAttributes(Element el1, Element el2) {
		List el1Attrs = el1.attributes();
		List el2Attrs = el2.attributes();

		if (el1Attrs.size() < el2Attrs.size()) {
			return -1;
		}
		else if (el1Attrs.size() > el2Attrs.size()) {
			return 1;
		}

		for (int i = 0; i < el1Attrs.size(); i++) {
			Attribute attr = (Attribute)el1Attrs.get(i);

			int value = _contains(el2Attrs, attr, new AttributeComparator());
			if (value != 0) {
				return value;
			}
		}
		return -1;
	}

	private int _compareChildren(Element el1, Element el2) {
		List el1Children = el1.elements();
		List el2Children = el2.elements();

		if (el1Children.size() < el2Children.size()) {
			return -1;
		}
		else if (el1Children.size() > el2Children.size()) {
			return 1;
		}

		for (int i = 0; i < el1Children.size(); i++) {
			Element el = (Element)el1Children.get(i);

			int value = _contains(el2Children, el, new ElementComparator());

			if (value != 0) {
				return value;
			}
		}
		return -1;
	}

	private int _contains(List list, Object obj, Comparator comparator) {
		int firstValue = -1;

		for (int i = 0; i < list.size(); i++) {
			Object o = list.get(i);

			int value = comparator.compare(obj, o);

			if (i == 0) {
				firstValue = value;
			}

			if (value == 0) {
				return 0;
			}
		}

		return firstValue;
	}

	private static final String[] _ROOT_ORDERED_CHILDREN = {
	};

}