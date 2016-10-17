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
 * Interface used to determine which methods are allowed to be executed.
 *
 * @author <a href="Will Glass-Husain">wglass@forio.com</a>
 * @version $Id: SecureIntrospectorControl.java 685685 2008-08-13 21:43:27Z nbubna $
 * @since 1.5
 */
public interface SecureIntrospectorControl
{

    /**
     * Determine which methods and classes to prevent from executing.  
     * 
     * @param clazz Class for which method is being called
     * @param method method being called.  This may be null in the case of a call to iterator, get, or set method
     *
     * @return true if method may be called on object
     */
    public boolean checkObjectExecutePermission(Class clazz, String method);

}
