package org.apache.velocity.app;

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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import org.apache.velocity.util.ClassUtils;

/**
 *  <p>
 *  This is a small utility class allow easy access to static fields in a class,
 *  such as string constants.  Velocity will not introspect for class
 *  fields (and won't in the future :), but writing setter/getter methods to do
 *  this really is a pain,  so use this if you really have
 *  to access fields.
 *
 *  <p>
 *  The idea it so enable access to the fields just like you would in Java.
 *  For example, in Java, you would access a static field like
 *  <blockquote><pre>
 *  MyClass.STRING_CONSTANT
 *  </pre></blockquote>
 *  and that is the same thing we are trying to allow here.
 *
 *  <p>
 *  So to use in your Java code, do something like this :
 *  <blockquote><pre>
 *   context.put("runtime", new FieldMethodizer( "org.apache.velocity.runtime.Runtime" ));
 *  </pre></blockquote>
 *  and then in your template, you can access any of your static fields in this way :
 *  <blockquote><pre>
 *   $runtime.COUNTER_NAME
 *  </pre></blockquote>
 *
 *  <p>
 *  Right now, this class only methodizes <code>public static</code> fields.  It seems
 *  that anything else is too dangerous.  This class is for convenience accessing
 *  'constants'.  If you have fields that aren't <code>static</code> it may be better
 *  to handle them by explicitly placing them into the context.
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: FieldMethodizer.java 652755 2008-05-02 04:00:58Z nbubna $
 */
public class FieldMethodizer
{
    /** Hold the field objects by field name */
    private HashMap fieldHash = new HashMap();

    /**
     * Allow object to be initialized without any data. You would use
     * addObject() to add data later.
     */
    public FieldMethodizer()
    {
    }

    /**
     *  Constructor that takes as it's arg the name of the class
     *  to methodize.
     *
     *  @param s Name of class to methodize.
     */
    public FieldMethodizer( String s )
    {
        try
        {
            addObject(s);
        }
        catch( Exception e )
        {
            System.err.println("Could not add " + s
                    + " for field methodizing: "
                    + e.getMessage());
        }
    }

  /**
     *  Constructor that takes as it's arg a living
     *  object to methodize.  Note that it will still
     *  only methodized the public static fields of
     *  the class.
     *
     *  @param o Name of class to methodize.
     */
    public FieldMethodizer( Object o )
    {
        try
        {
            addObject(o);
        }
        catch( Exception e )
        {
            System.err.println("Could not add " + o
                    + " for field methodizing: "
                    + e.getMessage());
        }
    }

    /**
     * Add the Name of the class to methodize
     * @param s
     * @throws Exception
     */
    public void addObject ( String s )
        throws Exception
    {
        inspect(ClassUtils.getClass(s));
    }

    /**
     * Add an Object to methodize
     * @param o
     * @throws Exception
     */
    public void addObject ( Object o )
        throws Exception
    {
        inspect(o.getClass());
    }

    /**
     *  Accessor method to get the fields by name.
     *
     *  @param fieldName Name of static field to retrieve
     *
     *  @return The value of the given field.
     */
    public Object get( String fieldName )
    {
        Object value = null;
        try
        {
            Field f = (Field) fieldHash.get( fieldName );
            if (f != null)
            {
                value = f.get(null);
            }
        }
        catch( IllegalAccessException e )
        {
            System.err.println("IllegalAccessException while trying to access " + fieldName
                    + ": " + e.getMessage());
        }
        return value;
    }

    /**
     *  Method that retrieves all public static fields
     *  in the class we are methodizing.
     */
    private void inspect(Class clas)
    {
        Field[] fields = clas.getFields();
        for( int i = 0; i < fields.length; i++)
        {
            /*
             *  only if public and static
             */
            int mod = fields[i].getModifiers();
            if ( Modifier.isStatic(mod) && Modifier.isPublic(mod) )
            {
                fieldHash.put(fields[i].getName(), fields[i]);
            }
        }
    }
}
