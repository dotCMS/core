package org.apache.velocity.runtime.parser.node;

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

import com.dotmarketing.util.Logger;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.util.introspection.Introspector;

/**
 * Resolves {@code $reference.component} against the accessor of a Java {@code record} component.
 *
 * <p>{@link PropertyExecutor} only looks for JavaBean-shaped getters ({@code getFoo()} /
 * {@code getfoo()}), and {@link BooleanPropertyExecutor} only for {@code isFoo()}. A record's
 * canonical accessor is named after the component itself ({@code foo()}), so none of the existing
 * executors can reach it and the reference silently renders as literal template text. This executor
 * closes that gap.</p>
 *
 * <p><strong>It is deliberately narrow.</strong> Resolution is attempted only when the target class
 * is a record <em>and</em> the identifier names one of its declared components. It never resolves an
 * arbitrary no-argument method, so classes that are not records behave exactly as before.</p>
 *
 * <p>It is also tried <strong>last</strong> in
 * {@link org.apache.velocity.util.introspection.UberspectImpl#getPropertyGet}, after the bean getter,
 * the {@code Map} key lookup, {@code get("foo")} and {@code isFoo()}. Every strategy that could
 * already resolve the reference is given its chance first, so this executor can only add a resolution
 * where there was none — no reference that resolves today changes meaning, without exception. Records
 * whose components are bean-named (as {@code SearchHit} still is) keep resolving through
 * {@link PropertyExecutor}.</p>
 *
 * <p>The accessor is looked up through the {@link Introspector} rather than through
 * {@link RecordComponent#getAccessor()} so that the method cache and the security checks of the
 * configured introspector (see {@code SecureIntrospectorImpl}) both still apply.</p>
 *
 * @see PropertyExecutor
 * @see org.apache.velocity.util.introspection.UberspectImpl#getPropertyGet
 */
public class RecordComponentExecutor extends AbstractExecutor
{
    private final Introspector introspector;

    /**
     * @param introspector the introspector used to resolve (and cache) the accessor
     * @param clazz        the class of the object the reference is being resolved against
     * @param property     the identifier written in the template
     */
    public RecordComponentExecutor(final Introspector introspector,
            final Class clazz, final String property)
    {
        this.introspector = introspector;

        // Mirrors PropertyExecutor: an empty identifier would only confuse the introspector.
        if (clazz != null && clazz.isRecord() && StringUtils.isNotEmpty(property))
        {
            discover(clazz, property);
        }
    }

    /**
     * @return The current introspector.
     */
    protected Introspector getIntrospector()
    {
        return this.introspector;
    }

    /**
     * Resolves the accessor, but only if {@code property} names a declared component of the record.
     *
     * @param clazz    the record class
     * @param property the identifier written in the template
     */
    protected void discover(final Class clazz, final String property)
    {
        try
        {
            final String component = componentNamed(clazz, property);

            if (component != null)
            {
                final Object[] params = {};
                setMethod(introspector.getMethod(clazz, component, params));
            }
        }
        /*
         * pass through application level runtime exceptions
         */
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            final String msg = "Exception while looking for record component accessor for '"
                    + property + "'";
            Logger.error(this, msg, e);
            throw new VelocityException(msg, e);
        }
    }

    /**
     * Returns the declared component name matching {@code property}, or {@code null} when the record
     * has no such component.
     *
     * <p>An exact match is preferred. Failing that, the first character is case-flipped, which is the
     * same convenience {@link PropertyExecutor} offers for bean getters so that {@code $rec.foo} and
     * {@code $rec.Foo} behave alike.</p>
     */
    private String componentNamed(final Class clazz, final String property)
    {
        final RecordComponent[] components = clazz.getRecordComponents();

        if (components == null)
        {
            return null;
        }

        for (final RecordComponent candidate : components)
        {
            if (candidate.getName().equals(property))
            {
                return candidate.getName();
            }
        }

        final String flipped = flipFirstCharacter(property);

        for (final RecordComponent candidate : components)
        {
            if (candidate.getName().equals(flipped))
            {
                return candidate.getName();
            }
        }

        return null;
    }

    /**
     * Flips the case of the first character, e.g. {@code Title} to {@code title}.
     */
    private String flipFirstCharacter(final String property)
    {
        final char first = property.charAt(0);
        final char flipped = Character.isLowerCase(first)
                ? Character.toUpperCase(first)
                : Character.toLowerCase(first);

        return flipped + property.substring(1);
    }

    /**
     * @see AbstractExecutor#execute(java.lang.Object)
     */
    @Override
    public Object execute(Object o)
        throws IllegalAccessException, InvocationTargetException
    {
        return isAlive() ? getMethod().invoke(o, ((Object[]) null)) : null;
    }
}
