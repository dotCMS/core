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

package com.liferay.portal.auth;

import java.io.Serializable;

import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;

/**
 * <a href="HttpPrincipal.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.8 $
 *
 */
public class HttpPrincipal implements Serializable {
    private static final long serialVersionUID = -5575766192585856481L;

    public HttpPrincipal(String url) {
		_url = url;
	}

	public HttpPrincipal(String url, String userId, String password) throws PasswordException {
		this(url, userId, password, false);
	}

	public HttpPrincipal(String url, String userId, String password, boolean digested) throws PasswordException {
		_url = url;
		_userId = userId;

		if (digested) {
			_password = password;
		}
		else {
			_password = PasswordFactoryProxy.generateHash(password);
		}
	}

	public String getUrl() {
		return _url;
	}

	public String getCompanyId() {
		return _companyId;
	}

	public void setCompanyId(String companyId) {
		_companyId = companyId;
	}

	public String getUserId() {
		return _userId;
	}

	public String getPassword() {
		return _password;
	}

	private String _url;
	private String _companyId;
	private String _userId;
	private String _password;

}