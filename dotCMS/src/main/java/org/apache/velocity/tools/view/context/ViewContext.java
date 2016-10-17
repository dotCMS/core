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

package org.apache.velocity.tools.view.context;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;


/**
 * <p>Objects implementing this interface are passed to view tools
 * upon initialization by the 
 * {@link org.apache.velocity.tools.view.servlet.ServletToolboxManager}.</p> 
 * 
 * <p>The interface provides view tools in a servlet environment 
 * access to relevant context information, like servlet request, servlet 
 * context and the velocity context.</p>
 *
 * @author <a href="mailto:sidler@teamup.com">Gabe Sidler</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 *
 * @version $Id: ViewContext.java 72106 2004-11-11 04:07:24Z nbubna $ 
 */
public interface ViewContext
{
    /** Key used for the HTTP request object. */
    public static final String REQUEST = "request";

    /** Key used for the HTTP response object. */
    public static final String RESPONSE = "response";

    /** Key used for the HTTP session object. */
    public static final String SESSION = "session";

    /** Key used for the servlet context object. */
    public static final String APPLICATION = "application";

    /** Key used for XHTML setting (tells tools and macros to output XHTML). */
    public static final String XHTML = "XHTML";


    /**
     * <p>Returns the instance of {@link HttpServletRequest} for this request.</p>
     */
    public HttpServletRequest getRequest();


    /**
     * <p>Returns the instance of {@link HttpServletResponse} for this request.</p>
     */
    public HttpServletResponse getResponse();


    /**
     * <p>Returns the instance of {@link ServletContext} for this request.</p>
     */
    public ServletContext getServletContext();


    /**
     * <p>Searches for the named attribute in request, session (if valid), 
     * and application scope(s) in order and returns the value associated 
     * or null.</p>
     *
     * @since VelocityTools 1.1
     */
    public Object getAttribute(String key);


    /**
     * <p>Returns a reference to the current Velocity context.</p>
     */
    public Context getVelocityContext();


    /**
     * <p>Returns the current VelocityEngine instance.</p>
     */
    public VelocityEngine getVelocityEngine();

}
