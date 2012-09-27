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

import org.apache.velocity.Template;


import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import org.apache.velocity.runtime.directive.Directive;

import org.apache.velocity.runtime.resource.ContentResource;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.ParseErrorException;

import org.apache.commons.collections.ExtendedProperties;

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
 * Runtime.setProperty(Runtime.FILE_RESOURCE_LOADER_PATH, templatePath);
 * Runtime.setProperty(Runtime.RUNTIME_LOG, pathToVelocityLog);
 * Runtime.init();
 * </pre>
 *
 * <pre>
 * -----------------------------------------------------------------------
 * N O T E S  O N  R U N T I M E  I N I T I A L I Z A T I O N
 * -----------------------------------------------------------------------
 * Runtime.init()
 *
 * If Runtime.init() is called by itself the Runtime will
 * initialize with a set of default values.
 * -----------------------------------------------------------------------
 * Runtime.init(String/Properties)
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
 *
 * @see org.apache.velocity.runtime.RuntimeInstance
 * @see org.apache.velocity.runtime.RuntimeSingleton
 * @deprecated Use RuntimeInstance or RuntimeSingleton instead.
 *
 * @version $Id: Runtime.java 685390 2008-08-13 00:07:23Z nbubna $
 */
public class Runtime implements RuntimeConstants
{

    /**
     * This is the primary initialization method in the Velocity
     * Runtime. The systems that are setup/initialized here are
     * as follows:
     *
     * <ul>
     *   <li>Logging System</li>
     *   <li>ResourceManager</li>
     *   <li>Parser Pool</li>
     *   <li>Global Cache</li>
     *   <li>Static Content Include System</li>
     *   <li>Velocimacro System</li>
     * </ul>
     *
     * @throws Exception When init fails for any reason.
     */
    public synchronized static void init()
        throws Exception
    {
        RuntimeSingleton.init();
    }

    /**
     * Allows an external system to set a property in
     * the Velocity Runtime.
     *
     * @param key The property key.
     * @param value The property value.
     */
    public static void setProperty(String key, Object value)
    {
        RuntimeSingleton.setProperty( key, value );
    }

    /**
     * Allow an external system to set an ExtendedProperties
     * object to use. This is useful where the external
     * system also uses the ExtendedProperties class and
     * the velocity configuration is a subset of
     * parent application's configuration. This is
     * the case with Turbine.
     *
     * @param configuration A configuration object.
     */
    public static void setConfiguration( ExtendedProperties configuration)
    {
        RuntimeSingleton.setConfiguration( configuration );
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
     * @param key A property key.
     * @param value The property value.
     */
    public static void addProperty(String key, Object value)
    {
        RuntimeSingleton.addProperty( key, value );
    }

    /**
     * Clear the values pertaining to a particular
     * property.
     *
     * @param key Name of the property to clear.
     */
    public static void clearProperty(String key)
    {
        RuntimeSingleton.clearProperty( key );
    }

    /**
     *  Allows an external caller to get a property.  The calling
     *  routine is required to know the type, as this routine
     *  will return an Object, as that is what properties can be.
     *
     *  @param key property to return
     * @return The property value or null.
     */
    public static Object getProperty( String key )
    {
        return RuntimeSingleton.getProperty( key );
    }

    /**
     * Initialize the Velocity Runtime with a Properties
     * object.
     *
     * @param p The properties used for initializiation.
     * @throws Exception When a problem occurs during init.
     */
    public static void init(Properties p) throws Exception
    {
        RuntimeSingleton.init(p);
    }

    /**
     * Initialize the Velocity Runtime with the name of
     * ExtendedProperties object.
     *
     * @param configurationFile The name of a properties file.
     * @throws Exception When a problem occurs during init.
     */
    public static void init(String configurationFile)
        throws Exception
    {
        RuntimeSingleton.init( configurationFile );
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
     * @param reader A reader returning the template input stream.
     * @param templateName name of the template being parsed
     * @return The root node of an AST structure for the template input stream.
     * @throws ParseException When the input stream is not parsable.
     */
    public static SimpleNode parse( Reader reader, String templateName )
        throws ParseException
    {
        return RuntimeSingleton.parse( reader, templateName );
    }

    /**
     * Parse the input and return the root of the AST node structure.
     *
     * @see #parse(Reader, String)
     *
     * @param reader A reader returning the template input stream.
     * @param templateName name of the template being parsed
     * @param dumpNamespace flag to dump the Velocimacro namespace for this template.
     * @return The root node of an AST structure for the template input stream.
     * @throws ParseException When the input stream is not parsable.
     */
    public static SimpleNode parse( Reader reader, String templateName, boolean dumpNamespace )
        throws ParseException
    {
        return RuntimeSingleton.parse( reader, templateName, dumpNamespace );
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
     * @throws Exception if an error occurs in template initialization.
     */
    public static Template getTemplate(String name)
        throws ResourceNotFoundException, ParseErrorException, Exception
    {
        return RuntimeSingleton.getTemplate( name );
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
     * @throws Exception if an error occurs in template initialization
     */
    public static Template getTemplate(String name, String  encoding)
        throws ResourceNotFoundException, ParseErrorException, Exception
    {
        return RuntimeSingleton.getTemplate( name, encoding );
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
     * @throws ParseErrorException if template cannot be parsed due
     *          to syntax (or other) error.
     * @throws Exception if an error occurs in template initialization
     */
    public static ContentResource getContent(String name)
        throws ResourceNotFoundException, ParseErrorException, Exception
    {
        return RuntimeSingleton.getContent( name );
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
     * @throws ParseErrorException if template cannot be parsed due
     *          to syntax (or other) error.
     * @throws Exception if an error occurs in template initialization
     */
    public static ContentResource getContent( String name, String encoding )
        throws ResourceNotFoundException, ParseErrorException, Exception
    {
        return RuntimeSingleton.getContent( name, encoding );
    }


    /**
     *  Determines is a template exists, and returns name of the loader that
     *  provides it.  This is a slightly less hokey way to support
     *  the Velocity.templateExists() utility method, which was broken
     *  when per-template encoding was introduced.  We can revisit this.
     *
     *  @param resourceName Name of template or content resource
     *  @return class name of loader than can provide it
     */
    public static String getLoaderNameForResource( String resourceName )
    {
        return RuntimeSingleton.getLoaderNameForResource( resourceName );
    }


    /**
     * Log a warning message.
     *
     * @param message message to log
     */
    public static void warn(Object message)
    {
        RuntimeSingleton.warn( message );
    }

    /**
     * Log an info message.
     *
     * @param message message to log
     */
    public static void info(Object message)
    {
        RuntimeSingleton.info( message );
    }

    /**
     * Log an error message.
     *
     * @param message message to log
     */
    public static void error(Object message)
    {
        RuntimeSingleton.error( message );
    }

    /**
     * Log a debug message.
     *
     * @param message message to log
     */
    public static void debug(Object message)
    {
        RuntimeSingleton.debug( message );
    }

    /**
     * String property accessor method with default to hide the
     * configuration implementation.
     *
     * @param key A property key.
     * @param defaultValue  default value to return if key not
     *               found in resource manager.
     * @return The property value of of key or default.
     */
    public static String getString( String key, String defaultValue)
    {
        return RuntimeSingleton.getString( key, defaultValue );
    }

    /**
     * Returns the appropriate VelocimacroProxy object if strVMname
     * is a valid current Velocimacro.
     *
     * @param vmName  Name of velocimacro requested
     * @param templateName The template from which the macro is requested.
     * @return A VelocimacroProxy object for the macro.
     */
    public static Directive getVelocimacro( String vmName, String templateName  )
    {
        return RuntimeSingleton.getVelocimacro( vmName, templateName );
    }

   /**
     * Adds a new Velocimacro. Usually called by Macro only while parsing.
     *
     * @param name  Name of a new velocimacro.
     * @param macro String form of the macro body.
     * @param argArray  Array of strings, containing the
     *                         #macro() arguments.  the 0th argument is the name.
     * @param sourceTemplate The template from which the macro is requested.
     * @return boolean  True if added, false if rejected for some
     *                  reason (either parameters or permission settings)
     * @deprecated Just like the whole class....
     */
    public static boolean addVelocimacro( String name,
                                          String macro,
                                          String argArray[],
                                          String sourceTemplate )
    {
        return RuntimeSingleton.addVelocimacro( name, macro, argArray, sourceTemplate );
    }

    /**
     *  Checks to see if a VM exists
     *
     * @param vmName  The name of velocimacro.
     * @param templateName The template from which the macro is requested.
     * @return boolean  True if VM by that name exists, false if not
     */
    public static boolean isVelocimacro( String vmName, String templateName )
    {
        return RuntimeSingleton.isVelocimacro( vmName, templateName );
    }

    /**
     *  tells the vmFactory to dump the specified namespace.  This is to support
     *  clearing the VM list when in inline-VM-local-scope mode
     *
     * @param namespace The namespace to dump.
     * @return True if the namespace has been dumped.
     */
    public static boolean dumpVMNamespace( String namespace )
    {
        return RuntimeSingleton.dumpVMNamespace( namespace );
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
     */
    public static String getString(String key)
    {
        return RuntimeSingleton.getString( key );
    }

    /**
     * Int property accessor method to hide the configuration implementation.
     *
     * @param key A property key.
     * @return Integer value for this key.
     */
    public static int getInt( String key )
    {
        return RuntimeSingleton.getInt( key );
    }

    /**
     * Int property accessor method to hide the configuration implementation.
     *
     * @param key  property key
     * @param defaultValue default value
     * @return The integer value.
     */
    public static int getInt( String key, int defaultValue )
    {
        return RuntimeSingleton.getInt( key, defaultValue );
    }

    /**
     * Boolean property accessor method to hide the configuration implementation.
     *
     * @param key  property key
     * @param def default default value if property not found
     * @return boolean  value of key or default value
     */
    public static boolean getBoolean( String key, boolean def )
    {
        return RuntimeSingleton.getBoolean( key, def );
    }

    /**
     * Return the velocity runtime configuration object.
     *
     * @return ExtendedProperties configuration object which houses
     *                       the velocity runtime properties.
     */
    public static ExtendedProperties getConfiguration()
    {
        return RuntimeSingleton.getConfiguration();
    }
}
