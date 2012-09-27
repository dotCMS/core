package org.apache.velocity;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;

import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.directive.Scope;
import org.apache.velocity.runtime.directive.StopCommand;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.ResourceManager;

/**
 * This class is used for controlling all template
 * operations. This class uses a parser created
 * by JavaCC to create an AST that is subsequently
 * traversed by a Visitor.
 *
 * <pre>
 * // set up and initialize Velocity before this code block
 *
 * Template template = Velocity.getTemplate("test.wm");
 * Context context = new VelocityContext();
 *
 * context.put("foo", "bar");
 * context.put("customer", new Customer());
 *
 * template.merge(context, writer);
 * </pre>
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: Template.java 778045 2009-05-23 22:17:46Z nbubna $
 */
public class Template extends Resource
{
    /*
     * The name of the variable to use when placing
     * the scope object into the context.
     */
    private String scopeName = "template";
    private boolean provideScope = false;

    private VelocityException errorCondition = null;

    /** Default constructor */
    public Template()
    {
        super();
        
        setType(ResourceManager.RESOURCE_TEMPLATE);
    }

    /**
     *  gets the named resource as a stream, parses and inits
     *
     * @return true if successful
     * @throws ResourceNotFoundException if template not found
     *          from any available source.
     * @throws ParseErrorException if template cannot be parsed due
     *          to syntax (or other) error.
     * @throws IOException problem reading input stream
     */
    public boolean process()
        throws ResourceNotFoundException, ParseErrorException
    {
        data = null;
        InputStream is = null;
        errorCondition = null;

        /*
         *  first, try to get the stream from the loader
         */
        try
        {
            is = resourceLoader.getResourceStream(name);
        }
        catch( ResourceNotFoundException rnfe )
        {
            /*
             *  remember and re-throw
             */

            errorCondition = rnfe;
            throw rnfe;
        }

        /*
         *  if that worked, lets protect in case a loader impl
         *  forgets to throw a proper exception
         */

        if (is != null)
        {
            /*
             *  now parse the template
             */

            try
            {
                BufferedReader br = new BufferedReader( new InputStreamReader( is, encoding ) );
                data = rsvc.parse( br, name);
                initDocument();
                return true;
            }
            catch( UnsupportedEncodingException  uce )
            {
                String msg = "Template.process : Unsupported input encoding : " + encoding
                + " for template " + name;

                errorCondition  = new ParseErrorException( msg );
                throw errorCondition;
            }
            catch ( ParseException pex )
            {
                /*
                 *  remember the error and convert
                 */
                errorCondition =  new ParseErrorException(pex, name);
                throw errorCondition;
            }
            catch ( TemplateInitException pex )
            {
                errorCondition = new ParseErrorException( pex, name);
                throw errorCondition;
            }
            /**
             * pass through runtime exceptions
             */
            catch( RuntimeException e )
            {
                errorCondition = new VelocityException("Exception thrown processing Template "
                    +getName(), e);
                throw errorCondition;
            }
            finally
            {
                /*
                 *  Make sure to close the inputstream when we are done.
                 */
                try
                {
                    is.close();
                }
                catch(IOException e)
                {
                    // If we are already throwing an exception then we want the original
                    // exception to be continued to be thrown, otherwise, throw a new Exception.
                    if (errorCondition == null)
                    {
                         throw new VelocityException(e);
                    }                    
                }
            }
        }
        else
        {
            /*
             *  is == null, therefore we have some kind of file issue
             */
            errorCondition = new ResourceNotFoundException("Unknown resource error for resource " + name );
            throw errorCondition;
        }
    }

    /**
     *  initializes the document.  init() is not longer
     *  dependant upon context, but we need to let the
     *  init() carry the template name down throught for VM
     *  namespace features
     * @throws TemplateInitException When a problem occurs during the document initialization.
     */
    public void initDocument()
    throws TemplateInitException
    {
        /*
         *  send an empty InternalContextAdapter down into the AST to initialize it
         */

        InternalContextAdapterImpl ica = new InternalContextAdapterImpl(  new VelocityContext() );

        try
        {
            /*
             *  put the current template name on the stack
             */

            ica.pushCurrentTemplateName( name );
            ica.setCurrentResource( this );

            /*
             *  init the AST
             */

            ((SimpleNode)data).init( ica, rsvc);

            String property = scopeName+'.'+RuntimeConstants.PROVIDE_SCOPE_CONTROL;
            provideScope = rsvc.getBoolean(property, provideScope);
        }
        finally
        {
            /*
             *  in case something blows up...
             *  pull it off for completeness
             */

            ica.popCurrentTemplateName();
            ica.setCurrentResource( null );
        }

    }

    /**
     * The AST node structure is merged with the
     * context to produce the final output.
     *
     *  @param context Conext with data elements accessed by template
     *  @param writer output writer for rendered template
     *  @throws ResourceNotFoundException if template not found
     *          from any available source.
     *  @throws ParseErrorException if template cannot be parsed due
     *          to syntax (or other) error.
     *  @throws MethodInvocationException When a method on a referenced object in the context could not invoked.
     */
    public void merge( Context context, Writer writer)
        throws ResourceNotFoundException, ParseErrorException, MethodInvocationException
    {
        merge(context, writer, null);
    }

    
    /**
     * The AST node structure is merged with the
     * context to produce the final output.
     *
     *  @param context Conext with data elements accessed by template
     *  @param writer output writer for rendered template
     *  @param macroLibraries a list of template files containing macros to be used when merging
     *  @throws ResourceNotFoundException if template not found
     *          from any available source.
     *  @throws ParseErrorException if template cannot be parsed due
     *          to syntax (or other) error.
     *  @throws MethodInvocationException When a method on a referenced object in the context could not invoked.
     *  @since 1.6
     */
    public void merge( Context context, Writer writer, List macroLibraries)
        throws ResourceNotFoundException, ParseErrorException, MethodInvocationException
    {
        /*
         *  we shouldn't have to do this, as if there is an error condition,
         *  the application code should never get a reference to the
         *  Template
         */

        if (errorCondition != null)
        {
            throw errorCondition;
        }

        if( data != null)
        {
            /*
             *  create an InternalContextAdapter to carry the user Context down
             *  into the rendering engine.  Set the template name and render()
             */

            InternalContextAdapterImpl ica = new InternalContextAdapterImpl( context );

            /**
             * Set the macro libraries
             */
            ica.setMacroLibraries(macroLibraries);

            if (macroLibraries != null)
            {
                for (int i = 0; i < macroLibraries.size(); i++)
                {
                    /**
                     * Build the macro library
                     */
                    try
                    {
                        rsvc.getTemplate((String) macroLibraries.get(i));
                    }
                    catch (ResourceNotFoundException re)
                    {
                        /*
                        * the macro lib wasn't found.  Note it and throw
                        */
                        rsvc.getLog().error("template.merge(): " +
                                "cannot find template " +
                                (String) macroLibraries.get(i));
                        throw re;
                    }
                    catch (ParseErrorException pe)
                    {
                        /*
                        * the macro lib was found, but didn't parse - syntax error
                        *  note it and throw
                        */
                        rsvc.getLog().error("template.merge(): " +
                                "syntax error in template " +
                                (String) macroLibraries.get(i) + ".");
                        throw pe;
                    }
                    
                    catch (Exception e)
                    {
                        throw new RuntimeException("Template.merge(): parse failed in template  " +
                                (String) macroLibraries.get(i) + ".", e);
                    }
                }
            }

            if (provideScope)
            {
                ica.put(scopeName, new Scope(this, ica.get(scopeName)));
            }
            try
            {
                ica.pushCurrentTemplateName( name );
                ica.setCurrentResource( this );

                ( (SimpleNode) data ).render( ica, writer);
            }
            catch (StopCommand stop)
            {
                if (!stop.isFor(this))
                {
                    throw stop;
                }
                else if (rsvc.getLog().isDebugEnabled())
                {
                    rsvc.getLog().debug(stop.getMessage());
                }
            }
            catch (IOException e)
            {
                throw new VelocityException("IO Error rendering template '"+ name + "'", e);
            }
            finally
            {
                /*
                 *  lets make sure that we always clean up the context
                 */
                ica.popCurrentTemplateName();
                ica.setCurrentResource( null );

                if (provideScope)
                {
                    Object obj = ica.get(scopeName);
                    if (obj instanceof Scope)
                    {
                        Scope scope = (Scope)obj;
                        if (scope.getParent() != null)
                        {
                            ica.put(scopeName, scope.getParent());
                        }
                        else if (scope.getReplaced() != null)
                        {
                            ica.put(scopeName, scope.getReplaced());
                        }
                        else
                        {
                            ica.remove(scopeName);
                        }
                    }
                }
            }
        }
        else
        {
            /*
             * this shouldn't happen either, but just in case.
             */

            String msg = "Template.merge() failure. The document is null, " +
                "most likely due to parsing error.";

            throw new RuntimeException(msg);

        }
    }
}
