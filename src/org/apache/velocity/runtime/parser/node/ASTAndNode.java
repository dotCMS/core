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

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.runtime.parser.Parser;

/**
 * Please look at the Parser.jjt file which is
 * what controls the generation of this class.
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: ASTAndNode.java 687184 2008-08-19 22:16:39Z nbubna $
 */
public class ASTAndNode extends SimpleNode
{
    /**
     * @param id
     */
    public ASTAndNode(int id)
    {
        super(id);
    }

    /**
     * @param p
     * @param id
     */
    public ASTAndNode(Parser p, int id)
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
     *  Returns the value of the expression.
     *  Since the value of the expression is simply the boolean
     *  result of evaluate(), lets return that.
     * @param context
     * @return The value of the expression.
     * @throws MethodInvocationException
     */
    public Object value(InternalContextAdapter context)
        throws MethodInvocationException
    {
        // TODO: JDK 1.4+ -> valueOf()
        // return new Boolean(evaluate(context));
        return evaluate(context) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * logical and :
     *   null && right = false
     *   left && null = false
     *   null && null = false
     * @param context
     * @return True if both sides are true.
     * @throws MethodInvocationException
     */
    public boolean evaluate( InternalContextAdapter context)
        throws MethodInvocationException
    {
        Node left = jjtGetChild(0);
        Node right = jjtGetChild(1);

        /*
         * null == false
         */
        if (left == null || right == null)
        {
            return false;
        }

        /*
         *  short circuit the test.  Don't eval the RHS if the LHS is false
         */

        if( left.evaluate( context ) )
        {
            if ( right.evaluate( context ) )
            {
                return true;
            }
        }

        return false;
    }
}

