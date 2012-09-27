package org.apache.velocity.runtime.resource;

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

import java.util.Iterator;
import org.apache.velocity.runtime.RuntimeServices;

/**
 * Interface that defines the shape of a pluggable resource cache
 *  for the included ResourceManager
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: ResourceCache.java 463298 2006-10-12 16:10:32Z henning $
 */
public interface ResourceCache
{
    /**
     *  initializes the ResourceCache.  Will be
     *  called before any utilization
     *
     *  @param rs RuntimeServices to use for logging, etc
     */
    public void initialize( RuntimeServices rs );

    /**
     *  retrieves a Resource from the
     *  cache
     *
     *  @param resourceKey key for Resource to be retrieved
     *  @return Resource specified or null if not found
     */
    public Resource get( Object resourceKey );

    /**
     *  stores a Resource in the cache
     *
     *  @param resourceKey key to associate with the Resource
     *  @param resource Resource to be stored
     *  @return existing Resource stored under this key, or null if none
     */
    public Resource put( Object resourceKey, Resource resource );

    /**
     *  removes a Resource from the cache
     *
     *  @param resourceKey resource to be removed
     *  @return stored under key
     */
    public Resource remove( Object resourceKey );

    /**
     *  returns an Iterator of Keys in the cache.
     * @return An Iterator of Keys in the cache.
     */
     public Iterator enumerateKeys();
}
