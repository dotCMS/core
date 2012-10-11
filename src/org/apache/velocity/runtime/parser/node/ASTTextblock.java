package org.apache.velocity.runtime.parser.node;

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

import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.parser.Parser;
import org.apache.velocity.runtime.parser.Token;

/**
 * This node holds the "Textblock" data which should not be interpreted by Velocity.
 *
 * Textblocks are marked in Velocity with #[[content here]]# notation. Velocity
 * will output everything between the markers and does not attempt to parse it in any way.
 */
public class ASTTextblock extends SimpleNode
{
    public static final String START = "#[[";
    public static final String END = "]]#";
    private char[] ctext;

    /**
     * @param id
     */
    public ASTTextblock(int id)
    {
        super(id);
    }

    /**
     * @param p
     * @param id
     */
    public ASTTextblock(Parser p, int id)
    {
        super(p, id);
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.SimpleNode#jjtAccept(org.apache.velocity.runtime.parser.node.ParserVisitor, java.lang.Object)
     */
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.SimpleNode#init(org.apache.velocity.context.InternalContextAdapter, java.lang.Object)
     */
    public Object init( InternalContextAdapter context, Object data)
    throws TemplateInitException
    {
        Token t = getFirstToken();
        
        String text = t.image;
        
        // t.image is in format: #% <string> %#
        // we must strip away the hash tags
        text = text.substring(START.length(), text.length() - END.length());

        ctext = text.toCharArray();
        return data;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.SimpleNode#render(org.apache.velocity.context.InternalContextAdapter, java.io.Writer)
     */
    public boolean render( InternalContextAdapter context, Writer writer)
        throws IOException
    {
        writer.write(ctext);
        return true;
    }
}
