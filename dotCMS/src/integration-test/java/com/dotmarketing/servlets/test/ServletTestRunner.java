package com.dotmarketing.servlets.test;

import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotmarketing.listeners.TestTextRingingListener;
import com.dotmarketing.listeners.TestXmlRingingListener;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.liferay.util.FileUtil;

import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Created by Jonathan Gamba.
 * Date: 3/7/12
 * Time: 7:08 PM
 */
public class ServletTestRunner extends HttpServlet {

    private static final Log Logger = LogFactory.getLog( ServletTestRunner.class );

    public static final String ALL_TESTS_SUITE = "com.AllTestsSuite";
    public static final String RESULT_TYPE_PLAIN = "plain";
    public static final String RESULT_TYPE_FILE = "file";
    
    public static ThreadLocal<HttpServletRequest> localRequest=new ThreadLocal<HttpServletRequest>();
    public static ThreadLocal<HttpServletResponse> localResponse=new ThreadLocal<HttpServletResponse>();
    

    /**
     * Servlet that will respond to an url pattern "/servlet/test".<br>
     * This call will accept a "class" parameter, we can send in this parameter the class of the junit o suite class to execute. If no class is
     * sent the servlet will execute all the tests it has in the {@link com.AllTestsSuite} class.<br><br>
     * <b>Examples: http://localhost:8080/servlet/test, http://localhost:8080/servlet/test?class=com.dotmarketing.portlets.structure.factories.FieldFactoryTest</b>
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @see com.AllTestsSuite
     */
    public void doGet ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        // exposing request/response
        localRequest.set(request);
        localResponse.set(response);
        
        //Getting the junit test class to run
        String className = request.getParameter( "class" );
        String methodName = request.getParameter( "method" );
        String resultType = request.getParameter( "resultType" ); //plain/file

        //If nothing was sent lets run all the tests
        if ( className == null || className.isEmpty() ) {
            className = ALL_TESTS_SUITE;
        }

        Logger.info( "Running unit tests....." );

        System.setProperty("TEST-RUNNER", ServletTestRunner.class.getCanonicalName());

        //If nothing is present the default is to create an xml report
        if ( resultType == null || resultType.isEmpty() ) {
            xmlReport( response, className, methodName );
        } else {

            if ( resultType.equals( RESULT_TYPE_PLAIN ) ) {
                plainTextReport( response, className, methodName );
            } else {
                xmlReport( response, className, methodName );
            }
        }
    }

    /**
     * Servlet that will respond to an url pattern "/servlet/test".<br>
     * This call will accept a "class" parameter, we can send in this parameter the class of the junit o suite class to execute. If no class is
     * sent the servlet will execute all the tests it has in the {@link com.AllTestsSuite} class.<br><br>
     * <b>Examples: http://localhost:8080/servlet/test, http://localhost:8080/servlet/test?class=com.dotmarketing.portlets.structure.factories.FieldFactoryTest</b>
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @see com.AllTestsSuite
     */
    protected void doPost ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        doGet( request, response );
    }

    /**
     * This method will run a given unit tests class using a custom xml listener who will create an xml report for the result of the tests.
     * <p/>Those xml reports are under <b>./tomcat/logs/test/</b> folder
     *
     * @param response
     * @param className
     * @throws ServletException
     * @throws IOException
     */
    private void xmlReport ( HttpServletResponse response, String className, String methodName ) throws ServletException, IOException {

        //Running the given junit test class
        JUnitCore jUnitCore = new JUnitCore();

        //Preparing the reports folder
        String logsDirectory = ConfigUtils.getDynamicContentPath() + "/logs/test/";
        File reportDirectory = new File( logsDirectory );
        FileUtils.deleteDirectory( reportDirectory );
        reportDirectory.mkdirs();

        Logger.info( "Generating XML report in " + reportDirectory.getAbsolutePath() );
        System.out.println("Generating XML report in: " + reportDirectory.getAbsolutePath());//Debug code for the catalina.out

        //Adding a listener for the running test
        TestXmlRingingListener testXmlRingingListener = new TestXmlRingingListener( reportDirectory );
        jUnitCore.addListener( testXmlRingingListener );

        try {
            Class clazz = Class.forName( className );
            testXmlRingingListener.startFile( clazz );

            if(!Strings.isNullOrEmpty(methodName)) {
                jUnitCore.run(Request.method(clazz, methodName));
            } else {
                jUnitCore.run( clazz );
            }

            testXmlRingingListener.closeFile();
        } catch ( ClassNotFoundException e ) {
            throw new ServletException( e );
        }

        //Setting the response
        response.setStatus( testXmlRingingListener.getStatusCode() );
    }

    /**
     * This method will run a given unit tests class using a custom listener who will return the results of the tests as a plain text also it will create a log file for those results.
     * <p/>The test results are under <b>./tomcat/logs/</b> folder
     *
     * @param response
     * @param className
     * @throws ServletException
     * @throws IOException
     */
    private void plainTextReport ( HttpServletResponse response, String className, String methodName ) throws ServletException, IOException {

        response.setContentType( "text/plain" );

        //Running the given junit test class
        JUnitCore jUnitCore = new JUnitCore();
        //Adding a listener for the running test
        TestTextRingingListener testRingingListener = new TestTextRingingListener();
        jUnitCore.addListener( testRingingListener );
        try {
            Class clazz = Class.forName( className );

            if(!Strings.isNullOrEmpty(methodName)) {
                jUnitCore.run(Request.method(clazz, methodName));
            } else {
                jUnitCore.run( clazz );
            }
        } catch ( ClassNotFoundException e ) {
            throw new ServletException( e );
        }

        //Setting the response
        response.setStatus( testRingingListener.getStatusCode() );
        Logger.info( testRingingListener.toString() );
        response.getWriter().print( testRingingListener.toString() );
    }

}