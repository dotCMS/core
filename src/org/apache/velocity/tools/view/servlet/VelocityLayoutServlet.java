/*
 * Copyright 2003 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.velocity.tools.view.servlet;


import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.view.servlet.VelocityViewServlet;


/**
 * Extension of the VelocityViewServlet to perform "two-pass" 
 * layout rendering and allow for a customized error screen.
 *
 * For a brief user-guide to this, i suggest trying to track down
 * the VLS_README.txt file that hopefully made it into the docs
 * somewhere.
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @version $Id: VelocityLayoutServlet.java 72116 2004-11-11 07:01:21Z nbubna $
 */

public class VelocityLayoutServlet extends VelocityViewServlet 
{

    /**
     * The velocity.properties key for specifying the
     * servlet's error template.
     */
    public static final String PROPERTY_ERROR_TEMPLATE = 
        "tools.view.servlet.error.template";

    /**
     * The velocity.properties key for specifying the
     * relative directory holding layout templates.
     */
    public static final String PROPERTY_LAYOUT_DIR = 
        "tools.view.servlet.layout.directory";

    /**
     * The velocity.properties key for specifying the
     * servlet's default layout template's filename. 
     */
    public static final String PROPERTY_DEFAULT_LAYOUT = 
        "tools.view.servlet.layout.default.template";


    /**
     * The default error template's filename. 
     */
    public static String DEFAULT_ERROR_TEMPLATE = "Error.vm";

    /**
     * The default layout directory 
     */
    public static String DEFAULT_LAYOUT_DIR = "layout/";

    /**
     * The default filename for the servlet's default layout
     */
    public static String DEFAULT_DEFAULT_LAYOUT = "Default.vm";


    //TODO? if demand for it exists, these context keys can be 
    //      made configurable by the velocity.properties
    //      until such time, if anyone really needs to change these,
    //      they are public and aren't final and can be altered

    /**
     * The context key that will hold the content of the screen.
     * 
     * This key ($screen_content) must be present in the layout
     * template for the current screen to be rendered.
     */
    public static String KEY_SCREEN_CONTENT = "screen_content";

    /**
     * The context/parameter key used to specify an alternate
     * layout to be used for a request instead of the default layout.
     */
    public static String KEY_LAYOUT = "layout";


    /**
     * The context key that holds the {@link Throwable} that
     * broke the rendering of the requested screen.
     */
    public static String KEY_ERROR_CAUSE = "error_cause";

    /**
     * The context key that holds the stack trace of the error that
     * broke the rendering of the requested screen.
     */
    public static String KEY_ERROR_STACKTRACE = "stack_trace";

    /**
     * The context key that holds the {@link MethodInvocationException} 
     * that broke the rendering of the requested screen.
     *
     * If this value is placed in the context, then $error_cause
     * will hold the error that this invocation exception is wrapping.
     */
    public static String KEY_ERROR_INVOCATION_EXCEPTION = "invocation_exception";


    protected String errorTemplate;
    protected String layoutDir;
    protected String defaultLayout;

    // keep a local reference for convenience
    private VelocityEngine velocity;

    /**
     * Initializes Velocity, the view servlet and checks for changes to 
     * the initial layout configuration.
     *
     * @param config servlet configuration parameters
     */
    public void init(ServletConfig config) throws ServletException 
    {
        // first do VVS' init()
        super.init(config);

        // grab the initialized engine
        velocity = super.getVelocityEngine();

        // check for default template path overrides
        errorTemplate = 
            getVelocityProperty(PROPERTY_ERROR_TEMPLATE, DEFAULT_ERROR_TEMPLATE);
        layoutDir = 
            getVelocityProperty(PROPERTY_LAYOUT_DIR, DEFAULT_LAYOUT_DIR);
        defaultLayout = 
            getVelocityProperty(PROPERTY_DEFAULT_LAYOUT, DEFAULT_DEFAULT_LAYOUT);

        // preventive error checking! directory must end in /
        if (!layoutDir.endsWith("/")) 
        {
            layoutDir += '/';
        }

        // log the current settings
        velocity.info("VelocityLayoutServlet: Error screen is '"+errorTemplate+"'");
        velocity.info("VelocityLayoutServlet: Layout directory is '"+layoutDir+"'");
        velocity.info("VelocityLayoutServlet: Default layout template is '"+defaultLayout+"'");

        // for efficiency's sake, make defaultLayout a full path now
        defaultLayout = layoutDir + defaultLayout;
    }


    /**
     * Overrides VelocityViewServlet to check the request for 
     * an alternate layout
     *
     * @param request client request
     * @param response client response
     * @return the Context to fill
     */
    protected Context createContext(HttpServletRequest request,
                                    HttpServletResponse response) 
    {

        Context ctx = super.createContext(request, response);

        // check if an alternate layout has been specified 
        // by way of the request parameters
        String layout = request.getParameter(KEY_LAYOUT);
        if (layout != null) 
        {
            // let the template know what its new layout is
            ctx.put(KEY_LAYOUT, layout);
        }
        return ctx;
    }


    /**
     * Overrides VelocityViewServlet.mergeTemplate to do a two-pass 
     * render for handling layouts
     */
    protected void mergeTemplate(Template template, 
                                 Context context, 
                                 HttpServletResponse response)
        throws ResourceNotFoundException, ParseErrorException, 
               MethodInvocationException, IOException,
               UnsupportedEncodingException, Exception 
    {
        //
        // this section is based on Tim Colson's "two pass render"
        //
        // Render the screen content
        StringWriter sw = new StringWriter();
        template.merge(context, sw);
        // Add the resulting content to the context
        context.put(KEY_SCREEN_CONTENT, sw.toString());

        // Check for an alternate layout
        //
        // we check after merging the screen template so the screen 
        // can overrule any layout set in the request parameters
        // by doing #set( $layout = "MyLayout.vm" )
        Object obj = context.get(KEY_LAYOUT);
        String layout = (obj == null) ? null : obj.toString();
        if (layout == null) 
        {
            // no alternate, use default
            layout = defaultLayout;
        } 
        else 
        {
            // make it a full(er) path
            layout = layoutDir + layout;
        }

        try 
        {
            //load the layout template
            template = getTemplate(layout);
        } 
        catch (Exception e) 
        {
            velocity.error("VelocityLayoutServlet: Can't load layout \"" + 
                           layout + "\": " + e);

            // if it was an alternate layout we couldn't get...
            if (!layout.equals(defaultLayout)) 
            {
                // try to get the default layout
                // if this also fails, let the exception go
                template = getTemplate(defaultLayout);
            }
        }

        // Render the layout template into the response
        super.mergeTemplate(template, context, response);
    }


    /**
     * Overrides VelocityViewServlet to display user's custom error template
     */
    protected void error(HttpServletRequest request, 
                         HttpServletResponse response, 
                         Exception e)
        throws ServletException 
    {
        try 
        {
            // get a velocity context
            Context ctx = createContext(request, response);

            Throwable cause = e;

            // if it's an MIE, i want the real cause and stack trace!
            if (cause instanceof MethodInvocationException) 
            {
                // put the invocation exception in the context
                ctx.put(KEY_ERROR_INVOCATION_EXCEPTION, e);
                // get the real cause
                cause = ((MethodInvocationException)e).getWrappedThrowable();
            }

            // add the cause to the context
            ctx.put(KEY_ERROR_CAUSE, cause);
                
            // grab the cause's stack trace and put it in the context
            StringWriter sw = new StringWriter();
            cause.printStackTrace(new java.io.PrintWriter(sw));
            ctx.put(KEY_ERROR_STACKTRACE, sw.toString());

            // retrieve and render the error template
            Template et = getTemplate(errorTemplate);
            mergeTemplate(et, ctx, response);

        } 
        catch (Exception e2) 
        {
            // d'oh! log this
            velocity.error("VelocityLayoutServlet: " + 
                           " Error during error template rendering - " + e2);
            // then punt the original to a higher authority
            super.error(request, response, e);
        }
    }


}
