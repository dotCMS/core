/**
 * Copyright (c) 2000-2004 Liferay, LLC. All rights reserved.
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

package com.dotmarketing.portlets.scheduler.action;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.jsp.PageContext;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class ViewSchedulersAction extends PortletAction {

	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {

        Logger.debug(this, "Running ViewSchedulersAction!!!!");

		try {
			String group = "User Job";
			String group2 = "Recurrent Campaign";
			Map<String,List<String>> results = new HashMap<String,List<String>>();
			List<String> list = new ArrayList<String>();
			String[] tasks =  QuartzUtils.getStandardScheduler().getJobNames(group);
			for(String task : tasks){
				list.add(task);
			}
			results.put(group, list);
			
			List<String> list2 = new ArrayList<String>();
			String[] tasks2 =  QuartzUtils.getStandardScheduler().getJobNames(group2);
			for(String task : tasks2){
				list2.add(task);
			}
			results.put(group2, list2);
			
			if (req.getWindowState().equals(WindowState.NORMAL)) {
				req.setAttribute(WebKeys.SCHEDULER_VIEW_PORTLET, results);
		        Logger.debug(this, "Going to: portlet.ext.scheduler.view");
				return mapping.findForward("portlet.ext.scheduler.view");
			}
			else {
				req.setAttribute(WebKeys.SCHEDULER_LIST_VIEW, results);
		        Logger.debug(this, "Going to: portlet.ext.scheduler.view_schedulers");
				return mapping.findForward("portlet.ext.scheduler.view_schedulers");
			}
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}
}