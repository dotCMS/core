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

package com.liferay.portal.util;

import java.util.Collections;
import java.util.Set;

import com.liferay.util.CollectionFactory;
import com.liferay.util.GetterUtil;

/**
 * <a href="PortalInstances.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.7 $
 *
 */
public class PortalInstances {

	public static String getDefaultCompanyId() {
		String[] companyIds = getCompanyIds();

		if (companyIds.length > 0) {
			return companyIds[0];
		}
		else {
			return null;
		}
	}

	public static String[] getCompanyIds() {
		return _getInstance()._getCompanyIds();
	}

	public static boolean init(String companyId) {
		return _getInstance()._init(companyId);
	}

	public static boolean matches() {
		return _getInstance()._matches();
	}

	private static PortalInstances _getInstance() {
		if (_instance == null) {
			synchronized (PortalInstances.class) {
				if (_instance == null) {
					_instance = new PortalInstances();
				}
			}
		}

		return _instance;
	}

	private PortalInstances() {
		_companyIds =
			Collections.synchronizedSet(CollectionFactory.getHashSet());
	}

	private String[] _getCompanyIds() {
		return (String[])_companyIds.toArray(new String[0]);
	}

	private boolean _init(String companyId) {
		return _companyIds.add(companyId);
	}

	private boolean _matches() {
		int instances =
			GetterUtil.get(PropsUtil.get(PropsUtil.PORTAL_INSTANCES), 1);

		if (instances > _companyIds.size()) {
			return false;
		}

		return true;
	}

	private static PortalInstances _instance;

	private Set _companyIds;

}