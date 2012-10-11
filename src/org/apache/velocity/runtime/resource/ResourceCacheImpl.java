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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.util.MapFactory;

import com.dotmarketing.util.Logger;

/**
 * Default implementation of the resource cache for the default
 * ResourceManager.  The cache uses a <i>least recently used</i> (LRU)
 * algorithm, with a maximum size specified via the
 * <code>resource.manager.cache.size</code> property (idenfied by the
 * {@link
 * org.apache.velocity.runtime.RuntimeConstants#RESOURCE_MANAGER_DEFAULTCACHE_SIZE}
 * constant).  This property get be set to <code>0</code> or less for
 * a greedy, unbounded cache (the behavior from pre-v1.5).
 *
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @version $Id: ResourceCacheImpl.java 898032 2010-01-11 19:51:03Z nbubna $
 */
public class ResourceCacheImpl implements ResourceCache
{
    /**
     * Cache storage, assumed to be thread-safe.
     */
    protected Map cache = MapFactory.create(512, 0.5f, 30, false);

    /**
     * Runtime services, generally initialized by the
     * <code>initialize()</code> method.
     */
    protected RuntimeServices rsvc = null;

    /**
     * @see org.apache.velocity.runtime.resource.ResourceCache#initialize(org.apache.velocity.runtime.RuntimeServices)
     */
    public void initialize( RuntimeServices rs )
    {
        rsvc = rs;

        int maxSize =
            rsvc.getInt(RuntimeConstants.RESOURCE_MANAGER_DEFAULTCACHE_SIZE, 89);
        if (maxSize > 0)
        {
            // Create a whole new Map here to avoid hanging on to a
            // handle to the unsynch'd LRUMap for our lifetime.
            Map lruCache = Collections.synchronizedMap(new LRUMap(maxSize));
            lruCache.putAll(cache);
            cache = lruCache;
        }
        Logger.debug(this,"ResourceCache: initialized ("+this.getClass()+") with "+
               cache.getClass()+" cache map.");
    }

    /**
     * @see org.apache.velocity.runtime.resource.ResourceCache#get(java.lang.Object)
     */
    public Resource get( Object key )
    {
        return (Resource) cache.get( key );
    }

    /**
     * @see org.apache.velocity.runtime.resource.ResourceCache#put(java.lang.Object, org.apache.velocity.runtime.resource.Resource)
     */
    public Resource put( Object key, Resource value )
    {
        return (Resource) cache.put( key, value );
    }

    /**
     * @see org.apache.velocity.runtime.resource.ResourceCache#remove(java.lang.Object)
     */
    public Resource remove( Object key )
    {
        return (Resource) cache.remove( key );
    }

    /**
     * @see org.apache.velocity.runtime.resource.ResourceCache#enumerateKeys()
     */
    public Iterator enumerateKeys()
    {
        return cache.keySet().iterator();
    }
}

