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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletContext;
import com.dotcms.repackage.javax.portlet.PortletMode;
import com.dotcms.repackage.javax.portlet.PortletPreferences;
import com.dotcms.repackage.javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.model.Portlet;
import com.liferay.portal.util.WebKeys;

/**
 * <a href="ActionRequestImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.10 $
 *
 */
public class ActionRequestImpl
	extends RenderRequestImpl implements ActionRequest {

	public ActionRequestImpl(HttpServletRequest req, Portlet portlet,
							 ConcretePortletWrapper concretePortletWrapper,
							 PortletContext portletCtx, WindowState windowState,
							 PortletMode portletMode,
							 PortletPreferences prefs) {

		this(req, portlet, concretePortletWrapper, portletCtx, windowState, portletMode,
			 prefs, null);
	}

	public ActionRequestImpl(HttpServletRequest req, Portlet portlet,
							 ConcretePortletWrapper concretePortletWrapper,
							 PortletContext portletCtx, WindowState windowState,
							 PortletMode portletMode, PortletPreferences prefs,
							 String layoutId) {

		super(req, portlet, concretePortletWrapper, portletCtx, windowState, portletMode,
			  prefs, layoutId);
	}

	public PortletPreferences getPreferences() {
		return new PortletPreferencesWrapper(getPreferencesImpl(), true);
	}

	public String getCharacterEncoding() {
		return getHttpServletRequest().getCharacterEncoding();
	}

	public void setCharacterEncoding(String enc)
		throws UnsupportedEncodingException {

		if (_calledGetReader) {
			throw new IllegalStateException();
		}

		getHttpServletRequest().setCharacterEncoding(enc);
	}

	public int getContentLength() {
		return getHttpServletRequest().getContentLength();
	}

	public String getContentType() {
		return getHttpServletRequest().getContentType();
	}

	public InputStream getPortletInputStream() throws IOException {
		return getHttpServletRequest().getInputStream();
	}

	public BufferedReader getReader()
		throws IOException, UnsupportedEncodingException {

		_calledGetReader = true;

		return getHttpServletRequest().getReader();
	}

	public void defineObjects(PortletConfig portletConfig, ActionResponse res) {
		setAttribute(WebKeys.JAVAX_PORTLET_CONFIG, portletConfig);
		setAttribute(WebKeys.JAVAX_PORTLET_REQUEST, this);
		setAttribute(WebKeys.JAVAX_PORTLET_RESPONSE, res);
	}

	public boolean isAction() {
		return true;
	}

	private boolean _calledGetReader;

}