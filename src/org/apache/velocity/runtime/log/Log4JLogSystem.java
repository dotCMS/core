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

/**
 * Implementation of a simple log4j system that will either latch onto
 * an existing category, or just do a simple rolling file log.
 *
 * Use this one rather than {@link SimpleLog4JLogSystem}; it uses the
 * modern <code>Logger</code> concept of Log4J, rather than the
 * deprecated <code>Categeory</code> concept.
 *
 * @author <a href="mailto:geirm@apache.org>Geir Magnusson Jr.</a>
 * @author <a href="mailto:dlr@finemaltcoding.com>Daniel L. Rall</a>
 * @author <a href="mailto:nbubna@apache.org>Nathan Bubna</a>
 * @version $Id: Log4JLogSystem.java 463298 2006-10-12 16:10:32Z henning $
 * @deprecated Use Log4JLogChute instead.
 * @since Velocity 1.5
 */
public class Log4JLogSystem extends Log4JLogChute implements LogSystem
{
    /**
     *  @param level
     * @param message
     * @deprecated Use log(level, message).
     */
    public void logVelocityMessage(int level, String message)
    {
        log(level, message);
    }
}
