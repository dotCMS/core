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

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletContext;

import com.dotmarketing.util.Logger;
import com.liferay.portal.model.PortletInfo;
import com.liferay.portal.servlet.PortletContextPool;
import com.liferay.portal.servlet.PortletContextWrapper;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.CollectionFactory;

/**
 * <a href="PortletConfigImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.19 $
 *
 */
public class PortletConfigImpl implements PortletConfig {

	public static String WAR_SEPARATOR = "_WAR_";
  public PortletConfigImpl(String portletName, PortletContext portletCtx,
      Map params, String resourceBundle) {
    this(portletName,portletCtx, params, resourceBundle, null);
    
  }
	public PortletConfigImpl(String portletName, PortletContext portletCtx,
							 Map params, String resourceBundle,
							 PortletInfo portletInfo) {

		_portletId = portletName;

		int pos = _portletId.indexOf(WAR_SEPARATOR);
		if (pos != -1) {
			_portletName = _portletId.substring(
				pos + WAR_SEPARATOR.length(), _portletId.length());

			_warFile = true;
		}
		else {
			_portletName = portletName;
		}

		_portletCtx = portletCtx;
		_params = params;
		_resourceBundle = resourceBundle;
		_portletInfo = portletInfo;
		_bundlePool = CollectionFactory.getHashMap();
	}

	public String getPortletId() {
		return _portletId;
	}

	public String getPortletName() {
		return _portletName;
	}

	public boolean isWARFile() {
		return _warFile;
	}

	public PortletContext getPortletContext() {
		return _portletCtx;
	}

	public ResourceBundle getResourceBundle(Locale locale) {
		if (_resourceBundle == null) {
			String poolId = _portletId;

			ResourceBundle bundle = (ResourceBundle)_bundlePool.get(poolId);
			if(bundle==null) {
				bundle=new com.liferay.portlet.StrutsResourceBundle(poolId, "dotcms.org", locale);
			}
			if (bundle == null) {
				StringBuffer sb = new StringBuffer();

				try {
					sb.append(WebKeys.JAVAX_PORTLET_TITLE);
					sb.append("=");
					sb.append(_portletInfo.getTitle());
					sb.append("\n");

					sb.append(WebKeys.JAVAX_PORTLET_SHORT_TITLE);
					sb.append("=");
					sb.append(_portletInfo.getShortTitle());
					sb.append("\n");

					sb.append(WebKeys.JAVAX_PORTLET_KEYWORDS);
					sb.append("=");
					sb.append(_portletInfo.getKeywords());
					sb.append("\n");

					bundle = new PropertyResourceBundle(
						new ByteArrayInputStream(sb.toString().getBytes()));
				}
				catch (Exception e) {
					Logger.error(this,e.getMessage(),e);
				}

				_bundlePool.put(poolId, bundle);
			}

			return bundle;
		}
		else {
			String poolId = _portletId + "." + locale.toString();

			ResourceBundle bundle = (ResourceBundle)_bundlePool.get(poolId);

			if (bundle == null) {
				if ((_portletId.indexOf(WAR_SEPARATOR) == -1) &&
					(_resourceBundle.equals(
						"com.liferay.portlet.StrutsResourceBundle"))) {

					String companyId =
						(String)_portletCtx.getAttribute(WebKeys.COMPANY_ID);

					bundle = StrutsResourceBundle.getBundle(
						_portletId, companyId, locale);
				}
				else {
					PortletContextWrapper pcw =
						PortletContextPool.get(_portletId);

					bundle = pcw.getResourceBundle(locale);
				}

				bundle = new PortletResourceBundle(bundle, _portletInfo);

				_bundlePool.put(poolId, bundle);
			}

			return bundle;
		}
	}

	public String getInitParameter(String name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}

		return (String)_params.get(name);
	}

	public Enumeration getInitParameterNames() {
		return Collections.enumeration(_params.keySet());
	}

	private String _portletId;
	private String _portletName;
	private boolean _warFile;
	private PortletContext _portletCtx;
	private Map _params;
	private String _resourceBundle;
	private PortletInfo _portletInfo;
	private Map _bundlePool;

}