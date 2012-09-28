package org.apache.velocity.app.event.implement;

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

import org.apache.velocity.app.event.IncludeEventHandler;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.util.ContextAware;
import org.apache.velocity.util.RuntimeServicesAware;
import org.apache.velocity.util.StringUtils;

import com.dotmarketing.util.Logger;

/**
 * Simple event handler that checks to see if an included page is available.
 * If not, it includes a designated replacement page instead.
 *
 * <P>By default, the name of the replacement page is "notfound.vm", however this
 * page name can be changed by setting the Velocity property
 * <code>eventhandler.include.notfound</code>, for example:
 * <code>
 * <PRE>
 * eventhandler.include.notfound = error.vm
 * </PRE>
 * </code>
 * </p><p>
 * The name of the missing resource is put into the Velocity context, under the 
 * key "missingResource", so that the "notfound" template can report the missing 
 * resource with a Velocity reference, like: 
 * <code>$missingResource</code>
 * </p>
 *
 * @author <a href="mailto:wglass@forio.com">Will Glass-Husain</a>
 * @version $Id: IncludeNotFound.java 809816 2009-09-01 05:14:27Z nbubna $
 * @since 1.5
 */
public class IncludeNotFound implements IncludeEventHandler,RuntimeServicesAware,ContextAware {

    private static final String DEFAULT_NOT_FOUND = "notfound.vm";
    private static final String PROPERTY_NOT_FOUND = "eventhandler.include.notfound";
    private RuntimeServices rs = null;
    String notfound;
    Context context;

    /**
     * Chseck to see if included file exists, and display "not found" page if it
     * doesn't. If "not found" page does not exist, log an error and return
     * null.
     * 
     * @param includeResourcePath
     * @param currentResourcePath
     * @param directiveName
     * @return message.
     */
    public String includeEvent(
        String includeResourcePath,
        String currentResourcePath,
        String directiveName)
    {

        /**
         * check to see if page exists
         */
        boolean exists = (rs.getLoaderNameForResource(includeResourcePath) != null);
        if (!exists)
        {
            context.put("missingResource", includeResourcePath);
            if (rs.getLoaderNameForResource(notfound) != null)
            {
                return notfound;
            }
            else
            {
                /**
                 * can't find not found, so display nothing
                 */
                Logger.error(this,"Can't find include not found page: " + notfound);
                return null;
            }
        }
        else
            return includeResourcePath;
    }


    /**
     * @see org.apache.velocity.util.RuntimeServicesAware#setRuntimeServices(org.apache.velocity.runtime.RuntimeServices)
     */
    public void setRuntimeServices(RuntimeServices rs)
    {
         this.rs = rs;
         notfound = StringUtils.nullTrim(rs.getString(PROPERTY_NOT_FOUND, DEFAULT_NOT_FOUND));
    }

    /** 
     * @see org.apache.velocity.util.ContextAware#setContext(org.apache.velocity.context.Context)
     */ 
    public void setContext(Context context)
    {
        this.context = context;
    }
}
