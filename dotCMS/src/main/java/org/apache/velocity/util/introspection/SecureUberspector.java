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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.util.RuntimeServicesAware;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rendering.velocity.util.TemplateEditableObject;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

/**
 * Use a custom introspector that prevents classloader related method 
 * calls.  Use this introspector for situations in which template 
 * writers are numerous or untrusted.  Specifically, this introspector 
 * prevents creation of arbitrary objects or reflection on objects.
 * 
 * <p>To use this introspector, set the following property:
 * <pre>
 * runtime.introspector.uberspect = org.apache.velocity.util.introspection.SecureUberspector
 * </pre>
 * 
 * @author <a href="mailto:wglass@forio.com">Will Glass-Husain</a>
 * @version $Id: SecureUberspector.java 774412 2009-05-13 15:54:07Z nbubna $
 * @since 1.5
 */
public class SecureUberspector extends UberspectImpl implements RuntimeServicesAware
{
    RuntimeServices runtimeServices;
    
    public SecureUberspector()
    {
        super();
    }

    /**
     *  init - generates the Introspector. As the setup code
     *  makes sure that the log gets set before this is called,
     *  we can initialize the Introspector using the log object.
     */
    public void init()
    {
        String [] badPackages = runtimeServices.getConfiguration()
                        .getStringArray(RuntimeConstants.INTROSPECTOR_RESTRICT_PACKAGES);

        String [] badClasses = runtimeServices.getConfiguration()
                        .getStringArray(RuntimeConstants.INTROSPECTOR_RESTRICT_CLASSES);
        
        introspector = new SecureIntrospectorImpl(badClasses, badPackages);
    }
    
    /**
     * Get an iterator from the given object.  Since the superclass method
     * this secure version checks for execute permission.
     * 
     * @param obj object to iterate over
     * @param i line, column, template info
     * @return Iterator for object
     */
    public Iterator getIterator(Object obj, Info i) throws Exception
    {
        if (obj != null)
        {
            SecureIntrospectorControl sic = (SecureIntrospectorControl)introspector;
            if (sic.checkObjectExecutePermission(obj.getClass(), null))
            {
                return super.getIterator(obj, i);
            }
            else
            {
                Logger.warn(this,"Cannot retrieve iterator from " + obj.getClass() +
                         " due to security restrictions.");
            }
        }
        return null;
    }

    /**
     * Resolves a method call. On content, site, container, template, folder and identifier objects, a call that would
     * change the object is resolved to a method that ignores the call, unless the object is the one
     * {@link TemplateEditableObject} names or {@link FeatureFlagName#FEATURE_FLAG_VELOCITY_ALLOW_MODEL_MUTATORS} is on.
     * A content object's {@code getMap()} returns a copy of its field map on the same terms.
     *
     * @param obj object the method is called on
     * @param methodName name of the method
     * @param args arguments of the call
     * @param i template, line and column of the call
     * @return the method, or {@code null} if it does not resolve
     */
    @Override
    public VelMethod getMethod(Object obj, String methodName, Object[] args, Info i) throws Exception
    {
        final VelMethod method = super.getMethod(obj, methodName, args, i);
        if (method == null)
        {
            return null;
        }
        if (SecureIntrospectorImpl.isModelMutator(obj.getClass(), methodName))
        {
            return new ReadOnlyModelMethod(method, i);
        }
        if (obj instanceof Contentlet && FIELD_MAP_GETTER.equals(methodName))
        {
            return new FieldMapCopyMethod(method);
        }
        return method;
    }

    /**
     * Resolves a property read. Reading {@code map} on a content object returns a copy of its field map, as
     * {@link #getMethod(Object, String, Object[], Info)} does for {@code getMap()}.
     *
     * @param obj object the property is read from
     * @param identifier property name
     * @param i template, line and column of the read
     * @return the property reader, or {@code null} if it does not resolve
     */
    @Override
    public VelPropertyGet getPropertyGet(Object obj, String identifier, Info i) throws Exception
    {
        final VelPropertyGet getter = super.getPropertyGet(obj, identifier, i);
        if (getter != null && obj instanceof Contentlet && FIELD_MAP_GETTER.equals(getter.getMethodName()))
        {
            return new FieldMapCopyGetter(getter);
        }
        return getter;
    }

    /**
     * Resolves a property assignment ({@code #set($obj.prop = value)}). On the model types it is ignored on the same
     * terms as a changing method call; see {@link #getMethod(Object, String, Object[], Info)}.
     *
     * @param obj object the property is assigned on
     * @param identifier property name
     * @param arg value assigned
     * @param i template, line and column of the assignment
     * @return the property writer, or {@code null} if it does not resolve
     */
    @Override
    public VelPropertySet getPropertySet(Object obj, String identifier, Object arg, Info i) throws Exception
    {
        final VelPropertySet setter = super.getPropertySet(obj, identifier, arg, i);
        if (setter != null && SecureIntrospectorImpl.isModelMutator(obj.getClass(), setter.getMethodName()))
        {
            return new ReadOnlyModelSetter(setter, i);
        }
        return setter;
    }

    /** The content object method that returns its own field map. */
    private static final String FIELD_MAP_GETTER = "getMap";

    /** Class and method pairs already logged, so each refusal is logged once. */
    private static final Set<String> LOGGED_REFUSALS = ConcurrentHashMap.newKeySet();

    /**
     * Tells whether template code may change this object now: the flag is on, or the object is the one the current
     * {@link TemplateEditableObject} scope names.
     *
     * @param target the object about to be changed
     * @return {@code true} if the change may go ahead
     */
    private static boolean mayChange(final Object target)
    {
        return TemplateEditableObject.isEditable(target) || allowsModelMutators();
    }

    /** The value of {@link FeatureFlagName#FEATURE_FLAG_VELOCITY_ALLOW_MODEL_MUTATORS}; {@code null} until read. */
    private static volatile Boolean allowModelMutators;

    /**
     * Reads {@link FeatureFlagName#FEATURE_FLAG_VELOCITY_ALLOW_MODEL_MUTATORS} once and keeps the value, since it is
     * consulted on every model method call and field map read a template makes.
     *
     * @return {@code true} if templates may change model objects
     */
    private static boolean allowsModelMutators()
    {
        Boolean allow = allowModelMutators;
        if (allow == null)
        {
            allow = Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_VELOCITY_ALLOW_MODEL_MUTATORS, false);
            allowModelMutators = allow;
        }
        return allow;
    }

    /**
     * Makes the next model method call read {@link FeatureFlagName#FEATURE_FLAG_VELOCITY_ALLOW_MODEL_MUTATORS} again,
     * for a change made at runtime through {@link Config#setProperty(String, Object)}.
     */
    public static void reloadModelMutatorSetting()
    {
        allowModelMutators = null;
    }

    /**
     * Logs an ignored change once per class and method, with the template that made the first one.
     *
     * @param target the object the template tried to change
     * @param methodName the method that would have changed it
     * @param info where in the template the call is, if known
     */
    private static void logRefusal(final Object target, final String methodName, final Info info)
    {
        final String className = target.getClass().getName();
        if (LOGGED_REFUSALS.add(className + '#' + methodName))
        {
            Logger.warn(SecureUberspector.class, "Template code called " + methodName + " on a " + className
                    + (info != null ? " at " + info : "")
                    + "; the call was ignored because this object is read-only in templates. Set "
                    + FeatureFlagName.FEATURE_FLAG_VELOCITY_ALLOW_MODEL_MUTATORS
                    + "=true to let templates change it. Further calls of this method are not logged.");
        }
    }

    /** A changing method on a model object, which runs only when {@link #mayChange(Object)} allows it. */
    private static final class ReadOnlyModelMethod implements VelMethod
    {
        private final VelMethod method;
        private final Info info;

        ReadOnlyModelMethod(final VelMethod method, final Info info)
        {
            this.method = method;
            this.info = info;
        }

        /**
         * Calls the method if the object may be changed; otherwise logs the call and returns {@code null}.
         *
         * @param o the object
         * @param params the arguments
         * @return the method's result, or {@code null} when the call is ignored
         * @throws Exception as the method throws it
         */
        @Override
        public Object invoke(final Object o, final Object[] params) throws Exception
        {
            if (mayChange(o))
            {
                return method.invoke(o, params);
            }
            logRefusal(o, method.getMethodName(), info);
            return null;
        }

        /** @return whether the wrapped method may be reused */
        @Override
        public boolean isCacheable()
        {
            return method.isCacheable();
        }

        /** @return the wrapped method's name */
        @Override
        public String getMethodName()
        {
            return method.getMethodName();
        }

        /** @return the wrapped method's return type */
        @Override
        public Class getReturnType()
        {
            return method.getReturnType();
        }
    }

    /** A property assignment on a model object, which runs only when {@link #mayChange(Object)} allows it. */
    private static final class ReadOnlyModelSetter implements VelPropertySet
    {
        private final VelPropertySet setter;
        private final Info info;

        ReadOnlyModelSetter(final VelPropertySet setter, final Info info)
        {
            this.setter = setter;
            this.info = info;
        }

        /**
         * Assigns the property if the object may be changed; otherwise logs the assignment and returns {@code null}.
         *
         * @param o the object
         * @param arg the value
         * @return the setter's result, or {@code null} when the assignment is ignored
         * @throws Exception as the setter throws it
         */
        @Override
        public Object invoke(final Object o, final Object arg) throws Exception
        {
            if (mayChange(o))
            {
                return setter.invoke(o, arg);
            }
            logRefusal(o, setter.getMethodName(), info);
            return null;
        }

        /** @return whether the wrapped setter may be reused */
        @Override
        public boolean isCacheable()
        {
            return setter.isCacheable();
        }

        /** @return the wrapped setter's method name */
        @Override
        public String getMethodName()
        {
            return setter.getMethodName();
        }
    }

    /**
     * Returns a copy of a field map unless the content object it came from may be changed.
     *
     * @param target the content object
     * @param map the map {@code getMap()} returned
     * @return the map itself, or a copy of it
     */
    private static Object fieldMapFor(final Object target, final Object map)
    {
        return map instanceof Map && !mayChange(target) ? new HashMap<>((Map<?, ?>) map) : map;
    }

    /** {@code getMap()} on a content object, returning a copy of the field map. */
    private static final class FieldMapCopyMethod implements VelMethod
    {
        private final VelMethod method;

        FieldMapCopyMethod(final VelMethod method)
        {
            this.method = method;
        }

        /**
         * Calls {@code getMap()} and copies the result unless the object may be changed.
         *
         * @param o the content object
         * @param params the arguments (none)
         * @return the field map or its copy
         * @throws Exception as {@code getMap()} throws it
         */
        @Override
        public Object invoke(final Object o, final Object[] params) throws Exception
        {
            return fieldMapFor(o, method.invoke(o, params));
        }

        /** @return whether the wrapped method may be reused */
        @Override
        public boolean isCacheable()
        {
            return method.isCacheable();
        }

        /** @return the wrapped method's name */
        @Override
        public String getMethodName()
        {
            return method.getMethodName();
        }

        /** @return the wrapped method's return type */
        @Override
        public Class getReturnType()
        {
            return method.getReturnType();
        }
    }

    /** The {@code map} property of a content object, returning a copy of the field map. */
    private static final class FieldMapCopyGetter implements VelPropertyGet
    {
        private final VelPropertyGet getter;

        FieldMapCopyGetter(final VelPropertyGet getter)
        {
            this.getter = getter;
        }

        /**
         * Reads the field map and copies it unless the object may be changed.
         *
         * @param o the content object
         * @return the field map or its copy
         * @throws Exception as {@code getMap()} throws it
         */
        @Override
        public Object invoke(final Object o) throws Exception
        {
            return fieldMapFor(o, getter.invoke(o));
        }

        /** @return whether the wrapped getter may be reused */
        @Override
        public boolean isCacheable()
        {
            return getter.isCacheable();
        }

        /** @return the wrapped getter's method name */
        @Override
        public String getMethodName()
        {
            return getter.getMethodName();
        }
    }

    /**
     * Store the RuntimeServices before the object is initialized..
     * @param rs RuntimeServices object for initialization
     */
    public void setRuntimeServices(RuntimeServices rs)
    {
        this.runtimeServices = rs;
    }
    
    
}
