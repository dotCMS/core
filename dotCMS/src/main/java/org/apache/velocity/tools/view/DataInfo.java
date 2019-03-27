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


/**
 * <p>ToolInfo implementation to handle "primitive" data types.
 * It currently supports String, Number, and Boolean data.</p>
 *
 * <p>An example of data elements specified in your toolbox.xml
 * might be:
 * <pre>
 *  &lt;data type="string"&gt;
 *    &lt;key&gt;app_name&lt;/key&gt;
 *    &lt;value&gt;FooWeb Deluxe&lt;/value&gt;
 *  &lt;/data&gt;
 *  &lt;data type="number"&gt;
 *    &lt;key&gt;app_version&lt;/key&gt;
 *    &lt;value&gt;4.2&lt;/value&gt;
 *  &lt;/data&gt;
 *  &lt;data type="boolean"&gt;
 *    &lt;key&gt;debug&lt;/key&gt;
 *    &lt;value&gt;true&lt;/value&gt;
 *  &lt;/data&gt;
 *  &lt;data type="number"&gt;
 *    &lt;key&gt;screen_width&lt;/key&gt;
 *    &lt;value&gt;400&lt;/value&gt;
 *  &lt;/data&gt;
 * </pre></p>
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 *
 * @version $Id: DataInfo.java 290281 2005-09-19 21:47:01Z nbubna $
 */
public class DataInfo implements ToolInfo
{

    public static final String TYPE_STRING = "string";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_BOOLEAN = "boolean";

    private static final int TYPE_ID_STRING = 0;
    private static final int TYPE_ID_NUMBER = 1;
    private static final int TYPE_ID_BOOLEAN = 2;

    private String key = null;
    private int type_id = TYPE_ID_STRING;
    private Object data = null;


    public DataInfo() {}


    /***********************  Mutators *************************/

    public void setKey(String key)
    { 
        this.key = key;
    }


    public void setType(String type)
    { 
        if (TYPE_BOOLEAN.equalsIgnoreCase(type))
        {
            this.type_id = TYPE_ID_BOOLEAN;
        }
        else if (TYPE_NUMBER.equalsIgnoreCase(type))
        {
            this.type_id = TYPE_ID_NUMBER;
        }
        else /* if no type or type="string" */
        {
            this.type_id = TYPE_ID_STRING;
        }
    }


    public void setValue(String value)
    {
        if (type_id == TYPE_ID_BOOLEAN)
        {
            this.data = Boolean.valueOf(value);
        }
        else if (type_id == TYPE_ID_NUMBER)
        {
            if (value.indexOf('.') >= 0)
            {
                this.data = new Double(value);
            }
            else
            {
                this.data = new Integer(value);
            }
        }
        else /* type is "string" */
        {
            this.data = value;
        }
    }


    /***********************  Accessors *************************/

    public String getKey()
    {
        return key;
    }


    public String getClassname()
    {
        return data != null ? data.getClass().getName() : null;
    }


    /**
     * Returns the data. Always returns the same
     * object since the data is a constant. Initialization
     * data is ignored.
     */
    public Object getInstance(Object initData)
    {
        return data;
    }

}
