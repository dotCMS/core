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
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

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
}
