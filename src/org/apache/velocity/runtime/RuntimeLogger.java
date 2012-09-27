package org.apache.velocity.runtime;

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
 * Interface for internal runtime logging services. This will hopefully
 * be dissolved into the Log class at some point soon.
 *
 * @author <a href="mailto:geirm@apache.org">Geir Magusson Jr.</a>
 * @version $Id: RuntimeLogger.java 463298 2006-10-12 16:10:32Z henning $
 * @deprecated This functionality has been taken over by the Log class
 */
public interface RuntimeLogger
{
    /**
     * @deprecated Use Log.warn(Object).
     * @see org.apache.velocity.runtime.log.Log#warn(Object)
     * @param message The message to log.
     */
    public void warn(Object message);

    /**
     * @deprecated Use Log.info(Object)
     * @see org.apache.velocity.runtime.log.Log#info(Object)
     * @param message The message to log.
     */
    public  void info(Object message);

    /**
     * @deprecated Use Log.error(Object)
     * @see org.apache.velocity.runtime.log.Log#error(Object)
     * @param message The message to log.
     */
    public void error(Object message);

    /**
     * @deprecated Use Log.debug(Object)
     * @see org.apache.velocity.runtime.log.Log#debug(Object)
     * @param message The message to log.
     */
    public void debug(Object message);
}
