/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.liferay.portal.util;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;

import com.dotmarketing.util.Logger;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.*;

import com.liferay.util.GetterUtil;

/**
 * <a href="ReleaseInfo.java.html"><b><i>View Source</i></b></a>
 * 
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.635 $
 * 
 */

public final class ReleaseInfo {



    private volatile Map<String, String> values = ImmutableMap.of("name", "dotCMS Platform", "version", "UNVERSIONED", "codename",
            "UNVERSIONED", "build", "0", "date", "March 6 2009");

    protected ReleaseInfo() {
        load();
    }

    private void load() {


        final Properties props = new Properties();

        try {
            URL url = this.getClass().getClassLoader().getResource("release.properties");
            props.load(url.openStream());
        } catch (IOException e) {
            Logger.error(ReleaseInfo.class, "IOException: " + e.getMessage(), e);
        }

        final Map<String, String> tempValuesMap = new HashMap<>(values);

        for (String key : values.keySet()) {
            String value = props.getProperty("dotcms.release." + key);
            if (value != null && !value.startsWith("${")) {
                tempValuesMap.put(key, value);
            }
        }

        values = ImmutableMap.copyOf(tempValuesMap);
    }

    private final static ReleaseInfo instance = new ReleaseInfo();

    public static final String getName() {

        return instance.values.get("name");
    }

    public static final String getVersion() {

        return instance.values.get("version");
    }

    public static final String getCodeName() {
        return instance.values.get("codename");
    }

    public static final int getBuildNumber() {
        return Integer.parseInt(instance.values.get("build"));
    }

    public static final String getBuildDateString() {
        return instance.values.get("date");
    }

    public static final String getBuildDateString(Locale locale) {
        return DateFormat.getDateInstance(DateFormat.LONG, locale).format(getBuildDate());
    }

    public static final Date getBuildDate() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
        return GetterUtil.getDate(instance.values.get("date"), df);
    }


    public static final String getReleaseInfo() {
        // return getName() + " " + getVersion() + " (" + getCodeName() + " / Build " +
        // getBuildNumber() + " / " + getBuildDateString()+ ")";
        return getName() + " " + getVersion() + " (" + getCodeName() + " / " + getBuildDateString() + ")";
    }

    public static String getServerInfo() {
        return getName() + " / " + getVersion();
    }

}
