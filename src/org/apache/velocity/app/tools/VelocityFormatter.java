package org.apache.velocity.app.tools;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.apache.velocity.context.Context;

/**
 * Formatting tool for inserting into the Velocity WebContext.  Can
 * format dates or lists of objects.
 *
 * <p>Here's an example of some uses:
 *
 * <code><pre>
 * $formatter.formatShortDate($object.Date)
 * $formatter.formatLongDate($db.getRecord(232).getDate())
 * $formatter.formatArray($array)
 * $formatter.limitLen(30, $object.Description)
 * </pre></code>
 *
 * @deprecated This class has been replaced by NumberTool, DateTool,
 * DisplayTool, and AlternatorTool available from the Velocity-Tools sub-project.
 * VelocityFormatter will be removed in a future version of Velocity.
 *
 * @author <a href="sean@somacity.com">Sean Legassick</a>
 * @author <a href="dlr@collab.net">Daniel Rall</a>
 * @version $Id: VelocityFormatter.java 544641 2007-06-05 21:30:22Z nbubna $
 */
public class VelocityFormatter
{
    Context context = null;

    /**
     * Constructor needs a backpointer to the context.
     *
     * @param context A Context.
     */
    public VelocityFormatter(Context context)
    {
        this.context = context;
    }

    /**
     * Formats a date in <code>DateFormat.SHORT</code> style.
     *
     * @param date The date to format.
     * @return The formatted date as text.
     */
    public String formatShortDate(Date date)
    {
        return DateFormat.getDateInstance(DateFormat.SHORT).format(date);
    }

    /**
     * Formats a date in <code>DateFormat.LONG</code> style.
     *
     * @param date The date to format.
     * @return The formatted date as text.
     */
    public String formatLongDate(Date date)
    {
        return DateFormat.getDateInstance(DateFormat.LONG).format(date);
    }

    /**
     * Formats a date/time in 'short' style.
     *
     * @param date The date to format.
     * @return The formatted date as text.
     */
    public String formatShortDateTime(Date date)
    {
        return DateFormat
            .getDateTimeInstance(DateFormat.SHORT,
                                 DateFormat.SHORT).format(date);
    }

    /**
     * Formats a date/time in 'long' style.
     *
     * @param date The date to format.
     * @return The formatted date as text.
     */
    public String formatLongDateTime(Date date)
    {
        return DateFormat.getDateTimeInstance(
                DateFormat.LONG, DateFormat.LONG).format(date);
    }

    /**
     * Formats an array into the form "A, B and C".
     *
     * @param array An Object.
     * @return A String.
     */
    public String formatArray(Object array)
    {
        return formatArray(array, ", ", " and ");
    }

    /**
     * Formats an array into the form
     * "A&lt;delim&gt;B&lt;delim&gt;C".
     *
     * @param array An Object.
     * @param delim A String.
     * @return A String.
     */
    public String formatArray(Object array,
                              String delim)
    {
        return formatArray(array, delim, delim);
    }

    /**
     * Formats an array into the form
     * "A&lt;delim&gt;B&lt;finaldelim&gt;C".
     *
     * @param array An Object.
     * @param delim A String.
     * @param finaldelim A String.
     * @return A String.
     */
    public String formatArray(Object array,
                              String delim,
                              String finaldelim)
    {
        StringBuffer sb = new StringBuffer();
        int arrayLen = Array.getLength(array);
        for (int i = 0; i < arrayLen; i++)
        {
            // Use the Array.get method as this will automatically
            // wrap primitive types in a suitable Object-derived
            // wrapper if necessary.
            sb.append(Array.get(array, i).toString());
            if (i  < arrayLen - 2)
            {
                sb.append(delim);
            }
            else if (i < arrayLen - 1)
            {
                sb.append(finaldelim);
            }
        }
        return sb.toString();
    }

    /**
     * Formats a vector into the form "A, B and C".
     *
     * @param list The list of elements to format.
     * @return A String.
     */
    public String formatVector(List list)
    {
        return formatVector(list, ", ", " and ");
    }

    /**
     * Formats a vector into the form "A&lt;delim&gt;B&lt;delim&gt;C".
     *
     * @param list The list of elements to format.
     * @param delim A String.
     * @return A String.
     */
    public String formatVector(List list,
                               String delim)
    {
        return formatVector(list, delim, delim);
    }

    /**
     * Formats a list into the form
     * "Adelim&gt;B&lt;finaldelim&gt;C".
     *
     * @param list The list of elements to format.
     * @param delim A String.
     * @param finaldelim A String.
     * @return A String.
     */
    public String formatVector(List list,
                               String delim,
                               String finaldelim)
    {
        StringBuffer sb = new StringBuffer();
        int size = list.size();
        for (int i = 0; i < size; i++)
        {
            sb.append(list.get(i));
            if (i < size - 2)
            {
                sb.append(delim);
            }
            else if (i < size - 1)
            {
                sb.append(finaldelim);
            }
        }
        return sb.toString();
    }

    /**
     * Limits 'string' to 'maxlen' characters.  If the string gets
     * curtailed, "..." is appended to it.
     *
     * @param maxlen An int with the maximum length.
     * @param string A String.
     * @return A String.
     */
    public String limitLen(int maxlen,
                           String string)
    {
        return limitLen(maxlen, string, "...");
    }

    /**
     * Limits 'string' to 'maxlen' character.  If the string gets
     * curtailed, 'suffix' is appended to it.
     *
     * @param maxlen An int with the maximum length.
     * @param string A String.
     * @param suffix A String.
     * @return A String.
     */
    public String limitLen(int maxlen,
                           String string,
                           String suffix)
    {
        String ret = string;
        if (string.length() > maxlen)
        {
            ret = string.substring(0, maxlen - suffix.length()) + suffix;
        }
        return ret;
    }

    /**
     * Class that returns alternating values in a template.  It stores
     * a list of alternate Strings, whenever alternate() is called it
     * switches to the next in the list.  The current alternate is
     * retrieved through toString() - i.e. just by referencing the
     * object in a Velocity template.  For an example of usage see the
     * makeAlternator() method below.
     */
    public class VelocityAlternator
    {
        /**
         *
         */
        protected String[] alternates = null;
        /**
         *
         */
        protected int current = 0;

        /**
         * Constructor takes an array of Strings.
         *
         * @param alternates A String[].
         */
        public VelocityAlternator(String[] alternates)
        {
            this.alternates = alternates;
        }

        /**
         * Alternates to the next in the list.
         *
         * @return The current alternate in the sequence.
         */
        public String alternate()
        {
            current++;
            current %= alternates.length;
            return "";
        }

        /**
         * Returns the current alternate.
         *
         * @return A String.
         */
        public String toString()
        {
            return alternates[current];
        }
    }

    /**
     * As VelocityAlternator, but calls <code>alternate()</code>
     * automatically on rendering in a template.
     */
    public class VelocityAutoAlternator extends VelocityAlternator
    {
        /**
         * Constructor takes an array of Strings.
         *
         * @param alternates A String[].
         */
        public VelocityAutoAlternator(String[] alternates)
        {
            super(alternates);
        }

        /**
         * Returns the current alternate, and automatically alternates
         * to the next alternate in its sequence (trigged upon
         * rendering).
         *
         * @return The current alternate in the sequence.
         */
        public final String toString()
        {
            String s = alternates[current];
            alternate();
            return s;
        }
    }

    /**
     * Makes an alternator object that alternates between two values.
     *
     * <p>Example usage in a Velocity template:
     *
     * <code><pre>
     * &lt;table&gt;
     * $formatter.makeAlternator("rowcolor", "#c0c0c0", "#e0e0e0")
     * #foreach $item in $items
     * #begin
     * &lt;tr&gt;&lt;td bgcolor="$rowcolor"&gt;$item.Name&lt;/td&gt;&lt;/tr&gt;
     * $rowcolor.alternate()
     * #end
     * &lt;/table&gt;
     * </pre></code>
     *
     * @param name The name for the alternator int the context.
     * @param alt1 The first alternate.
     * @param alt2 The second alternate.
     * @return The newly created instance.
     */
    public String makeAlternator(String name,
                                 String alt1,
                                 String alt2)
    {
        String[] alternates = { alt1, alt2 };
        context.put(name, new VelocityAlternator(alternates));
        return "";
    }

    /**
     * Makes an alternator object that alternates between three
     * values.
     * @param name
     * @param alt1
     * @param alt2
     * @param alt3
     * @return alternated object.
     *
     * @see #makeAlternator(String name, String alt1, String alt2)
     */
    public String makeAlternator(String name,
                                 String alt1,
                                 String alt2,
                                 String alt3)
    {
        String[] alternates = { alt1, alt2, alt3 };
        context.put(name, new VelocityAlternator(alternates));
        return "";
    }

    /**
     * Makes an alternator object that alternates between four values.
     * @param name
     * @param alt1
     * @param alt2
     * @param alt3
     * @param alt4
     * @return Alternated object.
     *
     * @see #makeAlternator(String name, String alt1, String alt2)
     */
    public String makeAlternator(String name, String alt1, String alt2,
                                 String alt3, String alt4)
    {
        String[] alternates = { alt1, alt2, alt3, alt4 };
        context.put(name, new VelocityAlternator(alternates));
        return "";
    }

    /**
     * Makes an alternator object that alternates between two values
     * automatically.
     * @param name
     * @param alt1
     * @param alt2
     * @return Alternated object.
     *
     * @see #makeAlternator(String name, String alt1, String alt2)
     */
    public String makeAutoAlternator(String name, String alt1, String alt2)
    {
        String[] alternates = { alt1, alt2 };
        context.put(name, new VelocityAutoAlternator(alternates));
        return "";
    }

    /**
     * Returns a default value if the object passed is null.
     * @param o
     * @param dflt
     * @return Object or default value when object is null.
     */
    public Object isNull(Object o, Object dflt)
    {
        if ( o == null )
        {
            return dflt;
        }
        else
        {
            return o;
        }
    }
}
