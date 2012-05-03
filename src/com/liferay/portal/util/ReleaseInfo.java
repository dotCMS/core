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

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import com.dotmarketing.util.Logger;
import com.liferay.util.GetterUtil;

/**
 * <a href="ReleaseInfo.java.html"><b><i>View Source</i></b></a>
 * 
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.635 $
 * 
 */

public class ReleaseInfo {

	private String name = null;

	private String version = null;

	private String codeName = null;

	private String build = null;

	private String date = null;

	protected ReleaseInfo() {
		load();
	}
	
	private void load() {
		Properties props = new Properties();

		try {
			URL url=this.getClass().getClassLoader().getResource("com/liferay/portal/util/build.properties");
			props.load(url.openStream());
		} catch (IOException e) {
			Logger
					.error(ReleaseInfo.class, "IOException: " + e.getMessage(),
							e);
		}
		if (name == null) {
			name = props.getProperty("dotcms.release.name", "dotCMS Platform");
		}
		if (version == null) {
			version = props
					.getProperty("dotcms.release.version", "UNVERSIONED");
		}
		if (codeName == null) {
			codeName = props.getProperty("dotcms.release.codename",
					"UNVERSIONED");
		}

		if (build == null) {

			//build = props.getProperty("dotcms.release.build", "0");

            //TODO:
            //We are not using build numbers now, also it assumes this number will be always an int, change the method getBuildNumber() to return an String
            //implies a lot of changes and for the dependencies it have we will have go even to the database model...., this will be change it on future releases...
            build = "0";
		}
		if (date == null) {
			date = props.getProperty("dotcms.release.date", "March 6 2009");
		}


	}

	protected static ReleaseInfo instance = new ReleaseInfo();
	
	public static final String getName() {
		
		return  instance.name;
	}

	public static final String getVersion() {
		
		return instance.version;
	}

	public static final String getCodeName() {
		return  instance.codeName;
	}

	public static final int getBuildNumber() {
		return Integer.parseInt( instance.build);
	}

	public static final String getBuildDateString() {
		return  instance.date;
	}
	
	public static final String getBuildDateString(Locale locale) {
		return DateFormat.getDateInstance(DateFormat.LONG,locale).format(getBuildDate());
	}	

	public static final Date getBuildDate() {
		DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
		return GetterUtil.getDate( instance.date, df);
	}


	public static final String getReleaseInfo() {
		//return getName() + " " + getVersion() + " (" + getCodeName() + " / Build " + getBuildNumber() + " / " + getBuildDateString()+ ")";
		return getName() + " " + getVersion() + " (" + getCodeName() + " / " + getBuildDateString()+ ")";
	}

	public static String getServerInfo() {
		return getName() + " / " + getVersion();
	}
 
}