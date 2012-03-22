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

package org.apache.velocity.tools.view.tools;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.tools.view.ImportSupport;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * General-purpose text-importing view tool for templates.
 * <p>Usage:<br />
 * Just call $import.read("http://www.foo.com/bleh.jsp?sneh=bar") to insert the contents of the named
 * resource into the template.
 * </p>
 * <p><pre>
 * Toolbox configuration:
 * &lt;tool&gt;
 *   &lt;key&gt;import&lt;/key&gt;
 *   &lt;scope&gt;request&lt;/scope&gt;
 *   &lt;class&gt;org.apache.velocity.tools.view.tools.ImportTool&lt;/class&gt;
 * &lt;/tool&gt;
 * </pre></p>
 *
 * @author <a href="mailto:marinoj@centrum.is">Marino A. Jonsson</a>
 * @since VelocityTools 1.1
 * @version $Revision: 72114 $ $Date: 2004-11-10 22:26:27 -0800 (Wed, 10 Nov 2004) $
 */
public class ImportTool extends ImportSupport implements ViewTool {

    protected static final Log LOG = LogFactory.getLog(ImportTool.class);

    /**
     * Default constructor. Tool must be initialized before use.
     */
    public ImportTool() {}

    /**
     * Initializes this tool.
     *
     * @param obj the current ViewContext
     * @throws IllegalArgumentException if the param is not a ViewContext
     */
    public void init(Object obj) {
        if (! (obj instanceof ViewContext)) {
            throw new IllegalArgumentException("Tool can only be initialized with a ViewContext");
        }

        ViewContext context = (ViewContext) obj;
        this.request = context.getRequest();
        this.response = context.getResponse();
        this.application = context.getServletContext();
    }

    /**
     * Returns the supplied URL rendered as a String.
     *
     * @param url the URL to import
     * @return the URL as a string
     */
    public String read(String url) {
        try {
            // check the URL
            if (url == null || url.equals("")) {
                LOG.warn("Import URL is null or empty");
                return null;
            }

            return acquireString(url);
        }
        catch (Exception ex) {
            LOG.error("Exception while importing URL: " + ex.getMessage());
            return null;
        }
    }

}
