package com.dotmarketing.osgi.job;

import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.quartz.CronScheduledTask;
import org.osgi.framework.BundleContext;
import org.quartz.CronTrigger;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Activator extends GenericBundleActivator {

    public final static String JOB_NAME = "Custom Job";
    public final static String JOB_CLASS = "com.dotmarketing.osgi.job.CustomJob";
    public final static String JOB_GROUP = "User Jobs";

    public final static String CRON_EXPRESSION = "0/10 * * * * ?";//Every 10 seconds

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        // Job params
        Map<String, Object> params = new HashMap<String, Object>();
        params.put( "param1", "value1" );
        params.put( "param2", "value2" );

        //Creating our custom Quartz Job
        CronScheduledTask cronScheduledTask =
                new CronScheduledTask( JOB_NAME, JOB_GROUP, JOB_NAME, JOB_CLASS,
                        new Date(), null, CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW,
                        params, CRON_EXPRESSION );

        //Schedule our custom job
        scheduleQuartzJob( cronScheduledTask );
    }

    public void stop ( BundleContext context ) throws Exception {
        //Unregister all the bundle services
        unregisterServices( context );
    }

}