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

import org.apache.velocity.util.introspection.IntrospectionCacheData;

import org.apache.velocity.runtime.resource.Resource;

import java.util.List;

/**
 *  interface to encapsulate the 'stuff' for internal operation of velocity.
 *  We use the context as a thread-safe storage : we take advantage of the
 *  fact that it's a visitor  of sorts  to all nodes (that matter) of the
 *  AST during init() and render().
 *
 *  Currently, it carries the template name for namespace
 *  support, as well as node-local context data introspection caching.
 *
 *  @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 *  @author <a href="mailto:Christoph.Reck@dlr.de">Christoph Reck</a>
 *  @version $Id: InternalHousekeepingContext.java 731266 2009-01-04 15:11:20Z byron $
 */
interface InternalHousekeepingContext
{
    /**
     *  set the current template name on top of stack
     *
     *  @param s current template name
     */
    void pushCurrentTemplateName( String s );

    /**
     *  remove the current template name from stack
     */
    void popCurrentTemplateName();

    /**
     *  get the current template name
     *
     *  @return String current template name
     */
    String getCurrentTemplateName();

    /**
     *  Returns the template name stack in form of an array.
     *
     *  @return Object[] with the template name stack contents.
     */
    Object[] getTemplateNameStack();

    /**
     *  set the current macro name on top of stack
     *
     *  @param s current macro name
     */
    void pushCurrentMacroName( String s );

    /**
     *  remove the current macro name from stack
     */
    void popCurrentMacroName();

    /**
     *  get the current macro name
     *
     *  @return String current macro name
     */
    String getCurrentMacroName();

    /**
     *  get the current macro call depth
     *
     *  @return int current macro call depth
     */
    int getCurrentMacroCallDepth();

    /**
     *  Returns the macro name stack in form of an array.
     *
     *  @return Object[] with the macro name stack contents.
     */
    Object[] getMacroNameStack();

    /**
     *  returns an IntrospectionCache Data (@see IntrospectionCacheData)
     *  object if exists for the key
     *
     *  @param key  key to find in cache
     *  @return cache object
     */
    IntrospectionCacheData icacheGet( Object key );

    /**
     *  places an IntrospectionCache Data (@see IntrospectionCacheData)
     *  element in the cache for specified key
     *
     *  @param key  key
     *  @param o  IntrospectionCacheData object to place in cache
     */
    void icachePut( Object key, IntrospectionCacheData o );

    /**
     *  temporary fix to enable #include() to figure out
     *  current encoding.
     *
     * @return The current resource.
     */
    Resource getCurrentResource();


    /**
     * @param r
     */
    void setCurrentResource( Resource r );

    /**
     * Set the macro library list for the current template.
     *
     * @param macroLibraries list of macro libraries to set
     */
     void setMacroLibraries(List macroLibraries);

    /**
     * Get the macro library list for the current template.
     *
     * @return List of macro library names
     */
     List getMacroLibraries();

}
