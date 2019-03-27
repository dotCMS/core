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

/**
 * Please look at the Parser.jjt file which is
 * what controls the generation of this class.
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: ASTIfStatement.java 517553 2007-03-13 06:09:58Z wglass $
*/


import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.parser.Parser;


/**
 *
 */
public class ASTIfStatement extends SimpleNode
{
    /**
     * @param id
     */
    public ASTIfStatement(int id)
    {
        super(id);
    }

    /**
     * @param p
     * @param id
     */
    public ASTIfStatement(Parser p, int id)
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
     * @see org.apache.velocity.runtime.parser.node.SimpleNode#render(org.apache.velocity.context.InternalContextAdapter, java.io.Writer)
     */
    public boolean render( InternalContextAdapter context, Writer writer)
        throws IOException,MethodInvocationException,
        	ResourceNotFoundException, ParseErrorException
    {
        /*
         * Check if the #if(expression) construct evaluates to true:
         * if so render and leave immediately because there
         * is nothing left to do!
         */
        if (jjtGetChild(0).evaluate(context))
        {
            jjtGetChild(1).render(context, writer);
            return true;
        }

        int totalNodes = jjtGetNumChildren();

        /*
         * Now check the remaining nodes left in the
         * if construct. The nodes are either elseif
         *  nodes or else nodes. Each of these node
         * types knows how to evaluate themselves. If
         * a node evaluates to true then the node will
         * render itself and this method will return
         * as there is nothing left to do.
         */
        for (int i = 2; i < totalNodes; i++)
        {
            if (jjtGetChild(i).evaluate(context))
            {
                jjtGetChild(i).render(context, writer);
                return true;
            }
        }

        /*
         * This is reached when an ASTIfStatement
         * consists of an if/elseif sequence where
         * none of the nodes evaluate to true.
         */
        return true;
    }

    /**
     * @param context
     * @param visitor
     */
    public void process( InternalContextAdapter context, ParserVisitor visitor)
    {
    }
}






