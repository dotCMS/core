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
 * <p>
 * When the runtime.introspection.uberspect configuration property contains several
 * uberspector class names, it means those uberspectors will be chained. When an
 * uberspector in the list other than the leftmost does not implement ChainableUberspector,
 * then this utility class is used to provide a basic default chaining where the
 * first non-null result is kept for each introspection call.
 * </p>
 * 
 * @since 1.6
 * @see ChainableUberspector
 * @version $Id: LinkingUberspector.java 10959 2008-07-01 00:12:29Z sdumitriu $
 */
public class LinkingUberspector extends AbstractChainableUberspector
{
    private Uberspect leftUberspect;
    private Uberspect rightUberspect;

    /**
     * Constructor that takes the two uberspectors to link
     */
    public LinkingUberspector(Uberspect left,Uberspect right) {
        leftUberspect = left;
        rightUberspect = right;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Init both wrapped uberspectors
     * </p>
     * 
     * @see org.apache.velocity.util.introspection.Uberspect#init()
     */
    //@Override
    public void init()
    {
        leftUberspect.init();
        rightUberspect.init();
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
        Iterator it = leftUberspect.getIterator(obj,i);
        return it != null ? it : rightUberspect.getIterator(obj,i);
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
        VelMethod method = leftUberspect.getMethod(obj,methodName,args,i);
        return method != null ? method : rightUberspect.getMethod(obj,methodName,args,i);
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
        VelPropertyGet getter = leftUberspect.getPropertyGet(obj,identifier,i);
        return getter != null ? getter : rightUberspect.getPropertyGet(obj,identifier,i);
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
        VelPropertySet setter = leftUberspect.getPropertySet(obj,identifier,arg,i);
        return setter != null ? setter : rightUberspect.getPropertySet(obj,identifier,arg,i);
    }
}
