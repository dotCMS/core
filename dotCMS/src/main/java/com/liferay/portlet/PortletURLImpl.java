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

import java.security.Key;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.dotcms.repackage.javax.portlet.PortletMode;
import com.dotcms.repackage.javax.portlet.PortletModeException;
import com.dotcms.repackage.javax.portlet.PortletRequest;
import com.dotcms.repackage.javax.portlet.PortletSecurityException;
import com.dotcms.repackage.javax.portlet.PortletSession;
import com.dotcms.repackage.javax.portlet.PortletURL;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.javax.portlet.WindowStateException;
import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.util.Logger;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.BrowserSniffer;
import com.liferay.util.Encryptor;
import com.liferay.util.EncryptorException;
import com.liferay.util.Http;
import com.liferay.util.StringPool;

/**
 * <a href="PortletURLImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.41 $
 *
 */
public class PortletURLImpl implements PortletURL {
	public PortletURLImpl(ActionRequestImpl req, String portletName,
						  String layoutId, boolean action) {

		this(req.getHttpServletRequest(), portletName, layoutId, action);

		_portletReq = req;
	}

	public PortletURLImpl(RenderRequestImpl req, String portletName,
						  String layoutId, boolean action) {

		this(req.getHttpServletRequest(), portletName, layoutId, action);

		_portletReq = req;
	}

	public PortletURLImpl(HttpServletRequest req, String portletName,
						  String layoutId, boolean action) {

		_req = req;
		_portletName = portletName;
		_layoutId = layoutId;
		_secure = req.isSecure();
		_action = action;
		_params = new LinkedHashMap();
		_angularCurrentPortlet = req.getParameter(WebKeys.PORTLET_URL_CURRENT_ANGULAR_PORTLET);
	}

	public WindowState getWindowState() {
		return _windowState;
	}

	public void setWindowState(WindowState windowState)
		throws WindowStateException {

		if (_portletReq != null) {
			if (!_portletReq.isWindowStateAllowed(windowState)) {
				throw new WindowStateException(
					windowState.toString(), windowState);
			}
		}

		_windowState = windowState;
	}

	public PortletMode getPortletMode() {
		return _portletMode;
	}

	public void setPortletMode(PortletMode portletMode)
		throws PortletModeException {
		_portletMode = portletMode;
	}

	public void setParameter(String name, String value) {
		if ((name == null) || (value == null)) {
			throw new IllegalArgumentException();
		}

		setParameter(name, new String[] {value});
	}

	public void setParameter(String name, String[] values) {
		if ((name == null) || (values == null)) {
			throw new IllegalArgumentException();
		}

		for (int i = 0; i < values.length; i++) {
			if (values[i] == null) {
				throw new IllegalArgumentException();
			}
		}

		_params.put(name, values);
	}

	public void setParameters(Map params) {
		if (params == null) {
			throw new IllegalArgumentException();
		}
		else {
			Map newParams = new LinkedHashMap();

			Iterator itr = params.entrySet().iterator();

			while (itr.hasNext()) {
				Map.Entry entry = (Map.Entry)itr.next();

				Object key = entry.getKey();
				Object value = entry.getValue();

				if (key == null) {
					throw new IllegalArgumentException();
				}
				else if (value == null) {
					throw new IllegalArgumentException();
				}

				if (value instanceof String[]) {
					newParams.put(key, value);
				}
				else {
					throw new IllegalArgumentException();
				}
			}

			_params = newParams;
		}
	}

	public void setSecure(boolean secure) throws PortletSecurityException {
		_secure = secure;
	}

	public void setAction(boolean action) {
		_action = action;
	}

	public void setAnchor(boolean anchor) {
		_anchor = anchor;
	}

	public void setEncrypt(boolean encrypt) {
		_encrypt = encrypt;
	}

	public String toString() {

        String ctxPath = (_portletReq != null) ?
            ctxPath = (String)_portletReq.getPortletSession().getAttribute(
                WebKeys.CTX_PATH, PortletSession.APPLICATION_SCOPE)
            : (String)_req.getSession().getAttribute(WebKeys.CTX_PATH);

        ctxPath = (null ==ctxPath) ? "/c" : ctxPath;
        
        StringBuffer sb = new StringBuffer();

        sb.append(ctxPath);
		sb.append("/portal");
		sb.append(PortalUtil.getAuthorizedPath(_req));
		sb.append("/layout?");

		Key key = null;
		try {
			if (_encrypt) {
				key = PortalUtil.getCompany(_req).getKeyObj();
			}
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}

		sb.append(WebKeys.PORTLET_URL_LAYOUT_ID);
		sb.append(StringPool.EQUAL);
		sb.append(_processValue(key, _layoutId));
		sb.append(StringPool.AMPERSAND);

		sb.append(WebKeys.PORTLET_URL_PORTLET_NAME);
		sb.append(StringPool.EQUAL);
		sb.append(_processValue(key, _portletName));
		sb.append(StringPool.AMPERSAND);

		sb.append(WebKeys.PORTLET_URL_ACTION);
		sb.append(StringPool.EQUAL);
		sb.append(_action ? _processValue(key, ACTION_TRUE) :
			_processValue(key, ACTION_FALSE));
		sb.append(StringPool.AMPERSAND);

		if (_windowState != null) {
			sb.append(WebKeys.PORTLET_URL_WINDOW_STATE);
			sb.append(StringPool.EQUAL);
			sb.append(_processValue(key, _windowState.toString()));
			sb.append(StringPool.AMPERSAND);
		}


		if (_angularCurrentPortlet != null) {
			sb.append(WebKeys.PORTLET_URL_CURRENT_ANGULAR_PORTLET);
			sb.append(StringPool.EQUAL);
			sb.append(_processValue(key, _angularCurrentPortlet));
			sb.append(StringPool.AMPERSAND);
		}

		if (_portletMode != null) {
			sb.append(WebKeys.PORTLET_URL_PORTLET_MODE);
			sb.append(StringPool.EQUAL);
			sb.append(_processValue(key, _portletMode.toString()));
			sb.append(StringPool.AMPERSAND);
		}

		Iterator itr = _params.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry entry = (Map.Entry)itr.next();

			String name =
				PortalUtil.getPortletNamespace(_portletName) +
				(String)entry.getKey();
			String[] values = (String[])entry.getValue();

			for (int i = 0; i < values.length; i++) {
				sb.append(name);
				sb.append(StringPool.EQUAL);
				sb.append(_processValue(key, values[i]));

				if ((i + 1 < values.length) || itr.hasNext()) {
					sb.append(StringPool.AMPERSAND);
				}
			}
		}

		if (_encrypt) {
			sb.append(StringPool.AMPERSAND + WebKeys.ENCRYPT + "=1");
		}

		if (!BrowserSniffer.is_ns_4(_req)) {
			if (_anchor && (_windowState != null) &&
				(!_windowState.equals(WindowState.MAXIMIZED))) {

				if (sb.lastIndexOf(StringPool.AMPERSAND) != (sb.length() - 1)) {
					sb.append(StringPool.AMPERSAND);
				}

				sb.append("#p_").append(_portletName);
			}
		}

		return sb.toString();
	}

	protected String getLayoutId() {
		return _layoutId;
	}

	protected Map getParams() {
		return _params;
	}

	protected Portlet getPortlet() {
		if (_portlet == null) {
			try {
				_portlet = PortletManagerUtil.getPortletById(
					PortalUtil.getCompanyId(_req), _portletName);
			}
			catch (SystemException se) {
				Logger.error(this,se.getMessage(),se);
			}
		}

		return _portlet;
	}

	protected String getPortletName() {
		return _portletName;
	}

	protected PortletRequest getPortletReq() {
		return _portletReq;
	}

	protected HttpServletRequest getReq() {
		return _req;
	}

	protected boolean isAction() {
		return _action;
	}

	protected boolean isAnchor() {
		return _anchor;
	}

	protected boolean isEncrypt() {
		return _encrypt;
	}

	protected boolean isSecure() {
		return _secure;
	}

	private String _processValue(Key key, String value) {
		if (key == null) {
			return Http.encodeURL(value);
		}
		else {
			try {
				return Http.encodeURL(Encryptor.encrypt(key, value));
			}
			catch (EncryptorException ee) {
				return value;
			}
		}
	}

	protected final String ACTION_FALSE = "0";
	protected final String ACTION_TRUE = "1";

	private HttpServletRequest _req;
	private PortletRequest _portletReq;
	private String _portletName;
	private Portlet _portlet;
	private String _layoutId;
	private boolean _action;
	private WindowState _windowState;
	private PortletMode _portletMode;
	private Map _params;
	private boolean _secure;
	private boolean _anchor = true;
	private boolean _encrypt = false;
	private String _angularCurrentPortlet = null;
}