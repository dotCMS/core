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

import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import com.liferay.util.xml.XMLFormatter;

/**
 * <a href="WebXMLStripper.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class WebXMLStripper {

	public static void main(String[] args) {
		if (args.length == 1) {
			new WebXMLStripper(args[0]);
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	public WebXMLStripper(String fileName) {
		try {
			String content = FileUtil.read(fileName);

			int x = content.indexOf("<ejb-local");
			int y = content.lastIndexOf("</ejb-local");

			if ((x != -1) && (y != -1)) {
				y = content.indexOf(">", y) + 1;

				content =
					content.substring(0, x) +
						content.substring(y, content.length());

				content = XMLFormatter.toString(content);

				FileUtil.write(fileName, content);
			}
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

}