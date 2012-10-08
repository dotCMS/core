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
import org.apache.commons.lang.text.StrBuilder;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.util.introspection.Introspector;

import com.dotmarketing.util.Logger;

/**
 * Executor for looking up property names in the passed in class
 * This will try to find a set&lt;foo&gt;(key, value) method
 *
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id: SetPropertyExecutor.java 687177 2008-08-19 22:00:32Z nbubna $
 * @since 1.5
 */
public class SetPropertyExecutor
        extends SetExecutor
{
    private final Introspector introspector;

    /**
     * @param log
     * @param introspector
     * @param clazz
     * @param property
     * @param arg
     */
    public SetPropertyExecutor(final Introspector introspector,
            final Class clazz, final String property, final Object arg)
    {
        this.introspector = introspector;

        // Don't allow passing in the empty string or null because
        // it will either fail with a StringIndexOutOfBounds error
        // or the introspector will get confused.
        if (StringUtils.isNotEmpty(property))
        {
            discover(clazz, property, arg);
        }
    }

    /**
     * @return The current introspector.
     */
    protected Introspector getIntrospector()
    {
        return this.introspector;
    }

    /**
     * @param clazz
     * @param property
     * @param arg
     */
    protected void discover(final Class clazz, final String property, final Object arg)
    {
        Object [] params = new Object [] { arg };

        try
        {
            StrBuilder sb = new StrBuilder("set");
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
            String msg = "Exception while looking for property setter for '" + property;
            Logger.error(this,msg, e);
            throw new VelocityException(msg, e);
        }
    }

    /**
     * Execute method against context.
     * @param o
     * @param value
     * @return The value of the invocation.
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public Object execute(final Object o, final Object value)
        throws IllegalAccessException,  InvocationTargetException
    {
        Object [] params = new Object [] { value };
        return isAlive() ? getMethod().invoke(o, params) : null;
    }
}
