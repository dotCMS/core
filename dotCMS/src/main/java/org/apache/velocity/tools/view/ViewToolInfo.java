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

package org.apache.velocity.tools.view;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.tools.view.tools.Configurable;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * ToolInfo implementation for view tools. New instances
 * are returned for every call to getInstance(obj), and tools
 * that implement {@link ViewTool} are initialized with the
 * given object before being returned.
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @author <a href="mailto:henning@schmiedehausen.org">Henning P. Schmiedehausen</a>
 * @version $Id: ViewToolInfo.java 321226 2005-10-14 21:54:13Z nbubna $
 */
public class ViewToolInfo implements ToolInfo
{
    protected static final Log LOG = LogFactory.getLog(ViewToolInfo.class);

    private String key;
    private Class clazz;
    private Map parameters;
    private boolean initializable = false;
    private boolean configurable = false;

    public ViewToolInfo() {}


    //TODO: if classloading becomes needed elsewhere, move this to a utils class
    /**
     * Return the <code>Class</code> object for the specified fully qualified
     * class name, from this web application's class loader.  If no 
     * class loader is set for the current thread, then the class loader
     * that loaded this class will be used.
     *
     * @param name Fully qualified class name to be loaded
     * @return Class object
     * @exception ClassNotFoundException if the class cannot be found
     * @since VelocityTools 1.1
     */
    protected Class getApplicationClass(String name) throws ClassNotFoundException
    {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null)
        {
            loader = ViewToolInfo.class.getClassLoader();
        }
        return loader.loadClass(name);
    }


    /***********************  Mutators *************************/

    public void setKey(String key)
    { 
        this.key = key;
    }

    /**
     * If an instance of the tool cannot be created from
     * the classname passed to this method, it will throw an exception.
     *
     * @param classname the fully qualified java.lang.Class name of the tool
     */
    public void setClassname(String classname) throws Exception
    {
        if (classname != null && classname.length() != 0)
        {
            this.clazz = getApplicationClass(classname);
            /* create an instance and see if it is a ViewTool or Configurable */
            Object instance = clazz.newInstance();
            if (instance instanceof ViewTool)
            {
                this.initializable = true;
            }
            if (instance instanceof Configurable)
            {
                this.configurable = true;
            }
        }
        else
        {
            this.clazz = null;
        }
    }

    /**
     * Set parameter map for this tool.
     *
     * @since VelocityTools 1.1
     */
    public void setParameters(Map parameters)
    {
        this.parameters = parameters;
    }

    /**
     * Set/add new parameter for this tool.
     *
     * @since VelocityTools 1.1
     */
    public void setParameter(String name, String value)
    {
        if (parameters == null)
        {
            parameters = new HashMap();
        }
        parameters.put(name, value);
    }


    /***********************  Accessors *************************/

    public String getKey()
    {
        return key;
    }


    public String getClassname()
    {
        return clazz != null ? clazz.getName() : null;
    }

    /**
     * Get parameters for this tool.
     * @since VelocityTools 1.1
     */
    public Map getParameters()
    {
        return parameters;
    }

    /**
     * Returns a new instance of the tool. If the tool
     * implements {@link ViewTool}, the new instance
     * will be initialized using the given data.
     */
    public Object getInstance(Object initData)
    {
        if (clazz == null)
        {
            LOG.error("Tool "+this.key+" has no Class definition!");
            return null;
        }

        Object tool = null;
        try
        {
            tool = clazz.newInstance();
        }
        /* we shouldn't get exceptions here because we already 
         * got an instance of this class during setClassname().
         * but to be safe, let's catch the declared ones and give 
         * notice of them, and let other exceptions slip by. */
        catch (IllegalAccessException e)
        {
            LOG.error("Exception while instantiating instance of \"" +
                    getClassname() + "\": " + e);
        }
        catch (InstantiationException e)
        {
            LOG.error("Exception while instantiating instance of \"" +
                    getClassname() + "\": " + e);
        }
        if (configurable && parameters != null)
        {
            ((Configurable)tool).configure(parameters);
        }
        if (initializable)
        {
            ((ViewTool)tool).init(initData);
        }
        return tool;
    }
}
