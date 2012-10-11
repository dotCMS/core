package org.apache.velocity;

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
import java.util.Map;

import org.apache.velocity.context.AbstractContext;
import org.apache.velocity.context.Context;

/**
 *  General purpose implemention of the application Context
 *  interface for general application use.  This class should
 *  be used in place of the original Context class.
 *
 *  This implementation uses a HashMap  (@see java.util.HashMap )
 *  for data storage.
 *
 *  This context implementation cannot be shared between threads
 *  without those threads synchronizing access between them, as
 *  the HashMap is not synchronized, nor are some of the fundamentals
 *  of AbstractContext.  If you need to share a Context between
 *  threads with simultaneous access for some reason, please create
 *  your own and extend the interface Context
 *
 *  @see org.apache.velocity.context.Context
 *
 *  @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 *  @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 *  @author <a href="mailto:fedor.karpelevitch@home.com">Fedor Karpelevitch</a>
 *  @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 *  @version $Id: VelocityContext.java 898032 2010-01-11 19:51:03Z nbubna $
 */
public class VelocityContext extends AbstractContext implements Cloneable
{
    /**
     * Version Id for serializable
     */
    private static final long serialVersionUID = 9033846851064645037L;

    /**
     *  Storage for key/value pairs.
     */
    private Map context = null;

    /**
     *  Creates a new instance (with no inner context).
     */
    public VelocityContext()
    {
        this(null, null);
    }

    /**
     *  Creates a new instance with the provided storage (and no inner
     *  context).
     * @param context
     */
    public VelocityContext(Map context)
    {
        this(context, null);
    }

    /**
     *  Chaining constructor, used when you want to
     *  wrap a context in another.  The inner context
     *  will be 'read only' - put() calls to the
     *  wrapping context will only effect the outermost
     *  context
     *
     *  @param innerContext The <code>Context</code> implementation to
     *  wrap.
     */
    public VelocityContext( Context innerContext )
    {
        this(null, innerContext);
    }

    /**
     *  Initializes internal storage (never to <code>null</code>), and
     *  inner context.
     *
     *  @param context Internal storage, or <code>null</code> to
     *  create default storage.
     *  @param innerContext Inner context.
     */
    public VelocityContext(Map context, Context innerContext)
    {
        super(innerContext);
        this.context = (context == null ? new HashMap() : context);
    }

    /**
     *  retrieves value for key from internal
     *  storage
     *
     *  @param key name of value to get
     *  @return value as object
     */
    public Object internalGet( String key )
    {
        return context.get( key );
    }

    /**
     *  stores the value for key to internal
     *  storage
     *
     *  @param key name of value to store
     *  @param value value to store
     *  @return previous value of key as Object
     */
    public Object internalPut( String key, Object value )
    {
        return context.put( key, value );
    }

    /**
     *  determines if there is a value for the
     *  given key
     *
     *  @param key name of value to check
     *  @return true if non-null value in store
     */
    public  boolean internalContainsKey(Object key)
    {
        return context.containsKey( key );
    }

    /**
     *  returns array of keys
     *
     *  @return keys as []
     */
    public  Object[] internalGetKeys()
    {
        return context.keySet().toArray();
    }

    /**
     *  remove a key/value pair from the
     *  internal storage
     *
     *  @param key name of value to remove
     *  @return value removed
     */
    public  Object internalRemove(Object key)
    {
        return context.remove( key );
    }

    /**
     * Clones this context object.
     *
     * @return A deep copy of this <code>Context</code>.
     */
    public Object clone()
    {
        VelocityContext clone = null;
        try
        {
            clone = (VelocityContext) super.clone();
            clone.context = new HashMap(context);
        }
        catch (CloneNotSupportedException ignored)
        {
        }
        return clone;
    }    
}
