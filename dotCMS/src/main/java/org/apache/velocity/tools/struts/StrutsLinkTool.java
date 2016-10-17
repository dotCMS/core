/*
 * Copyright 2003 The Apache Software Foundation.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.tools.view.tools.LinkTool;
import org.apache.velocity.tools.struts.StrutsUtils;

/**
 * <p>View tool to work with URI links in Struts.</p> 
 * <p><pre>
 * Template example(s):
 *   &lt;a href="$link.setAction('update')"&gt;update something&lt;/a&gt;
 *   #set( $base = $link.setForward('MyPage.vm').setAnchor('view') )
 *   &lt;a href="$base.addQueryData('select','this')"&gt;view this&lt;/a&gt;
 *   &lt;a href="$base.addQueryData('select','that')"&gt;view that&lt;/a&gt;
 *
 * Toolbox configuration:
 * &lt;tool&gt;
 *   &lt;key&gt;link&lt;/key&gt;
 *   &lt;scope&gt;request&lt;/scope&gt;
 *   &lt;class&gt;org.apache.velocity.tools.struts.StrutsLinkTool&lt;/class&gt;
 * &lt;/tool&gt;
 * </pre></p>
 *
 * <p>This tool should only be used in the request scope.</p>
 *
 * @author <a href="mailto:sidler@teamup.com">Gabe Sidler</a>
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 *
 * @version $Id: StrutsLinkTool.java 72114 2004-11-11 06:26:27Z nbubna $
 */
public class StrutsLinkTool extends LinkTool
{

    protected static final Log LOG = LogFactory.getLog(StrutsLinkTool.class);


    /**
     * <p>Returns a copy of the link with the given action name
     * converted into a server-relative URI reference. This method 
     * does not check if the specified action really is defined. 
     * This method will overwrite any previous URI reference settings 
     * but will copy the query string.</p>
     *
     * @param action an action path as defined in struts-config.xml
     *
     * @return a new instance of StrutsLinkTool
     */
    public StrutsLinkTool setAction(String action)
    {
        return (StrutsLinkTool)copyWith(
            StrutsUtils.getActionMappingURL(application, request, action));
    }
    
    
    /**
     * <p>Returns a copy of the link with the given global forward name
     * converted into a server-relative URI reference. If the parameter 
     * does not map to an existing global forward name, <code>null</code> 
     * is returned. This method will overwrite any previous URI reference 
     * settings but will copy the query string.</p>
     *
     * @param forward a global forward name as defined in struts-config.xml
     *
     * @return a new instance of StrutsLinkTool
     */
    public StrutsLinkTool setForward(String forward)
    {
        String url = StrutsUtils.getForwardURL(request, application, forward);
        if (url == null)
        {
            LOG.warn("In method setForward(" + forward +
                     "): Parameter does not map to a valid forward.");
            return null;
        }
        return (StrutsLinkTool)copyWith(url);
    }


}
