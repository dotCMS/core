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

import java.util.Iterator;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.util.ExceptionUtils;
import org.apache.velocity.util.introspection.Info;


/**
 * Calls on request all registered event handlers for a particular event. Each
 * method accepts two event cartridges (typically one from the application and
 * one from the context). All appropriate event handlers are executed in order
 * until a stopping condition is met. See the docs for the individual methods to
 * see what the stopping condition is for that method.
 *
 * @author <a href="mailto:wglass@wglass@forio.com">Will Glass-Husain </a>
 * @version $Id: EventHandlerUtil.java 685685 2008-08-13 21:43:27Z nbubna $
 * @since 1.5
 */
public class EventHandlerUtil {
    
    
    /**
     * Called before a reference is inserted. All event handlers are called in
     * sequence. The default implementation inserts the reference as is.
     * 
     * This is a major hotspot method called by ASTReference render.
     *
     * @param reference reference from template about to be inserted
     * @param value value about to be inserted (after toString() )
     * @param rsvc current instance of RuntimeServices
     * @param context The internal context adapter.
     * @return Object on which toString() should be called for output.
     */
    public static Object referenceInsert(RuntimeServices rsvc,
            InternalContextAdapter context, String reference, Object value)
    {
        // app level cartridges have already been initialized
        
        /*
         * Performance modification: EventCartridge.getReferenceInsertionEventHandlers
         * now returns a null if there are no handlers. Thus we can avoid creating the
         * Iterator object.
         */
        EventCartridge ev1 = rsvc.getApplicationEventCartridge();
        Iterator applicationEventHandlerIterator = 
            (ev1 == null) ? null: ev1.getReferenceInsertionEventHandlers();              
        
        EventCartridge ev2 = context.getEventCartridge();
        initializeEventCartridge(rsvc, ev2);
        Iterator contextEventHandlerIterator = 
            (ev2 == null) ? null: ev2.getReferenceInsertionEventHandlers();              
        
        try 
        {
            /*
             * Performance modification: methodExecutor is created only if one of the
             * iterators is not null.
             */
            
            EventHandlerMethodExecutor methodExecutor = null; 

            if( applicationEventHandlerIterator != null )
            {
                methodExecutor = 
                    new ReferenceInsertionEventHandler.referenceInsertExecutor(context, reference, value);
                iterateOverEventHandlers(applicationEventHandlerIterator, methodExecutor);
            }

            if( contextEventHandlerIterator != null )
            {
                if( methodExecutor == null )
                    methodExecutor = 
                        new ReferenceInsertionEventHandler.referenceInsertExecutor(context, reference, value);
                    
                iterateOverEventHandlers(contextEventHandlerIterator, methodExecutor);
            }

            
            return methodExecutor != null ? methodExecutor.getReturnValue() : value;   
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw ExceptionUtils.createRuntimeException("Exception in event handler.",e);
        }
    }

    /**
     * Called when a null is evaluated during a #set. All event handlers are
     * called in sequence until a false is returned. The default implementation
     * always returns true.
     *
     * @param lhs Left hand side of the expression.
     * @param rhs Right hand side of the expression.
     * @param rsvc current instance of RuntimeServices
     * @param context The internal context adapter.
     * @return true if to be logged, false otherwise
     */
    public static boolean shouldLogOnNullSet(RuntimeServices rsvc,
            InternalContextAdapter context, String lhs, String rhs)
    {
        // app level cartridges have already been initialized
        EventCartridge ev1 = rsvc.getApplicationEventCartridge();
        Iterator applicationEventHandlerIterator = 
            (ev1 == null) ? null: ev1.getNullSetEventHandlers();              
        
        EventCartridge ev2 = context.getEventCartridge();
        initializeEventCartridge(rsvc, ev2);
        Iterator contextEventHandlerIterator = 
            (ev2 == null) ? null: ev2.getNullSetEventHandlers();              
                
        try 
        {
            EventHandlerMethodExecutor methodExecutor = 
                new NullSetEventHandler.ShouldLogOnNullSetExecutor(context, lhs, rhs);

            callEventHandlers(
                    applicationEventHandlerIterator, 
                    contextEventHandlerIterator, methodExecutor);
            
            return ((Boolean) methodExecutor.getReturnValue()).booleanValue();    
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw ExceptionUtils.createRuntimeException("Exception in event handler.",e);
        }
    }
    
    /**
     * Called when a method exception is generated during Velocity merge. Only
     * the first valid event handler in the sequence is called. The default
     * implementation simply rethrows the exception.
     *
     * @param claz
     *            Class that is causing the exception
     * @param method
     *            method called that causes the exception
     * @param e
     *            Exception thrown by the method
     * @param rsvc current instance of RuntimeServices
     * @param context The internal context adapter.
     * @return Object to return as method result
     * @throws Exception
     *             to be wrapped and propogated to app
     */
    public static Object methodException(RuntimeServices rsvc,
            InternalContextAdapter context, Class claz, String method,
            Exception e) throws Exception 
    {
        // app level cartridges have already been initialized
        EventCartridge ev1 = rsvc.getApplicationEventCartridge();
        Iterator applicationEventHandlerIterator = 
            (ev1 == null) ? null: ev1.getMethodExceptionEventHandlers();              
        
        EventCartridge ev2 = context.getEventCartridge();
        initializeEventCartridge(rsvc, ev2);
        Iterator contextEventHandlerIterator = 
            (ev2 == null) ? null: ev2.getMethodExceptionEventHandlers();              
        
        EventHandlerMethodExecutor methodExecutor = 
            new MethodExceptionEventHandler.MethodExceptionExecutor(context, claz, method, e);
        
        if ( ((applicationEventHandlerIterator == null) || !applicationEventHandlerIterator.hasNext()) &&
                ((contextEventHandlerIterator == null) || !contextEventHandlerIterator.hasNext()) )
        {
            throw e;
        }
            
        callEventHandlers(
                applicationEventHandlerIterator, 
                contextEventHandlerIterator, methodExecutor);
        
        return methodExecutor.getReturnValue();
    }
    
    /**
     * Called when an include-type directive is encountered (#include or
     * #parse). All the registered event handlers are called unless null is
     * returned. The default implementation always processes the included
     * resource.
     *
     * @param includeResourcePath
     *            the path as given in the include directive.
     * @param currentResourcePath
     *            the path of the currently rendering template that includes the
     *            include directive.
     * @param directiveName
     *            name of the directive used to include the resource. (With the
     *            standard directives this is either "parse" or "include").
     * @param rsvc current instance of RuntimeServices
     * @param context The internal context adapter.
     *
     * @return a new resource path for the directive, or null to block the
     *         include from occurring.
     */
    public static String includeEvent(RuntimeServices rsvc,
            InternalContextAdapter context, String includeResourcePath,
            String currentResourcePath, String directiveName)
    {
        // app level cartridges have already been initialized
        EventCartridge ev1 = rsvc.getApplicationEventCartridge();
        Iterator applicationEventHandlerIterator = 
            (ev1 == null) ? null: ev1.getIncludeEventHandlers();              
        
        EventCartridge ev2 = context.getEventCartridge();
        initializeEventCartridge(rsvc, ev2);
        Iterator contextEventHandlerIterator = 
            (ev2 == null) ? null: ev2.getIncludeEventHandlers();              
        
        try 
        {
            EventHandlerMethodExecutor methodExecutor = 
                new IncludeEventHandler.IncludeEventExecutor(
                        context, includeResourcePath, 
                        currentResourcePath, directiveName);
            
            callEventHandlers(
                    applicationEventHandlerIterator, 
                    contextEventHandlerIterator, methodExecutor);
            
            return (String) methodExecutor.getReturnValue();
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw ExceptionUtils.createRuntimeException("Exception in event handler.",e);
        }
    }
   

    /**
     * Called when an invalid get method is encountered.
     * 
     * @param rsvc current instance of RuntimeServices
     * @param context the context when the reference was found invalid
     * @param reference complete invalid reference
     * @param object object from reference, or null if not available
     * @param property name of property, or null if not relevant
     * @param info contains info on template, line, col
     * @return substitute return value for missing reference, or null if no substitute
     */
    public static Object invalidGetMethod(RuntimeServices rsvc,
            InternalContextAdapter context, String reference, 
            Object object, String property, Info info)
    {
        return  
        invalidReferenceHandlerCall (
                new InvalidReferenceEventHandler.InvalidGetMethodExecutor
                (context, reference, object, property, info),
                rsvc, 
                context);       
    }
        
        
   /**
     * Called when an invalid set method is encountered.
     * 
     * @param rsvc current instance of RuntimeServices
     * @param context the context when the reference was found invalid
     * @param leftreference left reference being assigned to
     * @param rightreference invalid reference on the right
     * @param info contains info on template, line, col
     */
    public static void invalidSetMethod(RuntimeServices rsvc,
            InternalContextAdapter context, String leftreference, 
            String rightreference, Info info)
    {
        /**
         * ignore return value
         */
        invalidReferenceHandlerCall (
                new InvalidReferenceEventHandler.InvalidSetMethodExecutor
                (context, leftreference, rightreference, info),
                rsvc, 
                context);   
    }
    
    /**
     * Called when an invalid method is encountered.
     * 
     * @param rsvc current instance of RuntimeServices
     * @param context the context when the reference was found invalid
     * @param reference complete invalid reference
     * @param object object from reference, or null if not available
     * @param method name of method, or null if not relevant
     * @param info contains info on template, line, col
     * @return substitute return value for missing reference, or null if no substitute
     */
    public static Object invalidMethod(RuntimeServices rsvc,
            InternalContextAdapter context,  String reference,
            Object object, String method, Info info)
    {
        return 
        invalidReferenceHandlerCall (
                new InvalidReferenceEventHandler.InvalidMethodExecutor
                (context, reference, object, method, info),
                rsvc, 
                context);       
    }
    
    
    /**
     * Calls event handler method with appropriate chaining across event handlers.
     * 
     * @param methodExecutor
     * @param rsvc current instance of RuntimeServices
     * @param context The current context
     * @return return value from method, or null if no return value
     */
    public static Object invalidReferenceHandlerCall(
            EventHandlerMethodExecutor methodExecutor, 
            RuntimeServices rsvc,
            InternalContextAdapter context)
    {
        // app level cartridges have already been initialized
        EventCartridge ev1 = rsvc.getApplicationEventCartridge();
        Iterator applicationEventHandlerIterator = 
            (ev1 == null) ? null: ev1.getInvalidReferenceEventHandlers();              
        
        EventCartridge ev2 = context.getEventCartridge();
        initializeEventCartridge(rsvc, ev2);
        Iterator contextEventHandlerIterator = 
            (ev2 == null) ? null: ev2.getInvalidReferenceEventHandlers();              
        
        try
        {
            callEventHandlers(
                    applicationEventHandlerIterator, 
                    contextEventHandlerIterator, methodExecutor);
            
            return methodExecutor.getReturnValue();
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw ExceptionUtils.createRuntimeException("Exception in event handler.",e);
        }
        
    }

    /**
     * Initialize the event cartridge if appropriate.
     * 
     * @param rsvc current instance of RuntimeServices
     * @param eventCartridge the event cartridge to be initialized
     */
    private static void initializeEventCartridge(RuntimeServices rsvc, EventCartridge eventCartridge)
    {
        if (eventCartridge != null)
        {
            try
            {
                eventCartridge.initialize(rsvc);
            }
            catch (Exception e)
            {
                throw ExceptionUtils.createRuntimeException("Couldn't initialize event cartridge : ", e);
            }
        }
    }
    
    
    /**
     * Loop through both the application level and context-attached event handlers.
     * 
     * @param applicationEventHandlerIterator Iterator that loops through all global event handlers declared at application level
     * @param contextEventHandlerIterator Iterator that loops through all global event handlers attached to context
     * @param eventExecutor Strategy object that executes event handler method
     * @exception Exception generic exception potentially thrown by event handlers
     */
    private static void callEventHandlers(
            Iterator applicationEventHandlerIterator, 
            Iterator contextEventHandlerIterator,
            EventHandlerMethodExecutor eventExecutor)
    throws Exception
    {
        /**
         * First loop through the event handlers configured at the app level
         * in the properties file.
         */
        iterateOverEventHandlers(applicationEventHandlerIterator, eventExecutor);
        
        /**
         * Then loop through the event handlers attached to the context.
         */
        iterateOverEventHandlers(contextEventHandlerIterator, eventExecutor);
    }
    
    /**
     * Loop through a given iterator of event handlers.
     * 
     * @param handlerIterator Iterator that loops through event handlers
     * @param eventExecutor Strategy object that executes event handler method
     * @exception Exception generic exception potentially thrown by event handlers
     */
    private static void iterateOverEventHandlers(
            Iterator handlerIterator,
            EventHandlerMethodExecutor eventExecutor)
    throws Exception
    {
        if (handlerIterator != null)
        {
            for (Iterator i = handlerIterator; i.hasNext();)
            {
                EventHandler eventHandler = (EventHandler) i.next();
                
                if (!eventExecutor.isDone())
                {
                    eventExecutor.execute(eventHandler);
                }
            }            
        }
    }
    
}
