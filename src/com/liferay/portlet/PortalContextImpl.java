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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.dotcms.repackage.javax.portlet.PortalContext;
import com.dotcms.repackage.javax.portlet.PortletMode;
import com.dotcms.repackage.javax.portlet.WindowState;

import com.liferay.portal.util.ReleaseInfo;

/**
 * <a href="PortalContextImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.10 $
 *
 */
public class PortalContextImpl implements PortalContext {

	static Properties props = new Properties();
	static List portletModes = new ArrayList();
	static List windowStates = new ArrayList();

	static {
		portletModes.add(PortletMode.EDIT);
		portletModes.add(PortletMode.HELP);
		portletModes.add(PortletMode.VIEW);

		windowStates.add(WindowState.MAXIMIZED);
		windowStates.add(WindowState.MINIMIZED);
		windowStates.add(WindowState.NORMAL);
		windowStates.add(LiferayWindowState.EXCLUSIVE);
		windowStates.add(LiferayWindowState.POP_UP);
	}

	public static boolean isSupportedPortletMode(PortletMode portletMode) {
		Enumeration enu = Collections.enumeration(portletModes);

		while (enu.hasMoreElements()) {
			PortletMode supported = (PortletMode)enu.nextElement();

			if (supported.equals(portletMode)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isSupportedWindowState(WindowState windowState) {
		Enumeration enu = Collections.enumeration(windowStates);

		while (enu.hasMoreElements()) {
			WindowState supported = (WindowState)enu.nextElement();

			if (supported.equals(windowState)) {
				return true;
			}
		}

		return false;
	}

	public String getPortalInfo() {
		return ReleaseInfo.getReleaseInfo();
	}

	public String getProperty(String name) {
		return props.getProperty(name);
	}

	public Enumeration getPropertyNames() {
		return props.propertyNames();
	}

	public Enumeration getSupportedPortletModes() {
		return Collections.enumeration(portletModes);
	}

	public Enumeration getSupportedWindowStates() {
		return Collections.enumeration(windowStates);
	}

}