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

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.velocity.tools.view.ToolboxRuleSet;
import org.apache.velocity.tools.view.servlet.ServletToolboxManager;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

/**
 * <p>The set of Digester rules required to parse a toolbox
 * configuration file (<code>toolbox.xml</code>) for the
 * ServletToolboxManager class.</p> 
 *
 * @since VelocityTools 1.1
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @version $Id: ServletToolboxRuleSet.java 72042 2004-04-15 17:32:06Z nbubna $
 */
public class ServletToolboxRuleSet extends ToolboxRuleSet
{

    /**
     * Overrides {@link ToolboxRuleSet} to add create-session rule.
     * 
     * <p>These rules assume that an instance of
     * <code>org.apache.velocity.tools.view.ServletToolboxManager</code> is
     * pushed onto the evaluation stack before parsing begins.</p>
     *
     * @param digester Digester instance to which the new Rule instances
     *  should be added.
     */
    public void addRuleInstances(Digester digester)
    {
        digester.addRule("toolbox/create-session", new CreateSessionRule());
        digester.addRule("toolbox/xhtml", new XhtmlRule());
        super.addRuleInstances(digester);
    }


    /**
     * Overrides {@link ToolboxRuleSet} to add rule for scope element.
     */
    protected void addToolRules(Digester digester)
    {
        super.addToolRules(digester);
        digester.addBeanPropertySetter("toolbox/tool/scope", "scope");
    }


    /**
     * Overrides {@link ToolboxRuleSet} to use ServletToolInfo class.
     */
    protected Class getToolInfoClass()
    {
        return ServletToolInfo.class;
    }


    /****************************** Custom Rules *****************************/

    /**
     * Abstract rule for configuring boolean options on the parent 
     * object/element of the matching element.
     */
    protected abstract class BooleanConfigRule extends Rule
    {
        public void body(String ns, String name, String text) throws Exception
        {
            Object parent = digester.peek();
            if ("yes".equalsIgnoreCase(text))
            {
                setBoolean(parent, Boolean.TRUE);
            }
            else
            {
                setBoolean(parent, Boolean.valueOf(text));
            }
        }

        /**
         * Takes the parent object and boolean value in order to
         * call the appropriate method on the parent for the
         * implementing rule.
         *
         * @param parent the parent object/element in the digester's stack
         * @param value the boolean value contained in the current element
         */
        public abstract void setBoolean(Object parent, Boolean value) 
            throws Exception;
    }


    /**
     * Rule that sets <code>setCreateSession()</code> for the top object
     * on the stack, which must be a
     * <code>org.apache.velocity.tools.ServletToolboxManager</code>.
     */
    protected final class CreateSessionRule extends BooleanConfigRule
    {
        public void setBoolean(Object obj, Boolean b) throws Exception
        {
            ((ServletToolboxManager)obj).setCreateSession(b.booleanValue());
        }
    }


    /**
     * Rule that sets <code>setXhtml()</code> for the top object
     * on the stack, which must be a
     * <code>org.apache.velocity.tools.ServletToolboxManager</code>.
     */
    protected final class XhtmlRule extends BooleanConfigRule
    {
        public void setBoolean(Object obj, Boolean b) throws Exception
        {
            ((ServletToolboxManager)obj).setXhtml(b);
        }
    }

}
