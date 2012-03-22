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

package com.liferay.portal.util;

import java.io.InputStream;

import org.xml.sax.InputSource;

import com.liferay.util.KeyValuePair;

/**
 * <a href="EntityResolver.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.7 $
 *
 */
public class EntityResolver implements org.xml.sax.EntityResolver {

	public static KeyValuePair[] IDS = {
		new KeyValuePair(
			"-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN",
			"ejb-jar_2_0.dtd"
		),

		new KeyValuePair(
			"-//ObjectWeb//DTD JOnAS 3.2//EN",
			"jonas-ejb-jar_3_2.dtd"
		),

		new KeyValuePair(
			"-//Macromedia, Inc.//DTD jrun-ejb-jar 4.0//EN",
			"jrun-ejb-jar.dtd"
		),

		new KeyValuePair(
			"-//Liferay//DTD DISPLAY 2.0.0//EN",
			"liferay-display_2_0_0.dtd"
		),

		new KeyValuePair(
			"-//Liferay//DTD PORTLET 2.0.0//EN",
			"liferay-portlet_2_0_0.dtd"
		),

		new KeyValuePair(
			"-//Liferay//DTD PORTLET 2.2.0//EN",
			"liferay-portlet_2_2_0.dtd"
		),

		new KeyValuePair(
			"-//Liferay//DTD SKIN 2.0.0//EN",
			"liferay-skin_2_0_0.dtd"
		),

		new KeyValuePair(
			"-//Pramati Technologies //DTD Pramati J2ee Server 3.0//EN",
			"pramati-j2ee-server_3_0.dtd"
		),

		new KeyValuePair(
			"-//Sun Microsystems, Inc.//DTD " +
				"Sun ONE Application Server 7.0 EJB 2.0//EN",
			"sun-ejb-jar_2_0-0.dtd"
		),

		new KeyValuePair(
			"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN",
			"web-app_2_3_dtd"
		),

		new KeyValuePair(
			"-//BEA Systems, Inc.//DTD WebLogic 7.0.0 EJB//EN",
			"weblogic-ejb-jar.dtd"
		)
	};

	public InputSource resolveEntity(String publicId, String systemId) {
		for (int i = 0; i < IDS.length; i++) {
			if (publicId.equals(IDS[i].getKey())) {
				InputStream is =
					getClass().getClassLoader().getResourceAsStream(
						"com/liferay/portal/resources/" + IDS[i].getValue());

				return new InputSource(is);

			}
		}

		return null;
	}

}