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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Locale;

import com.dotcms.repackage.javax.portlet.PortletModeException;
import com.dotcms.repackage.javax.portlet.PortletURL;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowStateException;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.business.Layout;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.servlet.URLEncoder;

/**
 * <a href="RenderResponseImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.30 $
 *
 */
public class RenderResponseImpl implements RenderResponse {

	public RenderResponseImpl(RenderRequestImpl req, HttpServletResponse res,
							  String portletName, String companyId) {

		this(req, res, portletName, companyId, null);
	}

	public RenderResponseImpl(RenderRequestImpl req, HttpServletResponse res,
							  String portletName, String companyId,
							  String layoutId) {

		_req = req;
		_res = res;
		_portletName = portletName;
		_companyId = companyId;
		setLayoutId(layoutId);
	}

	public void addProperty(String key, String value) {
	}

	public void setProperty(String key, String value) {
	}

	public PortletURL createActionURL() {
		return createActionURL(_portletName);
	}

	public PortletURL createActionURL(String portletName) {
		PortletURL portletURL = createPortletURL(portletName, true);

		try {
			portletURL.setWindowState(_req.getWindowState());
		}
		catch (WindowStateException wse) {
		}

		try {
			portletURL.setPortletMode(_req.getPortletMode());
		}
		catch (PortletModeException pme) {
		}

		return portletURL;
	}

	public PortletURL createRenderURL() {
		return createRenderURL(_portletName);
	}

	public PortletURL createRenderURL(String portletName) {
		PortletURL portletURL = createPortletURL(portletName, false);

		try {
			portletURL.setWindowState(_req.getWindowState());
		}
		catch (WindowStateException wse) {
		}

		try {
			portletURL.setPortletMode(_req.getPortletMode());
		}
		catch (PortletModeException pme) {
		}

		return portletURL;
	}

	public String getNamespace() {
		return PortalUtil.getPortletNamespace(_portletName);
	}

	public void setURLEncoder(URLEncoder urlEncoder) {
		_urlEncoder = urlEncoder;
	}

	public String encodeURL(String path) {
		if ((path == null) ||
			(!path.startsWith("/") && (path.indexOf("://") == -1))) {

			throw new IllegalArgumentException();
		}

		if (_urlEncoder != null) {
			return _urlEncoder.encodeURL(path);
		}
		else {
			return path;
		}
	}

	public String getCharacterEncoding() {
		return _res.getCharacterEncoding();
	}

	public String getContentType() {
		return _contentType;
	}

	public void setContentType(String contentType) {
		Enumeration enu = _req.getResponseContentTypes();

		boolean valid = false;

		while (enu.hasMoreElements()) {
			String resContentType = (String)enu.nextElement();

			if (resContentType.equals(contentType)) {
				valid = true;
			}
		}

		if (!valid) {
			throw new IllegalArgumentException();
		}

		_contentType = contentType;
	}

	public Locale getLocale() {
		return _req.getLocale();
	}

	public OutputStream getPortletOutputStream() throws IOException{
		if (_calledGetWriter) {
			throw new IllegalStateException();
		}

		if (_contentType == null) {
			throw new IllegalStateException();
		}

		_calledGetPortletOutputStream = true;

		return _res.getOutputStream();
	}

	public String getTitle() {
		return _title;
	}

	public void setTitle(String title) {
		_title = title;
	}

	public PrintWriter getWriter() throws IOException {
		if (_calledGetPortletOutputStream) {
			throw new IllegalStateException();
		}

		if (_contentType == null) {
			throw new IllegalStateException();
		}

		_calledGetWriter = true;

		return _res.getWriter();
	}

	public int getBufferSize() {
		return 0;
	}

	public void setBufferSize(int size) {
	}

	public void flushBuffer() throws IOException {
	}

	public void resetBuffer() {
	}

	public boolean isCommitted() {
		return false;
	}

	public void reset() {
	}

	public HttpServletResponse getHttpServletResponse() {
		return _res;
	}

	protected PortletURL createPortletURL(boolean action) {
		return createPortletURL(_portletName, action);
	}

	protected PortletURL createPortletURL(String portletName, boolean action) {
		return new PortletURLImpl(_req, portletName, _layoutId, action);
	}

	protected String getCompanyId() {
		return _companyId;
	}

	protected String getLayoutId() {
		return _layoutId;
	}

	protected void setLayoutId(String layoutId) {
		_layoutId = layoutId;

		if (_layoutId == null) {
			Layout layout = (Layout)_req.getAttribute(WebKeys.LAYOUT);

			if (layout != null) {
				_layoutId = layout.getId();
			}
		}
	}

	protected String getPortletName() {
		return _portletName;
	}

	protected RenderRequestImpl getReq() {
		return _req;
	}

	protected URLEncoder getUrlEncoder() {
		return _urlEncoder;
	}

	protected boolean isCalledGetPortletOutputStream() {
		return _calledGetPortletOutputStream;
	}
	protected boolean isCalledGetWriter() {
		return _calledGetWriter;
	}

	private RenderRequestImpl _req;
	private HttpServletResponse _res;
	private String _portletName;
	private String _companyId;
	private String _layoutId;
	private URLEncoder _urlEncoder;
	private String _title;
	private String _contentType;
	private boolean _calledGetPortletOutputStream;
 	private boolean _calledGetWriter;

}