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

package com.liferay.portlet.admin.ejb;

/**
 * <a href="AdminConfigHBM.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.15 $
 *
 */
public class AdminConfigHBM {
	protected AdminConfigHBM() {
	}

	protected AdminConfigHBM(String configId) {
		_configId = configId;
	}

	protected AdminConfigHBM(String configId, String companyId, String type,
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

	protected void setPrimaryKey(String pk) {
		_configId = pk;
	}

	protected String getConfigId() {
		return _configId;
	}

	protected void setConfigId(String configId) {
		_configId = configId;
	}

	protected String getCompanyId() {
		return _companyId;
	}

	protected void setCompanyId(String companyId) {
		_companyId = companyId;
	}

	protected String getType() {
		return _type;
	}

	protected void setType(String type) {
		_type = type;
	}

	protected String getName() {
		return _name;
	}

	protected void setName(String name) {
		_name = name;
	}

	protected String getConfig() {
		return _config;
	}

	protected void setConfig(String config) {
		_config = config;
	}

	private String _configId;
	private String _companyId;
	private String _type;
	private String _name;
	private String _config;
}