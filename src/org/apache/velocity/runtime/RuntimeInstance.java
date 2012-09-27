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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.velocity.Template;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.EventHandler;
import org.apache.velocity.app.event.IncludeEventHandler;
import org.apache.velocity.app.event.InvalidReferenceEventHandler;
import org.apache.velocity.app.event.MethodExceptionEventHandler;
import org.apache.velocity.app.event.NullSetEventHandler;
import org.apache.velocity.app.event.ReferenceInsertionEventHandler;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.directive.Scope;
import org.apache.velocity.runtime.directive.StopCommand;
import org.apache.velocity.runtime.log.Log;
import org.apache.velocity.runtime.log.LogManager;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.Parser;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.runtime.resource.ContentResource;
import org.apache.velocity.runtime.resource.ResourceManager;
import org.apache.velocity.util.ClassUtils;
import org.apache.velocity.util.RuntimeServicesAware;
import org.apache.velocity.util.StringUtils;
import org.apache.velocity.util.introspection.ChainableUberspector;
import org.apache.velocity.util.introspection.Introspector;
import org.apache.velocity.util.introspection.LinkingUberspector;
import org.apache.velocity.util.introspection.Uberspect;
import org.apache.velocity.util.introspection.UberspectLoggable;

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
 * Turbine, for example, knows where the templates
 * are to be loaded from, and where the Velocity
 * log file should be placed.
 *
 * So in the case of Velocity cooperating with Turbine
 * the code might look something like the following:
 *
 * <blockquote><code><pre>
 * ri.setProperty(Runtime.FILE_RESOURCE_LOADER_PATH, templatePath);
 * ri.setProperty(Runtime.RUNTIME_LOG, pathToVelocityLog);
 * ri.init();
 * </pre></code></blockquote>
 *
 * <pre>
 * -----------------------------------------------------------------------
 * N O T E S  O N  R U N T I M E  I N I T I A L I Z A T I O N
 * -----------------------------------------------------------------------
 * init()
 *
 * If init() is called by itself the RuntimeInstance will initialize
 * with a set of default values.
 * -----------------------------------------------------------------------
 * init(String/Properties)
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
 * @version $Id: RuntimeInstance.java 898050 2010-01-11 20:15:31Z nbubna $
 */
public class RuntimeInstance implements RuntimeConstants, RuntimeServices
{
    /**
     *  VelocimacroFactory object to manage VMs
     */
    private  VelocimacroFactory vmFactory = null;

    /**
     * The Runtime logger.  We start with an instance of
     * a 'primordial logger', which just collects log messages
     * then, when the log system is initialized, all the
     * messages get dumpted out of the primordial one into the real one.
     */
    private Log log = new Log();

    /**
     * The Runtime parser pool
     */
    private  ParserPool parserPool;

    /**
     * Indicate whether the Runtime is in the midst of initialization.
     */
    private boolean initializing = false;

    /**
     * Indicate whether the Runtime has been fully initialized.
     */
    private volatile boolean initialized = false;

    /**
     * These are the properties that are laid down over top
     * of the default properties when requested.
     */
    private  ExtendedProperties overridingProperties = null;

    /**
     * This is a hashtable of initialized directives.
     * The directives that populate this hashtable are
     * taken from the RUNTIME_DEFAULT_DIRECTIVES
     * property file. 
     */
    private Map runtimeDirectives = new Hashtable();
    /**
     * Copy of the actual runtimeDirectives that is shared between
     * parsers. Whenever directives are updated, the synchronized 
     * runtimeDirectives is first updated and then an unsynchronized
     * copy of it is passed to parsers.
     */
    private Map runtimeDirectivesShared;

    /**
     * Object that houses the configuration options for
     * the velocity runtime. The ExtendedProperties object allows
     * the convenient retrieval of a subset of properties.
     * For example all the properties for a resource loader
     * can be retrieved from the main ExtendedProperties object
     * using something like the following:
     *
     * ExtendedProperties loaderConfiguration =
     *         configuration.subset(loaderID);
     *
     * And a configuration is a lot more convenient to deal
     * with then conventional properties objects, or Maps.
     */
    private  ExtendedProperties configuration = new ExtendedProperties();

    private ResourceManager resourceManager = null;

    /**
     * This stores the engine-wide set of event handlers.  Event handlers for
     * each specific merge are stored in the context.
     */
    private EventCartridge eventCartridge = null;

    /*
     *  Each runtime instance has it's own introspector
     *  to ensure that each instance is completely separate.
     */
    private Introspector introspector = null;

    /*
     * Settings for provision of root scope for evaluate(...) calls.
     */
    private String evaluateScopeName = "evaluate";
    private boolean provideEvaluateScope = false;

    /*
     *  Opaque reference to something specificed by the
     *  application for use in application supplied/specified
     *  pluggable components
     */
    private Map applicationAttributes = null;
    private Uberspect uberSpect;
    private String encoding;

    /**
     * Creates a new RuntimeInstance object.
     */
    public RuntimeInstance()
    {
        /*
         *  create a VM factory, introspector, and application attributes
         */
        vmFactory = new VelocimacroFactory( this );

        /*
         *  make a new introspector and initialize it
         */
        introspector = new Introspector(getLog());

        /*
         * and a store for the application attributes
         */
        applicationAttributes = new HashMap();
    }

    /**
     * This is the primary initialization method in the Velocity
     * Runtime. The systems that are setup/initialized here are
     * as follows:
     *
     * <ul>
     *   <li>Logging System</li>
     *   <li>ResourceManager</li>
     *   <li>EventHandler</li>
     *   <li>Parser Pool</li>
     *   <li>Global Cache</li>
     *   <li>Static Content Include System</li>
     *   <li>Velocimacro System</li>
     * </ul>
     */
    public synchronized void init()
    {
        if (!initialized && !initializing)
        {
            log.debug("Initializing Velocity, Calling init()...");
            initializing = true;

            log.trace("*******************************************************************");
            log.debug("Starting Apache Velocity v@build.version@ (compiled: @build.time@)");
            log.trace("RuntimeInstance initializing.");

            initializeProperties();
            initializeLog();
            initializeResourceManager();
            initializeDirectives();
            initializeEventHandlers();
            initializeParserPool();

            initializeIntrospection();
            initializeEvaluateScopeSettings();
            /*
             *  initialize the VM Factory.  It will use the properties
             * accessable from Runtime, so keep this here at the end.
             */
            vmFactory.initVelocimacro();

            log.trace("RuntimeInstance successfully initialized.");

            initialized = true;
            initializing = false;
        }
    }

    /**
     * Returns true if the RuntimeInstance has been successfully initialized.
     * @return True if the RuntimeInstance has been successfully initialized.
     * @since 1.5
     */
    public boolean isInitialized()
    {
        return initialized;
    }

    /**
     * Init or die! (with some log help, of course)
     */
    private void requireInitialization()
    {
        if (parserPool == null) {
            synchronized (this) {
                if (parserPool == null) {
                    try {
                        initialized = false;
                        initializing = false;
                        init();
                    } catch (Exception e) {
                        getLog().error("Could not auto-initialize Velocity", e);
                        throw new RuntimeException(
                                "Velocity could not be initialized!", e);
                    }
                    
                }
            }
            
        }
        if (!initialized && !initializing)
        {
            log.debug("Velocity was not initialized! Calling init()...");
            try
            {
                init();
            }
            catch (Exception e)
            {
                getLog().error("Could not auto-initialize Velocity", e);
                throw new RuntimeException("Velocity could not be initialized!", e);
            }
        }
    }

    /**
     *  Gets the classname for the Uberspect introspection package and
     *  instantiates an instance.
     */
    private void initializeIntrospection()
    {
        String[] uberspectors = configuration.getStringArray(RuntimeConstants.UBERSPECT_CLASSNAME);
        for (int i=0; i <uberspectors.length;i++)
        {
            String rm = uberspectors[i];
            Object o = null;

            try
            {
               o = ClassUtils.getNewInstance( rm );
            }
            catch (ClassNotFoundException cnfe)
            {
                String err = "The specified class for Uberspect (" + rm
                    + ") does not exist or is not accessible to the current classloader.";
                log.error(err);
                throw new VelocityException(err, cnfe);
            }
            catch (InstantiationException ie)
            {
              throw new VelocityException("Could not instantiate class '" + rm + "'", ie);
            }
            catch (IllegalAccessException ae)
            {
              throw new VelocityException("Cannot access class '" + rm + "'", ae);
            }

            if (!(o instanceof Uberspect))
            {
                String err = "The specified class for Uberspect ("
                    + rm + ") does not implement " + Uberspect.class.getName()
                    + "; Velocity is not initialized correctly.";

                log.error(err);
                throw new VelocityException(err);
            }

            Uberspect u = (Uberspect)o;

            if (u instanceof UberspectLoggable)
            {
                ((UberspectLoggable)u).setLog(getLog());
            }

            if (u instanceof RuntimeServicesAware)
            {
                ((RuntimeServicesAware)u).setRuntimeServices(this);
            }

            if (uberSpect == null)
            {
                uberSpect = u;
            }
            else
            {
                if (u instanceof ChainableUberspector)
                {
                    ((ChainableUberspector)u).wrap(uberSpect);
                    uberSpect = u;
                }
                else
                {
                    uberSpect = new LinkingUberspector(uberSpect,u);
                }
            }
        }

        if(uberSpect != null)
        {
            uberSpect.init();
        }
        else
        {
           /*
            *  someone screwed up.  Lets not fool around...
            */

           String err = "It appears that no class was specified as the"
           + " Uberspect.  Please ensure that all configuration"
           + " information is correct.";

           log.error(err);
           throw new VelocityException(err);
        }
    }

    /**
     * Initializes the Velocity Runtime with properties file.
     * The properties file may be in the file system proper,
     * or the properties file may be in the classpath.
     */
    private void setDefaultProperties()
    {
        InputStream inputStream = null;
        try
        {
            inputStream = getClass()
                .getResourceAsStream('/' + DEFAULT_RUNTIME_PROPERTIES);

            configuration.load( inputStream );

            if (log.isDebugEnabled())
            {
                log.debug("Default Properties File: " +
                    new File(DEFAULT_RUNTIME_PROPERTIES).getPath());
            }


        }
        catch (IOException ioe)
        {
            String msg = "Cannot get Velocity Runtime default properties!";
            log.error(msg, ioe);
            throw new RuntimeException(msg, ioe);
        }
        finally
        {
            try
            {
                if (inputStream != null)
                {
                    inputStream.close();
                }
            }
            catch (IOException ioe)
            {
                String msg = "Cannot close Velocity Runtime default properties!";
                log.error(msg, ioe);
                throw new RuntimeException(msg, ioe);
            }
        }
    }

    /**
     * Allows an external system to set a property in
     * the Velocity Runtime.
     *
     * @param key property key
     * @param  value property value
     */
    public void setProperty(String key, Object value)
    {
        if (overridingProperties == null)
        {
            overridingProperties = new ExtendedProperties();
        }

        overridingProperties.setProperty(key, value);
    }
    

    /**
     * Add all properties contained in the file fileName to the RuntimeInstance properties
     */
    public void setProperties(String fileName)
    {
        ExtendedProperties props = null;
        try
        {
              props = new ExtendedProperties(fileName);
        } 
        catch (IOException e)
        {
              throw new VelocityException("Error reading properties from '" 
                + fileName + "'", e);
        }
        
        Enumeration en = props.keys();
        while (en.hasMoreElements())
        {
            String key = en.nextElement().toString();
            setProperty(key, props.get(key));
        }
    }
    

    /**
     * Add all the properties in props to the RuntimeInstance properties
     */
    public void setProperties(Properties props)
    {
        Enumeration en = props.keys();
        while (en.hasMoreElements())
        {
            String key = en.nextElement().toString();
            setProperty(key, props.get(key));
        }
    }
        
    /**
     * Allow an external system to set an ExtendedProperties
     * object to use. This is useful where the external
     * system also uses the ExtendedProperties class and
     * the velocity configuration is a subset of
     * parent application's configuration. This is
     * the case with Turbine.
     *
     * @param  configuration
     */
    public void setConfiguration( ExtendedProperties configuration)
    {
        if (overridingProperties == null)
        {
            overridingProperties = configuration;
        }
        else
        {
            // Avoid possible ConcurrentModificationException
            if (overridingProperties != configuration)
            {
                overridingProperties.combine(configuration);
            }
        }
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
     * @param  key
     * @param  value
     */
    public void addProperty(String key, Object value)
    {
        if (overridingProperties == null)
        {
            overridingProperties = new ExtendedProperties();
        }

        overridingProperties.addProperty(key, value);
    }

    /**
     * Clear the values pertaining to a particular
     * property.
     *
     * @param key of property to clear
     */
    public void clearProperty(String key)
    {
        if (overridingProperties != null)
        {
            overridingProperties.clearProperty(key);
        }
    }

    /**
     *  Allows an external caller to get a property.  The calling
     *  routine is required to know the type, as this routine
     *  will return an Object, as that is what properties can be.
     *
     *  @param key property to return
     *  @return Value of the property or null if it does not exist.
     */
    public Object getProperty(String key)
    {
        Object o = null;
        
        /**
         * Before initialization, check the user-entered properties first.
         */
        if (!initialized && overridingProperties != null) 
        {
            o = overridingProperties.get(key);
        }
        
        /**
         * After initialization, configuration will hold all properties.
         */
        if (o == null) 
        {
            o = configuration.getProperty(key);
        }
        if (o instanceof String)
        {
            return StringUtils.nullTrim((String) o);
        }
        else
        {
            return o;
        }
    }

    /**
     * Initialize Velocity properties, if the default
     * properties have not been laid down first then
     * do so. Then proceed to process any overriding
     * properties. Laying down the default properties
     * gives a much greater chance of having a
     * working system.
     */
    private void initializeProperties()
    {
        /*
         * Always lay down the default properties first as
         * to provide a solid base.
         */
        if (configuration.isInitialized() == false)
        {
            setDefaultProperties();
        }

        if( overridingProperties != null)
        {
            configuration.combine(overridingProperties);
        }
    }

    /**
     * Initialize the Velocity Runtime with a Properties
     * object.
     *
     * @param p Velocity properties for initialization
     */
    public void init(Properties p)
    {
        setProperties(ExtendedProperties.convertProperties(p));
        init();
    }

    private void setProperties(ExtendedProperties p)
    {
        if (overridingProperties == null)
        {
            overridingProperties = p;
        }
        else
        {
            overridingProperties.combine(p);
        }
    }

    /**
     * Initialize the Velocity Runtime with the name of
     * ExtendedProperties object.
     *
     * @param configurationFile
     */
    public void init(String configurationFile)
    {
        try
        {
            setProperties(new ExtendedProperties(configurationFile));
        } 
        catch (IOException e)
        {
            throw new VelocityException("Error reading properties from '" 
                + configurationFile + "'", e);
        }
        init();
    }

    private void initializeResourceManager()
    {
        /*
         * Which resource manager?
         */      
        String rm = getString(RuntimeConstants.RESOURCE_MANAGER_CLASS);

        if (rm != null && rm.length() > 0)
        {
            /*
             *  if something was specified, then make one.
             *  if that isn't a ResourceManager, consider
             *  this a huge error and throw
             */

            Object o = null;

            try
            {
               o = ClassUtils.getNewInstance( rm );
            }
            catch (ClassNotFoundException cnfe )
            {
                String err = "The specified class for ResourceManager (" + rm
                    + ") does not exist or is not accessible to the current classloader.";
                log.error(err);
                throw new VelocityException(err, cnfe);
            }
            catch (InstantiationException ie)
            {
              throw new VelocityException("Could not instantiate class '" + rm + "'", ie);
            }
            catch (IllegalAccessException ae)
            {
              throw new VelocityException("Cannot access class '" + rm + "'", ae);
            }

            if (!(o instanceof ResourceManager))
            {
                String err = "The specified class for ResourceManager (" + rm
                    + ") does not implement " + ResourceManager.class.getName()
                    + "; Velocity is not initialized correctly.";

                log.error(err);
                throw new VelocityException(err);
            }

            resourceManager = (ResourceManager) o;

            resourceManager.initialize(this);
         }
         else
         {
            /*
             *  someone screwed up.  Lets not fool around...
             */

            String err = "It appears that no class was specified as the"
            + " ResourceManager.  Please ensure that all configuration"
            + " information is correct.";

            log.error(err);
            throw new VelocityException( err );
        }
    }

    private void initializeEventHandlers()
    {

        eventCartridge = new EventCartridge();

        /**
         * For each type of event handler, get the class name, instantiate it, and store it.
         */

        String[] referenceinsertion = configuration.getStringArray(RuntimeConstants.EVENTHANDLER_REFERENCEINSERTION);
        if ( referenceinsertion != null )
        {
            for ( int i=0; i < referenceinsertion.length; i++ )
            {
                EventHandler ev = initializeSpecificEventHandler(referenceinsertion[i],RuntimeConstants.EVENTHANDLER_REFERENCEINSERTION,ReferenceInsertionEventHandler.class);
                if (ev != null)
                    eventCartridge.addReferenceInsertionEventHandler((ReferenceInsertionEventHandler) ev);
            }
        }

        String[] nullset = configuration.getStringArray(RuntimeConstants.EVENTHANDLER_NULLSET);
        if ( nullset != null )
        {
            for ( int i=0; i < nullset.length; i++ )
            {
                EventHandler ev = initializeSpecificEventHandler(nullset[i],RuntimeConstants.EVENTHANDLER_NULLSET,NullSetEventHandler.class);
                if (ev != null)
                    eventCartridge.addNullSetEventHandler((NullSetEventHandler) ev);
            }
        }

        String[] methodexception = configuration.getStringArray(RuntimeConstants.EVENTHANDLER_METHODEXCEPTION);
        if ( methodexception != null )
        {
            for ( int i=0; i < methodexception.length; i++ )
            {
                EventHandler ev = initializeSpecificEventHandler(methodexception[i],RuntimeConstants.EVENTHANDLER_METHODEXCEPTION,MethodExceptionEventHandler.class);
                if (ev != null)
                    eventCartridge.addMethodExceptionHandler((MethodExceptionEventHandler) ev);
            }
        }

        String[] includeHandler = configuration.getStringArray(RuntimeConstants.EVENTHANDLER_INCLUDE);
        if ( includeHandler != null )
        {
            for ( int i=0; i < includeHandler.length; i++ )
            {
                EventHandler ev = initializeSpecificEventHandler(includeHandler[i],RuntimeConstants.EVENTHANDLER_INCLUDE,IncludeEventHandler.class);
                if (ev != null)
                    eventCartridge.addIncludeEventHandler((IncludeEventHandler) ev);
            }
        }

        String[] invalidReferenceSet = configuration.getStringArray(RuntimeConstants.EVENTHANDLER_INVALIDREFERENCES);
        if ( invalidReferenceSet != null )
        {
            for ( int i=0; i < invalidReferenceSet.length; i++ )
            {
                EventHandler ev = initializeSpecificEventHandler(invalidReferenceSet[i],RuntimeConstants.EVENTHANDLER_INVALIDREFERENCES,InvalidReferenceEventHandler.class);
                if (ev != null)
                {
                    eventCartridge.addInvalidReferenceEventHandler((InvalidReferenceEventHandler) ev);
                }
            }
        }


    }

    private EventHandler initializeSpecificEventHandler(String classname, String paramName, Class EventHandlerInterface)
    {
        if ( classname != null && classname.length() > 0)
        {
            Object o = null;
            try 
            {
                o = ClassUtils.getNewInstance(classname);
            }
            catch (ClassNotFoundException cnfe )
            {
                String err = "The specified class for "
                    + paramName + " (" + classname
                    + ") does not exist or is not accessible to the current classloader.";
                log.error(err);
                throw new VelocityException(err, cnfe);
            }
            catch (InstantiationException ie)
            {
              throw new VelocityException("Could not instantiate class '" + classname + "'", ie);
            }
            catch (IllegalAccessException ae)
            {
              throw new VelocityException("Cannot access class '" + classname + "'", ae);
            }

            if (!EventHandlerInterface.isAssignableFrom(EventHandlerInterface))
            {
                String err = "The specified class for " + paramName + " ("
                    + classname + ") does not implement "
                    + EventHandlerInterface.getName()
                    + "; Velocity is not initialized correctly.";

                log.error(err);
                throw new VelocityException(err);
            }

            EventHandler ev = (EventHandler) o;
            if ( ev instanceof RuntimeServicesAware )
                ((RuntimeServicesAware) ev).setRuntimeServices(this);
            return ev;

        } else
            return null;
    }

    /**
     * Initialize the Velocity logging system.
     */
    private void initializeLog()
    {
        // since the Log we started with was just placeholding,
        // let's update it with the real LogChute settings.
        try
        {
            LogManager.updateLog(this.log, this);
        } 
        catch (Exception e)
        {
            throw new VelocityException("Error initializing log: " + e.getMessage(), e);
        }
    }


    /**
     * This methods initializes all the directives
     * that are used by the Velocity Runtime. The
     * directives to be initialized are listed in
     * the RUNTIME_DEFAULT_DIRECTIVES properties
     * file.
     */
    private void initializeDirectives()
    {
        Properties directiveProperties = new Properties();

        /*
         * Grab the properties file with the list of directives
         * that we should initialize.
         */

        InputStream inputStream = null;

        try
        {
            inputStream = getClass().getResourceAsStream('/' + DEFAULT_RUNTIME_DIRECTIVES);

            if (inputStream == null)
            {
                throw new VelocityException("Error loading directive.properties! " +
                                    "Something is very wrong if these properties " +
                                    "aren't being located. Either your Velocity " +
                                    "distribution is incomplete or your Velocity " +
                                    "jar file is corrupted!");
            }

            directiveProperties.load(inputStream);

        }
        catch (IOException ioe)
        {
            String msg = "Error while loading directive properties!";
            log.error(msg, ioe);
            throw new RuntimeException(msg, ioe);
        }
        finally
        {
            try
            {
                if (inputStream != null)
                {
                    inputStream.close();
                }
            }
            catch (IOException ioe)
            {
                String msg = "Cannot close directive properties!";
                log.error(msg, ioe);
                throw new RuntimeException(msg, ioe);
            }
        }


        /*
         * Grab all the values of the properties. These
         * are all class names for example:
         *
         * org.apache.velocity.runtime.directive.Foreach
         */
        Enumeration directiveClasses = directiveProperties.elements();

        while (directiveClasses.hasMoreElements())
        {
            String directiveClass = (String) directiveClasses.nextElement();
            loadDirective(directiveClass);
            log.debug("Loaded System Directive: " + directiveClass);
        }

        /*
         *  now the user's directives
         */

        String[] userdirective = configuration.getStringArray("userdirective");

        for( int i = 0; i < userdirective.length; i++)
        {
            loadDirective(userdirective[i]);
            if (log.isDebugEnabled())
            {
                log.debug("Loaded User Directive: " + userdirective[i]);
            }
        }

    }

    /**
     * Programatically add a directive.
     * @param directive
     */
    public synchronized void addDirective(Directive directive) 
    {
        runtimeDirectives.put(directive.getName(), directive);
        updateSharedDirectivesMap();
    }

    /**
     * Retrieve a previously instantiated directive.
     * @param name name of the directive
     * @return the {@link Directive} for that name
     */
    public Directive getDirective(String name) 
    {
        return (Directive) runtimeDirectivesShared.get(name);
    }

    /**
     * Remove a directive.
     * @param name name of the directive.
     */
    public synchronized void removeDirective(String name) 
    {
        runtimeDirectives.remove(name);
        updateSharedDirectivesMap();
    }
    
    /**
     * Makes an unsynchronized copy of the directives map
     * that is used for Directive lookups by all parsers.
     * 
     * This follows Copy-on-Write pattern. The cost of creating
     * a new map is acceptable since directives are typically
     * set and modified only during Velocity setup phase.
     */
    private void updateSharedDirectivesMap()
    {
        Map tmp = new HashMap(runtimeDirectives);
        runtimeDirectivesShared = tmp;
    }

    /**
     *  instantiates and loads the directive with some basic checks
     *
     *  @param directiveClass classname of directive to load
     */
    public void loadDirective(String directiveClass)
    {
        try
        {
            Object o = ClassUtils.getNewInstance( directiveClass );

            if (o instanceof Directive)
            {
                Directive directive = (Directive) o;
                addDirective(directive);
            }
            else
            {
                String msg = directiveClass + " does not implement "
                    + Directive.class.getName() + "; it cannot be loaded.";
                log.error(msg);
                throw new VelocityException(msg);
            }
        }
        // The ugly threesome:  ClassNotFoundException,
        // IllegalAccessException, InstantiationException.
        // Ignore Findbugs complaint for now.
        catch (Exception e)
        {
            String msg = "Failed to load Directive: " + directiveClass;
            log.error(msg, e);
            throw new VelocityException(msg, e);
        }
    }


    /**
     * Initializes the Velocity parser pool.
     */
    private void initializeParserPool()
    {
        /*
         * Which parser pool?
         */
        String pp = getString(RuntimeConstants.PARSER_POOL_CLASS);

        if (pp != null && pp.length() > 0)
        {
            /*
             *  if something was specified, then make one.
             *  if that isn't a ParserPool, consider
             *  this a huge error and throw
             */

            Object o = null;

            try
            {
                o = ClassUtils.getNewInstance( pp );
            }
            catch (ClassNotFoundException cnfe )
            {
                String err = "The specified class for ParserPool ("
                    + pp
                    + ") does not exist (or is not accessible to the current classloader.";
                log.error(err);
                throw new VelocityException(err, cnfe);
            }
            catch (InstantiationException ie)
            {
              throw new VelocityException("Could not instantiate class '" + pp + "'", ie);
            }
            catch (IllegalAccessException ae)
            {
              throw new VelocityException("Cannot access class '" + pp + "'", ae);
            }

            if (!(o instanceof ParserPool))
            {
                String err = "The specified class for ParserPool ("
                    + pp + ") does not implement " + ParserPool.class
                    + " Velocity not initialized correctly.";

                log.error(err);
                throw new VelocityException(err);
            }

            parserPool = (ParserPool) o;

            parserPool.initialize(this);
        }
        else
        {
            /*
             *  someone screwed up.  Lets not fool around...
             */

            String err = "It appears that no class was specified as the"
                + " ParserPool.  Please ensure that all configuration"
                + " information is correct.";

            log.error(err);
            throw new VelocityException( err );
        }

    }

    /**
     * Returns a JavaCC generated Parser.
     *
     * @return Parser javacc generated parser
     */
    public Parser createNewParser()
    {
        requireInitialization();

        Parser parser = new Parser(this);
        return parser;
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
     * @param string String to be parsed
     * @param templateName name of the template being parsed
     * @return A root node representing the template as an AST tree.
     * @throws ParseException When the string could not be parsed as a template.
     * @since 1.6
     */
    public SimpleNode parse(String string, String templateName)
        throws ParseException
    {
        return parse(new StringReader(string), templateName);
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
     */
    public SimpleNode parse(Reader reader, String templateName)
        throws ParseException
    {
        /*
         *  do it and dump the VM namespace for this template
         */
        return parse(reader, templateName, true);
    }

    /**
     *  Parse the input and return the root of the AST node structure.
     *
     * @param reader Reader retrieved by a resource loader
     * @param templateName name of the template being parsed
     * @param dumpNamespace flag to dump the Velocimacro namespace for this template
     * @return A root node representing the template as an AST tree.
     * @throws ParseException When the template could not be parsed.
     */
    public SimpleNode parse(Reader reader, String templateName, boolean dumpNamespace)
        throws ParseException
    {
        requireInitialization();

        Parser parser = (Parser) parserPool.get();
        boolean keepParser = true;
        if (parser == null)
        {
            /*
             *  if we couldn't get a parser from the pool make one and log it.
             */
            if (log.isInfoEnabled())
            {
                log.info("Runtime : ran out of parsers. Creating a new one. "
                      + " Please increment the parser.pool.size property."
                      + " The current value is too small.");
            }
            parser = createNewParser();
            keepParser = false;
        }

        try
        {
            /*
             *  dump namespace if we are told to.  Generally, you want to
             *  do this - you don't in special circumstances, such as
             *  when a VM is getting init()-ed & parsed
             */
            if (dumpNamespace)
            {
                dumpVMNamespace(templateName);
            }
            return parser.parse(reader, templateName);
        }
        finally
        {
            if (keepParser)
            {
                parserPool.put(parser);
            }

        }
    }

    private void initializeEvaluateScopeSettings()
    {
        String property = evaluateScopeName+'.'+PROVIDE_SCOPE_CONTROL;
        provideEvaluateScope = getBoolean(property, provideEvaluateScope);
    }

    /**
     * Renders the input string using the context into the output writer.
     * To be used when a template is dynamically constructed, or want to use
     * Velocity as a token replacer.
     *
     * @param context context to use in rendering input string
     * @param out  Writer in which to render the output
     * @param logTag  string to be used as the template name for log
     *                messages in case of error
     * @param instring input string containing the VTL to be rendered
     *
     * @return true if successful, false otherwise.  If false, see
     *              Velocity runtime log
     * @throws ParseErrorException The template could not be parsed.
     * @throws MethodInvocationException A method on a context object could not be invoked.
     * @throws ResourceNotFoundException A referenced resource could not be loaded.
     * @since Velocity 1.6
     */
    public boolean evaluate(Context context,  Writer out,
                            String logTag, String instring)
    {
        return evaluate(context, out, logTag, new StringReader(instring));
    }

    /**
     * Renders the input reader using the context into the output writer.
     * To be used when a template is dynamically constructed, or want to
     * use Velocity as a token replacer.
     *
     * @param context context to use in rendering input string
     * @param writer  Writer in which to render the output
     * @param logTag  string to be used as the template name for log messages
     *                in case of error
     * @param reader Reader containing the VTL to be rendered
     *
     * @return true if successful, false otherwise.  If false, see
     *              Velocity runtime log
     * @throws ParseErrorException The template could not be parsed.
     * @throws MethodInvocationException A method on a context object could not be invoked.
     * @throws ResourceNotFoundException A referenced resource could not be loaded.
     * @since Velocity 1.6
     */
    public boolean evaluate(Context context, Writer writer,
                            String logTag, Reader reader)
    {
        if (logTag == null)
        {
            throw new NullPointerException("logTag (i.e. template name) cannot be null, you must provide an identifier for the content being evaluated");
        }

        SimpleNode nodeTree = null;
        try
        {
            nodeTree = parse(reader, logTag);
        }
        catch (ParseException pex)
        {
            throw new ParseErrorException(pex, null);
        }
        catch (TemplateInitException pex)
        {
            throw new ParseErrorException(pex, null);
        }

        if (nodeTree == null)
        {
            return false;
        }
        else
        {
            return render(context, writer, logTag, nodeTree);
        }
    }


    /**
     * Initializes and renders the AST {@link SimpleNode} using the context
     * into the output writer.
     *
     * @param context context to use in rendering input string
     * @param writer  Writer in which to render the output
     * @param logTag  string to be used as the template name for log messages
     *                in case of error
     * @param nodeTree SimpleNode which is the root of the AST to be rendered
     *
     * @return true if successful, false otherwise.  If false, see
     *              Velocity runtime log for errors
     * @throws ParseErrorException The template could not be parsed.
     * @throws MethodInvocationException A method on a context object could not be invoked.
     * @throws ResourceNotFoundException A referenced resource could not be loaded.
     * @since Velocity 1.6
     */
    public boolean render(Context context, Writer writer,
                          String logTag, SimpleNode nodeTree)
    {
        /*
         * we want to init then render
         */
        InternalContextAdapterImpl ica =
            new InternalContextAdapterImpl(context);

        ica.pushCurrentTemplateName(logTag);

        try
        {
            try
            {
                nodeTree.init(ica, this);
            }
            catch (TemplateInitException pex)
            {
                throw new ParseErrorException(pex, null);
            }
            /**
             * pass through application level runtime exceptions
             */
            catch(RuntimeException e)
            {
                throw e;
            }
            catch(Exception e)
            {
                String msg = "RuntimeInstance.render(): init exception for tag = "+logTag;
                getLog().error(msg, e);
                throw new VelocityException(msg, e);
            }

            try
            {
                if (provideEvaluateScope)
                {
                    Object previous = ica.get(evaluateScopeName);
                    context.put(evaluateScopeName, new Scope(this, previous));
                }
                nodeTree.render(ica, writer);
            }
            catch (StopCommand stop)
            {
                if (!stop.isFor(this))
                {
                    throw stop;
                }
                else if (getLog().isDebugEnabled())
                {
                    getLog().debug(stop.getMessage());
                }
            }
            catch (IOException e)
            {
                throw new VelocityException("IO Error in writer: " + e.getMessage(), e);
            }
        }
        finally
        {
            ica.popCurrentTemplateName();
            if (provideEvaluateScope)
            {
                Object obj = ica.get(evaluateScopeName);
                if (obj instanceof Scope)
                {
                    Scope scope = (Scope)obj;
                    if (scope.getParent() != null)
                    {
                        ica.put(evaluateScopeName, scope.getParent());
                    }
                    else if (scope.getReplaced() != null)
                    {
                        ica.put(evaluateScopeName, scope.getReplaced());
                    }
                    else
                    {
                        ica.remove(evaluateScopeName);
                    }
                }
            }
        }

        return true;
    }

    /**
     * Invokes a currently registered Velocimacro with the params provided
     * and places the rendered stream into the writer.
     * <br>
     * Note : currently only accepts args to the VM if they are in the context.
     *
     * @param vmName name of Velocimacro to call
     * @param logTag string to be used for template name in case of error. if null,
     *               the vmName will be used
     * @param params keys for args used to invoke Velocimacro, in java format
     *               rather than VTL (eg  "foo" or "bar" rather than "$foo" or "$bar")
     * @param context Context object containing data/objects used for rendering.
     * @param writer  Writer for output stream
     * @return true if Velocimacro exists and successfully invoked, false otherwise.
     * @since 1.6
     */
    public boolean invokeVelocimacro(final String vmName, String logTag,
                                     String[] params, final Context context,
                                     final Writer writer)
     {
        /* check necessary parameters */
        if (vmName == null || context == null || writer == null)
        {
            String msg = "RuntimeInstance.invokeVelocimacro() : invalid call : vmName, context, and writer must not be null";
            getLog().error(msg);
            throw new NullPointerException(msg);
        }

        /* handle easily corrected parameters */
        if (logTag == null)
        {
            logTag = vmName;
        }
        if (params == null)
        {
            params = new String[0];
        }

        /* does the VM exist? */
        if (!isVelocimacro(vmName, logTag))
        {
            String msg = "RuntimeInstance.invokeVelocimacro() : VM '" + vmName
                         + "' is not registered.";
            getLog().error(msg);
            throw new VelocityException(msg);
        }

        /* now just create the VM call, and use evaluate */
        StrBuilder template = new StrBuilder("#");
        template.append(vmName);
        template.append("(");
        for( int i = 0; i < params.length; i++)
        {
            template.append(" $");
            template.append(params[i]);
        }
        template.append(" )");

        return evaluate(context, writer, logTag, template.toString());
    }

    /**
     * Retrieves and caches the configured default encoding
     * for better performance. (VELOCITY-606)
     */
    private String getDefaultEncoding()
    {
        if (encoding == null)
        {
            encoding = getString(INPUT_ENCODING, ENCODING_DEFAULT);
        }
        return encoding;
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
     */
    public Template getTemplate(String name)
        throws ResourceNotFoundException, ParseErrorException
    {
        return getTemplate(name, getDefaultEncoding());
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
     */
    public Template getTemplate(String name, String  encoding)
        throws ResourceNotFoundException, ParseErrorException
    {
        requireInitialization();

        return (Template)
                resourceManager.getResource(name,
                    ResourceManager.RESOURCE_TEMPLATE, encoding);
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
     */
    public ContentResource getContent(String name)
        throws ResourceNotFoundException, ParseErrorException
    {
        /*
         *  the encoding is irrelvant as we don't do any converstion
         *  the bytestream should be dumped to the output stream
         */

        return getContent(name, getDefaultEncoding());
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
     */
    public ContentResource getContent(String name, String encoding)
        throws ResourceNotFoundException, ParseErrorException
    {
        requireInitialization();

        return (ContentResource)
                resourceManager.getResource(name,
                        ResourceManager.RESOURCE_CONTENT, encoding);
    }


    /**
     *  Determines if a template exists and returns name of the loader that
     *  provides it.  This is a slightly less hokey way to support
     *  the Velocity.resourceExists() utility method, which was broken
     *  when per-template encoding was introduced.  We can revisit this.
     *
     *  @param resourceName Name of template or content resource
     *  @return class name of loader than can provide it
     */
    public String getLoaderNameForResource(String resourceName)
    {
        requireInitialization();

        return resourceManager.getLoaderNameForResource(resourceName);
    }

    /**
     * Returns a convenient Log instance that wraps the current LogChute.
     * Use this to log error messages. It has the usual methods.
     *
     * @return A convenience Log instance that wraps the current LogChute.
     * @since 1.5
     */
    public Log getLog()
    {
        return log;
    }

    /**
     * @deprecated Use getLog() and call warn() on it.
     * @see Log#warn(Object)
     * @param message The message to log.
     */
    public void warn(Object message)
    {
        getLog().warn(message);
    }

    /**
     * @deprecated Use getLog() and call info() on it.
     * @see Log#info(Object)
     * @param message The message to log.
     */
    public void info(Object message)
    {
        getLog().info(message);
    }

    /**
     * @deprecated Use getLog() and call error() on it.
     * @see Log#error(Object)
     * @param message The message to log.
     */
    public void error(Object message)
    {
        getLog().error(message);
    }

    /**
     * @deprecated Use getLog() and call debug() on it.
     * @see Log#debug(Object)
     * @param message The message to log.
     */
    public void debug(Object message)
    {
        getLog().debug(message);
    }

    /**
     * String property accessor method with default to hide the
     * configuration implementation.
     *
     * @param key property key
     * @param defaultValue  default value to return if key not
     *               found in resource manager.
     * @return value of key or default
     */
    public String getString( String key, String defaultValue)
    {
        return configuration.getString(key, defaultValue);
    }

    /**
     * Returns the appropriate VelocimacroProxy object if vmName
     * is a valid current Velocimacro.
     *
     * @param vmName Name of velocimacro requested
     * @param templateName Name of the template that contains the velocimacro.
     * @return The requested VelocimacroProxy.
     * @since 1.6
     */
    public Directive getVelocimacro(String vmName, String templateName)
    {
        return vmFactory.getVelocimacro( vmName, templateName );
    }

    /**
     * Returns the appropriate VelocimacroProxy object if vmName
     * is a valid current Velocimacro.
     *
     * @param vmName  Name of velocimacro requested
     * @param templateName Name of the namespace.
     * @param renderingTemplate Name of the template we are currently rendering. This
     *    information is needed when VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL setting is true
     *    and template contains a macro with the same name as the global macro library.
     * 
     * @since Velocity 1.6
     * 
     * @return VelocimacroProxy
     */
    public Directive getVelocimacro(String vmName, String templateName, String renderingTemplate)
    {
        return vmFactory.getVelocimacro( vmName, templateName, renderingTemplate );
    }
    
    
   /**
    * Adds a new Velocimacro. Usually called by Macro only while parsing.
    *
    * @param name Name of velocimacro
    * @param macro String form of macro body
    * @param argArray Array of strings, containing the
    *                         #macro() arguments.  the 0th is the name.
    * @param sourceTemplate Name of the template that contains the velocimacro.
    * 
    * @deprecated Use addVelocimacro(String, Node, String[], String) instead
    * 
    * @return True if added, false if rejected for some
    *                  reason (either parameters or permission settings)
    */
    public boolean addVelocimacro( String name,
                                          String macro,
                                          String argArray[],
                                          String sourceTemplate )
    {
        return vmFactory.addVelocimacro(name.intern(), macro,  argArray,  sourceTemplate);
    }

    /**
     * Adds a new Velocimacro. Usually called by Macro only while parsing.
     * 
     * Called by org.apache.velocity.runtime.directive.processAndRegister
     *
     * @param name  Name of velocimacro
     * @param macro  root AST node of the parsed macro
     * @param argArray  Array of strings, containing the
     *                         #macro() arguments.  the 0th is the name.
     * @param sourceTemplate
     * 
     * @since Velocity 1.6
     *                   
     * @return boolean  True if added, false if rejected for some
     *                  reason (either parameters or permission settings)
     */
    public boolean addVelocimacro( String name,
                                          Node macro,
                                          String argArray[],
                                          String sourceTemplate )
    {
        return vmFactory.addVelocimacro(name.intern(), macro,  argArray,  sourceTemplate);
    }
    
    
    /**
     *  Checks to see if a VM exists
     *
     * @param vmName Name of the Velocimacro.
     * @param templateName Template on which to look for the Macro.
     * @return True if VM by that name exists, false if not
     */
    public boolean isVelocimacro( String vmName, String templateName )
    {
        return vmFactory.isVelocimacro(vmName.intern(), templateName);
    }

    /**
     * tells the vmFactory to dump the specified namespace.  This is to support
     * clearing the VM list when in inline-VM-local-scope mode
     * @param namespace Namespace to dump.
     * @return True if namespace was dumped successfully.
     */
    public boolean dumpVMNamespace(String namespace)
    {
        return vmFactory.dumpVMNamespace( namespace );
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
    public String getString(String key)
    {
        return StringUtils.nullTrim(configuration.getString(key));
    }

    /**
     * Int property accessor method to hide the configuration implementation.
     *
     * @param key Property key
     * @return value
     */
    public int getInt(String key)
    {
        return configuration.getInt(key);
    }

    /**
     * Int property accessor method to hide the configuration implementation.
     *
     * @param key  property key
     * @param defaultValue The default value.
     * @return value
     */
    public int getInt(String key, int defaultValue)
    {
        return configuration.getInt(key, defaultValue);
    }

    /**
     * Boolean property accessor method to hide the configuration implementation.
     *
     * @param key property key
     * @param def The default value if property not found.
     * @return value of key or default value
     */
    public boolean getBoolean(String key, boolean def)
    {
        return configuration.getBoolean(key, def);
    }

    /**
     * Return the velocity runtime configuration object.
     *
     * @return Configuration object which houses the Velocity runtime
     * properties.
     */
    public ExtendedProperties getConfiguration()
    {
        return configuration;
    }

    /**
     *  Return the Introspector for this instance
     * @return The Introspector for this instance
     */
    public Introspector getIntrospector()
    {
        return introspector;
    }

    /**
     * Returns the event handlers for the application.
     * @return The event handlers for the application.
     * @since 1.5
     */
    public EventCartridge getApplicationEventCartridge()
    {
        return eventCartridge;
    }


    /**
     *  Gets the application attribute for the given key
     *
     * @param key
     * @return The application attribute for the given key.
     */
    public Object getApplicationAttribute(Object key)
    {
        return applicationAttributes.get(key);
    }

    /**
     *   Sets the application attribute for the given key
     *
     * @param key
     * @param o The new application attribute.
     * @return The old value of this attribute or null if it hasn't been set before.
     */
    public Object setApplicationAttribute(Object key, Object o)
    {
        return applicationAttributes.put(key, o);
    }

    /**
     * Returns the Uberspect object for this Instance.
     *
     * @return The Uberspect object for this Instance.
     */
    public Uberspect getUberspect()
    {
        return uberSpect;
    }

}
