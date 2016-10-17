/*
 * Copyright 2003-2005 The Apache Software Foundation.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import javax.servlet.ServletRequest;
import org.apache.velocity.tools.generic.ValueParser;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * <p>Utility class for easy parsing of {@link ServletRequest} parameters.</p>
 * <p><pre>
 * Template example(s):
 *   $params.foo                ->  bar
 *   $params.getNumber('baz')   ->  12.6
 *   $params.getInt('baz')      ->  12
 *   $params.getNumbers('foo')  ->  [12.6]
 *
 * Toolbox configuration:
 * &lt;tool&gt;
 *   &lt;key&gt;params&lt;/key&gt;
 *   &lt;scope&gt;request&lt;/scope&gt;
 *   &lt;class&gt;org.apache.velocity.tools.view.tools.ParameterParser&lt;/class&gt;
 * &lt;/tool&gt;
 * </pre></p>
 *
 * <p>When used as a view tool, this should only be used in the request scope.
 * This class is, however, quite useful in your application's controller, filter,
 * or action code as well as in templates.</p>
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @version $Revision: 326942 $ $Date: 2005-10-20 09:47:19 -0700 (Thu, 20 Oct 2005) $
 */
public class ParameterParser extends ValueParser implements ViewTool
{
    private ServletRequest request;

    /**
     * Constructs a new instance
     */
    public ParameterParser()
    {}

    /**
     * Constructs a new instance using the specified request.
     *
     * @param request the {@link ServletRequest} to be parsed
     */
    public ParameterParser(ServletRequest request)
    {
        setRequest(request);
    }

    /**
     * Initializes this instance.
     *
     * @param obj the current ViewContext or ServletRequest
     * @throws IllegalArgumentException if the param is not a 
     *         ViewContext or ServletRequest
     */
    public void init(Object obj)
    {
        if (obj instanceof ViewContext)
        {
            setRequest(((ViewContext)obj).getRequest());
        }
        else if (obj instanceof ServletRequest)
        {
            setRequest((ServletRequest)obj);
        }
        else
        {
            throw new IllegalArgumentException("Was expecting " + ViewContext.class +
                                               " or " + ServletRequest.class);
        }
    }

    /**
     * Sets the current {@link ServletRequest}
     *
     * @param request the {@link ServletRequest} to be parsed
     */
    protected void setRequest(ServletRequest request)
    {
        this.request = request;
    }

    /**
     * Returns the current {@link ServletRequest} for this instance.
     *
     * @return the current {@link ServletRequest}
     * @throws UnsupportedOperationException if the request is null
     */
    protected ServletRequest getRequest()
    {
        if (request == null)
        {
            throw new UnsupportedOperationException("Request is null. ParameterParser must be initialized first!");
        }
        return request;
    }

    /**
     * Overrides ValueParser.getString(String key) to retrieve the
     * String from the ServletRequest instead of an arbitrary Map.
     *
     * @param key the parameter's key
     * @return parameter matching the specified key or
     *         <code>null</code> if there is no matching
     *         parameter
     */
    public String getString(String key)
    {
        return getRequest().getParameter(key);
    }


    /**
     * Overrides ValueParser.getString(String key) to retrieve
     * Strings from the ServletRequest instead of an arbitrary Map.
     *
     * @param key the key for the desired parameter
     * @return an array of String objects containing all of the values
     *         the given request parameter has, or <code>null</code>
     *         if the parameter does not exist
     */
    public String[] getStrings(String key)
    {
        return getRequest().getParameterValues(key);
    }

    /**
     * Overrides ValueParser.setSource(Map source) to throw an
     * UnsupportedOperationException, because this class uses
     * a servlet request as its source, not a Map.
     */
    protected void setSource(Map source)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Overrides ValueParser.getSource() to return the result
     * of getRequest().getParameterMap() if Servlet 2.3 or above
     * is being used.  Otherwise, this throws an
     * UnsupportedOperationException, because the class uses a
     * servlet request as its source, not a Map.
     */
    protected Map getSource()
    {
        try
        {
            // use reflection so we can compile against Servlet 2.2
            Method getmap = ServletRequest.class.getMethod("getParameterMap", null);
            return (Map)getmap.invoke(getRequest(), null);
        }
        catch (NoSuchMethodException nsme)
        {
            throw new UnsupportedOperationException("This method is only supported with Servlet 2.3 and higher.");
        }
        catch (IllegalAccessException iae)
        {
            throw new UnsupportedOperationException("ServletRequest.getParameterMap() is restricted - " + iae);
        }
        catch (InvocationTargetException ite)
        {
            throw new UnsupportedOperationException("ServletRequest.getParameterMap() threw an exception - " + ite);
        }
    }

}
