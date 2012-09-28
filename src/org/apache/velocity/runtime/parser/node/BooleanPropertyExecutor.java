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

import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.util.introspection.Introspector;

import com.dotmarketing.util.Logger;

/**
 *  Handles discovery and valuation of a
 *  boolean object property, of the
 *  form public boolean is<property> when executed.
 *
 *  We do this separately as to preserve the current
 *  quasi-broken semantics of get<as is property>
 *  get< flip 1st char> get("property") and now followed
 *  by is<Property>
 *
 *  @author <a href="geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: BooleanPropertyExecutor.java 687502 2008-08-20 23:19:52Z nbubna $
 */
public class BooleanPropertyExecutor extends PropertyExecutor
{
    /**
     * @param log
     * @param introspector
     * @param clazz
     * @param property
     * @since 1.5
     */
    public BooleanPropertyExecutor(final Introspector introspector,
            final Class clazz, final String property)
    {
        super(introspector, clazz, property);
    }


    protected void discover(final Class clazz, final String property)
    {
        try
        {
            Object [] params = {};

            StringBuffer sb = new StringBuffer("is");
            sb.append(property);

            setMethod(getIntrospector().getMethod(clazz, sb.toString(), params));

            if (!isAlive())
            {
                /*
                 *  now the convenience, flip the 1st character
                 */

                char c = sb.charAt(2);

                if (Character.isLowerCase(c))
                {
                    sb.setCharAt(2, Character.toUpperCase(c));
                }
                else
                {
                    sb.setCharAt(2, Character.toLowerCase(c));
                }

                setMethod(getIntrospector().getMethod(clazz, sb.toString(), params));
            }
            
            if (isAlive())
            {
                if( getMethod().getReturnType() != Boolean.TYPE &&
                    getMethod().getReturnType() != Boolean.class )
                {
                    setMethod(null);
                }
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
            String msg = "Exception while looking for boolean property getter for '" + property;
            Logger.error(this,msg, e);
            throw new VelocityException(msg, e);
        }
    }
}
