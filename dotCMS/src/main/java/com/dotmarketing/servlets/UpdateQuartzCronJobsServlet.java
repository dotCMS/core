package com.dotmarketing.servlets;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.quartz.CronTrigger;

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
            List<HashMap<String, String>> result = db.loadResults();
            if (result == null || result.isEmpty()) {
                return;
            }

            final String updateSql = "update qrtz_triggers set start_time=?, next_fire_time=? where trigger_name=? and trigger_group=?";
            final CronTrigger cronTrigger = new CronTrigger();

            try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
                conn.setAutoCommit(false);
                for (HashMap<String, String> trigger : result) {
                    try {
                        cronTrigger.setCronExpression(trigger.get("cron_expression"));
                        final Date nextFireDate = cronTrigger.getFireTimeAfter(new Date());
                        if (nextFireDate == null) {
                            Logger.warn(UpdateQuartzCronJobsServlet.class,
                                    "No future fire time for trigger " + trigger.get("trigger_name") + ", skipping");
                            continue;
                        }
                        final long nextFireTime = nextFireDate.getTime();
                        db.setSQL(updateSql);
                        db.addParam(nextFireTime);
                        db.addParam(nextFireTime);
                        db.addParam(trigger.get("trigger_name"));
                        db.addParam(trigger.get("trigger_group"));
                        db.loadResult(conn);
                    } catch (Exception e) {
                        Logger.error(UpdateQuartzCronJobsServlet.class,
                                "Failed to update trigger " + trigger.get("trigger_name") + ": " + e.getMessage(), e);
                    }
                }
                conn.commit();
            }
        } catch (Exception e) {
            Logger.error(UpdateQuartzCronJobsServlet.class, e.getMessage(), e);
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
