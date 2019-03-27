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

import com.liferay.portal.model.BaseModel;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.Xss;

/**
 * <a href="AdminConfigModel.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.74 $
 *
 */
public class AdminConfigModel extends BaseModel {
	public static boolean CACHEABLE = GetterUtil.get(PropsUtil.get(
				"value.object.cacheable.com.liferay.portlet.admin.model.AdminConfig"),
			VALUE_OBJECT_CACHEABLE);
	public static int MAX_SIZE = GetterUtil.get(PropsUtil.get(
				"value.object.max.size.com.liferay.portlet.admin.model.AdminConfig"),
			VALUE_OBJECT_MAX_SIZE);
	
	public static long LOCK_EXPIRATION_TIME = GetterUtil.getLong(PropsUtil.get(
				"lock.expiration.time.com.liferay.portlet.admin.model.AdminConfigModel"));

	public AdminConfigModel() {
	}

	public AdminConfigModel(String configId) {
		_configId = configId;
		setNew(true);
	}

	public AdminConfigModel(String configId, String companyId, String type,
		String name, String config) {
		_configId = configId;
		_companyId = companyId;
		_type = type;
		_name = name;
		_config = config;
	}

	public String getPrimaryKey() {
		return _configId;
	}

	public String getConfigId() {
		return _configId;
	}

	public void setConfigId(String configId) {
		if (((configId == null) && (_configId != null)) ||
				((configId != null) && (_configId == null)) ||
				((configId != null) && (_configId != null) &&
				!configId.equals(_configId))) {

			_configId = configId;
			setModified(true);
		}
	}

	public String getCompanyId() {
		return _companyId;
	}

	public void setCompanyId(String companyId) {
		if (((companyId == null) && (_companyId != null)) ||
				((companyId != null) && (_companyId == null)) ||
				((companyId != null) && (_companyId != null) &&
				!companyId.equals(_companyId))) {
		    
			_companyId = companyId;
			setModified(true);
		}
	}

	public String getType() {
		return _type;
	}

	public void setType(String type) {
		if (((type == null) && (_type != null)) ||
				((type != null) && (_type == null)) ||
				((type != null) && (_type != null) && !type.equals(_type))) {

			_type = type;
			setModified(true);
		}
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		if (((name == null) && (_name != null)) ||
				((name != null) && (_name == null)) ||
				((name != null) && (_name != null) && !name.equals(_name))) {

			_name = name;
			setModified(true);
		}
	}

	public String getConfig() {
		return _config;
	}

	public void setConfig(String config) {
		if (((config == null) && (_config != null)) ||
				((config != null) && (_config == null)) ||
				((config != null) && (_config != null) &&
				!config.equals(_config))) {

			_config = config;
			setModified(true);
		}
	}

	public BaseModel getProtected() {
		return null;
	}

	public void protect() {
	}

	public Object clone() {
		return new AdminConfig(getConfigId(), getCompanyId(), getType(),
			getName(), getConfig());
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		AdminConfig adminConfig = (AdminConfig)obj;
		String pk = adminConfig.getPrimaryKey();

		return getPrimaryKey().compareTo(pk);
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		AdminConfig adminConfig = null;

		try {
			adminConfig = (AdminConfig)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		String pk = adminConfig.getPrimaryKey();

		if (getPrimaryKey().equals(pk)) {
			return true;
		}
		else {
			return false;
		}
	}

	public int hashCode() {
		return getPrimaryKey().hashCode();
	}

	private String _configId;
	private String _companyId;
	private String _type;
	private String _name;
	private String _config;
}