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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.velocity.runtime.log.Log;

/**
 * This is the internal introspector cache implementation.
 *
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id: IntrospectorCacheImpl.java 898032 2010-01-11 19:51:03Z nbubna $
 * @since 1.5
 */
public final class IntrospectorCacheImpl implements IntrospectorCache
{
    /**
     * define a public string so that it can be looked for if interested
     */
    public final static String CACHEDUMP_MSG =
        "IntrospectorCache detected classloader change. Dumping cache.";

    /** Class logger */
    private final Log log;
    
    /**
     * Holds the method maps for the classes we know about. Map: Class --&gt; ClassMap object.
     */
    private final Map classMapCache = new HashMap();

    /**
     * Keep the names of the classes in another map. This is needed for a multi-classloader environment where it is possible
     * to have Class 'Foo' loaded by a classloader and then get asked to introspect on 'Foo' from another class loader. While these
     * two Class objects have the same name, a <code>classMethodMaps.get(Foo.class)</code> will return null. For that case, we
     * keep a set of class names to recognize this case.  
     */
    private final Set classNameCache = new HashSet();

    /**
     * C'tor
     */
    public IntrospectorCacheImpl(final Log log)
    {
	    this.log = log;
    }

    /**
     * Clears the internal cache.
     */
    public void clear()
    {
        synchronized (classMapCache)
        {
            classMapCache.clear();
            classNameCache.clear();
            log.debug(CACHEDUMP_MSG);
        }
    }

    /**
     * Lookup a given Class object in the cache. If it does not exist, 
     * check whether this is due to a class change and purge the caches
     * eventually.
     *
     * @param c The class to look up.
     * @return A ClassMap object or null if it does not exist in the cache.
     */
    public ClassMap get(final Class c)
    {
        if (c == null)
        {
            throw new IllegalArgumentException("class is null!");
        }

        ClassMap classMap = (ClassMap)classMapCache.get(c);
        if (classMap == null)
        {
            /*
             * check to see if we have it by name.
             * if so, then we have an object with the same
             * name but loaded through a different class loader.
             * In that case, we will just dump the cache to be sure.
             */
            synchronized (classMapCache)
            {
                if (classNameCache.contains(c.getName()))
                {
                    clear();
                }
            }
        }
        return classMap;
    }

    /**
     * Creates a class map for specific class and registers it in the
     * cache.  Also adds the qualified name to the name-&gt;class map
     * for later Classloader change detection.
     *
     * @param c The class for which the class map gets generated.
     * @return A ClassMap object.
     */
    public ClassMap put(final Class c)
    {
        final ClassMap classMap = new ClassMap(c, log);
        synchronized (classMapCache)
        {
            classMapCache.put(c, classMap);
            classNameCache.add(c.getName());
        }
        return classMap;
    }

}
