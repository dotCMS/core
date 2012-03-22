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


import javax.servlet.ServletContext;

import org.apache.velocity.runtime.log.LogSystem;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;


/**
 * Simple wrapper for the servlet log.  This has Velocity log
 * messages to ServletContext.log(String).
 *
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Revision: 71982 $ $Date: 2004-02-18 12:11:07 -0800 (Wed, 18 Feb 2004) $
 */
public class ServletLogger implements LogSystem
{
    protected ServletContext servletContext = null;

    public static final String PREFIX = " Velocity ";

    /**
     * Construct a simple logger for a servlet environment.
     * <br>
     * NOTE: this class expects that the ServletContext has already
     *       been placed in the runtime's application attributes
     *       under its full class name (i.e. "javax.servlet.ServletContext").
     */
    public ServletLogger()
    {
    }

    /**
     * init()
     * 
     * @throws IllegalStateException if the ServletContext is not available
     *         in the application attributes under the appropriate key.
     */
    public void init( RuntimeServices rs ) 
        throws Exception
    {
        Object obj = rs.getApplicationAttribute(ServletContext.class.getName());
        if (obj == null)
        {
            throw new IllegalStateException("Could not retrieve ServletContext from application attributes!");
        }
        servletContext = (ServletContext)obj;
    }

    /**
     * Send a log message from Velocity.
     */
    public void logVelocityMessage(int level, String message)
    {
        switch (level) 
        {
            case LogSystem.WARN_ID:
                servletContext.log( PREFIX + RuntimeConstants.WARN_PREFIX + message );
                break;
            case LogSystem.INFO_ID:
                servletContext.log( PREFIX + RuntimeConstants.INFO_PREFIX + message);
                break;
            case LogSystem.DEBUG_ID:
                servletContext.log( PREFIX + RuntimeConstants.DEBUG_PREFIX + message);
                break;
            case LogSystem.ERROR_ID:
                servletContext.log( PREFIX + RuntimeConstants.ERROR_PREFIX + message);
                break;
            default:
                servletContext.log( PREFIX + " : " + message);
                break;
        }
    }

}
