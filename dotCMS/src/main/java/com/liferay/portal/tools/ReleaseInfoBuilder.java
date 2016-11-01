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

package com.liferay.portal.tools;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;

import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import com.liferay.util.GetterUtil;

/**
 * <a href="ReleaseInfoBuilder.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.11 $
 *
 */
public class ReleaseInfoBuilder {

	public static void main(String[] args) {
		new ReleaseInfoBuilder();
	}

	public ReleaseInfoBuilder() {
		try {

			// Get version

			Properties releaseProps =
				FileUtil.toProperties("../release.properties");

			String version = releaseProps.getProperty("lp.version");

			File file = new File(
				"src/com/liferay/portal/util/ReleaseInfo.java");

			String content = FileUtil.read(file);

			int x = content.indexOf("String version = \"");
			x = content.indexOf("\"", x) + 1;
			int y = content.indexOf("\"", x);

			content =
				content.substring(0, x) + version +
				content.substring(y, content.length());

			// Get build

			x = content.indexOf("String build = \"");
			x = content.indexOf("\"", x) + 1;
			y = content.indexOf("\"", x);

			int build = GetterUtil.get(content.substring(x, y), 0) + 1;

			content =
				content.substring(0, x) + build +
				content.substring(y, content.length());

			// Get date

			DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);

			String date = df.format(new Date());

			x = content.indexOf("String date = \"");
			x = content.indexOf("\"", x) + 1;
			y = content.indexOf("\"", x);

			content =
				content.substring(0, x) + date +
				content.substring(y, content.length());

			// Update ReleaseInfo.java

			FileUtil.write(file, content);

			// Update portal.sql

			/*file = new File("../sql/portal.sql");

			content = FileUtil.read(file);

			x = content.indexOf("insert into Release_");
			y = content.indexOf(");", x);
			x = content.lastIndexOf(" ", y - 1) + 1;

			content =
				content.substring(0, x) + build +
				content.substring(y, content.length());

			FileUtil.write(file,content);*/
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

}