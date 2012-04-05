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


package org.apache.velocity.tools.view;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.tools.view.context.ToolboxContext;
import org.osgi.framework.BundleContext;

import com.dotmarketing.osgi.HostActivator;


/**
 * A ToolboxManager for loading a toolbox from xml.
 *
 * <p>A toolbox manager is responsible for automatically filling the Velocity
 * context with a set of view tools. This class provides the following 
 * features:</p>
 * <ul>
 *   <li>configurable through an XML-based configuration file</li>   
 *   <li>assembles a set of view tools (the toolbox) on request</li>
 *   <li>supports any class with a public constructor without parameters 
 *     to be used as a view tool</li>
 *   <li>supports adding primitive data values to the context(String,Number,Boolean)</li>
 * </ul>
 * 
 *
 * <p><strong>Configuration</strong></p>
 * <p>The toolbox manager is configured through an XML-based configuration
 * file. The configuration file is passed to the {@link #load(java.io.InputStream input)}
 * method. The format is shown in the following example:</p>
 * <pre> 
 * &lt;?xml version="1.0"?&gt;
 * &lt;toolbox&gt;
 *   &lt;tool&gt;
 *      &lt;key&gt;date&lt;/key&gt;
 *      &lt;class&gt;org.apache.velocity.tools.generic.DateTool&lt;/class&gt;
 *   &lt;/tool&gt;
 *   &lt;data type="Number"&gt;
 *      &lt;key&gt;luckynumber&lt;/key&gt;
 *      &lt;value&gt;1.37&lt;/value&gt;
 *   &lt;/data&gt;
 *   &lt;data type="String"&gt;
 *      &lt;key&gt;greeting&lt;/key&gt;
 *      &lt;value&gt;Hello World!&lt;/value&gt;
 *   &lt;/data&gt;
 * &lt;/toolbox&gt;    
 * </pre>
 *
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @author <a href="mailto:henning@schmiedehausen.org">Henning P. Schmiedehausen</a>
 * @version $Id: XMLToolboxManager.java 290236 2005-09-19 18:53:41Z nbubna $
 */
public class XMLToolboxManager implements ToolboxManager
{
    protected static final Log LOG = LogFactory.getLog(XMLToolboxManager.class);

    private List toolinfo;
    private Map data;

    private static RuleSet ruleSet = new ToolboxRuleSet();


    /**
     * Default constructor
     */
    public XMLToolboxManager()
    {
        toolinfo = new ArrayList();
        data = new HashMap();
    }

    // ------------------------------- ToolboxManager interface ------------
    
    protected boolean compare(ToolInfo info1, ToolInfo info2) {
    	if ( info1 == null )
    		return info2 == null;
    	if ( info2 == null )
    		return info1 == null;
    	if ( info1.getKey() != null )
    		if ( !info1.getKey().equals(info2.getKey()))
    			return false;
    	if (info1.getClassname() != null )
    		return info1.getClassname().equals(info2.getClassname());
    	return false;
    }

    public void removeTool(ToolInfo info) 
    {
    	ToolInfo toRemove = null;
        Iterator i = toolinfo.iterator();
        while(i.hasNext())
        {
            ToolInfo info_ = (ToolInfo)i.next();
        	if ( compare(info, info_) ) {
        		toRemove = info_;
        	}
        }
        if ( toRemove != null )
        	toolinfo.remove(toRemove);
    }

    public void addTool(ToolInfo info)
    {
    	System.out.println("Adding tool: " + info.getKey() + " - " + info.getClassname());
        if (validateToolInfo(info))
        {
            toolinfo.add(info);
            LOG.info("Added "+info.getKey()+" ("+info.getClassname()+") to the toolbox.");
        }
    }

    public void addData(ToolInfo info)
    {
        if (validateToolInfo(info))
        {
            data.put(info.getKey(), info.getInstance(null));
        }
    }

    /**
     * Checks whether an object described by a ToolInfo passes
     * some basic sanity checks.
     *
     * @param info A ToolInfo object
     *
     * @return true if the ToolInfo is valid
     */
    protected boolean validateToolInfo(ToolInfo info)
    {
        if (info == null)
        {
            return false;
        }
        if (info.getKey() == null || info.getKey().length() == 0)
        {
            LOG.error("Tool has no key defined!");
            return false;
        }
        if (info.getClassname() == null)
        {
            LOG.error("Tool " + info.getKey() + " has no Class definition!");
            return false;
        }
        return true;
    }

    /**
     * @deprecated Use getToolbox(Object) instead.
     */
    public ToolboxContext getToolboxContext(Object initData)
    {
        return new ToolboxContext(getToolbox(initData));
    }


    public Map getToolbox(Object initData)
    {
        Map toolbox = new HashMap(data);
        Iterator i = toolinfo.iterator();
        while(i.hasNext())
        {
            ToolInfo info = (ToolInfo)i.next();
            toolbox.put(info.getKey(), info.getInstance(initData));
        }
        return toolbox;
    }


    // ------------------------------- toolbox loading methods ------------

    /**
     * <p>Reads an XML document from an {@link InputStream}
     * and sets up the toolbox from that.</p>
     * 
     * @param input the InputStream to read from
     */
    public void load(InputStream input) throws Exception
    {
        LOG.trace("Loading toolbox...");

        Digester digester = new Digester();
        digester.setValidating(false);
        digester.setUseContextClassLoader(true);
        digester.push(this);
        digester.addRuleSet(getRuleSet());
        digester.parse(input);

        LOG.trace("Toolbox loaded.");
    }


    /**
     * <p>Retrieves the rule set Digester should use to parse and load
     * the toolbox for this manager.</p>
     *
     * <p>The DTD corresponding to the default ToolboxRuleSet is:
     * <pre>
     *  &lt;?xml version="1.0"?&gt;
     *  &lt;!ELEMENT toolbox (tool*,data*,#PCDATA)&gt;
     *  &lt;!ELEMENT tool    (key,class,parameter*,#PCDATA)&gt;
     *  &lt;!ELEMENT data    (key,value)&gt;
     *      &lt;!ATTLIST data type (string|number|boolean) "string"&gt;
     *  &lt;!ELEMENT key     (#CDATA)&gt;
     *  &lt;!ELEMENT class   (#CDATA)&gt;
     *  &lt;!ELEMENT parameter (EMPTY)&gt;
     *      &lt;!ATTLIST parameter name CDATA #REQUIRED&gt;
     *      &lt;!ATTLIST parameter value CDATA #REQUIRED&gt;
     *  &lt;!ELEMENT value   (#CDATA)&gt;
     * </pre></p>
     *
     * @since VelocityTools 1.1
     */
    protected RuleSet getRuleSet()
    {
        return ruleSet;
    }

}
