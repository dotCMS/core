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
 * <a href="CreditCard.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */
public class CreditCard {

	public static String hide(String number) {
		return hide(number, StringPool.STAR);
	}

	public static String hide(String number, String x) {
		if (number == null) {
			return number;
		}

		int numberLen = number.length();

		if (numberLen > 4) {
			StringBuffer sb = new StringBuffer();

			for (int i = 0; i < numberLen - 4; i++) {
				sb.append(x);
			}

			sb.append(number.substring(numberLen - 4, numberLen));

			number = sb.toString();
		}

		return number;
	}

	public static boolean isValid(String number, String type) {
		number = StringUtil.extractDigits(number);

		if (type.equals("cc_visa")) {
			if (!number.startsWith("4")) {
				return false;
			}

			if (number.length() != 13 &&
				number.length() != 16) {

				return false;
			}
		}
		else if (type.equals("cc_mastercard")) {
			if (!number.startsWith("51") &&
				!number.startsWith("52") &&
				!number.startsWith("53") &&
				!number.startsWith("54") &&
				!number.startsWith("55")) {

				return false;
			}

			if (number.length() != 16) {
				return false;
			}
		}
		else if (type.equals("cc_discover")) {
			if (!number.startsWith("6011")) {

				return false;
			}

			if (number.length() != 16) {
				return false;
			}
		}
		else if (type.equals("cc_amex")) {
			if (!number.startsWith("34") &&
				!number.startsWith("35") &&
				!number.startsWith("36") &&
				!number.startsWith("37")) {

				return false;
			}

			if (number.length() != 15) {
				return false;
			}
		}

		return Validator.isLUHN(number);
	}

}