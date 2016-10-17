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

/**
 * The introspector cache API definition.
 *
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id: IntrospectorCache.java 685685 2008-08-13 21:43:27Z nbubna $
 * @since 1.5
 */
public interface IntrospectorCache {

    /**
     * Clears the internal cache.
     */
    void clear();

    /**
     * Lookup a given Class object in the cache. If it does not exist, 
     * check whether this is due to a class change and purge the caches
     * eventually.
     *
     * @param c The class to look up.
     * @return A ClassMap object or null if it does not exist in the cache.
     */
    ClassMap get(Class c);

    /**
     * Creates a class map for specific class and registers it in the
     * cache.  Also adds the qualified name to the name-&gt;class map
     * for later Classloader change detection.
     *
     * @param c The class for which the class map gets generated.
     * @return A ClassMap object.
     */
    ClassMap put(Class c);

}
