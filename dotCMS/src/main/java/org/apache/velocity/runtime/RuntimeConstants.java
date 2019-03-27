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

/**
 * This class defines the keys that are used in the velocity.properties file so that they can be referenced as a constant within
 * Java code.
 *
 * @author  <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author  <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @author  <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @version  $Id: RuntimeConstants.java 806601 2009-08-21 15:30:38Z nbubna $
 */
public interface RuntimeConstants
{
    /*
     * ----------------------------------------------------------------------
     * These are public constants that are used as handles for the
     * properties that can be specified in your typical
     * velocity.properties file.
     * ----------------------------------------------------------------------
     */

    /*
     * ----------------------------------------------------------------------
     * L O G G I N G  C O N F I G U R A T I O N
     * ----------------------------------------------------------------------
     */

    /** Location of the velocity log file. */
    String RUNTIME_LOG = "runtime.log";

    /** externally provided logger. */
    String RUNTIME_LOG_LOGSYSTEM = "runtime.log.logsystem";

    /** class of log system to use. */
    String RUNTIME_LOG_LOGSYSTEM_CLASS = "runtime.log.logsystem.class";

    /**
     * Properties referenced in the template are required to exist the object
     */
    String RUNTIME_REFERENCES_STRICT = "runtime.references.strict";
    
    /**
     * Indicates we are going to use modifed escape behavior in strict mode
     */
    String RUNTIME_REFERENCES_STRICT_ESCAPE = "runtime.references.strict.escape";
       
    /**
     * @deprecated  This appears to have always been meaningless.
     */
    String RUNTIME_LOG_ERROR_STACKTRACE = "runtime.log.error.stacktrace";

    /**
     * @deprecated  The functionality this controlled is confusing and no longer necessary.
     */
    String RUNTIME_LOG_WARN_STACKTRACE = "runtime.log.warn.stacktrace";

    /**
     * @deprecated  This appears to have always been meaningless.
     */
    String RUNTIME_LOG_INFO_STACKTRACE = "runtime.log.info.stacktrace";

    /** Logging of invalid references. */
    String RUNTIME_LOG_REFERENCE_LOG_INVALID = "runtime.log.invalid.references";

    /**
     * @deprecated  Use LogChute.TRACE_PREFIX instead
     */
    String TRACE_PREFIX = " [trace] ";

    /**
     * @deprecated  Use LogChute.DEBUG_PREFIX instead
     */
    String DEBUG_PREFIX = " [debug] ";

    /**
     * @deprecated  Use LogChute.INFO_PREFIX instead
     */
    String INFO_PREFIX = "  [info] ";

    /**
     * @deprecated  Use LogChute.WARN_PREFIX instead
     */
    String WARN_PREFIX = "  [warn] ";

    /**
     * @deprecated  Use LogChute.ERROR_PREFIX instead
     */
    String ERROR_PREFIX = " [error] ";

    /**
     * @deprecated  This will be removed in a future version
     */
    String UNKNOWN_PREFIX = " [unknown] ";

    /*
     * ----------------------------------------------------------------------
     * D I R E C T I V E  C O N F I G U R A T I O N
     * ----------------------------------------------------------------------
     * Directive properties are of the form:
     *
     * directive.<directive-name>.<property>
     * ----------------------------------------------------------------------
     */

    /** Counter reference name in #foreach directives. */
    String COUNTER_NAME = "directive.foreach.counter.name";

    /**
     * Iterator.hasNext() reference name in #foreach directives.
     * @since 1.6
     */
    String HAS_NEXT_NAME = "directive.foreach.iterator.name";

    /** Initial counter value in #foreach directives. */
    String COUNTER_INITIAL_VALUE = "directive.foreach.counter.initial.value";

    /** Maximum allowed number of loops. */
    String MAX_NUMBER_LOOPS = "directive.foreach.maxloops";

    /**
     * Whether to throw an exception or just skip bad iterables. Default is true.
     * @since 1.6
     */
    String SKIP_INVALID_ITERATOR = "directive.foreach.skip.invalid";

    /** if set to true then allows #set to accept null values in the right hand side. */
    String SET_NULL_ALLOWED = "directive.set.null.allowed";

    /**
     * Indicates if toString() should be called during #if condition evaluation
     * just to ensure it does not return null. Check is unnecessary if all
     * toString() implementations are known to have non-null return values.
     * Disabling the check (like Velocity 1.5 did) will can boost performance
     * since toString() may be a complex operation on large objects.
     * @since 1.6
     */
    String DIRECTIVE_IF_TOSTRING_NULLCHECK = "directive.if.tostring.nullcheck";

    /**
     * Starting tag for error messages triggered by passing a parameter not allowed in the #include directive. Only string literals,
     * and references are allowed.
     */
    String ERRORMSG_START = "directive.include.output.errormsg.start";

    /**
     * Ending tag for error messages triggered by passing a parameter not allowed in the #include directive. Only string literals,
     * and references are allowed.
     */
    String ERRORMSG_END = "directive.include.output.errormsg.end";

    /** Maximum recursion depth allowed for the #parse directive. */
    String PARSE_DIRECTIVE_MAXDEPTH = "directive.parse.max.depth";

    /** Maximum recursion depth allowed for the #define directive. */
    String DEFINE_DIRECTIVE_MAXDEPTH = "directive.define.max.depth";

    /**
     * class to use for local context with #evaluate()
     * @since 1.6
     */
    String EVALUATE_CONTEXT_CLASS = "directive.evaluate.context.class";

    /**
     * Used to suppress various scope control objects.
     * @since 1.7
     */
    String PROVIDE_SCOPE_CONTROL = "provide.scope.control";

    /*
     * ----------------------------------------------------------------------
     *  R E S O U R C E   M A N A G E R   C O N F I G U R A T I O N
     * ----------------------------------------------------------------------
     */

    /**  */
    String RESOURCE_MANAGER_CLASS = "resource.manager.class";

    /**
     * The <code>resource.manager.cache.class</code> property specifies the name of the
     * {@link org.apache.velocity.runtime.resource.ResourceCache} implementation to use.
     */
    String RESOURCE_MANAGER_CACHE_CLASS = "resource.manager.cache.class";

    /** The <code>resource.manager.cache.size</code> property specifies the cache upper bound (if relevant). */
    String RESOURCE_MANAGER_DEFAULTCACHE_SIZE = "resource.manager.defaultcache.size";

    /*
     * ----------------------------------------------------------------------
     * R E S O U R C E  L O A D E R  C O N F I G U R A T I O N
     * ----------------------------------------------------------------------
     */

    /** controls if the finding of a resource is logged. */
    String RESOURCE_MANAGER_LOGWHENFOUND = "resource.manager.logwhenfound";

    /**
     * Key used to retrieve the names of the resource loaders to be used. In a properties file they may appear as the following:
     *
     * <p>resource.loader = file,classpath</p>
     */
    String RESOURCE_LOADER = "resource.loader";

    /** The public handle for setting a path in the FileResourceLoader. */
    String FILE_RESOURCE_LOADER_PATH = "file.resource.loader.path";

    /** The public handle for turning the caching on in the FileResourceLoader. */
    String FILE_RESOURCE_LOADER_CACHE = "file.resource.loader.cache";

    /*
     * ----------------------------------------------------------------------
     *  E V E N T  H A N D L E R  C O N F I G U R A T I O N
     * ----------------------------------------------------------------------
     */

    /**
     * The <code>eventhandler.referenceinsertion.class</code> property specifies a list of the
     * {@link org.apache.velocity.app.event.ReferenceInsertionEventHandler} implementations to use.
     */
    String EVENTHANDLER_REFERENCEINSERTION = "eventhandler.referenceinsertion.class";

    /**
     * The <code>eventhandler.nullset.class</code> property specifies a list of the
     * {@link org.apache.velocity.app.event.NullSetEventHandler} implementations to use.
     */
    String EVENTHANDLER_NULLSET = "eventhandler.nullset.class";

    /**
     * The <code>eventhandler.methodexception.class</code> property specifies a list of the
     * {@link org.apache.velocity.app.event.MethodExceptionEventHandler} implementations to use.
     */
    String EVENTHANDLER_METHODEXCEPTION = "eventhandler.methodexception.class";

    /**
     * The <code>eventhandler.include.class</code> property specifies a list of the
     * {@link org.apache.velocity.app.event.IncludeEventHandler} implementations to use.
     */
    String EVENTHANDLER_INCLUDE = "eventhandler.include.class";

    /**
     * The <code>eventhandler.invalidreferences.class</code> property specifies a list of the
     * {@link org.apache.velocity.app.event.InvalidReferenceEventHandler} implementations to use.
     */
    String EVENTHANDLER_INVALIDREFERENCES = "eventhandler.invalidreferences.class";


    /*
     * ----------------------------------------------------------------------
     * V E L O C I M A C R O  C O N F I G U R A T I O N
     * ----------------------------------------------------------------------
     */

    /** Name of local Velocimacro library template. */
    String VM_LIBRARY = "velocimacro.library";

    /** Default Velocimacro library template. */
    String VM_LIBRARY_DEFAULT = "VM_global_library.vm";

    /** switch for autoloading library-sourced VMs (for development). */
    String VM_LIBRARY_AUTORELOAD = "velocimacro.library.autoreload";

    /** boolean (true/false) default true : allow inline (in-template) macro definitions. */
    String VM_PERM_ALLOW_INLINE = "velocimacro.permissions.allow.inline";

    /** boolean (true/false) default false : allow inline (in-template) macro definitions to replace existing. */
    String VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL = "velocimacro.permissions.allow.inline.to.replace.global";

    /** Switch for forcing inline macros to be local : default false. */
    String VM_PERM_INLINE_LOCAL = "velocimacro.permissions.allow.inline.local.scope";

    /** Switch for VM blather : default true. */
    String VM_MESSAGES_ON = "velocimacro.messages.on";

    /** switch for local context in VM : default false. */
    String VM_CONTEXT_LOCALSCOPE = "velocimacro.context.localscope";

    /** if true, throw an exception for wrong number of arguments **/
    String VM_ARGUMENTS_STRICT = "velocimacro.arguments.strict";

    /**
     * Specify the maximum depth for macro calls
     * @since 1.6
     */
    String VM_MAX_DEPTH = "velocimacro.max.depth";

    /**
     * Defines name of the reference that can be used to get the AST block passed to block macro calls.
     * @since 1.7
     */
    String VM_BODY_REFERENCE = "velocimacro.body.reference";
    
    /*
     * ----------------------------------------------------------------------
     * G E N E R A L  R U N T I M E  C O N F I G U R A T I O N
     * ----------------------------------------------------------------------
     */

    /** Switch for the interpolation facility for string literals. */
    String INTERPOLATE_STRINGLITERALS = "runtime.interpolate.string.literals";

    /** The character encoding for the templates. Used by the parser in processing the input streams. */
    String INPUT_ENCODING = "input.encoding";

    /** Encoding for the output stream. Currently used by Anakia and VelocityServlet */
    String OUTPUT_ENCODING = "output.encoding";

    /** Default Encoding is ISO-8859-1. */
    String ENCODING_DEFAULT = "ISO-8859-1";

    /** key name for uberspector. Multiple classnames can be specified,in which case uberspectors will be chained. */
    String UBERSPECT_CLASSNAME = "runtime.introspector.uberspect";

    /** A comma separated list of packages to restrict access to in the SecureIntrospector. */
    String INTROSPECTOR_RESTRICT_PACKAGES = "introspector.restrict.packages";

    /** A comma separated list of classes to restrict access to in the SecureIntrospector. */
    String INTROSPECTOR_RESTRICT_CLASSES = "introspector.restrict.classes";

    /** Switch for ignoring nulls in math equations vs throwing exceptions. */
    String STRICT_MATH = "runtime.strict.math";

    /**
     * The <code>parser.pool.class</code> property specifies the name of the {@link org.apache.velocity.util.SimplePool}
     * implementation to use.
     */
    String PARSER_POOL_CLASS = "parser.pool.class";

    /**
     * @see  #NUMBER_OF_PARSERS
     */
    String PARSER_POOL_SIZE = "parser.pool.size";
    
    /*
     * ----------------------------------------------------------------------
     * These constants are used internally by the Velocity runtime i.e.
     * the constants listed below are strictly used in the Runtime
     * class itself.
     * ----------------------------------------------------------------------
     */

    /** Default Runtime properties. */
    String DEFAULT_RUNTIME_PROPERTIES = "org/apache/velocity/runtime/defaults/velocity.properties";

    /** Default Runtime properties. */
    String DEFAULT_RUNTIME_DIRECTIVES = "org/apache/velocity/runtime/defaults/directive.properties";

    /**
     * The default number of parser instances to create. Configurable via the parameter named by the {@link #PARSER_POOL_SIZE}
     * constant.
     */
    int NUMBER_OF_PARSERS = 20;

    



}
