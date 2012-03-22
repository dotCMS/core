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

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.Validator;

/**
 * <a href="JSPPortlet.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.20 $
 *
 */
public class JSPPortlet extends GenericPortlet {

	boolean useWEBINFDIR = false;
	public void init() throws PortletException {
		_editJSP = getInitParameter("edit-jsp");
		_helpJSP = getInitParameter("help-jsp");
		_viewJSP = getInitParameter("view-jsp");
		useWEBINFDIR = new Boolean(getInitParameter("useWEBINFDIR"));

		_copyRequestParameters = GetterUtil.get(
			getInitParameter("copy-request-parameters"), true);
	}

	public void doDispatch(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		String jspPage = req.getParameter("jsp_page");

		if (Validator.isNotNull(jspPage)) {
			include(jspPage, req, res);
		}
		else {
			super.doDispatch(req, res);
		}
	}

	public void doEdit(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		if (req.getPreferences() == null) {
			super.doEdit(req, res);
		}
		else {
			include(_editJSP, req, res);
		}
	}

	public void doHelp(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		include(_helpJSP, req, res);
	}

	public void doView(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		include(_viewJSP, req, res);
	}

	public void processAction(ActionRequest req, ActionResponse res)
		throws IOException, PortletException {

		if (_copyRequestParameters) {
			PortalUtil.copyRequestParameters(req, res);
		}
	}

	protected void include(String path, RenderRequest req, RenderResponse res)
		throws IOException, PortletException {
		
		PortletRequestDispatcher prd = null;
		if(useWEBINFDIR){
			prd =
				getPortletContext().getRequestDispatcher(
					"/WEB-INF" + path);
		}else{
		prd =
			getPortletContext().getRequestDispatcher(
				Constants.TEXT_HTML_DIR + path);
		}
		if (prd == null) {
			_log.error(path + " is not a valid include");
		}

		prd.include(req, res);

		if (_copyRequestParameters) {
			PortalUtil.clearRequestParameters(req);
		}
	}

	private static final Log _log = LogFactory.getLog(JSPPortlet.class);

	private String _editJSP;
	private String _helpJSP;
	private String _viewJSP;
	private boolean _copyRequestParameters;

}