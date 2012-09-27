package org.apache.velocity.util.introspection;

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

import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.util.RuntimeServicesAware;

/**
 * Use a custom introspector that prevents classloader related method 
 * calls.  Use this introspector for situations in which template 
 * writers are numerous or untrusted.  Specifically, this introspector 
 * prevents creation of arbitrary objects or reflection on objects.
 * 
 * <p>To use this introspector, set the following property:
 * <pre>
 * runtime.introspector.uberspect = org.apache.velocity.util.introspection.SecureUberspector
 * </pre>
 * 
 * @author <a href="mailto:wglass@forio.com">Will Glass-Husain</a>
 * @version $Id: SecureUberspector.java 774412 2009-05-13 15:54:07Z nbubna $
 * @since 1.5
 */
public class SecureUberspector extends UberspectImpl implements RuntimeServicesAware
{
    RuntimeServices runtimeServices;
    
    public SecureUberspector()
    {
        super();
    }

    /**
     *  init - generates the Introspector. As the setup code
     *  makes sure that the log gets set before this is called,
     *  we can initialize the Introspector using the log object.
     */
    public void init()
    {
        String [] badPackages = runtimeServices.getConfiguration()
                        .getStringArray(RuntimeConstants.INTROSPECTOR_RESTRICT_PACKAGES);

        String [] badClasses = runtimeServices.getConfiguration()
                        .getStringArray(RuntimeConstants.INTROSPECTOR_RESTRICT_CLASSES);
        
        introspector = new SecureIntrospectorImpl(badClasses, badPackages, log);
    }
    
    /**
     * Get an iterator from the given object.  Since the superclass method
     * this secure version checks for execute permission.
     * 
     * @param obj object to iterate over
     * @param i line, column, template info
     * @return Iterator for object
     */
    public Iterator getIterator(Object obj, Info i) throws Exception
    {
        if (obj != null)
        {
            SecureIntrospectorControl sic = (SecureIntrospectorControl)introspector;
            if (sic.checkObjectExecutePermission(obj.getClass(), null))
            {
                return super.getIterator(obj, i);
            }
            else
            {
                log.warn("Cannot retrieve iterator from " + obj.getClass() +
                         " due to security restrictions.");
            }
        }
        return null;
    }

    /**
     * Store the RuntimeServices before the object is initialized..
     * @param rs RuntimeServices object for initialization
     */
    public void setRuntimeServices(RuntimeServices rs)
    {
        this.runtimeServices = rs;
    }
    
    
}
