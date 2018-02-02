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

import com.liferay.util.xml.ElementIdentifier;
import org.dom4j.Document;

/**
 * <a href="FacesXMLDescriptor.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Jorge Ferrer
 * @version $Revision: 1.5 $
 *
 */
public class FacesXMLDescriptor extends SimpleXMLDescriptor {

	public boolean canHandleType(String doctype, Document root) {
		if (doctype.indexOf("faces-config") != -1) {
			return true;
		}
		else {
			return false;
		}
	}

	public String[] getRootChildrenOrder() {
		return _ROOT_ORDERED_CHILDREN;
	}

	public ElementIdentifier[] getElementsIdentifiedByAttribute() {
		return _ELEMENTS_IDENTIFIED_BY_ATTR;
	}

	public ElementIdentifier[] getElementsIdentifiedByChild() {
		return _ELEMENTS_IDENTIFIED_BY_CHILD;
	}

	public String[] getUniqueElements() {
		return _UNIQUE_ELEMENTS;
	}

	public String[] getJoinableElements() {
		return _JOINABLE_ELEMENTS;
	}

	private static final String[] _ROOT_ORDERED_CHILDREN = {
		"application", "factory", "component", "converter", "managed-bean",
		"navigation-rule", "referenced-bean", "render-kit", "lifecycle",
		"validator"
	};

	private static final ElementIdentifier[] _ELEMENTS_IDENTIFIED_BY_ATTR = {
	};

	private static final ElementIdentifier[] _ELEMENTS_IDENTIFIED_BY_CHILD = {
	};

	private static final String[] _UNIQUE_ELEMENTS = {
	};

	private static final String[] _JOINABLE_ELEMENTS = {
	};

}