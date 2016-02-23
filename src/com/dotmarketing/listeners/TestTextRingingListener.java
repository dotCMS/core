package com.dotmarketing.listeners;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import javax.servlet.http.HttpServletResponse;
import java.text.NumberFormat;
import java.util.List;

/**
 * Created by Jonathan Gamba.
 * Date: 3/8/12
 * <p/>
 * Class that will listen every event for the jUnit tests, with this class we can handle the way the output will be display it
 */
public class TestTextRingingListener extends RunListener {

    private int statusCode = HttpServletResponse.SC_OK;
    private StringBuffer globalBuffer;

    public TestTextRingingListener () {
        this.globalBuffer = new StringBuffer();
    }

    /**
     * Called when all tests have finished
     *
     * @param result the summary of the test run, including all the tests that failed
     */
    @Override
    public void testRunFinished ( Result result ) {

        if ( !result.wasSuccessful() ) {
            statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        printHeader( result.getRunTime() );
        printFailures( result );
        printFooter( result );
    }

    /**
     * Called when an atomic test is about to be started.
     *
     * @param description description the description of the test that is about to be run (generally a class and method name)
     * @throws Exception
     */
    @Override
    public void testStarted ( Description description ) {
        getBuffer().append( '.' );
    }

    /**
     * Called when an atomic test fails.
     *
     * @param failure describes the test that failed and the exception that was thrown
     */
    @Override
    public void testFailure ( Failure failure ) {
        getBuffer().append( 'E' );
    }

    /**
     * Called when a test will not be run, generally because a test method is annotated
     * with {@link org.junit.Ignore}.
     *
     * @param description describes the test that will not be run
     */
    @Override
    public void testIgnored ( Description description ) {
        getBuffer().append( 'I' );
    }

    /*
      * Internal methods
      */

    private StringBuffer getBuffer () {
        return globalBuffer;
    }

    protected void printHeader ( long runTime ) {

        getBuffer().append( "\n" );
        getBuffer().append( "Time: " ).append( elapsedTimeAsString( runTime ) ).append( "\n" );
    }

    protected void printFailures ( Result result ) {

        List<Failure> failures = result.getFailures();
        if ( failures.size() == 0 )
            return;

        if ( failures.size() == 1 )
            getBuffer().append( "There was " ).append( failures.size() ).append( " failure:" ).append( "\n" );
        else
            getBuffer().append( "There were " ).append( failures.size() ).append( " failures:" ).append( "\n" );

        int i = 1;
        for ( Failure each : failures )
            printFailure( each, "" + i++ );
    }

    protected void printFailure ( Failure each, String prefix ) {

        getBuffer().append( prefix ).append( ") " ).append( each.getTestHeader() ).append( "\n" );
        getBuffer().append( each.getTrace() );
    }

    protected void printFooter ( Result result ) {

        if ( result.wasSuccessful() ) {
            getBuffer().append( "\n" );
            getBuffer().append( "OK" );
            getBuffer().append( " (" ).append( result.getRunCount() ).append( " test" ).append( result.getRunCount() == 1? "" : "s" ).append( ")" ).append( "\n" );

        } else {
            getBuffer().append( "\n" );
            getBuffer().append( "FAILURES!!!" ).append( "\n" );
            getBuffer().append( "Tests run: " ).append( result.getRunCount() ).append( ",  Failures: " ).append( result.getFailureCount() ).append( "\n" );
        }

        getBuffer().append( "\n" );
    }

    /**
     * Returns the formatted string of the elapsed time. Duplicated from
     * BaseTestRunner. Fix it.
     */
    protected String elapsedTimeAsString ( long runTime ) {
        return NumberFormat.getInstance().format( ( double ) runTime / 1000 );
    }

    @Override
    public String toString () {
        return globalBuffer.toString();
    }

    public int getStatusCode () {
        return statusCode;
    }

}