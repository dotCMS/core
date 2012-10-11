package org.apache.velocity.app.event.implement;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.app.event.InvalidReferenceEventHandler;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.util.RuntimeServicesAware;
import org.apache.velocity.util.introspection.Info;

/**
 * Use this event handler to flag invalid references.  Since this 
 * is intended to be used for a specific request, this should be
 * used as a local event handler attached to a specific context
 * instead of being globally defined in the Velocity properties file.
 * 
 * <p>
 * Note that InvalidReferenceHandler can be used
 * in two modes.  If the Velocity properties file contains the following:
 * <pre>
 * eventhandler.invalidreference.exception = true
 * </pre>
 * then the event handler will throw a ParseErrorRuntimeException upon 
 * hitting the first invalid reference.  This stops processing and is 
 * passed through to the application code.  The ParseErrorRuntimeException
 * contain information about the template name, line number, column number,
 * and invalid reference.
 * 
 * <p>
 * If this configuration setting is false or omitted then the page 
 * will be processed as normal, but all invalid references will be collected
 * in a List of InvalidReferenceInfo objects.
 * 
 * <p>This feature should be regarded as experimental.
 * 
 * @author <a href="mailto:wglass@forio.com">Will Glass-Husain</a>
 * @version $Id: ReportInvalidReferences.java 685685 2008-08-13 21:43:27Z nbubna $
 * @since 1.5
 */
public class ReportInvalidReferences implements 
    InvalidReferenceEventHandler, RuntimeServicesAware
{

    public static final String EVENTHANDLER_INVALIDREFERENCE_EXCEPTION = "eventhandler.invalidreference.exception";
    
    /** 
     * List of InvalidReferenceInfo objects
     */
    List invalidReferences = new ArrayList();

    /**
     * If true, stop at the first invalid reference and throw an exception.
     */
    private boolean stopOnFirstInvalidReference = false;
    
       
    /**
     * Collect the error and/or throw an exception, depending on configuration.
     *
     * @param context the context when the reference was found invalid
     * @param reference string with complete invalid reference
     * @param object the object referred to, or null if not found
     * @param property the property name from the reference
     * @param info contains template, line, column details
     * @return always returns null
     * @throws ParseErrorException
     */
    public Object invalidGetMethod(Context context, String reference, Object object, 
            String property, Info info)
    {
        reportInvalidReference(reference, info);
        return null;
    }

    /**
     * Collect the error and/or throw an exception, depending on configuration.
     *
     * @param context the context when the reference was found invalid
     * @param reference complete invalid reference
     * @param object the object referred to, or null if not found
     * @param method the property name from the reference
     * @param info contains template, line, column details
     * @return always returns null
     * @throws ParseErrorException
     */
    public Object invalidMethod(Context context, String reference, Object object, 
            String method, Info info)
    {
        if (reference == null)
        {
            reportInvalidReference(object.getClass().getName() + "." + method, info);
        }
        else
        {
            reportInvalidReference(reference, info);
        }
        return null;
    }

    /**
     * Collect the error and/or throw an exception, depending on configuration.
     *
     * @param context the context when the reference was found invalid
     * @param leftreference left reference being assigned to
     * @param rightreference invalid reference on the right
     * @param info contains info on template, line, col
     * @return loop to end -- always returns false
     */
    public boolean invalidSetMethod(Context context, String leftreference, String rightreference, Info info)
    {
        reportInvalidReference(leftreference, info);
        return false;
    }


    /**
     * Check for an invalid reference and collect the error or throw an exception 
     * (depending on configuration).
     * 
     * @param reference the invalid reference
     * @param info line, column, template name
     */
    private void reportInvalidReference(String reference, Info info)
    {
        InvalidReferenceInfo invalidReferenceInfo = new InvalidReferenceInfo(reference, info);
        invalidReferences.add(invalidReferenceInfo);
        
        if (stopOnFirstInvalidReference)
        {
            throw new ParseErrorException(
                    "Error in page - invalid reference.  ",
                    info,
                    invalidReferenceInfo.getInvalidReference());
        }
    }


    /**
     * All invalid references during the processing of this page.
     * @return a List of InvalidReferenceInfo objects
     */
    public List getInvalidReferences()
    {
        return invalidReferences;
    }
    

    /**
     * Called automatically when event cartridge is initialized.
     * @param rs RuntimeServices object assigned during initialization
     */
    public void setRuntimeServices(RuntimeServices rs)
    {
        stopOnFirstInvalidReference = rs.getConfiguration().getBoolean(
                EVENTHANDLER_INVALIDREFERENCE_EXCEPTION,
                false);        
    }
    
}
