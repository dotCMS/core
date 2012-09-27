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

import java.lang.reflect.Method;

import org.apache.velocity.runtime.RuntimeLogger;
import org.apache.velocity.runtime.log.Log;
import org.apache.velocity.runtime.log.RuntimeLoggerLog;

/**
 * This basic function of this class is to return a Method
 * object for a particular class given the name of a method
 * and the parameters to the method in the form of an Object[]
 *
 * The first time the Introspector sees a
 * class it creates a class method map for the
 * class in question. Basically the class method map
 * is a Hastable where Method objects are keyed by a
 * concatenation of the method name and the names of
 * classes that make up the parameters.
 *
 * For example, a method with the following signature:
 *
 * public void method(String a, StringBuffer b)
 *
 * would be mapped by the key:
 *
 * "method" + "java.lang.String" + "java.lang.StringBuffer"
 *
 * This mapping is performed for all the methods in a class
 * and stored for
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:bob@werken.com">Bob McWhirter</a>
 * @author <a href="mailto:szegedia@freemail.hu">Attila Szegedi</a>
 * @author <a href="mailto:paulo.gaspar@krankikom.de">Paulo Gaspar</a>
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id: Introspector.java 687177 2008-08-19 22:00:32Z nbubna $
 */
public class Introspector extends IntrospectorBase
{
    /**
     * @param log A Log object to use for the introspector.
     * @since 1.5
     */
    public Introspector(final Log log)
    {
        super(log);
    }

    /**
     * @param logger A runtime logger object.
     * @deprecated RuntimeLogger is deprecated. Use Introspector(Log log).
     */
    public Introspector(final RuntimeLogger logger)
    {
        this(new RuntimeLoggerLog(logger));
    }

    /**
     * Gets the method defined by <code>name</code> and
     * <code>params</code> for the Class <code>c</code>.
     *
     * @param c Class in which the method search is taking place
     * @param name Name of the method being searched for
     * @param params An array of Objects (not Classes) that describe the
     *               the parameters
     *
     * @return The desired Method object.
     * @throws IllegalArgumentException When the parameters passed in can not be used for introspection.
     */
    public Method getMethod(final Class c, final String name, final Object[] params)
        throws IllegalArgumentException
    {
        try
        {
            return super.getMethod(c, name, params);
        }
        catch(MethodMap.AmbiguousException ae)
        {
            /*
             *  whoops.  Ambiguous.  Make a nice log message and return null...
             */

            StringBuffer msg = new StringBuffer("Introspection Error : Ambiguous method invocation ")
                    .append(name)
                    .append("(");

            for (int i = 0; i < params.length; i++)
            {
                if (i > 0)
                {
                    msg.append(", ");
                }

                if (params[i] == null)
                {
                    msg.append("null");
                }
                else
                {
                    msg.append(params[i].getClass().getName());
                }
            }

            msg.append(") for class ")
                    .append(c);

            log.debug(msg.toString());
        }

        return null;
    }

}
