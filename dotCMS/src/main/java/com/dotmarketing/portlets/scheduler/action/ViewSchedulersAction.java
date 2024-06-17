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
import javax.servlet.jsp.PageContext;
import org.quartz.Scheduler;
import org.quartz.JobKey;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewSchedulersAction extends PortletAction {

	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
			throws Exception {

		Logger.debug(this, "Running ViewSchedulersAction!!!!");

		try {
			String group = "User Job";
			String group2 = "Recurrent Campaign";
			Map<String, List<String>> results = new HashMap<>();
			List<String> list = new ArrayList<>();

			Scheduler scheduler = QuartzUtils.getScheduler();

			// Retrieve job keys by group
			for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group))) {
				list.add(jobKey.getName());
			}
			results.put(group, list);

			List<String> list2 = new ArrayList<>();
			for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group2))) {
				list2.add(jobKey.getName());
			}
			results.put(group2, list2);

			if (req.getWindowState().equals(WindowState.NORMAL)) {
				req.setAttribute(WebKeys.SCHEDULER_VIEW_PORTLET, results);
				Logger.debug(this, "Going to: portlet.ext.scheduler.view");
				return mapping.findForward("portlet.ext.scheduler.view");
			} else {
				req.setAttribute(WebKeys.SCHEDULER_LIST_VIEW, results);
				Logger.debug(this, "Going to: portlet.ext.scheduler.view_schedulers");
				return mapping.findForward("portlet.ext.scheduler.view_schedulers");
			}
		} catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}
}