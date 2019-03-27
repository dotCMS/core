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

import org.apache.velocity.context.Context;
import org.apache.velocity.util.ContextAware;

/**
 *  Reference 'Stream insertion' event handler.  Called with object
 *  that will be inserted into stream via value.toString().
 *
 *  Please return an Object that will toString() nicely :)
 *
 * @author <a href="mailto:wglass@forio.com">Will Glass-Husain</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: ReferenceInsertionEventHandler.java 685685 2008-08-13 21:43:27Z nbubna $
 */
public interface  ReferenceInsertionEventHandler extends EventHandler
{
    /**
     * A call-back which is executed during Velocity merge before a reference
     * value is inserted into the output stream. All registered
     * ReferenceInsertionEventHandlers are called in sequence. If no
     * ReferenceInsertionEventHandlers are are registered then reference value
     * is inserted into the output stream as is.
     *
     * @param reference Reference from template about to be inserted.
     * @param value Value about to be inserted (after its <code>toString()</code>
     *            method is called).
     * @return Object on which <code>toString()</code> should be called for
     *         output.
     */
    public Object referenceInsert( String reference, Object value  );

    /**
     * Defines the execution strategy for referenceInsert
     * @since 1.5
     */
    static class referenceInsertExecutor implements EventHandlerMethodExecutor
    {
        private Context context;
        private String reference;
        private Object value;

        referenceInsertExecutor(
                Context context, 
                String reference, 
                Object value)
        {
            this.context = context;
            this.reference = reference;
            this.value = value;
        }

        /**
         * Call the method referenceInsert()
         *  
         * @param handler call the appropriate method on this handler
         */
        public void execute(EventHandler handler)
        {
            ReferenceInsertionEventHandler eh = (ReferenceInsertionEventHandler) handler;
            
            if (eh instanceof ContextAware)
                ((ContextAware) eh).setContext(context);

            /**
             * Every successive call will alter the same value
             */
            value = ((ReferenceInsertionEventHandler) handler).referenceInsert(reference, value); 
        }

        public Object getReturnValue()
        {
            return value;
        }

        /**
         * Continue to end of event handler iteration
         * 
         * @return always returns false
         */
        public boolean isDone()
        {
            return false;
        }        
    }

}
