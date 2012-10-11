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

import java.math.BigInteger;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.parser.Parser;

/**
 * Handles integer numbers.  The value will be either an Integer, a Long, or a BigInteger.
 *
 * @author <a href="mailto:wglass@forio.com">Will Glass-Husain</a>
 * @since 1.5
 */
public class ASTIntegerLiteral extends SimpleNode
{

    // This may be of type Integer, Long or BigInteger
    private Number value = null;

    /**
     * @param id
     */
    public ASTIntegerLiteral(int id)
    {
        super(id);
    }

    /**
     * @param p
     * @param id
     */
    public ASTIntegerLiteral(Parser p, int id)
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
        /*
         *  init the tree correctly
         */

        super.init( context, data );

        /**
         * Determine the size of the item and make it an Integer, Long, or BigInteger as appropriate.
         */
         String str = getFirstToken().image;
         try
         {
             value = new Integer( str );
         }
         catch ( NumberFormatException E1 )
         {
            try
            {

                value = new Long( str );

            }
            catch ( NumberFormatException E2 )
            {

                // if there's still an Exception it will propogate out
                value = new BigInteger( str );
            }
        }

        return data;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.SimpleNode#value(org.apache.velocity.context.InternalContextAdapter)
     */
    public Object value( InternalContextAdapter context)
    {
        return value;
    }

}
