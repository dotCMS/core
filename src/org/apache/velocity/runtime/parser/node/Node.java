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
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.Renderable;
import org.apache.velocity.runtime.parser.Token;

/**
 *  This file describes the interface between the Velocity code
 *  and the JavaCC generated code.
 *
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id: Node.java 737539 2009-01-25 17:24:29Z nbubna $
 */

public interface Node extends Renderable
{
    /** This method is called after the node has been made the current
     * node.  It indicates that child nodes can now be added to it. */
    public void jjtOpen();

    /** This method is called after all the child nodes have been
      added.
     */
    public void jjtClose();

    /**
     * This pair of methods are used to inform the node of its
     * parent.
     * @param n
     *
     */
    public void jjtSetParent(Node n);

    /**
     * @return The node parent.
     */
    public Node jjtGetParent();

    /**
     * This method tells the node to add its argument to the node's
     * list of children.
     * @param n
     * @param i
     */
    public void jjtAddChild(Node n, int i);

    /**
     * This method returns a child node.  The children are numbered
     * from zero, left to right.
     * @param i
     * @return A child node.
     */
    public Node jjtGetChild(int i);

    /**
     * Return the number of children the node has.
     * @return The number of children of this node.
     */
    public int jjtGetNumChildren();

    /**
     * @param visitor
     * @param data
     * @return The Node execution result object.
     */
    public Object jjtAccept(ParserVisitor visitor, Object data);

    /*
     * ========================================================================
     *
     * The following methods are not generated automatically be the Parser but
     * added manually to be used by Velocity.
     *
     * ========================================================================
     */

    /**
     * @see #jjtAccept(ParserVisitor, Object)
     * @param visitor
     * @param data
     * @return The node execution result.
     */
    public Object childrenAccept(ParserVisitor visitor, Object data);

    /**
     * @return The first token.
     */
    public Token getFirstToken();
    /**
     * @return The last token.
     */
    public Token getLastToken();
    /**
     * @return The NodeType.
     */
    public int getType();

    /**
     * @param context
     * @param data
     * @return The init result.
     * @throws TemplateInitException
     */
    public Object init( InternalContextAdapter context, Object data) throws TemplateInitException;

    /**
     * @param context
     * @return The evaluation result.
     * @throws MethodInvocationException
     */
    public boolean evaluate( InternalContextAdapter context)
        throws MethodInvocationException;

    /**
     * @param context
     * @return The node value.
     * @throws MethodInvocationException
     */
    public Object value( InternalContextAdapter context)
        throws MethodInvocationException;

    /**
     * @param context
     * @param writer
     * @return True if the node rendered successfully.
     * @throws IOException
     * @throws MethodInvocationException
     * @throws ParseErrorException
     * @throws ResourceNotFoundException
     */
    public boolean render( InternalContextAdapter context, Writer writer)
        throws IOException,MethodInvocationException, ParseErrorException, ResourceNotFoundException;

    /**
     * @param o
     * @param context
     * @return The execution result.
     * @throws MethodInvocationException
     */
    public Object execute(Object o, InternalContextAdapter context)
      throws MethodInvocationException;

    /**
     * @param info
     */
    public void setInfo(int info);

    /**
     * @return The current node info.
     */
    public int getInfo();

    /**
     * @return A literal.
     */
    public String literal();

    /**
     * Mark the node as invalid.
     */
    public void setInvalid();

    /**
     * @return True if the node is invalid.
     */
    public boolean isInvalid();

    /**
     * @return The current line position.
     */
    public int getLine();

    /**
     * @return The current column position.
     */
    public int getColumn();
    
    /**
     * @return the file name of the template
     */
    public String getTemplateName();
}
