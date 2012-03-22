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

package com.liferay.portal.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.dotcms.enterprise.ClusterThreadProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.util.Logger;
import com.liferay.portal.events.EventsProcessor;
import com.liferay.portal.events.InitAction;

/**
 * <a href="InitServlet.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class InitServlet extends HttpServlet {

	public void init() throws ServletException {
		synchronized (InitServlet.class) {
			
			// Initialize

			super.init();
			try{
				ClusterThreadProxy.loadLevel();
			}
			catch(ArrayIndexOutOfBoundsException aiobe){
				Logger.info(this.getClass(), "No valid license found");
			}

			
			//initial system cache
			//http://jira.dotmarketing.net/browse/DOTCMS-1873
			CacheLocator.init();
			FactoryLocator.init();
			APILocator.init();
			
			try {

				// Make sure the initialization process is only run once

				EventsProcessor.process(
					new String[] {
						InitAction.class.getName()
					},
					true);
			}
			catch (Exception e) {
				Logger.error(this,e.getMessage(),e);
			}
		}
	}

}