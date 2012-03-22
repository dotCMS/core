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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.portlet.PortletPreferences;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.PortletPreferencesPK;
import com.liferay.util.GetterUtil;
import com.liferay.util.Validator;

/**
 * <a href="PortletPreferencesSerializer.java.html"><b><i>View Source</i></b>
 * </a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Jon Steer
 * @author  Zongliang Li
 * @version $Revision: 1.18 $
 *
 */
public class PortletPreferencesSerializer {

	public static PortletPreferences fromDefaultXML(String xml)
		throws PortalException, SystemException {

		PortletPreferencesImpl prefs = new PortletPreferencesImpl();

		if (Validator.isNull(xml)) {
			return prefs;
		}

		Map preferences = prefs.getPreferences();

		try {
			Document doc = new SAXReader().read(new StringReader(xml));

			Element root = doc.getRootElement();

			Iterator itr1 = root.elements("preference").iterator();

			while (itr1.hasNext()) {
				Element prefEl = (Element)itr1.next();

				String name = prefEl.elementTextTrim("name");

				List values = new ArrayList();

				Iterator itr2 = prefEl.elements("value").iterator();

				while (itr2.hasNext()) {
					Element valueEl = (Element)itr2.next();

					/*if (valueEl.nodeCount() <= 0) {
						values.add(valueEl.getText());
					}
					else {
						values.add(valueEl.node(0).asXML());
					}*/

					values.add(valueEl.getTextTrim());
				}

				boolean readOnly =
					GetterUtil.get(prefEl.elementText("read-only"), false);

				Preference preference = new Preference(
					name, (String[])values.toArray(new String[0]), readOnly);

				preferences.put(name, preference);
			}

			return prefs;
		}
		catch (DocumentException de) {
			throw new SystemException(de);
		}
	}

	public static PortletPreferencesImpl fromXML(
			String companyId, PortletPreferencesPK pk, String xml)
		throws PortalException, SystemException {

		try {
			PortletPreferencesImpl prefs =
				(PortletPreferencesImpl)fromDefaultXML(xml);

			prefs = new PortletPreferencesImpl(
				companyId, pk, prefs.getPreferences());

			return prefs;
		}
		catch (PortalException pe) {
			throw pe;
		}
		catch (SystemException se) {
			throw se;
		}
	}

	public static String toXML(PortletPreferencesImpl prefs)
		throws SystemException {

		try {
			Map preferences = prefs.getPreferences();

			DocumentFactory docFactory = DocumentFactory.getInstance();

			Element portletPreferences =
				docFactory.createElement("portlet-preferences");

			Iterator itr = preferences.entrySet().iterator();

			while (itr.hasNext()) {
				Map.Entry entry = (Map.Entry)itr.next();

				Preference preference = (Preference)entry.getValue();

				Element prefEl = docFactory.createElement("preference");

				Element nameEl = docFactory.createElement("name");
				nameEl.addText(preference.getName());

				prefEl.add(nameEl);

				String[] values = preference.getValues();

				for (int i = 0; i < values.length; i++) {
					Element valueEl = docFactory.createElement("value");
					valueEl.addText(values[i]);

					prefEl.add(valueEl);
				}

				if (preference.isReadOnly()) {
					Element valueEl = docFactory.createElement("read-only");
					valueEl.addText("true");
				}

				portletPreferences.add(prefEl);
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			XMLWriter writer = new XMLWriter(
				baos, OutputFormat.createCompactFormat());

			writer.write(portletPreferences);

			return baos.toString();
		}
		catch (IOException ioe) {
			throw new SystemException(ioe);
		}
	}

}