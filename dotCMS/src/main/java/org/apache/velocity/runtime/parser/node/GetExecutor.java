package org.apache.velocity.runtime.parser.node;

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

import java.lang.reflect.InvocationTargetException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.util.introspection.Introspector;

import com.dotmarketing.util.Logger;


/**
 * Executor that simply tries to execute a get(key)
 * operation. This will try to find a get(key) method
 * for any type of object, not just objects that
 * implement the Map interface as was previously
 * the case.
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @version $Id: GetExecutor.java 687177 2008-08-19 22:00:32Z nbubna $
 */
public class GetExecutor extends AbstractExecutor
{
    private final Introspector introspector;

    // This is still threadsafe because this object is only read except in the C'tor.
    private Object [] params = {};

    /**
     * @param log
     * @param introspector
     * @param clazz
     * @param property
     * @since 1.5
     */
    public GetExecutor(final Introspector introspector,
            final Class clazz, final String property)
    {
        this.introspector = introspector;

        // If you passed in null as property, we don't use the value
        // for parameter lookup. Instead we just look for get() without
        // any parameters.
        //
        // In any other case, the following condition will set up an array
        // for looking up get(String) on the class.

        if (property != null)
        {
            this.params = new Object[] { property };
        }
        discover(clazz);
    }

    /**
     * @since 1.5
     */
    protected void discover(final Class clazz)
    {
        try
        {
            setMethod(introspector.getMethod(clazz, "get", params));
        }
        /**
         * pass through application level runtime exceptions
         */
        catch( RuntimeException e )
        {
            throw e;
        }
        catch(Exception e)
        {
            String msg = "Exception while looking for get('" + params[0] + "') method";
            Logger.error(this,msg, e);
            throw new VelocityException(msg, e);
        }
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.AbstractExecutor#execute(java.lang.Object)
     */
    public Object execute(final Object o)
        throws IllegalAccessException,  InvocationTargetException
    {
        return isAlive() ? getMethod().invoke(o, params) : null;
    }
}
