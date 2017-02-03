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

package com.liferay.util;

/**
 * <a href="ColorUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.7 $
 *
 */
public class ColorUtil {

	public static String getHex(int[] rgb) {
		StringBuffer sb = new StringBuffer();

		sb.append("#");

		sb.append(
			_KEY.substring(
				(int)Math.floor(rgb[0] / 16),
				(int)Math.floor(rgb[0] / 16) + 1));

		sb.append(_KEY.substring(rgb[0] % 16, (rgb[0] % 16) + 1));

		sb.append(
			_KEY.substring(
				(int)Math.floor(rgb[1] / 16),
				(int)Math.floor(rgb[1] / 16) + 1));

		sb.append(_KEY.substring(rgb[1] % 16, (rgb[1] % 16) + 1));

		sb.append(
			_KEY.substring(
				(int)Math.floor(rgb[2] / 16),
				(int)Math.floor(rgb[2] / 16) + 1));

		sb.append(_KEY.substring(rgb[2] % 16, (rgb[2] % 16) + 1));

		return sb.toString();
	}

	public static int[] getRGB(String hex) {
		if (hex.startsWith("#")) {
			hex = hex.substring(1, hex.length()).toUpperCase();
		}
		else {
			hex = hex.toUpperCase();
		}

		int[] hexArray = new int[6];

		if (hex.length() == 6) {
			char[] c = hex.toCharArray();

			for (int i = 0; i < hex.length(); i++) {
				if (c[i] == 'A') {
					hexArray[i] = 10;
				}
				else if (c[i] == 'B') {
					hexArray[i] = 11;
				}
				else if (c[i] == 'C') {
					hexArray[i] = 12;
				}
				else if (c[i] == 'D') {
					hexArray[i] = 13;
				}
				else if (c[i] == 'E') {
					hexArray[i] = 14;
				}
				else if (c[i] == 'F') {
					hexArray[i] = 15;
				}
				else {
					hexArray[i] =
						GetterUtil.getInteger(new Character(c[i]).toString());
				}
			}
		}

		int[] rgb = new int[3];
		rgb[0] = (hexArray[0] * 16) + hexArray[1];
		rgb[1] = (hexArray[2] * 16) + hexArray[3];
		rgb[2] = (hexArray[4] * 16) + hexArray[5];

		return rgb;
	}

	private static final String _KEY = "0123456789ABCDEF";

}