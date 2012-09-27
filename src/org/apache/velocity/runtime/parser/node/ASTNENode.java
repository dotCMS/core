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
import org.apache.velocity.util.TemplateNumber;

/**
 *  Handles <code>arg1  != arg2</code>
 *
 *  This operator requires that the LHS and RHS are both of the
 *  same Class OR both are subclasses of java.lang.Number
 *
 *  @author <a href="mailto:wglass@forio.com">Will Glass-Husain</a>
 *  @author <a href="mailto:pero@antaramusic.de">Peter Romianowski</a>
 */
public class ASTNENode extends SimpleNode
{
    /**
     * @param id
     */
    public ASTNENode(int id)
    {
        super(id);
    }

    /**
     * @param p
     * @param id
     */
    public ASTNENode(Parser p, int id)
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
     * @see org.apache.velocity.runtime.parser.node.SimpleNode#evaluate(org.apache.velocity.context.InternalContextAdapter)
     */
    public boolean evaluate(  InternalContextAdapter context)
        throws MethodInvocationException
    {
        Object left = jjtGetChild(0).value( context );
        Object right = jjtGetChild(1).value( context );

        /*
         *  convert to Number if applicable
         */
        if (left instanceof TemplateNumber)
        {
           left = ( (TemplateNumber) left).getAsNumber();
        }
        if (right instanceof TemplateNumber)
        {
           right = ( (TemplateNumber) right).getAsNumber();
        }

       /*
        * If comparing Numbers we do not care about the Class.
        */
       if (left instanceof Number && right instanceof Number)
       {
            return MathUtils.compare ( (Number)left,(Number)right) != 0;
       }

        /**
         * if both are not null, then assume that if one class
         * is a subclass of the other that we should use the equals operator
         */
        if (left != null && right != null &&
            (left.getClass().isAssignableFrom(right.getClass()) ||
             right.getClass().isAssignableFrom(left.getClass())))
        {
            return !left.equals( right );
        }

        /*
         * Ok, time to compare string values
         */
        left = (left == null) ? null : left.toString();
        right = (right == null) ? null: right.toString();

        if (left == null && right == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Both right (" + getLiteral(false) + " and left "
                          + getLiteral(true) + " sides of '!=' operation returned null."
                          + "If references, they may not be in the context."
                          + getLocation(context));
            }
            return false;
        }
        else if (left == null || right == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug((left == null ? "Left" : "Right")
                        + " side (" + getLiteral(left == null)
                        + ") of '!=' operation has null value. If it is a "
                        + "reference, it may not be in the context or its "
                        + "toString() returned null. " + getLocation(context));

            }
            return true;
        }
        else
        {
            return !left.equals(right);
        }
    }

    private String getLiteral(boolean left)
    {
        return jjtGetChild(left ? 0 : 1).literal();
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.SimpleNode#value(org.apache.velocity.context.InternalContextAdapter)
     */
    public Object value(InternalContextAdapter context)
        throws MethodInvocationException
    {
        boolean val = evaluate(context);

        return val ? Boolean.TRUE : Boolean.FALSE;
    }

}
