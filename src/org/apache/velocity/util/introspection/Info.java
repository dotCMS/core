package org.apache.velocity.util.introspection;

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

import java.io.Serializable;

import org.apache.velocity.runtime.log.Log;
import org.apache.velocity.runtime.parser.node.Node;

/**
 *  Little class to carry in info such as template name, line and column
 *  for information error reporting from the uberspector implementations
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: Info.java 733416 2009-01-11 05:26:52Z byron $
 */
public class Info implements Serializable
{
    private static final long serialVersionUID = -7620503479533845293L;
    private int line;
    private int column;
    private String templateName;

    /**
     * @param source Usually a template name.
     * @param line The line number from <code>source</code>.
     * @param column The column number from <code>source</code>.
     */
    public Info(String source, int line, int column)
    {
        this.templateName = source;
        this.line = line;
        this.column = column;
    }

    public Info(Node node)
    {
      this(node.getTemplateName(), node.getLine(), node.getColumn());
    }
    
    /**
     * Force callers to set the location information.
     */
    private Info()
    {
    }
    
    /**
     * @return The template name.
     */
    public String getTemplateName()
    {
        return templateName;
    }

    /**
     * @return The line number.
     */
    public int getLine()
    {
        return line;
    }

    /**
     * @return The column number.
     */
    public int getColumn()
    {
        return column;
    }

    /**
     * Formats a textual representation of this object as <code>SOURCE
     * [line X, column Y]</code>.
     *
     * @return String representing this object.
     * @since 1.5
     */
    public String toString()
    {
        return Log.formatFileString(getTemplateName(), getLine(), getColumn());
    }
}
