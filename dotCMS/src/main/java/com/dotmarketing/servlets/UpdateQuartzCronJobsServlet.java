package com.dotmarketing.servlets;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;

import org.quartz.TriggerBuilder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class UpdateQuartzCronJobsServlet extends HttpServlet {

	@Override
	public void init(ServletConfig cfg) throws ServletException {
		super.init(cfg);

		try {
			DotConnect db = new DotConnect();

			StringBuilder query = new StringBuilder(512);
			query.append("select * ");
			query.append("from qrtz_triggers, ");
			query.append("     qrtz_cron_triggers ");
			query.append("where (qrtz_triggers.job_group='User Job' or ");
			query.append("       qrtz_triggers.job_group='Recurrent Campaign') and ");
			query.append("      qrtz_triggers.trigger_state<>'PAUSED' and ");
			query.append("      qrtz_triggers.start_time < ? and ");
			query.append("      qrtz_triggers.trigger_name=qrtz_cron_triggers.trigger_name and ");
			query.append("      qrtz_triggers.trigger_group=qrtz_cron_triggers.trigger_group");

			db.setSQL(query.toString());
			db.addParam(new Date().getTime());
			List<HashMap<String, String>> result = db.loadResults();

			if (result != null && !result.isEmpty()) {
				query = new StringBuilder(512);
				query.append("update qrtz_triggers ");
				query.append("set start_time=?, ");
				query.append("    next_fire_time=? ");
				query.append("where trigger_name=? and ");
				query.append("      trigger_group=?");

				for (HashMap<String, String> trigger : result) {
					CronTrigger cronTrigger = TriggerBuilder.newTrigger()
							.withIdentity(trigger.get("trigger_name"), trigger.get("trigger_group"))
							.withSchedule(CronScheduleBuilder.cronSchedule(trigger.get("cron_expression")))
							.build();

					long nextFireTime = cronTrigger.getFireTimeAfter(new Date()).getTime();

					db.setSQL(query.toString());
					db.addParam(nextFireTime);
					db.addParam(nextFireTime);
					db.addParam(trigger.get("trigger_name"));
					db.addParam(trigger.get("trigger_group"));

					db.getResult();
				}
			}
		} catch (Exception e) {
			Logger.error(UpdateQuartzCronJobsServlet.class, e.getMessage(), e);
		} finally {
			try {
				DbConnectionFactory.getConnection().close();
			} catch (SQLException e) {
				Logger.error(UpdateQuartzCronJobsServlet.class, e.getMessage(), e);
			}
		}
	}
}