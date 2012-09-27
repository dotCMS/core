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

import org.apache.velocity.runtime.RuntimeLogger;

/**
 * A temporary RuntimeLogger wrapper to make the deprecation
 * of UberspectLoggable.setRuntimeLogger(RuntimeLogger) feasible.
 * This overrides all Log methods, either throwing
 * UnsupportedOperationExceptions or passing things off to the
 * theoretical RuntimeLogger used to create it.  Oh, and all the
 * is<Level>Enabled() methods return true.  Of course, ideally
 * there is no one out there who actually created their own
 * RuntimeLogger instance to use with UberspectLoggable.setRuntimeLogger()
 * and this class will therefore never be used.  But it's here just in case.
 *
 * @author <a href="mailto:nbubna@apache.org">Nathan Bubna</a>
 * @version $Id: RuntimeLoggerLog.java 685685 2008-08-13 21:43:27Z nbubna $
 * @deprecated This will be removed along with the RuntimeLogger interface.
 * @since 1.5
 */
public class RuntimeLoggerLog extends Log
{

    private RuntimeLogger rlog;

    /**
     * Creates a new Log that wraps a PrimordialLogChute.
     * @param rlog
     */
    public RuntimeLoggerLog(RuntimeLogger rlog)
    {
        if (rlog == null)
        {
            throw new NullPointerException("RuntimeLogger cannot be null!");
        }
        this.rlog = rlog;
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#setLogChute(org.apache.velocity.runtime.log.LogChute)
     */
    protected void setLogChute(LogChute newLogChute)
    {
        throw new UnsupportedOperationException("RuntimeLoggerLog does not support this method.");
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#getLogChute()
     */
    protected LogChute getLogChute()
    {
        throw new UnsupportedOperationException("RuntimeLoggerLog does not support this method.");
    }

    /**
     * @param showStacks
     */
    protected void setShowStackTraces(boolean showStacks)
    {
        throw new UnsupportedOperationException("RuntimeLoggerLog does not support this method.");
    }

    /**
     * @return True if Stack traces should be shown.
     */
    public boolean getShowStackTraces()
    {
        throw new UnsupportedOperationException("RuntimeLoggerLog does not support this method.");
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#isTraceEnabled()
     */
    public boolean isTraceEnabled()
    {
        return true;
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#trace(java.lang.Object)
     */
    public void trace(Object message)
    {
        debug(message);
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#trace(java.lang.Object, java.lang.Throwable)
     */
    public void trace(Object message, Throwable t)
    {
        debug(message, t);
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#isDebugEnabled()
     */
    public boolean isDebugEnabled()
    {
        return true;
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#debug(java.lang.Object)
     */
    public void debug(Object message)
    {
        rlog.debug(message);
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#debug(java.lang.Object, java.lang.Throwable)
     */
    public void debug(Object message, Throwable t)
    {
        rlog.debug(message);
        rlog.debug(t);
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#isInfoEnabled()
     */
    public boolean isInfoEnabled()
    {
        return true;
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#info(java.lang.Object)
     */
    public void info(Object message)
    {
        rlog.info(message);
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#info(java.lang.Object, java.lang.Throwable)
     */
    public void info(Object message, Throwable t)
    {
        rlog.info(message);
        rlog.info(t);
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#isWarnEnabled()
     */
    public boolean isWarnEnabled()
    {
        return true;
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#warn(java.lang.Object)
     */
    public void warn(Object message)
    {
        rlog.warn(message);
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#warn(java.lang.Object, java.lang.Throwable)
     */
    public void warn(Object message, Throwable t)
    {
        rlog.warn(message);
        rlog.warn(t);
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#isErrorEnabled()
     */
    public boolean isErrorEnabled()
    {
        return true;
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#error(java.lang.Object)
     */
    public void error(Object message)
    {
        rlog.error(message);
    }

    /**
     * @see org.apache.velocity.runtime.log.Log#error(java.lang.Object, java.lang.Throwable)
     */
    public void error(Object message, Throwable t)
    {
        rlog.error(message);
        rlog.error(t);
    }

}
