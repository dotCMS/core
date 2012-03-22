package com.dotmarketing.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by Jonathan Gamba.
 * Date: 3/8/12
 * Time: 11:39 AM
 * <p/>
 * Class that will listen every event for the jUnit tests, with this class we can handle the way the output will be display it
 */
public class TestRingingListener extends RunListener {

    private static final Log Logger = LogFactory.getLog( TestRingingListener.class );

    private int statusCode = HttpServletResponse.SC_OK;
    private StringBuffer globalBuffer;

    public TestRingingListener () {
        this.globalBuffer = new StringBuffer();
    }

    /**
     * Called when an atomic test is about to be started.
     *
     * @param description description the description of the test that is about to be run (generally a class and method name)
     * @throws Exception
     */
    @Override
    public void testStarted ( Description description ) throws Exception {
        super.testStarted( description );
    }

    /**
     * Called when an atomic test has finished, whether the test succeeds or fails.
     *
     * @param description the description of the test that just ran
     */
    @Override
    public void testFinished ( Description description ) throws Exception {
        super.testFinished( description );
    }

    /**
     * Called when all tests have finished
     *
     * @param result the summary of the test run, including all the tests that failed
     */
    @Override
    public void testRunFinished ( Result result ) throws Exception {
        super.testRunFinished( result );

        if ( !result.wasSuccessful() ) {
            statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        globalBuffer.append( "\nTime: " ).append( result.getRunTime() ).append( "\n" ).append( "There where " ).append( result.getFailureCount() ).append( " failures: " );

        //Check for failures
        int failuresCount = 1;
        if ( result.getFailures() != null ) {

            for ( Failure failure : result.getFailures() ) {
                globalBuffer.append( "\n" ).append( String.valueOf( failuresCount++ ) ).append( ") " )
                        .append( failure.getDescription().toString() ).append( "\n" )
                        .append( failure.getTrace() );
            }
        }

        globalBuffer.append( "\n\n" ).append( result.wasSuccessful()? "SUCCESS!!!" : "FAILURES!!!" )
                .append( "\nTests run: " ).append( result.getRunCount() ).append( ", Failures: " ).append( result.getFailureCount() );
    }

    /**
     * Called when an atomic test fails.
     *
     * @param failure describes the test that failed and the exception that was thrown
     */
    @Override
    public void testFailure ( Failure failure ) throws Exception {
        super.testFailure( failure );
    }

    @Override
    public String toString () {
        return globalBuffer.toString();
    }

    public int getStatusCode () {
        return statusCode;
    }

}