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
 * 'Federated' introspection/reflection interface to allow the introspection
 *  behavior in Velocity to be customized.
 *
 * @author <a href="mailto:geirm@apache.org">Geir Magusson Jr.</a>
 * @version $Id: Uberspect.java 774412 2009-05-13 15:54:07Z nbubna $
 */
public interface Uberspect
{
    /**
     *  Initializer - will be called before use
     */
    public void init();

    /**
     *  To support iteratives - #foreach()
     * @param obj
     * @param info
     * @return An Iterator.
     */
    public Iterator getIterator(Object obj, Info info) throws Exception;

    /**
     *  Returns a general method, corresponding to $foo.bar( $woogie )
     * @param obj
     * @param method
     * @param args
     * @param info
     * @return A Velocity Method.
     */
    public VelMethod getMethod(Object obj, String method, Object[] args, Info info) throws Exception;

    /**
     * Property getter - returns VelPropertyGet appropos for #set($foo = $bar.woogie)
     * @param obj
     * @param identifier
     * @param info
     * @return A Velocity Getter.
     */
    public VelPropertyGet getPropertyGet(Object obj, String identifier, Info info) throws Exception;

    /**
     * Property setter - returns VelPropertySet appropos for #set($foo.bar = "geir")
     * @param obj
     * @param identifier
     * @param arg
     * @param info
     * @return A Velocity Setter.
     */
    public VelPropertySet getPropertySet(Object obj, String identifier, Object arg, Info info) throws Exception;
}
