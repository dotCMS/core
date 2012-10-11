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

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.directive.Macro;
import org.apache.velocity.runtime.directive.VelocimacroProxy;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.Node;

import com.dotmarketing.util.Logger;
import com.dotmarketing.velocity.DotResourceLoader;

/**
 *  VelocimacroFactory.java
 *
 *   manages the set of VMs in a running Velocity engine.
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: VelocimacroFactory.java 894953 2009-12-31 22:48:19Z nbubna $
 */
public class VelocimacroFactory
{
    /**
     *  runtime services for this instance
     */
    private final RuntimeServices rsvc;

    /**
     *  VMManager : deal with namespace management
     *  and actually keeps all the VM definitions
     */
    private VelocimacroManager vmManager = null;

    /**
     *  determines if replacement of global VMs are allowed
     *  controlled by  VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL
     */
    private boolean replaceAllowed = false;

    /**
     *  controls if new VMs can be added.  Set by
     *  VM_PERM_ALLOW_INLINE  Note the assumption that only
     *  through inline defs can this happen.
     *  additions through autoloaded VMs is allowed
     */
    private boolean addNewAllowed = true;

    /**
     *  sets if template-local namespace in used
     */
    private boolean templateLocal = false;

    /**
     *  determines if the libraries are auto-loaded
     *  when they change
     */
    private boolean autoReloadLibrary = false;

    /**
     *  vector of the library names
     */
    private List macroLibVec = null;

    /**
     *  map of the library Template objects
     *  used for reload determination
     */
    private Map libModMap;

    /**
     *  C'tor for the VelociMacro factory.
     *
     * @param rsvc Reference to a runtime services object.
     */
    public VelocimacroFactory(final RuntimeServices rsvc)
    {
        this.rsvc = rsvc;
        /*
         *  we always access in a synchronized(), so we
         *  can use an unsynchronized hashmap
         */
        libModMap = new HashMap();
        vmManager = new VelocimacroManager(rsvc);
    }

    /**
     *  initialize the factory - setup all permissions
     *  load all global libraries.
     */
    public void initVelocimacro()
    {
        /*
         *  maybe I'm just paranoid...
         */
        synchronized(this)
        {
            Logger.debug(this,"initialization starting.");

            /*
             *   allow replacements while we add the libraries, if exist
             */
            setReplacementPermission(true);

            /*
             *  add all library macros to the global namespace
             */

            vmManager.setNamespaceUsage(false);

            /*
             *  now, if there is a global or local libraries specified, use them.
             *  All we have to do is get the template. The template will be parsed;
             *  VM's  are added during the parse phase
             */

             Object libfiles = rsvc.getProperty(RuntimeConstants.VM_LIBRARY);

             if (libfiles == null)
             {
                 Logger.debug(this,"\"" + RuntimeConstants.VM_LIBRARY +
                     "\" is not set.  Trying default library: " +
                     RuntimeConstants.VM_LIBRARY_DEFAULT);

                 // try the default library.
                 if (rsvc.getLoaderNameForResource(RuntimeConstants.VM_LIBRARY_DEFAULT) != null)
                 {
                     libfiles = RuntimeConstants.VM_LIBRARY_DEFAULT;
                 }
                 else
                 {
                     Logger.debug(this,"Default library not found.");
                 }
             }

             if(libfiles != null)
             {
                 macroLibVec = new ArrayList();
                 if (libfiles instanceof Vector)
                 {
                     macroLibVec.addAll((Vector)libfiles);
                 }
                 else if (libfiles instanceof String)
                 {
                     macroLibVec.add(libfiles);
                 }

                 for(int i = 0, is = macroLibVec.size(); i < is; i++)
                 {
                     String lib = (String) macroLibVec.get(i);

                     /*
                      * only if it's a non-empty string do we bother
                      */

                     if (StringUtils.isNotEmpty(lib))
                     {
                         /*
                          *  let the VMManager know that the following is coming
                          *  from libraries - need to know for auto-load
                          */

                         vmManager.setRegisterFromLib(true);

                         Logger.debug(this,"adding VMs from VM library : " + lib);

                         try
                         {
                             Template template = rsvc.getTemplate(lib);

                             /*
                              *  save the template.  This depends on the assumption
                              *  that the Template object won't change - currently
                              *  this is how the Resource manager works
                              */

                             Twonk twonk = new Twonk();
                             twonk.template = template;
                             twonk.modificationTime = template.getLastModified();
                             libModMap.put(lib, twonk);
                         }
                         catch (Exception e)
                         {
                             String msg = "Velocimacro : Error using VM library : " + lib;
                             Logger.error(this,msg, e);
                             throw new VelocityException(msg, e);
                         }

                         Logger.debug(this,"VM library registration complete.");

                         vmManager.setRegisterFromLib(false);
                     }
                 }
             }

            /*
             *   now, the permissions
             */


            /*
             *  allowinline : anything after this will be an inline macro, I think
             *  there is the question if a #include is an inline, and I think so
             *
             *  default = true
             */
            setAddMacroPermission(true);

            if (!rsvc.getBoolean( RuntimeConstants.VM_PERM_ALLOW_INLINE, true))
            {
                setAddMacroPermission(false);

                Logger.debug(this,"allowInline = false : VMs can NOT be defined inline in templates");
            }
            else
            {
                Logger.debug(this,"allowInline = true : VMs can be defined inline in templates");
            }

            /*
             *  allowInlineToReplaceGlobal : allows an inline VM , if allowed at all,
             *  to replace an existing global VM
             *
             *  default = false
             */
            setReplacementPermission(false);

            if (rsvc.getBoolean(
                 RuntimeConstants.VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL, false))
            {
                setReplacementPermission(true);
                
                Logger.debug(this,"allowInlineToOverride = true : VMs " +
                    "defined inline may replace previous VM definitions");
            }
            else
            {
                Logger.debug(this,"allowInlineToOverride = false : VMs " +
                    "defined inline may NOT replace previous VM definitions");
            }

            /*
             * now turn on namespace handling as far as permissions allow in the
             * manager, and also set it here for gating purposes
             */
            vmManager.setNamespaceUsage(true);

            /*
             *  template-local inline VM mode : default is off
             */
            setTemplateLocalInline(rsvc.getBoolean(
                RuntimeConstants.VM_PERM_INLINE_LOCAL, false));

            if (getTemplateLocalInline())
            {
                Logger.debug(this,"allowInlineLocal = true : VMs " +
                    "defined inline will be local to their defining template only.");
            }
            else
            {
                Logger.debug(this,"allowInlineLocal = false : VMs " +
                    "defined inline will be global in scope if allowed.");
            }

            vmManager.setTemplateLocalInlineVM(getTemplateLocalInline());

            /*
             *  autoload VM libraries
             */
            setAutoload(rsvc.getBoolean(RuntimeConstants.VM_LIBRARY_AUTORELOAD, false));

            if (getAutoload())
            {
                Logger.debug(this,"autoload on : VM system " +
                     "will automatically reload global library macros");
            }
            else
            {
                Logger.debug(this,"autoload off : VM system " +
                      "will not automatically reload global library macros");
            }

            Logger.debug(this,"Velocimacro : initialization complete.");
        }
    }

    /**
     * Adds a macro to the factory.
     * 
     * addVelocimacro(String, Node, String[] argArray, String) should be used internally
     * instead of this.
     *
     * @param name Name of the Macro to add.
     * @param macroBody String representation of the macro.
     * @param argArray Macro arguments. First element is the macro name.
     * @param sourceTemplate Source template from which the macro gets registered.
     * 
     * @return true if Macro was registered successfully.
     */
    public boolean addVelocimacro(String name, String macroBody,
            String argArray[], String sourceTemplate)
    {
        /*
         * maybe we should throw an exception, maybe just tell
         * the caller like this...
         *
         * I hate this : maybe exceptions are in order here...
         * They definitely would be if this was only called by directly
         * by users, but Velocity calls this internally.
         */
        if (name == null || macroBody == null || argArray == null ||
            sourceTemplate == null)
        {
            String msg = "VM '"+name+"' addition rejected : ";
            if (name == null)
            {
                msg += "name";
            }
            else if (macroBody == null)
            {
                msg += "macroBody";
            }
            else if (argArray == null)
            {
                msg += "argArray";
            }
            else
            {
                msg += "sourceTemplate";
            }
            msg += " argument was null";
            Logger.error(this,msg);
            throw new NullPointerException(msg);
        }

        /*
         *  see if the current ruleset allows this addition
         */
        
        if (!canAddVelocimacro(name, sourceTemplate))
        {
            return false;
        }

        synchronized (this)
        {
            try
            {
                Node macroRootNode = rsvc.parse(new StringReader(macroBody), sourceTemplate);

                vmManager.addVM(name, macroRootNode, argArray, sourceTemplate, replaceAllowed);
            }
            catch (ParseException ex)
            {
                // to keep things 1.3 compatible call toString() here
                throw new RuntimeException(ex.toString());
            }
        }

        if (Logger.isDebugEnabled(this.getClass()))
        {
            StringBuffer msg = new StringBuffer("added ");
            Macro.macroToString(msg, argArray);
            msg.append(" : source = ").append(sourceTemplate);
            Logger.debug(this,msg.toString());
        }

        return true;
    }

    /**
     * Adds a macro to the factory.
     * 
     * @param name Name of the Macro to add.
     * @param macroBody root node of the parsed macro AST
     * @param argArray Name of the macro arguments. First element is the macro name.
     * @param sourceTemplate Source template from which the macro gets registered.
     * @return true if Macro was registered successfully.
     * @since 1.6
     */
    public boolean addVelocimacro(String name, Node macroBody,
            String argArray[], String sourceTemplate)
    {
        // Called by RuntimeInstance.addVelocimacro

    	/*
         * maybe we should throw an exception, maybe just tell
         * the caller like this...
         *
         * I hate this : maybe exceptions are in order here...
         * They definitely would be if this was only called by directly
         * by users, but Velocity calls this internally.
         */
        if (name == null || macroBody == null || argArray == null ||
            sourceTemplate == null)
        {
            String msg = "VM '"+name+"' addition rejected : ";
            if (name == null)
            {
                msg += "name";
            }
            else if (macroBody == null)
            {
                msg += "macroBody";
            }
            else if (argArray == null)
            {
                msg += "argArray";
            }
            else
            {
                msg += "sourceTemplate";
            }
            msg += " argument was null";
            Logger.error(this,msg);
            throw new NullPointerException(msg);
        }

        /*
         *  see if the current ruleset allows this addition
         */

        if (!canAddVelocimacro(name, sourceTemplate))
        {
            return false;
        }

        synchronized(this)
        {
            vmManager.addVM(name, macroBody, argArray, sourceTemplate, replaceAllowed);
        }
        if (Logger.isDebugEnabled(this.getClass()))
        {
            Logger.debug(this,"added VM "+name+": source="+sourceTemplate);
        }
        return true;
    }
    
    
    /**
     *  determines if a given macro/namespace (name, source) combo is allowed
     *  to be added
     *
     *  @param name Name of VM to add
     *  @param sourceTemplate Source template that contains the defintion of the VM
     *  @return true if it is allowed to be added, false otherwise
     */
    private synchronized boolean canAddVelocimacro(String name, String sourceTemplate)
    {
        /*
         *  short circuit and do it if autoloader is on, and the
         *  template is one of the library templates
         */
         
        if (autoReloadLibrary && (macroLibVec != null))
        {
            if( macroLibVec.contains(sourceTemplate) )
                return true;
        }


        /*
         * maybe the rules should be in manager?  I dunno. It's to manage
         * the namespace issues first, are we allowed to add VMs at all?
         * This trumps all.
         */
        if (!addNewAllowed)
        {
            Logger.warn(this,"VM addition rejected : "+name+" : inline VMs not allowed.");
            return false;
        }

        /*
         *  are they local in scope?  Then it is ok to add.
         */
        if (!templateLocal)
        {
            /*
             * otherwise, if we have it already in global namespace, and they can't replace
             * since local templates are not allowed, the global namespace is implied.
             *  remember, we don't know anything about namespace managment here, so lets
             *  note do anything fancy like trying to give it the global namespace here
             *
             *  so if we have it, and we aren't allowed to replace, bail
             */
            if (!replaceAllowed && isVelocimacro(name, sourceTemplate))
            {
                /*
                 * Concurrency fix: the log entry was changed to debug scope because it
                 * causes false alarms when several concurrent threads simultaneously (re)parse
                 * some macro
                 */ 
                if (Logger.isDebugEnabled(this.getClass()))
                    Logger.debug(this,"VM addition rejected : "+name+" : inline not allowed to replace existing VM");
                return false;
            }
        }

        return true;
    }

    /**
     * Tells the world if a given directive string is a Velocimacro
     * @param vm Name of the Macro.
     * @param sourceTemplate Source template from which the macro should be loaded.
     * @return True if the given name is a macro.
     */
    public boolean isVelocimacro(String vm, String sourceTemplate)
    {
        // synchronization removed
        return(vmManager.get(vm, sourceTemplate) != null);
    }

    /**
     *  actual factory : creates a Directive that will
     *  behave correctly wrt getting the framework to
     *  dig out the correct # of args
     * @param vmName Name of the Macro.
     * @param sourceTemplate Source template from which the macro should be loaded.
     * @return A directive representing the Macro.
     */
     public Directive getVelocimacro(String vmName, String sourceTemplate)
     {
        return(getVelocimacro(vmName, sourceTemplate, null));
     }

     /**
      * @since 1.6
      */
     public Directive getVelocimacro(String vmName, String sourceTemplate, String renderingTemplate)
     {
        VelocimacroProxy vp = null;

        vp = vmManager.get(vmName, sourceTemplate, renderingTemplate);

        /*
         * if this exists, and autoload is on, we need to check where this VM came from
         */

        if (vp != null && autoReloadLibrary )
        {
            synchronized (this)
            {
                /*
                 * see if this VM came from a library. Need to pass sourceTemplate in the event
                 * namespaces are set, as it could be masked by local
                 */

                String lib = vmManager.getLibraryName(vmName, sourceTemplate);

                if (lib != null)
                {
                    try
                    {
                        /*
                         * get the template from our map
                         */

                        Twonk tw = (Twonk) libModMap.get(lib);

                        if (tw != null)
                        {
                            Template template = tw.template;

                            /*
                             * now, compare the last modified time of the resource with the last
                             * modified time of the template if the file has changed, then reload.
                             * Otherwise, we should be ok.
                             */

                            long tt = tw.modificationTime;
                            long ft = DotResourceLoader.getInstance().getLastModified(template);

                            if (ft > tt)
                            {
                                Logger.debug(this,"auto-reloading VMs from VM library : " + lib);

                                /*
                                 * when there are VMs in a library that invoke each other, there are
                                 * calls into getVelocimacro() from the init() process of the VM
                                 * directive. To stop the infinite loop we save the current time
                                 * reported by the resource loader and then be honest when the
                                 * reload is complete
                                 */

                                tw.modificationTime = ft;

                                template = rsvc.getTemplate(lib);

                                /*
                                 * and now we be honest
                                 */

                                tw.template = template;
                                tw.modificationTime = template.getLastModified();

                                /*
                                 * note that we don't need to put this twonk
                                 * back into the map, as we can just use the
                                 * same reference and this block is synchronized
                                 */
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        String msg = "Velocimacro : Error using VM library : " + lib;
                        Logger.error(this,msg, e);
                        throw new VelocityException(msg, e);
                    }

                    vp = vmManager.get(vmName, sourceTemplate, renderingTemplate);
                }
            }
        }

        return vp;
    }

    /**
     * tells the vmManager to dump the specified namespace
     * 
     * @param namespace Namespace to dump.
     * @return True if namespace has been dumped successfully.
     */
    public boolean dumpVMNamespace(String namespace)
    {
        return vmManager.dumpNamespace(namespace);
    }

    /**
     * sets permission to have VMs local in scope to their declaring template note that this is
     * really taken care of in the VMManager class, but we need it here for gating purposes in addVM
     * eventually, I will slide this all into the manager, maybe.
     */
    private void setTemplateLocalInline(boolean b)
    {
        templateLocal = b;
    }

    private boolean getTemplateLocalInline()
    {
        return templateLocal;
    }

    /**
     * sets the permission to add new macros
     */
    private boolean setAddMacroPermission(final boolean addNewAllowed)
    {
        boolean b = this.addNewAllowed;
        this.addNewAllowed = addNewAllowed;
        return b;
    }

    /**
     * sets the permission for allowing addMacro() calls to replace existing VM's
     */
    private boolean setReplacementPermission(boolean arg)
    {
        boolean b = replaceAllowed;
        replaceAllowed = arg;
        vmManager.setInlineReplacesGlobal(arg);
        return b;
    }

    /**
     *  set the switch for automatic reloading of
     *  global library-based VMs
     */
    private void setAutoload(boolean b)
    {
        autoReloadLibrary = b;
    }

    /**
     *  get the switch for automatic reloading of
     *  global library-based VMs
     */
    private boolean getAutoload()
    {
        return autoReloadLibrary;
    }

    /**
     * small container class to hold the tuple
     * of a template and modification time.
     * We keep the modification time so we can
     * 'override' it on a reload to prevent
     * recursive reload due to inter-calling
     * VMs in a library
     */
    private static class Twonk
    {
        /** Template kept in this container. */
        public Template template;

        /** modification time of the template. */
        public long modificationTime;
    }
}







