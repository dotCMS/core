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
 * <a href="WebXMLDescriptor.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Jorge Ferrer
 * @version $Revision: 1.5 $
 *
 */
public class WebXMLDescriptor extends SimpleXMLDescriptor {

	public boolean canHandleType(String doctype, Document root) {
		if (doctype.indexOf("web-app") != -1) {
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
		"icon", "display-name", "description", "distributable", "context-param",
		"filter", "filter-mapping", "listener", "servlet", "servlet-mapping",
		"session-config", "mime-mapping", "welcome-file-list", "error-page",
		"taglib", "resource-env-ref", "resource-ref", "security-constraint",
		"login-config", "security-role", "env-entry", "ejb-ref", "ejb-local-ref"
	};

	private static final ElementIdentifier[] _ELEMENTS_IDENTIFIED_BY_ATTR = {
	};

	private static final ElementIdentifier[] _ELEMENTS_IDENTIFIED_BY_CHILD = {
		new ElementIdentifier("context-param", "param-name"),
		new ElementIdentifier("filter", "filter-name"),
		//new ElementIdentifier("filter-mapping", "filter-name"),
		new ElementIdentifier("servlet", "servlet-name"),
		new ElementIdentifier("servlet-mapping", "servlet-name"),
		new ElementIdentifier("init-param", "param-name"),
		new ElementIdentifier("resource-env-ref", "res-env-ref-name"),
		new ElementIdentifier("resource-ref", "res-ref-name"),
		new ElementIdentifier("ejb-local-ref", "ejb-ref-name")
	};

	private static final String[] _UNIQUE_ELEMENTS = {
		"icon", "display-name", "description", "distributable",
		"session-config", "welcome-file-list", "login-config"
	};

	private static final String[] _JOINABLE_ELEMENTS = {
		"welcome-file-list"
	};

}