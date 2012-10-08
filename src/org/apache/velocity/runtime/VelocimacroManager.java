package org.apache.velocity.runtime;

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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.directive.VelocimacroProxy;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.util.MapFactory;

/**
 * Manages VMs in namespaces.  Currently, two namespace modes are
 * supported:
 *
 * <ul>
 * <li>flat - all allowable VMs are in the global namespace</li>
 * <li>local - inline VMs are added to it's own template namespace</li>
 * </ul>
 *
 * Thanks to <a href="mailto:JFernandez@viquity.com">Jose Alberto Fernandez</a>
 * for some ideas incorporated here.
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @author <a href="mailto:JFernandez@viquity.com">Jose Alberto Fernandez</a>
 * @version $Id: VelocimacroManager.java 992115 2010-09-02 21:00:10Z nbubna $
 */
public class VelocimacroManager
{
    private static String GLOBAL_NAMESPACE = "";

    private boolean registerFromLib = false;

    /** Hash of namespace hashes. */
    private final Map namespaceHash = MapFactory.create(17, 0.5f, 20, false);

    /** reference to global namespace hash */
    private final Map globalNamespace;

    /** set of names of library tempates/namespaces */
    private final Set libraries = Collections.synchronizedSet(new HashSet());
    
    private RuntimeServices rsvc = null;

    /*
     * big switch for namespaces.  If true, then properties control
     * usage. If false, no.
     */
    private boolean namespacesOn = true;
    private boolean inlineLocalMode = false;
    private boolean inlineReplacesGlobal = false;

    /**
     * Adds the global namespace to the hash.
     */
    VelocimacroManager(RuntimeServices rsvc)
    {
        /*
         *  add the global namespace to the namespace hash. We always have that.
         */

        globalNamespace = addNamespace(GLOBAL_NAMESPACE);
        this.rsvc = rsvc;
    }

    /**
     * Adds a VM definition to the cache.
     * 
     * Called by VelocimacroFactory.addVelociMacro (after parsing and discovery in Macro directive)
     * 
     * @param vmName Name of the new VelociMacro.
     * @param macroBody String representation of the macro body.
     * @param argArray Array of macro parameters, first parameter is the macro name.
     * @param namespace The namespace/template from which this macro has been loaded.
     * @return Whether everything went okay.
     */
    public boolean addVM(final String vmName, final Node macroBody, final String argArray[],
                         final String namespace, boolean canReplaceGlobalMacro)
    {
        if (macroBody == null)
        {
            // happens only if someone uses this class without the Macro directive
            // and provides a null value as an argument
            throw new VelocityException("Null AST for "+vmName+" in "+namespace);
        }

        MacroEntry me = new MacroEntry(vmName, macroBody, argArray, namespace, rsvc);

        me.setFromLibrary(registerFromLib);
        
        /*
         *  the client (VMFactory) will signal to us via
         *  registerFromLib that we are in startup mode registering
         *  new VMs from libraries.  Therefore, we want to
         *  addto the library map for subsequent auto reloads
         */

        boolean isLib = true;

        MacroEntry exist = (MacroEntry) globalNamespace.get(vmName);
        
        if (registerFromLib)
        {
           libraries.add(namespace);
        }
        else
        {
            /*
             *  now, we first want to check to see if this namespace (template)
             *  is actually a library - if so, we need to use the global namespace
             *  we don't have to do this when registering, as namespaces should
             *  be shut off. If not, the default value is true, so we still go
             *  global
             */

            isLib = libraries.contains(namespace);
        }

        if ( !isLib && usingNamespaces(namespace) )
        {
            /*
             *  first, do we have a namespace hash already for this namespace?
             *  if not, add it to the namespaces, and add the VM
             */

            Map local = getNamespace(namespace, true);
            local.put(vmName, me);
            
            return true;
        }
        else
        {
            /*
             *  otherwise, add to global template.  First, check if we
             *  already have it to preserve some of the autoload information
             */


            if (exist != null)
            {
                me.setFromLibrary(exist.getFromLibrary());
            }

            /*
             *  now add it
             */

            globalNamespace.put(vmName, me);

            return true;
        }
    }
    
    /**
     * Gets a VelocimacroProxy object by the name / source template duple.
     * 
     * @param vmName Name of the VelocityMacro to look up.
     * @param namespace Namespace in which to look up the macro.
     * @return A proxy representing the Macro.
     */
     public VelocimacroProxy get(final String vmName, final String namespace)
     {
        return(get(vmName, namespace, null));
     }

     /**
      * Gets a VelocimacroProxy object by the name / source template duple.
      * 
      * @param vmName Name of the VelocityMacro to look up.
      * @param namespace Namespace in which to look up the macro.
      * @param renderingTemplate Name of the template we are currently rendering.
      * @return A proxy representing the Macro.
      * @since 1.6
      */
     public VelocimacroProxy get(final String vmName, final String namespace, final String renderingTemplate)
     {
        if( inlineReplacesGlobal && renderingTemplate != null )
        {
            /*
             * if VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL is true (local macros can
             * override global macros) and we know which template we are rendering at the
             * moment, check if local namespace contains a macro we are looking for
             * if so, return it instead of the global one
             */
            Map local = getNamespace(renderingTemplate, false);
            if (local != null)
            {
                MacroEntry me = (MacroEntry) local.get(vmName);

                if (me != null)
                {
                    return me.getProxy(namespace);
                }
            }
        }
        
        if (usingNamespaces(namespace))
        {
            Map local = getNamespace(namespace, false);

            /*
             *  if we have macros defined for this template
             */

            if (local != null)
            {
                MacroEntry me = (MacroEntry) local.get(vmName);
                
                if (me != null)
                {
                    return me.getProxy(namespace);
                }
            }
        }

        /*
         * if we didn't return from there, we need to simply see
         * if it's in the global namespace
         */

        MacroEntry me = (MacroEntry) globalNamespace.get(vmName);

        if (me != null)
        {
            return me.getProxy(namespace);
        }

        return null;
    }

    /**
     * Removes the VMs and the namespace from the manager.
     * Used when a template is reloaded to avoid
     * losing memory.
     *
     * @param namespace namespace to dump
     * @return boolean representing success
     */
    public boolean dumpNamespace(final String namespace)
    {
        if (usingNamespaces(namespace))
        {
            synchronized(this)
            {
                Map h = (Map) namespaceHash.remove(namespace);

                if (h == null)
                {
                    return false;
                }

                h.clear();

                return true;
            }
        }
        return false;
    }

    /**
     *  public switch to let external user of manager to control namespace
     *  usage indep of properties.  That way, for example, at startup the
     *  library files are loaded into global namespace
     *
     * @param namespaceOn True if namespaces should be used.
     */
    public void setNamespaceUsage(final boolean namespaceOn)
    {
        this.namespacesOn = namespaceOn;
    }

    /**
     * Should macros registered from Libraries be marked special?
     * @param registerFromLib True if macros from Libs should be marked.
     */
    public void setRegisterFromLib(final boolean registerFromLib)
    {
        this.registerFromLib = registerFromLib;
    }

    /**
     * Should macros from the same template be inlined?
     *
     * @param inlineLocalMode True if macros should be inlined on the same template.
     */
    public void setTemplateLocalInlineVM(final boolean inlineLocalMode)
    {
        this.inlineLocalMode = inlineLocalMode;
    }

    /**
     *  returns the hash for the specified namespace, and if it doesn't exist
     *  will create a new one and add it to the namespaces
     *
     *  @param namespace  name of the namespace :)
     *  @param addIfNew  flag to add a new namespace if it doesn't exist
     *  @return namespace Map of VMs or null if doesn't exist
     */
    private Map getNamespace(final String namespace, final boolean addIfNew)
    {
        Map h = (Map) namespaceHash.get(namespace);

        if (h == null && addIfNew)
        {
            h = addNamespace(namespace);
        }

        return h;
    }

    /**
     *   adds a namespace to the namespaces
     *
     *  @param namespace name of namespace to add
     *  @return Hash added to namespaces, ready for use
     */
    private Map addNamespace(final String namespace)
    {
        Map h = MapFactory.create(17, 0.5f, 20, false);
        Object oh;

        if ((oh = namespaceHash.put(namespace, h)) != null)
        {
          /*
           * There was already an entry on the table, restore it!
           * This condition should never occur, given the code
           * and the fact that this method is private.
           * But just in case, this way of testing for it is much
           * more efficient than testing before hand using get().
           */
          namespaceHash.put(namespace, oh);
          /*
           * Should't we be returning the old entry (oh)?
           * The previous code was just returning null in this case.
           */
          return null;
        }

        return h;
    }

    /**
     *  determines if currently using namespaces.
     *
     *  @param namespace currently ignored
     *  @return true if using namespaces, false if not
     */
    private boolean usingNamespaces(final String namespace)
    {
        /*
         *  if the big switch turns of namespaces, then ignore the rules
         */

        if (!namespacesOn)
        {
            return false;
        }

        /*
         *  currently, we only support the local template namespace idea
         */

        if (inlineLocalMode)
        {
            return true;
        }

        return false;
    }

    /**
     * Return the library name for a given macro.
     * @param vmName Name of the Macro to look up.
     * @param namespace Namespace to look the macro up.
     * @return The name of the library which registered this macro in a namespace.
     */
    public String getLibraryName(final String vmName, final String namespace)
    {
        if (usingNamespaces(namespace))
        {
            Map local = getNamespace(namespace, false);

            /*
             *  if we have this macro defined in this namespace, then
             *  it is masking the global, library-based one, so
             *  just return null
             */

            if ( local != null)
            {
                MacroEntry me = (MacroEntry) local.get(vmName);

                if (me != null)
                {
                    return null;
                }
            }
        }

        /*
         * if we didn't return from there, we need to simply see
         * if it's in the global namespace
         */

        MacroEntry me = (MacroEntry) globalNamespace.get(vmName);

        if (me != null)
        {
            return me.getSourceTemplate();
        }

        return null;
    }
    
    /**
     * @since 1.6
     */
    public void setInlineReplacesGlobal(boolean is)
    {
        inlineReplacesGlobal = is;
    }


    /**
     *  wrapper class for holding VM information
     */
    private static class MacroEntry
    {
        private final String vmName;
        private final String[] argArray;
        private final String sourceTemplate;
        private SimpleNode nodeTree = null;
        private boolean fromLibrary = false;
        private VelocimacroProxy vp;

        private MacroEntry(final String vmName, final Node macro,
                   final String argArray[], final String sourceTemplate,
                   RuntimeServices rsvc)
        {
            this.vmName = vmName;
            this.argArray = argArray;
            this.nodeTree = (SimpleNode)macro;
            this.sourceTemplate = sourceTemplate;

            vp = new VelocimacroProxy();
            vp.setName(this.vmName);
            vp.setArgArray(this.argArray);
            vp.setNodeTree(this.nodeTree);
            vp.setLocation(macro.getLine(), macro.getColumn(), macro.getTemplateName());
            vp.init(rsvc);
        }
        
        /**
         * Has the macro been registered from a library.
         * @param fromLibrary True if the macro was registered from a Library.
         */
        public void setFromLibrary(final boolean fromLibrary)
        {
            this.fromLibrary = fromLibrary;
        }

        /**
         * Returns true if the macro was registered from a library.
         * @return True if the macro was registered from a library.
         */
        public boolean getFromLibrary()
        {
            return fromLibrary;
        }

        /**
         * Returns the node tree for this macro.
         * @return The node tree for this macro.
         */
        public SimpleNode getNodeTree()
        {
            return nodeTree;
        }

        /**
         * Returns the source template name for this macro.
         * @return The source template name for this macro.
         */
        public String getSourceTemplate()
        {
            return sourceTemplate;
        }

        VelocimacroProxy getProxy(final String namespace)
        {
            /*
             * FIXME: namespace data is omitted, this probably 
             * breaks some error reporting?
             */ 
            return vp;
        }
    }
}


