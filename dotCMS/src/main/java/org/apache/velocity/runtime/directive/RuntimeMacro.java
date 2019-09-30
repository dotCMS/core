package org.apache.velocity.runtime.directive;

import com.dotcms.rendering.velocity.services.DotResourceCache;

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

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.Renderable;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.parser.ParserTreeConstants;
import org.apache.velocity.runtime.parser.Token;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.util.introspection.Info;

/**
 * This class acts as a proxy for potential macros.  When the AST is built
 * this class is inserted as a placeholder for the macro (whether or not
 * the macro is actually defined).  At render time we check whether there is
 * a implementation for the macro call. If an implementation cannot be
 * found the literal text is rendered.
 * @since 1.6
 */
public class RuntimeMacro extends Directive
{
    /**
     * Name of the macro
     */
    private String macroName;
    private final static String EVALING_MACRO="DOT_EVALING_MACRO";
    /**
     * Literal text of the macro
     */
    private String literal = null;

    /**
     * Node of the macro call
     */
    private Node node = null;

    /**
     * Indicates if we are running in strict reference mode.
     */
    protected boolean strictRef = false;
        
    /**
     * badArgsErrorMsg will be non null if the arguments to this macro
     * are deamed bad at init time, see the init method.  If his is non null, then this macro 
     * cannot be rendered, and if there is an attempt to render we throw an exception
     * with this as the message.
     */
    private String badArgsErrorMsg = null;
    
    /**
     * Create a RuntimeMacro instance. Macro name and source
     * template stored for later use.
     *
     * @param macroName name of the macro
     */
    public RuntimeMacro(String macroName)
    {
        if (macroName == null)
        {
            throw new IllegalArgumentException("Null arguments");
        }
        
        this.macroName = macroName;
    }

    /**
     * Return name of this Velocimacro.
     *
     * @return The name of this Velocimacro.
     */
    public String getName()
    {
        return macroName;
    }

    /**
     * Override to always return "macro".  We don't want to use
     * the macro name here, since when writing VTL that uses the
     * scope, we are within a #macro call.  The macro name will instead
     * be used as the scope name when defining the body of a BlockMacro.
     */
    public String getScopeName()
    {
        return "macro";
    }

    /**
     * Velocimacros are always LINE
     * type directives.
     *
     * @return The type of this directive.
     */
    public int getType()
    {
        return LINE;
    }


    /**
     * Intialize the Runtime macro. At the init time no implementation so we
     * just save the values to use at the render time.
     *
     * @param rs runtime services
     * @param context InternalContextAdapter
     * @param node node containing the macro call
     */
    public void init(RuntimeServices rs, InternalContextAdapter context,
                     Node node)
    {
        super.init(rs, context, node);
        
        
        Token tok = node.getFirstToken();
        Token t = node.getLastToken();
        StringBuilder sb = new StringBuilder();
        while(tok!=null){
          sb.append(tok.image);
          if(tok.equals(t))break;
          tok=tok.next;
        }
        literal = sb.toString();  

        
            
        /**
         * Apply strictRef setting only if this really looks like a macro,
         * so strict mode doesn't balk at things like #E0E0E0 in a template.
         * compare with ")" is a simple #foo() style macro, comparing to
         * "#end" is a block style macro. We use starts with because the token
         * may end with '\n'
         */

        
        if (t.image.startsWith(")") || t.image.startsWith("#end"))
        {
            RuntimeServices rsvc=VelocityUtil.getEngine().getRuntimeServices();
            strictRef = rsvc.getBoolean(RuntimeConstants.RUNTIME_REFERENCES_STRICT, false);
        }
                
        // Validate that none of the arguments are plain words, (VELOCITY-614)
        // they should be string literals, references, inline maps, or inline lists
        for (int n=0; n < node.jjtGetNumChildren(); n++)
        {
            Node child = node.jjtGetChild(n);
            if (child.getType() == ParserTreeConstants.JJTWORD)
            {
                badArgsErrorMsg = "Invalid arg '" + child.getFirstTokenImage()
                + "' in macro #" + macroName + " at " + VelocityException.formatFileString(child);
              
                if (strictRef)  // If strict, throw now
                {
                    /* indicate col/line assuming it starts at 0
                     * this will be corrected one call up  */
                    throw new TemplateInitException(badArgsErrorMsg,
                        context.getCurrentTemplateName(), 0, 0);
                }
            }
        }               
    }

    /**
     * It is probably quite rare that we need to render the macro literal
     * so do it only on-demand and then cache the value. This tactic helps to
     * reduce memory usage a bit.
     */
    private String getLiteral()
    {
        if (literal == null)
        {
            StrBuilder buffer = new StrBuilder();
            Token t = node.getFirstToken();
            while (t != null && t != node.getLastToken()){
                buffer.append(t.image);
              t = t.next;
            }
            
            literal = buffer.toString();
        }
        return literal;
    }
    

    /**
     * Velocimacro implementation is not known at the init time. So look for
     * a implementation in the macro libaries and if finds one renders it. The
     * actual rendering is delegated to the VelocimacroProxy object. When
     * looking for a macro we first loot at the template with has the
     * macro call then we look at the macro lbraries in the order they appear
     * in the list. If a macro has many definitions above look up will
     * determine the precedence.
     *
     * @param context
     * @param writer
     * @param node
     * @return true if the rendering is successful
     * @throws IOException
     * @throws ResourceNotFoundException
     * @throws ParseErrorException
     * @throws MethodInvocationException
     */
    public boolean render(InternalContextAdapter context, Writer writer,
                          Node node)
            throws IOException, ResourceNotFoundException,
            ParseErrorException, MethodInvocationException
    {
        return render(context, writer, node, null);
    }
    
    /**
     * This method is used with BlockMacro when we want to render a macro with a body AST.
     *
     * @param context
     * @param writer
     * @param node
     * @param body AST block that was enclosed in the macro body.
     * @return true if the rendering is successful
     * @throws IOException
     * @throws ResourceNotFoundException
     * @throws ParseErrorException
     * @throws MethodInvocationException
     */
    public boolean render(InternalContextAdapter context, Writer writer,
                          Node node, Renderable body)
            throws IOException, ResourceNotFoundException,
            ParseErrorException, MethodInvocationException
    {
        VelocimacroProxy vmProxy = null;
        String renderingTemplate = context.getCurrentTemplateName();
        
        /**
         * first look in the source template
         */
        RuntimeServices rsvc=VelocityUtil.getEngine().getRuntimeServices();
        Object o = rsvc.getVelocimacro(macroName, getTemplateName(), renderingTemplate);

        if( o != null )
        {
            // getVelocimacro can only return a VelocimacroProxy so we don't need the
            // costly instanceof check
            vmProxy = (VelocimacroProxy)o;
        }

        /**
         * if not found, look in the macro libraries.
         */
        if (vmProxy == null)
        {
            List macroLibraries = context.getMacroLibraries();
            if (macroLibraries != null)
            {
                for (int i = macroLibraries.size() - 1; i >= 0; i--)
                {
                    o = rsvc.getVelocimacro(macroName,
                            (String)macroLibraries.get(i), renderingTemplate);

                    // get the first matching macro
                    if (o != null)
                    {
                        vmProxy = (VelocimacroProxy) o;
                        break;
                    }
                }
            }
        }

        if (vmProxy != null)
        {
            try
            {
            	// mainly check the number of arguments
                vmProxy.checkArgs(context, node, body != null);
            }
            catch (TemplateInitException die)
            {
                throw new ParseErrorException(die.getMessage() + " at "
                    + VelocityException.formatFileString(node), new Info(node));
            }

            if (badArgsErrorMsg != null)
            {
                throw new TemplateInitException(badArgsErrorMsg,
                  context.getCurrentTemplateName(), node.getColumn(), node.getLine());
            }

            try
            {
                preRender(context);
                return vmProxy.render(context, writer, node, body);
            }
            catch (StopCommand stop)
            {
                if (!stop.isFor(this))
                {
                    throw stop;
                }
                return true;
            }
            catch (RuntimeException e)
            {
                /**
                 * We catch, the exception here so that we can record in
                 * the logs the template and line number of the macro call
                 * which generate the exception.  This information is
                 * especially important for multiple macro call levels.
                 * this is also true for the following catch blocks.
                 */
                Logger.error(this,"Exception in macro #" + macroName + " called at " +
                        VelocityException.formatFileString(node));
                throw e;
            }
            catch (IOException e)
            {
                Logger.error(this,"Exception in macro #" + macroName + " called at " +
                        VelocityException.formatFileString(node));
                throw e;
            }
            finally
            {
                postRender(context);
            }
        }

        else if(vmProxy==null && !strictRef){
            try{
               String[] macroMap = CacheLocator.getVeloctyResourceCache().getMacro(macroName);
                if(macroMap != null && context.get(EVALING_MACRO)==null) {
                    if(macroMap.length>0 && !macroMap[0].equals(DotResourceCache.MACRO404[0])) {
                        Context contextForEval = new VelocityContext(context);
                        contextForEval.put(EVALING_MACRO, Boolean.TRUE);
                        VelocityUtil.eval(macroMap[1], contextForEval);
                        return this.render(context, writer, node);
                    }
                }
                else if(macroMap==null){
                    CacheLocator.getVeloctyResourceCache().putMacro404(macroName);
                }
            } catch (Exception e) {
              Logger.warn(this,"Unable to load macro #" + macroName + " called at " +
                  VelocityException.formatFileString(node));
            }
        }
        else if (vmProxy==null){
            throw new VelocityException("Macro '#" + macroName + "' is not defined at "
                + VelocityException.formatFileString(node));
        }
        
        /**
         * If we cannot find an implementation write the literal text
         */
        writer.write(getLiteral());
        return true;
    }
}
