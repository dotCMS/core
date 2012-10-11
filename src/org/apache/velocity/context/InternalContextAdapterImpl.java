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

import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.util.introspection.IntrospectionCacheData;

import java.util.List;

/**
 *  This adapter class is the container for all context types for internal
 *  use.  The AST now uses this class rather than the app-level Context
 *  interface to allow flexibility in the future.
 *
 *  Currently, we have two context interfaces which must be supported :
 *  <ul>
 *  <li> Context : used for application/template data access
 *  <li> InternalHousekeepingContext : used for internal housekeeping and caching
 *  <li> InternalWrapperContext : used for getting root cache context and other
 *       such.
 *  <li> InternalEventContext : for event handling.
 *  </ul>
 *
 *  This class implements the two interfaces to ensure that all methods are
 *  supported.  When adding to the interfaces, or adding more context
 *  functionality, the interface is the primary definition, so alter that first
 *  and then all classes as necessary.  As of this writing, this would be
 *  the only class affected by changes to InternalContext
 *
 *  This class ensures that an InternalContextBase is available for internal
 *  use.  If an application constructs their own Context-implementing
 *  object w/o subclassing AbstractContext, it may be that support for
 *  InternalContext is not available.  Therefore, InternalContextAdapter will
 *  create an InternalContextBase if necessary for this support.  Note that
 *  if this is necessary, internal information such as node-cache data will be
 *  lost from use to use of the context.  This may or may not be important,
 *  depending upon application.
 *
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: InternalContextAdapterImpl.java 731266 2009-01-04 15:11:20Z byron $
 */
public final class InternalContextAdapterImpl implements InternalContextAdapter
{
    /**
     *  the user data Context that we are wrapping
     */
    Context context = null;

    /**
     *  the ICB we are wrapping.  We may need to make one
     *  if the user data context implementation doesn't
     *  support one.  The default AbstractContext-derived
     *  VelocityContext does, and it's recommended that
     *  people derive new contexts from AbstractContext
     *  rather than piecing things together
     */
    InternalHousekeepingContext icb = null;

    /**
     *  The InternalEventContext that we are wrapping.  If
     *  the context passed to us doesn't support it, no
     *  biggie.  We don't make it for them - since its a
     *  user context thing, nothing gained by making one
     *  for them now
     */
    InternalEventContext iec = null;

    /**
     *  CTOR takes a Context and wraps it, delegating all 'data' calls
     *  to it.
     *
     *  For support of internal contexts, it will create an InternalContextBase
     *  if need be.
     * @param c
     */
    public InternalContextAdapterImpl( Context c )
    {
        context = c;

        if ( !( c instanceof InternalHousekeepingContext ))
        {
            icb = new InternalContextBase();
        }
        else
        {
            icb = (InternalHousekeepingContext) context;
        }

        if ( c instanceof InternalEventContext)
        {
            iec = ( InternalEventContext) context;
        }
    }

    /* --- InternalHousekeepingContext interface methods --- */

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#pushCurrentTemplateName(java.lang.String)
     */
    public void pushCurrentTemplateName( String s )
    {
        icb.pushCurrentTemplateName( s );
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#popCurrentTemplateName()
     */
    public void popCurrentTemplateName()
    {
        icb.popCurrentTemplateName();
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#getCurrentTemplateName()
     */
    public String getCurrentTemplateName()
    {
        return icb.getCurrentTemplateName();
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#getTemplateNameStack()
     */
    public Object[] getTemplateNameStack()
    {
        return icb.getTemplateNameStack();
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#pushCurrentMacroName(java.lang.String)
     * @since 1.6
     */
    public void pushCurrentMacroName( String s )
    {
        icb.pushCurrentMacroName( s );
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#popCurrentMacroName()
     * @since 1.6
     */
    public void popCurrentMacroName()
    {
        icb.popCurrentMacroName();
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#getCurrentMacroName()
     * @since 1.6
     */
    public String getCurrentMacroName()
    {
        return icb.getCurrentMacroName();
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#getCurrentMacroCallDepth()
     * @since 1.6
     */
    public int getCurrentMacroCallDepth()
    {
        return icb.getCurrentMacroCallDepth();
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#getMacroNameStack()
     * @since 1.6
     */
    public Object[] getMacroNameStack()
    {
        return icb.getMacroNameStack();
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#icacheGet(java.lang.Object)
     */
    public IntrospectionCacheData icacheGet( Object key )
    {
        return icb.icacheGet( key );
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#icachePut(java.lang.Object, org.apache.velocity.util.introspection.IntrospectionCacheData)
     */
    public void icachePut( Object key, IntrospectionCacheData o )
    {
        icb.icachePut( key, o );
    }

   /**
    * @see org.apache.velocity.context.InternalHousekeepingContext#setCurrentResource(org.apache.velocity.runtime.resource.Resource)
    */
    public void setCurrentResource( Resource r )
    {
        icb.setCurrentResource(r);
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#getCurrentResource()
     */
    public Resource getCurrentResource()
    {
        return icb.getCurrentResource();
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#setMacroLibraries(List)
     * @since 1.6
     */
    public void setMacroLibraries(List macroLibraries)
    {
        icb.setMacroLibraries(macroLibraries);
    }

    /**
     * @see org.apache.velocity.context.InternalHousekeepingContext#getMacroLibraries()
     * @since 1.6
     */
    public List getMacroLibraries()
    {
        return icb.getMacroLibraries();
    }

    /* ---  Context interface methods --- */

    /**
     * @see org.apache.velocity.context.Context#put(java.lang.String, java.lang.Object)
     */
    public Object put(String key, Object value)
    {
        return context.put( key , value );
    }

    /**
     * @see InternalWrapperContext#localPut(String, Object)
     * @since 1.5
     */
    public Object localPut(final String key, final Object value)
    {
        return put(key, value);
    }

    /**
     * @see org.apache.velocity.context.Context#get(java.lang.String)
     */
    public Object get(String key)
    {
        return context.get( key );
    }

    /**
     * @see org.apache.velocity.context.Context#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key)
    {
        return context.containsKey( key );
    }

    /**
     * @see org.apache.velocity.context.Context#getKeys()
     */
    public Object[] getKeys()
    {
        return context.getKeys();
    }

    /**
     * @see org.apache.velocity.context.Context#remove(java.lang.Object)
     */
    public Object remove(Object key)
    {
        return context.remove( key );
    }


    /* ---- InternalWrapperContext --- */

    /**
     *  returns the user data context that
     *  we are wrapping
     * @return The internal user data context.
     */
    public Context getInternalUserContext()
    {
        return context;
    }

    /**
     *  Returns the base context that we are
     *  wrapping. Here, its this, but for other thing
     *  like VM related context contortions, it can
     *  be something else
     * @return The base context.
     */
    public InternalContextAdapter getBaseContext()
    {
        return this;
    }

    /* -----  InternalEventContext ---- */

    /**
     * @see org.apache.velocity.context.InternalEventContext#attachEventCartridge(org.apache.velocity.app.event.EventCartridge)
     */
    public EventCartridge attachEventCartridge( EventCartridge ec )
    {
        if (iec != null)
        {
            return iec.attachEventCartridge( ec );
        }

        return null;
    }

    /**
     * @see org.apache.velocity.context.InternalEventContext#getEventCartridge()
     */
    public EventCartridge getEventCartridge()
    {
        if ( iec != null)
        {
            return iec.getEventCartridge( );
        }

        return null;
    }
}



