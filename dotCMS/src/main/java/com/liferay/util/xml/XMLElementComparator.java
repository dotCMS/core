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
import org.dom4j.Element;

/**
 * <a href="XMLElementComparator.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Jorge Ferrer
 * @version $Revision: 1.3 $
 *
 */
public class XMLElementComparator extends ElementComparator {

	public XMLElementComparator(XMLDescriptor descriptor) {
		_descriptor = descriptor;
	}

	public int compare(Object obj1, Object obj2) {
		Element el1 = (Element)obj1;
		Element el2 = (Element)obj2;

		if (_descriptor.areEqual(el1, el2)) {
			return 0;
		}
		else {
			return -1;
		}
	}

	public boolean canJoinChildren(Element element) {
		return _descriptor.canJoinChildren(element);
	}

	private XMLDescriptor _descriptor;

}