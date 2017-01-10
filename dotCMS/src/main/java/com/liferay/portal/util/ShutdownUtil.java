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

package com.liferay.portal.util;

import java.util.Date;

import com.liferay.util.StringPool;

/**
 * <a href="ShutdownUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class ShutdownUtil {

	public static void cancel() {
		_getInstance()._cancel();
	}

	public static long getInProcess() {
		return _getInstance()._getInProcess();
	}

	public static String getMessage() {
		return _getInstance()._getMessage();
	}

	public static boolean isInProcess() {
		return _getInstance()._isInProcess();
	}

	public static boolean isShutdown() {
		return _getInstance()._isShutdown();
	}

	public static void shutdown(long milliseconds) {
		shutdown(milliseconds, StringPool.BLANK);
	}

	public static void shutdown(long milliseconds, String message) {
		_getInstance()._shutdown(milliseconds, message);
	}

	private static ShutdownUtil _getInstance() {
		if (_instance == null) {
			synchronized (ShutdownUtil.class) {
				if (_instance == null) {
					_instance = new ShutdownUtil();
				}
			}
		}

		return _instance;
	}

	private ShutdownUtil() {
	}

	private void _cancel() {
		_date = null;
	}

	private long _getInProcess() {
		long milliseconds = 0;

		if (_date != null) {
			milliseconds = _date.getTime() - System.currentTimeMillis();
		}

		return milliseconds;
	}

	private String _getMessage() {
		return _message;
	}

	private boolean _isInProcess() {
		if (_date == null) {
			return false;
		}
		else {
			if (_date.after(new Date())) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	private boolean _isShutdown() {
		if (_date == null) {
			return false;
		}
		else {
			if (_date.before(new Date())) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	private void _shutdown(long milliseconds, String message) {
		_date = new Date(System.currentTimeMillis() + milliseconds);
		_message = message;
	}

	private static ShutdownUtil _instance;

	private Date _date;
	private String _message;

}