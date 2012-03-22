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

package com.liferay.portal.form;

import java.util.Properties;

import org.apache.struts.action.ActionForm;

import com.liferay.util.NullSafeProperties;

/**
 * <a href="LayoutForm.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Javier Bermejo
 * @version $Revision: 1.1 $
 *
 */
public class LayoutForm extends ActionForm {

	public LayoutForm() {
	}

	public Object getTypeSettingsProperties(String key) {
		if (!_typeSettingsProperties.isEmpty()) {
			return _typeSettingsProperties.get(key);
		}
		else {
			return null;
		}
	}

	public Properties getTypeSettingsProperties() {
		return _typeSettingsProperties;
	}

	public void setTypeSettingsProperties(Properties typeSettingsProperties) {
		_typeSettingsProperties = typeSettingsProperties;
	}

	public void setTypeSettingsProperties(String key, Object value) {
		_typeSettingsProperties.put(key, value);
	}

	private static final long serialVersionUID = 1L;

	private Properties _typeSettingsProperties = new NullSafeProperties();

}