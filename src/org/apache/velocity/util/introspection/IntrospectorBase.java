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

/**
 * Lookup a a Method object for a particular class given the name of a method
 * and its parameters.
 *
 * The first time the Introspector sees a
 * class it creates a class method map for the
 * class in question. Basically the class method map
 * is a Hashtable where Method objects are keyed by a
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
 * and stored for.
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:bob@werken.com">Bob McWhirter</a>
 * @author <a href="mailto:szegedia@freemail.hu">Attila Szegedi</a>
 * @author <a href="mailto:paulo.gaspar@krankikom.de">Paulo Gaspar</a>
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id: IntrospectorBase.java 685685 2008-08-13 21:43:27Z nbubna $
 */
public abstract class IntrospectorBase
{

    /** The Introspector Cache */
    private final IntrospectorCache introspectorCache;
    
    /**
     * C'tor.
     */
    protected IntrospectorBase()
    {   
        introspectorCache = new IntrospectorCacheImpl(); // TODO: Load that from properties.
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
     * @throws MethodMap.AmbiguousException When the method map contains more than one match for the requested signature.
     */
    public Method getMethod(final Class c, final String name, final Object[] params)
            throws IllegalArgumentException,MethodMap.AmbiguousException
    {
        if (c == null)
        {
            throw new IllegalArgumentException ("class object is null!");
        }
        
        if (params == null)
        {
            throw new IllegalArgumentException("params object is null!");
        }

        IntrospectorCache ic = getIntrospectorCache();

        ClassMap classMap = ic.get(c);
        if (classMap == null)
        {
            classMap = ic.put(c);
        }

        return classMap.findMethod(name, params);
    }

    /**
     * Return the internal IntrospectorCache object.
     * 
     * @return The internal IntrospectorCache object.
     * @since 1.5
     */
    protected IntrospectorCache getIntrospectorCache()
    {
	    return introspectorCache;
    }

}
