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

import com.dotcms.repackage.javax.portlet.GenericPortlet;
import com.dotcms.repackage.javax.portlet.PortletException;
import com.dotcms.repackage.javax.portlet.PortletMode;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowState;

/**
 * <a href="LiferayPortlet.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.1 $
 *
 */
public class LiferayPortlet extends GenericPortlet {

	protected void doDispatch(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		WindowState state = req.getWindowState();

		if (!state.equals(WindowState.MINIMIZED)) {
			PortletMode mode = req.getPortletMode();

			if (mode.equals(PortletMode.VIEW)) {
				doView(req, res);
			}
			else if (mode.equals(LiferayPortletMode.ABOUT)) {
				doAbout(req, res);
			}
			else if (mode.equals(LiferayPortletMode.CONFIG)) {
				doConfig(req, res);
			}
			else if (mode.equals(PortletMode.EDIT)) {
				doEdit(req, res);
			}
			else if (mode.equals(LiferayPortletMode.EDIT_DEFAULTS)) {
				doEditDefaults(req, res);
			}
			else if (mode.equals(PortletMode.HELP)) {
				doHelp(req, res);
			}
			else if (mode.equals(LiferayPortletMode.PREVIEW)) {
				doPreview(req, res);
			}
			else if (mode.equals(LiferayPortletMode.PRINT)) {
				doPrint(req, res);
			}
			else {
				throw new PortletException(mode.toString());
			}
		}
	}

	protected void doAbout(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		throw new PortletException("doAbout method not implemented");
	}

	protected void doConfig(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		throw new PortletException("doConfig method not implemented");
	}

	protected void doEditDefaults(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		throw new PortletException("doEditDefaults method not implemented");
	}

	protected void doPreview(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		throw new PortletException("doPreview method not implemented");
	}

	protected void doPrint(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		throw new PortletException("doPrint method not implemented");
	}

}