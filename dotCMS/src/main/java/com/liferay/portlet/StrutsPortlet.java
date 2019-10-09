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

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.GenericPortlet;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletException;
import com.dotcms.repackage.javax.portlet.PortletRequest;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfig;
import com.liferay.portal.struts.PortletRequestProcessor;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.GetterUtil;
import com.liferay.util.Validator;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <a href="StrutsPortlet.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.27 $
 *
 */
public class StrutsPortlet extends GenericPortlet {

	public void init(PortletConfig config) throws PortletException {
		super.init(config);
    Map<String, String> params = new HashMap<>();
    Enumeration<String> e = this.getInitParameterNames();
    while (e.hasMoreElements()) {
      String key = e.nextElement();
      params.put(key, this.getInitParameter(key));
    }
    this.initParams = ImmutableMap.copyOf(params);
		_portletConfig = (PortletConfigImpl)config;

		_editAction = getInitParameter("edit-action");
		_helpAction = getInitParameter("help-action");
		_viewAction = getInitParameter("view-action");

		_copyRequestParameters = GetterUtil.get(
			getInitParameter("copy-request-parameters"), true);
	}

	public void doEdit(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {
	  req.setAttribute("initParams", initParams);
		if (req.getPreferences() == null) {
			super.doEdit(req, res);
		}
		else {
			req.setAttribute(WebKeys.PORTLET_STRUTS_ACTION, _editAction);

			include(req, res);
		}
	}

	public void doHelp(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		req.setAttribute(WebKeys.PORTLET_STRUTS_ACTION, _helpAction);

		include(req, res);
	}

	public void doView(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {
	  req.setAttribute("initParams", initParams);
		req.setAttribute(WebKeys.PORTLET_STRUTS_ACTION, _viewAction);

		include(req, res);
	}

	public void processAction(ActionRequest req, ActionResponse res)
		throws IOException, PortletException {
	  req.setAttribute("initParams", initParams);
		String path = req.getParameter("struts_action");

		if (Validator.isNotNull(path)) {

			// Call processAction of com.liferay.portal.struts.PortletAction

			try {

				// Process action

				_getPortletRequestProcessor(req).process(req, res, path);
			}
			catch (ServletException se) {
				throw new PortletException(se);
			}
		}

		if (_copyRequestParameters) {
			PortalUtil.copyRequestParameters(req, res);
		}
	}

	protected void include(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {
	  req.setAttribute("initParams", initParams);
		// Call render of com.liferay.portal.struts.PortletAction

		Map strutsAttributes = null;

		if (_portletConfig.isWARFile()) {

			// Remove any Struts request attributes

			strutsAttributes = _removeStrutsAttributes(req);

			//req.setAttribute(
			//	WebKeys.PORTLET_STRUTS_ATTRIBUTES, strutsAttributes);
		}

		// Process render

		try {
			_getPortletRequestProcessor(req).process(req, res);
		}
		catch (IOException ioe) {
			throw ioe;
		}
		catch (ServletException se) {
			_log.error(se.getRootCause());

			throw new PortletException(se);
		}
		finally {
			if (_portletConfig.isWARFile()) {

				// Set the Struts request attributes

				_setStrutsAttributes(req, strutsAttributes);

				//req.removeAttribute(WebKeys.PORTLET_STRUTS_ATTRIBUTES);
			}
		}

		if (_copyRequestParameters) {
			PortalUtil.clearRequestParameters(req);
		}
	}

	private PortletRequestProcessor _getPortletRequestProcessor(
		PortletRequest req) {

		return (PortletRequestProcessor)getPortletContext().getAttribute(
			WebKeys.PORTLET_STRUTS_PROCESSOR);
	}

	private Map _removeStrutsAttributes(PortletRequest req) {
		Map strutsAttributes = new HashMap();

		Enumeration enu = req.getAttributeNames();

		while (enu.hasMoreElements()) {
			String attributeName = (String)enu.nextElement();

			if (attributeName.startsWith(_STRUTS_PACKAGE)) {
				strutsAttributes.put(
					attributeName, req.getAttribute(attributeName));
			}
		}

		Iterator itr = strutsAttributes.keySet().iterator();

		while (itr.hasNext()) {
			String attributeName = (String)itr.next();

			req.removeAttribute(attributeName);
        }

		ModuleConfig moduleConfig =
			(ModuleConfig)getPortletContext().getAttribute(Globals.MODULE_KEY);

		req.setAttribute(Globals.MODULE_KEY, moduleConfig);

		return strutsAttributes;
	}

	private void _setStrutsAttributes(
		PortletRequest req, Map strutsAttributes) {

		Iterator itr = strutsAttributes.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry entry = (Map.Entry)itr.next();

			String key = (String)entry.getKey();
			Object value = entry.getValue();

			req.setAttribute(key, value);
		}
	}

	private static final Log _log = LogFactory.getLog(StrutsPortlet.class);

	private static String _STRUTS_PACKAGE = "com.dotcms.repackage.org.apache.struts.";

	private PortletConfigImpl _portletConfig;
	private String _editAction;
	private String _helpAction;
	private String _viewAction;
	private boolean _copyRequestParameters;
  private Map<String, String> initParams;

}