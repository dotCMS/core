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

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.WebKeys;
import com.liferay.util.cal.CalendarUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <a href="Validator.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Alysa Carver
 * @version $Revision: 1.11 $
 *
 */
public class Validator {

	public static boolean isAddress(String address) {
		if (isNull(address)) {
			return false;
		}

		char[] c = address.toCharArray();

		for (int i = 0; i < c.length; i++) {
			if ((!isChar(c[i])) &&
				(!isDigit(c[i])) &&
				(!Character.isWhitespace(c[i]))) {

				return false;
			}
		}

		return true;
	}

	public static boolean isChar(char c) {
		return Character.isLetter(c);
	}

	public static boolean isChar(String s) {
		if (isNull(s)) {
			return false;
		}

		char[] c = s.toCharArray();

		for (int i = 0; i < c.length; i++) {
			if (!isChar(c[i])) {
				return false;
			}
		}

		return true;
	}

	public static boolean isDigit(char c) {
		int x = (int)c;

		if ((x >= 48) && (x <= 57)) {
			return true;
		}

		return false;
	}

	public static boolean isDigit(String s) {
		if (isNull(s)) {
			return false;
		}

		char[] c = s.toCharArray();

		for (int i = 0; i < c.length; i++) {
			if (!isDigit(c[i])) {
				return false;
			}
		}

		return true;
	}

	public static boolean isHex(String s) {
		if (isNull(s)) {
			return false;
		}

		return true;
	}

	public static boolean isHTML(String s) {
		if (isNull(s)) {
			return false;
		}

		if (((s.indexOf("<html>") != -1) || (s.indexOf("<HTML>") != -1)) &&
			((s.indexOf("</html>") != -1) || (s.indexOf("</HTML>") != -1))) {

			return true;
		}

		return false;
	}

	public static boolean isLUHN(String number) {
		if (number == null) {
			return false;
		}

		number = StringUtil.reverse(number);

		int total = 0;

		for (int i = 0; i < number.length(); i++) {
			int x = 0;

			if (((i + 1) % 2) == 0) {
				x = Integer.parseInt(number.substring(i, i + 1)) * 2;

				if (x >= 10) {
					String s = Integer.toString(x);

					x = Integer.parseInt(s.substring(0, 1)) +
						Integer.parseInt(s.substring(1, 2));
				}
			}
			else {
				x = Integer.parseInt(number.substring(i, i + 1));
			}

			total = total + x;
		}

		if ((total % 10) == 0) {
			return true;
		}
		else {
			return false;
		}
	}

	public static boolean isDate(int month, int day, int year) {
		return CalendarUtil.isDate(month, day, year);
	}

	public static boolean isGregorianDate(int month, int day, int year) {
		return CalendarUtil.isGregorianDate(month, day, year);
	}

	public static boolean isJulianDate(int month, int day, int year) {
		return CalendarUtil.isJulianDate(month, day, year);
	}

	public static boolean isEmailAddress(String ea) {
		String emailRegex = Config.getStringProperty(com.dotmarketing.util.WebKeys.DOTCMS_USE_REGEX_TO_VALIDATE_EMAILS, Constants.REG_EX_EMAIL);

		if (isNull(ea)) {
			return false;
		}


		if(emailRegex !=null && !emailRegex.isEmpty() && !emailRegex.equals("null")){
			return regexEmailValidation(ea, emailRegex);
		}else{
			return defaultEmailValidation(ea);
		}

	}

	private static boolean regexEmailValidation(String ea, String emailRegex) {
		Pattern pattern = Pattern.compile(emailRegex);
		Matcher matcher = pattern.matcher(ea);
		return matcher.matches();
	}

	private static boolean defaultEmailValidation (String ea){
		int eaLength = ea.length();

		if (eaLength < 6) {

			// j@j.c

			return false;
		}

		ea = ea.toLowerCase();

		int at = ea.indexOf('@');

		if ((at > 64) || (at == -1) || (at == 0) ||
			((at <= eaLength) && (at > eaLength - 5))) {

			// 123456789012345678901234@joe.com
			// joe.com
			// @joe.com
			// joe@joe
			// joe@jo
			// joe@j

			return false;
		}

		int dot = ea.lastIndexOf('.');

		if ((dot == -1) || (dot < at) || (dot > eaLength - 3)) {

			// joe@joecom
			// joe.@joecom
			// joe@joe.c

			return false;
		}

		if (ea.indexOf("..") != -1) {

			// joe@joe..com

			return false;
		}

		char[] name = ea.substring(0, at).toCharArray();

		for (int i = 0; i < name.length; i++) {
			if ((!isChar(name[i])) &&
				(!isDigit(name[i])) &&
				(name[i] != '.') &&
				(name[i] != '-') &&
				(name[i] != '+') &&
				(name[i] != '_')) {

				return false;
			}
		}

		if ((name[0] == '.') || (name[name.length - 1] == '.') ||
			(name[0] == '-') || (name[name.length - 1] == '-') ||
			(name[0] == '_')) { // || (name[name.length - 1] == '_')) {

			// .joe.@joe.com
			// -joe-@joe.com
			// _joe_@joe.com

			return false;
		}

		char[] host = ea.substring(at + 1, ea.length()).toCharArray();

		for (int i = 0; i < host.length; i++) {
			if ((!isChar(host[i])) &&
				(!isDigit(host[i])) &&
				(host[i] != '.') &&
				(host[i] != '-')) {

				return false;
			}
		}

		if ((host[0] == '.') || (host[host.length - 1] == '.') ||
			(host[0] == '-') || (host[host.length - 1] == '-')) {

			// joe@.joe.com.
			// joe@-joe.com-

			return false;
		}

		// postmaster@joe.com

		if (ea.startsWith("postmaster@")) {
			return false;
		}

		// root@.com

		if (ea.startsWith("root@")) {
			return false;
		}

		return true;
	}

	/**
	 * @deprecated Use <code>isEmailAddress</code>.
	 */
	public static boolean isValidEmailAddress(String ea) {
		return isEmailAddress(ea);
	}

	public static boolean isName(String name) {
		if (isNull(name)) {
			return false;
		}

		char[] c = name.trim().toCharArray();

		for (int i = 0; i < c.length; i++) {
			if (((!isChar(c[i])) &&
				(!Character.isWhitespace(c[i]))) ||
					(c[i] == ',')) {

				return false;
			}
		}

		return true;
	}

	public static boolean isNumber(String number) {
		if (isNull(number)) {
			return false;
		}

		char[] c = number.toCharArray();

		for (int i = 0; i < c.length; i++) {
			if (!isDigit(c[i])) {
				return false;
			}
		}

		return true;
	}

	public static boolean isNull(String s) {
		if (s == null) {
			return true;
		}

		s = s.trim();

		if ((s.equals(StringPool.NULL)) || (s.equals(StringPool.BLANK))) {
			return true;
		}

		return false;
	}

	public static boolean isNotNull(String s) {
		return !isNull(s);
	}

	public static boolean isPassword(String password) {
		if (isNull(password)) {
			return false;
		}

		if (password.length() < 4) {
			return false;
		}

		char[] c = password.toCharArray();

		for (int i = 0; i < c.length; i++) {
			if ((!isChar(c[i])) &&
				(!isDigit(c[i]))) {

				return false;
			}
		}

		return true;
	}

	public static boolean isPhoneNumber(String phoneNumber) {
		return isNumber(PhoneNumber.strip(phoneNumber));
	}

}
