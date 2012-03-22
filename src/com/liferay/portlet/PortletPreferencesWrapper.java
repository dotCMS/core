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

package com.liferay.portlet;

import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;

import javax.portlet.PortletPreferences;
import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;

import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;

/**
 * <a href="PortletPreferencesWrapper.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */
public class PortletPreferencesWrapper
	implements PortletPreferences, Serializable {

	public PortletPreferencesWrapper(PortletPreferences prefs, boolean action) {
		_prefs = prefs;
		_action = action;
	}

	public Map getMap() {
		return _prefs.getMap();
	}

	public Enumeration getNames() {
		return _prefs.getNames();
	}

	public String getValue(String key, String def) {
		return _prefs.getValue(key, def);
	}

	public void setValue(String key, String value) throws ReadOnlyException {
		_prefs.setValue(key, value);
	}

	public String[] getValues(String key, String[] def) {
		return _prefs.getValues(key, def);
	}

	public void setValues(String key, String[] values)
		throws ReadOnlyException {

		_prefs.setValues(key, values);
	}

	public boolean isReadOnly(String key) {
		return _prefs.isReadOnly(key);
	}

	public void reset(String key) throws ReadOnlyException {
		_prefs.reset(key);
	}

	public void store() throws IOException, ValidatorException {
		if (GetterUtil.getBoolean(PropsUtil.get(PropsUtil.TCK_URL))) {

			// Be strict to pass the TCK

			if (_action) {
				_prefs.store();
			}
			else {
				throw new IllegalStateException(
					"Preferences cannot be stored inside a render call");
			}
		}
		else {

			// Relax so that poorly written portlets can still work

			_prefs.store();
		}
	}

	public PortletPreferencesImpl getPreferencesImpl() {
		return (PortletPreferencesImpl)_prefs;
	}

	private PortletPreferences _prefs = null;
	private boolean _action;

}