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

package org.apache.velocity.tools.struts;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.MessageResources;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * <p>Abstract view tool that provides access to Struts' message resources.</p>
 *
 * @author <a href="mailto:nbubna@apache.org">Nathan Bubna</a>
 * @since VelocityTools 1.1
 * @version $Id: MessageResourcesTool.java 72114 2004-11-11 06:26:27Z nbubna $
 */
public abstract class MessageResourcesTool implements ViewTool
{

    protected static final Log LOG = LogFactory.getLog(MessageResourcesTool.class);

    protected ServletContext application;
    protected HttpServletRequest request;
    protected Locale locale;
    protected MessageResources resources;


    /**
     * Initializes this tool.
     *
     * @param obj the current ViewContext
     * @throws IllegalArgumentException if the param is not a ViewContext
     */
    public void init(Object obj)
    {
        if (!(obj instanceof ViewContext))
        {
            throw new IllegalArgumentException("Tool can only be initialized with a ViewContext");
        }

        ViewContext context = (ViewContext)obj;
        this.request = context.getRequest();
        this.application = context.getServletContext();
        this.resources = 
            StrutsUtils.getMessageResources(request, application);
        this.locale = 
            StrutsUtils.getLocale(request, request.getSession(false));
    }


    /**
     * Retrieves the specified {@link MessageResources} bundle, or the
     * application's default MessageResources if no bundle is specified.
     * @since VelocityTools 1.1
     */
    protected MessageResources getResources(String bundle)
    {
        if (bundle == null)
        {
            if (resources == null) 
            {
                LOG.error("Message resources are not available.");
            }
            return resources;
        }
        
        MessageResources res = 
            StrutsUtils.getMessageResources(request, application, bundle);
        if (res == null)
        {
            LOG.error("MessageResources bundle '" + bundle + "' is not available.");
        }
        return res;
    }

}
