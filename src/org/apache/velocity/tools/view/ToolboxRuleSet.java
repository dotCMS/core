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

package org.apache.velocity.tools.view;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.RuleSetBase;
import org.apache.velocity.tools.view.DataInfo;
import org.apache.velocity.tools.view.ViewToolInfo;
import org.xml.sax.Attributes;

/**
 * <p>The set of Digester rules required to parse a toolbox
 * configuration file (<code>toolbox.xml</code>) for the
 * XMLToolboxManager class.</p> 
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @author <a href="mailto:henning@schmiedehausen.org">Henning P. Schmiedehausen</a>
 * @since VelocityTools 1.1
 * @version $Id: ToolboxRuleSet.java 290239 2005-09-19 18:56:10Z nbubna $
 */
public class ToolboxRuleSet extends RuleSetBase
{

    /**
     * <p>Add the set of Rule instances defined in this RuleSet to the
     * specified <code>Digester</code> instance, associating them with
     * our namespace URI (if any).  This method should only be called
     * by a Digester instance.  These rules assume that an instance of
     * <code>org.apache.velocity.tools.view.ToolboxManager</code> is pushed
     * onto the evaluation stack before parsing begins.</p>
     *
     * @param digester Digester instance to which the new Rule instances
     *        should be added.
     */
    public void addRuleInstances(Digester digester)
    {
        addToolRules(digester);
        addDataRules(digester);
    }


    /**
     * Add rules for digesting tool elements.
     */
    protected void addToolRules(Digester digester)
    {
        digester.addObjectCreate("toolbox/tool", getToolInfoClass());
        digester.addBeanPropertySetter("toolbox/tool/key", "key");
        digester.addBeanPropertySetter("toolbox/tool/class", "classname");
        digester.addRule("toolbox/tool/parameter", new ParameterRule());
        digester.addSetNext("toolbox/tool", "addTool");
    }


    /**
     * Add rules for digesting data elements.
     */
    protected void addDataRules(Digester digester)
    {
        digester.addObjectCreate("toolbox/data", getDataInfoClass());
        digester.addSetProperties("toolbox/data");
        digester.addBeanPropertySetter("toolbox/data/key", "key");
        digester.addBeanPropertySetter("toolbox/data/value", "value");
        digester.addSetNext("toolbox/data", "addData");
    }


    /**
     * Return the bean class to be created for tool elements.
     */
    protected Class getToolInfoClass()
    {
        return ViewToolInfo.class;
    }


    /**
     * Return the bean class to be created for data elements.
     */
    protected Class getDataInfoClass()
    {
        return DataInfo.class;
    }


    /****************************** Custom Rules *****************************/

    /**
     * 
     */
    protected class ParameterRule extends Rule
    {
        public void begin(String ns, String ln, Attributes attributes) 
            throws Exception
        {
            ViewToolInfo toolinfo = (ViewToolInfo)digester.peek();
            String name = attributes.getValue("name");
            String value = attributes.getValue("value");
            toolinfo.setParameter(name, value);
        }
    }

}
