/*
 * Copyright 2003-2004 The Apache Software Foundation.
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


package org.apache.velocity.tools.struts;

import org.apache.struts.action.ActionErrors;

/**
 * <p>View tool to work with the Struts error messages.</p>
 * <p><pre>
 * Template example(s):
 *   #if( $errors.exist() )
 *     &lt;div class="errors"&gt;
 *     #foreach( $e in $errors.all )
 *       $e &lt;br&gt;
 *     #end
 *     &lt;/div&gt;
 *   #end
 *
 * Toolbox configuration:
 * &lt;tool&gt;
 *   &lt;key&gt;errors&lt;/key&gt;
 *   &lt;scope&gt;request&lt;/scope&gt;
 *   &lt;class&gt;org.apache.velocity.tools.struts.ErrorsTool&lt;/class&gt;
 * &lt;/tool&gt;
 * </pre></p>
 *
 * <p>This tool should only be used in the request scope.</p>
 * <p>Since VelocityTools 1.1, ErrorsTool extends ActionMessagesTool.</p>
 *
 * @author <a href="mailto:sidler@teamup.com">Gabe Sidler</a>
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @since VelocityTools 1.0
 * @version $Id: ErrorsTool.java,v 1.12.2.1 2004/03/12 23:36:19 nbubna Exp $
 */
public class ErrorsTool extends ActionMessagesTool
{
    
    /**
     * Initializes this tool.
     *
     * @param obj the current ViewContext
     * @throws IllegalArgumentException if the param is not a ViewContext
     */
    public void init(Object obj)
    {
        //setup superclass instance members
        super.init(obj);

        this.actionMsgs = StrutsUtils.getErrors(this.request);
    }


    /**
     * <p>Renders the queued error messages as a list. This method expects
     * the message keys <code>errors.header</code> and <code>errors.footer</code>
     * in the message resources. The value of the former is rendered before
     * the list of error messages and the value of the latter is rendered
     * after the error messages.</p>
     * 
     * @return The formatted error messages. If no error messages are queued, 
     * an empty string is returned.
     */
    public String getMsgs()
    {
        return getMsgs(null, null);    
    }


    /**
     * <p>Renders the queued error messages of a particual category as a list. 
     * This method expects the message keys <code>errors.header</code> and 
     * <code>errors.footer</code> in the message resources. The value of the 
     * former is rendered before the list of error messages and the value of 
     * the latter is rendered after the error messages.</p>
     * 
     * @param property the category of errors to render
     * 
     * @return The formatted error messages. If no error messages are queued, 
     * an empty string is returned. 
     */
    public String getMsgs(String property)
    {
        return getMsgs(property, null);
    }


    /**
     * <p>Renders the queued error messages of a particual category as a list. 
     * This method expects the message keys <code>errors.header</code> and 
     * <code>errors.footer</code> in the message resources. The value of the 
     * former is rendered before the list of error messages and the value of 
     * the latter is rendered after the error messages.</p>
     * 
     * @param property the category of errors to render
     * @param bundle the message resource bundle to use
     * 
     * @return The formatted error messages. If no error messages are queued, 
     * an empty string is returned. 
     * @since VelocityTools 1.1
     */
    public String getMsgs(String property, String bundle)
    {
        return StrutsUtils.errorMarkup(property, bundle, request, 
                                       request.getSession(false), application);
    }


    /**
     * Overrides {@link ActionMessagesTool#getGlobalName()}
     * to return the "global" key for action errors.
     *
     * @see org.apache.struts.action.ActionErrors.GLOBAL_ERROR
     * @deprecated This will be removed after VelocityTools 1.1.
     */
    public String getGlobalName()
    {
        return ActionErrors.GLOBAL_ERROR;
    }

}
