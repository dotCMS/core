package org.apache.velocity.app.event;

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
 * Strategy object used to execute event handler method.  Will be called
 * while looping through all the chained event handler implementations.
 * Each EventHandler method call should have a parallel executor object
 * defined.  
 *
 * @author <a href="mailto:wglass@forio.com">Will Glass-Husain</a>
 * @version $Id: EventHandlerMethodExecutor.java 685685 2008-08-13 21:43:27Z nbubna $
 * @since 1.5
 */
public interface EventHandlerMethodExecutor
{
    /**
     * Execute the event handler method.  If Object is not null, do not 
     * iterate further through the handler chain.
     * If appropriate, the returned Object will be the return value.
     *  
     * @param handler call the appropriate method on this handler
     * @exception Exception generic exception potentially thrown by event handlers
     */
    public void execute(EventHandler handler) throws Exception;

    /**
     * Called after execute() to see if iterating should stop. Should
     * always return false before method execute() is run.
     * 
     * @return true if no more event handlers for this method should be called.
     */
    public boolean isDone();

    /**
     * Get return value at end of all the iterations
     * 
     * @return null if no return value is required
     */
    public Object getReturnValue();
}
