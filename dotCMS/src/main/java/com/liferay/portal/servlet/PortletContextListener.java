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

package com.liferay.portal.servlet;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import com.dotcms.repackage.javax.portlet.PreferencesValidator;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.job.Scheduler;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.WebAppPool;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.PortletPreferencesSerializer;
import com.liferay.util.CollectionFactory;
import com.liferay.util.FileUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.Http;
import com.liferay.util.KeyValuePair;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;
import com.liferay.util.Validator;


/**
 * <a href="PortletContextListener.java.html"><b><i>View Source </i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.27 $
 *
 */
public class PortletContextListener implements ServletContextListener {

	public void contextInitialized(ServletContextEvent sce) {
		try {

			// Servlet context

			ServletContext ctx = sce.getServletContext();

			_servletContextName = StringUtil.replace(
				ctx.getServletContextName(), StringPool.SPACE,
				StringPool.UNDERLINE);

			// Company ids

			_companyIds = StringUtil.split(ctx.getInitParameter("company_id"));

			// Class loader

			ClassLoader contextClassLoader =
				Thread.currentThread().getContextClassLoader();

			// Initialize portlets


			_portlets = APILocator.getPortletAPI().findAllPortlets();
			      

			// Portlet context wrapper

			Iterator itr1 = _portlets.iterator();

			while (itr1.hasNext()) {
				Portlet portlet = (Portlet)itr1.next();

				com.dotcms.repackage.javax.portlet.Portlet portletInstance =
					(com.dotcms.repackage.javax.portlet.Portlet)contextClassLoader.loadClass(
						portlet.getPortletClass()).newInstance();





				Map customUserAttributes = CollectionFactory.getHashMap();



				PortletContextWrapper pcw = new PortletContextWrapper(
					portlet.getPortletId(), ctx, portletInstance,
					null, null,
					null, customUserAttributes);

				PortletContextPool.put(portlet.getPortletId(), pcw);
			}

			// Portlet class loader

			String servletPath = ctx.getRealPath("/");
			if (!servletPath.endsWith("/") && !servletPath.endsWith("\\")) {
				servletPath += "/";
			}

			File servletClasses = new File(servletPath + "WEB-INF/classes");
			File servletLib = new File(servletPath + "WEB-INF/lib");

			List urls = new ArrayList();

			if (servletClasses.exists()) {
				urls.add(new URL("file:" + servletClasses + "/"));
			}

			if (servletLib.exists()) {
				String[] jars = FileUtil.listFiles(servletLib);

				for (int i = 0; i < jars.length; i++) {
					urls.add(new URL("file:" + servletLib + "/" + jars[i]));
				}
			}

			URLClassLoader portletClassLoader = new URLClassLoader(
				(URL[])urls.toArray(new URL[0]), contextClassLoader);

			ctx.setAttribute(WebKeys.PORTLET_CLASS_LOADER, portletClassLoader);

			// Portlet display


			// Reinitialize portal properties

			PropsUtil.init();
		}
		catch (Exception e2) {
			Logger.error(this,e2.getMessage(),e2);
		}
	}

	public void contextDestroyed(ServletContextEvent sce) {
		Set portletIds = new HashSet();

		if (_portlets != null) {
			Iterator itr = _portlets.iterator();

			while (itr.hasNext()) {
				Portlet portlet = (Portlet)itr.next();

				PortalUtil.destroyPortletInstance(portlet);

				portletIds.add(portlet.getPortletId());
			}

			_portlets = null;
		}

		if (portletIds.size() > 0) {
			for (int i = 0; i < _companyIds.length; i++) {
				String companyId = _companyIds[i];

				Map oldCategories = (Map)WebAppPool.get(
					companyId, WebKeys.PORTLET_DISPLAY);

				Iterator itr1 = oldCategories.entrySet().iterator();

				while (itr1.hasNext()) {
					Map.Entry entry = (Map.Entry)itr1.next();

					String categoryName = (String)entry.getKey();
					List oldKvps = (List)entry.getValue();

					Iterator itr2 = oldKvps.iterator();

					while (itr2.hasNext()) {
						KeyValuePair kvp = (KeyValuePair)itr2.next();

						String portletId = (String)kvp.getKey();

						if (portletIds.contains(portletId)) {
							itr2.remove();
						}
					}
				}
			}
		}
	}

	private static final Log _log =
		LogFactory.getLog(PortletContextListener.class);

	private String _servletContextName;
	private String[] _companyIds;
	private Collection<Portlet> _portlets;

}