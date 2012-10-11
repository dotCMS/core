package org.apache.velocity.util;

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

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Factory class for creating Maps.
 * 
 * The main purpose of this class is to take advantage of Java 5
 * Concurrent classes if they are available. We use reflection to instantiate
 * java.util.concurrent classes to avoid compile time dependency on Java 5.
 * 
 * See <a href="http://issues.apache.org/jira/browse/VELOCITY-607">Issue 607</a>
 * for more info on this class.
 * @author <a href="mailto:wyla@sci.fi">Jarkko Viinamaki</a>
 * @since 1.6
 */
public class MapFactory
{
    private static Constructor concurrentHashMapConstructor;
    static
    {
        try
        {
            concurrentHashMapConstructor =
                Class.forName("java.util.concurrent.ConcurrentHashMap")
                     .getConstructor(new Class[] { int.class, float.class, int.class } );
        }
        catch (Exception ex)
        {
            // not running under JRE 1.5+
        }
    }

    /**
     * Creates a new instance of a class that implements Map interface
     * using the JDK defaults for initial size, load factor, etc.
     * 
     * Note that there is a small performance penalty because concurrent
     * maps are created using reflection.
     * 
     * @param allowNullKeys if true, the returned Map instance supports null keys         
     * @return one of ConcurrentHashMap, HashMap, Hashtable
     */
    public static Map create(boolean allowNullKeys)
    {
        return create(16, 0.75f, 16, allowNullKeys);
    }

    /**
     * Creates a new instance of a class that implements Map interface.
     * 
     * Note that there is a small performance penalty because concurrent
     * maps are created using reflection.
     * 
     * @param size initial size of the map
     * @param loadFactor smaller value = better performance, 
     *          larger value = better memory utilization
     * @param concurrencyLevel estimated number of writer Threads. 
     *          If this is smaller than 1, HashMap is always returned which is not 
     *          threadsafe.
     * @param allowNullKeys if true, the returned Map instance supports null keys         
     *          
     * @return one of ConcurrentHashMap, HashMap, Hashtable
     */
    public static Map create(int size, float loadFactor,
                             int concurrencyLevel, boolean allowNullKeys)
    {
        Map map = null;
        if (concurrencyLevel <= 1)
        {
            map = new HashMap(size, loadFactor);
        }
        else
        {
            if (concurrentHashMapConstructor != null)
            {
                // running under JRE 1.5+
                try
                {
                    map = (Map)concurrentHashMapConstructor.newInstance(
                        new Object[] { new Integer(size), new Float(loadFactor), new Integer(concurrencyLevel) });
                }
                catch (Exception ex)
                {
                    throw new RuntimeException("this should not happen", ex);
                }
            }
            else
            {
                /*
                 * Hashtable should be faster than
                 * Collections.synchronizedMap(new HashMap());
                 * so favor it if there is no need for null key support
                 */
                if (allowNullKeys)
                {
                    map = Collections.synchronizedMap(new HashMap(size, loadFactor));
                }
                else
                {
                    map = new Hashtable(size, loadFactor);
                }
            }
        }
        return map;
    }
}
