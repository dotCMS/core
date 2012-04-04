package com.dotmarketing.quartz;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.cactus.ServletTestCase;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

public class DotSchedulerFactoryTest extends ServletTestCase {

	public void testSchedulingStandardJobs () throws SchedulerException {
		DotSchedulerFactory factory = DotSchedulerFactory.getInstance();

		Scheduler sched = factory.getScheduler();
		
		sched.deleteJob("test job", "test group");
		sched.deleteJob("test job 2", "test group");
		
		JobDetail jd = new JobDetail("test job", "test group", TestJob.class);
		
		JobDataMap map = new JobDataMap();
		map.put("jobid", "3");
		jd.setJobDataMap(map);
		
		sched.scheduleJob(jd, new SimpleTrigger("t1", "test group", new Date()));
		
		jd = new JobDetail("test job 2", "test group", TestJob.class);
		
		map = new JobDataMap();
		map.put("jobid", "4");
		jd.setJobDataMap(map);
		
		sched.scheduleJob(jd, new SimpleTrigger("t2", "test group", new Date()));		
	}
	
	public void testSchedulingSequentialJobs () throws SchedulerException {

		DotSchedulerFactory factory = DotSchedulerFactory.getInstance();

		Scheduler sched = factory.getSequentialScheduler();
		
		final Map<String, String> job1Status = new HashMap<String, String> ();
		
		JobListener jl1 = new JobListener() {
			public void jobWasExecuted(JobExecutionContext arg0, JobExecutionException arg1) {
				job1Status.put("finished", "yes");		
			}
			public void jobToBeExecuted(JobExecutionContext arg0) {
			}
			public void jobExecutionVetoed(JobExecutionContext arg0) {
			}
			public String getName() {
				return "job1Listener";
			}
		};
		sched.addJobListener(jl1);
		
		JobListener jl2 = new JobListener() {
			public void jobWasExecuted(JobExecutionContext arg0, JobExecutionException arg1) {
			}
			public void jobToBeExecuted(JobExecutionContext arg0) {
				assertTrue(job1Status.get("finished") != null);
			}
			public void jobExecutionVetoed(JobExecutionContext arg0) {
			}
			public String getName() {
				return "job2Listener";
			}
		};
		sched.addJobListener(jl2);
		
		sched.deleteJob("test job", "test group");
		sched.deleteJob("test job 2", "test group");
		
		JobDetail jd = new JobDetail("test job", "test group", TestJob.class);
		jd.addJobListener("job1Listener");
		
		JobDataMap map = new JobDataMap();
		map.put("jobid", "1");
		jd.setJobDataMap(map);
		
		sched.scheduleJob(jd, new SimpleTrigger("t1", "test group", new Date()));
		
		jd = new JobDetail("test job 2", "test group", TestJob.class);
		jd.addJobListener("job2Listener");
		
		map = new JobDataMap();
		map.put("jobid", "2");
		jd.setJobDataMap(map);
		
		sched.scheduleJob(jd, new SimpleTrigger("t2", "test group", new Date()));		
		
	}
	
}
