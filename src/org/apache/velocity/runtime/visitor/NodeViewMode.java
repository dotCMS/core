package org.apache.velocity.runtime.visitor;

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

import org.apache.velocity.runtime.parser.Token;
import org.apache.velocity.runtime.parser.node.ASTAddNode;
import org.apache.velocity.runtime.parser.node.ASTAndNode;
import org.apache.velocity.runtime.parser.node.ASTAssignment;
import org.apache.velocity.runtime.parser.node.ASTBlock;
import org.apache.velocity.runtime.parser.node.ASTDirective;
import org.apache.velocity.runtime.parser.node.ASTDivNode;
import org.apache.velocity.runtime.parser.node.ASTEQNode;
import org.apache.velocity.runtime.parser.node.ASTElseIfStatement;
import org.apache.velocity.runtime.parser.node.ASTElseStatement;
import org.apache.velocity.runtime.parser.node.ASTEscape;
import org.apache.velocity.runtime.parser.node.ASTEscapedDirective;
import org.apache.velocity.runtime.parser.node.ASTExpression;
import org.apache.velocity.runtime.parser.node.ASTFalse;
import org.apache.velocity.runtime.parser.node.ASTFloatingPointLiteral;
import org.apache.velocity.runtime.parser.node.ASTGENode;
import org.apache.velocity.runtime.parser.node.ASTGTNode;
import org.apache.velocity.runtime.parser.node.ASTIdentifier;
import org.apache.velocity.runtime.parser.node.ASTIfStatement;
import org.apache.velocity.runtime.parser.node.ASTIntegerLiteral;
import org.apache.velocity.runtime.parser.node.ASTIntegerRange;
import org.apache.velocity.runtime.parser.node.ASTLENode;
import org.apache.velocity.runtime.parser.node.ASTLTNode;
import org.apache.velocity.runtime.parser.node.ASTMap;
import org.apache.velocity.runtime.parser.node.ASTMethod;
import org.apache.velocity.runtime.parser.node.ASTModNode;
import org.apache.velocity.runtime.parser.node.ASTMulNode;
import org.apache.velocity.runtime.parser.node.ASTNENode;
import org.apache.velocity.runtime.parser.node.ASTNotNode;
import org.apache.velocity.runtime.parser.node.ASTObjectArray;
import org.apache.velocity.runtime.parser.node.ASTOrNode;
import org.apache.velocity.runtime.parser.node.ASTReference;
import org.apache.velocity.runtime.parser.node.ASTSetDirective;
import org.apache.velocity.runtime.parser.node.ASTStringLiteral;
import org.apache.velocity.runtime.parser.node.ASTSubtractNode;
import org.apache.velocity.runtime.parser.node.ASTText;
import org.apache.velocity.runtime.parser.node.ASTTrue;
import org.apache.velocity.runtime.parser.node.ASTWord;
import org.apache.velocity.runtime.parser.node.ASTprocess;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;

/**
 * This class is simply a visitor implementation
 * that traverses the AST, produced by the Velocity
 * parsing process, and creates a visual structure
 * of the AST. This is primarily used for
 * debugging, but it useful for documentation
 * as well.
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @version $Id: NodeViewMode.java 747106 2009-02-23 19:25:14Z nbubna $
 */
public class NodeViewMode extends BaseVisitor
{
    private int indent = 0;
    private boolean showTokens = true;

    /** Indent child nodes to help visually identify
      *  the structure of the AST.
      */
    private String indentString()
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < indent; ++i)
        {
            sb.append("  ");
        }
        return sb.toString();
    }

    /**
      * Display the type of nodes and optionally the
      * first token.
      */
    private Object showNode(Node node, Object data)
    {
        String tokens = "";
        String special = "";
        Token t;

        if (showTokens)
        {
            t = node.getFirstToken();

            if (t.specialToken != null && ! t.specialToken.image.startsWith("##"))
                special = t.specialToken.image;

            tokens = " -> " + special + t.image;
        }

        System.out.println(indentString() + node + tokens);
        ++indent;
        data = node.childrenAccept(this, data);
        --indent;
        return data;
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.SimpleNode, java.lang.Object)
     */
    public Object visit(SimpleNode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTprocess, java.lang.Object)
     */
    public Object visit(ASTprocess node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTExpression, java.lang.Object)
     */
    public Object visit(ASTExpression node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTAssignment, java.lang.Object)
     */
    public Object visit(ASTAssignment node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTOrNode, java.lang.Object)
     */
    public Object visit(ASTOrNode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTAndNode, java.lang.Object)
     */
    public Object visit(ASTAndNode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTEQNode, java.lang.Object)
     */
    public Object visit(ASTEQNode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTNENode, java.lang.Object)
     */
    public Object visit(ASTNENode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTLTNode, java.lang.Object)
     */
    public Object visit(ASTLTNode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTGTNode, java.lang.Object)
     */
    public Object visit(ASTGTNode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTLENode, java.lang.Object)
     */
    public Object visit(ASTLENode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTGENode, java.lang.Object)
     */
    public Object visit(ASTGENode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTAddNode, java.lang.Object)
     */
    public Object visit(ASTAddNode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTSubtractNode, java.lang.Object)
     */
    public Object visit(ASTSubtractNode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTMulNode, java.lang.Object)
     */
    public Object visit(ASTMulNode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTDivNode, java.lang.Object)
     */
    public Object visit(ASTDivNode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTModNode, java.lang.Object)
     */
    public Object visit(ASTModNode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTNotNode, java.lang.Object)
     */
    public Object visit(ASTNotNode node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTFloatingPointLiteral, java.lang.Object)
     */
    public Object visit(ASTFloatingPointLiteral node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTIntegerLiteral, java.lang.Object)
     * @since 1.5
     */
    public Object visit(ASTIntegerLiteral node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTStringLiteral, java.lang.Object)
     */
    public Object visit(ASTStringLiteral node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTIdentifier, java.lang.Object)
     */
    public Object visit(ASTIdentifier node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTMethod, java.lang.Object)
     */
    public Object visit(ASTMethod node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTReference, java.lang.Object)
     */
    public Object visit(ASTReference node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTTrue, java.lang.Object)
     */
    public Object visit(ASTTrue node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTFalse, java.lang.Object)
     */
    public Object visit(ASTFalse node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTBlock, java.lang.Object)
     */
    public Object visit(ASTBlock node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTText, java.lang.Object)
     */
    public Object visit(ASTText node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTIfStatement, java.lang.Object)
     */
    public Object visit(ASTIfStatement node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTElseStatement, java.lang.Object)
     */
    public Object visit(ASTElseStatement node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTElseIfStatement, java.lang.Object)
     */
    public Object visit(ASTElseIfStatement node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTObjectArray, java.lang.Object)
     */
    public Object visit(ASTObjectArray node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTDirective, java.lang.Object)
     */
    public Object visit(ASTDirective node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTWord, java.lang.Object)
     */
    public Object visit(ASTWord node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTSetDirective, java.lang.Object)
     */
    public Object visit(ASTSetDirective node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTEscapedDirective, java.lang.Object)
     * @since 1.5
     */
    public Object visit(ASTEscapedDirective node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTEscape, java.lang.Object)
     * @since 1.5
     */
    public Object visit(ASTEscape node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTMap, java.lang.Object)
     * @since 1.5
     */
    public Object visit(ASTMap node, Object data)
    {
        return showNode(node,data);
    }

    /**
     * @see org.apache.velocity.runtime.visitor.BaseVisitor#visit(org.apache.velocity.runtime.parser.node.ASTIntegerRange, java.lang.Object)
     */
    public Object visit(ASTIntegerRange node, Object data)
    {
        return showNode(node,data);
    }
}
