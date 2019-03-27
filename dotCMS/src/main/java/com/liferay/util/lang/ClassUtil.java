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

package com.liferay.util.lang;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.HashSet;
import java.util.Set;

import com.liferay.util.StringUtil;

/**
 * <a href="ClassUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class ClassUtil {

	public static Set getClasses(File file) throws IOException {
		Set classes = new HashSet();

		StreamTokenizer st = new StreamTokenizer(
			new BufferedReader(new FileReader(file)));
		st.resetSyntax();
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.wordChars('a', 'z');
		st.wordChars('A', 'Z');
		st.wordChars('.', '.');
		st.wordChars('0', '9');
		st.wordChars('_', '_');
		st.lowerCaseMode(false);
		st.eolIsSignificant(false);
		st.quoteChar('"');
		st.quoteChar('\'');
		st.parseNumbers();

		while (st.nextToken() != StreamTokenizer.TT_EOF) {
			if (st.ttype == StreamTokenizer.TT_WORD) {
				if (st.sval.equals("class") || st.sval.equals("interface")) {
					break;
				}
			}
		}

		while (st.nextToken() != StreamTokenizer.TT_EOF) {
			if (st.ttype == StreamTokenizer.TT_WORD) {
				if (Character.isUpperCase(st.sval.charAt(0))) {
					if (st.sval.indexOf('.') >= 0) {
						classes.add(st.sval.substring(0, st.sval.indexOf('.')));
					}
					else {
						classes.add(st.sval);
					}
				}
			}
			else if (st.ttype != StreamTokenizer.TT_NUMBER &&
					 st.ttype != StreamTokenizer.TT_EOL) {

				if (Character.isUpperCase((char)st.ttype)) {
					classes.add(String.valueOf((char)st.ttype));
				}
			}
		}

		classes.remove(StringUtil.replace(file.getName(), ".java", ""));

		return classes;
	}

	public static boolean isSubclass(Class a, Class b) {
		if (a == b) {
			return true;
		}

		if (a == null || b == null) {
			return false;
		}

		for (Class x = a; x != null; x = x.getSuperclass()) {
			if (x == b) {
				return true;
			}

			if (b.isInterface()) {
				Class[] interfaces = x.getInterfaces();

				for (int i = 0; i < interfaces.length; i++) {
					if (isSubclass(interfaces[i], b)) {
						return true;
					}
				}
			}
		}

		return false;
	}

}