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

import com.dotmarketing.util.Logger;

/**
 *  Handles <code>arg1  == arg2</code>
 *
 *  This operator requires that the LHS and RHS are both of the
 *  same Class OR both are subclasses of java.lang.Number
 *
 *  @author <a href="mailto:wglass@forio.com">Will Glass-Husain</a>
 *  @author <a href="mailto:pero@antaramusic.de">Peter Romianowski</a>
 *  @version $Id: ASTEQNode.java 691048 2008-09-01 20:26:11Z nbubna $
 */
public class ASTEQNode extends SimpleNode
{
    /**
     * @param id
     */
    public ASTEQNode(int id)
    {
        super(id);
    }

    /**
     * @param p
     * @param id
     */
    public ASTEQNode(Parser p, int id)
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
     *   Calculates the value of the logical expression
     *
     *     arg1 == arg2
     *
     *   All class types are supported.   Uses equals() to
     *   determine equivalence.  This should work as we represent
     *   with the types we already support, and anything else that
     *   implements equals() to mean more than identical references.
     *
     *
     *  @param context  internal context used to evaluate the LHS and RHS
     *  @return true if equivalent, false if not equivalent,
     *          false if not compatible arguments, or false
     *          if either LHS or RHS is null
     * @throws MethodInvocationException
     */
    public boolean evaluate(InternalContextAdapter context)
        throws MethodInvocationException
    {
        Object left = jjtGetChild(0).value(context);
        Object right = jjtGetChild(1).value(context);

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
           return MathUtils.compare( (Number)left, (Number)right) == 0;
       }

        /**
         * if both are not null, then assume that if one class
         * is a subclass of the other that we should use the equals operator
         */
        if (left != null && right != null &&
            (left.getClass().isAssignableFrom(right.getClass()) ||
             right.getClass().isAssignableFrom(left.getClass())))
        {
            return left.equals( right );
        }

        /*
         * Ok, time to compare string values
         */
        left = (left == null) ? null : left.toString();
        right = (right == null) ? null: right.toString();

        if (left == null && right == null)
        {
            if (Logger.isDebugEnabled(this.getClass()))
            {
                Logger.debug(this,"Both right (" + getLiteral(false) + " and left "
                          + getLiteral(true) + " sides of '==' operation returned null."
                          + "If references, they may not be in the context."
                          + getLocation(context));
            }
            return true;
        }
        else if (left == null || right == null)
        {
            if (Logger.isDebugEnabled(this.getClass()))
            {
                Logger.debug(this,(left == null ? "Left" : "Right")
                        + " side (" + getLiteral(left == null)
                        + ") of '==' operation has null value. If it is a "
                        + "reference, it may not be in the context or its "
                        + "toString() returned null. " + getLocation(context));

            }
            return false;
        }
        else
        {
            return left.equals(right);
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
        return evaluate(context) ? Boolean.TRUE : Boolean.FALSE;
    }
}
