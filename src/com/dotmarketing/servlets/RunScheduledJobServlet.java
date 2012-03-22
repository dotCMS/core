package com.dotmarketing.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class RunScheduledJobServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private String[]          classArr         = { "com.dotmarketing.quartz.job.BuildSearchThread",
            "com.dotmarketing.quartz.job.ContentIndexationThread", "com.dotmarketing.quartz.job.ContentReindexerThread",
            "com.dotmarketing.quartz.job.ContentReviewThread", "com.dotmarketing.quartz.job.DeliverCampaignThread",
            "com.dotmarketing.quartz.job.PopBouncedMailThread", "com.dotmarketing.quartz.job.UpdateRatingThread",
            "com.dotmarketing.quartz.job.UsersToDeleteThread" };

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();

        
        
        // http://jira.dotmarketing.net/browse/DOTCMS-1370
        User user = null; try { user =
        com.liferay.portal.util.PortalUtil.getUser(request); } catch
        (Exception e) { response.sendError(403); return; } try {
			if
			 (!com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
			 response.sendError(403); return; }
		} catch (DotDataException e2) {
			Logger.error(RunScheduledJobServlet.class,e2.getMessage(),e2);
			response.sendError(403); return;
		}


        try {

            String packageName = "com.dotmarketing.quartz.job";

            String runClass = request.getParameter("clazz");

            if (runClass == null) {

                response.setContentType("text/html");
                out.println("Please make me a portlet!\n<ul>");
                for (int i = 0; i < classArr.length; i++) {
                    String s = classArr[i];

                    out.println("<li><a href=\"?clazz=" + s + "\">" + s.replaceAll("Thread", "") + "</a></li>");
                }
                out.println("</ul>");
                return;
            }

            Class clazz = null;

            try {
                clazz = Class.forName(runClass);
            } catch (ClassNotFoundException e1) {
                out.println("exception: " + e1);
                return;
            }
            try {
                Scheduler sched = QuartzUtils.getStandardScheduler();
                JobDetail job = new JobDetail(runClass + System.currentTimeMillis(), null, clazz);
                Trigger trigger = TriggerUtils.makeImmediateTrigger(0, 1);
                trigger.setName(runClass + System.currentTimeMillis());
                sched.scheduleJob(job, trigger);
                out.println("Running: " + clazz);

            } catch (SchedulerException e) {
                out.println("exception: " + e);
            }

        }

        catch (Exception e) {
            e.printStackTrace(out);

        } finally {
            out.close();
        }
    }

}
