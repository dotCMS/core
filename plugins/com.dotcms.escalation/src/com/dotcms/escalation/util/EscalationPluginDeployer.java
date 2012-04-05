package	com.dotcms.escalation.util;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import org.quartz.CronTrigger;
import org.quartz.SchedulerException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.PluginDeployer;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;

public class EscalationPluginDeployer implements PluginDeployer {

	private String pluginId = "com.dotcms.escalation";
	private PluginAPI pluginAPI = APILocator.getPluginAPI();

	public boolean deploy() {
		try {

			String jobName = pluginAPI.loadProperty(pluginId, "escalation.job.name");
			String jobGroup = pluginAPI.loadProperty(pluginId, "escalation.job.group");
			String jobDescription = pluginAPI.loadProperty(pluginId, "escalation.job.description");
			String javaClassname = pluginAPI.loadProperty(pluginId, "escalation.job.java.classname");
			String cronExpression = pluginAPI.loadProperty(pluginId, "escalation.job.cron.expression");
			
			System.out.println("-------> CRON: "+cronExpression);

			CronScheduledTask cronScheduledTask = new CronScheduledTask(jobName, jobGroup, jobDescription, javaClassname, new Date(), null,
					CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, new HashMap<String, Object>(), cronExpression);
			try {
				QuartzUtils.scheduleTask(cronScheduledTask);
			} catch (SchedulerException e) {
				Logger.error(EscalationPluginDeployer.class, e.getMessage(), e);
			} catch (ParseException e) {
				Logger.error(EscalationPluginDeployer.class, e.getMessage(), e);
			} catch (ClassNotFoundException e) {
				Logger.error(EscalationPluginDeployer.class, e.getMessage(), e);
			}
			return true;

		} catch (DotDataException e) {
			Logger.error(EscalationPluginDeployer.class, e.getMessage(), e);
		}

		return false;
	}

	public boolean redeploy(String version) {

		String jobName;
		try {
			jobName = pluginAPI.loadProperty(pluginId, "escalation.job.name");
			String jobGroup = pluginAPI.loadProperty(pluginId, "escalation.job.group");
			String jobDescription = pluginAPI.loadProperty(pluginId, "escalation.job.description");
			String javaClassname = pluginAPI.loadProperty(pluginId, "escalation.job.java.classname");
			String cronExpression = pluginAPI.loadProperty(pluginId, "escalation.job.cron.expression");

			CronScheduledTask cronScheduledTask = new CronScheduledTask(jobName, jobGroup, jobDescription, javaClassname, new Date(), null,
					CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, new HashMap<String, Object>(), cronExpression);
			try {
				QuartzUtils.scheduleTask(cronScheduledTask);
			} catch (SchedulerException e) {
				Logger.error(EscalationPluginDeployer.class, e.getMessage(), e);
			} catch (ParseException e) {
				Logger.error(EscalationPluginDeployer.class, e.getMessage(), e);
			} catch (ClassNotFoundException e) {
				Logger.error(EscalationPluginDeployer.class, e.getMessage(), e);
			}
			return true;
		} catch (DotDataException e) {
			Logger.error(EscalationPluginDeployer.class, e.getMessage(), e);
		}
		return false;

	}

}
