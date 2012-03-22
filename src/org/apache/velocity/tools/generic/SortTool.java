/*
 * Copyright 2004 The Apache Software Foundation.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;

import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.viewtools.content.ContentMap;

/**
 * SortTool allows a user to sort a collection (or array, iterator, etc)
 * on any arbitary set of properties exposed by the objects contained 
 * within the collection.
 * 
 * <p>The sort tool is specifically designed to use within a #foreach 
 * but you may find other uses for it.</p>
 * 
 * <p>The sort tool can handle all of the collection types supported by 
 * #foreach and the same constraints apply as well as the following.
 * Every object in the collection must support the set of properties 
 * selected to sort on. Each property which is to be sorted on must 
 * return one of the follow:
 * <ul>
 *   <li>Primitive type: e.g. int, char, long etc</li>
 *   <li>Standard Object: e.g. String, Integer, Long etc</li>
 *   <li>Object which implements the Comparable interface.</li>
 * </ul>
 * </p>
 *
 * <p>During the sort operation all properties are compared by calling 
 * compareTo() with the exception of Strings for which 
 * compareToIgnoreCase() is called.</p>
 * 
 * <p>The sort is performed by calling Collections.sort() after 
 * marshalling the collection to sort into an appropriate collection type.
 * The original collection will not be re-ordered; a new list containing
 * the sorted elements will always be returned.</p>
 * 
 * <p>The tool is used as follows:
 * <pre>
 * Single Property Sort
 * #foreach($obj in $sorter.sort($objects, "name"))
 *   $obj.name Ordinal= $obj.ordinal
 * #end
 * End
 * 
 * Multiple Property Sort
 * #foreach($obj in $sorter.sort($objects, ["name", "ordinal"]))
 *   $obj.name, $obj.ordinal
 * #end
 * End
 * </pre>
 * 
 * The sort method takes two parameters a collection and a property name 
 * or an array of property names. The property names and corresponding 
 * methods must conform to java bean standards since commons-beanutils 
 * is used to extract the property values.</p>
 * 
 * <p>By default the sort tool sorts ascending, you can override this by 
 * adding a sort type suffix to any property name.</p>
 * 
 * <p>The supported suffixes are:
 * <pre>
 * For ascending
 * :asc 
 * For descending
 * :desc
 * 
 * Example
 * #foreach($obj in $sorter.sort($objects, ["name:asc", "ordinal:desc"]))
 *   $obj.name, $obj.ordinal
 * #end
 * </pre><p>
 * 
 * <p>This will sort first by Name in ascending order and then by Ordinal 
 * in descending order, of course you could have left the :asc off of the 
 * 'Name' property as ascending is always the default.</p>
 *
 * @author S. Brett Sutton
 * @author Nathan Bubna
 * @since VelocityTools 1.2
 * @version $Id: SortTool.java 72079 2004-07-22 01:27:12Z nbubna $
 */
public class SortTool
{

    public Collection sort(Collection collection)
    {
        return sort(collection, (List)null);
    }

    public Collection sort(Object[] array)
    {
        return sort(array, (List)null);
    }

    public Collection sort(Map map)
    {
        return sort(map, (List)null);
    }


    /**
     * Sorts the collection on a single property.
     *
     * @param object the collection to be sorted.
     * @param property the property to sort on.
     */
    public Collection sort(Object object, String property)
    {
        List properties = new ArrayList(1);
        properties.add(property);

        if (object instanceof Collection)
        {
            return sort((Collection)object, properties);
        }
        else if (object instanceof Object[])
        {
            return sort((Object[])object, properties);
        }
        else if (object instanceof Map)
        {
            return sort((Map)object, properties);
        }
        // the object type is not supported
        return null;
    }

    public Collection sort(Collection collection, List properties)
    {
        List list = new ArrayList(collection.size());
        list.addAll(collection);
        return internalSort(list, properties);
    }

    public Collection sort(Map map, List properties)
    {
        return sort(map.values(), properties);
    }

    public Collection sort(Object[] array, List properties)
    {
        return internalSort(Arrays.asList(array), properties);
    }

    protected Collection internalSort(List list, List properties)
    {
        try
        {
            if (properties == null)
            {
                Collections.sort(list);
            } else {
                Collections.sort(list, new PropertiesComparator(properties));
            }
            return list;
        }
        catch (Exception e)
        {
            //TODO: log this
            return null;
        }
    }


    /**
     * Does all of the comparisons 
     */
    public class PropertiesComparator implements Comparator
    {
        private static final int TYPE_ASCENDING = 1;
        private static final int TYPE_DESCENDING = -1;

        public static final String TYPE_ASCENDING_SHORT = "asc";
        public static final String TYPE_DESCENDING_SHORT = "desc";

        List properties;
        int[] sortTypes;

        public PropertiesComparator(List properties)
        {
            this.properties = properties;

            // determine ascending/descending
            sortTypes = new int[properties.size()];

            for (int i = 0; i < properties.size(); i++)
            {
                if (properties.get(i) == null)
                {
                    throw new IllegalArgumentException("Property " + i
                            + "is null, sort properties may not be null.");
                }

                // determine if the property contains a sort type
                // e.g "Name:asc" means sort by property Name ascending
                String prop = properties.get(i).toString();
                int colonIndex = prop.indexOf(':');
                if (colonIndex != -1)
                {
                    String sortType = prop.substring(colonIndex + 1);
                    properties.set(i, prop.substring(0, colonIndex));

                    if (TYPE_ASCENDING_SHORT.equalsIgnoreCase(sortType))
                    {
                        sortTypes[i] = TYPE_ASCENDING;
                    }
                    else if (TYPE_DESCENDING_SHORT.equalsIgnoreCase(sortType))
                    {
                        sortTypes[i] = TYPE_DESCENDING;
                    }
                    else
                    {
                        //FIXME: log this
                        // invalide property sort type. use default instead.
                        sortTypes[i] = TYPE_ASCENDING;
                    }
                }
                else
                {
                    // default sort type is ascending.
                    sortTypes[i] = TYPE_ASCENDING;
                }
            }
        }
        
        public int compare(Object lhs, Object rhs)
        {
            for (int i = 0; i < properties.size(); i++)
            {
                int comparison = 0;
                String property = (String)properties.get(i);

                // properties must be comparable
                Comparable left = getComparable(lhs, property);
                Comparable right = getComparable(rhs, property);

                if (left == null && right != null)
                {
                    // find out how right feels about left being null
                    comparison = right.compareTo(left);
                    // and reverse that (if it works)
                    comparison *= -1;
                }
                else if (left instanceof String)
                {
                    //TODO: make it optional whether or not case is ignored
                    comparison = ((String)left).compareToIgnoreCase((String)right);
                }
                else
                {
                    comparison = left.compareTo(right);
                }

                // return the first difference we find
                if (comparison != 0)
                {
                    // multiplied by the sort direction, of course
                    return comparison * sortTypes[i];
                }
            }
            return 0;
        }
    }

    /**
     * Safely retrieves the comparable value for the specified property
     * from the specified object. Subclasses that wish to perform more 
     * advanced, efficient, or just different property retrieval methods
     * should override this method to do so.
     */
    protected static Comparable getComparable(Object object, String property)
    {
        try
        {
        	if(object instanceof ContentMap){
        		ContentMap cm = (ContentMap) object;
        		return (Comparable)cm.get(property);
        	}
            return (Comparable)PropertyUtils.getProperty(object, property);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Could not retrieve comparable value for '"
                                               + property + "' from " + object + ": " + e);
        }
    }
    
   

}
