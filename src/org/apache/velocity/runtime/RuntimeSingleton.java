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

import java.io.Reader;
import java.util.Properties;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.Template;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.runtime.resource.ContentResource;
import org.apache.velocity.util.introspection.Introspector;
import org.apache.velocity.util.introspection.Uberspect;

/**
 * This is the Runtime system for Velocity. It is the
 * single access point for all functionality in Velocity.
 * It adheres to the mediator pattern and is the only
 * structure that developers need to be familiar with
 * in order to get Velocity to perform.
 *
 * The Runtime will also cooperate with external
 * systems like Turbine. Runtime properties can
 * set and then the Runtime is initialized.
 *
 * Turbine for example knows where the templates
 * are to be loaded from, and where the velocity
 * log file should be placed.
 *
 * So in the case of Velocity cooperating with Turbine
 * the code might look something like the following:
 *
 * <pre>
 * RuntimeSingleton.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templatePath);
 * RuntimeSingleton.setProperty(RuntimeConstants.RUNTIME_LOG, pathToVelocityLog);
 * RuntimeSingleton.init();
 * </pre>
 *
 * <pre>
 * -----------------------------------------------------------------------
 * N O T E S  O N  R U N T I M E  I N I T I A L I Z A T I O N
 * -----------------------------------------------------------------------
 * RuntimeSingleton.init()
 *
 * If Runtime.init() is called by itself the Runtime will
 * initialize with a set of default values.
 * -----------------------------------------------------------------------
 * RuntimeSingleton.init(String/Properties)
 *
 * In this case the default velocity properties are layed down
 * first to provide a solid base, then any properties provided
 * in the given properties object will override the corresponding
 * default property.
 * -----------------------------------------------------------------------
 * </pre>
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:jlb@houseofdistraction.com">Jeff Bowden</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magusson Jr.</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 *
 * @see org.apache.velocity.runtime.RuntimeInstance
 *
 * @version $Id: RuntimeSingleton.java 898050 2010-01-11 20:15:31Z nbubna $
 */
public class RuntimeSingleton implements RuntimeConstants
{
    private static RuntimeInstance ri = new RuntimeInstance();

    /**
     * This is the primary initialization method in the Velocity
     * Runtime. The systems that are setup/initialized here are
     * as follows:
     *
     * <ul>
     *   <li>Logging System</li>
     *   <li>ResourceManager</li>
     *   <li>Event Handlers</li>
     *   <li>Parser Pool</li>
     *   <li>Global Cache</li>
     *   <li>Static Content Include System</li>
     *   <li>Velocimacro System</li>
     * </ul>
     * @see RuntimeInstance#init()
     */
    public synchronized static void init()
    {
        ri.init();
    }

    /**
     * Returns true if the RuntimeInstance has been successfully initialized.
     * @return True if the RuntimeInstance has been successfully initialized.
     * @see RuntimeInstance#isInitialized()
     * @since 1.5
     */
    public static boolean isInitialized()
    {
        return ri.isInitialized();
    }

    /**
     * Returns the RuntimeServices Instance used by this wrapper.
     *
     * @return The RuntimeServices Instance used by this wrapper.
     */
    public static RuntimeServices getRuntimeServices()
    {
        return ri;
    }


    /**
     * Allows an external system to set a property in
     * the Velocity Runtime.
     *
     * @param key property key
     * @param  value property value
     * @see RuntimeInstance#setProperty(String, Object)
     */
    public static void setProperty(String key, Object value)
    {
        ri.setProperty( key, value );
    }

    /**
     * Allow an external system to set an ExtendedProperties
     * object to use. This is useful where the external
     * system also uses the ExtendedProperties class and
     * the velocity configuration is a subset of
     * parent application's configuration. This is
     * the case with Turbine.
     *
     * @param configuration
     * @see RuntimeInstance#setConfiguration(ExtendedProperties)
     */
    public static void setConfiguration( ExtendedProperties configuration)
    {
        ri.setConfiguration( configuration );
    }

    /**
     * Add a property to the configuration. If it already
     * exists then the value stated here will be added
     * to the configuration entry. For example, if
     *
     * resource.loader = file
     *
     * is already present in the configuration and you
     *
     * addProperty("resource.loader", "classpath")
     *
     * Then you will end up with a Vector like the
     * following:
     *
     * ["file", "classpath"]
     *
     * @param key
     * @param value
     * @see RuntimeInstance#addProperty(String, Object)
     */
    public static void addProperty(String key, Object value)
    {
        ri.addProperty( key, value );
    }

    /**
     * Clear the values pertaining to a particular
     * property.
     *
     * @param key of property to clear
     * @see RuntimeInstance#clearProperty(String)
     */
    public static void clearProperty(String key)
    {
        ri.clearProperty( key );
    }

    /**
     *  Allows an external caller to get a property.  The calling
     *  routine is required to know the type, as this routine
     *  will return an Object, as that is what properties can be.
     *
     *  @param key property to return
     *  @return Value of the property or null if it does not exist.
     * @see RuntimeInstance#getProperty(String)
     */
    public static Object getProperty( String key )
    {
        return ri.getProperty( key );
    }

    /**
     * Initialize the Velocity Runtime with a Properties
     * object.
     *
     * @param p
     * @see RuntimeInstance#init(Properties)
     */
    public static void init(Properties p)
    {
        ri.init(p);
    }

    /**
     * Initialize the Velocity Runtime with the name of
     * ExtendedProperties object.
     *
     * @param configurationFile
     * @see RuntimeInstance#init(String)
     */
    public static void init(String configurationFile)
    {
        ri.init( configurationFile );
    }

    /**
     * Parse the input and return the root of
     * AST node structure.
     * <br><br>
     *  In the event that it runs out of parsers in the
     *  pool, it will create and let them be GC'd
     *  dynamically, logging that it has to do that.  This
     *  is considered an exceptional condition.  It is
     *  expected that the user will set the
     *  PARSER_POOL_SIZE property appropriately for their
     *  application.  We will revisit this.
     *
     * @param reader Reader retrieved by a resource loader
     * @param templateName name of the template being parsed
     * @return A root node representing the template as an AST tree.
     * @throws ParseException When the template could not be parsed.
     * @see RuntimeInstance#parse(Reader, String)
     */
    public static SimpleNode parse( Reader reader, String templateName )
        throws ParseException
    {
        return ri.parse( reader, templateName );
    }

    /**
     *  Parse the input and return the root of the AST node structure.
     *
     * @param reader Reader retrieved by a resource loader
     * @param templateName name of the template being parsed
     * @param dumpNamespace flag to dump the Velocimacro namespace for this template
     * @return A root node representing the template as an AST tree.
     * @throws ParseException When the template could not be parsed.
     * @see RuntimeInstance#parse(Reader, String, boolean)
     */
    public static SimpleNode parse( Reader reader, String templateName, boolean dumpNamespace )
        throws ParseException
    {
        return ri.parse( reader, templateName, dumpNamespace );
    }


    /**
     * Returns a <code>Template</code> from the resource manager.
     * This method assumes that the character encoding of the
     * template is set by the <code>input.encoding</code>
     * property.  The default is "ISO-8859-1"
     *
     * @param name The file name of the desired template.
     * @return     The template.
     * @throws ResourceNotFoundException if template not found
     *          from any available source.
     * @throws ParseErrorException if template cannot be parsed due
     *          to syntax (or other) error.
     * @see RuntimeInstance#getTemplate(String)
     */
    public static Template getTemplate(String name)
        throws ResourceNotFoundException, ParseErrorException
    {
        return ri.getTemplate( name );
    }

    /**
     * Returns a <code>Template</code> from the resource manager
     *
     * @param name The  name of the desired template.
     * @param encoding Character encoding of the template
     * @return     The template.
     * @throws ResourceNotFoundException if template not found
     *          from any available source.
     * @throws ParseErrorException if template cannot be parsed due
     *          to syntax (or other) error.
     * @see RuntimeInstance#getTemplate(String, String)
     */
    public static Template getTemplate(String name, String  encoding)
        throws ResourceNotFoundException, ParseErrorException
    {
        return ri.getTemplate( name, encoding );
    }

    /**
     * Returns a static content resource from the
     * resource manager.  Uses the current value
     * if INPUT_ENCODING as the character encoding.
     *
     * @param name Name of content resource to get
     * @return parsed ContentResource object ready for use
     * @throws ResourceNotFoundException if template not found
     *          from any available source.
     * @throws ParseErrorException When the template could not be parsed.
     * @see RuntimeInstance#getContent(String)
     */
    public static ContentResource getContent(String name)
        throws ResourceNotFoundException, ParseErrorException
    {
        return ri.getContent( name );
    }

    /**
     * Returns a static content resource from the
     * resource manager.
     *
     * @param name Name of content resource to get
     * @param encoding Character encoding to use
     * @return parsed ContentResource object ready for use
     * @throws ResourceNotFoundException if template not found
     *          from any available source.
     * @throws ParseErrorException When the template could not be parsed.
     * @see RuntimeInstance#getContent(String, String)
     */
    public static ContentResource getContent( String name, String encoding )
        throws ResourceNotFoundException, ParseErrorException
    {
        return ri.getContent( name, encoding );
    }


    /**
     *  Determines is a template exists, and returns name of the loader that
     *  provides it.  This is a slightly less hokey way to support
     *  the Velocity.templateExists() utility method, which was broken
     *  when per-template encoding was introduced.  We can revisit this.
     *
     *  @param resourceName Name of template or content resource
     *  @return class name of loader than can provide it
     * @see RuntimeInstance#getLoaderNameForResource(String)
     */
    public static String getLoaderNameForResource( String resourceName )
    {
        return ri.getLoaderNameForResource( resourceName );
    }

    /**
     * String property accessor method with default to hide the
     * configuration implementation.
     *
     * @param key property key
     * @param defaultValue  default value to return if key not
     *               found in resource manager.
     * @return value of key or default
     * @see RuntimeInstance#getString(String, String)
     */
    public static String getString( String key, String defaultValue)
    {
        return ri.getString( key, defaultValue );
    }

    /**
     * Returns the appropriate VelocimacroProxy object if strVMname
     * is a valid current Velocimacro.
     *
     * @param vmName Name of velocimacro requested
     * @param templateName Name of the template that contains the velocimacro.
     * @return The requested VelocimacroProxy.
     * @see RuntimeInstance#getVelocimacro(String, String)
     */
    public static Directive getVelocimacro( String vmName, String templateName  )
    {
        return ri.getVelocimacro( vmName, templateName );
    }

    /**
     * Adds a new Velocimacro. Usually called by Macro only while parsing.
     *
     * @param name  Name of a new velocimacro.
     * @param macro  root AST node of the parsed macro
     * @param argArray  Array of strings, containing the
     *                         #macro() arguments.  the 0th argument is the name.
     * @param sourceTemplate The template from which the macro is requested.
     * @return boolean  True if added, false if rejected for some
     *                  reason (either parameters or permission settings)
     * @see RuntimeInstance#addVelocimacro(String, Node, String[], String)
     * @since 1.6
     */
    public static boolean addVelocimacro(String name, Node macro,
                                         String argArray[], String sourceTemplate)
    {
        return ri.addVelocimacro(name, macro, argArray, sourceTemplate);
    }

   /**
    * Adds a new Velocimacro. Usually called by Macro only while parsing.
    *
    * @param name Name of velocimacro
    * @param macro String form of macro body
    * @param argArray Array of strings, containing the
    *                         #macro() arguments.  the 0th is the name.
    * @param sourceTemplate Name of the template that contains the velocimacro.
    * @return True if added, false if rejected for some
    *                  reason (either parameters or permission settings)
    *                  
    * @deprecated Use addVelocimacro(String, Node, String[], String) instead                  
    *                  
    * @see RuntimeInstance#addVelocimacro(String, String, String[], String)
    */
    public static boolean addVelocimacro( String name,
                                          String macro,
                                          String argArray[],
                                          String sourceTemplate )
    {
        return ri.addVelocimacro( name, macro, argArray, sourceTemplate );
    }

    /**
     *  Checks to see if a VM exists
     *
     * @param vmName Name of the Velocimacro.
     * @param templateName Template on which to look for the Macro.
     * @return True if VM by that name exists, false if not
     * @see RuntimeInstance#isVelocimacro(String, String)
     */
    public static boolean isVelocimacro( String vmName, String templateName )
    {
        return ri.isVelocimacro( vmName, templateName );
    }

    /**
     * tells the vmFactory to dump the specified namespace.  This is to support
     * clearing the VM list when in inline-VM-local-scope mode
     * @param namespace Namespace to dump.
     * @return True if namespace was dumped successfully.
     * @see RuntimeInstance#dumpVMNamespace(String)
     */
    public static boolean dumpVMNamespace( String namespace )
    {
        return ri.dumpVMNamespace( namespace );
    }

    /* --------------------------------------------------------------------
     * R U N T I M E  A C C E S S O R  M E T H O D S
     * --------------------------------------------------------------------
     * These are the getXXX() methods that are a simple wrapper
     * around the configuration object. This is an attempt
     * to make a the Velocity Runtime the single access point
     * for all things Velocity, and allow the Runtime to
     * adhere as closely as possible the the Mediator pattern
     * which is the ultimate goal.
     * --------------------------------------------------------------------
     */

    /**
     * String property accessor method to hide the configuration implementation
     * @param key  property key
     * @return   value of key or null
     * @see RuntimeInstance#getString(String)
     */
    public static String getString(String key)
    {
        return ri.getString( key );
    }

    /**
     * Int property accessor method to hide the configuration implementation.
     *
     * @param key Property key
     * @return value
     * @see RuntimeInstance#getInt(String)
     */
    public static int getInt( String key )
    {
        return ri.getInt( key );
    }

    /**
     * Int property accessor method to hide the configuration implementation.
     *
     * @param key  property key
     * @param defaultValue The default value.
     * @return value
     * @see RuntimeInstance#getInt(String, int)
     */
    public static int getInt( String key, int defaultValue )
    {
        return ri.getInt( key, defaultValue );
    }

    /**
     * Boolean property accessor method to hide the configuration implementation.
     *
     * @param key property key
     * @param def The default value if property not found.
     * @return value of key or default value
     * @see RuntimeInstance#getBoolean(String, boolean)
     */
    public static boolean getBoolean( String key, boolean def )
    {
        return ri.getBoolean( key, def );
    }

    /**
     * Return the velocity runtime configuration object.
     *
     * @return ExtendedProperties configuration object which houses
     *                       the velocity runtime properties.
     * @see RuntimeInstance#getConfiguration()
     */
    public static ExtendedProperties getConfiguration()
    {
        return ri.getConfiguration();
    }

    /**
     *  Return the Introspector for this RuntimeInstance
     *
     *  @return Introspector object for this runtime instance
     * @see RuntimeInstance#getIntrospector()
     */
    public static Introspector getIntrospector()
    {
        return ri.getIntrospector();
    }

    /**
     * Returns the event handlers for the application.
     * @return The event handlers for the application.
     * @see RuntimeInstance#getApplicationEventCartridge()
     * @since 1.5
     */
     public EventCartridge getEventCartridge()
     {
         return ri.getApplicationEventCartridge();
     }

    /**
     *  Gets the application attribute for the given key
     *
     * @see org.apache.velocity.runtime.RuntimeServices#getApplicationAttribute(Object)
     * @param key
     * @return The application attribute for the given key.
     * @see RuntimeInstance#getApplicationAttribute(Object)
     */
    public static Object getApplicationAttribute(Object key)
    {
        return ri.getApplicationAttribute(key);
    }

    /**
     * Returns the Uberspect object for this Instance.
     *
     * @return The Uberspect object for this Instance.
     * @see org.apache.velocity.runtime.RuntimeServices#getUberspect()
     * @see RuntimeInstance#getUberspect()
     */
    public static Uberspect getUberspect()
    {
        return ri.getUberspect();
    }

    /**
     * @deprecated Use getRuntimeServices() instead.
     * @return The RuntimeInstance used by this Singleton.
     */
    public static RuntimeInstance getRuntimeInstance()
    {
        return ri;
    }
    
    /**
     * Remove a directive.
     * 
     * @param name name of the directive.
     */
    public static void removeDirective(String name)
    {
        ri.removeDirective(name);
    }

    /**
     * Instantiates and loads the directive with some basic checks.
     *
     * @param directiveClass classname of directive to load
     */
    public static void loadDirective(String directiveClass)
    {
        ri.loadDirective(directiveClass);
    }
}
