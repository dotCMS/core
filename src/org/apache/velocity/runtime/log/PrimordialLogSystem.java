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
 *  Pre-init logger.  I believe that this was suggested by
 *  Carsten Ziegeler <cziegeler@sundn.de> and
 *  Jeroen C. van Gelderen.  If this isn't correct, let me
 *  know as this was a good idea...
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: PrimordialLogSystem.java 463298 2006-10-12 16:10:32Z henning $
 * @deprecated Use HoldingLogChute instead!
 */
public class PrimordialLogSystem extends HoldingLogChute implements LogSystem
{
    /**
     * @param level
     * @param message
     * @deprecated Use log(level, message).
     */
    public void logVelocityMessage(int level, String message)
    {
        log(level, message);
    }

    /**
     * @param newLogger
     * @deprecated use transferTo(LogChute newChute)
     */
    public void dumpLogMessages( LogSystem newLogger )
    {
        transferTo(new LogChuteSystem(newLogger));
    }
}
