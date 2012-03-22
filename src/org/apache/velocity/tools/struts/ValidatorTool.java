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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.validator.Field;
import org.apache.commons.validator.Form;
import org.apache.commons.validator.ValidatorAction;
import org.apache.commons.validator.ValidatorResources;
import org.apache.commons.validator.util.ValidatorUtils;
import org.apache.commons.validator.Var;

import org.apache.struts.Globals;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.ModuleUtils;
import org.apache.struts.validator.Resources;
import org.apache.struts.validator.ValidatorPlugIn;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
import java.util.StringTokenizer;

/**
 * <p>View tool that works with Struts Validator to
 *    produce client side javascript validation for your forms.</p>
 * <p>Usage:
 * <pre>
 * Template example:
 *
 * $validator.getJavascript("nameOfYourForm")
 *
 * Toolbox configuration:
 * &lt;tool&gt;
 *   &lt;key&gt;validator&lt;/key&gt;
 *   &lt;scope&gt;request&lt;/scope&gt;
 *   &lt;class&gt;org.apache.velocity.tools.struts.ValidatorTool&lt;/class&gt;
 * &lt;/tool&gt;
 * </pre>
 * </p>
 * <p>This is an adaptation of the JavascriptValidatorTag
 * from the Struts 1.1 validator library.</p>
 *
 * @author David Winterfeldt
 * @author David Graham
 * @author <a href="mailto:marinoj@centrum.is">Marino A. Jonsson</a>
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @since VelocityTools 1.1
 * @version $Revision: 326310 $ $Date: 2005-10-18 17:47:39 -0700 (Tue, 18 Oct 2005) $
 */
public class ValidatorTool implements ViewTool {

    /** A reference to the ViewContext */
    protected ViewContext context;

    /** A reference to the ServletContext */
    protected ServletContext app;

    /** A reference to the HttpServletRequest. */
    protected HttpServletRequest request;

    /** A reference to the HttpSession. */
    protected HttpSession session;

    /** A reference to the ValidatorResources. */
    protected ValidatorResources resources;


    private static final String HTML_BEGIN_COMMENT = "\n<!-- Begin \n";
    private static final String HTML_END_COMMENT = "//End --> \n";

    private boolean xhtml = false;

    private boolean htmlComment = true;
    private boolean cdata = true;
    private String formName = null;
    private String methodName = null;
    private String src = null;
    private int page = 0;
    /**
     * formName is used for both Javascript and non-javascript validations.
     * For the javascript validations, there is the possibility that we will
     * be rewriting the formName (if it is a ValidatorActionForm instead of just
     * a ValidatorForm) so we need another variable to hold the formName just for
     * javascript usage.
     */
    protected String jsFormName = null;




    /**
     * A Comparator to use when sorting ValidatorAction objects.
     */
    private static final Comparator actionComparator = new Comparator() {
        public int compare(Object o1, Object o2) {

            ValidatorAction va1 = (ValidatorAction) o1;
            ValidatorAction va2 = (ValidatorAction) o2;

            if ((va1.getDepends() == null || va1.getDepends().length() == 0)
                && (va2.getDepends() == null || va2.getDepends().length() == 0)) {
                return 0;

            } else if (
                (va1.getDepends() != null && va1.getDepends().length() > 0)
                    && (va2.getDepends() == null || va2.getDepends().length() == 0)) {
                return 1;

            } else if (
                (va1.getDepends() == null || va1.getDepends().length() == 0)
                    && (va2.getDepends() != null && va2.getDepends().length() > 0)) {
                return -1;

            } else {
                return va1.getDependencyList().size() - va2.getDependencyList().size();
            }
        }
    };


    /**
     * Default constructor. Tool must be initialized before use.
     */
    public ValidatorTool() {}


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
            throw new IllegalArgumentException(
                    "Tool can only be initialized with a ViewContext");
        }

        this.context = (ViewContext)obj;
        this.request = context.getRequest();
        this.session = request.getSession(false);
        this.app = context.getServletContext();

        Boolean b = (Boolean)context.getAttribute(ViewContext.XHTML);
        if (b != null)
        {
            this.xhtml = b.booleanValue();
        }

        /* Is there a mapping associated with this request? */
        ActionConfig config =
                (ActionConfig)request.getAttribute(Globals.MAPPING_KEY);
        if (config != null)
        {
            /* Is there a form bean associated with this mapping? */
            this.formName = config.getAttribute();
        }

        ModuleConfig mconfig = ModuleUtils.getInstance().getModuleConfig(request, app);
        this.resources = (ValidatorResources)app.getAttribute(ValidatorPlugIn.
                VALIDATOR_KEY +
                mconfig.getPrefix());

    }

    /****************** get/set accessors ***************/

    /**
     * Gets the current page number of a multi-part form.
     * Only field validations with a matching page number
     * will be generated that match the current page number.
     * Only valid when the formName attribute is set.
     *
     * @return the current page number of a multi-part form
     */
    public int getPage()
    {
        return page;
    }

    /**
     * Sets the current page number of a multi-part form.
     * Only field validations with a matching page number
     * will be generated that match the current page number.
     *
     * @param page the current page number of a multi-part form
     */
    public void setPage(int page)
    {
        this.page = page;
    }

    /**
     * Gets the method name that will be used for the Javascript
     * validation method name if it has a value.  This overrides
     * the auto-generated method name based on the key (form name)
     * passed in.
     *
     * @return the method name that will be used for the Javascript validation method
     */
    public String getMethod()
    {
        return methodName;
    }

    /**
     * Sets the method name that will be used for the Javascript
     * validation method name if it has a value.  This overrides
     * the auto-generated method name based on the key (form name)
     * passed in.
     *
     * @param methodName the method name that will be used for the Javascript validation method name
     */
    public void setMethod(String methodName)
    {
        this.methodName = methodName;
    }

    /**
     * Gets whether or not to delimit the
     * JavaScript with html comments.  If this is set to 'true', which
     * is the default, html comments will surround the JavaScript.
     *
     * @return true if the JavaScript should be delimited with html comments
     */
    public boolean getHtmlComment()
    {
        return this.htmlComment;
    }

    /**
     * Sets whether or not to delimit the
     * JavaScript with html comments.  If this is set to 'true', which
     * is the default, html comments will surround the JavaScript.
     *
     * @param htmlComment whether or not to delimit the JavaScript with html comments
     */
    public void setHtmlComment(boolean htmlComment)
    {
        this.htmlComment = htmlComment;
    }

    /**
     * Gets the src attribute's value when defining
     * the html script element.
     *
     * @return the src attribute's value
     */
    public String getSrc()
    {
        return src;
    }

    /**
     * Sets the src attribute's value (used to include
     * an external script resource) when defining
     * the html script element. The src attribute is only recognized
     * when the formName attribute is specified.
     *
     * @param src the src attribute's value
     */
    public void setSrc(String src)
    {
        this.src = src;
    }

    /**
     * Returns the cdata setting "true" or "false".
     *
     * @return boolean - "true" if JavaScript will be hidden in a CDATA section
     */
    public boolean getCdata()
    {
        return cdata;
    }

    /**
     * Sets the cdata status.
     * @param cdata The cdata to set
     */
    public void setCdata(boolean cdata)
    {
        this.cdata = cdata;
    }


    /****************** methods that aren't just accessors ***************/

    /**
     * Render both dynamic and static JavaScript to perform
     * validations based on the form name attribute of the action
     * mapping associated with the current request (if such exists).
     *
     * @return the javascript for the current form
     * @throws Exception
     */
    public String getJavascript() throws Exception
    {
        return getJavascript(this.formName);
    }

    /**
     * Render both dynamic and static JavaScript to perform
     * validations based on the supplied form name.
     *
     * @param formName the key (form name)
     * @return the Javascript for the specified form
     * @throws Exception
     */
    public String getJavascript(String formName) throws Exception
    {
        this.formName = formName;
        return getJavascript(formName, true);
    }

    /**
     * Render just the dynamic JavaScript to perform validations based
     * on the form name attribute of the action mapping associated
     * with the current request (if such exists). Useful i.e. if the static
     * parts are located in a seperate .js file.
     *
     * @return the javascript for the current form
     * @throws Exception
     */
    public String getDynamicJavascript() throws Exception
    {
        return getDynamicJavascript(this.formName);
    }


    /**
     * Render just the static JavaScript methods. Useful i.e. if the static
     * parts should be located in a seperate .js file.
     *
     * @return all static Javascript methods
     * @throws Exception
     */
    public String getStaticJavascript() throws Exception
    {
        StringBuffer results = new StringBuffer();

        results.append(getStartElement());
        if (this.htmlComment)
        {
            results.append(HTML_BEGIN_COMMENT);
        }
        results.append(getJavascriptStaticMethods(resources));
        results.append(getJavascriptEnd());

        return results.toString();
    }


    /**
     * Render just the dynamic JavaScript to perform validations based
     * on the supplied form name. Useful i.e. if the static
     * parts are located in a seperate .js file.
     *
     * @param formName the key (form name)
     * @return the dynamic Javascript for the specified form
     * @throws Exception
     */
    public String getDynamicJavascript(String formName) throws Exception
    {
        this.formName = formName;
        return getJavascript(formName, false);
    }

    /**
     * Render both dynamic and static JavaScript to perform
     * validations based on the supplied form name.
     *
     * @param formName the key (form name)
     * @param getStatic indicates if the static methods should be rendered
     * @return the Javascript for the specified form
     * @throws Exception
     */
    protected String getJavascript(String formName, boolean getStatic) throws Exception
    {
        StringBuffer results = new StringBuffer();

        Locale locale = StrutsUtils.getLocale(request, session);

        Form form = resources.getForm(locale, formName);
        if (form != null)
        {
            results.append(getDynamicJavascript(resources, locale, form));
        }

        if(getStatic)
        {
            results.append(getJavascriptStaticMethods(resources));
        }

        if (form != null)
        {
            results.append(getJavascriptEnd());
        }

        return results.toString();
    }



    /**
     * Generates the dynamic JavaScript for the form.
     *
     * @param resources the validator resources
     * @param locale the locale for the current request
     * @param form the form to generate javascript for
     * @return the dynamic javascript
     */
    protected String getDynamicJavascript(ValidatorResources resources,
                                          Locale locale,
                                          Form form)
    {
        StringBuffer results = new StringBuffer();

        MessageResources messages =
            StrutsUtils.getMessageResources(request, app);

        List actions = createActionList(resources, form);

        final String methods = createMethods(actions);

        String formName = form.getName();

        jsFormName = formName;
        if(jsFormName.charAt(0) == '/') {
            String mappingName = StrutsUtils.getActionMappingName(jsFormName);
            ModuleConfig mconfig = ModuleUtils.getInstance().getModuleConfig(request, app);

            ActionConfig mapping = (ActionConfig) mconfig.findActionConfig(mappingName);
            if (mapping == null) {
                throw new NullPointerException("Cannot retrieve mapping for action " + mappingName);
            }
            jsFormName = mapping.getAttribute();
        }

        results.append(getJavascriptBegin(methods));

        for (Iterator i = actions.iterator(); i.hasNext();)
        {
            ValidatorAction va = (ValidatorAction)i.next();
            int jscriptVar = 0;
            String functionName = null;

            if (va.getJsFunctionName() != null && va.getJsFunctionName().length() > 0)
            {
                functionName = va.getJsFunctionName();
            }
            else
            {
                functionName = va.getName();
            }

            results.append("    function ");
            results.append(jsFormName);
            results.append("_");
            results.append(functionName);
            results.append(" () { \n");

            for (Iterator x = form.getFields().iterator(); x.hasNext();)
            {
                Field field = (Field)x.next();

                // Skip indexed fields for now until there is
                // a good way to handle error messages (and the length
                // of the list (could retrieve from scope?))
                if (field.isIndexed()
                    || field.getPage() != page
                    || !field.isDependency(va.getName()))
                {
                    continue;
                }

                String message =
                    Resources.getMessage(messages, locale, va, field);

                message = (message != null) ? message : "";

                //jscriptVar = this.getNextVar(jscriptVar);

                results.append("     this.a");
                results.append(jscriptVar++);
                results.append(" = new Array(\"");
                results.append(field.getKey()); // TODO: escape?
                results.append("\", \"");
                results.append(escapeJavascript(message));
                results.append("\", ");
                results.append("new Function (\"varName\", \"");

                Map vars = field.getVars();
                // Loop through the field's variables.
                Iterator varsIterator = vars.keySet().iterator();
                while (varsIterator.hasNext())
                {
                    String varName = (String)varsIterator.next(); // TODO: escape?
                    Var var = (Var)vars.get(varName);
                    String varValue = var.getValue();
                    String jsType = var.getJsType();

                    // skip requiredif variables field, fieldIndexed, fieldTest, fieldValue
                    if (varName.startsWith("field"))
                    {
                        continue;
                    }

                    // these are appended no matter what jsType is
                    results.append("this.");
                    results.append(varName);

                    String escapedVarValue = escapeJavascript(varValue);

                    if (Var.JSTYPE_INT.equalsIgnoreCase(jsType))
                    {
                        results.append("=");
                        results.append(escapedVarValue);
                        results.append("; ");
                    }
                    else if (Var.JSTYPE_REGEXP.equalsIgnoreCase(jsType))
                    {
                        results.append("=/");
                        results.append(escapedVarValue);
                        results.append("/; ");
                    }
                    else if (Var.JSTYPE_STRING.equalsIgnoreCase(jsType))
                    {
                        results.append("='");
                        results.append(escapedVarValue);
                        results.append("'; ");
                    }
                    // So everyone using the latest format
                    // doesn't need to change their xml files immediately.
                    else if ("mask".equalsIgnoreCase(varName))
                    {
                        results.append("=/");
                        results.append(escapedVarValue);
                        results.append("/; ");
                    }
                    else
                    {
                        results.append("='");
                        results.append(escapedVarValue);
                        results.append("'; ");
                    }
                }
                results.append(" return this[varName];\"));\n");
            }
            results.append("    } \n\n");
        }
        return results.toString();
    }


    /**
     * <p>Backslash-escapes the following characters from the input string:
     * &quot;, &apos;, \, \r, \n.</p>
     *
     * <p>This method escapes characters that will result in an invalid
     * Javascript statement within the validator Javascript.</p>
     *
     * @param str The string to escape.
     * @return The string <code>s</code> with each instance of a double quote,
     *         single quote, backslash, carriage-return, or line feed escaped
     *         with a leading backslash.
     * @since VelocityTools 1.2
     */
    protected String escapeJavascript(String str)
    {
        if (str == null)
        {
            return null;
        }
        int length = str.length();
        if (length == 0)
        {
            return str;
        }

        // guess at how many chars we'll be adding...
        StringBuffer out = new StringBuffer(length + 4);
        // run through the string escaping sensitive chars
        for (int i=0; i < length; i++)
        {
            char c = str.charAt(i);
            if (c == '"'  ||
                c == '\'' ||
                c == '\\' || 
                c == '\n' || 
                c == '\r')
            {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }


    /**
     * Creates the JavaScript methods list from the given actions.
     * @param actions A List of ValidatorAction objects.
     * @return JavaScript methods.
     */
    protected String createMethods(List actions)
    {
        String methodOperator = " && ";

        StringBuffer methods = null;
        for (Iterator i = actions.iterator(); i.hasNext();)
        {
            ValidatorAction va = (ValidatorAction)i.next();
            if (methods == null)
            {
                methods = new StringBuffer(va.getMethod());
            }
            else
            {
                methods.append(methodOperator);
                methods.append(va.getMethod());
            }
            methods.append("(form)");
        }
        return methods.toString();
    }


    /**
     * Get List of actions for the given Form.
     *
     * @param resources the validator resources
     * @param form the form for which the actions are requested
     * @return A sorted List of ValidatorAction objects.
     */
    protected List createActionList(ValidatorResources resources, Form form)
    {
        List actionMethods = new ArrayList();
        // Get List of actions for this Form
        for (Iterator i = form.getFields().iterator(); i.hasNext();)
        {
            Field field = (Field)i.next();
            for (Iterator x = field.getDependencyList().iterator(); x.hasNext();)
            {
                Object o = x.next();
                if (o != null && !actionMethods.contains(o))
                {
                    actionMethods.add(o);
                }
            }
        }

        List actions = new ArrayList();

        // Create list of ValidatorActions based on actionMethods
        for (Iterator i = actionMethods.iterator(); i.hasNext();)
        {
            String depends = (String) i.next();
            ValidatorAction va = resources.getValidatorAction(depends);

            // throw nicer NPE for easier debugging
            if (va == null)
            {
                throw new NullPointerException(
                    "Depends string \"" + depends +
                    "\" was not found in validator-rules.xml.");
            }

            String javascript = va.getJavascript();
            if (javascript != null && javascript.length() > 0)
            {
                actions.add(va);
            }
            else
            {
                i.remove();
            }
        }

        Collections.sort(actions, actionComparator);
        return actions;
    }


    /**
     * Returns the opening script element and some initial javascript.
     *
     * @param methods javascript validation methods
     * @return  the opening script element and some initial javascript
     */
    protected String getJavascriptBegin(String methods)
    {
        StringBuffer sb = new StringBuffer();
        String name = jsFormName.replace('/', '_'); // remove any '/' characters
        name = jsFormName.substring(0, 1).toUpperCase() +
                      jsFormName.substring(1, jsFormName.length());

        sb.append(this.getStartElement());

        if (this.xhtml && this.cdata)
        {
            sb.append("<![CDATA[\r\n");
        }

        if (!this.xhtml && this.htmlComment)
        {
            sb.append(HTML_BEGIN_COMMENT);
        }
        sb.append("\n     var bCancel = false; \n\n");

        if (methodName == null || methodName.length() == 0)
        {
            sb.append("    function validate");
            sb.append(name);
        }
        else
        {
            sb.append("    function ");
            sb.append(methodName);
        }
        sb.append("(form) {");
        //FIXME? anyone know why all these spaces need to be here?
        sb.append("                                                                   \n");
        sb.append("        if (bCancel) \n");
        sb.append("      return true; \n");
        sb.append("        else \n");

        // Always return true if there aren't any Javascript validation methods
        if (methods == null || methods.length() == 0)
        {
            sb.append("       return true; \n");
        }
        else
        {
            //Making Sure that Bitwise operator works:
            sb.append(" var formValidationResult;\n");
            sb.append("       formValidationResult = " + methods + "; \n");
            sb.append("     return (formValidationResult == 1);\n");

        }
        sb.append("   } \n\n");

        return sb.toString();
    }

    /**
     *
     * @param resources the validation resources
     * @return the static javascript methods
     */
    protected String getJavascriptStaticMethods(ValidatorResources resources)
    {
        StringBuffer sb = new StringBuffer("\n\n");

        Iterator actions = resources.getValidatorActions().values().iterator();
        while (actions.hasNext())
        {
            ValidatorAction va = (ValidatorAction) actions.next();
            if (va != null)
            {
                String javascript = va.getJavascript();
                if (javascript != null && javascript.length() > 0)
                {
                    sb.append(javascript + "\n");
                }
            }
        }
        return sb.toString();
    }


    /**
     * Returns the closing script element.
     *
     * @return the closing script element
     */
    protected String getJavascriptEnd()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("\n");

        if (!this.xhtml && this.htmlComment)
        {
            sb.append(HTML_END_COMMENT);
        }

        if (this.xhtml && this.cdata)
        {
            sb.append("]]>\r\n");
        }
        sb.append("</script>\n\n");

        return sb.toString();
    }


    /**
     * Constructs the beginning <script> element depending on xhtml status.
     *
     * @return the beginning <script> element depending on xhtml status
     */
    private String getStartElement()
    {
        StringBuffer start = new StringBuffer("<script type=\"text/javascript\"");

        // there is no language attribute in xhtml
        if (!this.xhtml)
        {
            start.append(" language=\"Javascript1.1\"");
        }

        if (this.src != null)
        {
            start.append(" src=\"" + src + "\"");
        }

        start.append("> \n");
        return start.toString();
    }

}
