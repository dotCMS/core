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

import com.dotmarketing.util.Logger;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <a href="ReleaseInfo.java.html"><b><i>View Source</i></b></a>
 * 
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.635 $
 * 
 */

public final class ReleaseInfo {

    private Map<String, String> values = Map.of("name", "dotCMS Platform", "version", "UNVERSIONED", "revision", "0", "timestamp", "1699480662");

    protected ReleaseInfo() {
        load();
    }

    private void load() {


        final Properties props = new Properties();

        try {
            final URL url = this.getClass().getClassLoader().getResource("build.properties");
            //we include a validation in case the file does not exist
            if (null != url){
                props.load(url.openStream());
                values = Maps.fromProperties(props);
            } 
        } catch (IOException e) {
            Logger.error(ReleaseInfo.class, "IOException: " + e.getMessage(), e);
        }
    }

    private static final ReleaseInfo instance = new ReleaseInfo();

    public static String getName() {

        return instance.values.get("name");
    }

    public static String getVersion() {

        return instance.values.get("version");
    }

    public static String getBuildNumber() {
        return instance.values.get("revision");
    }

    public static String getBuildDateString() {
        return new SimpleDateFormat("MMMM dd, yyyy h:mm a", Locale.US).format(getBuildDate());
    }

    public static Date getBuildDate() {
        return new Date(Long.parseLong(instance.values.get("timestamp")));
    }


    public static String getReleaseInfo() {
        return getName() + " " + getVersion() + " (" + getBuildDateString() + ")";
    }

    public static String getServerInfo() {
        return getName() + " / " + getVersion();
    }

}
