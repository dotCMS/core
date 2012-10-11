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

import java.util.HashMap;
import java.util.Stack;
import java.util.List;

import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.util.introspection.IntrospectionCacheData;

/**
 *  class to encapsulate the 'stuff' for internal operation of velocity.
 *  We use the context as a thread-safe storage : we take advantage of the
 *  fact that it's a visitor  of sorts  to all nodes (that matter) of the
 *  AST during init() and render().
 *  Currently, it carries the template name for namespace
 *  support, as well as node-local context data introspection caching.
 *
 *  Note that this is not a public class.  It is for package access only to
 *  keep application code from accessing the internals, as AbstractContext
 *  is derived from this.
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: InternalContextBase.java 731266 2009-01-04 15:11:20Z byron $
 */
class InternalContextBase implements InternalHousekeepingContext, InternalEventContext
{
    /**
     * Version Id for serializable
     */
    private static final long serialVersionUID = -245905472770843470L;

    /**
     *  cache for node/context specific introspection information
     */
    private HashMap introspectionCache = new HashMap(33);

    /**
     *  Template name stack. The stack top contains the current template name.
     */
    private Stack templateNameStack = new Stack();

    /**
     *  Velocimacro name stack. The stack top contains the current macro name.
     */
    private Stack macroNameStack = new Stack();

    /**
     *  EventCartridge we are to carry.  Set by application
     */
    private EventCartridge eventCartridge = null;

    /**
     *  Current resource - used for carrying encoding and other
     *  information down into the rendering process
     */
    private Resource currentResource = null;

    /**
     *  List for holding the macro libraries. Contains the macro library
     *  template name as strings.
     */
    private List macroLibraries = null;

    /**
     *  set the current template name on top of stack
     *
     *  @param s current template name
     */
    public void pushCurrentTemplateName( String s )
    {
        templateNameStack.push(s);
    }

    /**
     *  remove the current template name from stack
     */
    public void popCurrentTemplateName()
    {
        templateNameStack.pop();
    }

    /**
     *  get the current template name
     *
     *  @return String current template name
     */
    public String getCurrentTemplateName()
    {
        if ( templateNameStack.empty() )
            return "<undef>";
        else
            return (String) templateNameStack.peek();
    }

    /**
     *  get the current template name stack
     *
     *  @return Object[] with the template name stack contents.
     */
    public Object[] getTemplateNameStack()
    {
        return templateNameStack.toArray();
    }

    /**
     *  set the current macro name on top of stack
     *
     *  @param s current macro name
     */
    public void pushCurrentMacroName( String s )
    {
        macroNameStack.push(s);
    }

    /**
     *  remove the current macro name from stack
     */
    public void popCurrentMacroName()
    {
        macroNameStack.pop();
    }

    /**
     *  get the current macro name
     *
     *  @return String current macro name
     */
    public String getCurrentMacroName()
    {
        if (macroNameStack.empty())
        {
            return "<undef>";
        }
        else
        {
            return (String) macroNameStack.peek();
        }
    }

    /**
     *  get the current macro call depth
     *
     *  @return int current macro call depth
     */
    public int getCurrentMacroCallDepth()
    {
        return macroNameStack.size();
    }

    /**
     *  get the current macro name stack
     *
     *  @return Object[] with the macro name stack contents.
     */
    public Object[] getMacroNameStack()
    {
        return macroNameStack.toArray();
    }

    /**
     *  returns an IntrospectionCache Data (@see IntrospectionCacheData)
     *  object if exists for the key
     *
     *  @param key  key to find in cache
     *  @return cache object
     */
    public IntrospectionCacheData icacheGet( Object key )
    {
        return ( IntrospectionCacheData ) introspectionCache.get( key );
    }

    /**
     *  places an IntrospectionCache Data (@see IntrospectionCacheData)
     *  element in the cache for specified key
     *
     *  @param key  key
     *  @param o  IntrospectionCacheData object to place in cache
     */
    public void icachePut( Object key, IntrospectionCacheData o )
    {
        introspectionCache.put( key, o );
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#setCurrentResource(org.apache.velocity.runtime.resource.Resource)
     */
    public void setCurrentResource( Resource r )
    {
        currentResource = r;
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#getCurrentResource()
     */
    public Resource getCurrentResource()
    {
        return currentResource;
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#setMacroLibraries(List)
     */
    public void setMacroLibraries(List macroLibraries)
    {
        this.macroLibraries = macroLibraries;
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#getMacroLibraries()
     */
    public List getMacroLibraries()
    {
        return macroLibraries;
    }


    /**
     * @see org.apache.velocity.context.InternalEventContext#attachEventCartridge(org.apache.velocity.app.event.EventCartridge)
     */
    public EventCartridge attachEventCartridge( EventCartridge ec )
    {
        EventCartridge temp = eventCartridge;

        eventCartridge = ec;

        return temp;
    }

    /**
     * @see org.apache.velocity.context.InternalEventContext#getEventCartridge()
     */
    public EventCartridge getEventCartridge()
    {
        return eventCartridge;
    }
}


