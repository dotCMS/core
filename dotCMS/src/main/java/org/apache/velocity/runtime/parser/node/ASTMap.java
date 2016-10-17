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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.runtime.parser.Parser;

/**
 * AST Node for creating a map / dictionary.
 *
 * This class was originally generated from Parset.jjt.
 *
 * @version $Id: ASTMap.java 928475 2010-03-28 18:54:55Z nbubna $
 * @since 1.5
 */
public class ASTMap extends SimpleNode
{
    /**
     * @param id
     */
    public ASTMap(int id)
    {
        super(id);
    }

    /**
     * @param p
     * @param id
     */
    public ASTMap(Parser p, int id)
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
     * @see org.apache.velocity.runtime.parser.node.SimpleNode#value(org.apache.velocity.context.InternalContextAdapter)
     */
    public Object value(InternalContextAdapter context)
        throws MethodInvocationException
    {
        int size = jjtGetNumChildren();

        Map objectMap = new LinkedHashMap();

        for (int i = 0; i < size; i += 2)
        {
            SimpleNode keyNode = (SimpleNode) jjtGetChild(i);
            SimpleNode valueNode = (SimpleNode) jjtGetChild(i+1);

            Object key = (keyNode == null ? null : keyNode.value(context));
            Object value = (valueNode == null ? null : valueNode.value(context));

            objectMap.put(key, value);
        }

        return objectMap;
    }
}
