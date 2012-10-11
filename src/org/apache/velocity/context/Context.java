package org.apache.velocity.context;

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
 *  Interface describing the application data context.  This set of
 *  routines is used by the application to set and remove 'named' data
 *  object to pass them to the template engine to use when rendering
 *  a template.
 *
 *  This is the same set of methods supported by the original Context
 *  class
 *
 *  @see org.apache.velocity.context.AbstractContext
 *  @see org.apache.velocity.VelocityContext
 *
 *  @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 *  @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 *  @version $Id: Context.java 463298 2006-10-12 16:10:32Z henning $
 */
public interface Context
{
    /**
     * Adds a name/value pair to the context.
     *
     * @param key   The name to key the provided value with.
     * @param value The corresponding value.
     * @return The old object or null if there was no old object.
     */
    Object put(String key, Object value);

    /**
     * Gets the value corresponding to the provided key from the context.
     *
     * @param key The name of the desired value.
     * @return    The value corresponding to the provided key.
     */
    Object get(String key);

    /**
     * Indicates whether the specified key is in the context.
     *
     * @param key The key to look for.
     * @return    Whether the key is in the context.
     */
    boolean containsKey(Object key);

    /**
     * Get all the keys for the values in the context.
     * @return All the keys for the values in the context.
     */
    Object[] getKeys();

    /**
     * Removes the value associated with the specified key from the context.
     *
     * @param key The name of the value to remove.
     * @return    The value that the key was mapped to, or <code>null</code>
     *            if unmapped.
     */
    Object remove(Object key);
}
