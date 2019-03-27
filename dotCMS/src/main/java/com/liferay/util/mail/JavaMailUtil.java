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

package com.liferay.util.mail;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import com.liferay.util.lang.FastStringBuffer;

/**
 * <a href="JavaMailUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class JavaMailUtil {

	public static String toUnicodeString(Address[] addresses) {
		return toUnicodeString((InternetAddress[])addresses);
	}

	public static String toUnicodeString(InternetAddress[] addresses) {
		FastStringBuffer sb = new FastStringBuffer();

		if (addresses != null) {
			for (int i = 0; i < addresses.length; i++) {
				if (addresses[i] != null) {
					sb.append(addresses[i].toUnicodeString());
				}

				if ((i + 1) != addresses.length) {
					sb.append(", ");
				}
			}
		}

		return sb.toString();
	}

}