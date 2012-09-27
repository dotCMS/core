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

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;

/**
 * <p><em>This class is deprecated in favor of the new {@link Log4JLogChute},
 * which makes use of Log4J's <code>Logger</code> rather than its deprecated
 * <code>Category</code> class.</em></p>
 *
 *  Implementation of a simple log4j system that will either
 *  latch onto an existing category, or just do a simple
 *  rolling file log.  Derived from Jon's 'complicated'
 *  version :)
 *
 * @author <a href="mailto:geirm@apache.org>Geir Magnusson Jr.</a>
 * @version $Id: SimpleLog4JLogSystem.java 718424 2008-11-17 22:50:43Z nbubna $
 * @deprecated Use Log4JLogChute instead.
 */
public class SimpleLog4JLogSystem implements LogSystem
{
    private RuntimeServices rsvc = null;
    private RollingFileAppender appender = null;

    /** log4java logging interface */
    protected Category logger = null;

    /**
     *
     */
    public SimpleLog4JLogSystem()
    {
    }

    /**
     * @see org.apache.velocity.runtime.log.LogSystem#init(org.apache.velocity.runtime.RuntimeServices)
     */
    public void init( RuntimeServices rs )
    {
        rsvc = rs;

        /*
         *  first see if there is a category specified and just use that - it allows
         *  the application to make us use an existing logger
         */

        String categoryname =  (String) rsvc.getProperty("runtime.log.logsystem.log4j.category");

        if ( categoryname != null )
        {
            logger = Category.getInstance( categoryname );

            logVelocityMessage( 0,
                                "SimpleLog4JLogSystem using category '" + categoryname + "'");

            return;
        }

        /*
         *  if not, use the file...
         */

        String logfile = rsvc.getString( RuntimeConstants.RUNTIME_LOG );

        /*
         *  now init.  If we can't, panic!
         */
        try
        {
            internalInit( logfile );

            logVelocityMessage( 0,
                "SimpleLog4JLogSystem initialized using logfile '" + logfile + "'" );
        }
        catch( Exception e )
        {
            System.err.println(
                "PANIC : error configuring SimpleLog4JLogSystem : " + e );
        }
    }

    /**
     *  initializes the log system using the logfile argument
     */
    private void internalInit( String logfile )
        throws Exception
    {
        /*
         *  do it by our classname to avoid conflicting with anything else
         *  that might be used...
         */

        logger = Category.getInstance(this.getClass().getName());
        logger.setAdditivity(false);

        /*
         * Priority is set for DEBUG becouse this implementation checks
         * log level.
         */
        logger.setLevel(Level.DEBUG);

        appender = new RollingFileAppender( new PatternLayout( "%d - %m%n"), logfile, true);

        appender.setMaxBackupIndex( 1 );

        appender.setMaximumFileSize( 100000 );

        logger.addAppender(appender);
    }

    /**
     *  logs messages
     *
     *  @param level severity level
     *  @param message complete error message
     */
    public void logVelocityMessage(int level, String message)
    {
        switch (level)
        {
            case LogSystem.WARN_ID:
                logger.warn( message );
                break;
            case LogSystem.INFO_ID:
                logger.info(message);
                break;
            case LogSystem.ERROR_ID:
                logger.error(message);
                break;
            case LogSystem.DEBUG_ID:
            default:
                logger.debug(message);
                break;
        }
    }

    /**
     * Also do a shutdown if the object is destroy()'d.
     * @throws Throwable
     */
    protected void finalize() throws Throwable
    {
        shutdown();
    }

    /** Close all destinations*/
    public void shutdown()
    {
        if (appender != null)
        {
            logger.removeAppender(appender);
            appender.close();
            appender = null;
        }
    }
}
