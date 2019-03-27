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
 *  Holds information for node-local context data introspection
 *  information.
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: IntrospectionCacheData.java 463298 2006-10-12 16:10:32Z henning $
 */
public class IntrospectionCacheData
{
    /**
     *  Object to pair with class - currently either a Method or
     *  AbstractExecutor. It can be used in any way the using node
     *  wishes.
     */
    public Object thingy;

    /**
     *  Class of context data object associated with the introspection
     *  information
     */
    public Class  contextData;
}
