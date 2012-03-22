/*
 * Copyright 2003-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.velocity.tools.generic;

import java.lang.reflect.Array;
import java.util.List;

/**
 * Tool for working with Lists and arrays in Velocity templates.
 * It provides a method to get and set specified elements.
 * Also provides methods to perform the following actions to Lists and arrays:
 * <ul>
 *   <li>Check if it is empty.</li>
 *   <li>Check if it contains a certain element.</li>
 * </ul>
 *
 * <p><pre>
 * Example uses:
 *  $primes                    -> new int[] {2, 3, 5, 7}
 *  $list.size($primes)        -> 4
 *  $list.get($primes, 2)      -> 5
 *  $list.set($primes, 2, 1)   -> (primes[2] becomes 1)
 *  $list.get($primes, 2)      -> 1
 *  $list.isEmpty($primes)     -> false
 *  $list.contains($primes, 7) -> true
 *
 * Example toolbox.xml config (if you want to use this with VelocityView):
 * &lt;tool&gt;
 *   &lt;key&gt;list&lt;/key&gt;
 *   &lt;scope&gt;application&lt;/scope&gt;
 *   &lt;class&gt;org.apache.velocity.tools.generic.ListTool&lt;/class&gt;
 * &lt;/tool&gt;
 * </pre></p>
 *
 * <p>This tool is entirely threadsafe, and has no instance members.
 * It may be used in any scope (request, session, or application).
 * </p>
 *
 * @author <a href="mailto:shinobu@ieee.org">Shinobu Kawai</a>
 * @version $Id: $
 * @since VelocityTools 1.2
 */
public class ListTool
{

    /**
     * Default constructor.
     */
    public ListTool()
    {
    }

    /**
     * Gets the specified element of a List/array.
     * It will return null under the following conditions:
     * <ul>
     *   <li><code>list</code> is null.</li>
     *   <li><code>list</code> is not a List/array.</li>
     *   <li><code>list</code> doesn't have an <code>index</code>th value.</li>
     * </ul>
     * @param list the List/array object.
     * @param index the index of the List/array to get.
     * @return the specified element of the List/array.
     */
    public Object get(Object list, int index)
    {
        if (isArray(list))
        {
            return getFromArray(list, index);
        }
        if (!isList(list))
        {
            return null;
        }

        try
        {
            return ((List) list).get(index);
        }
        catch (IndexOutOfBoundsException e)
        {
            // The index was wrong.
            return null;
        }
    }

    /**
     * Gets the specified element of an array.
     * @param array the array object.
     * @param index the index of the array to get.
     * @return the specified element of the array.
     */
    private Object getFromArray(Object array, int index)
    {
        try
        {
            return Array.get(array, index);
        }
        catch (IndexOutOfBoundsException e)
        {
            // The index was wrong.
            return null;
        }
    }

    /**
     * Sets the specified element of a List/array.
     * It will return null under the following conditions:
     * <ul>
     *   <li><code>list</code> is null.</li>
     *   <li><code>list</code> is not a List/array.</li>
     *   <li><code>list</code> doesn't have an <code>index</code>th value.</li>
     * </ul>
     * @param list the List/array object.
     * @param index the index of the List/array to set.
     * @param value the element to set.
     * @return blank if set, null if not set.
     */
    public Object set(Object list, int index, Object value)
    {
        if (isArray(list))
        {
            return setToArray(list, index, value);
        }
        if (!isList(list))
        {
            return null;
        }

        try
        {
            ((List) list).set(index, value);
            return "";
        }
        catch (IndexOutOfBoundsException e)
        {
            // The index was wrong.
            return null;
        }
    }

    /**
     * Sets the specified element of an array.
     * @param array the array object.
     * @param index the index of the array to set.
     * @param value the element to set.
     * @return blank if set, null if not set.
     */
    private Object setToArray(Object array, int index, Object value)
    {
        try
        {
            Array.set(array, index, value);
            return "";
        }
        catch (IndexOutOfBoundsException e)
        {
            // The index was wrong.
            return null;
        }
    }

    /**
     * Gets the size of a List/array.
     * It will return null under the following conditions:
     * <ul>
     *   <li><code>list</code> is null.</li>
     *   <li><code>list</code> is not a List/array.</li>
     * </ul>
     * @param list the List object.
     * @return the size of the List.
     */
    public Integer size(Object list)
    {
        if (isArray(list))
        {
            // Thanks to Eric Fixler for this refactor.
            return new Integer(Array.getLength(list));
        }
        if (!isList(list))
        {
            return null;
        }

        return new Integer(((List) list).size());
    }

    /**
     * Checks if an object is an array.
     * @param object the object to check.
     * @return <code>true</code> if the object is an array.
     */
    public boolean isArray(Object object)
    {
        if (object == null)
        {
            return false;
        }
        return object.getClass().isArray();
    }

    /**
     * Checks if an object is a List.
     * @param object the object to check.
     * @return <code>true</code> if the object is a List.
     */
    public boolean isList(Object object)
    {
        return object instanceof List;
    }

    /**
     * Checks if a List/array is empty.
     * @param list the List/array to check.
     * @return <code>true</code> if the List/array is empty.
     */
    public Boolean isEmpty(Object list)
    {
        Integer size = size(list);
        if (size == null)
        {
            return null;
        }

        return new Boolean(size.intValue() == 0);
    }

    /**
     * Checks if a List/array contains a certain element.
     * @param list the List/array to check.
     * @param element the element to check.
     * @return <code>true</code> if the List/array contains the element.
     */
    public Boolean contains(Object list, Object element)
    {
        if (isArray(list))
        {
            return arrayContains(list, element);
        }
        if (!isList(list))
        {
            return null;
        }

        return new Boolean(((List) list).contains(element));
    }

    /**
     * Checks if an array contains a certain element.
     * @param array the array to check.
     * @param element the element to check.
     * @return <code>true</code> if the array contains the element.
     */
    private Boolean arrayContains(Object array, Object element)
    {
        int size = size(array).intValue();

        for (int index = 0; index < size; ++index)
        {
            if (equals(element, getFromArray(array, index)))
            {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * Check if two objects are equal.
     * @param what an object
     * @param with another object.
     * @return <code>true</code> if the two objects are equal.
     */
    private boolean equals(Object what, Object with)
    {
        if (what == null)
        {
            return with == null;
        }

        return what.equals(with);
    }

}
