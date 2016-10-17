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
 *  Interface used for setting values that appear to be properties in
 *  Velocity.  Ex.
 *
 *      #set($foo.bar = "hello")
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: VelPropertySet.java 463298 2006-10-12 16:10:32Z henning $
 */
public interface VelPropertySet
{
    /**
     *  method used to set the value in the object
     *
     *  @param o Object on which the method will be called with the arg
     *  @param arg value to be set
     *  @return the value returned from the set operation (impl specific)
     * @throws Exception
     */
    public Object invoke(Object o, Object arg) throws Exception;

    /**
     *  specifies if this VelPropertySet is cacheable and able to be
     *  reused for this class of object it was returned for
     *
     *  @return true if can be reused for this class, false if not
     */
    public boolean isCacheable();

    /**
     *  returns the method name used to set this 'property'
     * @return The method name used to set this 'property'
     */
    public String getMethodName();
}
