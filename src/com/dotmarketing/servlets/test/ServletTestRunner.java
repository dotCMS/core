package com.dotmarketing.servlets.test;

import com.dotmarketing.listeners.TestRingingListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.JUnitCore;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Jonathan Gamba.
 * Date: 3/7/12
 * Time: 7:08 PM
 */
public class ServletTestRunner extends HttpServlet {

    private static final Log Logger = LogFactory.getLog( ServletTestRunner.class );

    public static final String ALL_TESTS_SUITE = "com.AllTestsSuite";

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

        //Getting the junit test class to run
        String className = request.getParameter( "class" );

        //If nothing was sent lets run all the tests
        if ( className == null || className.isEmpty() ) {
            className = ALL_TESTS_SUITE;
        }

        response.setContentType( "text/plain" );
        /*OutputStream out = response.getOutputStream();
        final PrintStream pout = new PrintStream( out );

        //Running the given junit test class
        new JUnitCore().runMain( new JUnitSystem() {

            public PrintStream out () {
                return pout;
            }

            public void exit ( int arg0 ) {
            }

        }, className );

        out.close();*/

        Logger.info( "Running unit tests....." );

        //Running the given junit test class
        JUnitCore jUnitCore = new JUnitCore();
        //Adding a listener for the running test
        TestRingingListener testRingingListener = new TestRingingListener();
        jUnitCore.addListener( testRingingListener );
        try {
            jUnitCore.run( Class.forName( className ) );
        } catch ( ClassNotFoundException e ) {
            throw new ServletException( e );
        }

        //Setting the response
        response.setStatus( testRingingListener.getStatusCode() );
        Logger.info( testRingingListener.toString() );
        response.getWriter().print( testRingingListener.toString() );
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

}