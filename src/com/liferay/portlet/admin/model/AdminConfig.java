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

package com.liferay.portlet.admin.model;

import org.exolab.castor.xml.CastorException;

import com.dotmarketing.util.Logger;
import com.liferay.portal.SystemException;
import com.liferay.util.xml.Serializer;

/**
 * <a href="AdminConfig.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.10 $
 *
 */
public class AdminConfig extends AdminConfigModel {

	public AdminConfig() {
		super();
	}

	public AdminConfig(String configId) {
		super(configId);
	}

	public AdminConfig(String configId, String companyId, String type,
					   String name, String config) {

		super(configId, companyId, type, name, config);

		setConfig(config);
	}

	public void setConfig(String config) {
		try {
			Class c = null;

			if (getType().startsWith(JournalConfig.JOURNAL_CONFIG)) {
				c = JournalConfig.class;
			}
			else if (getType().equals(ShoppingConfig.SHOPPING_CONFIG)) {
				c = ShoppingConfig.class;
			}
			else if (getType().equals(UserConfig.USER_CONFIG)) {
				c = UserConfig.class;
			}

			_configObj = Serializer.readObject(c, config);

			super.setConfig(config);
		}
		catch (CastorException ce) {
			Logger.error(this,ce.getMessage(),ce);
		}
	}

	public Object getConfigObj() {
		return _configObj;
	}

	public void setConfigObj(Object configObj) throws SystemException {
		_configObj = configObj;

		try {
			super.setConfig(Serializer.writeObject(configObj));
		}
		catch (CastorException ce) {
			Logger.error(this,ce.getMessage(),ce);
		}
	}

	private Object _configObj = null;

}