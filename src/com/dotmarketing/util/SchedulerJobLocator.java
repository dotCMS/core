package com.dotmarketing.util;

import java.util.ArrayList;
import java.util.List;

public class SchedulerJobLocator {
	
	public static List<String> getJobClassess(){
		
		List<String> jobsList = new ArrayList<String>();
		jobsList.add("com.dotmarketing.quartz.job.BinaryCleanupJob");
		jobsList.add("com.dotmarketing.quartz.job.CalendarReminderThread");
		jobsList.add("com.dotmarketing.quartz.job.CascadePermissionsJob");
		jobsList.add("com.dotmarketing.quartz.job.CleanBlockCacheScheduledTask");
		jobsList.add("com.dotmarketing.quartz.job.ContentFromEmailJob");
		jobsList.add("com.dotmarketing.quartz.job.ContentImportThread");
		jobsList.add("com.dotmarketing.quartz.DotJob");
		jobsList.add("com.dotmarketing.quartz.job.PopBouncedMailThread");
		jobsList.add("com.dotmarketing.quartz.job.ResetPermissionsJob");
		jobsList.add("com.dotmarketing.quartz.job.TrashCleanupJob");
		jobsList.add("com.dotmarketing.quartz.job.UsersToDeleteThread");
		jobsList.add("com.dotmarketing.quartz.job.WebDavCleanupJob");
		jobsList.add("com.dotmarketing.portlets.webforms.jobs.WebFormsMailExcelJob");
		jobsList.add("com.dotmarketing.portlets.linkchecker.quartz.LinkCheckerJob");
		
		return jobsList;
	}

}
