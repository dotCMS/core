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


package org.apache.velocity.tools.view.servlet;


import org.apache.velocity.tools.view.ViewToolInfo;
import org.apache.velocity.tools.view.context.ViewContext;


/**
 * <p>ToolInfo implementation that holds scope information for tools
 * used in a servlet environment.  The ServletToolboxManager uses
 * this to allow tool definitions to specify the scope/lifecycle
 * of individual view tools.</p>
 *
 * <p>Example of toolbox.xml definitions for servlet tools:<pre>
 *  &lt;tool&gt;
 *    &lt;key&gt;link&lt;/key&gt;
 *    &lt;scope&gt;request&lt;/scope&gt;
 *    &lt;class&gt;org.apache.velocity.tools.struts.StrutsLinkTool&lt;/class&gt;
 *  &lt;/tool&gt;
 *  &lt;tool&gt;
 *    &lt;key&gt;math&lt;/key&gt;
 *    &lt;scope&gt;application&lt;/scope&gt;
 *    &lt;class&gt;org.apache.velocity.tools.generic.MathTool&lt;/class&gt;
 *  &lt;/tool&gt;
 *  &lt;tool&gt;
 *    &lt;key&gt;user&lt;/key&gt;
 *    &lt;scope&gt;session&lt;/scope&gt;
 *    &lt;class&gt;com.mycompany.tools.MyUserTool&lt;/class&gt;
 *  &lt;/tool&gt;
 *  </pre></p>
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 *
 * @version $Id: ServletToolInfo.java 72030 2004-03-12 20:50:38Z nbubna $
 */
public class ServletToolInfo extends ViewToolInfo
{
        
    private String scope;


    public ServletToolInfo() {}


    public void setScope(String scope) { 
        this.scope = scope;
    }


    /**
     * @return the scope of the tool
     */
    public String getScope()
    {
        return scope;
    }

}
