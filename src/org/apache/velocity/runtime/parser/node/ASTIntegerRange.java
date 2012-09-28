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

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.runtime.log.Log;
import org.apache.velocity.runtime.parser.Parser;

import com.dotmarketing.util.Logger;

/**
 * handles the range 'operator'  [ n .. m ]
 *
 * Please look at the Parser.jjt file which is
 * what controls the generation of this class.
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 */
public class ASTIntegerRange extends SimpleNode
{
    /**
     * @param id
     */
    public ASTIntegerRange(int id)
    {
        super(id);
    }

    /**
     * @param p
     * @param id
     */
    public ASTIntegerRange(Parser p, int id)
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
     *  does the real work.  Creates an Vector of Integers with the
     *  right value range
     *
     *  @param context  app context used if Left or Right of .. is a ref
     *  @return Object array of Integers
     * @throws MethodInvocationException
     */
    public Object value( InternalContextAdapter context)
        throws MethodInvocationException
    {
        /*
         *  get the two range ends
         */

        Object left = jjtGetChild(0).value( context );
        Object right = jjtGetChild(1).value( context );

        /*
         *  if either is null, lets log and bail
         */

        if (left == null || right == null)
        {
            Logger.error(this,(left == null ? "Left" : "Right")
                           + " side of range operator [n..m] has null value."
                           + " Operation not possible. "
                           + Log.formatFileString(this));
            return null;
        }

        /*
         *  if not a Number, not much we can do either
         */

        if ( !( left instanceof Number )  || !( right instanceof Number ))
        {
            Logger.error(this,(!(left instanceof Number) ? "Left" : "Right")
                           + " side of range operator is not a valid type. "
                           + "Currently only integers (1,2,3...) and the Number type are supported. "
                           + Log.formatFileString(this));
            return null;
        }


        /*
         *  get the two integer values of the ends of the range
         */

        int l = ((Number) left).intValue() ;
        int r = ((Number) right).intValue();

        /*
         *  find out how many there are
         */

        int nbrElements = Math.abs( l - r );
        nbrElements += 1;

        /*
         *  Determine whether the increment is positive or negative.
         */

        int delta = ( l >= r ) ? -1 : 1;

        /*
         * Fill the range with the appropriate values.
         */

        List elements = new ArrayList(nbrElements);
        int value = l;

        for (int i = 0; i < nbrElements; i++)
        {
            // TODO: JDK 1.4+ -> valueOf()
            elements.add(new Integer(value));
            value += delta;
        }

        return elements;
    }
}
