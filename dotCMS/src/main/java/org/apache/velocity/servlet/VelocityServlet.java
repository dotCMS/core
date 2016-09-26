package org.apache.velocity.servlet;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.io.VelocityWriter;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.util.SimplePool;

/**
 * Base class which simplifies the use of Velocity with Servlets.
 * Extend this class, implement the <code>handleRequest()</code> method,
 * and add your data to the context.  Then call
 * <code>getTemplate("myTemplate.wm")</code>.
 *
 * This class puts some things into the context object that you should
 * be aware of:
 * <pre>
 * "req" - The HttpServletRequest object
 * "res" - The HttpServletResponse object
 * </pre>
 *
 * There are other methods you can override to access, alter or control
 * any part of the request processing chain.  Please see the javadocs for
 * more information on :
 * <ul>
 * <li> loadConfiguration() : for setting up the Velocity runtime
 * <li> createContext() : for creating and loading the Context
 * <li> setContentType() : for changing the content type on a request
 *                         by request basis
 * <li> handleRequest() : you <b>must</b> implement this
 * <li> mergeTemplate()  : the template rendering process
 * <li> requestCleanup() : post rendering resource or other cleanup
 * <li> error() : error handling
 * </ul>
 * <br>
 * If you put a String with key "contentType" object into the context within either your
 * servlet or within your template, then that will be used to override
 * the default content type specified in the properties file.
 *
 * @deprecated This servlet has been replaced by VelocityViewServlet,
 * available from the Velocity-Tools sub-project.  VelocityViewServlet
 * provides support for quick, clean MVC web development.
 * VelocityServlet will be removed in a future version of Velocity.
 *
 * @author Dave Bryson
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @author <a href="kjohnson@transparent.com">Kent Johnson</a>
 * @author <a href="dlr@finemaltcoding.com">Daniel Rall</a>
 * $Id: VelocityServlet.java 463298 2006-10-12 16:10:32Z henning $
 */
public abstract class VelocityServlet extends HttpServlet
{
    /**
     * The context key for the HTTP request object.
     */
    public static final String REQUEST = "req";

    /**
     * The context key for the HTTP response object.
     */
    public static final String RESPONSE = "res";

    /**
     * The HTTP content type context key.
     */
    public static final String CONTENT_TYPE = "default.contentType";

    /**
     *  The default content type for the response
     */
    public static final String DEFAULT_CONTENT_TYPE = "text/html";


    /**
     *  Encoding for the output stream
     */
    public static final String DEFAULT_OUTPUT_ENCODING = "ISO-8859-1";

    /**
     * The default content type, itself defaulting to {@link
     * #DEFAULT_CONTENT_TYPE} if not configured.
     */
    private static String defaultContentType;

    /**
     * This is the string that is looked for when getInitParameter is
     * called (<code>org.apache.velocity.properties</code>).
     */
    protected static final String INIT_PROPS_KEY =
        "org.apache.velocity.properties";

    /**
     * Use of this properties key has been deprecated, and will be
     * removed in Velocity version 1.5.
     */
    private static final String OLD_INIT_PROPS_KEY = "properties";

    /**
     * Cache of writers
     */

    private static SimplePool writerPool = new SimplePool(40);

    /**
     * Performs initialization of this servlet.  Called by the servlet
     * container on loading.
     *
     * @param config The servlet configuration to apply.
     *
     * @exception ServletException
     */
    public void init( ServletConfig config )
        throws ServletException
    {
        super.init( config );

        /*
         *  do whatever we have to do to init Velocity
         */
        initVelocity( config );

        /*
         *  Now that Velocity is initialized, cache some config.
         */
        VelocityServlet.defaultContentType =
                RuntimeSingleton.getString(CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
    }

    /**
     *  Initializes the Velocity runtime, first calling
     *  loadConfiguration(ServletConvig) to get a
     *  java.util.Properties of configuration information
     *  and then calling Velocity.init().  Override this
     *  to do anything to the environment before the
     *  initialization of the singelton takes place, or to
     *  initialize the singleton in other ways.
     * @param config
     * @throws ServletException
     */
    protected void initVelocity( ServletConfig config )
         throws ServletException
    {
        try
        {
            /*
             *  call the overridable method to allow the
             *  derived classes a shot at altering the configuration
             *  before initializing Runtime
             */

            Properties props = loadConfiguration( config );

            Velocity.init( props );
        }
        catch( Exception e )
        {
            throw new ServletException("Error initializing Velocity: " + e, e);
        }
    }

    /**
     *  Loads the configuration information and returns that
     *  information as a Properties, which will be used to
     *  initialize the Velocity runtime.
     *  <br><br>
     *  Currently, this method gets the initialization parameter
     *  VelocityServlet.INIT_PROPS_KEY, which should be a file containing
     *  the configuration information.
     *  <br><br>
     *  To configure your Servlet Spec 2.2 compliant servlet runner to pass
     *  this to you, put the following in your WEB-INF/web.xml file
     *  <br>
     *  <pre>
     *    &lt;servlet&gt;
     *      &lt;servlet-name&gt; YourServlet &lt/servlet-name&gt;
     *      &lt;servlet-class&gt; your.package.YourServlet &lt;/servlet-class&gt;
     *      &lt;init-param&gt;
     *         &lt;param-name&gt; org.apache.velocity.properties &lt;/param-name&gt;
     *         &lt;param-value&gt; velocity.properties &lt;/param-value&gt;
     *      &lt;/init-param&gt;
     *    &lt;/servlet&gt;
     *   </pre>
     *
     * Alternately, if you wish to configure an entire context in this
     * fashion, you may use the following:
     *  <br>
     *  <pre>
     *    &lt;context-param&gt;
     *       &lt;param-name&gt; org.apache.velocity.properties &lt;/param-name&gt;
     *       &lt;param-value&gt; velocity.properties &lt;/param-value&gt;
     *       &lt;description&gt; Path to Velocity configuration &lt;/description&gt;
     *    &lt;/context-param&gt;
     *   </pre>
     *
     *  Derived classes may do the same, or take advantage of this code to do the loading for them via :
     *   <pre>
     *      Properties p = super.loadConfiguration( config );
     *   </pre>
     *  and then add or modify the configuration values from the file.
     *  <br>
     *
     *  @param config ServletConfig passed to the servlets init() function
     *                Can be used to access the real path via ServletContext (hint)
     *  @return java.util.Properties loaded with configuration values to be used
     *          to initialize the Velocity runtime.
     *  @throws FileNotFoundException if a specified file is not found.
     *  @throws IOException I/O problem accessing the specified file, if specified.
     * @deprecated Use VelocityViewServlet from the Velocity Tools
     * library instead.
     */
    protected Properties loadConfiguration(ServletConfig config)
        throws IOException, FileNotFoundException
    {
        // This is a little overly complex because of legacy support
        // for the initialization properties key "properties".
        // References to OLD_INIT_PROPS_KEY should be removed at
        // Velocity version 1.5.
        String propsFile = config.getInitParameter(INIT_PROPS_KEY);
        if (propsFile == null || propsFile.length() == 0)
        {
            ServletContext sc = config.getServletContext();
            propsFile = config.getInitParameter(OLD_INIT_PROPS_KEY);
            if (propsFile == null || propsFile.length() == 0)
            {
                propsFile = sc.getInitParameter(INIT_PROPS_KEY);
                if (propsFile == null || propsFile.length() == 0)
                {
                    propsFile = sc.getInitParameter(OLD_INIT_PROPS_KEY);
                    if (propsFile != null && propsFile.length() > 0)
                    {
                        sc.log("Use of the properties initialization " +
                               "parameter '" + OLD_INIT_PROPS_KEY + "' has " +
                               "been deprecated by '" + INIT_PROPS_KEY + '\'');
                    }
                }
            }
            else
            {
                sc.log("Use of the properties initialization parameter '" +
                       OLD_INIT_PROPS_KEY + "' has been deprecated by '" +
                       INIT_PROPS_KEY + '\'');
            }
        }

        /*
         * This will attempt to find the location of the properties
         * file from the relative path to the WAR archive (ie:
         * docroot). Since JServ returns null for getRealPath()
         * because it was never implemented correctly, then we know we
         * will not have an issue with using it this way. I don't know
         * if this will break other servlet engines, but it probably
         * shouldn't since WAR files are the future anyways.
         */

        Properties p = new Properties();

        if ( propsFile != null )
        {
            p.load(getServletContext().getResourceAsStream(propsFile));
        }

        return p;
    }

    /**
     * Handles HTTP <code>GET</code> requests by calling {@link
     * #doRequest(HttpServletRequest, HttpServletResponse)}.
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void doGet( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        doRequest(request, response);
    }

    /**
     * Handles HTTP <code>POST</code> requests by calling {@link
     * #doRequest(HttpServletRequest, HttpServletResponse)}.
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void doPost( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        doRequest(request, response);
    }

    /**
     *  Handles all requests (by default).
     *
     *  @param request  HttpServletRequest object containing client request
     *  @param response HttpServletResponse object for the response
     * @throws ServletException
     * @throws IOException
     */
    protected void doRequest(HttpServletRequest request, HttpServletResponse response )
         throws ServletException, IOException
    {
        Context context = null;
        try
        {
            /*
             *  first, get a context
             */

            context = createContext( request, response );

            /*
             *   set the content type
             */

            setContentType( request, response );

            /*
             *  let someone handle the request
             */

            Template template = handleRequest( request, response, context );
            /*
             *  bail if we can't find the template
             */

            if ( template == null )
            {
                return;
            }

            /*
             *  now merge it
             */

            mergeTemplate( template, context, response );
        }
        catch (Exception e)
        {
            /*
             *  call the error handler to let the derived class
             *  do something useful with this failure.
             */

            error( request, response, e);
        }
        finally
        {
            /*
             *  call cleanup routine to let a derived class do some cleanup
             */

            requestCleanup( request, response, context );
        }
    }

    /**
     *  A cleanup routine which is called at the end of the {@link
     *  #doRequest(HttpServletRequest, HttpServletResponse)}
     *  processing sequence, allowing a derived class to do resource
     *  cleanup or other end of process cycle tasks.
     *
     *  @param request servlet request from client
     *  @param response servlet reponse
     *  @param context  context created by the createContext() method
     */
    protected void requestCleanup( HttpServletRequest request, HttpServletResponse response, Context context )
    {
    }

    /**
     *  merges the template with the context.  Only override this if you really, really
     *  really need to. (And don't call us with questions if it breaks :)
     *
     *  @param template template object returned by the handleRequest() method
     *  @param context  context created by the createContext() method
     *  @param response servlet reponse (use this to get the output stream or Writer
     * @throws ResourceNotFoundException
     * @throws ParseErrorException
     * @throws MethodInvocationException
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws Exception
     */
    protected void mergeTemplate( Template template, Context context, HttpServletResponse response )
        throws ResourceNotFoundException, ParseErrorException,
               MethodInvocationException, IOException, UnsupportedEncodingException, Exception
    {
        ServletOutputStream output = response.getOutputStream();
        VelocityWriter vw = null;
        // ASSUMPTION: response.setContentType() has been called.
        String encoding = response.getCharacterEncoding();

        try
        {
            vw = (VelocityWriter) writerPool.get();

            if (vw == null)
            {
                vw = new VelocityWriter(new OutputStreamWriter(output,
                                                               encoding),
                                        4 * 1024, true);
            }
            else
            {
                vw.recycle(new OutputStreamWriter(output, encoding));
            }

            template.merge(context, vw);
        }
        finally
        {
            if (vw != null)
            {
                try
                {
                    /*
                     *  flush and put back into the pool
                     *  don't close to allow us to play
                     *  nicely with others.
                     */
                    vw.flush();
                }
                catch (IOException e)
                {
                    // do nothing
                }

                /*
                 * Clear the VelocityWriter's reference to its
                 * internal OutputStreamWriter to allow the latter
                 * to be GC'd while vw is pooled.
                 */
                vw.recycle(null);
                writerPool.put(vw);
            }
        }
    }

    /**
     * Sets the content type of the response, defaulting to {@link
     * #defaultContentType} if not overriden.  Delegates to {@link
     * #chooseCharacterEncoding(HttpServletRequest)} to select the
     * appropriate character encoding.
     *
     * @param request The servlet request from the client.
     * @param response The servlet reponse to the client.
     */
    protected void setContentType(HttpServletRequest request,
                                  HttpServletResponse response)
    {
        String contentType = VelocityServlet.defaultContentType;
        int index = contentType.lastIndexOf(';') + 1;
        if (index <= 0 || (index < contentType.length() &&
                           contentType.indexOf("charset", index) == -1))
        {
            // Append the character encoding which we'd like to use.
            String encoding = chooseCharacterEncoding(request);
            //RuntimeSingleton.debug("Chose output encoding of '" +
            //                       encoding + '\'');
            if (!DEFAULT_OUTPUT_ENCODING.equalsIgnoreCase(encoding))
            {
                contentType += "; charset=" + encoding;
            }
        }
        response.setContentType(contentType);
        //RuntimeSingleton.debug("Response Content-Type set to '" +
        //                       contentType + '\'');
    }

    /**
     * Chooses the output character encoding to be used as the value
     * for the "charset=" portion of the HTTP Content-Type header (and
     * thus returned by <code>response.getCharacterEncoding()</code>).
     * Called by {@link #setContentType(HttpServletRequest,
     * HttpServletResponse)} if an encoding isn't already specified by
     * Content-Type.  By default, chooses the value of
     * RuntimeSingleton's <code>output.encoding</code> property.
     *
     * @param request The servlet request from the client.
     * @return The chosen character encoding.
     */
    protected String chooseCharacterEncoding(HttpServletRequest request)
    {
        return RuntimeSingleton.getString(RuntimeConstants.OUTPUT_ENCODING,
                                          DEFAULT_OUTPUT_ENCODING);
    }

    /**
     *  Returns a context suitable to pass to the handleRequest() method
     *  <br><br>
     *  Default implementation will create a VelocityContext object,
     *   put the HttpServletRequest and HttpServletResponse
     *  into the context accessable via the keys VelocityServlet.REQUEST and
     *  VelocityServlet.RESPONSE, respectively.
     *
     *  @param request servlet request from client
     *  @param response servlet reponse to client
     *
     *  @return context
     */
    protected Context createContext(HttpServletRequest request,  HttpServletResponse response )
    {
        /*
         *   create a new context
         */

        VelocityContext context = new VelocityContext();

        /*
         *   put the request/response objects into the context
         *   wrap the HttpServletRequest to solve the introspection
         *   problems
         */

        context.put( REQUEST,  request );
        context.put( RESPONSE, response );

        return context;
    }

    /**
     * Retrieves the requested template.
     *
     * @param name The file name of the template to retrieve relative to the
     *             template root.
     * @return     The requested template.
     * @throws ResourceNotFoundException if template not found
     *          from any available source.
     * @throws ParseErrorException if template cannot be parsed due
     *          to syntax (or other) error.
     * @throws Exception if an error occurs in template initialization
     */
    public Template getTemplate( String name )
        throws ResourceNotFoundException, ParseErrorException, Exception
    {
        return RuntimeSingleton.getTemplate(name);
    }

    /**
     * Retrieves the requested template with the specified
     * character encoding.
     *
     * @param name The file name of the template to retrieve relative to the
     *             template root.
     * @param encoding the character encoding of the template
     *
     * @return     The requested template.
     * @throws ResourceNotFoundException if template not found
     *          from any available source.
     * @throws ParseErrorException if template cannot be parsed due
     *          to syntax (or other) error.
     * @throws Exception if an error occurs in template initialization
     *
     *  @since Velocity v1.1
     */
    public Template getTemplate( String name, String encoding )
        throws ResourceNotFoundException, ParseErrorException, Exception
    {
        return RuntimeSingleton.getTemplate( name, encoding );
    }

    /**
     * Implement this method to add your application data to the context,
     * calling the <code>getTemplate()</code> method to produce your return
     * value.
     * <br><br>
     * In the event of a problem, you may handle the request directly
     * and return <code>null</code> or throw a more meaningful exception
     * for the error handler to catch.
     *
     *  @param request servlet request from client
     *  @param response servlet reponse
     *  @param ctx The context to add your data to.
     *  @return    The template to merge with your context or null, indicating
     *    that you handled the processing.
     * @throws Exception
     *
     *  @since Velocity v1.1
     */
    protected Template handleRequest( HttpServletRequest request, HttpServletResponse response, Context ctx )
        throws Exception
    {
        /*
         * invoke handleRequest
         */

        Template t =  handleRequest( ctx );

        /*
         *  if it returns null, this is the 'old' deprecated
         *  way, and we want to mimic the behavior for a little
         *  while anyway
         */

        if (t == null)
        {
            throw new Exception ("handleRequest(Context) returned null - no template selected!" );
        }

        return t;
    }

    /**
     * Implement this method to add your application data to the context,
     * calling the <code>getTemplate()</code> method to produce your return
     * value.
     * <br><br>
     * In the event of a problem, you may simple return <code>null</code>
     * or throw a more meaningful exception.
     *
     * @deprecated Use
     * {@link #handleRequest( HttpServletRequest request,
     * HttpServletResponse response, Context ctx )}
     *
     * @param ctx The context to add your data to.
     * @return    The template to merge with your context.
     * @throws Exception
     */
    protected Template handleRequest( Context ctx )
        throws Exception
    {
        throw new Exception ("You must override VelocityServlet.handleRequest( Context) "
                             + " or VelocityServlet.handleRequest( HttpServletRequest, "
                             + " HttpServletResponse, Context)" );
    }

    /**
     * Invoked when there is an error thrown in any part of doRequest() processing.
     * <br><br>
     * Default will send a simple HTML response indicating there was a problem.
     *
     * @param request original HttpServletRequest from servlet container.
     * @param response HttpServletResponse object from servlet container.
     * @param cause  Exception that was thrown by some other part of process.
     * @throws ServletException
     * @throws IOException
     */
    protected void error( HttpServletRequest request, HttpServletResponse response, Exception cause )
        throws ServletException, IOException
    {
        StringBuffer html = new StringBuffer();
        html.append("<html>");
        html.append("<title>Error</title>");
        html.append("<body bgcolor=\"#ffffff\">");
        html.append("<h2>VelocityServlet: Error processing the template</h2>");
        html.append("<pre>");
        String why = cause.getMessage();
        if (why != null && why.trim().length() > 0)
        {
            html.append(why);
            html.append("<br>");
        }

        StringWriter sw = new StringWriter();
        cause.printStackTrace( new PrintWriter( sw ) );

        html.append( sw.toString()  );
        html.append("</pre>");
        html.append("</body>");
        html.append("</html>");
        response.getOutputStream().print( html.toString() );
    }
}
