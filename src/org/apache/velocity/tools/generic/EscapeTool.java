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

package org.apache.velocity.tools.generic;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Tool for working with escaping in Velocity templates.
 * It provides methods to escape outputs for Java, JavaScript, HTML, XML and SQL.
 * Also provides methods to render VTL characters that otherwise needs escaping.
 *
 * <p><pre>
 * Example uses:
 *  $java                        -> He didn't say, "Stop!"
 *  $esc.java($java)             -> He didn't say, \"Stop!\"
 *
 *  $javascript                  -> He didn't say, "Stop!"
 *  $esc.javascript($javascript) -> He didn\'t say, \"Stop!\"
 *
 *  $html                        -> "bread" & "butter"
 *  $esc.html($html)             -> &amp;quot;bread&amp;quot; &amp;amp; &amp;quot;butter&amp;quot;
 *
 *  $xml                         -> "bread" & "butter"
 *  $esc.xml($xml)               -> &amp;quot;bread&amp;quot; &amp;amp; &amp;quot;butter&amp;quot;
 *
 *  $sql                         -> McHale's Navy
 *  $esc.sql($sql)               -> McHale''s Navy
 *
 *  $esc.dollar                  -> $
 *  $esc.d                       -> $
 *
 *  $esc.hash                    -> #
 *  $esc.h                       -> #
 *
 *  $esc.backslash               -> \
 *  $esc.b                       -> \
 *
 *  $esc.quote                   -> "
 *  $esc.q                       -> "
 *
 *  $esc.singleQuote             -> '
 *  $esc.s                       -> '
 *
 *  $esc.exclamation             -> !
 *  $esc.e                       -> !
 *
 * Example toolbox.xml config (if you want to use this with VelocityView):
 * &lt;tool&gt;
 *   &lt;key&gt;esc&lt;/key&gt;
 *   &lt;scope&gt;application&lt;/scope&gt;
 *   &lt;class&gt;org.apache.velocity.tools.generic.EscapeTool&lt;/class&gt;
 * &lt;/tool&gt;
 * </pre></p>
 *
 * <p>This tool is entirely threadsafe, and has no instance members.
 * It may be used in any scope (request, session, or application).
 * </p>
 *
 * @author <a href="mailto:shinobu@ieee.org">Shinobu Kawai</a>
 * @version $Id: $
 * @since VelocityTools 1.2
 * @see StringEscapeUtils
 */
public class EscapeTool
{

    /**
     * Default constructor.
     */
    public EscapeTool()
    {
    }

    /**
     * Escapes the characters in a <code>String</code> using Java String rules.
     * <br />
     * Delegates the process to {@link StringEscapeUtils#escapeJava(String)}.
     *
     * @param string the string to escape values, may be null
     * @return String with escaped values, <code>null</code> if null string input
     *
     * @see StringEscapeUtils#escapeJava(String)
     */
    public String java(Object string)
    {
        if (string == null)
        {
            return null;
        }
        return StringEscapeUtils.escapeJava(String.valueOf(string));
    }

    /**
     * Escapes the characters in a <code>String</code> using JavaScript String rules.
     * <br />
     * Delegates the process to {@link StringEscapeUtils#escapeJavaScript(String)}.
     *
     * @param string the string to escape values, may be null
     * @return String with escaped values, <code>null</code> if null string input
     *
     * @see StringEscapeUtils#escapeJavaScript(String)
     */
    public String javascript(Object string)
    {
        if (string == null)
        {
            return null;
        }
        return StringEscapeUtils.escapeJavaScript(String.valueOf(string));
    }

    /**
     * Escapes the characters in a <code>String</code> using HTML entities.
     * <br />
     * Delegates the process to {@link StringEscapeUtils#escapeHtml(String)}.
     *
     * @param string the string to escape, may be null
     * @return a new escaped <code>String</code>, <code>null</code> if null string input
     *
     * @see StringEscapeUtils#escapeHtml(String)
     */
    public String html(Object string)
    {
        if (string == null)
        {
            return null;
        }
        return StringEscapeUtils.escapeHtml(String.valueOf(string));
    }

    /**
     * Escapes the characters in a <code>String</code> using XML entities.
     * <br />
     * Delegates the process to {@link StringEscapeUtils#escapeXml(String)}.
     *
     * @param string the string to escape, may be null
     * @return a new escaped <code>String</code>, <code>null</code> if null string input
     *
     * @see StringEscapeUtils#escapeXml(String)
     */
    public String xml(Object string)
    {
        if (string == null)
        {
            return null;
        }
        return StringEscapeUtils.escapeXml(String.valueOf(string));
    }

    /**
     * Escapes the characters in a <code>String</code> to be suitable to pass to an SQL query.
     * <br />
     * Delegates the process to {@link StringEscapeUtils#escapeSql(String)}.
     *
     * @param string the string to escape, may be null
     * @return a new String, escaped for SQL, <code>null</code> if null string input
     *
     * @see StringEscapeUtils#escapeSql(String)
     */
    public String sql(Object string)
    {
        if (string == null)
        {
            return null;
        }
        return StringEscapeUtils.escapeSql(String.valueOf(string));
    }

    /**
     * Renders a dollar sign ($).
     * @return a dollar sign ($).
     * @see #getD()
     */
    public String getDollar()
    {
        return "$";
    }

    /**
     * Renders a dollar sign ($).
     * @return a dollar sign ($).
     * @see #getDollar()
     */
    public String getD()
    {
        return this.getDollar();
    }

    /**
     * Renders a hash (#).
     * @return a hash (#).
     * @see #getH()
     */
    public String getHash()
    {
        return "#";
    }

    /**
     * Renders a hash (#).
     * @return a hash (#).
     * @see #getHash()
     */
    public String getH()
    {
        return this.getHash();
    }

    /**
     * Renders a backslash (\).
     * @return a backslash (\).
     * @see #getB()
     */
    public String getBackslash()
    {
        return "\\";
    }

    /**
     * Renders a backslash (\).
     * @return a backslash (\).
     * @see #getBackslash()
     */
    public String getB()
    {
        return this.getBackslash();
    }

    /**
     * Renders a double quotation mark (").
     * @return a double quotation mark (").
     * @see #getQ()
     */
    public String getQuote()
    {
        return "\"";
    }

    /**
     * Renders a double quotation mark (").
     * @return a double quotation mark (").
     * @see #getQuote()
     */
    public String getQ()
    {
        return this.getQuote();
    }

    /**
     * Renders a single quotation mark (').
     * @return a single quotation mark (').
     * @see #getS()
     */
    public String getSingleQuote()
    {
        return "'";
    }

    /**
     * Renders a single quotation mark (').
     * @return a single quotation mark (').
     * @see #getSingleQuote()
     */
    public String getS()
    {
        return this.getSingleQuote();
    }

    /**
     * Renders an exclamation mark (!).
     * @return an exclamation mark (!).
     * @see #getE()
     */
    public String getExclamation()
    {
        return "!";
    }

    /**
     * Renders an exclamation mark (!).
     * @return an exclamation mark (!).
     * @see #getExclamation()
     */
    public String getE()
    {
        return this.getExclamation();
    }

}
