package com.dotmarketing.listeners;

import com.dotcms.repackage.org.dom4j.Document;
import com.dotcms.repackage.org.dom4j.DocumentHelper;
import com.dotcms.repackage.org.dom4j.Element;
import com.dotcms.repackage.org.dom4j.io.OutputFormat;
import com.dotcms.repackage.org.dom4j.io.XMLWriter;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Created by Jonathan Gamba.
 * Date: 5/14/12
 * Class that will listen every event for the jUnit tests, with this class we can handle the way the output will be display it
 */
public class TestXmlRingingListener extends RunListener {

    private int statusCode = HttpServletResponse.SC_OK;

    private final File reportDirectory;
    private String fileName;
    private Document document;
    private Element root;
    private String name;
    int nError = 0;
    int nFailure = 0;
    private Element currentFailureNode;
    DecimalFormat TIME_FORMAT = new DecimalFormat( "######0.000" );
    private long t1;

    ByteArrayOutputStream err = new ByteArrayOutputStream();
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    PrintStream originalErr;
    PrintStream originalOut;

    public TestXmlRingingListener ( File reportDirectory ) {
        this.reportDirectory = reportDirectory;
    }

    private String formatTime ( long time ) {
        return TIME_FORMAT.format( ( ( double ) time ) / 1000 );
    }

    /**
     * Called before any tests have been run.
     *
     * @param description describes the tests to be run
     */
    @Override
    public void testRunStarted ( Description description ) throws Exception {

        originalErr = System.err;
        originalOut = System.out;
        System.setErr( new PrintStream( err ) );
        System.setOut( new PrintStream( out ) );
    }
    
    protected static String sanitizeXmlChars(String xml) {
        if (xml == null || ("".equals(xml))) return "";
        Pattern xmlInvalidChars =
          Pattern.compile("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\x{10000}-\\x{10FFFF}]");
        return xmlInvalidChars.matcher(xml).replaceAll("");
    }

    /**
     * Called when all tests have finished
     *
     * @param result the summary of the test run, including all the tests that failed
     */
    @Override
    public void testRunFinished ( Result result ) throws Exception {

        if ( !result.wasSuccessful() ) {
            statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        root.addAttribute( "tests", "" + result.getRunCount() );
        root.addAttribute( "name", name );
        root.addAttribute( "failures", "" + nFailure );
        root.addAttribute( "errors", "" + nError );
        root.addAttribute( "time", formatTime( result.getRunTime() ) );
        root.addAttribute( "hostname", getHostName() );
        root.addAttribute( "timestamp", getIsoTimestamp() );
        root.addElement( "system-out" ).addCDATA( sanitizeXmlChars( out.toString() ) );
        root.addElement( "system-err" ).addCDATA( sanitizeXmlChars( err.toString() ) );

        System.setErr( originalErr );
        System.setOut( originalOut );
    }

    public static String getIsoTimestamp () {
        DateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );
        final String timestamp = dateFormat.format( new Date() );
        return timestamp;
    }

    private String getHostName () {

        InetAddress address;
        try {
            address = InetAddress.getLocalHost();
            return address.getHostName().toLowerCase();
        } catch ( UnknownHostException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Called when an atomic test is about to be started.
     *
     * @param description description the description of the test that is about to be run (generally a class and method name)
     * @throws Exception
     */
    @Override
    public void testStarted ( Description description ) throws Exception {
        this.t1 = System.currentTimeMillis();
    }

    /**
     * Called when an atomic test has finished, whether the test succeeds or fails.
     *
     * @param description the description of the test that just ran
     */
    @Override
    public void testFinished ( Description description ) throws Exception {

        long time = System.currentTimeMillis() - t1;

        Element currentTestcase = root.addElement( "testcase" );
        currentTestcase.addAttribute( "time", formatTime( time ) );
        currentTestcase.addAttribute( "classname", description.getClassName() );
        currentTestcase.addAttribute( "name", description.getMethodName() );
        if ( currentFailureNode != null ) {
            currentTestcase.add( currentFailureNode );
            currentFailureNode = null;
        }
    }

    /**
     * Called when an atomic test fails.
     *
     * @param failure describes the test that failed and the exception that was thrown
     */
    @Override
    public void testFailure ( Failure failure ) throws Exception {

        if ( failure.getException() instanceof java.lang.AssertionError ) {
            failure( failure );
        } else {
            error( failure );
        }
    }

    private void error ( Failure failure ) {
        nError++;
        final String elementName = "error";
        currentFailureNode = createFailure( elementName, failure );
    }

    private Element createFailure ( String elementName, Failure failure ) {

        final Element element = DocumentHelper.createElement( elementName );
        element.addAttribute( "message", failure.getMessage() );
        element.addAttribute( "type", failure.getException().getClass().getName() );
        element.addText( failure.getTrace() );
        return element;
    }

    private void failure ( Failure failure ) {

        nFailure++;
        currentFailureNode = createFailure( "failure", failure );
    }

    /**
     * Called when a test will not be run, generally because a test method is annotated
     * with {@link org.junit.Ignore}.
     *
     * @param description describes the test that will not be run
     */
    @Override
    public void testIgnored ( Description description ) throws Exception {

        System.out.println( "Ignored" );
    }

    public void startFile ( Class<?> aClass ) {

        document = DocumentHelper.createDocument();
        root = document.addElement( "testsuite" );

        name = aClass.getName();
        this.fileName = "TEST-" + name + ".xml";
        System.out.println("*** Starting XML report file: " + this.fileName);//Debug code for the catalina.out
    }

    public void closeFile () {

        try {

            System.out.println("*** Closing XML report file: " + reportDirectory + File.separator + fileName);//Debug code for the catalina.out

            // lets write to a file
            OutputFormat outformat = OutputFormat.createPrettyPrint();
            outformat.setTrimText( false );

            XMLWriter writer = new XMLWriter(
                    new FileWriter( new File( reportDirectory, fileName ) ), outformat
            );

            writer.write( document );
            writer.close();

        } catch ( IOException e ) {
            throw new RuntimeException("Error closing XML report file [" + reportDirectory + File.separator + fileName + "]", e);
        }

    }

    public int getStatusCode () {
        return statusCode;
    }

}