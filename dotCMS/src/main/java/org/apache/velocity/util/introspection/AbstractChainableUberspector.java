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

import java.util.Iterator;

/**
 * Default implementation of a {@link ChainableUberspector chainable uberspector} that forwards all calls to the wrapped
 * uberspector (when that is possible). It should be used as the base class for all chainable uberspectors.
 * 
 * @version $Id: $
 * @since 1.6
 * @see ChainableUberspector
 */
public abstract class AbstractChainableUberspector extends UberspectImpl implements ChainableUberspector
{
    /** The wrapped (decorated) uberspector. */
    protected Uberspect inner;

    /**
     * {@inheritDoc}
     * 
     * @see ChainableUberspector#wrap(org.apache.velocity.util.introspection.Uberspect)
     * @see #inner
     */
    public void wrap(Uberspect inner)
    {
        this.inner = inner;
    }

    /**
     * init - the chainable uberspector is responsible for the initialization of the wrapped uberspector
     * 
     * @see org.apache.velocity.util.introspection.Uberspect#init()
     */
    //@Override
    public void init()
    {
        if (this.inner != null) {
            this.inner.init();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.velocity.util.introspection.Uberspect#getIterator(java.lang.Object,
     *      org.apache.velocity.util.introspection.Info)
     */
    //@SuppressWarnings("unchecked")
    //@Override
    public Iterator getIterator(Object obj, Info i) throws Exception
    {
        return (this.inner != null) ? this.inner.getIterator(obj, i) : null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.velocity.util.introspection.Uberspect#getMethod(java.lang.Object, java.lang.String,
     *      java.lang.Object[], org.apache.velocity.util.introspection.Info)
     */
    //@Override
    public VelMethod getMethod(Object obj, String methodName, Object[] args, Info i)
        throws Exception
    {
        return (this.inner != null) ? this.inner.getMethod(obj, methodName, args, i) : null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.velocity.util.introspection.Uberspect#getPropertyGet(java.lang.Object, java.lang.String,
     *      org.apache.velocity.util.introspection.Info)
     */
    //@Override
    public VelPropertyGet getPropertyGet(Object obj, String identifier, Info i)
        throws Exception
    {
        return (this.inner != null) ? this.inner.getPropertyGet(obj, identifier, i) : null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.velocity.util.introspection.Uberspect#getPropertySet(java.lang.Object, java.lang.String,
     *      java.lang.Object, org.apache.velocity.util.introspection.Info)
     */
    //@Override
    public VelPropertySet getPropertySet(Object obj, String identifier, Object arg, Info i)
        throws Exception
    {
        return (this.inner != null) ? this.inner.getPropertySet(obj, identifier, arg, i) : null;
    }
}
