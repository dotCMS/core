package com.dotcms.publisher.util;

import java.util.Date;
import java.util.HashMap;

import org.quartz.CronTrigger;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.plugin.PluginDeployer;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;

/**
 * Create the SOLR_ASSETS_DATABASE AND INITIALICE THE QUARTZ JOB
 * @author Oswaldo
 *
 */
public class SolrPluginDeployer implements PluginDeployer{

	private String pluginId = "com.dotcms.solr";
	private PluginAPI pluginAPI = APILocator.getPluginAPI();

	public boolean deploy() {
		try {
			SolrUtil.createSolrTable();
			String jobName = pluginAPI.loadProperty(pluginId, "quartz.job.name");
			String jobGroup = pluginAPI.loadProperty(pluginId, "quartz.job.group");
			String jobDescription = pluginAPI.loadProperty(pluginId, "quartz.job.description");
			String javaClassname = pluginAPI.loadProperty(pluginId, "quartz.job.java.classname");
			String cronExpression = pluginAPI.loadProperty(pluginId, "quartz.job.cron.expression");

			CronScheduledTask cronScheduledTask = new CronScheduledTask(jobName, jobGroup, jobDescription, javaClassname, new Date(), null, CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, new HashMap<String, Object>(), cronExpression);
			QuartzUtils.scheduleTask(cronScheduledTask);
			return true;
		}catch(Exception e){
			Logger.error(SolrPluginDeployer.class,e.getMessage(),e);			
		}
		return false;
	}

	public boolean redeploy(String version) {
		try {
			boolean dropAndRecreateTable = Boolean.parseBoolean(pluginAPI.loadProperty(pluginId, "com.dotcms.solr.DROP_AND_RECREATE_TABLE"));
			if(dropAndRecreateTable){
				SolrUtil.deleteSolrTable();				
			}
			SolrUtil.createSolrTable();

			String jobName = pluginAPI.loadProperty(pluginId, "quartz.job.name");
			String jobGroup = pluginAPI.loadProperty(pluginId, "quartz.job.group");
			String jobDescription = pluginAPI.loadProperty(pluginId, "quartz.job.description");
			String javaClassname = pluginAPI.loadProperty(pluginId, "quartz.job.java.classname");
			String cronExpression = pluginAPI.loadProperty(pluginId, "quartz.job.cron.expression");

			CronScheduledTask cronScheduledTask = new CronScheduledTask(jobName, jobGroup, jobDescription, javaClassname, new Date(), null, CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, new HashMap<String, Object>(), cronExpression);
			QuartzUtils.scheduleTask(cronScheduledTask);
			return true;
		}catch(Exception e){
			Logger.error(SolrPluginDeployer.class,e.getMessage(),e);			
		}
		return false;
	}

}
