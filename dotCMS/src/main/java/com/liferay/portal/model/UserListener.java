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

package com.liferay.portal.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <a href="UserListener.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Jon Steer
 * @version $Revision: 1.4 $
 *
 */
public class UserListener implements ModelListener {

	public void onBeforeCreate(BaseModel model) {
		_log.debug("onBeforeCreate");
	}

	public void onAfterCreate(BaseModel model) {
		_log.debug("onAfterCreate");
	}

	public void onBeforeRemove(BaseModel model) {
		_log.debug("onBeforeRemove");
	}

	public void onAfterRemove(BaseModel model) {
		_log.debug("onAfterRemove");
	}

	public void onBeforeUpdate(BaseModel model) {
		_log.debug("onBeforeUpdate");
	}

	public void onAfterUpdate(BaseModel model) {
		_log.debug("onAfterUpdate");
	}

	private static final Log _log = LogFactory.getLog(UserListener.class);

}