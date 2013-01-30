package com.dotmarketing.osgi.job;

import com.dotmarketing.util.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by Jonathan Gamba
 * Date: 1/28/13
 */
public class CustomJob implements Job {

    @Override
    public void execute ( JobExecutionContext context ) throws JobExecutionException {

        Logger.info( this, "------------------------------------------" );
        Logger.info( this, "Start custom job" );
        Logger.info( this, "" );

        Logger.info( this, "param1: " + context.getJobDetail().getJobDataMap().get( "param1" ) );
        Logger.info( this, "param2: " + context.getJobDetail().getJobDataMap().get( "param2" ) );

        Logger.info( this, "" );

        TestClass testClass = new TestClass();
        testClass.printA();
        testClass.printB();

        Logger.info( this, "" );
        Logger.info( this, "Finish custom job (osgi version)." );
        Logger.info( this, "------------------------------------------" );
    }

}