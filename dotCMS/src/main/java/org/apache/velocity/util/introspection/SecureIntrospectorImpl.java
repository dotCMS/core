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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.security.Principal;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.AsyncContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import javax.servlet.http.PushBuilder;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.CompanyModel;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.expression.Resolver;

/**
 * <p>Prevent "dangerous" classloader/reflection related calls.  Use this
 * introspector for situations in which template writers are numerous
 * or untrusted.  Specifically, this introspector prevents creation of
 * arbitrary objects and prevents reflection on objects.
 *
 * <p>See documentation of checkObjectExecutePermission() for
 * more information on specific classes and methods blocked.
 *
 * @author <a href="mailto:wglass@forio.com">Will Glass-Husain</a>
 * @version $Id: SecureIntrospectorImpl.java 705375 2008-10-16 22:06:30Z nbubna $
 * @since 1.5
 */
public class SecureIntrospectorImpl extends Introspector implements SecureIntrospectorControl
{
    private String[] badClasses;
    private String[] badPackages;

    public SecureIntrospectorImpl(String[] badClasses, String[] badPackages)
    {
        super();
        this.badClasses = badClasses;
        this.badPackages = badPackages;
    }

    /**
     * Get the Method object corresponding to the given class, name and parameters.
     * Will check for appropriate execute permissions and return null if the method
     * is not allowed to be executed.
     *
     * @param clazz Class on which method will be called
     * @param methodName Name of method to be called
     * @param params array of parameters to method
     * @return Method object retrieved by Introspector
     * @throws IllegalArgumentException The parameter passed in were incorrect.
     */
    @Override
    public Method getMethod(Class clazz, String methodName, Object[] params)
        throws IllegalArgumentException
    {
        if (!checkObjectExecutePermission(clazz, methodName))
        {
            Logger.warn(this,"Cannot retrieve method " + methodName +
                     " from object of class " + clazz.getName() +
                     " due to security restrictions.");
            return null;
        }
        else
        {
            return super.getMethod(clazz, methodName, params);
        }
    }

    /**
     * Determine which methods and classes to prevent from executing.  Always blocks
     * methods wait() and notify().  Always allows methods on Number, Boolean, and String.
     * Blocks getRequest() on servlet request wrappers and getResponse() on servlet response
     * wrappers, write() and delete() on uploaded parts, setLocked() on the container's parameter
     * map, and every method on the container's own request, response, session, context,
     * dispatcher, async, push and principal objects. On any request or session object, including
     * the dotCMS wrappers, blocks getServletContext(), getSessionContext(), getAsyncContext(),
     * startAsync(), upgrade() and authenticate(), and records the attempt in the security log.
     * Prohibits method calls on classes related to reflection and system operations.
     * For the complete list, see the properties <code>introspector.restrict.classes</code>
     * and <code>introspector.restrict.packages</code>.
     *
     * @param clazz Class on which method will be called
     * @param methodName Name of method to be called
     * @see org.apache.velocity.util.introspection.SecureIntrospectorControl#checkObjectExecutePermission(java.lang.Class, java.lang.String)
     */
    @Override
    public boolean checkObjectExecutePermission(Class clazz, String methodName)
    {
		/**
		 * check for wait and notify
		 */
        if (methodName != null &&
            (methodName.equals("wait") || methodName.equals("notify")) )
		{
			return false;
		}

		/**
		 * Always allow the most common classes - Number, Boolean and String
		 */
		else if (Number.class.isAssignableFrom(clazz))
		{
			return true;
		}
		else if (Boolean.class.isAssignableFrom(clazz))
		{
			return true;
		}
		else if (String.class.isAssignableFrom(clazz))
		{
			return true;
		}



        /**
         * Always allow Class.getName()
         */
        else if (Class.class.isAssignableFrom(clazz) &&
                 (methodName != null) && methodName.equals("getName"))
        {
            return true;
        }

        // backporting
        // https://github.com/apache/velocity-engine/pull/21/files
        //
        /**
         * Always disallow ClassLoader, Thread and subclasses
         */
        if (ClassLoader.class.isAssignableFrom(clazz) ||
                Thread.class.isAssignableFrom(clazz))
        {
            return false;
        }

        if (isWrapperUnwrap(clazz, methodName))
        {
            return false;
        }

        if (isRequestValueMutator(clazz, methodName))
        {
            return false;
        }

        if (isCompanyKeyAccessor(clazz, methodName))
        {
            return false;
        }

        /**
         * Extend restricted-class coverage to the file, IO and network-resource type families.
         * These are matched by type hierarchy so that concrete platform implementations (for
         * example the JDK's internal Path implementation) are covered as well, which an
         * exact-string class/package list cannot reach. The static utility holders Files and
         * Paths are final and have no restricted supertype, so they are matched by identity.
         */
        if (isRestrictedResourceType(clazz))
        {
            return false;
        }

        /**
         * Servlet-container internals, embedded script interpreters and the alternate
         * reflection, naming, process and desktop/OS-integration APIs. These can construct or
         * run arbitrary code, or launch external programs, so no method on them may be called
         * from a template.
         */
        if (isRestrictedRuntimeType(clazz))
        {
            return false;
        }

        /**
         * The servlet container's own request, response, session, context, dispatcher, async,
         * push and principal objects. Templates only ever work with these through dotCMS wrappers.
         */
        if (isContainerObject(clazz))
        {
            return false;
        }

        /**
         * No request or session object, including the dotCMS wrappers, may hand a template the
         * servlet context or an async context, or hand the connection or authentication over to
         * the container.
         */
        if (isRestrictedRequestOrSessionMethod(clazz, methodName))
        {
            logToSecurityLog(clazz, methodName);
            return false;
        }



        /**
         * check the classname (minus any array info)
         * whether it matches disallowed classes or packages
         */
        String className = clazz.getName();
        if (className.startsWith("[L") && className.endsWith(";"))
        {
            className = className.substring(2, className.length() - 1);
        }

        int dotPos = className.lastIndexOf('.');
        String packageName = (dotPos == -1) ? "" : className.substring(0, dotPos);

        for (int i = 0, size = badPackages.length; i < size; i++)
        {
            if (packageName.equals(badPackages[i]))
            {
                return false;
            }
        }

        for (int i = 0, size = badClasses.length; i < size; i++)
        {
            if (className.equals(badClasses[i]))
            {
                return false;
            }
        }

        /**
         * Operator-configurable restricted packages/classes (additive to the code-level floor
         * above). This lets deployments push the sandbox toward a stricter, package-level stance
         * as new holes are found, without a code change — for example
         * {@code DOT_VELOCITY_INTROSPECTOR_RESTRICT_PACKAGES=java.nio,java.net}. An allow-list
         * (ALLOW_CLASSES_PROP) carves specific classes back out of that configurable denial.
         * Denials are logged by getMethod(). Defaults are empty, so behavior is unchanged unless
         * configured.
         */
        if (isConfigRestricted(className, packageName))
        {
            return false;
        }

        return true;
    }

    /** dotCMS Config key (env: DOT_VELOCITY_INTROSPECTOR_RESTRICT_PACKAGES): extra restricted package prefixes. */
    public static final String RESTRICT_PACKAGES_PROP = "velocity.introspector.restrict.packages";

    /** dotCMS Config key (env: DOT_VELOCITY_INTROSPECTOR_RESTRICT_CLASSES): extra restricted class names. */
    public static final String RESTRICT_CLASSES_PROP = "velocity.introspector.restrict.classes";

    /** dotCMS Config key (env: DOT_VELOCITY_INTROSPECTOR_ALLOW_CLASSES): classes carved out of the configurable denial. */
    public static final String ALLOW_CLASSES_PROP = "velocity.introspector.allow.classes";

    private static final String[] EMPTY = new String[0];

    /**
     * Reads a comma-separated dotCMS Config list, tolerating an uninitialized Config so this
     * vendored introspector stays usable outside a running dotCMS (e.g. plain unit tests).
     *
     * @param prop the Config/env property name
     * @return the configured values, or an empty array if unset or unavailable
     */
    private static String[] configList(final String prop)
    {
        try
        {
            final String[] values = Config.getStringArrayProperty(prop, EMPTY);
            return values == null ? EMPTY : values;
        }
        catch (final Throwable t)
        {
            return EMPTY;
        }
    }

    /**
     * Determines whether the given class is denied by the operator-configurable layer. An
     * explicit allow-list entry wins, so a broadened package can still exempt specific classes.
     * This layer never overrides the code-level floor or the reflection/system denials above.
     *
     * @param className fully-qualified class name (array markers already stripped)
     * @param packageName package of the class ("" for the default package)
     * @return {@code true} if the configurable layer denies the class, {@code false} otherwise
     */
    private static boolean isConfigRestricted(final String className, final String packageName)
    {
        for (final String allowed : configList(ALLOW_CLASSES_PROP))
        {
            if (className.equals(allowed.trim()))
            {
                return false;
            }
        }

        for (final String badClass : configList(RESTRICT_CLASSES_PROP))
        {
            if (className.equals(badClass.trim()))
            {
                return true;
            }
        }

        for (final String badPackage : configList(RESTRICT_PACKAGES_PROP))
        {
            final String prefix = badPackage.trim();
            if (!prefix.isEmpty()
                    && (packageName.equals(prefix) || packageName.startsWith(prefix + ".")))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * The supertypes whose method calls are restricted. Matching by hierarchy means every
     * concrete subtype and implementation is covered without enumerating platform-internal
     * class names.
     */
    private static final Class<?>[] RESTRICTED_SUPERTYPES = {
            File.class, Path.class, RandomAccessFile.class, InputStream.class,
            OutputStream.class, Reader.class, Writer.class, Channel.class,
            FileSystem.class, FileSystemProvider.class, URL.class, URI.class
    };

    /**
     * The final utility holders that expose file-system operations but have no restricted
     * supertype; matched by identity.
     */
    private static final Class<?>[] RESTRICTED_EXACT_TYPES = {
            Files.class, Paths.class
    };

    /**
     * Determines whether method execution on the given class is restricted because it belongs to
     * the file, IO or network-resource type families.
     *
     * @param clazz class a method is about to be resolved on
     * @return {@code true} if the class is a restricted resource type (deny), {@code false} otherwise
     */
    private static boolean isRestrictedResourceType(final Class<?> clazz)
    {
        for (final Class<?> supertype : RESTRICTED_SUPERTYPES)
        {
            if (supertype.isAssignableFrom(clazz))
            {
                return true;
            }
        }

        for (final Class<?> exact : RESTRICTED_EXACT_TYPES)
        {
            if (exact == clazz)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Package prefixes of servlet-container internals (Tomcat, the JSP runtime and its expression
     * language), the CDI container (Weld), the expression-language APIs, embedded script
     * interpreters and the alternate reflection and naming APIs. Matched against the class itself.
     */
    private static final String[] RESTRICTED_RUNTIME_PREFIXES = {
            "org.apache.tomcat.",
            "bsh.", "com.dotcms.repackage.bsh.",
            "groovy.lang.", "org.codehaus.groovy.", "org.mozilla.javascript.",
            "jdk.nashorn.", "org.openjdk.nashorn.", "org.graalvm.polyglot.",
            "javax.naming.", "java.lang.invoke.",
            "javax.el.", "jakarta.el.", "org.apache.el.", "org.apache.jasper.", "org.jboss.weld."
    };

    /**
     * Types whose implementations may live in other packages: Tomcat's InstanceManager, JNDI
     * contexts, script engine factories, the JDK's ProcessHandle, the desktop/OS-integration API,
     * the servlet and async contexts, the CDI BeanManager, and the expression-language context and
     * expression factory (both the javax and jakarta namespaces). Matched anywhere in the class
     * hierarchy, so an implementation is caught whatever package it is in.
     */
    private static final Set<String> RESTRICTED_RUNTIME_TYPES = Set.of(
            "org.apache.tomcat.InstanceManager",
            "javax.naming.Context",
            "javax.script.ScriptEngineFactory",
            "java.lang.ProcessHandle",
            "java.lang.ProcessHandle$Info",
            "java.awt.Desktop",
            "javax.servlet.ServletContext",
            "javax.servlet.AsyncContext",
            "javax.enterprise.inject.spi.BeanManager",
            "javax.el.ELContext",
            "javax.el.ExpressionFactory",
            "jakarta.el.ELContext",
            "jakarta.el.ExpressionFactory"
    );

    /** Package of the servlet container's classes. */
    private static final String CONTAINER_PACKAGE_PREFIX = "org.apache.catalina.";

    /** The container's request parameter map; matched by name so Tomcat stays optional at runtime. */
    private static final String CONTAINER_PARAMETER_MAP = "org.apache.catalina.util.ParameterMap";

    /**
     * Servlet API types whose container implementations hold or hand back the container's own
     * request, response, session or dispatch state. {@link Principal} covers the container's
     * principal, which can carry credentials.
     */
    private static final Class<?>[] CONTAINER_OBJECT_TYPES = {
            ServletRequest.class, ServletResponse.class, HttpSession.class, ServletContext.class,
            AsyncContext.class, RequestDispatcher.class, PushBuilder.class, Principal.class
    };

    /**
     * Determines whether the given class is one of the servlet container's own request, response,
     * session, context, dispatcher, async, push or principal implementations. Other container
     * classes a template reaches through the wrapped request, such as the parameter map or an
     * uploaded part, are value objects and are not matched here.
     *
     * @param clazz class a method is about to be resolved on
     * @return {@code true} if the class is a container class implementing one of
     *         {@link #CONTAINER_OBJECT_TYPES} (deny), {@code false} otherwise
     */
    static boolean isContainerObject(final Class<?> clazz)
    {
        if (clazz == null || !clazz.getName().startsWith(CONTAINER_PACKAGE_PREFIX))
        {
            return false;
        }

        for (final Class<?> type : CONTAINER_OBJECT_TYPES)
        {
            if (type.isAssignableFrom(clazz))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines whether the call is a servlet API unwrap accessor. Templates receive the request
     * and response through dotCMS wrappers, which decide what a template may read or change.
     * Unwrapping one would hand the template the object underneath (the container's own request,
     * or another wrapper that writes straight through to it), so these accessors are refused. Java
     * code is unaffected and keeps unwrapping normally, for example when a RequestDispatcher
     * includes or forwards.
     *
     * @param clazz      class the method is about to be resolved on
     * @param methodName name of the method
     * @return {@code true} for {@code getRequest} on a {@link ServletRequestWrapper} or
     *         {@code getResponse} on a {@link ServletResponseWrapper} (deny), {@code false} otherwise
     */
    static boolean isWrapperUnwrap(final Class<?> clazz, final String methodName)
    {
        return ("getRequest".equals(methodName) && ServletRequestWrapper.class.isAssignableFrom(clazz))
                || ("getResponse".equals(methodName) && ServletResponseWrapper.class.isAssignableFrom(clazz));
    }

    /**
     * Determines whether the call would change an object a template can read through the wrapped
     * request but must not change: an uploaded part may not be written to a file or deleted, and
     * the container's parameter map stays locked against changes. Their read methods stay
     * available.
     *
     * @param clazz      class the method is about to be resolved on
     * @param methodName name of the method
     * @return {@code true} for {@code write} or {@code delete} on a {@link Part}, or
     *         {@code setLocked} on the container's parameter map (deny), {@code false} otherwise
     */
    static boolean isRequestValueMutator(final Class<?> clazz, final String methodName)
    {
        return (("write".equals(methodName) || "delete".equals(methodName)) && Part.class.isAssignableFrom(clazz))
                || ("setLocked".equals(methodName) && CONTAINER_PARAMETER_MAP.equals(clazz.getName()));
    }

    /**
     * Determines whether method execution on the given class is restricted because it is a
     * servlet-container internal, an embedded script interpreter, or an alternate reflection,
     * naming, process or desktop/OS-integration API.
     *
     * @param clazz class a method is about to be resolved on
     * @return {@code true} if the class is a restricted runtime type (deny), {@code false} otherwise
     */
    static boolean isRestrictedRuntimeType(final Class<?> clazz)
    {
        if (clazz == null)
        {
            return false;
        }

        final String name = clazz.getName();
        for (final String prefix : RESTRICTED_RUNTIME_PREFIXES)
        {
            if (name.startsWith(prefix))
            {
                return true;
            }
        }

        final Deque<Class<?>> pending = new ArrayDeque<>();
        final Set<Class<?>> seen = new HashSet<>();
        pending.add(clazz);
        while (!pending.isEmpty())
        {
            final Class<?> current = pending.poll();
            if (!seen.add(current))
            {
                continue;
            }
            if (RESTRICTED_RUNTIME_TYPES.contains(current.getName()))
            {
                return true;
            }
            if (current.getSuperclass() != null)
            {
                pending.add(current.getSuperclass());
            }
            Collections.addAll(pending, current.getInterfaces());
        }

        return false;
    }

    /**
     * Methods that return the company key. The key is only for server code, so a template that gets hold of a
     * company, through {@code $UtilMethods}, a request attribute or any other value, can read everything on it
     * except the key. The key digest stays readable: it cannot be turned back into the key.
     */
    private static final Set<String> COMPANY_KEY_ACCESSORS = Set.of("getKey", "getKeyObj");

    /**
     * Determines whether the call is one of {@link #COMPANY_KEY_ACCESSORS} on a company. Template helpers that
     * read an object's properties by reflection, outside this introspector, use it too, so they leave the key out.
     *
     * @param clazz class a method is about to be resolved on
     * @param methodName name of the method to be called
     * @return {@code true} if the call is restricted (deny), {@code false} otherwise
     */
    public static boolean isCompanyKeyAccessor(final Class<?> clazz, final String methodName)
    {
        return clazz != null && methodName != null && CompanyModel.class.isAssignableFrom(clazz)
                && COMPANY_KEY_ACCESSORS.contains(methodName);
    }

    /**
     * Reads a property path the way {@link PropertyUtils#getProperty(Object, String)} does, but refuses a path that
     * reads the company key at any step. Template helpers that resolve a path by reflection, such as
     * {@code $math.getTotal} and {@code $sorter.sort}, never go through this introspector, so they read paths with
     * this method instead. Each step is checked and read once.
     *
     * @param bean the object the path starts from
     * @param path the property path, in {@link PropertyUtils} syntax (for example {@code item.price} or
     *             {@code items[0].name})
     * @return the value at the end of the path
     * @throws RestrictedPropertyException if a step of the path reads the company key
     * @throws Exception if the path cannot be read, as {@link PropertyUtils#getProperty(Object, String)} reports it
     */
    public static Object readPropertyPath(final Object bean, final String path) throws Exception
    {
        final Resolver resolver = BeanUtilsBean.getInstance().getPropertyUtils().getResolver();
        Object current = bean;
        String remaining = path;
        while (remaining != null && !remaining.isEmpty())
        {
            if (current == null)
            {
                throw new NestedNullException("Null value in the property path '" + path + "'");
            }
            final String step = resolver.next(remaining);
            if (readsCompanyKey(current, resolver.getProperty(step)))
            {
                throw new RestrictedPropertyException(path);
            }
            current = PropertyUtils.getProperty(current, step);
            remaining = resolver.remove(remaining);
        }
        return current;
    }

    /**
     * Thrown by {@link #readPropertyPath(Object, String)} when a path reads the company key.
     */
    public static class RestrictedPropertyException extends IllegalArgumentException
    {
        private static final long serialVersionUID = 1L;

        /**
         * Creates the exception for a refused path.
         *
         * @param path the path that was refused
         */
        public RestrictedPropertyException(final String path)
        {
            super("The property path '" + path + "' cannot be read from a template");
        }
    }

    /**
     * Tells whether reading the named property of an object calls a company key getter.
     *
     * @param bean the object the property is read from
     * @param name the property name, without index or key
     * @return {@code true} if the property's getter is a company key getter
     */
    private static boolean readsCompanyKey(final Object bean, final String name)
    {
        if (bean instanceof Map)
        {
            return false;
        }
        try
        {
            final PropertyDescriptor descriptor = PropertyUtils.getPropertyDescriptor(bean, name);
            return descriptor != null && descriptor.getReadMethod() != null
                    && isCompanyKeyAccessor(bean.getClass(), descriptor.getReadMethod().getName());
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Methods on request and session objects that a template may not call: the ones that hand back
     * the servlet context or an async context (which exposes the original request and response),
     * {@code upgrade}, which has the container construct a handler class and switch the
     * connection, and {@code authenticate}, which drives the container's authentication and writes
     * to the response.
     */
    private static final Set<String> RESTRICTED_REQUEST_SESSION_METHODS = Set.of(
            "getServletContext", "getSessionContext", "getAsyncContext", "startAsync",
            "upgrade", "authenticate"
    );

    /**
     * Determines whether the call is one of {@link #RESTRICTED_REQUEST_SESSION_METHODS} on a
     * request or session object. This covers the dotCMS wrappers as well as any other request or
     * session implementation, so it does not depend on which object a template was handed.
     *
     * @param clazz class a method is about to be resolved on
     * @param methodName name of the method to be called
     * @return {@code true} if the call is restricted (deny), {@code false} otherwise
     */
    static boolean isRestrictedRequestOrSessionMethod(final Class<?> clazz, final String methodName)
    {
        if (clazz == null || methodName == null)
        {
            return false;
        }

        return (ServletRequest.class.isAssignableFrom(clazz) || HttpSession.class.isAssignableFrom(clazz))
                && RESTRICTED_REQUEST_SESSION_METHODS.contains(methodName);
    }

    /**
     * Records a refused request or session call in the security log, as the request wrapper does
     * for servlet-context access. Logging must never change the permission decision, so a logger
     * that is unavailable (for example in plain unit tests) is ignored.
     *
     * @param clazz class the method was to be called on
     * @param methodName name of the refused method
     */
    private static void logToSecurityLog(final Class<?> clazz, final String methodName)
    {
        try
        {
            SecurityLogger.logInfo(SecureIntrospectorImpl.class,
                    "Template tried to call " + methodName + "() on " + clazz.getName());
        }
        catch (final Throwable t)
        {
            // never let logging affect the decision
        }
    }
}
