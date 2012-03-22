package com.dotmarketing.servlets;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.quartz.CronTrigger;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.Logger;

public class UpdateQuartzCronJobsServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public void init(ServletConfig cfg) throws javax.servlet.ServletException {
		super.init(cfg);
		
		try {
			DotConnect db = new DotConnect();
			
			StringBuilder query = new StringBuilder(512);
			query.ensureCapacity(128);
			query.append("select * ");
			query.append("from qrtz_triggers, ");
			query.append("     qrtz_cron_triggers ");
			query.append("where (qrtz_triggers.job_group='User Job' or ");
			query.append("       qrtz_triggers.job_group='Recurrent Campaign') and ");
			query.append("      qrtz_triggers.job_group<>'PAUSED' and ");
			query.append("      qrtz_triggers.start_time < ? and ");
			query.append("      qrtz_triggers.trigger_name=qrtz_cron_triggers.trigger_name and ");
			query.append("      qrtz_triggers.trigger_group=qrtz_cron_triggers.trigger_group");
			
			db.setSQL(query.toString());
			db.addParam(new Date().getTime());
			List<HashMap<String, String>> result = db.getResults();
			
			CronTrigger cronTrigger = new CronTrigger();
			
			query = new StringBuilder(512);
			query.ensureCapacity(128);
			query.append("update qrtz_triggers ");
			query.append("set start_time=?, ");
			query.append("    next_fire_time=? ");
			query.append("where trigger_name=? and ");
			query.append("      trigger_group=?");
			
			for (HashMap<String, String> trigger: result) {
				cronTrigger.setCronExpression(trigger.get("cron_expression"));
				
				db.setSQL(query.toString());
				long nextFireTime = cronTrigger.getFireTimeAfter(new Date()).getTime();
				db.addParam(nextFireTime);
				db.addParam(nextFireTime);
				db.addParam(trigger.get("trigger_name"));
				db.addParam(trigger.get("trigger_group"));
				
				db.getResult();
			}
		} catch (Exception e) {
			Logger.info(UpdateQuartzCronJobsServlet.class, e.getMessage());
		}
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}
}