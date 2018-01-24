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

package com.liferay.util.xml;

import com.liferay.util.xml.descriptor.XMLDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.dom4j.Document;
import org.dom4j.Element;

/**
 * <a href="XMLMerger.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Alan Zimmerman
 * @author  Jorge Ferrer
 * @version $Revision: 1.6 $
 *
 */
public class XMLMerger {

	public XMLMerger(XMLDescriptor descriptor) {
		_descriptor = descriptor;
	}

	public XMLElementComparator getElementComparator() {
		return new XMLElementComparator(_descriptor);
	}

	public Document merge(Document masterDoc, Document slaveDoc) {
		Document mergedDoc = (Document)masterDoc.clone();

		Element root1 = mergedDoc.getRootElement();
		Element root2 = slaveDoc.getRootElement();

		List children1 = root1.elements();
		List children2 = root2.elements();

		for (int i = 0; i < children2.size(); i++) {
			Element el2 = (Element)children2.get(i);

			Element el2Clone = (Element)el2.clone();
			el2Clone.detach();

			root1.add(el2Clone);
		}

		organizeXML(mergedDoc);

		return mergedDoc;
	}

	public void organizeXML(Document doc) {
		Element root = doc.getRootElement();

		_orderChildren(root, _descriptor.getRootChildrenOrder());
		_mergeDuplicateElements(root, getElementComparator());
	}

	private void _addChildren(Element first, Collection childrenToJoin) {
	    Collection clones = new Vector();

		Iterator itr = childrenToJoin.iterator();

		while (itr.hasNext()) {
	        clones.add(((Element)itr.next()).clone());
	    }

		first.elements().addAll(clones);
	}

	private boolean _containsObjectEqualTo(
		Element example, List list, ElementComparator comparator) {

		Iterator itr = list.iterator();

		while (itr.hasNext()) {
			Element candidate = (Element)itr.next();

			if (comparator.compare(example, candidate) == 0) {
				return true;
			}
		}

		return false;
	}

	private Element _findObjectEqualTo(
		Element example, List list, ElementComparator comparator) {

		Iterator itr = list.iterator();

		while (itr.hasNext()) {
	        Element candidate = (Element)itr.next();

			if (comparator.compare(example, candidate) == 0) {
	            return candidate;
	        }
	    }

		return example;
	}

	private void _mergeDuplicateElements(
		Element el, ElementComparator comparator) {

		if (el.elements().size() > 0) {
			List children = el.elements();

			List originals = new ArrayList();
			List duplicates = new ArrayList();

			for (int i = 0; i < children.size(); i++) {
				Element child = (Element)children.get(i);

				if (_containsObjectEqualTo(child, originals, comparator)) {
					if (comparator.shouldJoinChildren(child)) {
						Element first =
							_findObjectEqualTo(child, originals, comparator);

						Collection childrenToJoin = child.elements();

						_addChildren(first, childrenToJoin);
					}

					duplicates.add(child);
				}
				else {
					originals.add(child);
				}
			}

			for (int i = 0; i < duplicates.size(); i++) {
				Element duplicate = (Element)duplicates.get(i);

				duplicate.detach();
			}

			Iterator itr = originals.iterator();

			while (itr.hasNext()) {
				Element child = (Element)itr.next();

				_mergeDuplicateElements(child, comparator);
			}
		}
	}

	private void _orderChildren(
		Element parent, String[] orderedChildrenNames) {

		if (orderedChildrenNames == null) {
			return;
		}

		List elements = new ArrayList();

		for (int i = 0; i < orderedChildrenNames.length; i++) {
			elements.addAll(parent.elements(orderedChildrenNames[i]));
		}

		for (int i = 0; i < elements.size(); i++) {
			Element el = (Element)elements.get(i);
			el.detach();
			parent.add(el);
		}
	}

	private XMLDescriptor _descriptor;

}