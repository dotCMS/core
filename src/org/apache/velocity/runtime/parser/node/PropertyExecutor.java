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

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.util.introspection.Introspector;

import com.dotmarketing.util.Logger;

/**
 * Returned the value of object property when executed.
 */
public class PropertyExecutor extends AbstractExecutor
{
    private final Introspector introspector;

    /**
     * @param log
     * @param introspector
     * @param clazz
     * @param property
     * @since 1.5
     */
    public PropertyExecutor(final Introspector introspector,
            final Class clazz, final String property)
    {
        
        this.introspector = introspector;

        // Don't allow passing in the empty string or null because
        // it will either fail with a StringIndexOutOfBounds error
        // or the introspector will get confused.
        if (StringUtils.isNotEmpty(property))
        {
            discover(clazz, property);
        }
    }

    /**
     * @return The current introspector.
     * @since 1.5
     */
    protected Introspector getIntrospector()
    {
        return this.introspector;
    }

    /**
     * @param clazz
     * @param property
     */
    protected void discover(final Class clazz, final String property)
    {
        /*
         *  this is gross and linear, but it keeps it straightforward.
         */

        try
        {
            Object [] params = {};

            StringBuffer sb = new StringBuffer("get");
            sb.append(property);

            setMethod(introspector.getMethod(clazz, sb.toString(), params));

            if (!isAlive())
            {
                /*
                 *  now the convenience, flip the 1st character
                 */

                char c = sb.charAt(3);

                if (Character.isLowerCase(c))
                {
                    sb.setCharAt(3, Character.toUpperCase(c));
                }
                else
                {
                    sb.setCharAt(3, Character.toLowerCase(c));
                }

                setMethod(introspector.getMethod(clazz, sb.toString(), params));
            }
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
            String msg = "Exception while looking for property getter for '" + property;
            Logger.error(this,msg, e);
            throw new VelocityException(msg, e);
        }
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.AbstractExecutor#execute(java.lang.Object)
     */
    public Object execute(Object o)
        throws IllegalAccessException,  InvocationTargetException
    {
        return isAlive() ? getMethod().invoke(o, ((Object []) null)) : null;
    }
}
