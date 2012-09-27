package org.apache.velocity.util;

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

import java.io.InputStream;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.runtime.parser.node.ASTMethod.MethodCacheKey;
import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.IntrospectionCacheData;
import org.apache.velocity.util.introspection.VelMethod;



/**
 * Simple utility functions for manipulating classes and resources
 * from the classloader.
 *
 *  @author <a href="mailto:wglass@apache.org">Will Glass-Husain</a>
 *  @version $Id: ClassUtils.java 898032 2010-01-11 19:51:03Z nbubna $
 * @since 1.5
 */
public class ClassUtils {

    /**
     * Utility class; cannot be instantiated.
     */
    private ClassUtils()
    {
    }

    /**
     * Return the specified class.  Checks the ThreadContext classloader first,
     * then uses the System classloader.  Should replace all calls to
     * <code>Class.forName( claz )</code> (which only calls the System class
     * loader) when the class might be in a different classloader (e.g. in a
     * webapp).
     *
     * @param clazz the name of the class to instantiate
     * @return the requested Class object
     * @throws ClassNotFoundException
     */
    public static Class getClass(String clazz) throws ClassNotFoundException
    {
        /**
         * Use the Thread context classloader if possible
         */
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader != null)
        {
            try
            {
                return Class.forName(clazz, true, loader);
            }
            catch (ClassNotFoundException E)
            {
                /**
                 * If not found with ThreadContext loader, fall thru to
                 * try System classloader below (works around bug in ant).
                 */
            }
        }
        /**
         * Thread context classloader isn't working out, so use system loader.
         */
        return Class.forName(clazz);
    }

    /**
     * Return a new instance of the given class.  Checks the ThreadContext
     * classloader first, then uses the System classloader.  Should replace all
     * calls to <code>Class.forName( claz ).newInstance()</code> (which only
     * calls the System class loader) when the class might be in a different
     * classloader (e.g. in a webapp).
     *
     * @param clazz the name of the class to instantiate
     * @return an instance of the specified class
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static Object getNewInstance(String clazz)
        throws ClassNotFoundException,IllegalAccessException,InstantiationException
    {
        return getClass(clazz).newInstance();
    }

    /**
     * Finds a resource with the given name.  Checks the Thread Context
     * classloader, then uses the System classloader.  Should replace all
     * calls to <code>Class.getResourceAsString</code> when the resource
     * might come from a different classloader.  (e.g. a webapp).
     * @param claz Class to use when getting the System classloader (used if no Thread
     * Context classloader available or fails to get resource).
     * @param name name of the resource
     * @return InputStream for the resource.
     */
    public static InputStream getResourceAsStream(Class claz, String name)
    {
        InputStream result = null;

        /**
         * remove leading slash so path will work with classes in a JAR file
         */
        while (name.startsWith("/"))
        {
            name = name.substring(1);
        }

        ClassLoader classLoader = Thread.currentThread()
                                    .getContextClassLoader();

        if (classLoader == null)
        {
            classLoader = claz.getClassLoader();
            result = classLoader.getResourceAsStream( name );
        }
        else
        {
            result= classLoader.getResourceAsStream( name );

            /**
            * for compatibility with texen / ant tasks, fall back to
            * old method when resource is not found.
            */

            if (result == null)
            {
                classLoader = claz.getClassLoader();
                if (classLoader != null)
                    result = classLoader.getResourceAsStream( name );
            }
        }

        return result;

    }

  /**
   * Lookup a VelMethod object given the method signature that is specified in
   * the passed in parameters.  This method first searches the cache, if not found in
   * the cache then uses reflections to inspect Object o, for the given method.
   * @param methodName Name of method
   * @param params Array of objects that are parameters to the method
   * @param paramClasses Array of Classes coresponding to the types in params.
   * @param o Object to introspect for the given method.
   * @param context Context from which the method cache is aquirred
   * @param node ASTNode, used for error reporting.
   * @param strictRef If no method is found, throw an exception, never return null in this case
   * @return VelMethod object if the object is found, null if not matching method is found
   */    
  public static VelMethod getMethod(String methodName, Object[] params,
                                    Class[] paramClasses, Object o, InternalContextAdapter context,
                                    SimpleNode node, boolean strictRef)
  {
    VelMethod method = null;
    try
    {
      /*
       * check the cache
       */
      MethodCacheKey mck = new MethodCacheKey(methodName, paramClasses);
      IntrospectionCacheData icd = context.icacheGet(mck);

      /*
       * like ASTIdentifier, if we have cache information, and the Class of
       * Object o is the same as that in the cache, we are safe.
       */
      if (icd != null && (o != null && icd.contextData == o.getClass()))
      {

        /*
         * get the method from the cache
         */
        method = (VelMethod) icd.thingy;
      } 
      else
      {
        /*
         * otherwise, do the introspection, and then cache it
         */
        method = node.getRuntimeServices().getUberspect().getMethod(o, methodName, params,
           new Info(node.getTemplateName(), node.getLine(), node.getColumn()));

        if ((method != null) && (o != null))
        {
          icd = new IntrospectionCacheData();
          icd.contextData = o.getClass();
          icd.thingy = method;

          context.icachePut(mck, icd);
        }
      }

      /*
       * if we still haven't gotten the method, either we are calling a method
       * that doesn't exist (which is fine...) or I screwed it up.
       */
      if (method == null)
      {
        if (strictRef)
        {
          // Create a parameter list for the exception error message
          StringBuffer plist = new StringBuffer();
          for (int i = 0; i < params.length; i++)
          {
            Class param = paramClasses[i];
            plist.append(param == null ? "null" : param.getName());
            if (i < params.length - 1)
              plist.append(", ");
          }
          throw new MethodInvocationException("Object '"
              + o.getClass().getName() + "' does not contain method "
              + methodName + "(" + plist + ")", null, methodName, node
               .getTemplateName(), node.getLine(), node.getColumn());
        } 
        else
        {
          return null;
        }
      }

    } 
    catch (MethodInvocationException mie)
    {
      /*
       * this can come from the doIntrospection(), as the arg values are
       * evaluated to find the right method signature. We just want to propogate
       * it here, not do anything fancy
       */

      throw mie;
    }    
    catch (RuntimeException e)
    {
      /**
       * pass through application level runtime exceptions
       */
      throw e;
    } 
    catch (Exception e)
    {
      /*
       * can come from the doIntropection() also, from Introspector
       */
      String msg = "ASTMethod.execute() : exception from introspection";
      throw new VelocityException(msg, e);
    }

    return method;
  }
    
}
