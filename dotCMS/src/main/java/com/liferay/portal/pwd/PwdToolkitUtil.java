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

package com.liferay.portal.pwd;

import com.liferay.portal.util.PropsUtil;
import com.liferay.portlet.words.util.WordsUtil;
import com.liferay.util.InstancePool;

/**
 * <a href="PwdToolkitUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */
public class PwdToolkitUtil {

	public static String generate() {
		return _getInstance()._generate();
	}

	public static boolean validate(String password) {
		return _getInstance()._validate(password);
	}

	private static PwdToolkitUtil _getInstance() {
		if (_instance == null) {
			synchronized (PwdToolkitUtil.class) {
				if (_instance == null) {
					_instance = new PwdToolkitUtil();
				}
			}
		}

		return _instance;
	}

	private PwdToolkitUtil() {
		_toolkit = (BasicToolkit)InstancePool.get(
			PropsUtil.get(PropsUtil.PASSWORDS_TOOLKIT));
	}

	private String _generate() {
		return _toolkit.generate();
	}

	private boolean _validate(String password) {
		boolean validPassword = _toolkit.validate(password);

		if (validPassword && !_toolkit.allowDictionaryWord()) {
			validPassword = !WordsUtil.isDictionaryWord(password);
		}

		return validPassword;
	}

	private static PwdToolkitUtil _instance;

	private BasicToolkit _toolkit;

}