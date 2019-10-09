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

import com.dotcms.repackage.javax.portlet.PortletContext;
import com.dotcms.repackage.javax.portlet.PortletException;
import com.dotcms.repackage.javax.portlet.PortletRequestDispatcher;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.util.Logger;
import com.liferay.portal.struts.StrutsURLEncoder;
import com.liferay.util.ServerDetector;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;
import com.liferay.util.servlet.DynamicServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * <a href="PortletRequestDispatcherImpl.java.html"><b><i>View Source</i></b>
 * </a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.12 $
 *
 */
public class PortletRequestDispatcherImpl implements PortletRequestDispatcher {

	public PortletRequestDispatcherImpl(RequestDispatcher rd,
										PortletContext portletCtx) {

		this(rd, portletCtx, null);
	}

	public PortletRequestDispatcherImpl(RequestDispatcher rd,
										PortletContext portletCtx,
										String path) {

		_rd = rd;
		_portletCtx = portletCtx;
		_path = path;
	}

	public void include(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		include(req, res, false);
	}

	public void include(
			RenderRequest req, RenderResponse res, boolean strutsURLEncoder)
		throws IOException, PortletException {

		try {
			RenderRequestImpl reqImpl = (RenderRequestImpl)req;
			RenderResponseImpl resImpl = (RenderResponseImpl)res;

			HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

			String pathInfo = null;
			String queryString = null;
			String requestURI = null;
			String servletPath = null;

			if (_path != null) {
				/*if (ServerDetector.isJetty()) {
					int pos = _path.indexOf(StringPool.QUESTION);

					if (pos != -1) {
						_path = _path.substring(0, pos);
					}
				}*/

				String pathNoQueryString = _path;

				int pos = _path.indexOf(StringPool.QUESTION);

				if (pos != -1) {
					pathNoQueryString = _path.substring(0, pos);
					queryString = _path.substring(pos + 1, _path.length());

					Map queryParams = new HashMap();

					String[] queryParamsArray =
						StringUtil.split(queryString, StringPool.AMPERSAND);

					for (int i = 0; i < queryParamsArray.length; i++) {
						String[] nameValuePair = StringUtil.split(
							queryParamsArray[i], StringPool.EQUAL);
						String name = nameValuePair[0];
						String value = nameValuePair[1];

						String[] values = (String[])queryParams.get(name);

						if (values == null) {
							queryParams.put(name, new String[] {value});
						}
						else {
							String[] newValues = new String[values.length + 1];

							System.arraycopy(
								values, 0, newValues, 0, values.length);

							newValues[newValues.length - 1] = value;

							queryParams.put(name, newValues);
						}
					}

					DynamicServletRequest dynamicReq =
						new DynamicServletRequest(httpReq);

					Iterator itr = queryParams.entrySet().iterator();

					while (itr.hasNext()) {
						Map.Entry entry = (Map.Entry)itr.next();

						String name = (String)entry.getKey();
						String[] values = (String[])entry.getValue();

						String[] oldValues =
							dynamicReq.getParameterValues(name);

						if (oldValues == null) {
							dynamicReq.setParameterValues(name, values);
						}
						else {
							String[] newValues =
								new String[values.length + oldValues.length];

							System.arraycopy(
								values, 0, newValues, 0, values.length);

							System.arraycopy(
								oldValues, 0, newValues, values.length,
								oldValues.length);

							dynamicReq.setParameterValues(name, newValues);
						}
					}

					httpReq = dynamicReq;
				}

				pos = pathNoQueryString.indexOf(StringPool.SLASH, 1);

				if (pos != -1) {
					pathInfo = pathNoQueryString.substring(
						pos, pathNoQueryString.length());
					servletPath = pathNoQueryString.substring(0, pos);
				}
				else {
					servletPath = pathNoQueryString;
				}

				requestURI = req.getContextPath() + pathNoQueryString;
			}

			PortletServletRequest portletServletReq = new PortletServletRequest(
				httpReq, reqImpl, pathInfo, queryString, requestURI,
				servletPath);

			PortletServletResponse portletServletRes =
				new PortletServletResponse(
					resImpl.getHttpServletResponse(), resImpl);

			if (strutsURLEncoder) {
				resImpl.setURLEncoder(new StrutsURLEncoder(
					portletServletReq.getContextPath(),
					(String)_portletCtx.getAttribute(Globals.SERVLET_KEY),
					(com.liferay.portlet.PortletURLImpl)res.createRenderURL()));
			}

			if (ServerDetector.isJetty()) {
				portletServletReq.setAttribute(
					"org.mortbay.jetty.servlet.Dispatcher.shared_session",
					new Boolean(true));
			}

			_rd.include(portletServletReq, portletServletRes);
		}
		catch (ServletException se) {
			Logger.error(this,se.getMessage(),se);

			throw new PortletException(se);
		}
	}

	private RequestDispatcher _rd;
	private PortletContext _portletCtx;
	private String _path;

}