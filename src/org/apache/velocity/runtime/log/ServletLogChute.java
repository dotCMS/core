package org.apache.velocity.runtime.log;

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

import java.io.StringWriter;
import java.io.PrintWriter;
import javax.servlet.ServletContext;
import org.apache.velocity.runtime.RuntimeServices;

/**
 * Simple wrapper for the servlet log.  This passes Velocity log
 * messages to ServletContext.log(String).  You may configure the
 * level of output in your velocity.properties by adding the
 * "runtime.log.logsystem.servlet.level" property with one of the
 * following values: error, warn, info, debug, or trace.  The default
 * is trace.
 *
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @author Nathan Bubna
 * @version $Revision: 730039 $ $Date: 2008-12-29 19:53:19 -0800 (Mon, 29 Dec 2008) $
 * @since 1.6
 */
public class ServletLogChute implements LogChute
{
    public static final String RUNTIME_LOG_LEVEL_KEY = 
        "runtime.log.logsystem.servlet.level";

    private int enabled = TRACE_ID;

    protected ServletContext servletContext = null;

    public static final String PREFIX = " Velocity ";

    /**
     * Construct a simple logger for a servlet environment.
     * <br>
     * NOTE: this class expects that the ServletContext has already
     *       been placed in the runtime's application attributes
     *       under its full class name (i.e. "javax.servlet.ServletContext").
     */
    public ServletLogChute()
    {
    }

    /**
     * init()
     *
     * @throws IllegalStateException if the ServletContext is not available
     *         in the application attributes under the appropriate key.
     */
    public void init(RuntimeServices rs) throws Exception
    {
        Object obj = rs.getApplicationAttribute(ServletContext.class.getName());
        if (obj == null)
        {
            throw new UnsupportedOperationException("Could not retrieve ServletContext from application attributes");
        }
        servletContext = (ServletContext)obj;

        // look for a level config property
        String level = (String)rs.getProperty(RUNTIME_LOG_LEVEL_KEY);
        if (level != null)
        {
            // and set it accordingly
            setEnabledLevel(toLevel(level));
        }
    }

    protected int toLevel(String level) {
        if (level.equalsIgnoreCase("debug"))
        {
            return DEBUG_ID;
        }
        else if (level.equalsIgnoreCase("info"))
        {
            return INFO_ID;
        }
        else if (level.equalsIgnoreCase("warn"))
        {
            return WARN_ID;
        }
        else if (level.equalsIgnoreCase("error"))
        {
            return ERROR_ID;
        }
        else
        {
            return TRACE_ID;
        }
    }

    /**
     * Set the minimum level at which messages will be printed.
     */
    public void setEnabledLevel(int level)
    {
        this.enabled = level;
    }

    /**
     * Returns the current minimum level at which messages will be printed.
     */
    public int getEnabledLevel()
    {
        return this.enabled;
    }

    /**
     * This will return true if the specified level
     * is equal to or higher than the level this
     * LogChute is enabled for.
     */
    public boolean isLevelEnabled(int level)
    {
        return (level >= this.enabled);
    }

    /**
     * Send a log message from Velocity.
     */
    public void log(int level, String message)
    {
        if (!isLevelEnabled(level))
        {
            return;
        }

        switch (level)
        {
            case WARN_ID:
                servletContext.log(PREFIX + WARN_PREFIX + message);
                break;
            case INFO_ID:
                servletContext.log(PREFIX + INFO_PREFIX + message);
                break;
            case DEBUG_ID:
                servletContext.log(PREFIX + DEBUG_PREFIX + message);
                break;
            case TRACE_ID:
                servletContext.log(PREFIX + TRACE_PREFIX + message);
                break;
            case ERROR_ID:
                servletContext.log(PREFIX + ERROR_PREFIX + message);
                break;
            default:
                servletContext.log(PREFIX + " : " + message);
                break;
        }
    }

    public void log(int level, String message, Throwable t)
    {
        if (!isLevelEnabled(level))
        {
            return;
        }

        message += " - "+t.toString();
        if (level >= ERROR_ID)
        {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            message += "\n" + sw.toString();
        }

        log(level, message);
    }

}
