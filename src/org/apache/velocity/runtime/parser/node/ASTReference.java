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

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;

import org.apache.velocity.app.event.EventHandlerUtil;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.Renderable;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.directive.Block.Reference;
import org.apache.velocity.runtime.parser.Parser;
import org.apache.velocity.runtime.parser.Token;
import org.apache.velocity.util.ClassUtils;
import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.VelMethod;
import org.apache.velocity.util.introspection.VelPropertySet;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;

/**
 * This class is responsible for handling the references in
 * VTL ($foo).
 *
 * Please look at the Parser.jjt file which is
 * what controls the generation of this class.
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @author <a href="mailto:Christoph.Reck@dlr.de">Christoph Reck</a>
 * @author <a href="mailto:kjohnson@transparent.com>Kent Johnson</a>
 * @version $Id: ASTReference.java 832302 2009-11-03 05:32:31Z wglass $
*/
public class ASTReference extends SimpleNode
{
    /* Reference types */
    private static final int NORMAL_REFERENCE = 1;
    private static final int FORMAL_REFERENCE = 2;
    private static final int QUIET_REFERENCE = 3;
    private static final int RUNT = 4;

    private int referenceType;
    private String nullString;
    private String rootString;
    private boolean escaped = false;
    private boolean computableReference = true;
    private boolean logOnNull = true;
    private String escPrefix = "";
    private String morePrefix = "";
    private String identifier = "";

    private String literal = null;

    /**
     * Indicates if we are running in strict reference mode.
     */
    public boolean strictRef = false;
    
    /**
     * non null Indicates if we are setting an index reference e.g, $foo[2], which basically
     * means that the last syntax of the reference are brackets.
     */
    private ASTIndex astIndex = null;
    
    /**
     * Indicates if we are using modified escape behavior in strict mode.
     * mainly we allow \$abc -> to render as $abc
     */
    public boolean strictEscape = false;
    
    /**
     * Indicates if toString() should be called during condition evaluation just
     * to ensure it does not return null. Check is unnecessary if all toString()
     * implementations are known to have non-null return values. Disabling the
     * check will give a performance improval since toString() may be a complex
     * operation on large objects.
     */
    public boolean toStringNullCheck = true;
    
    private int numChildren = 0;

    protected Info uberInfo;

    /**
     * @param id
     */
    public ASTReference(int id)
    {
        super(id);
    }

    /**
     * @param p
     * @param id
     */
    public ASTReference(Parser p, int id)
    {
        super(p, id);
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.SimpleNode#jjtAccept(org.apache.velocity.runtime.parser.node.ParserVisitor, java.lang.Object)
     */
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.SimpleNode#init(org.apache.velocity.context.InternalContextAdapter, java.lang.Object)
     */
    public Object init(InternalContextAdapter context, Object data)
    throws TemplateInitException
    {
        super.init(context, data);
        RuntimeServices rsvc=VelocityUtil.getEngine().getRuntimeServices();
        
        strictEscape = rsvc.getBoolean(RuntimeConstants.RUNTIME_REFERENCES_STRICT_ESCAPE, false);
        strictRef = rsvc.getBoolean(RuntimeConstants.RUNTIME_REFERENCES_STRICT, false);
        toStringNullCheck = rsvc.getBoolean(RuntimeConstants.DIRECTIVE_IF_TOSTRING_NULLCHECK, true); 
            
        /*
         *  the only thing we can do in init() is getRoot()
         *  as that is template based, not context based,
         *  so it's thread- and context-safe
         */

        rootString = getRoot().intern();

        numChildren = jjtGetNumChildren();
        
        // This is an expensive call, so get it now.
        literal = literal();
        
        /*
         * and if appropriate...
         */

        if (numChildren > 0 )
        {
            Node lastNode = jjtGetChild(numChildren-1);
            if (lastNode instanceof ASTIndex)
                astIndex = (ASTIndex)lastNode;
            else
                identifier = lastNode.getFirstToken().image;            
        }

        /*
         * make an uberinfo - saves new's later on
         */
        uberInfo = new Info(getTemplateName(), getLine(),getColumn());

        /*
         * track whether we log invalid references
         */
        logOnNull =
            rsvc.getBoolean(RuntimeConstants.RUNTIME_LOG_REFERENCE_LOG_INVALID, true);

        /**
         * In the case we are referencing a variable with #if($foo) or
         * #if( ! $foo) then we allow variables to be undefined and we 
         * set strictRef to false so that if the variable is undefined
         * an exception is not thrown. 
         */
        if (strictRef && numChildren == 0)
        {
            logOnNull = false; // Strict mode allows nulls
            
            Node node = this.jjtGetParent();
            if (node instanceof ASTNotNode     // #if( ! $foo)
             || node instanceof ASTExpression  // #if( $foo )
             || node instanceof ASTOrNode      // #if( $foo || ...
             || node instanceof ASTAndNode)    // #if( $foo && ...
            {
                // Now scan up tree to see if we are in an If statement
                while (node != null)
                {
                    if (node instanceof ASTIfStatement)
                    {
                       strictRef = false;
                       break;
                    }
                    node = node.jjtGetParent();
                }
            }
        }
                
        return data;
    }

    /**
     *  Returns the 'root string', the reference key
     * @return the root string.
     */
     public String getRootString()
     {
        return rootString;
     }

    /**
     *   gets an Object that 'is' the value of the reference
     *
     *   @param o   unused Object parameter
     *   @param context context used to generate value
     * @return The execution result.
     * @throws MethodInvocationException
     */
    public Object execute(Object o, InternalContextAdapter context)
        throws MethodInvocationException
    {

        if (referenceType == RUNT)
            return null;

        /*
         *  get the root object from the context
         */

        Object result = getVariableValue(context, rootString);
        RuntimeServices rsvc=VelocityUtil.getEngine().getRuntimeServices();
        
        if (result == null && !strictRef)
        {
            return EventHandlerUtil.invalidGetMethod(rsvc, context, 
                    getDollarBang() + rootString, null, null, uberInfo);
        }

        /*
         * Iteratively work 'down' (it's flat...) the reference
         * to get the value, but check to make sure that
         * every result along the path is valid. For example:
         *
         * $hashtable.Customer.Name
         *
         * The $hashtable may be valid, but there is no key
         * 'Customer' in the hashtable so we want to stop
         * when we find a null value and return the null
         * so the error gets logged.
         */

        try
        {
            Object previousResult = result; 
            int failedChild = -1;
            for (int i = 0; i < numChildren; i++)
            {
                if (strictRef && result == null)
                {
                    /**
                     * At this point we know that an attempt is about to be made
                     * to call a method or property on a null value.
                     */
                    String name = jjtGetChild(i).getFirstToken().image;
                    throw new VelocityException("Attempted to access '"  
                        + name + "' on a null value at "
                        + VelocityException.formatFileString(uberInfo.getTemplateName(),
                        + jjtGetChild(i).getLine(), jjtGetChild(i).getColumn()));                  
                }
                previousResult = result;
                result = jjtGetChild(i).execute(result,context);
                if (result == null && !strictRef)  // If strict and null then well catch this
                                                   // next time through the loop
                {
                    failedChild = i;
                    break;
                }
            }

            if (result == null)
            {
                if (failedChild == -1)
                {
                    result = EventHandlerUtil.invalidGetMethod(rsvc, context, 
                            getDollarBang() + rootString, previousResult, null, uberInfo);                    
                }
                else
                {
                    StringBuffer name = new StringBuffer(getDollarBang()).append(rootString);
                    for (int i = 0; i <= failedChild; i++)
                    {
                        Node node = jjtGetChild(i);
                        if (node instanceof ASTMethod)
                        {
                            name.append(".").append(((ASTMethod) node).getMethodName()).append("()");
                        }
                        else
                        {
                            name.append(".").append(node.getFirstToken().image);
                        }
                    }
                    
                    if (jjtGetChild(failedChild) instanceof ASTMethod)
                    {
                        String methodName = ((ASTMethod) jjtGetChild(failedChild)).getMethodName();
                        result = EventHandlerUtil.invalidMethod(rsvc, context, 
                                name.toString(), previousResult, methodName, uberInfo);                                                                
                    }
                    else
                    {
                        String property = jjtGetChild(failedChild).getFirstToken().image;
                        result = EventHandlerUtil.invalidGetMethod(rsvc, context, 
                                name.toString(), previousResult, property, uberInfo);                        
                    }
                }
                
            }
            
            return result;
        }
        catch(MethodInvocationException mie)
        {
            mie.setReferenceName(rootString);
            throw mie;
        }
    }

    /**
     *  gets the value of the reference and outputs it to the
     *  writer.
     *
     *  @param context  context of data to use in getting value
     *  @param writer   writer to render to
     * @return True if rendering was successful.
     * @throws IOException
     * @throws MethodInvocationException
     */
    public boolean render(InternalContextAdapter context, Writer writer) throws IOException,
            MethodInvocationException
    {
        if (referenceType == RUNT)
        {
            writer.write(rootString);
            return true;
        }

        Object value = null;
        if (escaped && strictEscape)
        {
          /**
           * If we are in strict mode and the variable is escaped, then don't bother to
           * retreive the value since we won't use it. And if the var is not defined 
           * it will throw an exception.  Set value to TRUE to fall through below with
           * simply printing $foo, and not \$foo
           */
          value = Boolean.TRUE;
        }
        else
        {
          value = execute(null, context);
        }

        String localNullString = null;

        /*
         * if this reference is escaped (\$foo) then we want to do one of two things : 1) if this is
         * a reference in the context, then we want to print $foo 2) if not, then \$foo (its
         * considered schmoo, not VTL)
         */

        if (escaped)
        {
            localNullString = getNullString(context);
            
            if (value == null)
            {
                writer.write(escPrefix);
                writer.write("\\");
                writer.write(localNullString);
            }
            else
            {
                writer.write(escPrefix);
                writer.write(localNullString);
            }
            return true;
        }

        /*
         * the normal processing
         * 
         * if we have an event cartridge, get a new value object
         */
        RuntimeServices rsvc=VelocityUtil.getEngine().getRuntimeServices();
        value = EventHandlerUtil.referenceInsert(rsvc, context, literal, value);

        String toString = null;
        if (value != null)
        {          
            if (value instanceof Renderable)
            {
                Renderable renderable = (Renderable)value;
                try
                {
                    if (renderable.render(context,writer))
                      return true;
                }
                catch(RuntimeException e)
                {
                    // We commonly get here when an error occurs within a block reference.
                    // We want to log where the reference is at so that a developer can easily
                    // know where the offending call is located.  This can be seen
                    // as another element of the error stack we report to log.
                    Logger.error(this,"Exception rendering "
                        + ((renderable instanceof Reference)? "block ":"Renderable ")
                        + rootString + " at " + VelocityException.formatFileString(this));
                    throw e;
                }
            }

            toString = value.toString();
        }

        if (value == null || toString == null)
        {          
            if (strictRef)
            {
                if (referenceType != QUIET_REFERENCE)
                {
                  Logger.error(this,"Prepend the reference with '$!' e.g., $!" + literal().substring(1)
                      + " if you want Velocity to ignore the reference when it evaluates to null");
                  if (value == null)
                  {
                    throw new VelocityException("Reference " + literal() 
                        + " evaluated to null when attempting to render at " 
                        + VelocityException.formatFileString(this));
                  }
                  else  // toString == null
                  {
                    // This will probably rarely happen, but when it does we want to
                    // inform the user that toString == null so they don't pull there
                    // hair out wondering why Velocity thinks the value is null.                    
                    throw new VelocityException("Reference " + literal()
                        + " evaluated to object " + value.getClass().getName()
                        + " whose toString() method returned null at "
                        + VelocityException.formatFileString(this));
                  }
                }              
                return true;
            }
          
            /*
             * write prefix twice, because it's schmoo, so the \ don't escape each
             * other...
             */
            localNullString = getNullString(context);
            if (!strictEscape)
            {
                // If in strict escape mode then we only print escape once.
                // Yea, I know.. brittle stuff
                writer.write(escPrefix);
            }            
            writer.write(escPrefix);
            writer.write(morePrefix);
            writer.write(localNullString);

            if (logOnNull && referenceType != QUIET_REFERENCE && Logger.isDebugEnabled(this.getClass()))
            {
                Logger.debug(this,"Null reference [template '" + getTemplateName()
                        + "', line " + this.getLine() + ", column " + this.getColumn() + "] : "
                        + this.literal() + " cannot be resolved.");
            }
            return true;
        }
        else
        {
            /*
             * non-null processing
             */
            writer.write(escPrefix);
            writer.write(morePrefix);
            writer.write(toString);

            return true;
        }
    }

    /**
     * This method helps to implement the "render literal if null" functionality.
     * 
     * VelocimacroProxy saves references to macro arguments (AST nodes) so that if we have a macro
     * #foobar($a $b) then there is key "$a.literal" which points to the literal presentation of the
     * argument provided to variable $a. If the value of $a is null, we render the string that was
     * provided as the argument.
     * 
     * @param context
     * @return
     */
    private String getNullString(InternalContextAdapter context)
    {
        Object callingArgument = context.get(".literal." + nullString);

        if (callingArgument != null)
            return ((Node) callingArgument).literal();
        else
            return nullString;
    }

    /**
     *   Computes boolean value of this reference
     *   Returns the actual value of reference return type
     *   boolean, and 'true' if value is not null
     *
     *   @param context context to compute value with
     * @return True if evaluation was ok.
     * @throws MethodInvocationException
     */
    public boolean evaluate(InternalContextAdapter context)
        throws MethodInvocationException
    {
        Object value = execute(null, context);

        if (value == null)
        {
            return false;
        }
        else if (value instanceof Boolean)
        {
            if (((Boolean) value).booleanValue())
                return true;
            else
                return false;
        }        
        else if (toStringNullCheck)
        {
            try
            {
                return value.toString() != null;
            }
            catch(Exception e)
            {
                throw new VelocityException("Reference evaluation threw an exception at " 
                    + VelocityException.formatFileString(this), e);
            }
        }
        else
        {
            return true;
        }
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.SimpleNode#value(org.apache.velocity.context.InternalContextAdapter)
     */
    public Object value(InternalContextAdapter context)
        throws MethodInvocationException
    {
        return (computableReference ? execute(null, context) : null);
    }

    
    /**
     * Utility class to handle nulls when printing a class type
     */
    public static String printClass(Class clazz)
    {
      return clazz == null ? "null" : clazz.getName();
    }
    
    
    /**
     *  Sets the value of a complex reference (something like $foo.bar)
     *  Currently used by ASTSetReference()
     *
     *  @see ASTSetDirective
     *
     *  @param context context object containing this reference
     *  @param value Object to set as value
     *  @return true if successful, false otherwise
     * @throws MethodInvocationException
     */
    public boolean setValue( InternalContextAdapter context, Object value)
      throws MethodInvocationException
    {
        if (jjtGetNumChildren() == 0)
        {
            context.put(rootString, value);
            return true;
        }

        /*
         *  The rootOfIntrospection is the object we will
         *  retrieve from the Context. This is the base
         *  object we will apply reflection to.
         */

        Object result = getVariableValue(context, rootString);

        if (result == null)
        {
            String msg = "reference set is not a valid reference at "
                    + VelocityException.formatFileString(uberInfo);
            Logger.error(this,msg);
            return false;
        }

        /*
         * How many child nodes do we have?
         */

        for (int i = 0; i < numChildren - 1; i++)
        {
            result = jjtGetChild(i).execute(result, context);

            if (result == null)
            {
                if (strictRef)
                {
                    String name = jjtGetChild(i+1).getFirstToken().image;
                    throw new MethodInvocationException("Attempted to access '"  
                        + name + "' on a null value", null, name, uberInfo.getTemplateName(),
                        jjtGetChild(i+1).getLine(), jjtGetChild(i+1).getColumn());
                }            
              
                String msg = "reference set is not a valid reference at "
                    + VelocityException.formatFileString(uberInfo);
                Logger.error(this,msg);

                return false;
            }
        }

        if (astIndex != null)
        {
            // If astIndex is not null then we are actually setting an index reference,
            // something of the form $foo[1] =, or in general any reference that ends with
            // the brackets.  This means that we need to call a more general method
            // of the form set(Integer, <something>), or put(Object, <something), where
            // the first parameter is the index value and the second is the LHS of the set.
          
            Object argument = astIndex.jjtGetChild(0).value(context);
            // If negative, turn -1 into (size - 1)
            argument = ASTIndex.adjMinusIndexArg(argument, result, context, astIndex);            
            Object [] params = {argument, value};
            Class[] paramClasses = {params[0] == null ? null : params[0].getClass(), 
                                    params[1] == null ? null : params[1].getClass()};

            String methodName = "set";
            VelMethod method = ClassUtils.getMethod(methodName, params, paramClasses, 
                result, context, astIndex, false);
            
            if (method == null)
            {
                // If we can't find a 'set' method, lets try 'put',  This warrents a little
                // investigation performance wise... if the user is using the hash 
                // form $foo["blaa"], then it may be expensive to first try and fail on 'set'
                // then go to 'put'?  The problem is that getMethod will try the cache, then
                // perform introspection on 'result' for 'set'
                methodName = "put";
                method = ClassUtils.getMethod(methodName, params, paramClasses, 
                      result, context, astIndex, false);
            }   
            
            if (method == null)
            {
                // couldn't find set or put method, so bail
                if (strictRef)
                {
                    throw new VelocityException(
                        "Found neither a 'set' or 'put' method with param types '("
                        + printClass(paramClasses[0]) + "," + printClass(paramClasses[1])
                        + ")' on class '" + result.getClass().getName() 
                        + "' at " + VelocityException.formatFileString(astIndex));
                }
                return false;
            }
          
            try
            { 
                method.invoke(result, params);
            }
            catch(RuntimeException e)
            {
                // Kludge since invoke throws Exception, pass up Runtimes
                throw e;
            }
            catch(Exception e)
            {
                throw new MethodInvocationException(
                  "Exception calling method '"
                  + methodName + "(" 
                  + printClass(paramClasses[0]) + "," + printClass(paramClasses[1])
                  + ")' in  " + result.getClass(),
                  e.getCause(), identifier, astIndex.getTemplateName(), astIndex.getLine(), 
                    astIndex.getColumn());
            }
            
            return true;
        }
        
        
        /*
         *  We support two ways of setting the value in a #set($ref.foo = $value ) :
         *  1) ref.setFoo( value )
         *  2) ref,put("foo", value ) to parallel the get() map introspection
         */

        try
        {
            RuntimeServices rsvc=VelocityUtil.getEngine().getRuntimeServices();
            VelPropertySet vs =
                    rsvc.getUberspect().getPropertySet(result, identifier,
                            value, uberInfo);

            if (vs == null)
            {
                if (strictRef)
                {
                    throw new MethodInvocationException("Object '" + result.getClass().getName() +
                       "' does not contain property '" + identifier + "'", null, identifier,
                       uberInfo.getTemplateName(), uberInfo.getLine(), uberInfo.getColumn());
                }
                else
                {
                  return false;
                }
            }

            vs.invoke(result, value);
        }
        catch(InvocationTargetException ite)
        {
            /*
             *  this is possible
             */

            throw  new MethodInvocationException(
                "ASTReference : Invocation of method '"
                + identifier + "' in  " + result.getClass()
                + " threw exception "
                + ite.getTargetException().toString(),
               ite.getTargetException(), identifier, getTemplateName(), this.getLine(), this.getColumn());
        }
        /**
         * pass through application level runtime exceptions
         */
        catch( RuntimeException e )
        {
            throw e;
        }
        catch(Exception e)
        {
            /*
             *  maybe a security exception?
             */
            String msg = "ASTReference setValue() : exception : " + e
                          + " template at " + VelocityException.formatFileString(uberInfo);
            Logger.error(this,msg, e);
            throw new VelocityException(msg, e);
         }

        return true;
    }

    private String getRoot()
    {
        Token t = getFirstToken();

        /*
         *  we have a special case where something like
         *  $(\\)*!, where the user want's to see something
         *  like $!blargh in the output, but the ! prevents it from showing.
         *  I think that at this point, this isn't a reference.
         */

        /* so, see if we have "\\!" */

        int slashbang = t.image.indexOf("\\!");

        if (slashbang != -1)
        {
            if (strictEscape)
            {
                // If we are in strict escape mode, then we consider this type of 
                // pattern a non-reference, and we print it out as schmoo...
                nullString = literal();
                escaped = true;
                return literal();
            }
          
            /*
             *  lets do all the work here.  I would argue that if this occurrs,
             *  it's not a reference at all, so preceeding \ characters in front
             *  of the $ are just schmoo.  So we just do the escape processing
             *  trick (even | odd) and move on.  This kind of breaks the rule
             *  pattern of $ and # but '!' really tosses a wrench into things.
             */

             /*
              *  count the escapes : even # -> not escaped, odd -> escaped
              */

            int i = 0;
            int len = t.image.length();

            i = t.image.indexOf('$');

            if (i == -1)
            {
                /* yikes! */
                Logger.error(this,"ASTReference.getRoot() : internal error : "
                            + "no $ found for slashbang.");
                computableReference = false;
                nullString = t.image;
                return nullString;
            }

            while (i < len && t.image.charAt(i) != '\\')
            {
                i++;
            }

            /*  ok, i is the first \ char */

            int start = i;
            int count = 0;

            while (i < len && t.image.charAt(i++) == '\\')
            {
                count++;
            }

            /*
             *  now construct the output string.  We really don't care about
             *  leading  slashes as this is not a reference.  It's quasi-schmoo
             */

            nullString = t.image.substring(0,start); // prefix up to the first
            nullString += t.image.substring(start, start + count-1 ); // get the slashes
            nullString += t.image.substring(start+count); // and the rest, including the

            /*
             *  this isn't a valid reference, so lets short circuit the value
             *  and set calcs
             */

            computableReference = false;

            return nullString;
        }

        /*
         *  we need to see if this reference is escaped.  if so
         *  we will clean off the leading \'s and let the
         *  regular behavior determine if we should output this
         *  as \$foo or $foo later on in render(). Lazyness..
         */

        escaped = false;

        if (t.image.startsWith("\\"))
        {
            /*
             *  count the escapes : even # -> not escaped, odd -> escaped
             */

            int i = 0;
            int len = t.image.length();

            while (i < len && t.image.charAt(i) == '\\')
            {
                i++;
            }

            if ((i % 2) != 0)
                escaped = true;

            if (i > 0)
                escPrefix = t.image.substring(0, i / 2 );

            t.image = t.image.substring(i);
        }

        /*
         *  Look for preceeding stuff like '#' and '$'
         *  and snip it off, except for the
         *  last $
         */

        int loc1 = t.image.lastIndexOf('$');

        /*
         *  if we have extra stuff, loc > 0
         *  ex. '#$foo' so attach that to
         *  the prefix.
         */
        if (loc1 > 0)
        {
            morePrefix = morePrefix + t.image.substring(0, loc1);
            t.image = t.image.substring(loc1);
        }

        /*
         *  Now it should be clean. Get the literal in case this reference
         *  isn't backed by the context at runtime, and then figure out what
         *  we are working with.
         */

        // FIXME: this is the key to render nulls as literals, we need to look at context(refname+".literal") 
        nullString = literal();

        if (t.image.startsWith("$!"))
        {
            referenceType = QUIET_REFERENCE;

            /*
             *  only if we aren't escaped do we want to null the output
             */

            if (!escaped)
                nullString = "";

            if (t.image.startsWith("$!{"))
            {
                /*
                 *  ex : $!{provider.Title}
                 */

                return t.next.image;
            }
            else
            {
                /*
                 *  ex : $!provider.Title
                 */

                return t.image.substring(2);
            }
        }
        else if (t.image.equals("${"))
        {
            /*
             *  ex : ${provider.Title}
             */

            referenceType = FORMAL_REFERENCE;
            return t.next.image;
        }
        else if (t.image.startsWith("$"))
        {
            /*
             *  just nip off the '$' so we have
             *  the root
             */

            referenceType = NORMAL_REFERENCE;
            return t.image.substring(1);
        }
        else
        {
            /*
             * this is a 'RUNT', which can happen in certain circumstances where
             *  the parser is fooled into believeing that an IDENTIFIER is a real
             *  reference.  Another 'dreaded' MORE hack :).
             */
            referenceType = RUNT;
            return t.image;
        }

    }

    /**
     * @param context
     * @param variable
     * @return The evaluated value of the variable.
     * @throws MethodInvocationException
     */
    public Object getVariableValue(Context context, String variable) throws MethodInvocationException
    {
        Object obj = null;
        try
        {
            obj = context.get(variable);
        }
        catch(RuntimeException e)
        {
            Logger.error(this,"Exception calling reference $" + variable + " at "
                      + VelocityException.formatFileString(uberInfo));
            throw e;
        }
        
        if (strictRef && obj == null)
        {
          if (!context.containsKey(variable))
          {
              Logger.error(this,"Variable $" + variable + " has not been set at "
                        + VelocityException.formatFileString(uberInfo));
              throw new MethodInvocationException("Variable $" + variable +
                  " has not been set", null, identifier,
                  uberInfo.getTemplateName(), uberInfo.getLine(), uberInfo.getColumn());            
          }
        }
        return obj;        
    }

    public String getDollarBang()
    {
        return (referenceType == QUIET_REFERENCE) ? "$!" : "$";
    }
}
