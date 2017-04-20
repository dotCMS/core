package com.dotmarketing.velocity.directive;

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
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;

import org.apache.velocity.Template;
import org.apache.velocity.app.event.EventHandlerUtil;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.directive.InputBase;
import org.apache.velocity.runtime.directive.StopCommand;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class ParseContainer extends DotDirective
{
    private final String hostIndicator="//";

    private final String scopeName="template";

    
    
    
    
    @Override
    public String getName() {

      return "parseContainer";
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
      // TODO Auto-generated method stub
      return super.clone();
    }

    /**
     * Overrides the default to use "template", so that all templates
     * can use the same scope reference, whether rendered via #parse
     * or direct merge.
     */
    public String getScopeName()
    {
        return scopeName;
    }

    /**
     * Return type of this directive.
     * @return The type of this directive.
     */
    public int getType()
    {
        return LINE;
    }

    /**
     * Init's the #parse directive.
     * @param rs
     * @param context
     * @param node
     * @throws TemplateInitException
     */
    public void init(RuntimeServices rs, InternalContextAdapter context, Node node)
        throws TemplateInitException
    {
        super.init(rs, context, node);

    }

    /**
     *  iterates through the argument list and renders every
     *  argument that is appropriate.  Any non appropriate
     *  arguments are logged, but render() continues.
     * @param context
     * @param writer
     * @param node
     * @return True if the directive rendered successfully.
     * @throws IOException
     * @throws ResourceNotFoundException
     * @throws ParseErrorException
     * @throws MethodInvocationException
     */
    public boolean render( InternalContextAdapter context,
                           Writer writer, Node node)
        throws IOException, ResourceNotFoundException, ParseErrorException,
               MethodInvocationException
    {
        /*
         *  did we get an argument?
         */
        if ( node.jjtGetNumChildren() == 0 )
        {
            throw new VelocityException("#parseContainer(): argument missing at " +
                                        VelocityException.formatFileString(this));
        }

        /*
         *  does it have a value?  If you have a null reference, then no.
         */
        Object value =  node.jjtGetChild(0).value( context );
        RuntimeServices rsvc=VelocityUtil.getEngine().getRuntimeServices();
        boolean live = isLive(context);
        /*
         *  get the path
         */
        String sourcearg = value == null ? null : value.toString();
        if (sourcearg == null)
        {
            // abort early, but still consider it a successful rendering
            return true;
        }
        /*
         *  check to see if the argument will be changed by the event cartridge
         */
        String templatePath = (live) ? "/live/" +sourcearg + ".container" :"/working/" +sourcearg + ".container";
          
        




        Template t = null;
        


        try
        {
            t = rsvc.getTemplate( templatePath, getInputEncoding(context) );
        }
        catch ( ResourceNotFoundException rnfe )
        {
            /*
             * the arg wasn't found.  Note it and throw
             */
            Logger.error(this,"#parseContainer(): cannot find template '" + templatePath +
                                "', called at " + VelocityException.formatFileString(this));
            throw rnfe;
        }
        catch ( ParseErrorException pee )
        {
            /*
             * the arg was found, but didn't parse - syntax error
             *  note it and throw
             */
            Logger.error(this,"#parseContainer(): syntax error in #parseContainer()-ed template '"
                                + templatePath + "', called at " + VelocityException.formatFileString(this));
            throw pee;
        }
        /**
         * pass through application level runtime exceptions
         */
        catch( RuntimeException e )
        {
            Logger.error(this,"Exception rendering #parseContainer(" + templatePath + ") at " +
                    VelocityException.formatFileString(this));
            throw e;
        }
        catch ( Exception e)
        {
            String msg = "Exception rendering #parseContainer(" + templatePath + ") at " +
                    VelocityException.formatFileString(this);
            Logger.error(this,msg, e);
            throw new VelocityException(msg, e);
        }



        /*
         *  and render it
         */
        try
        {
            preRender(context);
            context.pushCurrentTemplateName(templatePath);

            ((SimpleNode) t.getData()).render(context, writer);
        }
        catch( StopCommand stop )
        {
            if (!stop.isFor(this))
            {
                throw stop;
            }
        }
        /**
         * pass through application level runtime exceptions
         */
        catch( RuntimeException e )
        {
            /**
             * Log #parse errors so the user can track which file called which.
             */
            Logger.error(this,"Exception rendering #parseContainer(" + templatePath + ") at " +
                    VelocityException.formatFileString(this));
            throw e;
        }
        catch ( Exception e )
        {
            String msg = "Exception rendering #parseContainer(" + templatePath + ") at " +
                    VelocityException.formatFileString(this);
            Logger.error(this,msg, e);
            throw new VelocityException(msg, e);
        }
        finally
        {
            context.popCurrentTemplateName();
            postRender(context);
        }

        /*
         *    note - a blocked input is still a successful operation as this is
         *    expected behavior.
         */

        return true;
    }

}

