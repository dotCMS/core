package org.apache.velocity.app.event.implement;

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

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Escapes the characters in a String to be suitable for use in JavaScript.
 * @see <a href="http://jakarta.apache.org/commons/lang/api/org/apache/commons/lang/StringEscapeUtils.html#escapeJavaScript(java.lang.String)">StringEscapeUtils</a>
 * @author wglass
 * @since 1.5
 */
public class EscapeJavaScriptReference extends EscapeReference
{

    /**
     * Escapes the characters in a String to be suitable for use in JavaScript.
     * 
     * @param text
     * @return An escaped String.
     * @see <a href="http://jakarta.apache.org/commons/lang/api/org/apache/commons/lang/StringEscapeUtils.html#escapeJavaScript(java.lang.String)">StringEscapeUtils</a>
     */
    protected String escape(Object text)
    {
        return StringEscapeUtils.escapeJavaScript(text.toString());
    }

    /**
     * @return attribute "eventhandler.escape.javascript.match"
     */
    protected String getMatchAttribute()
    {
        return "eventhandler.escape.javascript.match";
    }

}
