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

package com.liferay.portal.events;

import java.text.NumberFormat;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liferay.portal.struts.ActionException;
import com.liferay.portal.struts.SessionAction;

/**
 * <a href="GarbageCollectorAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.1 $
 *
 */
public class GarbageCollectorAction extends SessionAction {

	public void run(HttpSession ses) throws ActionException {

		try {
			Runtime runtime = Runtime.getRuntime();

			NumberFormat nf = NumberFormat.getInstance();

			_log.debug(
				"Before GC: " +
					nf.format(runtime.freeMemory()) + " free, " +
						nf.format(runtime.totalMemory()) + " total, and " +
							nf.format(runtime.maxMemory()) + " max");

			_log.debug("Running garbage collector");

			System.gc();

			_log.debug(
				"After GC: " +
					nf.format(runtime.freeMemory()) + " free, " +
						nf.format(runtime.totalMemory()) + " total, and " +
							nf.format(runtime.maxMemory()) + " max");
		}
		catch (Exception e) {
			throw new ActionException(e);
		}
	}

	private static final Log _log =
		LogFactory.getLog(GarbageCollectorAction.class);

}