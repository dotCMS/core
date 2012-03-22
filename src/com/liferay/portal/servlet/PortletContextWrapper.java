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

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.portlet.Portlet;
import javax.portlet.PreferencesValidator;
import javax.servlet.ServletContext;

import com.liferay.portal.job.Scheduler;
import com.liferay.util.lucene.Indexer;

/**
 * <a href="PortletContextWrapper.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class PortletContextWrapper {

	public PortletContextWrapper(String portletName,
								 ServletContext	servletContext,
								 Portlet portletInstance,
								 Indexer indexerInstance,
								 Scheduler schedulerInstance,
								 PreferencesValidator prefsValidator,
								 Map resourceBundles,
								 Map customUserAttributes) {

		_portletName = portletName;
		_servletContext = servletContext;
		_portletInstance = portletInstance;
		_indexerInstance = indexerInstance;
		_schedulerInstance = schedulerInstance;
		_prefsValidator = prefsValidator;
		_resourceBundles = resourceBundles;
		_customUserAttributes = customUserAttributes;
	}

	public String getPortletName() {
		return _portletName;
	}

	public ServletContext getServletContext() {
		return _servletContext;
	}

	public Portlet getPortletInstance() {
		return _portletInstance;
	}

	public void removePortletInstance() {
		_portletInstance = null;
	}

	public Indexer getIndexerInstance() {
		return _indexerInstance;
	}

	public Scheduler getSchedulerInstance() {
		return _schedulerInstance;
	}

	public PreferencesValidator getPreferencesValidator() {
		return _prefsValidator;
	}

	public ResourceBundle getResourceBundle(Locale locale) {
		ResourceBundle resourceBundle = (ResourceBundle)_resourceBundles.get(
			locale.getLanguage());

		if (resourceBundle == null) {
			resourceBundle = (ResourceBundle)_resourceBundles.get(
				Locale.getDefault().getLanguage());
		}

		return resourceBundle;
	}

	public Map getCustomUserAttributes() {
		return _customUserAttributes;
	}

	private String _portletName;
	private ServletContext _servletContext;
	private Portlet _portletInstance;
	private Indexer _indexerInstance;
	private Scheduler _schedulerInstance;
	private PreferencesValidator _prefsValidator;
	private Map _resourceBundles;
	private Map _customUserAttributes;

}