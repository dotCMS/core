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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liferay.portal.struts.Action;
import com.liferay.portal.struts.ActionException;

/**
 * <a href="LogMemoryUsageAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.16 $
 *
 */
public class LogMemoryUsageAction extends Action {

	public void run(HttpServletRequest req, HttpServletResponse res)
		throws ActionException {

		Runtime runtime = Runtime.getRuntime();

		NumberFormat nf = NumberFormat.getInstance();

		String freeMemory = nf.format(runtime.freeMemory());
		String totalMemory = nf.format(runtime.totalMemory());
		String maxMemory = nf.format(runtime.maxMemory());

		_log.debug(
			"Memory Usage:\t" + freeMemory + "\t" + totalMemory + "\t" +
				maxMemory);
	}

	private static final Log _log =
		LogFactory.getLog(LogMemoryUsageAction.class);

}