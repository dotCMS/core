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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>Utility class for easy parsing of String values held in a Map.</p>
 * <p><pre>
 * Template example(s):
 *   $parser.foo                ->  bar
 *   $parser.getNumber('baz')   ->  12.6
 *   $parser.getInt('baz')      ->  12
 *   $parser.getNumbers('foo')  ->  [12.6]
 *
 * Toolbox configuration:
 * &lt;tool&gt;
 *   &lt;key&gt;parser&lt;/key&gt;
 *   &lt;class&gt;org.apache.velocity.generic.Parser&lt;/class&gt;
 * &lt;/tool&gt;
 * </pre></p>
 *
 * <p>This comes in very handy when parsing parameters.</p>
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @version $Revision: 326944 $ $Date: 2005-10-20 09:48:02 -0700 (Thu, 20 Oct 2005) $
 * @since VelocityTools 1.2
 */
public class ValueParser
{
    private Map source = null;

    public ValueParser() {}

    public ValueParser(Map source)
    {
        setSource(source);
    }

    protected void setSource(Map source)
    {
        this.source = source;
    }

    protected Map getSource()
    {
        if (source == null)
        {
            throw new NullPointerException("You must set a Map source for values to be parsed.");
        }
        return this.source;
    }

    // ----------------- public parsing methods --------------------------

    /**
     * Convenience method for checking whether a certain parameter exists.
     *
     * @param key the parameter's key
     * @return <code>true</code> if a parameter exists for the specified
     *         key; otherwise, returns <code>false</code>.
     */
    public boolean exists(String key)
    {
        return (getString(key) != null);
    }

    /**
     * Convenience method for use in Velocity templates.
     * This allows for easy "dot" access to parameters.
     *
     * e.g. $params.foo instead of $params.getString('foo')
     *
     * @param key the parameter's key
     * @return parameter matching the specified key or
     *         <code>null</code> if there is no matching
     *         parameter
     */
    public String get(String key)
    {
        return getString(key);
    }

    /**
     * @param key the parameter's key
     * @return parameter matching the specified key or
     *         <code>null</code> if there is no matching
     *         parameter
     */
    public String getString(String key)
    {
        Object value = getSource().get(key);
        if (value == null)
        {
            return null;
        }

        if (value instanceof Collection)
        {
            Collection values = (Collection)value;
            if (!values.isEmpty())
            {
                // take the next available value
                value = values.iterator().next();
            }
        }
        else if (value.getClass().isArray())
        {
            if (Array.getLength(value) > 0)
            {
                // take the first value
                value = Array.get(value, 0);
            }
        }
        return String.valueOf(value);
    }

    /**
     * @param key the desired parameter's key
     * @param alternate The alternate value
     * @return parameter matching the specified key or the 
     *         specified alternate String if there is no matching
     *         parameter
     */
    public String getString(String key, String alternate)
    {
        String s = getString(key);
        return (s != null) ? s : alternate;
    }

    /**
     * @param key the desired parameter's key
     * @return a {@link Boolean} object for the specified key or 
     *         <code>null</code> if no matching parameter is found
     */
    public Boolean getBoolean(String key)
    {
        String s = getString(key);
        return (s != null) ? parseBoolean(s) : null;
    }

    /**
     * @param key the desired parameter's key
     * @param alternate The alternate boolean value
     * @return boolean value for the specified key or the 
     *         alternate boolean is no value is found
     */
    public boolean getBoolean(String key, boolean alternate)
    {
        Boolean bool = getBoolean(key);
        return (bool != null) ? bool.booleanValue() : alternate;
    }

    /**
     * @param key the desired parameter's key
     * @param alternate the alternate {@link Boolean}
     * @return a {@link Boolean} for the specified key or the specified
     *         alternate if no matching parameter is found
     */
    public Boolean getBoolean(String key, Boolean alternate)
    {
        Boolean bool = getBoolean(key);
        return (bool != null) ? bool : alternate;
    }

    /**
     * @param key the desired parameter's key
     * @return a {@link Number} for the specified key or 
     *         <code>null</code> if no matching parameter is found
     */
    public Number getNumber(String key)
    {
        String s = getString(key);
        if (s == null || s.length() == 0)
        {
            return null;
        }
        try
        {
            return parseNumber(s);
        }
        catch (Exception e)
        {
            //there is no Number with that key
            return null;
        }
    }

    /**
     * @param key the desired parameter's key
     * @param alternate The alternate Number
     * @return a Number for the specified key or the specified
     *         alternate if no matching parameter is found
     */
    public Number getNumber(String key, Number alternate)
    {
        Number n = getNumber(key);
        return (n != null) ? n : alternate;
    }

    /**
     * @param key the desired parameter's key
     * @param alternate The alternate int value
     * @return the int value for the specified key or the specified
     *         alternate value if no matching parameter is found
     */
    public int getInt(String key, int alternate)
    {
        Number n = getNumber(key);
        return (n != null) ? n.intValue() : alternate;
    }

    /**
     * @param key the desired parameter's key
     * @param alternate The alternate double value
     * @return the double value for the specified key or the specified
     *         alternate value if no matching parameter is found
     */
    public double getDouble(String key, double alternate)
    {
        Number n = getNumber(key);
        return (n != null) ? n.doubleValue() : alternate;
    }


    /**
     * @param key the key for the desired parameter
     * @return an array of String objects containing all of the values
     *         associated with the given key, or <code>null</code>
     *         if the no values are associated with the given key
     */
    public String[] getStrings(String key)
    {
        Object value = getSource().get(key);
        if (value == null)
        {
            return null;
        }

        String[] strings = null;
        if (value instanceof Collection)
        {
            Collection values = (Collection)value;
            if (!values.isEmpty())
            {
                strings = new String[values.size()];
                int index = 0;
                for (Iterator i = values.iterator(); i.hasNext(); )
                {
                    strings[index++] = String.valueOf(i.next());
                }
            }
        }
        else if (value.getClass().isArray())
        {
            strings = new String[Array.getLength(value)];
            for (int i=0; i < strings.length; i++)
            {
                strings[i] = String.valueOf(Array.get(value, i));
            }
        }
        else
        {
            strings = new String[] { String.valueOf(value) };
        }
        return strings;
    }


    /**
     * @param key the key for the desired parameter
     * @return an array of Boolean objects associated with the given key.
     */
    public Boolean[] getBooleans(String key)
    {
        String[] strings = getStrings(key);
        if (strings == null)
        {
            return null;
        }

        Boolean[] bools = new Boolean[strings.length];
        for (int i=0; i<strings.length; i++)
        {
            if (strings[i] != null && strings[i].length() > 0)
            {
                bools[i] = parseBoolean(strings[i]);
            }
        }
        return bools;
    }

    /**
     * @param key the key for the desired parameter
     * @return an array of Number objects associated with the given key, 
     *         or <code>null</code> if Numbers are not associated with it.
     */
    public Number[] getNumbers(String key)
    {
        String[] strings = getStrings(key);
        if (strings == null)
        {
            return null;
        }
        
        Number[] nums = new Number[strings.length];
        try
        {
            for (int i=0; i<nums.length; i++)
            {
                if (strings[i] != null && strings[i].length() > 0)
                {
                    nums[i] = parseNumber(strings[i]);
                }
            }
        }
        catch (NumberFormatException nfe)
        {
            return null;
        }
        return nums;
    }

    /**
     * @param key the key for the desired parameter
     * @return an array of int values associated with the given key,
     *         or <code>null</code> if numbers are not associated with it.
     */
    public int[] getInts(String key)
    {
        String[] strings = getStrings(key);
        if (strings == null)
        {
            return null;
        }
        
        int[] ints = new int[strings.length];
        try
        {
            for (int i=0; i<ints.length; i++)
            {
                if (strings[i] != null && strings[i].length() > 0)
                {
                    ints[i] = parseNumber(strings[i]).intValue();
                }
            }
        }
        catch (NumberFormatException nfe)
        {
            return null;
        }
        return ints;
    }

    /**
     * @param key the key for the desired parameter
     * @return an array of double values associated with the given key, 
     *         or <code>null</code> if numbers are not associated with it.
     */
    public double[] getDoubles(String key)
    {
        String[] strings = getStrings(key);
        if (strings == null)
        {
            return null;
        }
        
        double[] doubles = new double[strings.length];
        try
        {
            for (int i=0; i<doubles.length; i++)
            {
                if (strings[i] != null && strings[i].length() > 0)
                {
                    doubles[i] = parseNumber(strings[i]).doubleValue();
                }
            }
        }
        catch (NumberFormatException nfe)
        {
            return null;
        }
        return doubles;
    }


    // --------------------------- protected methods ------------------
 
    /**
     * Converts a parameter value into a {@link Number}
     * This is used as the base for all numeric parsing methods. So,
     * sub-classes can override to allow for customized number parsing.
     * (e.g. to handle fractions, compound numbers, etc.)
     *
     * @param value the string to be parsed
     * @return the value as a {@link Number}
     */
    protected Number parseNumber(String value) throws NumberFormatException
    {
        if (value.indexOf('.') >= 0)
        {
            return new Double(value);
        }
        return new Long(value);
    }

    /**
     * Converts a parameter value into a {@link Boolean}
     * Sub-classes can override to allow for customized boolean parsing.
     * (e.g. to handle "Yes/No" or "T/F")
     *
     * @param value the string to be parsed
     * @return the value as a {@link Boolean}
     */
    protected Boolean parseBoolean(String value)
    {
        return Boolean.valueOf(value);
    }

}
