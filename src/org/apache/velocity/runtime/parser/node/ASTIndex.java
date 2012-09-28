package org.apache.velocity.runtime.parser.node;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.Log;
import org.apache.velocity.runtime.parser.Parser;
import org.apache.velocity.util.ClassUtils;
import org.apache.velocity.util.introspection.VelMethod;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;

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
 *  This node is responsible for the bracket notation at the end of
 *  a reference, e.g., $foo[1]
 */

public class ASTIndex extends SimpleNode
{
    private final String methodName = "get";

    /**
     * Indicates if we are running in strict reference mode.
     */
    protected boolean strictRef = false;

    public ASTIndex(int i)
    {
        super(i);
    }

    public ASTIndex(Parser p, int i)
    {
        super(p, i);
    }
  
    public Object init(InternalContextAdapter context, Object data)
        throws TemplateInitException
    {
        super.init(context, data);
        RuntimeServices rsvc=VelocityUtil.getEngine().getRuntimeServices();
        strictRef = rsvc.getBoolean(RuntimeConstants.RUNTIME_REFERENCES_STRICT, false);
        return data;
    }  


    
    private final static Object[] noParams = {};
    private final static Class[] noTypes = {};      
    /**
     * If argument is an Integer and negative, then return (o.size() - argument). 
     * Otherwise return the original argument.  We use this to calculate the true
     * index of a negative index e.g., $foo[-1]. If no size() method is found on the
     * 'o' object, then we throw an VelocityException.
     * @param context Used to access the method cache.
     * @param node  ASTNode used for error reporting.
     */
    public static Object adjMinusIndexArg(Object argument, Object o, 
                               InternalContextAdapter context, SimpleNode node)
    {
      if (argument instanceof Integer && ((Integer)argument).intValue() < 0)
      {
          // The index value is a negative number, $foo[-1], so we want to actually
          // Index [size - value], so try and call the size method.
          VelMethod method = ClassUtils.getMethod("size", noParams, noTypes, 
                             o, context, node, false);
          if (method == null)
          {
              // The object doesn't have a size method, so there is no notion of "at the end"
              throw new VelocityException(
                "A 'size()' method required for negative value "
                 + ((Integer)argument).intValue() + " does not exist for class '" 
                 + o.getClass().getName() + "' at " + Log.formatFileString(node));
          }             

          Object size = null;
          try
          {
              size = method.invoke(o, noParams);
          }
          catch (Exception e)
          {
              throw new VelocityException("Error trying to calls the 'size()' method on '"
                + o.getClass().getName() + "' at " + Log.formatFileString(node), e);
          }
          
          int sizeint = 0;          
          try
          {
              sizeint = ((Integer)size).intValue();
          }
          catch (ClassCastException e)
          {
              // If size() doesn't return an Integer we want to report a pretty error
              throw new VelocityException("Method 'size()' on class '" 
                  + o.getClass().getName() + "' returned '" + size.getClass().getName()
                  + "' when Integer was expected at " + Log.formatFileString(node));
          }
          
          argument = new Integer(sizeint + ((Integer)argument).intValue());
      }
      
      // Nothing to do, return the original argument
      return argument;
    }
    
    public Object execute(Object o, InternalContextAdapter context)
        throws MethodInvocationException
    {
        Object argument = jjtGetChild(0).value(context);
        // If negative, turn -1 into size - 1
        argument = adjMinusIndexArg(argument, o, context, this);
        Object [] params = {argument};
        Class[] paramClasses = {argument == null ? null : argument.getClass()};

        VelMethod method = ClassUtils.getMethod(methodName, params, paramClasses, 
                                                o, context, this, strictRef);

        if (method == null) return null;
    
        try
        {
            /*
             *  get the returned object.  It may be null, and that is
             *  valid for something declared with a void return type.
             *  Since the caller is expecting something to be returned,
             *  as long as things are peachy, we can return an empty
             *  String so ASTReference() correctly figures out that
             *  all is well.
             */
            Object obj = method.invoke(o, params);

            if (obj == null)
            {
                if( method.getReturnType() == Void.TYPE)
                {
                    return "";
                }
            }

            return obj;
        }
        /**
         * pass through application level runtime exceptions
         */
        catch( RuntimeException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            String msg = "Error invoking method 'get("
              + (argument == null ? "null" : argument.getClass().getName()) 
              + ")' in " + o.getClass().getName()
              + " at " + Log.formatFileString(this);
            Logger.error(this,msg, e);
            throw new VelocityException(msg, e);
        }
    }  
}
