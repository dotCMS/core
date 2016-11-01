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
 * Executor that simply tries to execute a put(key, value)
 * operation. This will try to find a put(key) method
 * for any type of object, not just objects that
 * implement the Map interface as was previously
 * the case.
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id: PutExecutor.java 687177 2008-08-19 22:00:32Z nbubna $
 * @since 1.5
 */
public class PutExecutor extends SetExecutor
{
    private final Introspector introspector;
    private final String property;

    /**
     * @param log
     * @param introspector
     * @param clazz
     * @param arg
     * @param property
     */
    public PutExecutor(final Introspector introspector,
            final Class clazz, final Object arg, final String property)
    {
        this.introspector = introspector;
        this.property = property;

        discover(clazz, arg);
    }

    /**
     * @param clazz
     * @param arg
     */
    protected void discover(final Class clazz, final Object arg)
    {
        Object [] params;

        // If you passed in null as property, we don't use the value
        // for parameter lookup. Instead we just look for put(Object) without
        // any parameters.
        //
        // In any other case, the following condition will set up an array
        // for looking up put(String, Object) on the class.

        if (property == null)
        {
            // The passed in arg object is used by the Cache to look up the method.
            params = new Object[] { arg };
        }
        else
        {
            params = new Object[] { property, arg };
        }

        try
        {
            setMethod(introspector.getMethod(clazz, "put", params));
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
            String msg = "Exception while looking for put('" + params[0] + "') method";
            Logger.error(this,msg, e);
            throw new VelocityException(msg, e);
        }
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.SetExecutor#execute(java.lang.Object, java.lang.Object)
     */
    public Object execute(final Object o, final Object value)
        throws IllegalAccessException,  InvocationTargetException
    {
        Object [] params;

        if (isAlive())
        {
            // If property != null, pass in the name for put(key, value). Else just put(value).
            if (property == null)
            {
                params = new Object [] { value };
            }
            else
            {
                params = new Object [] { property, value };
            }

            return getMethod().invoke(o, params);
        }

        return null;
    }
}
